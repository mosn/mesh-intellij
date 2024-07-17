package io.mosn.coder.plugin.handler;

import io.mosn.coder.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.mosn.coder.plugin.Protocol.Response;

/**
 * @author yiji@apache.org
 */
public class InitTaskFactory implements HandlerFactory {

    public static final String action = "sys_init_task";

    private static final Logger LOG = LoggerFactory.getLogger(InitTaskFactory.class.getName());

    public InitTaskFactory() {
    }

    public void init() {
        HandlerAdapter.registerFactory(action, this);
    }

    @Override
    public Handler createHandler() {
        return new InitTaskHandler();
    }

    public class InitTaskHandler  extends AbstractHandler {
    }
}
