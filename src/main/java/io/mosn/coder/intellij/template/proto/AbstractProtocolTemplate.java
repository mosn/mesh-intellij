package io.mosn.coder.intellij.template.proto;


import io.mosn.coder.intellij.option.*;
import io.mosn.coder.intellij.template.AbstractCodeTemplate;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.mosn.coder.intellij.option.AbstractOption.X_MOSN_DATA_ID;

/**
 * @author yiji@apache.org
 */
public abstract class AbstractProtocolTemplate<T extends PluginOption> extends AbstractCodeTemplate<T> {

    public abstract Source api(T option);

    public abstract Source command(T option);

    public abstract Source decoder(T option);

    public abstract Source encoder(T option);

    public abstract Source mapping(T option);

    public abstract Source matcher(T option);

    public abstract Source protocol(T option);

    public abstract Source types(T option);

    public abstract Source buffer(T option);

    public abstract Source codec(T option);

    public abstract List<Configuration> configurations(T option);

    public abstract Metadata metadata(T option);

    @Override
    public List<Source> create(T option) {
        ArrayList<Source> source = new ArrayList<>();

        // create api code
        Source api = api(option);
        if (api != null) {
            source.add(api);
        }

        // create command code
        Source command = command(option);
        if (command != null) {
            source.add(command);
        }

        // create decoder
        Source decoder = decoder(option);
        if (decoder != null) {
            source.add(decoder);
        }

        // create encoder
        Source encoder = encoder(option);
        if (encoder != null) {
            source.add(encoder);
        }

        // create mapping
        Source mapping = mapping(option);
        if (mapping != null) {
            source.add(mapping);
        }

        // create matcher
        Source matcher = matcher(option);
        if (matcher != null) {
            source.add(matcher);
        }

        // create protocol
        Source protocol = protocol(option);
        if (protocol != null) {
            source.add(protocol);
        }

        // create types
        Source types = types(option);
        if (types != null) {
            source.add(types);
        }

        // create buffer
        Source buffer = buffer(option);
        if (buffer != null) {
            source.add(buffer);
        }

        // plugin startup code
        Source codec = codec(option);
        if (codec != null) {
            source.add(codec);
        }

        // append configuration
        List<Configuration> configurations = configurations(option);
        if (configurations != null && !configurations.isEmpty()) {
            source.addAll(configurations);
        }

        // append metadata
        Metadata metadata = metadata(option);
        if (metadata != null) {
            source.add(metadata);
        }

        return source;
    }

    protected ArrayList<Configuration> createConfigurations(String path
            , String egressName
            , String egressPath
            , String ingressName
            , String ingressPath) {
        ArrayList<Configuration> configurations = new ArrayList<>();

        String content = plainText(egressPath);
        if (content != null) {
            configurations.add(new Configuration(egressName, path, content));
        } else {
            LOG.warning(egressName + " not generate correctly, maybe bug triggered.");
        }

        content = plainText(ingressPath);
        if (content != null) {
            configurations.add(new Configuration(ingressName, path, content));
        } else {
            LOG.warning(ingressName + " not generate correctly, maybe bug triggered.");
        }

        return configurations;
    }

