package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.AbstractOption;
import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.mosn.coder.intellij.option.AbstractOption.X_MOSN_DATA_ID;

/**
 * @author yiji@apache.org
 */
public class RegistryShellTemplate extends AbstractCodeTemplate {

    @Override
    public List<Source> create(PluginOption option) {

        if (option != null) {
            // support protocol pub/sub feature
            if (option instanceof ProtocolOption) {
                ProtocolOption opt = (ProtocolOption) option;

                String name = "auto_pub_sub.sh";
                String proto = option.getPluginName().toLowerCase();
                String path = "configs/codecs/" + proto;

                List<Source> sources = new ArrayList<>();
                CodeBuilder buffer = new CodeBuilder(new StringBuilder());

                // generate subscribe shell
                buffer.append("#!/bin/bash")
                        .line()
                        .with("export SERVICE_ID=\"").with(proto).with("-provider").with("@").with(proto).with("\" # please change ").with(proto).with("-provider").append(" to your service identity")
                        .append("export BACKEND_PORT=7755            # please change port 7755 to your java server port")
                        .with("export PROVIDER_APP=").with(proto).append("-provider")
                        .line()
                        .with("export MOCK_PUB_DATA=")
                        .with("\"{\\\"protocolType\\\": \\\"")
                        .with(proto).with("\\\", ").append("\\\"providerMetaInfo\\\": { \\\"appName\\\": \\\"${PROVIDER_APP}\\\",\\\"properties\\\": {\\\"application\\\": \\\"${PROVIDER_APP}\\\",\\\"port\\\": \\\"${BACKEND_PORT}\\\" }},\t\\\"serviceName\\\": \\\"${SERVICE_ID}\\\"}\"")
                        .line()
                        .with("export MOCK_SUB_DATA=\"{\\\"protocolType\\\":\\\"")
                        .with(proto).append("\\\",\\\"serviceName\\\":\\\"${SERVICE_ID}\\\"}\"")
                        .line()
                        .append("echo \"publish service ${SERVICE_ID}\"")
                        .append("echo \"curl -d \\\"${MOCK_PUB_DATA}\\\" localhost:13330/services/publish\"")
                        .append("curl -s -d \"${MOCK_PUB_DATA}\" localhost:13330/services/publish")
                        .line()
                        .append("sleep 2")
                        .line()
                        .append("echo")
                        .append("echo")
                        .append("echo \"subscribe service ${SERVICE_ID}\"")
                        .append("echo \"curl -d \\\"${MOCK_SUB_DATA}\\\" localhost:13330/services/subscribe\"")
                        .append("curl -s -d \"${MOCK_SUB_DATA}\" localhost:13330/services/subscribe");

                sources.add(new Source(name, path, buffer.toString()));

                name = "auto_invoke.sh";

                int port;
                if (opt.getClientPort() != null && opt.getClientPort().intValue() > 0) {
                    port = opt.getClientPort().intValue();
                } else {
                    port = opt.getServerPort().intValue();
                }

                buffer = new CodeBuilder(new StringBuilder());
                buffer.append("#!/bin/bash")
                        .line();

                if (opt.isHttp()) {

                    String httpKey = "X-SERVICE"; // default
                    if (opt.getRequiredKeys() != null && !opt.getRequiredKeys().isEmpty()) {
                        for (Map.Entry<AbstractOption.OptionKey<String>, AbstractOption.OptionValue<String>> key : opt.getRequiredKeys()) {
                            AbstractOption.OptionValue<String> optionValue = key.getValue();
                            // skip invalid variable
                            if (optionValue == null
                                    || optionValue.getItems() == null
                                    || optionValue.getItems().isEmpty()) {
                                continue;
                            }

                            if (key.getKey().equals(X_MOSN_DATA_ID)) {
                                httpKey = optionValue.first();
                            }
                        }
                    }

                    buffer.with("# please change ").with(proto).with("-provider").append(" to your service identity")
                            .with("export SERVICE=\"").with(proto).with("-provider").append("\"")
                            .append("# please change userInfo to your request url")
                            .append("export REQUEST_URL=hello")
                            .with("export REQUEST_PORT=").append(String.valueOf(port))
                            .line()
                            .with("REQUEST_COMMAND=\"curl -v -H \\\"").with(httpKey).append(": ${SERVICE}\\\" -H \\\"Content-Type: application/json\\\" localhost:${REQUEST_PORT}/${REQUEST_URL}\"")
                            .line()
                            .append("echo \"${REQUEST_COMMAND}\"")
                            .line()
                            .append("curl -v -H \"X-SERVICE: ${SERVICE}\" -H \"Content-Type: application/json\" localhost:${REQUEST_PORT}/${REQUEST_URL}");

                    sources.add(new Source(name, path, buffer.toString()));

                } else {

                    if (opt.isXmlCodec()) {
                        buffer.append("IP=127.0.0.1")
                                .with("PORT=").append(String.valueOf(port))
                                .line()
                                .append("EXT_REF_ID=\"$(date +%Y%m%d%H%M%S)$RANDOM\"")
                                .line()
                                .append("echo")
                                .append("echo  $IP $PORT $EXT_REF_ID")
                                .line()
                                .append("BODY=$(sed \"s/EXT_REF/${EXT_REF_ID}/g\" request_template.txt)")
                                .line()
                                .with("BODY=$(printf \"%").with(opt.getCodecOption().prefix).with(String.valueOf(opt.getCodecOption().length)).append("d\" ${#BODY})${BODY}")
                                .line()
                                .append("echo")
                                .with("echo \"--------START ").with(proto).append(" invoke---------\"")
                                .append("echo")
                                .line()
                                .append("echo \"$BODY\"")
                                .line()
                                .append("(echo \"$BODY\"; sleep 15;) | nc -4 $IP $PORT")
                                .append("echo")
                                .append("echo \"--------END-------------\"");

                        sources.add(new Source(name, path, buffer.toString()));
                    }

                    // support tcp protocol invoke in the future.
                }

                return sources;
            }
        }

        return null;
    }

}
