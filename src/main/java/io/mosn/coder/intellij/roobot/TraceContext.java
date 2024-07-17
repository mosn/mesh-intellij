package io.mosn.coder.intellij.roobot;

import com.intellij.openapi.vfs.VirtualFile;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.option.TraceOption;
import io.mosn.coder.intellij.template.Template;
import io.mosn.coder.intellij.template.trace.SkyWalkingTemplate;
import io.mosn.coder.intellij.template.trace.ZipKinTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TraceContext extends AbstractContext {

    public TraceContext(TraceOption option, File dir) {
        super(option, dir);
    }

    public TraceContext(TraceOption option, VirtualFile dir) {
        super(option, dir);
    }

    @Override
    public void createTemplateCode() {
        Template template;
        if (option != null) {

            TraceOption opt = ((TraceOption) option);

            if (opt.getRealTraceType() != null) {
                String traceType = opt.getRealTraceType();

                switch (traceType) {
                    case TraceOption.SKY_WALKING: {
                        template = new SkyWalkingTemplate();

                        List<Source> internal = new ArrayList<>();
                        /**
                         * create code、configuration and metadata
                         */
                        List<Source> code = template.create(option);
                        internal.addAll(code);

                        flush(internal);

                        break;
                    }
                    case TraceOption.ZIP_KIN: {

                        template = new ZipKinTemplate();

                        List<Source> internal = new ArrayList<>();
                        /**
                         * create code、configuration and metadata
                         */
                        List<Source> code = template.create(option);
                        internal.addAll(code);

                        flush(internal);

                        break;
                    }
                }
            }
        }
    }
}

