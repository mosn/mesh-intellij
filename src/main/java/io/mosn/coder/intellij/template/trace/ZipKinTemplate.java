package io.mosn.coder.intellij.template.trace;

import io.mosn.coder.intellij.option.Configuration;
import io.mosn.coder.intellij.option.Metadata;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.option.TraceOption;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

public class ZipKinTemplate extends AbstractTraceTemplate<TraceOption> {

    String path = "plugins/traces/";

    @Override
    public Source config(TraceOption option) {
        String name = "config.go";

        String content = plainText("/META-INF/template.zipkin/config.go.template");
        if (content != null) {
            return new Source(name, path + option.getPluginName() + "/main", content);
        }

        LOG.warning("trace config.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source driver(TraceOption option) {
        String name = "driver.go";
        String content = plainText("/META-INF/template.zipkin/driver.go.template");
        if (content != null) {

            content = content.replace("${go_module_name}", option.context().getModule());

            return new Source(name, path + option.getPluginName() + "/main", content);
        }

        LOG.warning("trace driver.go not generate correctly, maybe bug triggered.");

        return null;
    }

    @Override
    public Source span(TraceOption option) {
        String name = "span.go";
        String content = plainText("/META-INF/template.zipkin/span.go.template");
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
        String content = plainText("/META-INF/template.zipkin/tracer.go.template");
        if (content != null) {

            content = content.replaceAll("\\$\\{go_module_name}", option.context().getModule());

            return new Source(name, path + option.getPluginName() + "/main", content);
        }

        LOG.warning("trace tracer.go not generate correctly, maybe bug triggered.");

        return null;
    }

    /**
     * reused for reporter
     */
    @Override
    public Source logger(TraceOption option) {
        String name = "reporter_factory.go";
        String content = plainText("/META-INF/template.zipkin/reporter.go.template");
        if (content != null) {
            return new Source(name, path + option.getPluginName() + "/main", content);
        }

        LOG.warning("trace reporter_factory.go not generate correctly, maybe bug triggered.");

        return null;
    }

    public List<Configuration> configurations(TraceOption option) {

        String name = "config.json";
        final String path = "configs/traces/" + option.getPluginName();

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("{")
                .with("\"reporter\": \"").with(option.getReporterType()).append("\",")
                .append("    \"plugin_type\": \"zipkin\",")
                .append2("  \"service_name\": \"${APPNAME}\",")
                .append("    \"mosn_generator_span_enabled\" :\"${MOSN_GENERATOR_SAPN_ENABLED}\",")
                .append("  \"http\": {")
                .append("    \"batch_size\": \"${BATCH_SIZE}\",")
                .append("    \"address\": \"${ZIPKIN_ADDRESS}\"")
                .append("  },")
                .append("  \"kafka\": {")
                .append("    \"topic\": \"${KAFKA_TOPIC}\",")
                .append("    \"address\": \"${ZIPKIN_ADDRESS}\"")
                .append("  },")
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


        String content = plainText("/META-INF/template.zipkin/docker.yaml.template");
        if (content != null) {
            return new Source(name, path + option.getPluginName(), content);
        }

        LOG.warning("trace docker compose not generate correctly, maybe bug triggered.");

        return null;
    }

}
