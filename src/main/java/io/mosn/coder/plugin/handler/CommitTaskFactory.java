package io.mosn.coder.plugin.handler;


import io.mosn.coder.plugin.*;

import static io.mosn.coder.plugin.Protocol.Response;

/**
 * @author yiji@apache.org
 */
public class CommitTaskFactory implements HandlerFactory {

    public static final String action = "sys_commit_task";

    public CommitTaskFactory() {
    }

    public void init() {
        HandlerAdapter.registerFactory(action, this);
    }

    @Override
    public Handler createHandler() {
        return new CommitTaskHandler();
    }

    public class CommitTaskHandler extends AbstractHandler {
    }
}
