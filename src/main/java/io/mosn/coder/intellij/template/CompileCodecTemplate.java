package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class CompileCodecTemplate extends AbstractScriptTemplate {

    public static final String Name = "compile-codec.sh";

    public static final String Path = "etc/script";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendHeader(buffer);

        buffer.append("build_opts=\"\"")
                .append("if [[ -n ${PLUGIN_OS} && -n ${PLUGIN_ARCH} ]]; then")
                .append("  build_opts=\"GOOS=${PLUGIN_OS} GOARCH=${PLUGIN_ARCH}\"")
                .append("  echo \"compiling codec ${PLUGIN_TARGET} for ${PLUGIN_OS} ${PLUGIN_ARCH} ...\"")
                .append("else")
                .append("  echo \"compiling codec ${PLUGIN_TARGET} for linux $(dpkg --print-architecture) ...\"")
                .append("fi")
                .line()
                .append("export BUILD_OPTS=${build_opts}")
                .append("export PLUGIN_CODEC_OUTPUT=${PLUGIN_CODEC_PREFIX}-${PLUGIN_TARGET}.so")
                .append("if [[ -n \"${PLUGIN_GIT_VERSION}\" ]]; then")
                .append("  export PLUGIN_CODEC_OUTPUT=${PLUGIN_CODEC_PREFIX}-${PLUGIN_TARGET}-${PLUGIN_GIT_VERSION}.so")
                .append("fi")
                .append("make compile-codec")
                .line()
                .append("# build stream filter")
                .append("if [[ -n \"${PLUGIN_STREAM_FILTER}\" ]]; then")
                .append("  bash /go/src/\"${PLUGIN_PROJECT_NAME}\"/etc/script/compile-filter.sh")
                .append("fi")
                .line()
                .append("# copy stream filter")
                .append("if [[ -n \"${PLUGIN_STREAM_FILTER}\" ]]; then")
                .append("  filters=(${PLUGIN_STREAM_FILTER//,/ })")
                .append("  rm -rf /go/src/${PLUGIN_PROJECT_NAME}/build/codecs/${PLUGIN_TARGET}/stream_filters")
                .append("  meta=")
                .append("  for name in \"${filters[@]}\"; do")
                .append("    mkdir -p /go/src/${PLUGIN_PROJECT_NAME}/build/codecs/${PLUGIN_TARGET}/stream_filters/${name}")
                .append("    echo \"cp  /go/src/${PLUGIN_PROJECT_NAME}/build/stream_filters/${name} /go/src/${PLUGIN_PROJECT_NAME}/build/codecs/${PLUGIN_TARGET}/stream_filters/\"")
                .append("    cp -r /go/src/${PLUGIN_PROJECT_NAME}/build/stream_filters/${name} \\")
                .append("      /go/src/${PLUGIN_PROJECT_NAME}/build/codecs/${PLUGIN_TARGET}/stream_filters/")
                .append("    # append ,")
                .append("    if [[ -n ${meta} ]]; then")
                .append("      meta+=\",\"")
                .append("    fi")
                .append("    meta+=\"\\\"${name}\\\"\"")
                .append("  done")
                .line()
                .append("  # write metadata")
                .append("  echo \"{\\\"plugins\\\":[${meta}]}\" >/go/src/${PLUGIN_PROJECT_NAME}/build/codecs/${PLUGIN_TARGET}/stream_filters/plugin-meta.json")
                .append("fi")
                .line()
                .append("# build transcoder")
                .append("if [[ -n \"${PLUGIN_TRANSCODER}\" ]]; then")
                .append("  bash /go/src/\"${PLUGIN_PROJECT_NAME}\"/etc/script/compile-transcoder.sh")
                .append("fi")
                .line()
                .append("# copy transcoder")
                .append("if [[ ${PLUGIN_TRANSCODER} != \"\" ]]; then")
                .append("  coders=(${PLUGIN_TRANSCODER//,/ })")
                .append("  rm -rf /go/src/${PLUGIN_PROJECT_NAME}/build/codecs/${PLUGIN_TARGET}/transcoders")
                .append("  for name in \"${coders[@]}\"; do")
                .append("    mkdir -p /go/src/${PLUGIN_PROJECT_NAME}/build/codecs/${PLUGIN_TARGET}/transcoders/${name}/")
                .append("    echo \"cp  /go/src/${PLUGIN_PROJECT_NAME}/build/transcoders/${name} /go/src/${PLUGIN_PROJECT_NAME}/build/codecs/${PLUGIN_TARGET}/transcoders/\"")
                .append("    cp -r /go/src/${PLUGIN_PROJECT_NAME}/build/transcoders/${name} \\")
                .append("      /go/src/${PLUGIN_PROJECT_NAME}/build/codecs/${PLUGIN_TARGET}/transcoders/")
                .append("  done")
                .append("fi");


        Content = buffer.toString();
    }

    public static Source compileCodecSource() {
        return new Source(Name, Path, Content);
    }

    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(compileCodecSource());
    }

}