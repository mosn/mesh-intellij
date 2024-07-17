package io.mosn.coder.intellij.roobot;

import com.intellij.openapi.vfs.VirtualFile;
import io.mosn.coder.intellij.option.FilterOption;
import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.template.Template;
import io.mosn.coder.intellij.template.filter.FilterTemplate;

import java.io.File;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class FilterContext extends AbstractContext {

    public FilterContext(PluginOption option, File dir) {
        super(option, dir);
    }

    public FilterContext(FilterOption option, VirtualFile dir) {
        super(option, dir);
    }

    @Override
    public void createTemplateCode() {
        if (option != null) {
            Template template = new FilterTemplate();
            List<Source> code = template.create(option);
            flush(code);
        }
    }
}
