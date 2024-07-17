package io.mosn.coder.intellij.internal;

import io.mosn.coder.intellij.option.ProtocolOption;

public class BoltOptionImpl extends ProtocolOption {

    public BoltOptionImpl() {
        this.name = "bolt";
        this.clientPort = 12220;
        this.serverPort = 12200;
    }
}
