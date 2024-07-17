package io.mosn.coder.intellij.view;

import com.goide.sdk.GoSdk;
import com.goide.wizard.GoNewGopathBasedProjectSettings;
import io.mosn.coder.intellij.GoModuleWizardStep;
import io.mosn.coder.intellij.option.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author yiji@apache.org
 */
public class GoNewProjectSettings extends GoNewGopathBasedProjectSettings {

    ProtocolOption protocol;

    FilterOption filter;

    TranscoderOption transcoder;

    TraceOption trace;

    protected GoModuleWizardStep step;

    public GoNewProjectSettings(@NotNull GoSdk sdk, boolean indexEntireGoPath, GoModuleWizardStep step) {
        super(sdk, indexEntireGoPath);
        this.step = step;
        this.step.setSettings(this);

        this.protocol = new ProtocolOption();
        this.filter = new FilterOption();
        this.transcoder = new TranscoderOption();

        this.trace = new TraceOption();
    }

    public ProtocolOption getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolOption protocol) {
        this.protocol = protocol;
    }

    public FilterOption getFilter() {
        return filter;
    }

    public void setFilter(FilterOption filter) {
        this.filter = filter;
    }

    public TranscoderOption getTranscoder() {
        return transcoder;
    }

    public TraceOption getTrace() {
        return trace;
    }

    public void setTrace(TraceOption trace) {
        this.trace = trace;
    }

    public void setTranscoder(TranscoderOption transcoder) {
        this.transcoder = transcoder;
    }

    public GoModuleWizardStep getStep() {
        return step;
    }

    public void setStep(GoModuleWizardStep step) {
        this.step = step;
    }

    public void updatePluginModel() {
        this.step.getProto().updatePluginModel(this);
    }

    public PluginType activePlugin() {
        return this.step.getProto().selectedPluginType();
    }
}
