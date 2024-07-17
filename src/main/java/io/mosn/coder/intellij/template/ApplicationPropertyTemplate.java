package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class ApplicationPropertyTemplate implements Template {

    public static final String Name = "application.properties";
    public static final String Path = "";

    @Override
    public List<Source> create(PluginOption option) {

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append2("# plugin upgrade configuration")
                .append("# upgrade image")
                .append2("plugin.remote.sidecar.image=")
                .append("# auto compile filter when build codec")
                .append("# example: codec.bolt.filter=logger,auth")
                .append2("# codec.${protocol}.filter=")
                .append("# auto compile transcoder when build codec")
                .append("# example: codec.bolt.transcoder=bolt2sp")
                .append2("# codec.${protocol}.transcoder=")
                .line()
                .append("# direct mesh server address, format: ip:port")
                .append("# mesh.server.address=")
                .line()
                .append("# switch on or off arm transform to amd")
                .append("# plugin.arm.transform.amd=off");


        return Arrays.asList(new Source(Name, Path, buffer.toString()));
    }
}
