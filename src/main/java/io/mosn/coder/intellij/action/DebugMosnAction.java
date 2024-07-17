package io.mosn.coder.intellij.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import io.mosn.coder.common.HttpUtils;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.compiler.PluginCompiler;
import io.mosn.coder.console.PluginConsole;
import io.mosn.coder.plugin.model.PluginBundle;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DebugMosnAction extends AbstractPluginAction {

    static Command task;

    @Override
    public void update(AnActionEvent e) {

        // Set the availability based on whether a project is open
        Presentation presentation = e.getPresentation();
        if (!presentation.isEnabled()) {
            return;
        }

        if (e.getProject() == null || e.getProject().getBasePath() == null) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        }

        // check mosn file exists
        File file = new File(e.getProject().getBasePath(), "build/sidecar/binary/mosn");
        boolean exist = file.exists();
        presentation.setEnabled(exist);
        presentation.setVisible(exist);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        quitExitForRegistry();
        task = createSingleCommand(new PluginActionInfo(e.getProject()));
        runCommand(e.getProject(), task);
    }

    private void quitExitForRegistry() {
        if (task != null) {
            /**
             * notify any running command
             */
            task.fastQuit = true;
        }
    }

    @Override
    protected Command createSingleCommand(PluginActionInfo info) {
        Command command = new Command();

        ArrayList<String> exec = new ArrayList<>();
        exec.add("make");
        exec.add("debug");

        command.title = "mosn-container";
        command.exec = exec;

        Application application = ApplicationManager.getApplication();

        command.callback = status -> {
            if (status == 0) {
                Project project = info.project;
                if (project != null) {
                    File file = new File(project.getBasePath(), "build/codecs");
                    if (file.exists()) {
                        File[] files = file.listFiles();

                        /**
                         * Serial command execution wrapper
                         */
                        PluginBundle.Plugin plugin = new PluginBundle.Plugin();
                        plugin.setCommands(new ArrayList<>());

                        boolean containsShell = false;

                        for (File runningCodec : files) {
                            String name = runningCodec.getName();
                            File config = new File(project.getBasePath(), "configs/codecs/" + name + "/auto_pub_sub.sh");
                            if (!config.exists()) {
                                /**
                                 * Compatible with previous versions of code generators
                                 */
                                config = new File(project.getBasePath(), "configs/codecs/" + name + "/" + name + "_pub_sub.sh");
                            }

                            if (config.exists()) {

                                containsShell = true;

                                Command registry = new Command();

                                ArrayList<String> run = new ArrayList<>();
                                run.add("bash");
                                run.add(config.getAbsolutePath());

                                registry.shortAlias = "registry";
                                registry.exec = run;
                                registry.title = "codec_" + name;

                                registry.callback = code -> {
                                    if (code != 0) {
                                        application.invokeLater(() -> {
                                            PluginConsole.displayMessage(project, registry.title, name + " publish or subscribe failed, please retry again. ");
                                        });
                                    }
                                };

                                plugin.getCommands().add(registry);
                            }
                        }

                        if (containsShell) {
                            PluginCompiler.submit(() -> {

                                boolean started = false;

                                try {

                                    /**
                                     * waiting mosn start already
                                     */
                                    long start = System.currentTimeMillis();
                                    long timeout = 15;

                                    if (plugin.getCommands() != null && plugin.getCommands().size() > 0) {
                                        application.invokeLater(() -> {
                                            PluginConsole.displayMessage(project, command.title, "After the mosn initialized, the registry script will be triggered... ");
                                        });
                                    }

                                    while (!started && TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) < timeout) {

                                        if (command.fastQuit) {

                                            for (Command cmd : plugin.getCommands()) {
                                                cmd.fastQuit = true;
                                            }

                                            /**
                                             * fast quit
                                             */
                                            return;
                                        }

                                        String response = HttpUtils.http("http://127.0.0.1:11001/api/v1/states", "GET", null);

                                        if (response == null) {
                                            return;
                                        }

                                        if (response.length() == 0) {

                                            Thread.sleep(1000);

                                            application.invokeLater(() -> {
                                                PluginConsole.displayMessage(project, command.title, "Waiting mosn initialized, the registry script will be triggered... " + (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)) + "s");
                                            });

                                            /**
                                             * retry next time
                                             */
                                            continue;
                                        }

                                        String[] items = response.split("&");
                                        if (items.length > 0) {
                                            for (String item : items) {
                                                if (item.contains("state=")) {
                                                    String[] states = item.split("=");
                                                    /**
                                                     * hack: state=1
                                                     */
                                                    if (states.length == 2 && "1".equals(states[1])) {
                                                        started = true;

                                                        PluginConsole.displayMessage(project, command.title, "The mosn initialized success, elapse: " + (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)) + "s");
                                                        break;
                                                    } else {
                                                        application.invokeLater(() -> {
                                                            PluginConsole.displayMessage(project, command.title, "Waiting mosn initialized, the registry script will be triggered... " + (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)) + "s");
                                                        });
                                                    }
                                                }
                                            }
                                        }
                                    }

                                } catch (Exception ignored) {
                                }

                                if (!started || command.fastQuit) {
                                    return;
                                }

                                application.invokeLater(() -> {
                                    PluginCompiler.compile(project, plugin);
                                });
                            });
                        }

                    }
                }
            }
        };

        command.clearConsole = true;

        return command;
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
