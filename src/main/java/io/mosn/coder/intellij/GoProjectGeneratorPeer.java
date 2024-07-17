package io.mosn.coder.intellij;

import com.goide.project.GoProjectLibrariesService;
import com.goide.sdk.GoSdk;
import com.goide.sdk.combobox.GoBasedSdkChooserCombo;
import com.goide.wizard.GoGopathBasedProjectGeneratorPeer;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.panels.NonOpaquePanel;
import io.mosn.coder.intellij.view.GoNewProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author yiji@apache.org
 */
public class GoProjectGeneratorPeer extends GoGopathBasedProjectGeneratorPeer<GoNewProjectSettings> {

    private GoModuleWizardStep step;

    private Runnable checkValid;

    public GoProjectGeneratorPeer() {
        this.step = new GoModuleWizardStep();
    }

    protected GoBasedSdkChooserCombo.Validator<GoSdk> getSdkValidator() {
        return GoModuleBuilder.getSdkValidator();
    }

    @NotNull
    public GoNewProjectSettings getSettings() {
        return new GoNewProjectSettings(this.getSdkFromCombo(), this.isIndexGoPath(), this.step);
    }

    @Override
    public @NotNull JComponent getComponent() {
        return this.step.getComponent();
    }

    @Override
    public @NotNull JComponent getComponent(@NotNull TextFieldWithBrowseButton myLocationField, @NotNull Runnable checkValid) {
        this.checkValid = checkValid;
        // register checkValid callback
        this.step.proto.setCheckValid(this.checkValid);
        return getComponent();
    }

    @Override
    public @NotNull JPanel createSettingsPanel(@NotNull Disposable parentDisposable, @Nullable LabeledComponent<TextFieldWithBrowseButton> locationComponent) {
        this.step.setLocationComponent(locationComponent);
        JPanel panel = super.createSettingsPanel(parentDisposable, locationComponent);
        if (this.myIndexEntireGoPathCheckBox != null) {
            Project defaultProject = ProjectManager.getInstance().getDefaultProject();
            GoProjectLibrariesService.getInstance(defaultProject).setIndexEntireGopath(true);

            // invisible panel
            for (Component component : panel.getComponents()) {
                if (component != null && component instanceof NonOpaquePanel) {
                    component.setVisible(false);
                } else if (component != null && component instanceof JPanel) {
                    // pretty Location & GO ROOT
                    paddingText((JPanel) component);
                }
            }
        }
        return panel;
    }

    private void paddingText(JPanel component) {
        JLabel base = this.step.proto.getPluginNameField();
        int padding = SwingUtilities.computeStringWidth(base.getFontMetrics(base.getFont()), base.getText());
        for (Component cpt : component.getComponents()) {
            if (cpt instanceof JLabel) {
                JLabel text = (JLabel) cpt;
                /**
                 *
                 * Fill the alignment plugin panel:
                 *
                 * It's best to just align the text box, but currently the trigger
                 * callback method doesn't get the correct location information
                 */
                int append = Math.max(padding - SwingUtilities.computeStringWidth(text.getFontMetrics(text.getFont()), text.getText()), 0);
                int textPad = SwingUtilities.computeStringWidth(text.getFontMetrics(text.getFont()), " ");
                if (append > 0 && text.getText() != null && text.getText().length() > 0) {
                    StringBuffer buffer = new StringBuffer();
                    for (int i = append / textPad + 1; i > 0; i--) {
                        buffer.append(" ");
                    }
                    if (text.getText().length() < padding) {
                        text.setText(text.getText() + buffer);
                    }
                }
            }
        }
    }

    @Override
    public @NotNull JPanel createModuleSettingsPanel(@NotNull Disposable parentDisposable, @Nullable Project project) {
        return super.createModuleSettingsPanel(parentDisposable, project);
    }

    @Override
    public void buildUI(@NotNull SettingsStep settingsStep) {
        super.buildUI(settingsStep);
    }

    @Override
    public @Nullable ValidationInfo validate() {
        return step.proto.validate(step.settings == null ? getSettings() : step.settings);
    }
}
