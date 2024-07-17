package io.mosn.coder.intellij.internal;

import io.mosn.coder.intellij.option.ProtocolOption;

public class DubboOptionImpl extends ProtocolOption {

    public DubboOptionImpl() {
        this.name = "dubbo";
        this.clientPort = 30880;
        this.serverPort = 30800;
    }
}
