package io.mosn.coder.intellij;

import com.intellij.openapi.module.ModuleType;
import io.mosn.coder.intellij.util.Icons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static io.mosn.coder.intellij.util.Constants.MODULE_PLUGIN_TYPE_ID;

/**
 * @author yiji@apache.org
 */
public class GoModuleType extends ModuleType<GoModuleBuilder> {

    public GoModuleType() {
        super(MODULE_PLUGIN_TYPE_ID);
    }

    @Override
    public @NotNull GoModuleBuilder createModuleBuilder() {
        return new GoModuleBuilder(new GoProjectGeneratorPeer());
    }

    @Override
    public @NotNull
    @Nls(capitalization = Nls.Capitalization.Title) String getName() {
        return "Mosn Plugin";
    }

    @Override
    public @NotNull
    @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription() {
        return "Mosn plugin modules are used for developing <b>mosn plugin</b> applications.";
    }

    @Override
    public Icon getNodeIcon(boolean isOpened) {
        return Icons.MODULE_ICON;
    }
}
