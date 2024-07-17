package io.mosn.coder.intellij.template.trace;

import io.mosn.coder.intellij.option.*;
import io.mosn.coder.intellij.template.AbstractCodeTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public abstract class AbstractTraceTemplate<T extends PluginOption> extends AbstractCodeTemplate<T> {

    public abstract Source config(T option);

    public abstract Source driver(T option);

    public abstract Source logger(T option);

    public abstract Source span(T option);

    public abstract Source tracer(T option);

    public abstract List<Configuration> configurations(T option);

    public abstract Metadata metadata(T option);

    public abstract Source docker(T option);

    @Override
    public List<Source> create(T option) {
        ArrayList<Source> source = new ArrayList<>();

        // create noopSpan code
        Source noopSpan = noopSpan(option);
        if (noopSpan != null) {
            source.add(noopSpan);
        }

        // create config code
        Source config = config(option);
        if (config != null) {
            source.add(config);
        }

        // create driver code
        Source driver = driver(option);
        if (driver != null) {
            source.add(driver);
        }

        // create tracer log code
        Source logger = logger(option);
        if (logger != null) {
            source.add(logger);
        }

        // create span code
        Source span = span(option);
        if (span != null) {
            source.add(span);
        }

        // create tracer code
        Source tracer = tracer(option);
        if (tracer != null) {
            source.add(tracer);
        }

        List<Configuration> configurations = configurations(option);
        if (configurations != null) {
            source.addAll(configurations);
        }

        Source metadata = metadata(option);
        if (metadata != null) {
            source.add(metadata);
        }

        Source docker = docker(option);
        if (docker != null){
            source.add(docker);
        }

        return source;
    }

    protected Source noopSpan(T option) {
        String name = "span.go";
        String path = "pkg/trace";

        String content = plainText("/META-INF/template.skywalking/noopspan.go.template");
        if (content != null) {
            return new Source(name, path, content);
        }

        LOG.warning("trace noop span.go not generate correctly, maybe bug triggered.");

        return null;
    }
}
