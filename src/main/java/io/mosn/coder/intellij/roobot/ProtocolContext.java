package io.mosn.coder.intellij.roobot;

import com.intellij.openapi.vfs.VirtualFile;
import io.mosn.coder.intellij.internal.BoltOptionImpl;
import io.mosn.coder.intellij.internal.DubboOptionImpl;
import io.mosn.coder.intellij.internal.SpringCloudOptionImpl;
import io.mosn.coder.intellij.option.*;
import io.mosn.coder.intellij.template.Template;
import io.mosn.coder.intellij.template.proto.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.mosn.coder.intellij.option.AbstractOption.X_MOSN_DATA_ID;

/**
 * @author yiji@apache.org
 */
public class ProtocolContext extends AbstractContext {

    public ProtocolContext(PluginOption option, File dir) {
        super(option, dir);
    }

    public ProtocolContext(ProtocolOption option, VirtualFile dir) {
        super(option, dir);
    }

    @Override
    public void createTemplateCode() {
        Template template = null;
        if (option != null) {

            ProtocolOption opt = ((ProtocolOption) option);

            // create internal protocol
            if (opt.getEmbedded() != null
                    && !opt.getEmbedded().isEmpty()) {

                List<Source> internal = new ArrayList<>();
                for (ProtocolOption proto : opt.getEmbedded()) {
                    if (proto.context() == null) {
                        proto.setContext(this);
                    }

                    if (proto instanceof BoltOptionImpl) {
                        template = new BoltTemplate();
                    } else if (proto instanceof DubboOptionImpl) {
                        template = new DubboTemplate();
                    } else if (proto instanceof SpringCloudOptionImpl) {
                        template = new SpringCloudTemplate();
                    }

                    if (template != null) {
                        /**
                         * create code、configuration and metadata
                         */
                        List<Source> code = template.create(proto);
                        internal.addAll(code);
                    }
                }

                flush(internal);
            }

            injectHttpHeadIfRequired(opt);

            // standard code template
            template = opt.isHttp() ? new HttpTemplate() : new ProtoTemplate();
            List<Source> code = template.create(option);
            flush(code);
        }
    }

    private void injectHttpHeadIfRequired(ProtocolOption opt) {
        // inject header ?
        if (opt.isHttp() && opt.isInjectHead()) {

            FilterOption filterOption = new FilterOption();
            filterOption.setActiveMode(AbstractOption.ActiveMode.ALL);
            filterOption.setBefore("govern_config");
            filterOption.setPluginName(opt.getPluginName());

            // inject head key
            if (opt.getRequiredKeys() != null && !opt.getRequiredKeys().isEmpty()) {
                for (Map.Entry<AbstractOption.OptionKey<String>, AbstractOption.OptionValue<String>> key : opt.getRequiredKeys()) {

                    AbstractOption.OptionValue<String> optionValue = key.getValue();
                    // skip invalid variable
                    if (optionValue == null
                            || optionValue.getItems() == null
                            || optionValue.getItems().isEmpty()) {
                        continue;
                    }

                    if (key.getKey().equals(X_MOSN_DATA_ID)) {
                        filterOption.addRequired(key.getKey(), optionValue.first());
                    }
                }
            }

            FilterContext context = new FilterContext(filterOption, dir);
            context.createTemplateCode();
        }
    }
}
