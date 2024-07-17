package io.mosn.coder.intellij.roobot;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.mosn.coder.intellij.option.*;
import io.mosn.coder.intellij.view.GoNewProjectSettings;

import java.io.File;

/**
 * @author yiji@apache.org
 */
public class CodeGenerator {

    public static void createPluginApplication(Project project, VirtualFile dir, GoNewProjectSettings settings, Module module) {

        PluginOption option = null;

        /**
         * generate plugin code
         */
        PluginType pluginType = settings.activePlugin();
        switch (pluginType) {
            case Protocol: {

                option = settings.getProtocol();

                /**
                 * create new protocol plugin code.
                 */
                ProtocolContext context = new ProtocolContext(settings.getProtocol(), dir);
                context.createTemplateCode();

                break;
            }
            case Filter: {

                option = settings.getFilter();

                /**
                 * create new filter plugin code.
                 */
                FilterContext context = new FilterContext(settings.getFilter(), dir);
                context.createTemplateCode();

                break;
            }
            case Transcoder: {

                option = settings.getTranscoder();

                /**
                 * create new transcoder plugin code.
                 */
                TranscoderContext context = new TranscoderContext(settings.getTranscoder(), dir);
                context.createTemplateCode();

                break;
            }

            // add new plugin type here.
            case Trace: {
                option = settings.getTrace();
                TraceContext context = new TraceContext(settings.getTrace(),  dir);
                context.createTemplateCode();
                break;
            }
        }

        if (option != null) {
            /**
             * generate static template code, include shell
             */
            ScriptContext scriptContext = new ScriptContext(option, dir);
            scriptContext.createTemplateCode();
        }
    }


    public static void createCliApplication(File dir, PluginType pluginType, PluginOption option) {

        /**
         * generate plugin code
         */
        switch (pluginType) {
            case Protocol: {

                ProtocolOption opt = (ProtocolOption) option;
                /**
                 * create new protocol plugin code.
                 */
                ProtocolContext context = new ProtocolContext(opt, dir);
                context.createTemplateCode();

                break;
            }
            case Filter: {

                FilterOption opt = (FilterOption) option;

                /**
                 * create new filter plugin code.
                 */
                FilterContext context = new FilterContext(opt, dir);
                context.createTemplateCode();

                break;
            }
            case Transcoder: {

                TranscoderOption opt = (TranscoderOption) option;

                /**
                 * create new transcoder plugin code.
                 */
                TranscoderContext context = new TranscoderContext(opt, dir);
                context.createTemplateCode();

                break;
            }

            // add new plugin type here.
        }

        if (option != null) {
            /**
             * generate static template code, include shell
             */
            ScriptContext scriptContext = new ScriptContext(option, dir);
            scriptContext.createTemplateCode();
        }
    }

}
