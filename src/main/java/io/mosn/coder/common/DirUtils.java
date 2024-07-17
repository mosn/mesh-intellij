package io.mosn.coder.common;

import io.mosn.coder.intellij.option.PluginType;

public class DirUtils {


    public final static String ROOT_DIR = "plugins";

    public final static String ROOT_CONFIG_DIR = "configs";

    public final static String CODECS_DIR = "codecs";

    public final static String STREAM_FILTERS_DIR = "stream_filters";

    public final static String TRANSCODER_DIR = "transcoders";

    public final static String TRACE_DIR = "traces";

    public static boolean isPluginDirectory(String current, String parent) {
        return isPluginTypeDir(current)
                && isRootDirectory(parent);
    }

    public static boolean isPluginTypeDir(String current) {
        if (current == null || current.length() == 0)
            return false;

        return CODECS_DIR.equals(current)
                || STREAM_FILTERS_DIR.equals(current)
                || TRANSCODER_DIR.equals(current)
                || TRACE_DIR.equals(current);
    }

    public static boolean isPluginChildDirectory(String parent, String grandpa) {
        return isPluginDirectory(parent, grandpa);
    }

    public static boolean isRootDirectory(String parent) {
        return parent != null && ROOT_DIR.equals(parent) || isRootConfigDirectory(parent);
    }

    public static boolean isRootConfigDirectory(String parent) {
        return parent != null && ROOT_CONFIG_DIR.equals(parent);
    }

    public static PluginType pluginTypeOf(String dir) {
        if (dir == null || dir.length() == 0) return null;

        switch (dir) {
            case STREAM_FILTERS_DIR:
                return PluginType.Filter;
            case TRANSCODER_DIR:
                return PluginType.Transcoder;
            case CODECS_DIR:
                return PluginType.Protocol;
            case TRACE_DIR:
                return PluginType.Trace;
            default:
                return null;
        }
    }

}
