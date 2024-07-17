package io.mosn.coder.plugin;

import io.mosn.coder.plugin.Protocol.*;
import io.mosn.coder.plugin.handler.*;

/**
 * @author yiji@apache.org
 */
public class RequestBuilder {

    /**
     * GET /v1/plugin/query_enabled_sidecar_rules
     */
    public static Request newSidecarRuleRequest(String project) {
        return newSidecarRuleRequest(project, null, null);
    }

    /**
     * GET /v1/plugin/query_enabled_sidecar_rule?rule=${id}
     */
    public static Request newSidecarRuleRequest(String project, String ruleId, String updatedId) {

        Request request = Protocol.request(project);

        /**
         * append sidecar rule field
         */
        request.appendHead(WrapCommand.ACTION, SidecarRuleFactory.action);

        if (ruleId != null) {
            request.appendHead(WrapCommand.RULE_ID, ruleId);
        }

        if (updatedId != null) {
            request.appendHead(WrapCommand.UPGRADE_ID, updatedId);
        }

        return request;
    }

    public static Request newSidecarVersionRequest(String project) {

        Request request = Protocol.request(project);

        /**
         * append sidecar rule field
         */
        request.appendHead(WrapCommand.ACTION, SidecarVersionFactory.action);

        return request;
    }

    /**
     * @see InitTaskFactory
     */
    public static Request newInitTaskRequest(String project) {

        Request request = Protocol.request(project);

        request.appendHead(WrapCommand.ACTION, InitTaskFactory.action);

        return request;
    }

    /**
     * @see UploadFileFactory
     */
    public static Request newUploadTaskRequest(String project) {

        Request request = Protocol.request(project);

        request.appendHead(WrapCommand.ACTION, UploadFileFactory.action);

        return request;
    }

    public static Request newCommitTaskRequest(String project) {

        Request request = Protocol.request(project);

        request.appendHead(WrapCommand.ACTION, CommitTaskFactory.action);

        return request;
    }
}
