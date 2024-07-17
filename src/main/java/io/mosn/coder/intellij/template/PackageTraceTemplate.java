package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class PackageTraceTemplate extends AbstractScriptTemplate {

    public static final String Name = "package-trace.sh";

    public static final String Path = "etc/script";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append("#!/bin/bash")
                .line()
                .append("# package trace plugins")
                .append("if [[ -n \"${PLUGIN_TRACE}\" ]]; then")
                .append("  if [[ -n ${PLUGIN_OS} && -n ${PLUGIN_ARCH} ]]; then")
                .append("    bash /go/src/\"${PLUGIN_PROJECT_NAME}\"/etc/script/compile-trace.sh")
                .append("  elif [[ \"${PLUGIN_BUILD_PLATFORM}\" == \"Darwin\" && \"${PLUGIN_BUILD_PLATFORM_ARCH}\" == \"arm64\" ]]; then")
                .append("    # apple m1 chip compile plugin(amd64)")
                .append("    export PLUGIN_OS=\"linux\"")
                .append("    export PLUGIN_ARCH=\"amd64\"")
                .append("    bash /go/src/\"${PLUGIN_PROJECT_NAME}\"/etc/script/compile-trace.sh")
                .append("  fi")
                .append("fi")
                .line()
                .append("pkg_version=")
                .append("if [[ -f \"/go/src/${PLUGIN_PROJECT_NAME}/VERSION.txt\" ]]; then")
                .append("  pkg_version=$(cat /go/src/${PLUGIN_PROJECT_NAME}/VERSION.txt)")
                .append("fi")
                .line()
                .append("# package trace plugins")
                .append("if [[ -n \"${PLUGIN_TRACE}\" ]]; then")
                .append("  tracers=(${PLUGIN_TRACE//,/ })")
                .append("  for name in \"${tracers[@]}\"; do")
                .append("    PLUGIN_TRACE_ZIP_OUTPUT=${name}.zip")
                .append("    if [[ -n \"${pkg_version}\" ]]; then")
                .append("      PLUGIN_TRACE_ZIP_OUTPUT=${name}-${pkg_version}.zip")
                .append("    fi")
                .append("    rm -rf /go/src/${PLUGIN_PROJECT_NAME}/build/target/traces/${PLUGIN_TRACE_ZIP_OUTPUT}")
                .append("    mkdir -p /go/src/${PLUGIN_PROJECT_NAME}/build/target/traces/")
                .append("    if [ -d \"/go/src/${PLUGIN_PROJECT_NAME}/build/traces/${name}/\" ]; then")
                .append("      cd /go/src/${PLUGIN_PROJECT_NAME}/build/traces/")
                .append("      echo \"packaging trace ${name}...\"")
                .append("      zip -r /go/src/${PLUGIN_PROJECT_NAME}/build/target/traces/${PLUGIN_TRACE_ZIP_OUTPUT} ${name} \\")
                .append("        -x \"stream_filters/*\" -x \"transcoders/*\" -x \"mosn_config.json\"")
                .append("    fi")
                .append("  done")
                .append("fi");


        Content = buffer.toString();
    }

    public static Source packageTranscoderSource() {
        return new Source(Name, Path, Content);
    }


    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(packageTranscoderSource());
    }
}