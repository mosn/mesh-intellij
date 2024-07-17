package io.mosn.coder.intellij.roobot;


/**
 * @author yiji@apache.org
 */
public interface PluginContext {

    /**
     * create generated code
     */
    void createTemplateCode();

    /**
     * release resource
     */
    void destroy();

    public String getModule();
}
