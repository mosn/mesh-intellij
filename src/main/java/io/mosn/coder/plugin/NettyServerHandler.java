package io.mosn.coder.plugin;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.mosn.coder.plugin.Protocol.*;

/**
 * @author yiji@apache.org
 */
public class NettyServerHandler extends ChannelDuplexHandler {

    private static final Logger LOG = LoggerFactory.getLogger(NettyServerHandler.class.getName());

    /**
     * action -> handler
     */
    Map<String, Handler> handlers = new LinkedHashMap<>();

    /**
     * file handler context key
     */
    AttributeKey<Context> contextKey = AttributeKey.valueOf("file_server_handler_context");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof Request) {
            handleRequest(ctx, (Request) msg);
        } else if (msg instanceof Response) {
            handleResponse(ctx, (Response) msg);
        }
    }

    private void handleRequest(ChannelHandlerContext ctx, Request msg) {
        try {
            Protocol.Request request = msg;
            if (request.isHeartbeat()) {
                /**
                 * just reply heartbeat response.
                 */
                ctx.writeAndFlush(Response.heartbeat(request.requestId));
                return;
            }

            /**
             * action -> handler
             */
            if (request.headers != null) {
                String action = request.headers.get(Context.action);
                Handler handler = getHandler(action);

                /**
                 * server not support current action
                 */
                if (handler == null) {
                    ctx.writeAndFlush(Response.unknownAction(request))
                            .addListener((ChannelFutureListener) future -> ctx.close());
                    return;
                }

                Response response = handler.handleRequest(context(ctx), request);
                if (response != null) {
                    ctx.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                        if ((!response.isSuccess() || !future.isSuccess())
                                && future.channel().isActive()) {
                            ctx.close();
                        }
                    });
                }
            }
        } finally {
            msg.release();
        }
    }

    private void handleResponse(ChannelHandlerContext ctx, Response msg) {
        try {
            Response response = msg;
            if (response.isHeartbeat()) {
                /**
                 * heartbeat response, do nothing.
                 */
                return;
            }


            /**
             * action -> handler
             */
            if (response.headers != null) {
                String action = response.headers.get(Context.action);
                Handler handler = getHandler(action);

                /**
                 * server not support current action
                 */
                if (handler == null) {
                    return;
                }

                handler.handleResponse(context(ctx), response);
            }
        } finally {
            msg.release();
        }
    }

    private Handler getHandler(String action) {
        Handler handler = this.handlers.get(action);
        if (handler == null) {
            handler = HandlerAdapter.handler(action);
            // register callback
            if (handler != null)
                this.handlers.put(action, handler);
        }
        return handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        /**
         * create file server context
         */

        createContext(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        /**
         * invoke all handler destroy
         */

        destroy(ctx);

        cleanContext(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("close connection because error occurred", cause);

        if (ctx.channel().isActive()) {
            /**
             * close local tcp connection and trigger resource destroy.
             */
            ctx.close();
        }
    }

    private void destroy(ChannelHandlerContext ctx) {
        if (!handlers.isEmpty()) {
            for (Handler handler : handlers.values()) {
                try {
                    handler.destroy(context(ctx));
                } catch (Exception e) {
                    LOG.error("failed to invoke handler destroy", e);
                }
            }
        }
    }

    private void createContext(ChannelHandlerContext ctx) {
        Attribute<Context> context = ctx.channel().attr(contextKey);
        if (context.get() == null) {
            context.set(new Context(ctx));
        }
    }

    private void cleanContext(ChannelHandlerContext ctx) {
        /**
         * clean file server context
         */
        Attribute<Context> context = ctx.channel().attr(contextKey);

        Context c = context.getAndSet(null);
        if (c != null) {
            // help gc
            c.ctx = null;
        }
    }

    Context context(ChannelHandlerContext ctx) {
        Attribute<Context> key = ctx.channel().attr(contextKey);

        Context context = key.get();
        if (context == null) {
            throw new RuntimeException("bad context");
        }

        return context;
    }
}