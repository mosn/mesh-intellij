package io.mosn.coder.intellij.view;

import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class MiniMeshQuitAction extends DialogBuilder.DialogActionDescriptor implements PluginAction {

    private MiniMeshForm form;

    private Action self;

    private DialogBuilder builder;

    public MiniMeshQuitAction(MiniMeshForm form, DialogBuilder builder) {
        this(form, "Quit", -1);
        this.builder = builder;
    }

    private MiniMeshQuitAction(MiniMeshForm form, @NlsActions.ActionText String name, int mnemonicChar) {
        super(name, mnemonicChar);

        this.form = form;

        this.setDefault(false);
    }

    @Override
    protected Action createAction(final DialogWrapper dialogWrapper) {

        if (this.self != null) {
            return this.self;
        }
        this.self = new AbstractAction() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                MiniMeshForm form = MiniMeshQuitAction.this.form;
                form.destroy();
                builder.getDialogWrapper().disposeIfNeeded();
            }
        };

        return this.self;
    }

    @Override
    public void retry() {
        if (this.self != null) {
            this.self.setEnabled(true);
        }
    }

    @Override
    public void disable() {
        if (this.self != null) {
            this.self.setEnabled(false);
        }
    }
}