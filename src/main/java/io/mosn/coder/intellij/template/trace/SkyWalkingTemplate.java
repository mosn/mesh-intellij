package io.mosn.coder.intellij.template.trace;

import io.mosn.coder.intellij.option.Configuration;
import io.mosn.coder.intellij.option.Metadata;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.option.TraceOption;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

public class SkyWalkingTemplate extends AbstractTraceTemplate<TraceOption> {

    String path = "plugins/traces/";

    @Override
    public Source config(TraceOption option) {
        String name = "config.go";

        String content = plainText("/META-INF/template.skywalking/config.go.template");
        if (content != null) {
            return new Source(name, path + option.getPluginName() + "/main", content);
        }

        LOG.warning("trace config.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source driver(TraceOption option) {
        String name = "driver.go";
        String content = plainText("/META-INF/template.skywalking/driver.go.template");
        if (content != null) {

            content = content.replace("${go_module_name}", option.context().getModule());

            return new Source(name, path + option.getPluginName() + "/main", content);
        }

        LOG.warning("trace driver.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source logger(TraceOption option) {
        String name = "logger.go";
        String content = plainText("/META-INF/template.skywalking/logger.go.template");
        if (content != null) {
            return new Source(name, path + option.getPluginName() + "/main", content);
        }

        LOG.warning("trace logger.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source span(TraceOption option) {
        String name = "span.go";
        String content = plainText("/META-INF/template.skywalking/span.go.template");
        if (content != null) {

            content = content.replaceAll("\\$\\{go_module_name}", option.context().getModule());

            return new Source(name, path + option.getPluginName() + "/main", content);
        }

        LOG.warning("trace span.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source tracer(TraceOption option) {
        String name = "tracer.go";
        String content = plainText("/META-INF/template.skywalking/tracer.go.template");
        if (content != null) {

            content = content.replaceAll("\\$\\{go_module_name}", option.context().getModule());

            return new Source(name, path + option.getPluginName() + "/main", content);
        }

        LOG.warning("trace tracer.go not generate correctly, maybe bug triggered.");

        return null;
    }

    public List<Configuration> configurations(TraceOption option) {

        String name = "config.json";
        final String path = "configs/traces/" + option.getPluginName();

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("{")
                .with("\"reporter\": \"").with(option.getReporterType()).append("\",")
                .append("    \"plugin_type\": \"skywalking\",")
                .append("    \"backend_service\": \"${SKY_WALKING_ADDRESS}\",")
                .append("    \"service_name\": \"${APPNAME}\",")
                .append("    \"max_send_queue_size\":\"${MAX_SEND_QUEUE_SIZE}\",")
                .append("    \"mosn_generator_span_enabled\" :\"${MOSN_GENERATOR_SAPN_ENABLED}\",")
                .append("    \"vmmode\":\"${VMMODE}\",")
                .append("    \"pod_name\":\"${POD_NAME}\"")
                .append("}");

        return Arrays.asList(new Configuration(name, path, buffer.toString()));

    }

    public Metadata metadata(TraceOption option) {
        String name = "metadata.json";
        String path = "configs/traces/" + option.getPluginName();

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("{")
                .with("    \"name\" : \"").with(option.getPluginName()).append("\",")
                .append("    \"kind\": \"trace\",    ")
                .append("\t\"dependencies\": [{")
                .with("\t\t\"mosn_api\": \"").with(option.getApi()).append("\",")
                .with("\t\t\"mosn_pkg\": \"").with(option.getPkg()).append("\"")
                .append("\t}]")
                .append("}");

        return new Metadata(name, path, buffer.toString());

    }

    @Override
    public Source docker(TraceOption option) {

        String name = "docker-compose.yaml";
        String path = "etc/docker-compose/traces/";


        String content = plainText("/META-INF/template.skywalking/docker.yaml.template");
        if (content != null) {
            return new Source(name, path + option.getPluginName(), content);
        }

        LOG.warning("trace docker compose not generate correctly, maybe bug triggered.");

        return null;
    }

}
