package io.mosn.coder.compiler;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.MessageView;
import io.mosn.coder.console.MostlySilentColoredProcessHandler;
import io.mosn.coder.console.PluginConsole;
import io.mosn.coder.plugin.model.PluginBundle;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * @author yiji@apache.org
 */
public class PluginCompiler extends AbstractCompiler {


    /**
     * goland compiler, should be in awt thread invoke
     */
    public static void compile(Project project, PluginBundle.Plugin plugin) {

        if (plugin.getCommands() != null) {
            for (Command command : plugin.getCommands()) {
                /**
                 * initialize compile terminal first.
                 */
                PluginConsole.findOrCreate(project, command.title);
            }
        }

        pool.execute(() -> {

            for (Command command : plugin.getCommands()) {

                if (command.fastQuit) continue;

                // find or create console from ui thread
                PluginConsole console = PluginConsole.findOrCreate(project, command.title);
                runCommand(project, command, console);

            }

        });
    }

    public static void compile(Project project, Command command) {

        if (command != null) {
            /**
             * initialize compile terminal first.
             */
            PluginConsole console = PluginConsole.findOrCreate(project, command.title);

            pool.execute(() -> {
                runCommand(project, command, console);
            });
        }
    }

    public static void submit(Runnable runnable) {
        if (runnable != null) {
            pool.execute(runnable);
        }
    }

    public static boolean runCommand(Project project, Command command, PluginConsole console) {
        try {

            command.start = System.currentTimeMillis();

            if (command.clearConsole) {
                PluginConsole.displayMessage(project, command.title, "", true);
            }

            GeneralCommandLine commandLine = new GeneralCommandLine(command.exec);
            commandLine.setCharset(Charset.forName("UTF-8"));
            commandLine.setWorkDirectory(project.getBasePath());

            ProcessHandler handler = new MostlySilentColoredProcessHandler(commandLine);
            console.getView().attachToProcess(handler);

            ApplicationManager.getApplication().invokeLater(() -> {
                final MessageView messageView = MessageView.SERVICE.getInstance(project);
                messageView.runWhenInitialized(() -> {
                    // invoke from ui thread
                    console.bringToFront();
                });
            });

            handler.addProcessListener(new ProcessAdapter() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {

                    command.stop = System.currentTimeMillis();

                    ApplicationManager.getApplication().invokeLater(() -> {
                        final MessageView messageView = MessageView.SERVICE.getInstance(project);
                        messageView.runWhenInitialized(() -> {
                            // scroll terminal
                            console.getView().requestScrollingToEnd();
                        });
                    });

                    if (command.getCallback() != null) {
                        command.getCallback().terminated(event.getExitCode());
                    }
                }
            });

            if (command.fastQuit) return false;

            handler.startNotify();
        } catch (Exception ex) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("Run command ").append(command).append(" failed: ").append(ex.getMessage());
            PluginConsole.displayMessage(project, command.title, buffer.toString());

            // dump stack trace
            ex.printStackTrace();

            return false;
        }

        return true;
    }
}
