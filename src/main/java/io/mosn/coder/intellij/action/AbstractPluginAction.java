package io.mosn.coder.intellij.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.compiler.PluginCompiler;
import io.mosn.coder.intellij.option.PluginType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

import static io.mosn.coder.common.DirUtils.*;

public abstract class AbstractPluginAction extends AnAction {


    public final static String PLUGIN_CONSOLE = "GoPluginConsole";

    @Override
    public void update(AnActionEvent e) {
        // Set the availability based on whether a project is open
        Presentation presentation = e.getPresentation();
        if (presentation == null) {
            return;
        }

            // check plugin directory clicked
        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (element != null) {

            if (element instanceof PsiDirectory) {
                PsiDirectory dir = (PsiDirectory) element;

                if (dir.getParent() != null) {

                    // compile all plugins
                    if (isRootDirectory(dir.getName())) {
                        // update all plugins text
                        presentation.setText(presentation.getDescription() + " " + wrapName(dir));
                        presentation.setEnabled(true);
                        return;
                    }

                    // compile same kind plugins
                    if (isPluginDirectory(dir.getName(), dir.getParent().getName())) {
                        // update special plugin text
                        presentation.setText(presentation.getDescription() + " " + wrapName(dir));
                        presentation.setEnabled(true);
                        return;
                    }

                    // single plugin
                    PsiDirectory parent = dir.getParent();
                    if (parent != null && parent.getParent() != null) {
                        if (isPluginChildDirectory(parent.getName(), parent.getParent().getName())) {
                            // update single plugin text
                            presentation.setText(presentation.getDescription() + " " + dir.getName());
                            presentation.setEnabled(true);
                            return;
                        }
                    }
                }

            }
        }

        presentation.setEnabled(false);

    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        PluginActionInfo info = actionInfo(e);
        if (info != null) {

            if (info.isBatchAction) {

                // scan all plugins
                if (scanAllPlugins(info)) {
                    // execute all plugin command
                    runAllPlugins(e, info);
                    return;
                }

                // scan same kind plugin
                runSameKindPlugins(e, info);
                return;
            }

            // single command
            runSinglePlugin(e, info);
        }

    }

    protected void runAllPlugins(@NotNull AnActionEvent e, PluginActionInfo info) {
        File[] files = new File(info.dir).listFiles();
        if (files != null) {
            for (File file : files) {
                // same kind plugin
                if (file.isDirectory() && isPluginDirectory(file.getName(), info.pluginName)) {
                    PluginActionInfo actionInfo = new PluginActionInfo(
                            file.getPath()
                            , null
                            , pluginTypeOf(file.getName())
                            , true);
                    actionInfo.project = e.getProject();
                    runSameKindPlugins(e, actionInfo);
                }
            }
        }
    }

    protected void runSinglePlugin(@NotNull AnActionEvent e, PluginActionInfo info) {
        if (info.project == null) info.project = e.getProject();
        Command command = createSingleCommand(info);
        if (command != null) {
            runCommand(e.getProject(), command);
        }
    }

    protected void runSameKindPlugins(@NotNull AnActionEvent e, PluginActionInfo info) {
        ArrayList<Command> commands = new ArrayList<>();
        File[] files = new File(info.dir).listFiles();
        if (files != null) {
            for (File file : files) {
                // plugin name
                if (file.isDirectory()) {
                    PluginActionInfo actionInfo = new PluginActionInfo(
                            file.getPath()
                            , file.getName()
                            , info.pluginType
                            , false);
                    actionInfo.project = e.getProject();
                    Command command = createSingleCommand(actionInfo);
                    commands.add(command);
                }
            }

            for (Command command : commands) {
                runCommand(e.getProject(), command);
            }
        }
    }

    protected Command createSingleCommand(PluginActionInfo info) {

        Command command = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("make");
        switch (info.pluginType) {
            case Filter:
                exec.add("filter");
                command.title = "filter_" + info.pluginName;
                break;
            case Transcoder:
                exec.add("trans");
                command.title = "trans_" + info.pluginName;
                break;
            case Protocol:
                exec.add("codec");
                command.title = "codec_" + info.pluginName;
                break;
            case Trace:
                exec.add("trace");
                command.title = "trace_" + info.pluginName;
                break;
        }
        exec.add("plugin=" + info.pluginName);

        command.exec = exec;

        return command;
    }

    void runCommand(Project project, Command command) {
        PluginCompiler.compile(project, command);
    }

    protected PluginActionInfo actionInfo(AnActionEvent e) {

        // check plugin directory clicked
        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (element != null) {

            if (element instanceof PsiDirectory) {
                PsiDirectory dir = (PsiDirectory) element;

                if (dir.getParent() != null) {

                    // compile all plugins
                    if (isRootDirectory(dir.getName())) {
                        return new PluginActionInfo(dir.getVirtualFile().getPath(), dir.getName(), null, true);
                    }

                    // same kind plugins
                    if (isPluginDirectory(dir.getName(), dir.getParent().getName())) {
                        return new PluginActionInfo(dir.getVirtualFile().getPath(), dir.getName(), pluginTypeOf(dir.getName()), true);
                    }

                    // single plugin
                    PsiDirectory parent = dir.getParent();
                    if (parent != null && parent.getParent() != null) {
                        if (isPluginChildDirectory(parent.getName(), parent.getParent().getName())) {
                            return new PluginActionInfo(dir.getVirtualFile().getPath(), dir.getName(), pluginTypeOf(parent.getName()), false);
                        }
                    }
                }

            }
        }

        return null;
    }

    @NotNull
    String wrapName(PsiDirectory dir) {

        String name = dir.getName();
        switch (name) {
            case STREAM_FILTERS_DIR:
                return "All Filters";
            case TRANSCODER_DIR:
                return "All Transcoders";
            case CODECS_DIR:
                return "All Protocols";
            case ROOT_DIR:
            case ROOT_CONFIG_DIR:
                return "All Plugins";
            default:
                return name;
        }
    }

    boolean scanAllPlugins(PluginActionInfo info) {
        return info.pluginType == null;
    }

    class PluginActionInfo {
        String dir; // action dir
        String pluginName; // action type

        PluginType pluginType;

        boolean isBatchAction; // batch action

        Project project; // maybe null

        public PluginActionInfo(Project project) {
            this.project = project;
        }

        public PluginActionInfo(String dir, String pluginName, PluginType pluginType, boolean isBatchAction) {
            this.dir = dir;
            this.pluginName = pluginName;
            this.pluginType = pluginType;
            this.isBatchAction = isBatchAction;
        }
    }

}
