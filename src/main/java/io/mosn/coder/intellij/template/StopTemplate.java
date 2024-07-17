package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class StopTemplate extends AbstractScriptTemplate {

    public static final String Name = "stop.sh";

    public static final String Path = "etc/ant";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append("#!/bin/bash")
                .line()
                .append("# kill container if running")
                .append("sidecar=$(docker ps -a -q -f name=mosn-container)")
                .append("if [[ -n \"$sidecar\" ]]; then")
                .append("  echo \"mosn-container is running and terminating...\"")
                .append("  docker stop mosn-container >/dev/null")
                .append("  docker rm -f mosn-container >/dev/null")
                .append("  echo \"terminated ok\"")
                .append("else")
                .append("  echo \"no mosn-container is running\"")
                .append("fi");

        Content = buffer.toString();
    }

    public static Source stopSource() {
        return new Source(Name, Path, Content);
    }


    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(stopSource());
    }
}
