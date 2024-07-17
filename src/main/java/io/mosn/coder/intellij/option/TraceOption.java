package io.mosn.coder.intellij.option;

public class TraceOption extends AbstractOption {

    public static final String SKY_WALKING =  "sky-walking";

    public static final String ZIP_KIN = "zipkin";

    String realTraceType;

    String reporterType;

    public String getRealTraceType() {
        return realTraceType;
    }

    public void setRealTraceType(String realTraceType) {
        this.realTraceType = realTraceType;
    }

    public String getReporterType() {
        return reporterType;
    }

    public void setReporterType(String reporterType) {
        this.reporterType = reporterType;
    }

    @Override
    public PluginType getPluginType() {
        return PluginType.Trace;
    }

    @Override
    public String pluginTypeDescriptor() {
        return "trace";
    }
}
