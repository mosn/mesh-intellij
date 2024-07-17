package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class PackageCodecTemplate  extends AbstractScriptTemplate {

    public static final String Name = "package-codec.sh";

    public static final String Path = "etc/script";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append("#!/bin/bash")
                .line()
                .append("# build codec plugins")
                .append("if [[ -n \"${PLUGIN_CODEC}\" ]]; then")
                .append("  coders=(${PLUGIN_CODEC//,/ })")
                .append("  for name in \"${coders[@]}\"; do")
                .append("    if [[ -n ${PLUGIN_OS} && -n ${PLUGIN_ARCH} ]]; then")
                .append("      export PLUGIN_TARGET=${name}")
                .append("      export PLUGIN_CODEC_OUTPUT=${PLUGIN_CODEC_PREFIX}-${PLUGIN_TARGET}.so")
                .append("      bash /go/src/\"${PLUGIN_PROJECT_NAME}\"/etc/script/compile-codec.sh")
                .append("    elif [[ \"${PLUGIN_BUILD_PLATFORM}\" == \"Darwin\" && \"${PLUGIN_BUILD_PLATFORM_ARCH}\" == \"arm64\" ]]; then")
                .append("      # apple m1 chip compile plugin(amd64)")
                .append("      export PLUGIN_TARGET=${name}")
                .append("      export PLUGIN_CODEC_OUTPUT=${PLUGIN_CODEC_PREFIX}-${PLUGIN_TARGET}.so")
                .line()
                .append("      export PLUGIN_OS=\"linux\"")
                .append("      export PLUGIN_ARCH=\"amd64\"")
                .append("      bash /go/src/\"${PLUGIN_PROJECT_NAME}\"/etc/script/compile-codec.sh")
                .append("    fi")
                .append("  done")
                .append("fi")
                .line()
                .append("pkg_version=")
                .append("if [[ -f \"/go/src/${PLUGIN_PROJECT_NAME}/VERSION.txt\" ]]; then")
                .append("  pkg_version=$(cat /go/src/${PLUGIN_PROJECT_NAME}/VERSION.txt)")
                .append("fi")
                .line()
                .append("# package codec plugins")
                .append("if [[ -n \"${PLUGIN_CODEC}\" ]]; then")
                .append("  coders=(${PLUGIN_CODEC//,/ })")
                .append("  for name in \"${coders[@]}\"; do")
                .append("    PLUGIN_CODEC_ZIP_OUTPUT=${name}.zip")
                .append("    if [[ -n \"${pkg_version}\" ]]; then")
                .append("      PLUGIN_CODEC_ZIP_OUTPUT=${name}-${pkg_version}.zip")
                .append("    fi")
                .append("    rm -rf /go/src/${PLUGIN_PROJECT_NAME}/build/target/codecs/${PLUGIN_CODEC_ZIP_OUTPUT}")
                .append("    mkdir -p /go/src/${PLUGIN_PROJECT_NAME}/build/target/codecs/")
                .append("    if [ -d \"/go/src/${PLUGIN_PROJECT_NAME}/build/codecs/${name}/\" ]; then")
                .append("      cd /go/src/${PLUGIN_PROJECT_NAME}/build/codecs/")
                .append("      echo \"packaging codec ${name}...\"")
                .append("      zip -r /go/src/${PLUGIN_PROJECT_NAME}/build/target/codecs/${PLUGIN_CODEC_ZIP_OUTPUT} ${name} \\")
                .append("        -x \"stream_filters/*\" -x \"transcoders/*\" -x \"mosn_config.json\"")
                .append("    fi")
                .append("  done")
                .append("fi");

        Content = buffer.toString();
    }

    public static Source packageCodecSource() {
        return new Source(Name, Path, Content);
    }


    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(packageCodecSource());
    }
}
