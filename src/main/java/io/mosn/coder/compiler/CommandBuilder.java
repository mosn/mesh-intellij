package io.mosn.coder.compiler;

import io.mosn.coder.console.CustomCommandUtil;
import io.mosn.coder.plugin.model.PluginBundle;
import io.mosn.coder.plugin.model.PluginBundleRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * @author yiji@apache.org
 */
public class CommandBuilder {

    public static Command createCompileCommand(PluginBundle.Plugin plugin) {
        Command command = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("make");
        switch (plugin.getKind()) {
            case PluginBundle.KIND_FILTER:
                exec.add("filter");
                command.title = "filter_" + plugin.getName();
                break;
            case PluginBundle.KIND_TRANSCODER:
                exec.add("trans");
                command.title = "trans_" + plugin.getName();
                break;
            case PluginBundle.KIND_PROTOCOL:
                exec.add("codec");
                command.title = "codec_" + plugin.getName();
                break;
            case PluginBundle.KIND_TRACE:
                exec.add("trace");
                command.title = "trace_" + plugin.getName();
                break;
        }
        exec.add("plugin=" + plugin.getName());

        command.shortAlias = "compile";

        command.exec = exec;
        return command;
    }

    public static Command createPackageCommand(String project, PluginBundle.Plugin plugin) {
        Command command = new Command();
        ArrayList<String> exec = new ArrayList<>();

        exec.add("make");
        switch (plugin.getKind()) {
            case PluginBundle.KIND_FILTER:
                exec.add("pkg-filter");
                command.title = "filter_" + plugin.getName();
                break;
            case PluginBundle.KIND_TRANSCODER:
                exec.add("pkg-trans");
                command.title = "trans_" + plugin.getName();
                break;
            case PluginBundle.KIND_PROTOCOL:
                exec.add("pkg-codec");
                command.title = "codec_" + plugin.getName();
                break;
            case PluginBundle.KIND_TRACE:
                exec.add("pkg-trace");
                command.title = "trace_" + plugin.getName();
                break;
        }
        exec.add("plugin=" + plugin.getName());

        if (project != null) {
            File file = new File(project, "application.properties");
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

        command.shortAlias = "package";

        command.exec = exec;
        return command;
    }

    public static Command createCopyUpgradeCommand(String project, PluginBundleRule.PluginRule image) {

        String targetDir = project + "/build/upgrade/";
        File dir = new File(targetDir);
        if (!dir.exists()) {
            /**
             * create parent if necessary
             */
            dir.mkdirs();
        }

        Command command = new Command();

        File containerId = new File(project, "build/upgrade/container.id");
        if (containerId.exists()) containerId.delete();

        ArrayList<String> exec = new ArrayList<>();
        exec.add("docker");
        exec.add("create");
        exec.add("--cidfile");
        exec.add(containerId.getAbsolutePath());
        exec.add("--platform=linux/amd64");
        exec.add(image.getImage());

        command.exec = exec;
        command.title = CustomCommandUtil.CUSTOM_CONSOLE;
        return command;
    }

}
