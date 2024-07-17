package io.mosn.coder.intellij.action;

import com.alibaba.fastjson.JSON;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.compiler.PluginCompiler;
import io.mosn.coder.console.PluginConsole;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.template.*;
import io.mosn.coder.intellij.util.FileWriter;
import io.mosn.coder.plugin.model.PluginMetadata;
import io.mosn.coder.plugin.model.PluginSimpleMetadata;
import io.mosn.coder.upgrade.ProjectMod;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static io.mosn.coder.common.DirUtils.*;

/**
 * @author yiji@apache.org
 */
public class RefreshProjectAction extends AbstractPluginAction {

    class FreshInfo {
        int count;
    }

    @Override
    public void update(AnActionEvent e) {
        // Set the availability based on whether a project is open
        Presentation presentation = e.getPresentation();
        if (!presentation.isEnabled()) {
            return;
        }
        presentation.setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        FreshInfo info = new FreshInfo();
        recreateMakefile(e, info);
        recreateStartSh(e, info);
        recreateStopSh(e, info);
        recreateApplicationProperty(e, info);
        recreateVersion(e, info);

        recreateEnvConf(e, info);

        recreateImageFile(e, info);
        recreateGoMod(e, info);

        // compile
        recreateCompileScript(e, info);
        recreateCompileCodecScript(e, info);
        recreateCompileFilterScript(e, info);
        recreateCompileTranscoderScript(e, info);

        // package
        recreatePackageCodecScript(e, info);
        recreatePackageFilterScript(e, info);
        recreatePackageTranscoderScript(e, info);

        // ignore
        recreateIgnoreScript(e, info);

        refreshRegistry(e, info);

        displayFreshInfo(e, info);
    }

