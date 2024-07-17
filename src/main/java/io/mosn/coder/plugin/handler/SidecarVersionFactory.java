package io.mosn.coder.plugin.handler;

import io.mosn.coder.plugin.*;
import io.mosn.coder.plugin.model.PluginBundleRule;

import static io.mosn.coder.plugin.Protocol.*;

/**
 * @author yiji@apache.org
 */
public class SidecarVersionFactory implements HandlerFactory {

    public static final String action = "sys_active_sidecar_version";

    public SidecarVersionFactory() {
    }

    public void init() {
        HandlerAdapter.registerFactory(action, this);
    }

    @Override
    public Handler createHandler() {
        return new SidecarRuleHandler();
    }

    public class SidecarRuleHandler extends AbstractHandler {
    }
}
