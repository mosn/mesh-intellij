package io.mosn.coder.intellij.view;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.UserActivityProviderComponent;
import com.intellij.util.containers.ContainerUtil;
import io.mosn.coder.task.MiniMeshConfig;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

public class MiniConfigBrowseButton extends TextFieldWithBrowseButton implements UserActivityProviderComponent {

    private MiniMeshConfig config;

    private final List<ChangeListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    private Project project;

    public MiniConfigBrowseButton() {
        super();
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (config.getRootDir() != null
                        && config.getProperty() == null) {
                    File env = new File(config.getRootDir(), "cloudmesh-env.properties");
                    config.setProperty(env);
                }

                createDialog().show();
            }
        });
    }

    private void createUIComponents() {
    }

    private void $$$setupUI$$$() {
        createUIComponents();
    }

    protected MiniConfigDialog createDialog() {
        return new MiniConfigDialog(this);
    }

    @Override
    public void addChangeListener(@NotNull ChangeListener changeListener) {
        myListeners.add(changeListener);
    }

    @Override
    public void removeChangeListener(@NotNull ChangeListener changeListener) {
        myListeners.remove(changeListener);
    }

    private void fireStateChanged() {
        for (ChangeListener listener : myListeners) {
            listener.stateChanged(new ChangeEvent(this));
        }
    }

    protected static List<EnvironmentVariable> convertToVariables(Map<String, String> map, final boolean readOnly) {
        return ContainerUtil.map(map.entrySet(), entry -> new EnvironmentVariable(entry.getKey(), entry.getValue(), readOnly) {
            @Override
            public boolean getNameIsWriteable() {
                return !readOnly;
            }
        });
    }

    @Override
    protected @NotNull @NlsContexts.Tooltip String getIconTooltip() {
        return ExecutionBundle.message("specify.environment.variables.tooltip") + " (" +
                KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)) + ")";
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public MiniMeshConfig getConfig() {
        return config;
    }

    public void setConfig(MiniMeshConfig config) {
        this.config = config;
    }

    public Map<String, String> getEnvs() {
        // user append value
        if (config.getProperty() != null
                && config.getProperty().exists()) {
            Properties properties = new Properties();
            try (FileInputStream in = new FileInputStream(config.getProperty())) {
                properties.load(in);

                for (String key : properties.stringPropertyNames()) {
                    config.getDefaults().put(key, properties.getProperty(key));
                }
            } catch (Exception ignored) {
            }
        }
        return new TreeMap<>(config.getDefaults());
    }

    public void setEnvs(Map<String, String> envs) {
        config.getDefaults().putAll(envs);

        try (FileOutputStream out = new FileOutputStream(config.getProperty())) {
            Properties properties = new Properties();
            for (String key : envs.keySet()) {
                properties.put(key, envs.get(key));
            }
            properties.store(out, null);

        } catch (Exception ignored) {
        }
    }
}
