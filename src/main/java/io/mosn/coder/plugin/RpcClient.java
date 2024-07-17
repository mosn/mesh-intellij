package io.mosn.coder.plugin;

import io.mosn.coder.common.TimerHolder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Timeout;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.mosn.coder.plugin.Protocol.*;

/**
 * @author yiji@apache.org
 */
public class RpcClient {

    static short defaultTimeout = 30000;

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class.getName());

    Bootstrap bootstrap;

    String host;

    int port;

    String address;

    volatile Channel channel;

    static int defaultIoThreads = Math.min(Runtime.getRuntime().availableProcessors() + 1, 2);

    List<Runnable> closeCallbacks = new ArrayList<>();

    static final NioEventLoopGroup nioEventLoopGroup =
            new NioEventLoopGroup(defaultIoThreads, new DefaultThreadFactory("NettyClientWorker", true));

    static final Map<Short, AsyncFuture> results = new ConcurrentHashMap<>();

    static final Map<String, RpcClient> clients = new HashMap<>();
    static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    static final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    static final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public Response request(Request request) {
        return AsyncRequest(request).getResult();
    }

    public AsyncFuture AsyncRequest(Request request) {
        if (request.timeout == 0) request.timeout = defaultTimeout;

        int rpcTimeout = request.timeout;
        if (rpcTimeout < 0) {
            /**
             * short timeout: max 32765, we use header timeout
             */
            String tm = request.header(Request.TIMEOUT_KEY);
            if (tm != null) {
                rpcTimeout = Integer.parseInt(tm);
            }
        }

        AsyncFuture asyncFuture = AsyncFuture.of(request);
        Timeout timeout = TimerHolder.getTimer().newTimeout(tt -> {
            AsyncFuture future = AsyncFuture.remove(request);
            if (future != null) {
                Response response = Response.create(request)
                        .failed(ERROR_TIMEOUT, "timeout");
                future.putResponse(response);
            }
        }, rpcTimeout, TimeUnit.MILLISECONDS);

        asyncFuture.addTimeout(timeout);

        // send to remote
        Channel ch = ensureConnected().channel;
        try {
            ch.writeAndFlush(request)
                    .addListener((ChannelFutureListener) channelFuture -> {
                        if (!channelFuture.isSuccess()) {

                            AsyncFuture future = AsyncFuture.remove(request);
                            if (future != null) {
                                future.cancelTimeout();
                                Response response = Response.create(request).failed(Protocol.ERROR_INTERNAL
                                        , channelFuture.cause().getMessage());
                                future.putResponse(response);
                                logger.error("sending failed, remote address: " + channelFuture.channel().remoteAddress(), channelFuture.cause());
                            }
                        }
                    });
        } catch (Exception e) {
            AsyncFuture future = AsyncFuture.remove(request);
            if (future != null) {
                future.cancelTimeout();
                Response response = Response.create(request).failed(Protocol.ERROR_INTERNAL
                        , e.getMessage());
                future.putResponse(response);

                logger.error("sending invocation failed, remote address: " + ch.remoteAddress(), e);
            }
        }

        return asyncFuture;
    }

    Channel connect() throws RuntimeException {

        bootstrap = new Bootstrap();
        bootstrap.group(nioEventLoopGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class);

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

        bootstrap.handler(new ChannelInitializer() {

            @Override
            protected void initChannel(Channel ch) {
                NettyCodecAdapter adapter = new NettyCodecAdapter();
                ch.pipeline()
                        .addLast("decoder", adapter.getDecoder())
                        .addLast("encoder", adapter.getEncoder())
                        .addLast("handler", new NettyServerHandler() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);

                                /**
                                 * run disconnect callback
                                 */

                                if (!RpcClient.this.closeCallbacks.isEmpty()) {
                                    for (Runnable task : RpcClient.this.closeCallbacks) {
                                        try {
                                            task.run();
                                        } catch (Exception ignored) {
                                        }
                                    }

                                    RpcClient.this.closeCallbacks.clear();
                                }
                            }
                        });
            }
        });

        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));

        boolean complete = future.awaitUninterruptibly(3000, TimeUnit.MILLISECONDS);
        if (complete && future.isSuccess()) {
            Channel newChannel = future.channel();
            try {
                // Close old channel
                Channel oldChannel = RpcClient.this.channel; // copy reference
                if (oldChannel != null) {
                    logger.info("Close old netty channel " + oldChannel + " on create new netty channel "
                            + newChannel);
                    oldChannel.close();
                }
            } finally {
                RpcClient.this.channel = newChannel;
            }
        } else {
            throw new RuntimeException(
                    "client failed to connect to server " + host + ":" + port + ", error message is:" + (
                            future.cause() == null ? "unknown" : future.cause()));
        }

        return future.channel();
    }

    private RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.address = host + ":" + port;
    }

    public static RpcClient getClient(String address) {
        String[] hosts = address.split(":");
        if (hosts != null && hosts.length == 2) {
            String host = hosts[0];
            String port = hosts[1];
            return getClient(host, Integer.parseInt(port));
        }

        throw new RuntimeException("bad address " + address + ", expect format host:port ");
    }

    static RpcClient getClient(String host, int port) {

        readLock.lock();
        try {
            RpcClient client = clients.get(host + ":" + port);
            // connection is active
            if (client != null && client.channel != null && client.channel.isActive()) {
                return client;
            }
        } finally {
            readLock.unlock();
        }

        return new RpcClient(host, port).ensureConnected();
    }

    RpcClient ensureConnected() {

        if (this.channel != null
                && this.channel.isActive()) {
            return this;
        }

        writeLock.lock();
        try {
            // double check: connection is active
            if (this.channel != null && this.channel.isActive()) {
                return this;
            }

            // client is not init or connection is closed already.
            if (this.channel == null || !this.channel.isActive()) {
                if (this.connect().isActive()) {

                    // put into client cache.
                    this.clients.put(address, this);
                }
            }

            return this;
        } finally {
            writeLock.unlock();
        }

    }

    public void addCloseCallback(Runnable task) {
        if (task != null
                && !this.closeCallbacks.contains(task)) {
            this.closeCallbacks.add(task);
        }
    }


    public void destroy() {
        if (RpcClient.clients != null) {
            for (RpcClient client : RpcClient.clients.values()) {
                /**
                 * close all connection
                 */
                Channel channel = client.channel;
                if (channel != null && channel.isActive()) {
                    channel.close();
                }
            }
        }

        if (RpcClient.results != null
                && !RpcClient.results.isEmpty()) {
            RpcClient.results.clear();
        }

    }

    public static class AsyncFuture {

        Request request;
        Response response;

        Timeout timeout;

        ReentrantLock lock = new ReentrantLock();
        Condition notAck = lock.newCondition();

        FutureCallback callback;

        AtomicBoolean executed = new AtomicBoolean(false);

        private AsyncFuture(Request request) {
            this.request = request;
        }

        static AsyncFuture of(Request request) {
            AsyncFuture future = new AsyncFuture(request);
            RpcClient.results.put(request.requestId, future);

            return future;
        }

        static AsyncFuture remove(Request request) {
            return remove(request.requestId);
        }

        static AsyncFuture remove(Short requestId) {
            AsyncFuture future = RpcClient.results.remove(requestId);
            return future;
        }

        void addTimeout(Timeout timeout) {
            this.timeout = timeout;
        }

        public void cancelTimeout() {
            if (this.timeout != null) {
                this.timeout.cancel();
            }
        }

        public FutureCallback getCallback() {
            return callback;
        }

        public void whenComplete(FutureCallback callback) {
            this.callback = callback;
        }

        public Response getResult() {

            if (response != null) {
                return response;
            }

            lock.lock();
            try {

                long start = System.currentTimeMillis();

                int rpcTimeout = request.timeout;
                if (request.timeout == 0) {
                    rpcTimeout = defaultTimeout;
                }

                if (rpcTimeout < 0) {
                    /**
                     * short timeout: max 32765, we use header timeout
                     */
                    String tm = request.header(Request.TIMEOUT_KEY);
                    if (tm != null) {
                        rpcTimeout = Integer.parseInt(tm);
                    }
                }

                boolean notTimeout = notAck.await(rpcTimeout, TimeUnit.MILLISECONDS);
                if (!notTimeout) {
                    this.response = Response.create(request).failed(ERROR_TIMEOUT, "timeout " + (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)) + "s");
                }
            } catch (Throwable e) {
                this.response = Response.create(request).failed(ERROR_INTERNAL, e.getMessage());
            } finally {
                lock.unlock();
            }

            /**
             * run callback if required.
             */
            executeCallBack();

            return response;
        }

        void putResponse(Response response) {
            lock.lock();
            try {
                this.response = response;
                notAck.signal();
            } finally {
                lock.unlock();
            }

            /**
             * run callback if required.
             */
            executeCallBack();
        }

        void executeCallBack() {
            if (this.callback != null) {
                if (executed.compareAndSet(false, true)) {
                    this.callback.run(this);
                }
            }
        }
    }

    public interface FutureCallback {
        void run(AsyncFuture future);
    }
}