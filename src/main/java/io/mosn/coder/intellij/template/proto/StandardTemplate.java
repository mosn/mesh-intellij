package io.mosn.coder.intellij.template.proto;

import io.mosn.coder.intellij.option.*;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.ArrayList;
import java.util.List;

public class StandardTemplate extends AbstractProtocolTemplate<ProtocolOption> {

    String path = "pkg/protocol/";

    @Override
    public Source api(ProtocolOption option) {
        return null;
    }

    @Override
    public Source command(ProtocolOption option) {

        String name = "command.go";
        String proto = option.getPluginName().toLowerCase();

        String content = plainText("/META-INF/template.standard/command.go.template");
        if (content != null) {

            content = content.replace("${go_package_name}", proto);
            content = content.replace("${go_module_name}", option.context().getModule());

            if (option.getPoolMode() == PoolMode.PingPong) {
                content = content.replaceAll("\\$\\{go_getRequestID}", "0");
                content = content.replaceAll("\\$\\{go_setRequestID}", "");
            } else {
                content = content.replaceAll("\\$\\{go_getRequestID}", "uint64(r.RequestId)");
                content = content.replaceAll("\\$\\{go_setRequestID}", "r.RequestId = uint32(id)");
            }

            return new Source(name, path + proto, content);
        }

        LOG.warning(proto + " command.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source decoder(ProtocolOption option) {

        String name = "decoder.go";
        String proto = option.getPluginName().toLowerCase();

        String content = plainText("/META-INF/template.standard/decoder.go.template");
        if (content != null) {

            content = content.replace("${go_package_name}", proto);

            return new Source(name, path + proto, content);
        }

        LOG.warning(proto + " decoder.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source encoder(ProtocolOption option) {

        String name = "encoder.go";
        String proto = option.getPluginName().toLowerCase();

        String content = plainText("/META-INF/template.standard/encoder.go.template");
        if (content != null) {

            content = content.replace("${go_package_name}", proto);

            return new Source(name, path + proto, content);
        }

        LOG.warning(proto + " encoder.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source mapping(ProtocolOption option) {

        String name = "mapping.go";
        String proto = option.getPluginName().toLowerCase();

        String content = plainText("/META-INF/template.standard/mapping.go.template");
        if (content != null) {

            content = content.replace("${go_package_name}", proto);

            return new Source(name, path + proto, content);
        }

        LOG.warning(proto + " mapping.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source matcher(ProtocolOption option) {

        String name = "matcher.go";
        String proto = option.getPluginName().toLowerCase();

        String content = plainText("/META-INF/template.standard/matcher.go.template");
        if (content != null) {

            content = content.replace("${go_package_name}", proto);

            return new Source(name, path + proto, content);
        }

        LOG.warning(proto + " matcher.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source protocol(ProtocolOption option) {

        String name = "protocol.go";
        String proto = option.getPluginName().toLowerCase();

        String content = plainText("/META-INF/template.standard/protocol.go.template");
        if (content != null) {

            content = content.replace("${go_package_name}", proto);

            String p = option.Alias();

            content = content.replaceAll("\\$\\{go_protocol_name}", p);

            if (option.getPoolMode() == PoolMode.PingPong) {
                content = content.replace("${go_poolMode}", "api.PingPong");
                content = content.replace("${go_enablePool}", "false");
                content = content.replace("${go_generateRequestID}", "0");
            } else {
                content = content.replace("${go_poolMode}", "api.Multiplex");
                content = content.replace("${go_enablePool}", "true");
                content = content.replace("${go_generateRequestID}", "atomic.AddUint64(streamID, 1)");
            }

            return new Source(name, path + proto, content);
        }

        LOG.warning(proto + " protocol.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source types(ProtocolOption option) {

        String name = "types.go";
        String proto = option.getPluginName().toLowerCase();

        String content = plainText("/META-INF/template.standard/types.go.template");
        if (content != null) {

            content = content.replaceAll("\\$\\{go_package_name}", proto);

            return new Source(name, path + proto, content);
        }

        LOG.warning(proto + " types.go not generate correctly, maybe bug triggered.");

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

        String p = proto;
        // fist char should be start with 'A-Z'
        if (p.length() == 1) {
            p = proto.toUpperCase();
        } else if (p.length() >= 1) {
            p = p.substring(0, 1).toUpperCase() + p.substring(1);
        }

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.append("package main")
                .line()
                .append("import (")
                .append("\t\"context\"")
                .line()
                .append("\t\"mosn.io/api\"")
                .with("\t\"").with(option.context().getModule()).with("/pkg/protocol/").with(proto).append("\"")
                .append(")")
                .line()
                .append("// LoadCodec load codec function")
                .append("func LoadCodec() api.XProtocolCodec {")
                .append("	return &Codec{}")
                .append("}")
                .line()
                .append("type Codec struct {")
                .with("	HttpStatusMapping ").with(proto).append(".StatusMapping")
                .append("}")
                .line()
                .append("func (r Codec) ProtocolName() api.ProtocolName {")
                .with("	return ").with(proto).append(".ProtocolName")
                .append("}")
                .line()
                .append("func (r Codec) ProtocolMatch() api.ProtocolMatch {")
                .with("	return ").with(proto).append(".Matcher")
                .append("}")
                .line()
                .append("func (r Codec) HTTPMapping() api.HTTPMapping {")
                .append("	return r.HttpStatusMapping")
                .append("}")
                .line()
                .append("func (r Codec) NewXProtocol(context.Context) api.XProtocol {")
                .with("	return &").with(proto).with(".").with(p).append("Protocol{}")
                .append("}")
                .line()
                .append("// compiler check")
                .append("var _ api.XProtocolCodec = &Codec{}");

        return new Source(name, path, buffer.toString());
    }

    @Override
    public List<Configuration> configurations(ProtocolOption option) {
        ArrayList<Configuration> configurations = new ArrayList<>();

        String proto = option.getPluginName().toLowerCase();

        final String path = "configs/codecs/" + proto;

        final String egressName = "egress_" + proto + ".json";
        final String egressPath = "/META-INF/template.standard/configs/egress_standard.json.template";

        final String ingressName = "ingress_" + proto + ".json";
        final String ingressPath = "/META-INF/template.standard/configs/ingress_standard.json.template";

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
                .append("\t\"framework\": \"X\",")
                .append("\t\"internal\": false,")
                .append("\t\"variables\": [");

        appendVariables(proto, option, buffer);

        buffer.append("],") // end variables
                .append("\t\"dependencies\": [{")
                .with("\t\t\"mosn_api\": \"").with(option.getApi()).append("\",")
                .with("\t\t\"mosn_pkg\": \"").with(option.getPkg()).append("\"")
                .append("\t}]")
                .append("}");

        return new Metadata(name, path, buffer.toString());
    }
}
