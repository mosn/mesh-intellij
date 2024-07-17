package io.mosn.coder.intellij.view;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ReflectionUtil;
import io.mosn.coder.common.TimerHolder;
import io.mosn.coder.console.CustomCommandUtil;
import io.mosn.coder.console.PluginConsole;
import io.mosn.coder.plugin.model.PluginStatus;
import io.mosn.coder.task.*;
import io.netty.util.Timeout;

import javax.swing.*;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MiniMeshForm {
    private JPanel rootContent;
    private JCheckBox customConfigurationCheckBox;
    private TextFieldWithBrowseButton bundleChooser;
    private JLabel configFileLabel;
    private MiniConfigBrowseButton miniConfigTextFieldWithBrowseButton1;
    private JTextArea infoTextArea;
    private JScrollPane infoPanel;
    private JComboBox k8sversionCombox;
    private JCheckBox changeClusterQuotaCheckBox;
    private JComboBox cpuCombox;
    private JComboBox memCombox;
    private JComboBox diskCombox;
    private JLabel quotaLabel;
    private JLabel cpuLabel;
    private JLabel memLabel;
    private JLabel diskLabel;
    private JComboBox debugComboBox;
    private JCheckBox debugCheckBox;
    private JLabel componentLabel;
    private ComboBox comboBox1;
    private JCheckBox keepStartCheckBox;

    private MiniMeshConfig config = new MiniMeshConfig();

    private PluginAction action;

    private PrettyTask prettyTask;

    private Project project;

    private Application application;

    private AtomicReference<Timeout> runningTimeout = new AtomicReference<>();

    public MiniMeshForm(Project project) {
        this.project = project;
        this.application = ApplicationManager.getApplication();

        $$$setupUI$$$();

        setConfigComponentVisible(false);
        //setK8sQuotaVisible(false);
        customConfigurationCheckBox.addItemListener(e -> {
            boolean checked = e.getStateChange() == ItemEvent.SELECTED;
            setConfigComponentVisible(checked);

            if (config.getRootDir() != null) {
                String path = config.getRootDir().getPath();
                if (!path.endsWith("/")) {
                    path = path + "/cloudmesh-env.properties";
                }
                miniConfigTextFieldWithBrowseButton1.setText("cloudmesh-env.properties");

                miniConfigTextFieldWithBrowseButton1.setToolTipText(path);
            }
        });

        initInfoPanel();
//        changeClusterQuotaCheckBox.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                boolean checked = e.getStateChange() == ItemEvent.SELECTED;
//                setK8sQuotaVisible(checked);
//            }
//        });

        bundleChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileChooserDescriptor dirDesc =
                        FileChooserDescriptorFactory.createSingleFolderDescriptor();
                VirtualFile select = FileChooser.chooseFile(dirDesc, project, null);
                if (select != null && select.isDirectory() && select.exists()) {
                    config.setRootDir(new File(select.getPath()));

                    config.setProperty(new File(config.getRootDir(), "cloudmesh-env.properties"));
                    // display directory path
                    bundleChooser.setText(select.getPath());
                    bundleChooser.setToolTipText(select.getPath());
                    // enable
                    miniConfigTextFieldWithBrowseButton1.setEnabled(true);
                    miniConfigTextFieldWithBrowseButton1.setToolTipText(config.getProperty().getPath());
                    miniConfigTextFieldWithBrowseButton1.setText("cloudmesh-env.properties");

                    // append
                    if (config.getProperty().exists()) {
                        Properties properties = new Properties();
                        try (FileInputStream in = new FileInputStream(config.getProperty())) {
                            properties.load(in);

                            for (String key : properties.stringPropertyNames()) {
                                config.getDefaults().put(key, properties.getProperty(key));
                            }
                        } catch (Exception ignored) {
                        }
                    }

                } else {
                    miniConfigTextFieldWithBrowseButton1.setEnabled(false);
                }
            }
        });


        initPrettyTask();
    }

    public String startDeployMiniMesh() {
        String message = checkDeployConfig();
        if (message != null) {
            return message;
        }
        initDeployTask();
        return null;
    }

    private String checkDeployConfig() {
//
//        System.out.println("default sys env:" );
//        for (String key : System.getenv().keySet()) {
//            System.out.println("--> key " + key + " --> value " + System.getenv().get(key));
//        }
//
//        if (this.config.getRootDir() == null) {
//            return "mini mesh directory is required";
//        }

        String dir = this.config.getRootDir().getPath();
        File rootDir = new File(dir);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return "mini mesh directory is invalid";
        }

        File release = new File(dir, "release");
        if (!release.exists() || !release.isFile()) {
            return "mini mesh directory is not mini mesh bundle root directory";
        }

        String userDir = System.getProperty("user.home");
        File miniDir = new File(userDir, ".minimesh");

        if (!miniDir.exists()) {
            miniDir.mkdir();
        }

        File mysql = new File(miniDir, "mysql/data");
        if (!mysql.exists()) {
            mysql.mkdirs();
        }

        File hostDir = new File(userDir, ".minikube");
        if (!hostDir.exists()) hostDir.mkdirs();

        File etc = new File(hostDir, "files/etc");
        if (!etc.exists()) etc.mkdirs();

        File hosts = new File(etc, "hosts");
        if (hosts.exists()) hosts.delete();

        try {
            Files.write(hosts.toPath(), "192.168.200.200 sidecar-operator-service".getBytes());
        } catch (IOException e) {
            return "failed to write minikube hosts, err: " + e.getMessage();
        }

        this.config.setProject(miniDir.getPath());
        this.config.setMysql(mysql.getPath());

        this.config.setDebug(this.debugCheckBox.isSelected());

