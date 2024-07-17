package io.mosn.coder.intellij;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import io.mosn.coder.intellij.view.GoNewProjectSettings;
import io.mosn.coder.intellij.view.ProtoPanelForm;

import javax.swing.*;

/**
 * @author yiji@apache.org
 */
public class GoModuleWizardStep extends ModuleWizardStep {

    public enum FromType {
        NewProject,
        NewFile;
    }

    FromType from;

    protected GoNewProjectSettings settings;

    private LabeledComponent<TextFieldWithBrowseButton> locationComponent;

    public GoModuleWizardStep() {
        this(FromType.NewProject);
    }

    public GoModuleWizardStep(FromType from) {
        this.from = from;

        if (this.from == FromType.NewFile) {
            disableOrganization();
        }

        this.proto.setFromType(from);
    }

    protected ProtoPanelForm proto = new ProtoPanelForm();

    public void updateDependency(String project) {
        this.proto.updateDependency(project);
    }

    @Override
    public JComponent getComponent() {
        return this.proto.getContent();
    }

    @Override
    public void updateDataModel() {

    }

    public void updatePluginModel() {
        proto.updatePluginModel(settings);
    }

    public void disableOrganization() {
        proto.updateOrganizationVisible(false);
        proto.updateGenerateStandardCodeVisible(false, true);
    }

    public void setSettings(GoNewProjectSettings settings) {
        this.settings = settings;
    }

    public GoNewProjectSettings getSettings() {
        return settings;
    }

    public ValidationInfo CheckValidate() {
        if (settings != null) {
            return this.proto.validate(settings);
        }
        return null;
    }

    public void setLocationComponent(LabeledComponent<TextFieldWithBrowseButton> locationComponent) {
        this.locationComponent = locationComponent;
        proto.setLocationComponent(locationComponent);
    }

    public ProtoPanelForm getProto() {
        return proto;
    }
}
