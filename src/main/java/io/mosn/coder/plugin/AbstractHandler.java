package io.mosn.coder.plugin;

import static io.mosn.coder.plugin.Protocol.*;

public abstract class AbstractHandler extends HandlerAdapter {

    @Override
    public Protocol.Response handleRequest(Context context, Protocol.Request request) {
        return null;
    }

    @Override
    public void handleResponse(Context context, Response response) {
        RpcClient.AsyncFuture future = RpcClient.AsyncFuture.remove(response.requestId);
        if (future != null) {
            future.cancelTimeout();
            future.putResponse(response);
        }
    }
}
