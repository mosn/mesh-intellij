package io.mosn.coder.plugin;

import com.alipay.sofa.registry.client.api.registration.PublisherRegistration;
import com.alipay.sofa.registry.client.factory.RegistryClientFactory;
import io.mosn.coder.common.NetUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static io.mosn.coder.plugin.Protocol.Port;

/**
 * @author yiji@apache.org
 */
public class PluginFileServer {

    private static final String defaultGroup = "SOFA";

    private static final Logger logger = LoggerFactory.getLogger(PluginFileServer.class.getName());

    public static void main(String[] args) throws InterruptedException {
        startSocketService(Port, true);
    }

    public void init() throws InterruptedException {
        startSocketService(Port, false);
        publishPluginServer(Port);
    }

    private void publishPluginServer(int port) {

        String serviceName = "com.alipay.sofa.cloud.middleware.plugin.server";

        String serviceData = "plugin://" + NetUtils.getLocalHost() + ":" + port ;

        PublisherRegistration dsrRegistration;

        dsrRegistration = new PublisherRegistration(serviceName);
        dsrRegistration.setGroup(defaultGroup);

        RegistryClientFactory.getRegistryClient()
                .register(dsrRegistration, serviceData);

        logger.info("register plugin server " + serviceData);
    }

    public static void startSocketService(int port, boolean block) throws InterruptedException {

        ServerBootstrap bootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", true));
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(2, new DefaultThreadFactory("NettyServerWorker", true));

        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024) // 1M
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        NettyCodecAdapter adapter = new NettyCodecAdapter();
                        ch.pipeline()
                                .addLast("decoder", adapter.getDecoder())
                                .addLast("encoder", adapter.getEncoder())
                                .addLast("handler", new NettyServerHandler());
                    }
                });
        // bind
        ChannelFuture channelFuture = bootstrap.bind("0.0.0.0", port);
        channelFuture.syncUninterruptibly();
        Channel channel = channelFuture.channel();

        if (block)
            blockThread();
    }

    private static void blockThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }
}
