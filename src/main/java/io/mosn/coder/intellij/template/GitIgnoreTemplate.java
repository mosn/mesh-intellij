package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

public class GitIgnoreTemplate implements Template {

    public static final String Name = ".gitignore";

    public static final String Path = "";

    @Override
    public List<Source> create(PluginOption option) {

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("/build/codecs")
                .append("/build/stream_filters")
                .append("/build/transcoders")
                .append("/build/sidecar/binary/")
                .append("/build/target/")
                .append("/build/upgrade/")
                .append("/logs/")
                .append(".idea");

        return Arrays.asList(new Source(Name, Path, buffer.toString()));
    }

}
