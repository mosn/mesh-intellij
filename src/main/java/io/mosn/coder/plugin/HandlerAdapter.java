package io.mosn.coder.plugin;


import io.mosn.coder.plugin.handler.*;
import io.mosn.coder.plugin.model.PluginBundle;
import io.mosn.coder.plugin.model.PluginBundleRule;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.mosn.coder.plugin.Protocol.*;

/**
 * @author yiji@apache.org
 */
public abstract class HandlerAdapter implements Handler {

    /**
     * all user implements handler should be registered here
     */
    private static Map<String, HandlerFactory> allHandlers = new ConcurrentHashMap<>();

    static {
        registerFactory(EventNotifyFactory.action, new EventNotifyFactory());

        registerFactory(InitTaskFactory.action, new InitTaskFactory());
        registerFactory(UploadFileFactory.action, new UploadFileFactory());
        registerFactory(CommitTaskFactory.action, new CommitTaskFactory());

        registerFactory(SidecarRuleFactory.action, new SidecarRuleFactory());
        registerFactory(SidecarVersionFactory.action, new SidecarVersionFactory());
    }

    public static void registerFactory(String action, HandlerFactory factory) {
        if (!allHandlers.containsKey(action)) {
            allHandlers.put(action, factory);
        }
    }

    /**
     * create handler for registered action.
     */
    public static Handler handler(String action) {
        HandlerFactory factory = allHandlers.get(action);
        return factory != null ? factory.createHandler() : null;
    }

    @Override
    public void handleResponse(Context context, Response response) {
    }

    @Override
    public void destroy(Context context) {
    }
}
