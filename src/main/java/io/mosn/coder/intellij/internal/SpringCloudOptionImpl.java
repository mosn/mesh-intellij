package io.mosn.coder.intellij.internal;

import io.mosn.coder.intellij.option.ProtocolOption;

public class SpringCloudOptionImpl extends ProtocolOption {

    public SpringCloudOptionImpl() {
        this.name = "springcloud";
        this.clientPort = 10088;
        this.serverPort = 10080;
    }
}
