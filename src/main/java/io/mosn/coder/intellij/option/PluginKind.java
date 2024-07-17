package io.mosn.coder.intellij.option;

public class PluginKind {

    public String kind;
    public Plugin[] plugins;

    public PluginKind() {
    }

    public PluginKind(String kind) {
        this.kind = kind;
    }

    public boolean appendPlugin(String name) {

        if (this.plugins == null) {
            this.plugins = new Plugin[0];
        }

        for (Plugin plugin : this.plugins) {
            if (name.equals(plugin.name)) {
                return false;
            }
        }

        Plugin[] plugins = new Plugin[this.plugins.length + 1];
        // insert plugin to last index
        plugins[this.plugins.length] = new Plugin(name);

        System.arraycopy(this.plugins, 0, plugins, 0, this.plugins.length);
        this.plugins = plugins;

        return true;
    }

    @Override
    public String toString() {

        if (kind == null || kind.length() <= 0) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();

        buffer.append("{\"kind\":\"").append(kind).append("\",\"plugins\":[");
        {
            if (this.plugins != null) {
                for (int i = 0; i < this.plugins.length; i++) {
                    Plugin plugin = this.plugins[i];
                    if (i > 0) {
                        buffer.append(",");
                    }
                    buffer.append("{\"name\":\"").append(plugin.name).append("\",\"version\":\"default\"}");
                }
            }
        }
        buffer.append("]}");

        return buffer.toString();
    }
}
