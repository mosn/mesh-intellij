package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class ReadMeTemplate extends AbstractCodeTemplate {

    @Override
    public List<Source> create(PluginOption option) {
        String name = "README.md";
        String path = "";

        List<Source> sources = new ArrayList<>();

        String content = plainText("/META-INF/template.standard/readme.template");
        if (content != null) {
            sources.add(new Source(name, path, content));
        }

        // copy images
        path = "doc/images";

        name = "add_mosn_src.png";
        final String srcPath = "/META-INF/template.standard/images/add_mosn_src.png";
        byte[] contentBytes = plainBytes(srcPath);
        if (contentBytes != null) {
            sources.add(new Source(name, path, contentBytes));
        }

        name = "debug-mosn.png";
        final String debugPath = "/META-INF/template.standard/images/debug-mosn.png";
        contentBytes = plainBytes(debugPath);
        if (contentBytes != null) {
            sources.add(new Source(name, path, contentBytes));
        }

        name = "remote_debug.png";
        final String remotePath = "/META-INF/template.standard/images/remote_debug.png";
        contentBytes = plainBytes(remotePath);
        if (contentBytes != null) {
            sources.add(new Source(name, path, contentBytes));
        }

        return sources;
    }

}
