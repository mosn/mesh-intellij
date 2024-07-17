package io.mosn.coder.cli;

import com.alibaba.fastjson.JSON;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.compiler.TerminalCompiler;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.template.*;
import io.mosn.coder.intellij.util.FileWriter;
import io.mosn.coder.plugin.model.PluginMetadata;
import io.mosn.coder.plugin.model.PluginSimpleMetadata;
import io.mosn.coder.upgrade.ProjectMod;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static io.mosn.coder.common.DirUtils.*;

/**
 * @author yiji@apache.org
 */

@CommandLine.Command(name = "fresh",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage:|@%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description:|@%n%n",
        parameterListHeading = "%n@|bold,underline Parameters:|@%n",
        optionListHeading = "%n@|bold,underline Options:|@%n",
        header = "Auto upgrade plugin project.",
        description = "Automatically fresh project code, including shell and configs.")
public class CliRefresh implements Runnable {

    class CliFreshInfo {
        int count;
    }

    @CommandLine.Option(required = true, names = {"--project-dir", "-d"}, paramLabel = "<dir>", description = "Set the path to the project including project name")
    String path;

    @Override
    public void run() {

        /**
         * update plugin project
         */

        if (path != null) {
            File mod = new File(path, "go.mod");

            if (!mod.exists()) {
                System.err.println("Invalid project path");
                System.exit(0);
            }

            if (mod.exists()) {
                /**
                 * project root
                 */

                CliFreshInfo info = new CliFreshInfo();

                refreshProject(path, info, new MakefileTemplate(), MakefileTemplate.Path, MakefileTemplate.Name, false);
                refreshProject(path, info, new StartTemplate(), StartTemplate.Path, StartTemplate.Name, false);
                refreshProject(path, info, new StopTemplate(), StopTemplate.Path, StopTemplate.Name, false);

                refreshProject(path, info, new ApplicationPropertyTemplate(), ApplicationPropertyTemplate.Path, ApplicationPropertyTemplate.Name, true);
                refreshProject(path, info, new VersionTemplate(), VersionTemplate.Path, VersionTemplate.Name, true);

                // compile
                refreshProject(path, info, new CompileTemplate(), CompileTemplate.Path, CompileTemplate.Name, false);
                refreshProject(path, info, new CompileCodecTemplate(), CompileCodecTemplate.Path, CompileCodecTemplate.Name, false);
                refreshProject(path, info, new CompileFilterTemplate(), CompileFilterTemplate.Path, CompileFilterTemplate.Name, false);
                refreshProject(path, info, new CompileTranscoderTemplate(), CompileTranscoderTemplate.Path, CompileTranscoderTemplate.Name, false);

                // package
                refreshProject(path, info, new PackageCodecTemplate(), PackageCodecTemplate.Path, PackageCodecTemplate.Name, false);
                refreshProject(path, info, new PackageFilterTemplate(), PackageFilterTemplate.Path, PackageFilterTemplate.Name, false);
                refreshProject(path, info, new PackageTranscoderTemplate(), PackageTranscoderTemplate.Path, PackageTranscoderTemplate.Name, false);

                // ignore
                refreshProject(path, info, new GitIgnoreTemplate(), GitIgnoreTemplate.Path, GitIgnoreTemplate.Name, false);

                refreshRegistry(path, info);

                // upgrade project
                recreateEnvConf(path, info);

                recreateImageFile(path, info);
                recreateGoMod(path, info);

                displayFreshInfo(path, info);

            }

        }
    }

    protected void refreshProject(String project, CliFreshInfo info, Template template, String path, String name, boolean onlyMissing) {
        File file = new File(project, path + "/" + name);

        List<Source> code = template.create(null);
        Source source = code.get(0);

        boolean exist = file.exists();

        /**
         * run runnable
         */
        if (!exist && onlyMissing) {

            info.count++;

            FileWriter.writeAndFlush(new File(project), template.create(null));
            System.out.println("create " + name + " complete");

            return;
        }


        if (exist) {

            /**
             * record app exported port
             */
            ProtocolOption option = null;

            try {
                String prev = new String(Files.readAllBytes(file.toPath()));
                if (prev.length() != source.getContent().length()
                        || !source.getContent().equals(prev)) {

                    if (template instanceof StartTemplate) {
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

                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                        /**
                         * recreate start shell and compare again
                         */
                        code = template.create(option);
                        source = code.get(0);
                        if (prev.length() != source.getContent().length()
                                || !source.getContent().equals(prev)) {
                            file.delete();
                        } else {
                            /**
                             * file not changed
                             */
                            return;
                        }

                    } else {
                        /**
                         * other template just delete
                         */
                        file.delete();
                    }
                } else {
                    return;
                }

            } catch (IOException ex) {
                System.out.println("failed to delete file " + name + "");
                return;
            }

            info.count++;

            FileWriter.writeAndFlush(new File(project), code);
            System.out.println("update " + name + " complete");
        }

    }

    private void refreshRegistry(String project, CliFreshInfo info) {
        TerminalCompiler.submit(() -> {
            if (project != null && project.length() > 0) {
                /**
                 * Initialize the registry ahead of time
                 */
                // SubscribeConsoleAddress.getMeshServerAddress(project);
            }
        });
    }

    private void displayFreshInfo(String project, CliFreshInfo info) {
        if (info.count <= 0) {
            System.out.println("No need to update");
        }
    }

    private static void recreateEnvConf(String path, CliFreshInfo info) {
        File file = new File(path, EnvConfTemplate.Path + "/" + EnvConfTemplate.Name);

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
                System.err.println("failed to update file " + EnvConfTemplate.Name + "");
                return;
            }
        }

        info.count++;
        System.out.println("update " + EnvConfTemplate.Name + " complete");
    }

    private static void recreateImageFile(String path, CliFreshInfo info) {
        File file = new File(path, DockerfileTemplate.Path + "/" + DockerfileTemplate.Name);

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
                System.err.println("failed to delete file " + DockerfileTemplate.Name + "");
                return;
            }
        }

        info.count++;

        FileWriter.writeAndFlush(new File(path), code);
        System.out.println("update " + DockerfileTemplate.Name + " complete");
    }

    private void recreateGoMod(String path, CliFreshInfo info) {
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

            command.callback = status -> {
                if (status != 0) {
                    System.err.println("run to mod tidy failed");

                    if (command.output != null && !command.output.isEmpty()){
                        for (String line: command.output){
                            System.err.println(line);
                        }
                    }

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

                            System.out.println("update go.mod complete");
                        }
                    }

                    System.out.println("go mod tidy complete");

                } catch (Exception ignored) {
                    System.err.println("failed to update local go.mod ");
                }
            };

            TerminalCompiler.runCommand(path, command);

        } catch (Exception ex) {
            System.err.println("failed to update file go.mod ");
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

    private static ReplaceAction replaceFutureGate() {
        return (line, options) -> {

            String override = line.replace("MOSN_FEATURE_OPENAPI_ENABLE=false,", "");
            // return replaced text line
            return TextLine.Terminate.with(override);
        };
    }

}
