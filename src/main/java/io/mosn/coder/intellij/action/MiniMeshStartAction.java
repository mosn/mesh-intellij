package io.mosn.coder.intellij.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogBuilder;
import io.mosn.coder.intellij.view.*;
import org.jetbrains.annotations.NotNull;

public class MiniMeshStartAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        MiniMeshForm meshForm = new MiniMeshForm(e.getProject());

        DialogBuilder builder = new DialogBuilder(e.getProject()).centerPanel(meshForm.getRootContent());
        builder.removeAllActions();


        builder.addActionDescriptor(new MiniMeshDialogAction(meshForm, builder));
        builder.addActionDescriptor(new MiniMeshQuitAction(meshForm, builder));
        builder.showNotModal();
    }


}
