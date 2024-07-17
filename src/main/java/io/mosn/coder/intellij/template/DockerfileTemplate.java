package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class DockerfileTemplate implements Template {

    public static String Path = "build/image";
    public static String Name = "Dockerfile";

    @Override
    public List<Source> create(PluginOption option) {

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("FROM golang:1.20.1")
                .append("MAINTAINER yiji@apache.org")
                .line()
                .append("ENV GO111MODULE=on")
                .append("ENV GOPROXY=https://goproxy.cn,direct")
                .line()
                .append("RUN go install github.com/go-delve/delve/cmd/dlv@v1.20.1")
                .line()
                .append("# install tools.")
                .append("RUN apt update && apt -y install net-tools sudo openbsd-inetd \\")
                .append("    telnet tcpdump zip iptables vim libpcre3-dev openssl libssl-dev zlib1g zlib1g-dev \\")
                .append("    iputils-ping wrk sysstat man")
                .line()
                .append("# change container root pwd")
                .append("RUN  useradd -ms /bin/bash admin && \\")
                .append("     echo \"root:root#123\" | chpasswd && \\")
                .append("     echo \"admin:admin#123\" | chpasswd")
                .line()
                .append("WORKDIR /go")
                .line()
                .append("# how to use this Dockefile build multiple platform image ?")
                .append("# docker buildx create --use --name=builder --driver docker-container")
                .append("# docker buildx build --platform linux/arm64,linux/amd64 -t zonghaishang/delve:v1.20.1 . --push")
                .append("# docker buildx rm builder")
                .line()
                .append("# ====> how to use this Dockefile build local image for development ? <====")
                .append("# docker buildx rm builder > /dev/null 2>&1")
                .append("# docker buildx create --use --name=builder --driver docker-container")
                .append("# docker buildx build --load --platform linux/arm64 -t zonghaishang/delve:v1.20.1 .")
                .append("# docker buildx build --load --platform linux/amd64 -t zonghaishang/delve:v1.20.1 .")
                .append("# ====> end to use this Dockefile build local image for development <====");

        return Arrays.asList(new Source(Name, Path, buffer.toString()));
    }

}
