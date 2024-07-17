package io.mosn.coder.intellij.option;

/**
 * @author yiji@apache.org
 */
public class FilterOption extends AbstractOption {

    private ActiveMode activeMode;

    private String before;

    @Override
    public PluginType getPluginType() {
        return PluginType.Filter;
    }

    @Override
    public String pluginTypeDescriptor() {
        return "stream_filter";
    }

    public ActiveMode getActiveMode() {
        return activeMode;
    }

    public void setActiveMode(ActiveMode activeMode) {
        this.activeMode = activeMode;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }
}
