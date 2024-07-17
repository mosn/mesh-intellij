package io.mosn.coder.intellij.view;

import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class MiniMeshDialogAction extends DialogBuilder.DialogActionDescriptor implements PluginAction {

    private MiniMeshForm form;

    private DialogBuilder builder;

    private Action self;

    public MiniMeshDialogAction(MiniMeshForm form, DialogBuilder builder) {
        this(form, "Start Deploy Mesh Now", -1);
        this.builder = builder;
    }

    private MiniMeshDialogAction(MiniMeshForm form, @NlsActions.ActionText String name, int mnemonicChar) {
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

                MiniMeshForm form = MiniMeshDialogAction.this.form;
                String message = form.startDeployMiniMesh();

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
            this.self.putValue(Action.NAME, "Retry Deploy Mesh Now");
            this.self.setEnabled(true);
        }
    }

    @Override
    public void disable() {
        this.self.setEnabled(false);
    }

}
