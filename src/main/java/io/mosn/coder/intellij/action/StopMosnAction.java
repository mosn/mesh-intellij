package io.mosn.coder.intellij.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import io.mosn.coder.compiler.Command;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

public class StopMosnAction extends AbstractPluginAction {

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
        runCommand(e.getProject(), createSingleCommand(null));
    }

    @Override
    protected Command createSingleCommand(PluginActionInfo info) {
        Command command = new Command();

        ArrayList<String> exec = new ArrayList<>();
        exec.add("make");
        exec.add("stop");

        command.title = "mosn-container";
        command.exec = exec;

        command.clearConsole = true;

        return command;
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
