package io.mosn.coder.intellij.template.proto;

import io.mosn.coder.intellij.option.Configuration;
import io.mosn.coder.intellij.option.Metadata;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.ArrayList;
import java.util.List;

public class HttpTemplate extends AbstractProtocolTemplate<ProtocolOption> {

    @Override
    public Source api(ProtocolOption option) {
        return null;
    }

    @Override
    public Source command(ProtocolOption option) {
        return null;
    }

    @Override
    public Source decoder(ProtocolOption option) {
        return null;
    }

    @Override
    public Source encoder(ProtocolOption option) {
        return null;
    }

    @Override
    public Source mapping(ProtocolOption option) {
        return null;
    }

    @Override
    public Source matcher(ProtocolOption option) {
        return null;
    }

    @Override
    public Source protocol(ProtocolOption option) {
        return null;
    }

    @Override
    public Source types(ProtocolOption option) {
        return null;
    }

    @Override
    public Source buffer(ProtocolOption option) {
        return null;
    }

    @Override
    public Source codec(ProtocolOption option) {
        String name = "codec.go";
        String proto = option.getPluginName().toLowerCase();
        String path = "plugins/codecs/" + proto + "/main";

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.append("package main");

        return new Source(name, path, buffer.toString());
    }

    @Override
    public List<Configuration> configurations(ProtocolOption option) {
        ArrayList<Configuration> configurations = new ArrayList<>();

        String proto = option.getPluginName().toLowerCase();

        final String path = "configs/codecs/" + proto;

        final String egressName = "egress_" + proto + ".json";
        final String egressPath = "/META-INF/template.standard/configs/egress_http.json.template";

        final String ingressName = "ingress_" + proto + ".json";
        final String ingressPath = "/META-INF/template.standard/configs/ingress_http.json.template";

        return appendConfigurations(option, configurations, proto, path, egressName, egressPath, ingressName, ingressPath);
    }

    @Override
    public Metadata metadata(ProtocolOption option) {
        String proto = option.getPluginName().toLowerCase();

        String name = "metadata.json";
        String path = "configs/codecs/" + proto;

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("{")
                .with("\t\"name\": \"").with(proto).append("\",")
                .append("\t\"kind\": \"protocol\",")
                .append("\t\"framework\": \"HTTP1\",")
                .append("\t\"internal\": false,")
                .append("\t\"variables\": [");

        appendVariables(proto, option, buffer);

        buffer.append("\t],")
                .append("\t\"dependencies\": [{")
                .with("\t\t\"mosn_api\": \"").with(option.getApi()).append("\",")
                .with("\t\t\"mosn_pkg\": \"").with(option.getPkg()).append("\"")
                .append("\t}]")
                .append("}");

        return new Metadata(name, path, buffer.toString());
    }
}
