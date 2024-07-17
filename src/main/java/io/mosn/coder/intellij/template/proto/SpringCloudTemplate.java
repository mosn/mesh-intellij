package io.mosn.coder.intellij.template.proto;

import io.mosn.coder.intellij.option.Configuration;
import io.mosn.coder.intellij.option.Metadata;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.List;

public class SpringCloudTemplate extends AbstractProtocolTemplate<ProtocolOption> {

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
        String path = "plugins/codecs/springcloud/main";

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.append("package main");

        return new Source(name, path, buffer.toString());
    }

    @Override
    public List<Configuration> configurations(ProtocolOption option) {

        final String path = "configs/codecs/springcloud";

        final String egressName = "egress_springcloud.json";
        final String egressPath = "/META-INF/template.springcloud/configs/egress_springcloud.json.template";

        final String ingressName = "ingress_springcloud.json";
        final String ingressPath = "/META-INF/template.springcloud/configs/ingress_springcloud.json.template";

        return createConfigurations(path, egressName, egressPath, ingressName, ingressPath);
    }

    @Override
    public Metadata metadata(ProtocolOption option) {
        String name = "metadata.json";
        String path = "configs/codecs/springcloud";

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("{")
                .append("\t\"name\": \"springcloud\",")
                .append("\t\"kind\": \"protocol\",")
                .append("\t\"framework\": \"HTTP1\",")
                .append("\t\"internal\": false,")
                .append("\t\"variables\": [{")
                .append("\t\t\t\"field\": \"x-mosn-data-id\",")
                .append("\t\t\t\"pattern\": [\"${X-TARGET-APP}@springcloud\"],")
                .append("\t\t\t\"required\": true")
                .append("\t\t},")
                .append("\t\t{")
                .append("\t\t\t\"field\": \"x-mosn-method\",")
                .append("\t\t\t\"pattern\": [\"${x-mosn-method}\"],")
                .append("\t\t\t\"required\": false")
                .append("\t\t},")
                .append("\t\t{")
                .append("\t\t\t\"field\": \"x-mosn-caller-app\",")
                .append("\t\t\t\"pattern\": [\"${X-CALLER-APP}\"],")
                .append("\t\t\t\"required\": false")
                .append("\t\t},")
                .append("\t\t{")
                .append("\t\t\t\"field\": \"x-mosn-target-app\",")
                .append("\t\t\t\"pattern\": [\"${X-TARGET-APP}\"],")
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
