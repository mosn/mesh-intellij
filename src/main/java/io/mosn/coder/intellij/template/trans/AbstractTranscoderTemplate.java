package io.mosn.coder.intellij.template.trans;

import io.mosn.coder.intellij.option.Configuration;
import io.mosn.coder.intellij.option.Metadata;
import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.template.AbstractCodeTemplate;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTranscoderTemplate extends AbstractCodeTemplate {

    public abstract Source config(PluginOption option);

    public abstract List<Source> transcoder(PluginOption option);

    public abstract List<Configuration> configurations(PluginOption option);

    public abstract Metadata metadata(PluginOption option);

    @Override
    public List<Source> create(PluginOption option) {
        ArrayList<Source> source = new ArrayList<>();

        // create api code
        List<Source> transcoder = transcoder(option);
        if (transcoder != null) {
            source.addAll(transcoder);
        }

        // append configuration
        List<Configuration> configurations = configurations(option);
        if (configurations != null && !configurations.isEmpty()) {
            source.addAll(configurations);
        }

        Source config = config(option);
        if (config != null) {
            source.add(config);
        }

        // append metadata
        Metadata metadata = metadata(option);
        if (metadata != null) {
            source.add(metadata);
        }

        return source;
    }

}