//        if (customConfigurationCheckBox.isSelected()) {
//            File config = this.config.getProperty();
//            if (config == null || !config.exists()) {
//
//            }
//
//        }

        // check cpu
        String cpu = (String) cpuCombox.getSelectedItem();
        String checkCpu = null;
        if (cpu != null && cpu.endsWith("c")) {
            checkCpu = cpu.replace("c", "");
        } else {
            cpu = "max";
        }

        if (checkCpu != null) {
            try {
                Integer.parseInt(checkCpu);
            } catch (Exception ignore) {
                return "cpu is invalid";
            }
        }

        if (checkCpu != null) {
            this.config.setCpu(checkCpu);
        } else {
            this.config.setCpu(cpu);
        }

        String version = (String) k8sversionCombox.getSelectedItem();
        if (version == null || version.trim().length() == 0) {
            version = "v1.23.8";
        }
        this.config.setK8sVersion(version);

        // check memory
        String memory = (String) memCombox.getSelectedItem();
        String checkMem = null;
        if (memory != null && memory.endsWith("g")) {
            checkMem = memory.replace("g", "");
        } else {
            memory = "max";
        }

        if (checkMem != null) {
            try {
                Integer.parseInt(checkMem);
            } catch (Exception ignore) {
                return "memory is invalid";
            }
        }
        this.config.setMemory(memory);

        // check disk
        String disk = (String) diskCombox.getSelectedItem();
        String checkDisk = null;
        if (disk != null && disk.endsWith("g")) {
            checkDisk = disk.replace("g", "");
        } else {
            disk = "90g";
        }

        if (checkDisk != null) {
            try {
                Integer.parseInt(checkDisk);
            } catch (Exception ignore) {
                return "disk is invalid";
            }
        }
        this.config.setDisk(disk);

        this.config.setKeepStart(this.keepStartCheckBox.isSelected());

        int deploys = this.comboBox1.getModel().getSize();
        for (int i = 0; i < deploys; i++) {
            JCheckBox box = (JCheckBox) this.comboBox1.getModel().getElementAt(i);
            if (box.isSelected()) {
                this.config.getDeploy().put(box.getText(), box.getText());
            }
        }

        initPrettyTask();

        return null;
    }

    private void initDeployTask() {

        if (getAction() != null) {
            getAction().disable();
        }

        prettyTask.setOnStepFailure(step -> {

            String output = step.getFailedOutput();
            application.invokeLater(() -> {

                Timeout timeout = runningTimeout.get();
                if (timeout != null) timeout.cancel();

                StringBuilder buf = new StringBuilder(prettyTask.render());

                String appendText = "\n" + step.getName() + " error:\n";
                buf.append(appendText);
                buf.append(output);

                if (config.getLogFile() != null && config.getLogFile().exists()) {
                    try {
                        String err = Files.readString(config.getLogFile().toPath());
                        PluginConsole.displayMessage(project, CustomCommandUtil.CUSTOM_CONSOLE, err);
                    } catch (IOException ignored) {
                    }
                }


                wrapTextArea(buf.toString(), true);

                if (getAction() != null) {

                    destroy();

                    getAction().retry();
                }
            });
        });

        prettyTask.setOnStepSuccess(new StepCallback() {
            @Override
            public void onComplete(RunStep step) {

                application.invokeLater(() -> {

                    wrapTextArea(prettyTask.render(), true);

                    // check all tasks complete but any failed
                    if (step.isComplete()) {
                        for (Task task : step.getTasks()) {

                            if (!PluginStatus.SUCCESS.equals(task.getCmd().getStatus())) {
                                String buf = prettyTask.render() + "\n" +
                                        step.getFailedOutput();
                                wrapTextArea(buf, true);

                                return;
                            }
                        }
                    }
                });
            }
        });

        prettyTask.setOnSuccess(() -> {
//            Timeout timeout = runningTimeout.get();
//            if (timeout != null) timeout.cancel();

            application.invokeLater(() -> {

                wrapTextArea(prettyTask.render(), true);

                String text = infoTextArea.getText();
                if (text != null) {
                    String foundText = "minikube kubectl -- port-forward service/meshserver-service 7080:80 -n sofamesh";

                    int start = text.indexOf(foundText);

                    if (start < 0) {
                        foundText = "minikube kubectl -- port-forward service/nacos-service 8848 9848 -n sofamesh";
                        start = text.indexOf(foundText);
                    }

                    if (start > 0) {
                        infoTextArea.select(start, start + foundText.length());

                        try {
                            String copyEnabled = config.getDefaults().getOrDefault("runtime.copy.forward.enable", "true");
                            if ("true".equals(copyEnabled)) {
                                Toolkit.getDefaultToolkit()
                                        .getSystemClipboard()
                                        .setContents(new StringSelection(foundText), null);
                            }
                        } catch (Exception ignored) {

                        }

                    }
                }
            });

        });

        // start ui update
        startUpdateNotify();

        // debug enable
        // env required SIDECAR_DLV_DEBUG
        config.getDefaults().put("runtime.debug.enable", String.valueOf(this.config.isDebug()));

        // start task step
        prettyTask.execute();
    }

    private void initPrettyTask() {
        prettyTask = new PrettyTask();

        prettyTask.setConfig(config);

        prettyTask.setRunSteps(new ArrayList<>());

        if (!this.keepStartCheckBox.isSelected()) {
            prettyTask.getRunSteps().add(createMinikubeStep());
        }
        prettyTask.getRunSteps().add(createBuildImageStep());
        prettyTask.getRunSteps().add(createDeployStep());

        // first time to update ui
        wrapTextArea(prettyTask.render(), false);
    }

    private void wrapTextArea(String content, boolean wrap) {
        int start = infoTextArea.getSelectionStart();
        int end = infoTextArea.getSelectionEnd();


        infoTextArea.setText(content);

        if (content.length() > end && wrap) {
            infoTextArea.select(start, end);
        }
    }

    private void startUpdateNotify() {
        Timeout timeout = TimerHolder.getTimer().newTimeout(tt -> {
            if (tt.isCancelled()) {
                return;
            }

//            boolean complete = true;
//            for (RunStep step : prettyTask.getRunSteps()) {
//                if (!step.isComplete()) {
//                    complete = false;
//                }
//            }
//
//            if (complete) return;

            this.application.invokeLater(() -> wrapTextArea(prettyTask.render(), true));

            /**
             * schedule next time
             *
             */
            runningTimeout.set(tt.timer().newTimeout(tt.task(), 1, TimeUnit.SECONDS));

        }, 1, TimeUnit.SECONDS);
        runningTimeout.set(timeout);
    }

    private RunStep createMinikubeStep() {

        MinikubeRunStep step = new MinikubeRunStep(this.config);
        step.setName("prepare minikube");
        step.setOnTaskSuccess(task -> {
            application.invokeLater(() -> wrapTextArea(prettyTask.render(), true));
        });
//
//        step.setOnTaskFailure(task -> {
//
//
//            application.invokeLater(() -> infoTextArea.setText(prettyTask.render()));
//        });
//
//        // all task complete
//        step.setOnSuccess(() -> {
////            Timeout timeout = runningTimeout.get();
////            if (timeout != null) timeout.cancel();
//
//            application.invokeLater(() -> infoTextArea.setText(prettyTask.render()));
//        });

        return step;
    }

    private RunStep createBuildImageStep() {
        BuildImageStep step = new BuildImageStep(this.config);
        step.setName("build image");
        step.setOnTaskSuccess(task -> {
            application.invokeLater(() -> wrapTextArea(prettyTask.render(), true));
        });

        return step;
    }

    private RunStep createDeployStep() {
        DeployStep step = new DeployStep(this.config);
        step.setName("deploy mesh");
        step.setOnTaskSuccess(task -> {
            application.invokeLater(() -> wrapTextArea(prettyTask.render(), true));
        });

        return step;
    }

    private void initInfoPanel() {
        //comboBox1.setComponentPopupMenu(new BasicComboPopup(comboBox1));

        //        comboBox1 = new ComboBox();

        DefaultCaret caret = (DefaultCaret) infoTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        //this.comboBox1 = new ComboBox()
        //comboBox1 = new ComboBox();
        CheckBoxModel model = new CheckBoxModel(comboBox1);
        comboBox1.setModel(model);

        comboBox1.setRenderer(model.new CheckBoxCellRenderer());

        //comboBox1.setComponentPopupMenu(new BasicComboPopup(comboBox1));

//        comboBox1.setEditable(true);

        comboBox1.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                comboBox1.getPopup().show();
            }
        });

        comboBox1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //comboBox1.getModel().setSelectedItem(comboBox1.getModel().getSelectedItem());
                int i = comboBox1.getSelectedIndex();

                JCheckBox box = (JCheckBox) comboBox1.getSelectedItem();
                if (box != null)
                    box.setSelected(!box.isSelected());

                //comboBox1.firePopupMenuWillBecomeVisible();
                // comboBox1.
                //new BasicComboPopup(comboBox1).show();

