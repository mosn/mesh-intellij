package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class HeaderTemplate implements Template {

    public static final String Name = "header.go";

    public static final String Path = "pkg/common";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.header();

        // write package and import

        buffer.line()
                .append("package common").line()
                .append("import (")
                .append("\t\"mosn.io/api\"")
                .append("\t\"mosn.io/pkg/header\"")
                .append(")").line();

        // write code

        buffer.append("type Header struct {")
                .append("	header.BytesHeader")
                .append("}")
                .line();


        buffer.append("func (h *Header) Clone() api.HeaderMap {").line()
                .append("	return &Header{BytesHeader: *h.BytesHeader.Clone()}")
                .append("}")
                .line();

        buffer.append("var _ api.HeaderMap = &Header{}").line();

        Content = buffer.toString();
    }

    public static Source header() {
        return new Source(Name, Path, Content);
    }

    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(header());
    }
}