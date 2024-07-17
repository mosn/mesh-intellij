package io.mosn.coder.intellij.action;

import io.mosn.coder.compiler.Command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * @author yiji@apache.org
 */
public class PackagePluginAction extends AbstractPluginAction {

    @Override
    protected Command createSingleCommand(PluginActionInfo info) {
        Command command = new Command();

        command.shortAlias = "package";
        ArrayList<String> exec = new ArrayList<>();

        exec.add("make");
        switch (info.pluginType) {
            case Filter:
                exec.add("pkg-filter");
                command.title = "filter_" + info.pluginName;
                break;
            case Transcoder:
                exec.add("pkg-trans");
                command.title = "trans_" + info.pluginName;
                break;
            case Protocol:
                exec.add("pkg-codec");
                command.title = "codec_" + info.pluginName;
                break;
            case Trace: {
                exec.add("pkg-trace");
                command.title = "trace_" + info.pluginName;
            }
        }
        exec.add("plugin=" + info.pluginName);

        if (info.project != null) {
            File file = new File(info.project.getBasePath(), "application.properties");
            if (file.exists()) {
                // append filter and transcoder
                Properties application = new Properties();
                try (FileInputStream stream = new FileInputStream(file)) {
                    application.load(stream);

                    String transform = (String) application.get("plugin.arm.transform.amd");
                    if ("off".equals(transform)) {
                        exec.add("arm.to.amd=off");
                    }

                } catch (IOException ignored) {

                }

            }
        }

        command.exec = exec;

        return command;
    }
}
