package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class PackageTranscoderTemplate extends AbstractScriptTemplate {

    public static final String Name = "package-transcoder.sh";

    public static final String Path = "etc/script";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append("#!/bin/bash")
                .line()
                .append("# package transcoder plugins")
                .append("if [[ -n \"${PLUGIN_TRANSCODER}\" ]]; then")
                .append("  if [[ -n ${PLUGIN_OS} && -n ${PLUGIN_ARCH} ]]; then")
                .append("    bash /go/src/\"${PLUGIN_PROJECT_NAME}\"/etc/script/compile-transcoder.sh")
                .append("  elif [[ \"${PLUGIN_BUILD_PLATFORM}\" == \"Darwin\" && \"${PLUGIN_BUILD_PLATFORM_ARCH}\" == \"arm64\" ]]; then")
                .append("    # apple m1 chip compile plugin(amd64)")
                .append("    export PLUGIN_OS=\"linux\"")
                .append("    export PLUGIN_ARCH=\"amd64\"")
                .append("    bash /go/src/\"${PLUGIN_PROJECT_NAME}\"/etc/script/compile-transcoder.sh")
                .append("  fi")
                .append("fi")
                .line()
                .append("pkg_version=")
                .append("if [[ -f \"/go/src/${PLUGIN_PROJECT_NAME}/VERSION.txt\" ]]; then")
                .append("  pkg_version=$(cat /go/src/${PLUGIN_PROJECT_NAME}/VERSION.txt)")
                .append("fi")
                .line()
                .append("# package transcoder plugins")
                .append("if [[ -n \"${PLUGIN_TRANSCODER}\" ]]; then")
                .append("  codecs=(${PLUGIN_TRANSCODER//,/ })")
                .append("  for name in \"${codecs[@]}\"; do")
                .append("    PLUGIN_TRANSCODER_ZIP_OUTPUT=${name}.zip")
                .append("    if [[ -n \"${pkg_version}\" ]]; then")
                .append("      PLUGIN_TRANSCODER_ZIP_OUTPUT=${name}-${pkg_version}.zip")
                .append("    fi")
                .append("    rm -rf /go/src/${PLUGIN_PROJECT_NAME}/build/target/transcoders/${PLUGIN_TRANSCODER_ZIP_OUTPUT}")
                .append("    mkdir -p /go/src/${PLUGIN_PROJECT_NAME}/build/target/transcoders/")
                .append("    if [ -d \"/go/src/${PLUGIN_PROJECT_NAME}/build/transcoders/${name}/\" ]; then")
                .append("      cd /go/src/${PLUGIN_PROJECT_NAME}/build/transcoders/")
                .append("      echo \"packaging transcoder ${name}...\"")
                .append("      zip -r /go/src/${PLUGIN_PROJECT_NAME}/build/target/transcoders/${PLUGIN_TRANSCODER_ZIP_OUTPUT} ${name} \\")
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