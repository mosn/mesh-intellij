package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yiji@apache.org
 */
public class StartTemplate extends AbstractScriptTemplate {

    public static final String Name = "start.sh";

    public static final String Path = "etc/ant";

    public static Source runSource(PluginOption option) {

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append("#!/bin/bash")
                .line()
                .append("BASE_IMAGE=zonghaishang/delve:v1.20.1")
                .line()
                .append("DEBUG_PORTS=\"-p 2345:2345\"")
                .with("LISTENER_PORTS=\"-p 11001:11001");

        Set<Integer> excludePort = new HashSet<>();

        StringBuilder sb = new StringBuilder();
        excludePort.add(11001);

        if (option instanceof ProtocolOption) {
            ProtocolOption opt = (ProtocolOption) option;
            appendExportPort(sb, opt, excludePort);

            /**
             * append listener backup ports
             */
            if (opt.getListenerPort() != null) {
                for (ProtocolOption o : opt.getListenerPort()) {
                    appendExportPort(sb, o, excludePort);
                }
            }
        }
        // append listener port to LISTENER_PORTS
        buffer.with(sb.toString()).append("\"");

        buffer.with("EXPORT_PORTS=\"-p 34901:34901 -p 13330:13330");
        excludePort.add(34901);
        excludePort.add(13330);

        sb = new StringBuilder();
        if (option instanceof ProtocolOption) {
            ProtocolOption opt = (ProtocolOption) option;
            /**
             * append export backup ports
             */
            if (opt.getExportPort() != null) {
                for (ProtocolOption o : opt.getExportPort()) {
                    appendExportPort(sb, o, excludePort);
                }
            }
        }
        buffer.with(sb.toString()).append("\"");

        buffer
                .line()
                .append("# biz port export");

        sb = new StringBuilder();
        if (option instanceof ProtocolOption) {
            ProtocolOption opt = (ProtocolOption) option;
            if (opt.getEmbedded() != null) {
                for (ProtocolOption o : opt.getEmbedded()) {
                    appendExportPort(sb, o, excludePort);
                }
            }
        }

        buffer.with("BIZ_PORTS=\"").with(sb.toString()).append("\"");

        buffer.line()
                .append("MAPPING_PORTS=\"${DEBUG_PORTS} ${LISTENER_PORTS} ${EXPORT_PORTS} ${BIZ_PORTS}\"")
                .line()
                .append("sidecar=$(docker ps -a -q -f name=mosn-container)")
                .append("if [[ -n \"$sidecar\" ]]; then")
                .append("  echo")
                .append("  echo \"found mosn-container is running already and terminating...\"")
                .append("  docker stop mosn-container >/dev/null")
                .append("  docker rm -f mosn-container >/dev/null")
                .append("  rm -rf \"${FULL_PROJECT_NAME}/logs\"")
                .append("  echo \"terminated ok\"")
                .append("  echo")
                .append("fi")
                .line()
                .append("DEBUG_MODE=${DLV_DEBUG}")
                .line()
                .append("chmod +x etc/ant/run.sh")
                .line()
                .append("# export local ip for mosn")
                .append("os_name=$(uname)")
                .append("if [[ \"$os_name\" == \"Linux\" ]]; then")
                .append("    export PUB_BOLT_LOCAL_IP=$(ip -f inet address | grep inet | grep -v docker | grep -v 127.0.0.1 | head -n1 | awk '{print $2}' | awk -F/ '{print $1}')")
                .append("  else")
                .append("    # default for mac os")
                .append("    export PUB_BOLT_LOCAL_IP=$(ipconfig getifaddr en0)")
                .append("fi")
                .append("echo \"host address: ${PUB_BOLT_LOCAL_IP} ->  ${PROJECT_NAME}\"")
                .line()
                .append("# create mapping logs directory")
                .append("if [[ -d ${FULL_PROJECT_NAME}/logs ]]; then")
                .append("   rm -rf ${FULL_PROJECT_NAME}/logs")
                .append("fi")
                .append("mkdir -p ${FULL_PROJECT_NAME}/logs")
                .line()
                .append("docker run ${DOCKER_BUILD_OPTS} \\")
                .append("  -u admin --privileged \\")
                .append("  -e PLUGIN_PROJECT_NAME=\"${PROJECT_NAME}\" \\")
                .append("  -e DYNAMIC_CONF_PATH=/go/src/${PROJECT_NAME}/build/codecs \\")
                .append("  -e SIDECAR_PROJECT_NAME=${SIDECAR_GITLAB_PROJECT_NAME} \\")
                .append("  -e SIDECAR_DLV_DEBUG=\"${DEBUG_MODE}\" \\")
                .append("  -v ${FULL_PROJECT_NAME}:/go/src/${PROJECT_NAME} \\")
                .append("  -v ${FULL_PROJECT_NAME}/logs:/home/admin/logs \\")
                .append("  -v $(go env GOPATH)/src/${SIDECAR_GITLAB_PROJECT_NAME}:/go/src/${SIDECAR_GITLAB_PROJECT_NAME} \\")
                .append("  -d --name mosn-container --env-file \"${FULL_PROJECT_NAME}\"/etc/ant/env_conf ${MAPPING_PORTS} \\")
                .append("  -w /go/src/${PROJECT_NAME} \\")
                .append("  ${BASE_IMAGE} /go/src/${PROJECT_NAME}/etc/ant/run.sh \"$@\"")
                .line()
                .append("echo \"start mosn-container container success.\"")
                .append("echo \"run 'docker exec -it mosn-container /bin/bash' command enter mosn container.\"");


        String content = buffer.toString();

        return new Source(Name, Path, content);
    }

    private static void appendExportPort(StringBuilder sb, ProtocolOption opt, Set<Integer> excludePort) {
        if (opt.getClientPort() != null && opt.getClientPort() > 0) {
            if (!excludePort.contains(opt.getClientPort())) {
                sb.append(" -p ").append(opt.getClientPort()).append(":").append(opt.getClientPort());

                excludePort.add(opt.getClientPort());
            }
        }
        if (opt.getServerPort() != null && opt.getServerPort() > 0) {
            if (!excludePort.contains(opt.getServerPort())) {
                sb.append(" -p ").append(opt.getServerPort()).append(":").append(opt.getServerPort());

                excludePort.add(opt.getServerPort());
            }
        }
    }


    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(runSource(option));
    }

}
