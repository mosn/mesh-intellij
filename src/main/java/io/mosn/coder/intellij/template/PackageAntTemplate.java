package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class PackageAntTemplate extends AbstractScriptTemplate {

    public static final String Name = "package-ant.sh";

    public static final String Path = "etc/script";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append("#!/bin/bash")
                .line()
                .append("# package transcoder plugins")
                .append("if [[ -f \"/go/src/${PLUGIN_PROJECT_NAME}/build/sidecar/binary/mosn\" ]]; then")
                .append("  PLUGIN_ANT_ZIP_OUTPUT=mosn.zip")
                .append("  if [[ -f \"/go/src/${PLUGIN_PROJECT_NAME}/etc/bundle/${PLUGIN_ANT_ZIP_OUTPUT}\" ]]; then")
                .append("    rm -rf \"/go/src/${PLUGIN_PROJECT_NAME}/etc/bundle/${PLUGIN_ANT_ZIP_OUTPUT}\"")
                .append("  fi")
                .append("  cd \"/go/src/${PLUGIN_PROJECT_NAME}/build/sidecar/binary/\"")
                .append("  echo \"packaging mosn...\"")
                .append("  zip -r \"${PLUGIN_ANT_ZIP_OUTPUT}\" .")
                .append("  mv \"/go/src/${PLUGIN_PROJECT_NAME}/build/sidecar/binary/${PLUGIN_ANT_ZIP_OUTPUT}\" \\")
                .append("    \"/go/src/${PLUGIN_PROJECT_NAME}/etc/bundle/${PLUGIN_ANT_ZIP_OUTPUT}\"")
                .append("fi");

        Content = buffer.toString();
    }

    public static Source packageAntSource() {
        return new Source(Name, Path, Content);
    }


    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(packageAntSource());
    }
}
