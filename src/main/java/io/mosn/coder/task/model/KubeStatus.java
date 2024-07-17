package io.mosn.coder.task.model;

import com.alibaba.fastjson.annotation.JSONField;

public class KubeStatus {

    @JSONField(name = "Name")
    private String name;

    @JSONField(name = "Host")
    private String host;

    @JSONField(name = "Kubelet")
    private String kubelet;

    @JSONField(name = "APIServer")
    private String aPIServer;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getKubelet() {
        return kubelet;
    }

    public void setKubelet(String kubelet) {
        this.kubelet = kubelet;
    }

    public String getaPIServer() {
        return aPIServer;
    }

    public void setaPIServer(String aPIServer) {
        this.aPIServer = aPIServer;
    }
}
