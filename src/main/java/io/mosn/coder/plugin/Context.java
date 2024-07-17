package io.mosn.coder.plugin;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * @author yiji@apache.org
 */
public class Context {

    /**
     * netty context
     */
    ChannelHandlerContext ctx;

    public static final String action = "sys_action";

    public Context(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public <T> T attribute(String name) {
        if (ctx == null) return null;

        AttributeKey<T> key = AttributeKey.valueOf(name);
        return attribute(key);
    }

    public <T> T attribute(AttributeKey<T> name) {
        if (ctx == null) return null;

        Attribute<T> attribute = ctx.channel().attr(name);
        return attribute != null ? attribute.get() : null;
    }

    public <T> T putAttribute(String name, T value) {
        if (ctx == null) return null;

        AttributeKey<T> key = AttributeKey.valueOf(name);
        Attribute<T> attribute = ctx.channel().attr(key);
        T old = attribute.getAndSet(value);

        return old;
    }

}
