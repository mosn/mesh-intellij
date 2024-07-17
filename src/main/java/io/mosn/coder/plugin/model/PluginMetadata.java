package io.mosn.coder.plugin.model;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;
import java.util.Map;

/**
 * @author yiji@apache.org
 */
public class PluginMetadata extends PluginSimpleMetadata {

    @JSONField(ordinal = 3)
    private String framework;

    @JSONField(ordinal = 4)
    private boolean internal;

    @JSONField(ordinal = 5)
    private List<Variable> variables;

    @JSONField(ordinal = 6)
    private Map<String, String> extension;

    // override parent order
    @JSONField(ordinal = 7)
    private Map<String, String> dependencies;

    public Map<String, String> getExtension() {
        return extension;
    }

    public void setExtension(Map<String, String> extension) {
        this.extension = extension;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    @Override
    public Map<String, String> getDependencies() {
        return dependencies;
    }

    @Override
    public void setDependencies(Map<String, String> dependencies) {
        this.dependencies = dependencies;
    }

    public static class Variable {

        private String field;

        private List<String> pattern;

        private boolean required;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public List<String> getPattern() {
            return pattern;
        }

        public void setPattern(List<String> pattern) {
            this.pattern = pattern;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }

}
