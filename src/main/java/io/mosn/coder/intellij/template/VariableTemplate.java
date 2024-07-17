package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;

import java.util.Arrays;
import java.util.List;

public class VariableTemplate extends AbstractCodeTemplate {

    public static final String Name = "variable.go";

    public static final String Path = "pkg/keys";

    @Override
    public List<Source> create(PluginOption option) {

        String content = plainText("/META-INF/template.standard/variable.go.template");
        if (content != null) {
            return Arrays.asList(new Source(Name, Path, content));
        }

        LOG.warning("variable.go not generate correctly, maybe bug triggered.");

        return null;
    }

}
