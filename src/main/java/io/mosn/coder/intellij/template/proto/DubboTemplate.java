package io.mosn.coder.intellij.template.proto;

import io.mosn.coder.intellij.option.Configuration;
import io.mosn.coder.intellij.option.Metadata;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.List;

/**
 * @author yiji@apache.org
 */
public class DubboTemplate extends AbstractProtocolTemplate<ProtocolOption> {

    String path = "pkg/protocol/dubbo";

    @Override
    public Source api(ProtocolOption option) {

        String name = "api.go";

        String content = plainText("/META-INF/template.dubbo/api.go.template");
        if (content != null) {
            return new Source(name, path, content);
        }

        LOG.warning("dubbo api.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source command(ProtocolOption option) {

        String name = "command.go";

        String content = plainText("/META-INF/template.dubbo/command.go.template");
        if (content != null) {

            content = content.replace("${go_module_name}", option.context().getModule());

            return new Source(name, path, content);
        }

        LOG.warning("dubbo command.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source decoder(ProtocolOption option) {

        String name = "decoder.go";

        String content = plainText("/META-INF/template.dubbo/decoder.go.template");
        if (content != null) {

            content = content.replace("${go_module_name}", option.context().getModule());

            return new Source(name, path, content);
        }

        LOG.warning("dubbo decoder.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source encoder(ProtocolOption option) {

        String name = "encoder.go";

        String content = plainText("/META-INF/template.dubbo/encoder.go.template");
        if (content != null) {
            return new Source(name, path, content);
        }

        LOG.warning("dubbo encoder.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source mapping(ProtocolOption option) {

        String name = "mapping.go";

        String content = plainText("/META-INF/template.dubbo/mapping.go.template");
        if (content != null) {
            return new Source(name, path, content);
        }

        LOG.warning("dubbo mapping.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source matcher(ProtocolOption option) {

        final String name = "matcher.go";

        String content = plainText("/META-INF/template.dubbo/matcher.go.template");
        if (content != null) {
            return new Source(name, path, content);
        }

        LOG.warning("dubbo matcher.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source protocol(ProtocolOption option) {

        final String name = "protocol.go";

        String content = plainText("/META-INF/template.dubbo/protocol.go.template");
        if (content != null) {
            return new Source(name, path, content);
        }

        LOG.warning("dubbo protocol.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source types(ProtocolOption option) {

        final String name = "types.go";

        String content = plainText("/META-INF/template.dubbo/types.go.template");
        if (content != null) {
            return new Source(name, path, content);
        }

        LOG.warning("dubbo types.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source buffer(ProtocolOption option) {
        return null;
    }

    @Override
    public Source codec(ProtocolOption option) {
        String name = "codec.go";
        String path = "plugins/codecs/dubbo/main";

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.append("package main")
                .line()
                .append("import (")
                .append("\t\"context\"")
                .line()
                .append("\t\"mosn.io/api\"")
                .with("\t\"").with(option.context().getModule()).append("/pkg/protocol/dubbo\"")
                .append(")")
                .line()
                .append("// LoadCodec load codec function")
                .append("func LoadCodec() api.XProtocolCodec {")
                .append("	return &Codec{}")
                .append("}")
                .line()
                .append("type Codec struct {")
                .append("	HttpStatusMapping dubbo.StatusMapping")
                .append("}")
                .line()
                .append("func (r Codec) ProtocolName() api.ProtocolName {")
                .append("	return dubbo.ProtocolName")
                .append("}")
                .line()
                .append("func (r Codec) ProtocolMatch() api.ProtocolMatch {")
                .append("	return dubbo.Matcher")
                .append("}")
                .line()
                .append("func (r Codec) HTTPMapping() api.HTTPMapping {")
                .append("	return r.HttpStatusMapping")
                .append("}")
                .line()
                .append("func (r Codec) NewXProtocol(context.Context) api.XProtocol {")
                .append("	return dubbo.DubboProtocol{}")
                .append("}")
                .line()
                .append("// compiler check")
                .append("var _ api.XProtocolCodec = &Codec{}");

        return new Source(name, path, buffer.toString());
    }

    @Override
    public List<Configuration> configurations(ProtocolOption option) {

        final String path = "configs/codecs/dubbo";

        final String egressName = "egress_dubbo.json";
        final String egressPath = "/META-INF/template.dubbo/configs/egress_dubbo.json.template";

        final String ingressName = "ingress_dubbo.json";
        final String ingressPath = "/META-INF/template.dubbo/configs/ingress_dubbo.json.template";

        return createConfigurations(path, egressName, egressPath, ingressName, ingressPath);
    }

    @Override
    public Metadata metadata(ProtocolOption option) {
        String name = "metadata.json";
        String path = "configs/codecs/dubbo";

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("{")
                .append("\t\"name\": \"dubbo\",")
                .append("\t\"kind\": \"protocol\",")
                .append("\t\"framework\": \"X\",")
                .append("\t\"internal\": false,")
                .append("\t\"variables\": [{")
                .append("\t\t\t\"field\": \"x-mosn-data-id\",")
                .append("\t\t\t\"pattern\": [\"${service}[:${version}][:${group}]@dubbo\"],")
                .append("\t\t\t\"required\": true")
                .append("\t\t},")
                .append("\t\t{")
                .append("\t\t\t\"field\": \"x-mosn-method\",")
                .append("\t\t\t\"pattern\": [\"${method}\"],")
                .append("\t\t\t\"required\": false")
                .append("\t\t},")
                .append("\t\t{")
                .append("\t\t\t\"field\": \"x-mosn-caller-app\",")
                .append("\t\t\t\"pattern\": [\"${X-CALLER-APP}\"],")
                .append("\t\t\t\"required\": false")
                .append("\t\t}")
                .append("\t],")
                .append("\t\"dependencies\": [{")
                .with("\t\t\"mosn_api\": \"").with(option.getApi()).append("\",")
                .with("\t\t\"mosn_pkg\": \"").with(option.getPkg()).append("\"")
                .append("\t}]")
                .append("}");

        return new Metadata(name, path, buffer.toString());

    }
}
