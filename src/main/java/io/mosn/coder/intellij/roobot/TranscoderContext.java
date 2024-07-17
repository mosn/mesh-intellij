package io.mosn.coder.intellij.roobot;

import com.intellij.openapi.vfs.VirtualFile;
import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.option.TranscoderOption;
import io.mosn.coder.intellij.template.Template;
import io.mosn.coder.intellij.template.trans.TranscoderTemplate;

import java.io.File;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class TranscoderContext extends AbstractContext {

    public TranscoderContext(PluginOption option, File dir) {
        super(option, dir);
    }

    public TranscoderContext(TranscoderOption option, VirtualFile dir) {
        super(option, dir);
    }

    @Override
    public void createTemplateCode() {
        if (option != null) {
            Template template = new TranscoderTemplate();
            List<Source> code = template.create(option);
            flush(code);
        }
    }
}
