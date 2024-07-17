package io.mosn.coder.intellij.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import io.mosn.coder.intellij.view.PluginPanelForm;
import io.mosn.coder.intellij.view.QuitDialogAction;
import io.mosn.coder.intellij.view.UpgradeDialogAction;
import io.mosn.coder.plugin.model.PluginBundle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * @author yiji@apache.org
 */
public class UpgradePluginAction extends AbstractPluginAction {

    private ThreadLocal<PluginBundle> localBundle = new InheritableThreadLocal<>() {
        @Override
        protected PluginBundle initialValue() {
            PluginBundle bundle = new PluginBundle();
            bundle.setBundles(new ArrayList<>());

            return bundle;
        }
    };

    @Override
    public void update(AnActionEvent e) {
        // Set the availability based on whether a project is open
        Presentation presentation = e.getPresentation();
        if (!presentation.isEnabled()) {
            return;
        }

        presentation.setEnabled(true);

    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        try {

            Project project = e.getProject();
            PluginPanelForm form = new PluginPanelForm(project, PluginPanelForm.DeployMode.Upgrade);

            DialogBuilder builder = new DialogBuilder(project).centerPanel(form.getContent());
            builder.removeAllActions();

            /**
             * append upgrade button
             */
            builder.addActionDescriptor(new UpgradeDialogAction(form, builder));

            builder.addActionDescriptor(new QuitDialogAction(form, builder));

            /**
             * prepare plugin bundle for upgrade
             */

            builder.showNotModal();

        } finally {
            localBundle.remove();
        }

    }


}

