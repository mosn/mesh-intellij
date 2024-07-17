package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.internal.DubboOptionImpl;
import io.mosn.coder.intellij.internal.SpringCloudOptionImpl;
import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.option.TraceOption;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class GoModTemplate implements Template {

    @Override
    public List<Source> create(PluginOption option) {
        String name = "go.mod";
        String path = "";

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        if (option != null) {
            buffer.with("module ").append(option.context().getModule())
                    .line()
                    .append("go 1.18")
                    .line()
                    .append("require (")
                    .with("\tmosn.io/api ").append(option.getApi())
                    .with("\tmosn.io/pkg ").append(option.getPkg());

            boolean appendHttpDependency = false;
            boolean appendDubboDependency = false;

            if (option instanceof ProtocolOption) {
                ProtocolOption opt = (ProtocolOption) option;
                if (opt.getEmbedded() != null && !opt.getEmbedded().isEmpty()) {
                    for (ProtocolOption o : opt.getEmbedded()) {
                        if (o instanceof DubboOptionImpl) {
                            appendDubboDependency = true;
                        }

                        if (o instanceof SpringCloudOptionImpl) {
                            appendHttpDependency = true;
                        }
                    }
                }

                if (!appendHttpDependency) {
                    appendHttpDependency = opt.isHttp();
                }
            }

            if (appendDubboDependency) {
                // append dependencies
                buffer.append("\tgithub.com/apache/dubbo-go-hessian2 v1.9.2 // dubbo")
                        .append("\tgithub.com/stretchr/testify v1.7.0 // dubbo")
                        .append("\tgopkg.in/yaml.v3 v3.0.0-20210107192922-496545a6307b // indirect from dubbo");
            }

            if (appendHttpDependency) {
                buffer.append("\tgithub.com/valyala/fasthttp v1.31.0");
            }

            if (option instanceof TraceOption) {
                buffer.append("\tgithub.com/google/uuid v1.3.0");
                String traceType;
                if ((traceType = ((TraceOption) option).getRealTraceType()) != null) {
                    switch (traceType) {
                        case TraceOption.SKY_WALKING: {
                            buffer.append("\tgithub.com/SkyAPM/go2sky v0.5.0")
                                    .append("\tgoogle.golang.org/grpc v1.49.0")
                                    .append("\tgoogle.golang.org/protobuf v1.28.0")
                                    .append("\tgithub.com/golang/protobuf v1.4.3");
                            break;
                        }
                        case TraceOption.ZIP_KIN: {
                            buffer.append("\tgithub.com/openzipkin/zipkin-go v0.4.1");
                            break;
                        }
                    }
                }
            }

            buffer.append(")")
                    .line()
                    .append("replace (")
                    .append("\tgithub.com/rcrowley/go-metrics => github.com/rcrowley/go-metrics v0.0.0-20201227073835-cf1acfcdf475");

            if (appendHttpDependency) {
                buffer.append("\tgithub.com/klauspost/compress => github.com/klauspost/compress v1.13.5 // fast http");
            }

            buffer.append(")");
        }

        return Arrays.asList(new Source(name, path, buffer.toString()));
    }
}
