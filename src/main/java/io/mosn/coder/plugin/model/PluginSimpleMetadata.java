package io.mosn.coder.plugin.model;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Map;

/**
 * @author yiji@apache.org
 */
public class PluginSimpleMetadata {

    @JSONField(ordinal = 1)
    private String name;

    @JSONField(ordinal = 2)
    private String kind;

    @JSONField(ordinal = 3)
    private Map<String, String> dependencies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, String> dependencies) {
        this.dependencies = dependencies;
    }
}
