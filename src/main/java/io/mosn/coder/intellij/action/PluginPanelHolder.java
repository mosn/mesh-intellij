package io.mosn.coder.intellij.action;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import io.mosn.coder.intellij.view.DeployDialogAction;
import io.mosn.coder.intellij.view.PluginPanelForm;
import io.mosn.coder.intellij.view.UpgradeDialogAction;
import io.mosn.coder.plugin.model.PluginBundle;

public class PluginPanelHolder {

    public Project project;

    public PluginPanelForm form;

    public DialogBuilder builder;

    public UpgradeDialogAction upgradeAction;

    public DeployDialogAction deployAction;

    public PluginBundle bundle;

    public PluginPanelHolder(Project project, PluginPanelForm form, DialogBuilder builder, UpgradeDialogAction upgradeAction) {
        this.project = project;
        this.form = form;
        this.builder = builder;
        this.upgradeAction = upgradeAction;
    }

    public PluginPanelHolder(Project project, PluginPanelForm form, DialogBuilder builder, DeployDialogAction deployAction) {
        this.project = project;
        this.form = form;
        this.builder = builder;
        this.deployAction = deployAction;
    }

    void showNotModal() {
        if (builder != null) {
            builder.showNotModal();
        }
    }

}
