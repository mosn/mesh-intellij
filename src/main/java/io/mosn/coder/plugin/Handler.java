package io.mosn.coder.plugin;

import static io.mosn.coder.plugin.Protocol.*;

/**
 * action -> handler
 *
 * @author yiji@apache.org
 */
public interface Handler {

    /**
     * handle client request.
     */
    Response handleRequest(Context context, Request request);

    /**
     * handle service response.
     */
    void handleResponse(Context context, Response response);


    /**
     * destroy invoked when connection closed
     */
    void destroy(Context context);

}
