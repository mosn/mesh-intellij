package io.mosn.coder.plugin.handler;

import io.mosn.coder.plugin.*;

import static io.mosn.coder.plugin.Protocol.*;

/**
 * @author yiji@apache.org
 */
public class EventNotifyFactory implements HandlerFactory {

    public static final String action = "sys_event_notify";

    public EventNotifyFactory() {
    }

    public void init() {
        HandlerAdapter.registerFactory(action, this);
    }

    @Override
    public Handler createHandler() {
        return new EventNotifyHandler();
    }

    public class EventNotifyHandler extends AbstractHandler {
    }
}
