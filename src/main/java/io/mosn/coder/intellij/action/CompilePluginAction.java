package io.mosn.coder.intellij.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.intellij.option.PluginType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static io.mosn.coder.common.DirUtils.isPluginChildDirectory;
import static io.mosn.coder.common.DirUtils.pluginTypeOf;

/**
 * @author yiji@apache.org
 */
public class CompilePluginAction extends AbstractPluginAction {

    @Override
    public void update(AnActionEvent e) {

        Presentation presentation = e.getPresentation();
        if (presentation == null) {
            return;
        }

        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (element != null) {

            if (element instanceof PsiDirectory) {
                PsiDirectory dir = (PsiDirectory) element;
                if (dir.getParent() != null) {
                    // single plugin
                    PsiDirectory parent = dir.getParent();
                    if (parent != null && parent.getParent() != null) {
                        if (isPluginChildDirectory(parent.getName(), parent.getParent().getName())) {
                            // only codec available
                            if (pluginTypeOf(parent.getName()) == PluginType.Protocol) {
                                // update single plugin text
                                presentation.setText(presentation.getDescription() + " " + dir.getName() + " with Dependency...");
                                presentation.setEnabled(true);
                                return;
                            }

                        }
                    }
                }

            }
        }


        super.update(e);
    }

    @Override
    protected Command createSingleCommand(PluginActionInfo info) {

        Command command = super.createSingleCommand(info);

        if (info.project != null && info.pluginType == PluginType.Protocol) {
            File file = new File(info.project.getBasePath(), "application.properties");
            if (file.exists() && !info.isBatchAction) {
                // append filter and transcoder
                Properties application = new Properties();
                try (FileInputStream stream = new FileInputStream(file)) {
                    application.load(stream);
                    String filterKey = "codec." + info.pluginName + ".filter";
                    if (application.get(filterKey) != null) {
                        String filter = (String) application.get(filterKey);
                        command.exec.add("filter=" + filter);
                    }

                    String transKey = "codec." + info.pluginName + ".transcoder";
                    if (application.get(transKey) != null) {
                        String transcoder = (String) application.get(transKey);
                        command.exec.add("trans=" + transcoder);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }

        return command;
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
