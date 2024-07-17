package io.mosn.coder.intellij.view;

import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author yiji@apache.org
 */
public class UpgradeDialogAction extends DialogBuilder.DialogActionDescriptor implements PluginAction {

    private PluginPanelForm form;

    private DialogBuilder builder;

    private Action self;

    public UpgradeDialogAction(PluginPanelForm form, DialogBuilder builder) {
        this("Start Upgrade Now", -1);
        this.form = form;

        this.builder = builder;

        this.form.setAction(this);
    }

    private UpgradeDialogAction(@NlsActions.ActionText String name, int mnemonicChar) {
        super(name, mnemonicChar);

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

                /**
                 * start upgrade plugins
                 */
                String message = form.deployOrUpgradePlugins(null);
                if (message != null && message.length() > 0) {
                    builder.setErrorText(message);
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