    protected void appendVariables(String proto, ProtocolOption opt, CodeBuilder buffer) {

        int n = 0;

        // append required keys
        if (opt.getRequiredKeys() != null && !opt.getRequiredKeys().isEmpty()) {
            for (Map.Entry<AbstractOption.OptionKey<String>, AbstractOption.OptionValue<String>> key : opt.getRequiredKeys()) {

                AbstractOption.OptionValue<String> optionValue = key.getValue();
                // skip invalid variable
                if (optionValue == null
                        || optionValue.getItems() == null
                        || optionValue.getItems().isEmpty()) {
                    continue;
                }

                // append ,
                if (n++ > 0) {
                    buffer.with(",");
                }

                if (key.getKey().equals(X_MOSN_DATA_ID)) {
                    buffer.append("\t\t{")
                            .append("\t\t\t\"field\": \"x-mosn-data-id\",")
                            .with("\t\t\t\"pattern\": [");

                    int i = 0;
                    for (String headKey : optionValue.getItems()) {
                        if (!headKey.endsWith(proto) && !headKey.contains("@")) {
                            if (i++ > 0) {
                                buffer.with(",");
                            }

                            buffer.with("\"");
                            if (headKey.startsWith("${")) {
                                buffer.with(headKey).with("@").with(proto);
                            } else {
                                buffer.with("${").with(headKey).with("}").with("@").with(proto);
                            }
                            buffer.with("\"");
                        } else {
                            // ${xxx}@proto format
                            if (i++ > 0) {
                                buffer.with(",");
                            }
                            buffer.with("\"").with(headKey).with("\"");
                        }
                    }

                    buffer.append("],")
                            .append("\t\t\t\"required\": true")
                            .with("\t\t}");

                } else {
                    appendVariable(buffer, key, true);
                }
            }
        }

        // append optional keys
        if (opt.getOptionalKeys() != null && !opt.getOptionalKeys().isEmpty()) {
            for (Map.Entry<AbstractOption.OptionKey<String>, AbstractOption.OptionValue<String>> key : opt.getOptionalKeys()) {
                AbstractOption.OptionValue<String> optionValue = key.getValue();

                // skip invalid variable
                if (optionValue == null
                        || optionValue.getItems() == null
                        || optionValue.getItems().isEmpty()) {
                    continue;
                }

                // append ,
                if (n > 0) {
                    buffer.append(",");
                }

                appendVariable(buffer, key, false);
            }
        }
    }

    protected void appendVariable(CodeBuilder buffer
            , Map.Entry<AbstractOption.OptionKey<String>, AbstractOption.OptionValue<String>> key
            , boolean required) {
        buffer.append("\t\t{")
                .with("\t\t\t\"field\": \"").with(key.getKey().getName()).append("\",")
                .with("\t\t\t\"pattern\": [");
        AbstractOption.OptionValue<String> optionValue = key.getValue();
        if (optionValue != null
                && optionValue.getItems() != null
                && !optionValue.getItems().isEmpty()) {

            int i = 0;
            for (String headKey : optionValue.getItems()) {
                if (i++ > 0) {
                    buffer.with(",");
                }

                buffer.with("\"");
                if (headKey.startsWith("${")) {
                    buffer.with(headKey);
                } else {
                    buffer.with("${").with(headKey).with("}");
                }
                buffer.with("\"");
            }
        }

        buffer.append("],")
                .with("\t\t\t\"required\": ").append(required ? "true" : "false")
                .with("\t\t}");
    }

    protected ArrayList<Configuration> appendConfigurations(PluginOption option, ArrayList<Configuration> configurations, String proto, String path, String egressName, String egressPath, String ingressName, String ingressPath) {
        if (option instanceof ProtocolOption) {
            ProtocolOption opt = (ProtocolOption) option;
            if (opt.getClientPort() != null && opt.getClientPort() > 0) {

                String content = plainText(egressPath);
                if (content != null) {

                    content = content.replaceAll("\\$\\{go_protocol_name}", proto);
                    content = content.replace("${go_client_port}", opt.getClientPort().toString());

                    configurations.add(new Configuration(egressName, path, content));
                } else {
                    LOG.warning(egressName + " not generate correctly, maybe bug triggered.");
                }
            }

            if (opt.getServerPort() != null && opt.getServerPort() > 0) {

                String content = plainText(ingressPath);
                if (content != null) {

                    content = content.replaceAll("\\$\\{go_protocol_name}", proto);
                    content = content.replace("${go_server_port}", opt.getServerPort().toString());

                    configurations.add(new Configuration(ingressName, path, content));
                } else {
                    LOG.warning(ingressName + " not generate correctly, maybe bug triggered.");
                }
            }
        }

        return configurations;
    }
}
