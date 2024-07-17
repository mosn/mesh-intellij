package io.mosn.coder.intellij.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.intellij.template.VersionTemplate;
import io.mosn.coder.intellij.view.DeployDialogAction;
import io.mosn.coder.intellij.view.PluginPanelForm;
import io.mosn.coder.intellij.view.QuitDialogAction;
import io.mosn.coder.plugin.model.PluginBundle;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

/**
 * @author yiji@apache.org
 */
public class DeployPluginAction extends AbstractPluginAction {


    private ThreadLocal<PluginBundle> localBundle = new InheritableThreadLocal<>() {
        @Override
        protected PluginBundle initialValue() {
            PluginBundle bundle = new PluginBundle();
            bundle.setBundles(new ArrayList<>());

            return bundle;
        }
    };

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        super.actionPerformed(e);

        try {

            Project project = e.getProject();
            PluginPanelForm form = new PluginPanelForm(project, PluginPanelForm.DeployMode.Deploy);

            DialogBuilder builder = new DialogBuilder(project).centerPanel(form.getContent());
            builder.removeAllActions();

            /**
             * append upgrade button
             */
            builder.addActionDescriptor(new DeployDialogAction(form, builder, localBundle.get()));

            builder.addActionDescriptor(new QuitDialogAction(form, builder));

            form.renderBundle(localBundle.get());

            /**
             * prepare plugin bundle for upgrade
             */

            builder.showNotModal();

        } finally {
            localBundle.remove();
        }


    }

    @Override
    protected Command createSingleCommand(PluginActionInfo info) {

        /**
         * append plugin to current bundle
         */

        PluginBundle bundle = localBundle.get();

        PluginBundle.Plugin plugin = new PluginBundle.Plugin();
        switch (info.pluginType) {

            case Filter: {
                plugin.setKind(PluginBundle.KIND_FILTER);
                break;
            }
            case Transcoder: {
                plugin.setKind(PluginBundle.KIND_TRANSCODER);
                break;
            }
            case Protocol: {
                plugin.setKind(PluginBundle.KIND_PROTOCOL);
                break;
            }
            case Trace:{
                plugin.setKind(PluginBundle.KIND_TRACE);
                break;
            }
        }

        plugin.setOwner(true);
        plugin.setName(info.pluginName);

        /**
         * update local plugin version
         */
        File version = new File(info.project.getBasePath(), VersionTemplate.Name);
        if (version.exists()) {
            try (FileInputStream in = new FileInputStream(version)) {
                byte[] bytes = in.readAllBytes();
                if (bytes != null) {
                    plugin.setRevision(new String(bytes));
                }
            } catch (Exception ignored) {
            }
        }

        bundle.getBundles().add(plugin);

        /**
         * mock command: The plugin will be compiled on the deployment console
         */
        return null;
    }

}

