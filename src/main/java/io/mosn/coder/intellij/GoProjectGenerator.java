package io.mosn.coder.intellij;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.ProjectGeneratorPeer;
import io.mosn.coder.intellij.roobot.CodeGenerator;
import io.mosn.coder.intellij.view.GoNewProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static io.mosn.coder.intellij.util.Icons.MODULE_ICON;

/**
 * @author yiji@apache.org
 */
public class GoProjectGenerator extends com.goide.wizard.GoProjectGenerator<GoNewProjectSettings> {

    @Override
    protected void doGenerateProject( Project project, @NotNull VirtualFile virtualFile, @NotNull GoNewProjectSettings goNewProjectSettings, @NotNull Module module) {

        VirtualFile baseDir = virtualFile;
        if (baseDir != null) {
            goNewProjectSettings.updatePluginModel();
            CodeGenerator.createPluginApplication(module.getProject(), baseDir, goNewProjectSettings, module);
        }

    }

    @Override
    public @NotNull ProjectGeneratorPeer<GoNewProjectSettings> createPeer() {
        return new GoProjectGeneratorPeer();
    }

    @Override
    public @Nullable String getDescription() {
        return "Mosn plugin modules are used for developing <b>mosn plugin</b> applications.";
    }

    @Override
    public @NotNull @NlsContexts.Label String getName() {
        return "Mosn Plugin";
    }

    @Override
    public @Nullable Icon getLogo() {
        return MODULE_ICON;
    }

    @Override
    public @NotNull ValidationResult validate(@NotNull String baseDirPath) {
        return ValidationResult.OK;
    }


}