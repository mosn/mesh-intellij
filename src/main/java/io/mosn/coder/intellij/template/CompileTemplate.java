package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class CompileTemplate extends AbstractScriptTemplate {

    public static final String Name = "compile.sh";

    public static final String Path = "etc/script";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendHeader(buffer);

        buffer.append("export PLUGIN_PROJECT=${PROJECT_NAME}")
                .append2("export SIDECAR_PROJECT=${SIDECAR_PROJECT_NAME}");

        buffer.append("MAJOR_VERSION=$(cat VERSION)")
                .append("GIT_VERSION=$(git log -1 --pretty=format:%h)").line();

        buffer.append("rm -rf \"/go/src/${PLUGIN_PROJECT}/build/sidecar/binary/\"")
                .append("mkdir -p \"/go/src/${PLUGIN_PROJECT}/build/sidecar/binary/\"").line();


        buffer.append2("build_opts=\"CGO_ENABLED=1\"")
                .append("if [[ -n ${PLUGIN_OS} && -n ${PLUGIN_ARCH} ]]; then")
                .append("  export GOOS=${PLUGIN_OS}")
                .append("  export GOARCH=${PLUGIN_ARCH}")
                .line()
                .append("  build_opts=\"${build_opts} GOOS=${GOOS} GOARCH=${GOARCH}\"")
                .append("  echo \"compiling mosn for ${PLUGIN_OS} ${PLUGIN_ARCH} ...\"")
                .append("else")
                .append("  echo \"compiling mosn for linux $(dpkg --print-architecture) ...\"")
                .append("fi").line();

        buffer.append2("export CGO_ENABLED=1")
                .append2("echo \"${build_opts} go build -o mosn ${SIDECAR_PROJECT}/cmd/mosn/main\"");

        buffer.append("go build -mod=readonly -gcflags \"all=-N -l\" \\")
                .append("  -ldflags \"-B 0x$(head -c20 /dev/urandom | od -An -tx1 | tr -d ' \\n') -X main.Version=${MAJOR_VERSION} -X main.GitVersion=${GIT_VERSION}\" \\")
                .append("  -o mosn \"${SIDECAR_PROJECT}/cmd/mosn/main\"").line();

        buffer.append("if [[ -f mosn ]]; then")
                .append("  md5sum -b mosn | cut -d' ' -f1 >mosn-${MAJOR_VERSION}-${GIT_VERSION}.md5")
                .append("  mv mosn-${MAJOR_VERSION}-${GIT_VERSION}.md5 \"/go/src/${PLUGIN_PROJECT}/build/sidecar/binary/mosn-${MAJOR_VERSION}-${GIT_VERSION}.md5\"")
                .append("  mv mosn \"/go/src/${PLUGIN_PROJECT}/build/sidecar/binary/mosn\"")
                .append("  cp \"/go/src/${SIDECAR_PROJECT}/go.mod\" \"/go/src/${PLUGIN_PROJECT}/build/sidecar/binary/local.mod\"")
                .append("  chmod 644 /go/src/${PLUGIN_PROJECT}/build/sidecar/binary/*")
                .append("    echo \"compile success\"")
                .append("else ")
                .append("    echo \"compile failed\"")
                .append("fi");

        Content = buffer.toString();
    }

    public static Source compileSource() {
        return new Source(Name, Path, Content);
    }

    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(compileSource());
    }

}