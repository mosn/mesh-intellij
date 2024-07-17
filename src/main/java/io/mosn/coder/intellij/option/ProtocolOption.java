package io.mosn.coder.intellij.option;

import java.util.List;

/**
 * @author yiji@apache.org
 */
public class ProtocolOption extends AbstractOption {

    /**
     * internal protocol
     */
    private boolean internal;

    private boolean stringRequestId;

    /**
     * http protocol
     */
    private boolean http;

    private boolean injectHead;

    /**
     * egress port
     */
    protected Integer clientPort;


    /**
     * ingress port
     */
    protected Integer serverPort;

    /**
     * connection pool mode
     */
    private PoolMode poolMode;

    /**
     * codec option
     */
    private CodecOption codecOption;

    /**
     * create optional http filter or not
     */
    private FilterOption filterOption;

    private List<ProtocolOption> embedded;

    private List<ProtocolOption> listenerPort;

    private List<ProtocolOption> exportPort;

    @Override
    public PluginType getPluginType() {
        return PluginType.Protocol;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isHttp() {
        return http;
    }

    public void setHttp(boolean http) {
        this.http = http;
    }

    public Integer getClientPort() {
        return clientPort;
    }

    public void setClientPort(Integer clientPort) {
        this.clientPort = clientPort;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public PoolMode getPoolMode() {
        return poolMode;
    }

    public void setPoolMode(PoolMode poolMode) {
        this.poolMode = poolMode;
    }

    public CodecOption getCodecOption() {
        return codecOption;
    }

    public void setCodecOption(CodecOption codecOption) {
        this.codecOption = codecOption;
    }

    public FilterOption getFilterOption() {
        return filterOption;
    }

    public void setFilterOption(FilterOption filterOption) {
        this.filterOption = filterOption;
    }

    public boolean isInjectHead() {
        return injectHead;
    }

    public void setInjectHead(boolean injectHead) {
        this.injectHead = injectHead;
    }

    public List<ProtocolOption> getEmbedded() {
        return embedded;
    }

    public void setEmbedded(List<ProtocolOption> embedded) {
        this.embedded = embedded;
    }

    public List<ProtocolOption> getListenerPort() {
        return listenerPort;
    }

    public void setListenerPort(List<ProtocolOption> listenerPort) {
        this.listenerPort = listenerPort;
    }

    public List<ProtocolOption> getExportPort() {
        return exportPort;
    }

    public void setExportPort(List<ProtocolOption> exportPort) {
        this.exportPort = exportPort;
    }

    public boolean isXmlCodec() {
        return this.getCodecOption() != null
                && this.getCodecOption().fixedLengthCodec
                && this.getCodecOption().prefix != null
                && this.getCodecOption().length > 0;
    }

    public String Alias() {
        String p = this.name.toLowerCase();
        // fist char should be start with 'A-Z'
        if (p.length() == 1) {
            p = this.name.toUpperCase();
        } else if (p.length() >= 1) {
            p = p.substring(0, 1).toUpperCase() + p.substring(1);
        }

        return p;
    }

    @Override
    public String pluginTypeDescriptor() {
        return "protocol";
    }

    public boolean isStringRequestId() {
        return stringRequestId;
    }

    public void setStringRequestId(boolean stringRequestId) {
        this.stringRequestId = stringRequestId;
    }
}
