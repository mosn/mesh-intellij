package io.mosn.coder.intellij.action;

import com.goide.sdk.GoSdk;
import com.goide.sdk.GoSdkService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import io.mosn.coder.intellij.GoModuleWizardStep;
import io.mosn.coder.intellij.roobot.CodeGenerator;
import io.mosn.coder.intellij.view.GoNewProjectSettings;
import org.jetbrains.annotations.NotNull;

/**
 * @author yiji@apache.org
 */
public class GoCreatePluginAction extends AnAction {

    // private DialogWrapper wrapper;

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // Using the event, create and show a dialog
        Project project = event.getProject();
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);

        GoModuleWizardStep step = new GoModuleWizardStep(GoModuleWizardStep.FromType.NewFile);
        DialogBuilder builder = new DialogBuilder(project).centerPanel(step.getComponent());

        DialogWrapper wrapper = builder.getDialogWrapper();

        if (project.getBasePath() != null) {
            step.updateDependency(project.getBasePath());
        }

        builder.setOkOperation(() -> {
            GoSdk sdk = GoSdkService.getInstance(project).getSdk(null);
            // update setting
            step.setSettings(new GoNewProjectSettings(sdk, false, step));

            ValidationInfo info = step.CheckValidate();
            if (info != null) {
                MessageDialogBuilder.okCancel("Plugin Waining", info.message).ask(project);

                wrapper.show();

                return;
            }

            step.updatePluginModel();
            CodeGenerator.createPluginApplication(project, baseDir, step.getSettings(), null);

            /**
             * fresh project automatically
             */

            new RefreshProjectAction().actionPerformed(event);

            // dispose dialog
            wrapper.disposeIfNeeded();

        });

        builder.showNotModal();
    }

    /**
     * Determines whether this menu item is available for the current context.
     * Requires a project to be open.
     *
     * @param e Event received when the associated group-id menu is chosen.
     */
    @Override
    public void update(AnActionEvent e) {
        // Set the availability based on whether a project is open
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

}
