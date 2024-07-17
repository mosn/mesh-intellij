package io.mosn.coder.intellij.option;

/**
 * @author yiji@apache.org
 */
public class TranscoderOption extends AbstractOption {

    /**
     * only Client or Server is valid.
     */
    private ActiveMode activeMode;

    private String srcProtocol;

    private String dstProtocol;

    @Override
    public PluginType getPluginType() {
        return PluginType.Transcoder;
    }

    @Override
    public String pluginTypeDescriptor() {
        return "transcoder";
    }

    public ActiveMode getActiveMode() {
        return activeMode;
    }

    public void setActiveMode(ActiveMode activeMode) {
        this.activeMode = activeMode;
    }

    public String getSrcProtocol() {
        return srcProtocol;
    }

    public void setSrcProtocol(String srcProtocol) {
        this.srcProtocol = srcProtocol;
    }

    public String getDstProtocol() {
        return dstProtocol;
    }

    public void setDstProtocol(String dstProtocol) {
        this.dstProtocol = dstProtocol;
    }
}