//                if (comboBox1.getUI() != null) {
//                    //comboBox1.getUI(
//                }
                comboBox1.getPopup().show();

//                comboBox1.repaint();
//
//                final ComboBoxUI ui = comboBox1.getUI();
//
//
//                if (ui instanceof BasicComboBoxUI) {
//                    ui.paint(MiniMeshForm.this.);
//                }
//                comboBox1.repaint();
                //MiniMeshForm.this.rootContent.repaint();
            }
        });

//        comboBox1.addPopupMenuListener(new PopupMenuListenerAdapter() {
//            @Override
//            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//                super.popupMenuWillBecomeVisible(e);
//            }
//
//            @Override
//            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
//                super.popupMenuWillBecomeInvisible(e);
//            }
//        });
        comboBox1.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);

                comboBox1.getPopup().hide();
            }
        });
    }


    public void setConfigComponentVisible(boolean enable) {
        miniConfigTextFieldWithBrowseButton1.setVisible(enable);
        configFileLabel.setVisible(enable);

        debugCheckBox.setVisible(enable);
//        debugComboBox.setVisible(enable);

        componentLabel.setVisible(enable);
        comboBox1.setVisible(enable);

        keepStartCheckBox.setVisible(enable);
    }

    public void setK8sQuotaVisible(boolean enable) {
        quotaLabel.setVisible(enable);
        cpuLabel.setVisible(enable);
        cpuCombox.setVisible(enable);

        memLabel.setVisible(enable);
        memCombox.setVisible(enable);

        diskLabel.setVisible(enable);
        diskCombox.setVisible(enable);
    }

    private void createUIComponents() {

        miniConfigTextFieldWithBrowseButton1 = new MiniConfigBrowseButton();
        miniConfigTextFieldWithBrowseButton1.setProject(this.project);
        miniConfigTextFieldWithBrowseButton1.setConfig(this.config);

        miniConfigTextFieldWithBrowseButton1.setEditable(false);

        this.comboBox1 = new ComboBox();
        comboBox1.setEditable(false);
//
//        if (this.comboBox1.getMouseListeners() != null) {
//            for (MouseListener listener : this.comboBox1.getMouseListeners()) {
//                this.comboBox1.removeMouseListener(listener);
//            }
//        }


        final ComboBoxUI ui = comboBox1.getUI();


        if (ui instanceof BasicComboBoxUI) {

            MyBasicComboPopup popup = new MyBasicComboPopup(comboBox1);
//            this.comboBox1.addMouseListener(popup.createListMouseListener());
            ReflectionUtil.setField(BasicComboBoxUI.class, ui, ComboPopup.class, "popup", popup);

        }

    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootContent = new JPanel();
        rootContent.setLayout(new GridLayoutManager(4, 10, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("minimesh directory:");
        rootContent.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bundleChooser = new TextFieldWithBrowseButton();
        bundleChooser.setEditable(false);
        rootContent.add(bundleChooser, new GridConstraints(0, 1, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(520, -1), null, null, 0, false));
        configFileLabel = new JLabel();
        configFileLabel.setText("deploy config file:");
        configFileLabel.setVisible(true);
        rootContent.add(configFileLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        infoPanel = new JScrollPane();
        infoPanel.setAutoscrolls(true);
        rootContent.add(infoPanel, new GridConstraints(3, 0, 1, 10, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 360), null, null, 0, false));
        infoTextArea = new JTextArea();
        infoTextArea.setEditable(false);
        infoTextArea.setLineWrap(true);
        infoTextArea.setMargin(new Insets(10, 10, 5, 10));
        infoTextArea.setMinimumSize(new Dimension(1137, 480));
        infoPanel.setViewportView(infoTextArea);
        final JLabel label2 = new JLabel();
        label2.setText("kubernate version:");
        rootContent.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        k8sversionCombox = new JComboBox();
        k8sversionCombox.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("v1.26.0");
        defaultComboBoxModel1.addElement("v1.23.0");
        defaultComboBoxModel1.addElement("v1.22.0");
        defaultComboBoxModel1.addElement("v1.21.0");
        defaultComboBoxModel1.addElement("v1.20.0");
        defaultComboBoxModel1.addElement("v1.19.4");
        k8sversionCombox.setModel(defaultComboBoxModel1);
        rootContent.add(k8sversionCombox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cpuLabel = new JLabel();
        cpuLabel.setText("cpu:");
        rootContent.add(cpuLabel, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cpuCombox = new JComboBox();
        cpuCombox.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("max");
        defaultComboBoxModel2.addElement("2c");
        defaultComboBoxModel2.addElement("4c");
        defaultComboBoxModel2.addElement("8c");
        defaultComboBoxModel2.addElement("16c");
        defaultComboBoxModel2.addElement("32c");
        cpuCombox.setModel(defaultComboBoxModel2);
        rootContent.add(cpuCombox, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        memLabel = new JLabel();
        memLabel.setText("mem(g):");
        rootContent.add(memLabel, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        memCombox = new JComboBox();
        memCombox.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        defaultComboBoxModel3.addElement("max");
        defaultComboBoxModel3.addElement("2g");
        defaultComboBoxModel3.addElement("4g");
        defaultComboBoxModel3.addElement("8g");
        defaultComboBoxModel3.addElement("16g");
        memCombox.setModel(defaultComboBoxModel3);
        rootContent.add(memCombox, new GridConstraints(2, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        diskLabel = new JLabel();
        diskLabel.setText("disk(g):");
        rootContent.add(diskLabel, new GridConstraints(2, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        diskCombox = new JComboBox();
        diskCombox.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        defaultComboBoxModel4.addElement("60g");
        defaultComboBoxModel4.addElement("90g");
        defaultComboBoxModel4.addElement("120g");
        defaultComboBoxModel4.addElement("150g");
        diskCombox.setModel(defaultComboBoxModel4);
        rootContent.add(diskCombox, new GridConstraints(2, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        customConfigurationCheckBox = new JCheckBox();
        customConfigurationCheckBox.setText("custom deploy config");
        rootContent.add(customConfigurationCheckBox, new GridConstraints(0, 7, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rootContent.add(miniConfigTextFieldWithBrowseButton1, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        rootContent.add(comboBox1, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        componentLabel = new JLabel();
        componentLabel.setText("deploy: ");
        rootContent.add(componentLabel, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        debugCheckBox = new JCheckBox();
        debugCheckBox.setSelected(true);
        debugCheckBox.setText("debug");
        rootContent.add(debugCheckBox, new GridConstraints(1, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        keepStartCheckBox = new JCheckBox();
        keepStartCheckBox.setText("keep start");
        keepStartCheckBox.setToolTipText("Please make sure the cluster is started");
        rootContent.add(keepStartCheckBox, new GridConstraints(1, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootContent;
    }


    public JPanel getRootContent() {
        return rootContent;
    }

    public MiniMeshConfig getConfig() {
        return config;
    }

    public PluginAction getAction() {
        return action;
    }

    public void setAction(PluginAction action) {
        this.action = action;
    }

    public void destroy() {

        if (runningTimeout.get() != null
                && !runningTimeout.get().isCancelled()) {
            runningTimeout.get().cancel();
        }

        prettyTask.cancel();

        // clean pool task
    }

}
