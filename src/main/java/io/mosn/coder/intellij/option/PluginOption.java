package io.mosn.coder.intellij.option;

import io.mosn.coder.intellij.roobot.PluginContext;

/**
 * @author yiji@apache.org
 */
public abstract class PluginOption {

    public static String DEFAULT_API = "v0.0.0-20211217011300-b851d129be01";

    public static String DEFAULT_PKG = "v0.0.0-20211217101631-d914102d1baf";

    public abstract PluginType getPluginType();

    public abstract String pluginTypeDescriptor();

    public abstract String getPluginName();

    public abstract String getOrganization();

    public PluginContext context() {
        return context;
    }

    public void setContext(PluginContext context) {
        this.context = context;
    }

    /**
     * metadata.json api version
     */
    public String getApi() {
        if (api != null) {
            return api;
        }
        return DEFAULT_API;
    }

    /**
     * metadata.json pkg version
     */
    public String getPkg() {
        if (pkg != null) {
            return pkg;
        }
        return DEFAULT_PKG;
    }

    abstract void destroy();

    protected String api;

    protected String pkg;

    protected PluginContext context;

    public void setApi(String api) {
        this.api = api;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }
}
