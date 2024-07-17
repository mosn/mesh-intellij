package io.mosn.coder.plugin.model;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;
import java.util.Map;

/**
 * @author yiji@apache.org
 */
public class PluginBundleRule extends PluginBundleBase {

    /**
     * only used http api
     */
    private Boolean success;

    /**
     * only used http api
     */
    private String description;

    /**
     * reused for rest api:
     * GET /v1/plugin/query_enabled_sidecar_rules
     * GET /v1/plugin/query_upgrade_sidecar
     */
    private List<PluginRule> pluginRules;

    /**
     * GET /v1/plugin/query_enabled_sidecar_rule?rule=${id}
     */
    private PluginTask bindingTask;

    public List<PluginRule> getPluginRules() {
        return pluginRules;
    }

    public void setPluginRules(List<PluginRule> pluginRules) {
        this.pluginRules = pluginRules;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public PluginTask getBindingTask() {
        return bindingTask;
    }

    public void setBindingTask(PluginTask bindingTask) {
        this.bindingTask = bindingTask;
    }

    public String render() {

        StringBuilder buff = new StringBuilder();

        /**
         *
         * render document: https://yuque.antfin-inc.com/ypgo6v/vadb69/wvywdl#wqPFy
         *
         * GET /v1/plugin/query_enabled_sidecar_rules
         *
         * GET /v1/plugin/query_upgrade_sidecar
         *
         * @see PluginBundleRule#pluginRules
         */
        if (this.getPluginRules() != null) {
            buff.append("{");
            buff.append("\"pluginRules\":[");
            boolean appendSplit = false;
            for (PluginBundleRule.PluginRule pluginRule : this.getPluginRules()) {
                if (appendSplit) {
                    buff.append(",");
                }
                appendSplit = true;
                append(buff, pluginRule);
            }
            buff.append("]");
            buff.append("}");
            return buff.toString();
        }

        /**
         * GET /v1/plugin/query_enabled_sidecar_rule?rule=${id}
         */

        buff.append("{");

        if (this.getBindingTask() != null) {
            buff.append("\"bindingTask\":{");
            PluginBundleRule.PluginTask task = this.getBindingTask();
            if (task.getPluginRule() != null) {
                buff.append("\"pluginRule\":");
                append(buff, task.getPluginRule());
            }

            if (task.getPluginBundle() != null
                    && !task.getPluginBundle().isEmpty()) {
                buff.append(",\"pluginBundle\":");
                String bundle = render(task.getPluginBundle());
                buff.append(bundle);
            }

            buff.append("}");
        }

        buff.append("}");

        return buff.toString();
    }

    protected void append(StringBuilder buff, PluginBundleRule.PluginRule pluginRule) {
        buff.append("{");

        if (pluginRule.getId() != null) buff.append("\"rule\": \"").append(pluginRule.getId()).append("\", ");
        if (pluginRule.getSidecarVersion() != null)
            buff.append("\"version\": \"").append(pluginRule.getSidecarVersion()).append("\", ");

        if (pluginRule.getImage() != null) buff.append("\"image\": \"").append(pluginRule.getImage()).append("\", ");
        if (pluginRule.getCluster() != null)
            buff.append("\"cluster\": \"").append(pluginRule.getCluster()).append("\", ");

        buff.append("\"rule_name\": \"").append(pluginRule.getName()).append("\" ");
        buff.append("}");
    }

    public static class PluginTask {

        private PluginRule pluginRule;

        private List<PluginBundle.Plugin> pluginBundle;

        public PluginRule getPluginRule() {
            return pluginRule;
        }

        public void setPluginRule(PluginRule pluginRule) {
            this.pluginRule = pluginRule;
        }

        public List<PluginBundle.Plugin> getPluginBundle() {
            return pluginBundle;
        }

        public void setPluginBundle(List<PluginBundle.Plugin> pluginBundle) {
            this.pluginBundle = pluginBundle;
        }
    }

    public static class PluginRule {

        private PluginTask pluginTask;

        private Long id;

        private String instanceId;

        private String cluster;

        private String name;

        @JSONField(name = "version")
        private String sidecarVersion;

        private String image;

        private Map<String, String> attributes;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSidecarVersion() {
            return sidecarVersion;
        }

        public void setSidecarVersion(String sidecarVersion) {
            this.sidecarVersion = sidecarVersion;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }

        public String getCluster() {
            return cluster;
        }

        public void setCluster(String cluster) {
            this.cluster = cluster;
        }

        public PluginTask getPluginTask() {
            return pluginTask;
        }

        public void setPluginTask(PluginTask pluginTask) {
            this.pluginTask = pluginTask;
        }
    }
}
