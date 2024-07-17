package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class BinaryDirectoryTemplate implements Template {

    @Override
    public List<Source> create(PluginOption option) {
        String name = "";
        String path = "build/sidecar/binary";

        return Arrays.asList(new Source(name, path, (String)null));
    }

}
