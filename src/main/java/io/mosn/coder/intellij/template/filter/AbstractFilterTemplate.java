package io.mosn.coder.intellij.template.filter;

import io.mosn.coder.intellij.option.Configuration;
import io.mosn.coder.intellij.option.FilterOption;
import io.mosn.coder.intellij.option.Metadata;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.template.AbstractCodeTemplate;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFilterTemplate extends AbstractCodeTemplate<FilterOption> {

    public abstract List<Source> filter(FilterOption option);

    public abstract List<Configuration> configurations(FilterOption option);

    public abstract Metadata metadata(FilterOption option);

    @Override
    public List<Source> create(FilterOption option) {
        ArrayList<Source> source = new ArrayList<>();

        // create api code
        List<Source> filter = filter(option);
        if (filter != null) {
            source.addAll(filter);
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

}
