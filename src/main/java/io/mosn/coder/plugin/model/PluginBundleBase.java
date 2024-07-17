package io.mosn.coder.plugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class PluginBundleBase {

    private Long id;

    private String instanceId;

    private String taskId;

    private String syncMeta;

    private String ruleId;

    private String upgradeId;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSyncMeta() {
        return syncMeta;
    }

    public void setSyncMeta(String syncMeta) {
        this.syncMeta = syncMeta;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getUpgradeId() {
        return upgradeId;
    }

    public void setUpgradeId(String upgradeId) {
        this.upgradeId = upgradeId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    protected String render(List<PluginBundle.Plugin> plugins) {
        StringBuilder buff = new StringBuilder();

        List<PluginBundle.Plugin> filters = new ArrayList<>();
        List<PluginBundle.Plugin> transcoders = new ArrayList<>();
        List<PluginBundle.Plugin> codecs = new ArrayList<>();
        List<PluginBundle.Plugin> traces = new ArrayList<>();
        for (PluginBundle.Plugin plugin : plugins) {
            switch (plugin.getKind()) {
                case PluginBundle.KIND_FILTER:
                    filters.add(plugin);
                    break;
                case PluginBundle.KIND_TRANSCODER:
                    transcoders.add(plugin);
                    break;
                case PluginBundle.KIND_TRACE:
                    traces.add(plugin);
                    break;
                case PluginBundle.KIND_PROTOCOL:
                    codecs.add(plugin);
                    break;
            }
        }

        buff.append("[");
        append(buff, filters);
        append(buff, transcoders);
        append(buff, codecs);
        append(buff, traces);
        buff.append("]");

        return buff.toString();
    }

    protected void append(StringBuilder buff, List<PluginBundle.Plugin> plugins) {
        for (PluginBundle.Plugin plugin : plugins) {
            if (buff.length() > 1) {
                buff.append(", ");
            }

            String fullName = plugin.getFullName();
            if (fullName == null) {
                fullName = plugin.getName() + (
                        plugin.getVersion() == null ? ".zip" :
                                ("-" + plugin.getVersion() + ".zip"));
            }

            buff.append("{")
                    .append("\"name\": \"").append(plugin.getName()).append("\", ")
                    .append("\"fullName\": \"").append(fullName).append("\", ")
                    .append("\"kind\": \"").append(plugin.getKind()).append("\", ");

            if (plugin.getVersion() != null) {
                buff.append("\"version\": \"").append(plugin.getVersion()).append("\", ");
            }

            if (plugin.getRevision() != null) {
                buff.append("\"revision\": \"").append(plugin.getRevision()).append("\", ");
            }

            if (plugin.getDependency() != null) {
                buff.append("\"dependency\": {");
                boolean shouldAppend = false;
                for (String key : plugin.getDependency().keySet()) {
                    if (shouldAppend) {
                        buff.append(",");
                    }
                    String value = plugin.getDependency().get(key);
                    buff.append("\"").append(key).append("\": \"").append(value).append("\"");

                    shouldAppend = true;
                }
                buff.append("}, ");
            }

            buff.append("\"owner\": ").append(plugin.getOwner() == null
                    ? false : plugin.getOwner());

            if (plugin.getFilters() != null) {
                /**
                 * append protocol filter plugin
                 */
                buff.append(",\"stream_filters\": [");
                StringBuilder filterBuff = new StringBuilder();
                append(filterBuff, plugin.getFilters());

                if (filterBuff.length() > 0) {
                    buff.append(filterBuff);
                }

                buff.append("]");
            }

            if (plugin.getTranscoders() != null) {
                /**
                 * append protocol transcoder plugin
                 */
                buff.append(",\"transcoders\": [");
                StringBuilder transcoderBuff = new StringBuilder();
                append(transcoderBuff, plugin.getTranscoders());

                if (transcoderBuff.length() > 0) {
                    buff.append(transcoderBuff);
                }

                buff.append("]");
            }

            buff.append("}");
        }
    }
}
