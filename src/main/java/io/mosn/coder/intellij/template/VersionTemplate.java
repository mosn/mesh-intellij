package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

public class VersionTemplate implements Template {

    public static final String Name = "VERSION.txt";

    public static final String Path = "";

    @Override
    public List<Source> create(PluginOption option) {

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append("1.0.0-dev", false);


        return Arrays.asList(new Source(Name, Path, buffer.toString()));
    }
}