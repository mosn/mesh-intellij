package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class CompileFilterTemplate extends AbstractScriptTemplate {

    public static final String Name = "compile-filter.sh";

    public static final String Path = "etc/script";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendHeader(buffer);

        buffer.append("# build stream filter plugins")
                .append("if [[ -n \"${PLUGIN_STREAM_FILTER}\" ]]; then")
                .append("  filters=(${PLUGIN_STREAM_FILTER//,/ })")
                .append("  rm -rf /go/src/${PLUGIN_PROJECT_NAME}/build/stream_filters")
                .append("  for name in \"${filters[@]}\"; do")
                .append("    export PLUGIN_TARGET=${name}")
                .append("    export PLUGIN_STEAM_FILTER_OUTPUT=${PLUGIN_STEAM_FILTER_PREFIX}-${PLUGIN_TARGET}.so")
                .append("    if [[ -n \"${PLUGIN_GIT_VERSION}\" ]]; then")
                .append("      export PLUGIN_STEAM_FILTER_OUTPUT=${PLUGIN_STEAM_FILTER_PREFIX}-${PLUGIN_TARGET}-${PLUGIN_GIT_VERSION}.so")
                .append("    fi")
                .append("    # check BUILD_OPTS")
                .append("    if [[ -n ${PLUGIN_OS} && -n ${PLUGIN_ARCH} ]]; then")
                .append("      build_opts=\"GOOS=${PLUGIN_OS} GOARCH=${PLUGIN_ARCH}\"")
                .append("      export BUILD_OPTS=${build_opts}")
                .append("      echo \"compiling filter ${name} for ${PLUGIN_OS} ${PLUGIN_ARCH} ...\"")
                .append("    else")
                .append("      echo \"compiling filter ${name} for linux $(dpkg --print-architecture) ...\"")
                .append("    fi")
                .append("    make compile-stream-filter")
                .append("  done")
                .append("fi");

        Content = buffer.toString();
    }

    public static Source compileFilterSource() {
        return new Source(Name, Path, Content);
    }

    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(compileFilterSource());
    }

}