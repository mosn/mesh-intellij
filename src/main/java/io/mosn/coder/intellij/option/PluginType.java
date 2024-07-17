package io.mosn.coder.intellij.option;

/**
 * @author yiji@apache.org
 */
public enum PluginType {

    /**
     * protocol plugin, support create protocol extension
     */
    Protocol,
    /**
     * stream filter plugin, support create filter extension
     */
    Filter,
    /**
     * transcoder plugin, support protocol transcode extension
     */
    Transcoder,

    /**
     * trace plugin, support sky-walking and zipkin.
     */
    Trace

}
