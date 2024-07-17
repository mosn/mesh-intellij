package io.mosn.coder.intellij.view;

import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.NlsActions;
import io.mosn.coder.plugin.model.PluginBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author yiji@apache.org
 */
public class DeployDialogAction extends DialogBuilder.DialogActionDescriptor implements PluginAction {

    private PluginPanelForm form;

    private DialogBuilder builder;

    private PluginBundle bundle;

    private Action self;

    public DeployDialogAction(PluginPanelForm form, DialogBuilder builder, PluginBundle bundle) {
        this(form, "Start Deploy Now", -1);
        this.builder = builder;
        this.bundle = bundle;
    }

    private DeployDialogAction(PluginPanelForm form, @NlsActions.ActionText String name, int mnemonicChar) {
        super(name, mnemonicChar);

        this.form = form;

        this.form.setAction(this);

        /**
         * default action
         */
        this.setDefault(true);
    }

    @Override
    protected Action createAction(final DialogWrapper dialogWrapper) {

        if (this.self != null) {
            return this.self;
        }
        this.self = new AbstractAction() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {

                builder.setErrorText(null);

                PluginPanelForm form = DeployDialogAction.this.form;
                String message = form.deployOrUpgradePlugins(DeployDialogAction.this.bundle);

                if (message != null && message.length() > 0) {
                    builder.setErrorText(message);
                    return;
                }

                builder.getDialogWrapper().show();
            }
        };

        return this.self;
    }

    @Override
    public void retry() {
        if (this.self != null) {
            this.self.putValue(Action.NAME, "Retry Deploy Now");
            this.self.setEnabled(true);
        }
    }

    @Override
    public void disable() {
        this.self.setEnabled(false);
    }
}