package io.mosn.coder.intellij;

import com.goide.project.GoModuleBuilderBase;
import com.goide.sdk.GoSdk;
import com.goide.sdk.combobox.GoBasedSdkChooserCombo;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import io.mosn.coder.intellij.view.GoNewProjectSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yiji@apache.org
 */
public class GoModuleBuilder extends GoModuleBuilderBase<GoNewProjectSettings> {

    private static final Logger LOG = Logger.getInstance(GoModuleBuilder.class);

    protected Project project;

    protected GoModuleBuilder(@NotNull com.goide.wizard.GoProjectGeneratorPeer<GoNewProjectSettings> peer) {
        super(peer);
    }

    @Override
    public @NotNull ModuleType getModuleType() {
        return new GoModuleType();
    }

    @Override
    protected void moduleCreated(@NotNull Module module, boolean isCreatingNewProject) {
    }

    public String getPresentableName() {
        return "Mosn Plugin";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription() {
        return "Mosn plugin modules are used for developing <b>mosn plugin</b> applications.";
    }

    @Override
    public boolean validate(@Nullable Project currentProject, @NotNull Project project) {
        return super.validate(currentProject, project);
    }

    protected static GoBasedSdkChooserCombo.Validator<GoSdk> getSdkValidator() {
        return (sdk) -> ValidationResult.OK;
    }

    @Override
    public ModuleWizardStep modifyStep(SettingsStep settingsStep) {
        return super.modifyStep(settingsStep);

    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
        return super.createWizardSteps(wizardContext, modulesProvider);
    }


}
