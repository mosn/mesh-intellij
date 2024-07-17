package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class DynamicConfigTemplate implements Template {

    public static final String Name = "dynamic.go";

    public static final String Path = "pkg/config";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.header();

        // write package and import

        buffer.line()
                .append("package config").line()
                .append("import (")
                .append("\t\"context\"")
                .append("\t\"sync\"")
                .append(")").line();

        // write code

        buffer.append("const ExtendConfigKey = \"global_extend_config\"").line();


        buffer.append("func GlobalExtendMapByContext(ctx context.Context) (*sync.Map, bool) {")
                .append("	cfg, ok := ctx.Value(ExtendConfigKey).(*sync.Map)")
                .append("	return cfg, ok")
                .append("}")
                .line();

        buffer.append("func GlobalExtendConfigByContext(ctx context.Context, key string) (string, bool) {")
                .append("	cfg, ok := GlobalExtendMapByContext(ctx)")
                .append("	if !ok {")
                .append("		return \"\", false")
                .append("	}")
                .append("	info, ok := cfg.Load(key)")
                .append("	if !ok {")
                .append("		return \"\", false")
                .append("	}")
                .append("	v, ok := info.(string)")
                .append("	return v, ok")
                .append("}")
                .line();

        Content = buffer.toString();
    }

    public static Source dynamic() {
        return new Source(Name, Path, Content);
    }

    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(dynamic());
    }
}