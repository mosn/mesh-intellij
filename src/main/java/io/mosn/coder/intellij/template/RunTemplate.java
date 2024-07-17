package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class RunTemplate extends AbstractScriptTemplate {

    public static final String Name = "run.sh";

    public static final String Path = "etc/ant";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append("#!/bin/bash")
                .line()
                .append("mosn=\"/go/src/${PLUGIN_PROJECT_NAME}/build/sidecar/binary/mosn\"")
                .append("MOSN_PREFIX=/home/admin/mosn")
                .line()
                .append("mkdir -p $MOSN_PREFIX/conf \\")
                .append("  $MOSN_PREFIX/logs \\")
                .append("  $MOSN_PREFIX/bin \\")
                .append("  $MOSN_PREFIX/base_conf/clusters \\")
                .append("  $MOSN_PREFIX/conf/clusters \\")
                .append("  $MOSN_PREFIX/base_conf/certs")
                .line()
                .append("if [[ -d \"/go/src/${SIDECAR_PROJECT_NAME}\" ]]; then")
                .line()
                .append("  if [[ -f \"/go/src/${SIDECAR_PROJECT_NAME}/etc/script/process_checker.sh\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/etc/script/process_checker.sh /home/admin/mosn/bin/process_checker.sh")
                .append("  fi")
                .line()
                .append("  if [[ -f \"/go/src/${SIDECAR_PROJECT_NAME}/etc/script/update_checker.sh\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/etc/script/update_checker.sh /home/admin/mosn/bin/update_checker.sh")
                .append("  fi")
                .line()
                .append("  if [[ -f \"/go/src/${SIDECAR_PROJECT_NAME}/etc/script/zclean.sh\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/etc/script/zclean.sh /home/admin/mosn/bin/zclean.sh")
                .append("  fi")
                .line()
                .append("  if [[ -f \"/go/src/${SIDECAR_PROJECT_NAME}/etc/script/zclean_crontab.sh\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/etc/script/zclean_crontab.sh /home/admin/mosn/bin/zclean_crontab.sh")
                .append("  fi")
                .line()
                .append("  if [[ -f \"/go/src/${SIDECAR_PROJECT_NAME}/etc/script/gen-cert.sh\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/etc/script/gen-cert.sh /home/admin/mosn/base_conf/certs/gen-cert.sh")
                .append("  fi")
                .line()
                .append("  if [[ -f \"/go/src/${SIDECAR_PROJECT_NAME}/etc/script/prestop.sh\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/etc/script/prestop.sh /home/admin/mosn/bin/prestop.sh")
                .append("  fi")
                .line()
                .append("  if [[ -f \"go/src/${SIDECAR_PROJECT_NAME}/etc/script/export_node_port.py\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/etc/script/export_node_port.py /home/admin/mosn/bin/export_node_port.py")
                .append("  fi")
                .line()
                .append("  if [[ -f \"/go/src/${SIDECAR_PROJECT_NAME}/etc/script/modify_iptables.sh\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/etc/script/modify_iptables.sh /home/admin/mosn/bin/modify_iptables.sh")
                .append("  fi")
                .line()
                .append("  if [[ -f \"/go/src/${SIDECAR_PROJECT_NAME}/etc/script/iptables.hijack\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/etc/script/iptables.hijack /home/admin/mosn/bin/iptables.hijack")
                .append("  fi")
                .append("  if [[ -f \"/go/src/${SIDECAR_PROJECT_NAME}/etc/script/clean_iptables.sh\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/etc/script/clean_iptables.sh /home/admin/mosn/bin/clean_iptables.sh")
                .append("  fi")
                .line()
                .append("  # COPY Basic configs")
                .line()
                .append("  if [[ -d \"/go/src/${SIDECAR_PROJECT_NAME}/configs/routers\" ]]; then")
                .append("    cp -r /go/src/${SIDECAR_PROJECT_NAME}/configs/routers $MOSN_PREFIX/base_conf/routers")
                .append("  fi")
                .line()
                .append("  if [[ -d \"/go/src/${SIDECAR_PROJECT_NAME}/configs/listeners\" ]]; then")
                .append("    cp -r /go/src/${SIDECAR_PROJECT_NAME}/configs/listeners $MOSN_PREFIX/base_conf/listeners")
                .append("  fi")
                .line()
                .append("  if [[ -d \"/go/src/${SIDECAR_PROJECT_NAME}/configs/certs\" ]]; then")
                .append("    cp -r /go/src/${SIDECAR_PROJECT_NAME}/configs/certs $MOSN_PREFIX/base_conf/certs")
                .append("  fi")
                .line()
                .append("  if [[ -f \"/go/src/${SIDECAR_PROJECT_NAME}/configs/mosn_config.json\" ]]; then")
                .append("    cp /go/src/${SIDECAR_PROJECT_NAME}/configs/mosn_config.json $MOSN_PREFIX/base_conf/mosn_config.json")
                .append("  else")
                .append("    # no source available, remove source level dependency")
                .append("    cp -r /go/src/${PLUGIN_PROJECT_NAME}/configs/internal/base_conf/routers $MOSN_PREFIX/base_conf/routers")
                .append("    cp -r /go/src/${PLUGIN_PROJECT_NAME}/configs/internal/base_conf/listeners $MOSN_PREFIX/base_conf/listeners")
                .append("    cp /go/src/${PLUGIN_PROJECT_NAME}/configs/internal/base_conf/mosn_config.json $MOSN_PREFIX/base_conf/mosn_config.json")
                .append("  fi")
                .append("fi")
                .line()
                .append("cp \"${mosn}\" /home/admin/mosn/bin/mosn")
                .line()
                .append("chmod +x /home/admin/mosn/bin/mosn")
                .append("chown -R admin:admin /home/admin")
                .append("chmod +x /go/src/${PLUGIN_PROJECT_NAME}/etc/ant/*.sh")
                .append("chmod +x /go/src/${PLUGIN_PROJECT_NAME}/etc/script/*.sh")
                .line()
                .append("echo \"sidecar->  ${mosn}\"")
                .line()
                .append("debug=${SIDECAR_DLV_DEBUG}")
                .append("if [[ -n \"$debug\" && \"$debug\" == \"true\" ]]; then")
                .append("  echo \"running mode: debug, arch: $(dpkg --print-architecture)\"")
                .append("  dlv --listen=0.0.0.0:2345 --continue --headless=true --api-version=2 --accept-multiclient --allow-non-terminal-interactive exec /home/admin/mosn/bin/mosn -- start -c /home/admin/mosn/conf/mosn_config.json -b /home/admin/mosn/base_conf/mosn_config.json -n \"sidecar~$RequestedIP~$POD_NAME.$POD_NAMESPACE~$POD_NAMESPACE.$DOMAINNAME\" -s \"$Sigma_Site\" -l /home/admin/logs/mosn/default.log")
                .append("else")
                .append("  /home/admin/mosn/bin/mosn start -c /home/admin/mosn/conf/mosn_config.json -b /home/admin/mosn/base_conf/mosn_config.json -n \"sidecar~$RequestedIP~$POD_NAME.$POD_NAMESPACE~$POD_NAMESPACE.$DOMAINNAME\" -s \"$Sigma_Site\" -l /home/admin/logs/mosn/default.log")
                .append("fi");

        Content = buffer.toString();
    }

    public static Source runSource() {
        return new Source(Name, Path, Content);
    }


    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(runSource());
    }

}