    private static void recreateMakefile(@NotNull AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), MakefileTemplate.Name);

        MakefileTemplate template = new MakefileTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {

                    // file not changed.

                    // PluginConsole.displayMessage(e.getProject(), "file " + MakefileTemplate.Name + " is the latest version already");

                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + MakefileTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + MakefileTemplate.Name + " complete");
    }

    private static void recreateImageFile(@NotNull AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), DockerfileTemplate.Path + "/" + DockerfileTemplate.Name);

        DockerfileTemplate template = new DockerfileTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    // file not changed.
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + DockerfileTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + DockerfileTemplate.Name + " complete");
    }

    private static void recreateEnvConf(@NotNull AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), EnvConfTemplate.Path + "/" + EnvConfTemplate.Name);

        EnvConfTemplate template = new EnvConfTemplate();
        List<Source> code = template.create(null);

        if (file.exists()) {
            try {

                boolean debugMode = false;
                boolean openapi = false;

                StringBuilder buffer = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                    String line;
                    do {
                        line = reader.readLine();
                        /**
                         * append to buffer, when complete replaced file will be refreshed
                         */
                        if (line != null) {

                            /**
                             *  check DEBUG_MODE exist
                             */
                            if (!debugMode && line.contains("DEBUG_MODE")) {
                                debugMode = true;
                            } else if (!openapi && line.contains("MOSN_FEATURE_OPENAPI_ENABLE=false")) {
                                openapi = true;

                                /**
                                 * just replace
                                 */
                                line = line.replace("MOSN_FEATURE_OPENAPI_ENABLE=false,", "");
                            }

                            buffer.append(line).append("\n");
                        }

                    } while (line != null);

                } catch (Exception ignored) {
                }

                if (!debugMode) {
                    buffer.append("DEBUG_MODE=true").append("\n");
                }

                if (!debugMode || openapi) {

                    if (buffer.length() > 0) {
                        byte[] content = buffer.toString().getBytes();
                        try (OutputStream stream = new FileOutputStream(file)) {
                            stream.write(content);
                        }
                    }

                } else {
                    // file not changed.
                    return;
                }


            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to update file " + EnvConfTemplate.Name + "");
                return;
            }
        }

        info.count++;
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + EnvConfTemplate.Name + " complete");
    }

    private static ReplaceAction replaceFutureGate() {
        return (line, options) -> {

            String override = line.replace("MOSN_FEATURE_OPENAPI_ENABLE=false,", "");
            // return replaced text line
            return TextLine.Terminate.with(override);
        };
    }


    private static void recreateStartSh(@NotNull AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), StartTemplate.Path + "/" + StartTemplate.Name);

        StartTemplate template = new StartTemplate();

        /**
         * record app exported port
         */
        ProtocolOption option = null;
        List<Source> code = null;

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));

                option = new ProtocolOption();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                    String line;
                    do {
                        line = reader.readLine();

                        /**
                         * backup listener port
                         */
                        if (line != null && line.contains("LISTENER_PORTS=")) {
                            String[] items = line.trim().split(" ");
                            for (String item : items) {
                                int index = item.indexOf(":");
                                /**
                                 * we found port mapping, eg: 12200:12200
                                 */
                                if (index > 0) {

                                    item = item.trim();


                                    /**
                                     * listener port for protocol
                                     */
                                    ProtocolOption o = new ProtocolOption();
                                    o.setServerPort(Integer.parseInt(item.substring(0, index)));

                                    if (option.getListenerPort() == null) {
                                        option.setListenerPort(new ArrayList<>());
                                    }

                                    option.getListenerPort().add(o);

                                }
                            }
                        }

                        /**
                         * backup export port
                         */
                        if (line != null && line.contains("EXPORT_PORTS=")) {
                            String[] items = line.trim().split(" ");
                            for (String item : items) {
                                int index = item.indexOf(":");
                                /**
                                 * we found port mapping, eg: 12200:12200
                                 */
                                if (index > 0) {

                                    item = item.trim();


                                    /**
                                     * export port for protocol
                                     */
                                    ProtocolOption o = new ProtocolOption();
                                    o.setServerPort(Integer.parseInt(item.substring(0, index)));

                                    if (option.getExportPort() == null) {
                                        option.setExportPort(new ArrayList<>());
                                    }

                                    option.getExportPort().add(o);

                                }
                            }
                        }

                        /**
                         * append to buffer, when complete replaced file will be refreshed
                         */
                        if (line != null && line.contains("BIZ_PORTS=")) {
                            String[] items = line.trim().split(" ");
                            for (String item : items) {
                                int index = item.indexOf(":");
                                /**
                                 * we found port mapping, eg: 12200:12200
                                 */
                                if (index > 0) {

                                    item = item.trim();


                                    /**
                                     * embed for protocol
                                     */
                                    ProtocolOption o = new ProtocolOption();
                                    o.setServerPort(Integer.parseInt(item.substring(0, index)));

                                    if (option.getEmbedded() == null) {
                                        option.setEmbedded(new ArrayList<>());
                                    }

                                    option.getEmbedded().add(o);

                                }
                            }

                            /**
                             * we stop reading start script
                             */
                            break;
                        }

                    } while (line != null);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }


                code = template.create(option);
                Source source = code.get(0);
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    /**
                     * file not changed
                     */
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + StartTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + StartTemplate.Name + " complete");
    }

    private static void recreateStopSh(@NotNull AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), StopTemplate.Path + "/" + StopTemplate.Name);

        StopTemplate template = new StopTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + StopTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + StopTemplate.Name + " complete");
    }

    private void recreateApplicationProperty(AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), "application.properties");
        if (!file.exists()) {

            info.count++;

            ApplicationPropertyTemplate app = new ApplicationPropertyTemplate();

            FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), app.create(null));
            PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "create application.properties complete");
        }

        // update application.properties
    }

    private void recreateVersion(AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), VersionTemplate.Name);
        if (!file.exists()) {

            info.count++;

            VersionTemplate app = new VersionTemplate();

            FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), app.create(null));
            PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "create VERSION.txt complete");
        }
    }

    private void recreateCompileScript(AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), CompileTemplate.Path + "/" + CompileTemplate.Name);

        CompileTemplate template = new CompileTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + CompileTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + CompileTemplate.Name + " complete");
    }

    private void recreateCompileCodecScript(AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), CompileCodecTemplate.Path + "/" + CompileCodecTemplate.Name);

        CompileCodecTemplate template = new CompileCodecTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + CompileCodecTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + CompileCodecTemplate.Name + " complete");
    }

    private void recreateCompileFilterScript(AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), CompileFilterTemplate.Path + "/" + CompileFilterTemplate.Name);

        CompileFilterTemplate template = new CompileFilterTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + CompileFilterTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + CompileFilterTemplate.Name + " complete");
    }

    private void recreateCompileTranscoderScript(AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), CompileTranscoderTemplate.Path + "/" + CompileTranscoderTemplate.Name);

        CompileTranscoderTemplate template = new CompileTranscoderTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + CompileTranscoderTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + CompileTranscoderTemplate.Name + " complete");
    }

    private void recreatePackageCodecScript(AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), PackageCodecTemplate.Path + "/" + PackageCodecTemplate.Name);

        PackageCodecTemplate template = new PackageCodecTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + PackageCodecTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + PackageCodecTemplate.Name + " complete");
    }

    private void recreatePackageFilterScript(AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), PackageFilterTemplate.Path + "/" + PackageFilterTemplate.Name);

        PackageFilterTemplate template = new PackageFilterTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + PackageFilterTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + PackageFilterTemplate.Name + " complete");
    }

    private void recreatePackageTranscoderScript(AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), PackageTranscoderTemplate.Path + "/" + PackageTranscoderTemplate.Name);

        PackageTranscoderTemplate template = new PackageTranscoderTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + PackageTranscoderTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + PackageTranscoderTemplate.Name + " complete");
    }

    private void recreateIgnoreScript(AnActionEvent e, FreshInfo info) {
        File file = new File(e.getProject().getBasePath(), GitIgnoreTemplate.Name);

        GitIgnoreTemplate template = new GitIgnoreTemplate();
        List<Source> code = template.create(null);
        Source source = code.get(0);

        if (file.exists()) {
            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {
                    file.delete();
                } else {
                    return;
                }

            } catch (IOException ex) {
                PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to delete file " + GitIgnoreTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(e.getProject().getBasePath()), code);
        PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update " + GitIgnoreTemplate.Name + " complete");
    }

    private void refreshRegistry(AnActionEvent e, FreshInfo info) {
        PluginCompiler.submit(() -> {
            if (e.getProject() != null
                    && e.getProject().getBasePath() != null) {
                /**
                 * Initialize the registry ahead of time
                 */
                // SubscribeConsoleAddress.getMeshServerAddress(e.getProject().getBasePath());
            }
        });
    }

    private void recreateGoMod(AnActionEvent e, FreshInfo info) {
        String path = e.getProject().getBasePath();
        File file = new File(path, "go.mod");

        /**
         * local.mod exist
         */
        ProjectMod current = new ProjectMod(path, "go.mod");
        ProjectMod upgrade = new ProjectMod(path, "build/sidecar/binary/local.mod");
        /**
         * refresh project dependencies
         */
        try {

            /**
             * execute go mod tidy first
             */
            ProjectMod tidyMod = new ProjectMod(path, "go.mod");
            tidyMod.readFile();
            if (tidyMod.getGoVersion() != null) {
                String go = tidyMod.getGoVersion().trim();
                go = go.substring(2).trim(); // version

                int index = go.indexOf(".");
                if (index > 0) {
                    int major = 0, minor = 0;
                    try {
                        major = Integer.parseInt(go.substring(0, index));
                    } catch (Exception ignored) {
                    }

                    try {
                        minor = Integer.parseInt(go.substring(index));
                    } catch (Exception ignored) {
                    }

                    if (major == 1 && minor < 18) {
                        tidyMod.setGoVersion("go 1.18");
                    }

                    tidyMod.prepareFlush(null);
                    tidyMod.flush();
                }
            }

            /**
             * run go mod tidy first
             */
            Command command = new Command();
            ArrayList<String> exec = new ArrayList<>();
            exec.add("go");
            exec.add("mod");
            exec.add("tidy");
            command.exec = exec;
            command.title = PLUGIN_CONSOLE;
            PluginConsole console = PluginConsole.findOrCreate(e.getProject(), command.title);

            command.callback = status -> {
                if (status != 0) {
                    PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "run to mod tidy failed");
                    return;
                }

                try {

                    if (new File(path, "build/sidecar/binary/local.mod").exists()) {
                        current.merge(upgrade);

                        String updateText = current.getBufferText();
                        String prev = new String(Files.readAllBytes(file.toPath()));
                        if (!updateText.equals(prev)) {

                            file.delete();

                            info.count++;

                            /**
                             * flush mod dependency
                             */
                            current.flush();

                            /**
                             * update all configs metadata ?
                             */
                            File conf = new File(path, ROOT_CONFIG_DIR);
                            if (conf.exists()) {
                                File[] dirs = conf.listFiles();
                                for (File dir : dirs) {
                                    if (isPluginTypeDir(dir.getName())) {
                                        switch (dir.getName()) {
                                            case CODECS_DIR: {

                                                File[] protos = dir.listFiles();
                                                for (File proto : protos) {
                                                    File metadata = new File(proto.getAbsolutePath(), "metadata.json");
                                                    if (metadata.exists()) {
                                                        PluginMetadata pm = JSON.parseObject(new FileInputStream(metadata), PluginMetadata.class);
                                                        updateMetadata(metadata, pm, current);
                                                    }
                                                }

                                                break;
                                            }
                                            case STREAM_FILTERS_DIR:
                                            case TRANSCODER_DIR:
                                            case TRACE_DIR: {

                                                File[] plugins = dir.listFiles();
                                                for (File plugin : plugins) {
                                                    File metadata = new File(plugin.getAbsolutePath(), "metadata.json");
                                                    if (metadata.exists()) {
                                                        PluginSimpleMetadata pm = JSON.parseObject(new FileInputStream(metadata), PluginSimpleMetadata.class);
                                                        updateMetadata(metadata, pm, current);
                                                    }
                                                }

                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "update go.mod complete");
                        }
                    }

                    PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "go mod tidy complete");
                } catch (Exception ignored) {
                    PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to update local go.mod ");
                }
            };

            PluginCompiler.runCommand(e.getProject(), command, console);

        } catch (Exception ex) {
            PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "failed to update file go.mod ");
        }
    }


    private void displayFreshInfo(AnActionEvent e, FreshInfo info) {
        if (info.count <= 0) {
            PluginConsole.displayMessage(e.getProject(), PLUGIN_CONSOLE, "No need to update");
        }
    }

    private void updateMetadata(File metadata, PluginSimpleMetadata pm, ProjectMod current) throws IOException {
        String mApi = pm.getDependencies().get("mosn_api");
        String mPkg = pm.getDependencies().get("mosn_pkg");

        if (mApi != null && !mApi.equals(current.getApi()) ||
                mPkg != null && !mPkg.equals(current.getPkg())) {
            pm.getDependencies().put("mosn_api", current.getApi());
            pm.getDependencies().put("mosn_pkg", current.getPkg());
            /**
             * flush metadata
             */
            try (FileOutputStream out = new FileOutputStream(metadata)) {
                out.write(JSON.toJSONString(pm, true).getBytes());
                out.flush();
            }
        }
    }

}
