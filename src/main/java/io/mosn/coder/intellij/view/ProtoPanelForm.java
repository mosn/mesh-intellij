package io.mosn.coder.intellij.view;

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import io.mosn.coder.intellij.GoModuleWizardStep;
import io.mosn.coder.intellij.internal.BoltOptionImpl;
import io.mosn.coder.intellij.internal.DubboOptionImpl;
import io.mosn.coder.intellij.internal.SpringCloudOptionImpl;
import io.mosn.coder.intellij.option.*;
import io.mosn.coder.upgrade.ProjectMod;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import static io.mosn.coder.intellij.option.AbstractOption.*;
import static io.mosn.coder.intellij.option.CodecType.FixedLength;
import static io.mosn.coder.intellij.option.PluginOption.DEFAULT_API;
import static io.mosn.coder.intellij.option.PluginOption.DEFAULT_PKG;
import static io.mosn.coder.intellij.util.Constants.COMMA_SPLIT_PATTERN;

public class ProtoPanelForm {
    private JTextField protoField;
    private JCheckBox httpField;
    private JPanel content;
    private JLabel pluginNameField;
    private JTextField serverPortField;
    private JTextField clientPortField;
    private JComboBox<PluginType> pluginTypeField;
    private JCheckBox boltCheckBox;
    private JCheckBox dubboCheckBox;
    private JCheckBox springCloudCheckBox;
    private JCheckBox filterClientCheckBox;
    private JCheckBox filterServerCheckBox;

    private JLabel filterField;
    private JLabel transField;
    private JCheckBox transClientCheckBox;
    private JCheckBox transServerCheckBox;
    private JTextField serviceKeyField;
    private JTextField orgField;
    private JTextField serviceMethodField;
    private JTextField serviceTraceField;
    private JTextField serviceSpanField;
    private JTextField serviceCallerAppFiled;
    private JPanel govPanel;
    private JTextField apiFiled;
    private JTextField packageField;
    private JComboBox<PoolMode> poolModelField;
    private JCheckBox requestIdIsStringCheckBox;
    private JLabel serverPortLabel;
    private JLabel clientPortLabel;
    private JLabel poolModelLabel;
    private JComboBox<CodecType> codecTypeField;
    private JLabel codecTypeLabel;
    private JSpinner codecSpinnerField;
    private JTextField codecPrefixField;
    private JLabel codecLengthLabel;
    private JLabel codecPrefixLabel;
    private JLabel generateCodeLabel;
    private JLabel serviceKeyLabel;
    private JLabel orgLabel;
    private LinkLabel urlLabel;
    private JCheckBox injectHeadCheckBox;
    private JComboBox srcProtocolField;
    private JLabel srcProtocolLabel;
    private JComboBox dstProtocolField;
    private JLabel dstProtocolLabel;
    private JLabel transSplitLabel;
    private JComboBox traceTypeField;
    private JLabel traceTypeLabel;
    private JLabel reporterLabel;
    private JComboBox reporterField;

    private GoModuleWizardStep.FromType fromType;

    private LabeledComponent<TextFieldWithBrowseButton> locationComponent;

    private Runnable checkValid;

    public ProtoPanelForm() {
        updateFilterVisible(false);
        updateTranscoderVisible(false);
        updateCodecTypeVisible(false);
        updateTraceVisible(false);

        this.injectHeadCheckBox.setVisible(false);

        this.apiFiled.setText(DEFAULT_API);
        this.packageField.setText(DEFAULT_PKG);

        this.pluginTypeField.setModel(new PluginTypeModel());
        this.poolModelField.setModel(new PoolModel());
        this.codecTypeField.setModel(new CodecTypeModel());

        this.urlLabel.setIcon(null);
        this.urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.urlLabel.setListener((label, linkUrl) ->
                BrowserLauncher.getInstance().browse("https://github.com/mosn/extensions/tree/master/go-plugin", null), null);

        this.codecSpinnerField.setModel(new SpinnerNumberModel(8, 1, 256, 1));

        this.pluginTypeField.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                PluginType type = (PluginType) this.pluginTypeField.getSelectedItem();

                switch (Objects.requireNonNull(type)) {
                    case Protocol: {

                        // invisible filter
                        updateFilterVisible(false);

                        // invisible transcoder
                        updateTranscoderVisible(false);

                        // invisible trace
                        updateTraceVisible(false);

                        // visible protocol
                        updateProtocolVisible(true);

                        break;
                    }
                    case Filter: {

                        // invisible transcoder
                        updateTranscoderVisible(false);

                        // invisible protocol
                        updateProtocolVisible(false);

                        // invisible trace
                        updateTraceVisible(false);

                        updateFilterVisible(true);

                        break;
                    }
                    case Transcoder: {

                        // invisible filter
                        updateFilterVisible(false);

                        // invisible protocol
                        updateProtocolVisible(false);

                        // invisible trace
                        updateTraceVisible(false);

                        updateTranscoderVisible(true);

                        break;
                    }
                    case Trace: {


                        // invisible filter
                        updateFilterVisible(false);

                        // invisible protocol
                        updateProtocolVisible(false);

                        // invisible transcoder
                        updateTranscoderVisible(false);

                        // visible trace
                        updateTraceVisible(true);

                        break;

                    }
                }

            }
        });

        this.codecTypeField.addItemListener(e -> {
            boolean checked = e.getStateChange() == ItemEvent.SELECTED;
            if (checked) {
                CodecType type = (CodecType) e.getItem();
                switch (type) {
                    case Customize: {
                        updateCodecTypeVisible(false);
                        updateCodeTypeField(true);
                        break;
                    }
                    case FixedLength: {
                        updateCodecTypeVisible(true);
                        break;
                    }
                }
            }
        });

        this.traceTypeField.addItemListener(e -> {
            boolean checked = e.getStateChange() == ItemEvent.SELECTED;
            if (checked) {
                String pt = (String) e.getItem();
                switch (pt) {
                    case TraceOption.SKY_WALKING: {
                        prepareSkyWalkingReporter();
                        break;
                    }
                    case TraceOption.ZIP_KIN: {
                        prepareZipKinReporter();
                        break;
                    }
                }
            }
        });

        this.requestIdIsStringCheckBox.addItemListener(e -> {
            boolean checked = e.getStateChange() == ItemEvent.SELECTED;
            if (!checked) {
                updateCodecTypeVisible(false);
                return;
            }

            if (FixedLength == this.codecTypeField.getSelectedItem()) {
                updateCodecTypeVisible(true);
            } else {
                updateCodeTypeField(true);
            }
        });

        this.httpField.addItemListener(e -> {
            boolean checked = e.getStateChange() == ItemEvent.SELECTED;
            updatePoolModelVisible(!checked);
            this.injectHeadCheckBox.setVisible(checked);

            if (checked) {
                this.requestIdIsStringCheckBox.setSelected(false);
            }

            this.requestIdIsStringCheckBox.setVisible(!checked);
        });
    }

    private void prepareTraceReporter() {

        String traceType = (String) this.traceTypeField.getSelectedItem();
        if (traceType != null) {
            switch (traceType) {
                case TraceOption.SKY_WALKING: {
                    prepareSkyWalkingReporter();
                    break;
                }
                case TraceOption.ZIP_KIN: {
                    prepareZipKinReporter();
                    break;
                }
            }
        }

    }

    private void prepareSkyWalkingReporter() {
        this.reporterField.removeAllItems();

        /**
         * only support reporter:
         * 1. grpc
         * 2. log
         */
        this.reporterField.addItem("gRPC");
        this.reporterField.addItem("log");

        this.reporterField.setSelectedIndex(0);
    }

    private void prepareZipKinReporter() {
        this.reporterField.removeAllItems();

        /**
         * only support reporter:
         * 1. kafka
         * 2. http
         */
        this.reporterField.addItem("kafka");
        this.reporterField.addItem("http");

        // default http reporter
        this.reporterField.setSelectedIndex(1);
    }

    public void updateDependency(String project) {

        /**
         * read remote upgrade api and pkg
         */
        if (project != null) {
            File upgradeMod = new File(project, "build/upgrade/remote.mod");
            if (upgradeMod.exists()) {
                ProjectMod current = new ProjectMod(project, "build/upgrade/remote.mod");
                current.readFile();

                if (current.getApi() != null) {
                    this.apiFiled.setText(current.getApi());
                }

                if (current.getPkg() != null) {
                    this.packageField.setText(current.getPkg());
                }
            } else {
                upgradeMod = new File(project, "build/sidecar/binary/local.mod");
                if (upgradeMod.exists()) {
                    ProjectMod current = new ProjectMod(project, "build/sidecar/binary/local.mod");
                    current.readFile();

                    if (current.getApi() != null) {
                        this.apiFiled.setText(current.getApi());
                    }

                    if (current.getPkg() != null) {
                        this.packageField.setText(current.getPkg());
                    }
                }
            }
        }

    }

    private void updateCodecTypeVisible(boolean b) {
        this.codecPrefixLabel.setVisible(b);
        this.codecSpinnerField.setVisible(b);
        this.codecLengthLabel.setVisible(b);
        this.codecPrefixLabel.setVisible(b);
        this.codecPrefixField.setVisible(b);

        updateCodeTypeField(b);
    }

    private void updateCodeTypeField(boolean b) {
        this.codecTypeField.setVisible(b);
        this.codecTypeLabel.setVisible(b);
    }

    private void updateTranscoderVisible(boolean b) {
        this.transField.setVisible(b);
        this.transClientCheckBox.setVisible(b);
        this.transServerCheckBox.setVisible(b);

        this.transSplitLabel.setVisible(b);
        this.srcProtocolLabel.setVisible(b);
        this.srcProtocolField.setVisible(b);

        this.dstProtocolLabel.setVisible(b);
        this.dstProtocolField.setVisible(b);
    }

    private void updateTraceVisible(boolean b) {

        this.traceTypeLabel.setVisible(b);
        this.reporterLabel.setVisible(b);

        this.traceTypeField.setVisible(b);
        this.reporterField.setVisible(b);

        if (b) {
            prepareTraceReporter();
        }
    }

    private void updateFilterVisible(boolean b) {
        this.filterField.setVisible(b);
        this.filterClientCheckBox.setVisible(b);
        this.filterServerCheckBox.setVisible(b);

    }

    public void updateOrganizationVisible(boolean b) {
        this.orgField.setVisible(b);
        this.orgLabel.setVisible(b);
    }

    public void updateGenerateStandardCodeVisible(boolean b, boolean uncheck) {
        this.generateCodeLabel.setVisible(b);

        this.boltCheckBox.setVisible(b);
        this.dubboCheckBox.setVisible(b);
        this.springCloudCheckBox.setVisible(b);

        if (uncheck) {
            this.boltCheckBox.setSelected(false);
            this.dubboCheckBox.setVisible(false);
            this.springCloudCheckBox.setVisible(false);
        }
    }

    private void updateProtocolVisible(boolean b) {
        this.httpField.setVisible(b);

        this.serverPortLabel.setVisible(b);
        this.serverPortField.setVisible(b);

        this.clientPortLabel.setVisible(b);
        this.clientPortField.setVisible(b);

        this.updateGenerateStandardCodeVisible(b, false);

        updatePoolModelVisible(b);

        this.serviceKeyLabel.setVisible(b);
        this.serviceKeyField.setVisible(b);

        this.govPanel.setVisible(b);

        updateCodecTypeVisible(b);
    }

    private void updatePoolModelVisible(boolean b) {
        this.poolModelLabel.setVisible(b);
        this.poolModelField.setVisible(b);
        this.requestIdIsStringCheckBox.setVisible(b);
    }

    public JPanel getContent() {
        return content;
    }

    public JTextField getProtoField() {
        return protoField;
    }

    public JLabel getPluginNameField() {
        return pluginNameField;
    }

    public GoModuleWizardStep.FromType getFromType() {
        return fromType;
    }

    public void setFromType(GoModuleWizardStep.FromType fromType) {
        this.fromType = fromType;
    }

    public LabeledComponent<TextFieldWithBrowseButton> getLocationComponent() {
        return locationComponent;
    }

    public void setLocationComponent(LabeledComponent<TextFieldWithBrowseButton> locationComponent) {
        this.locationComponent = locationComponent;

        if (locationComponent != null) {
            TextFieldWithBrowseButton button = locationComponent.getComponent();
            String location = button.getText();
            if (location != null && location.length() > 0) {
                int index = location.lastIndexOf("/");
                if (index > 0) {
                    String prefix = location.substring(0, index);
                    String pluginName = "plugin-project";

                    String pos = prefix + "/" + pluginName;
                    String project = pos;
                    int next = 1;
                    // choose non exists project
                    while (new File(project).exists()) {
                        project = pos + (next++);
                    }

                    button.setText(project);
                }
            }

        }
    }

    public ValidationInfo validate(GoNewProjectSettings settings) {

        String name = protoField.getText();
        if (name == null || name.length() <= 0) {
            return new ValidationInfo("Plugin Name is required.", this.getContent());
        }

        String orgName = orgField.getText().trim();
        if (fromType == GoModuleWizardStep.FromType.NewProject) {
            if (orgName == null || orgName.length() <= 0) {
                return new ValidationInfo("Organization field is required.", this.getContent());
            }
        }


        PluginType pluginType = (PluginType) this.pluginTypeField.getSelectedItem();
        switch (Objects.requireNonNull(pluginType)) {
            case Protocol: {

                String serverPort = this.serverPortField.getText();
                if (serverPort == null || serverPort.length() <= 0) {
                    return new ValidationInfo("Server port field is required.", this.getContent());
                }

                try {
                    Integer.parseInt(serverPort.trim());
                } catch (Exception ignored) {
                    return new ValidationInfo("Server port should be number.", this.getContent());
                }

                String clientPort = this.clientPortField.getText();
                if (clientPort != null && clientPort.length() > 0) {
                    try {
                        Integer.parseInt(clientPort.trim());
                    } catch (Exception ignored) {
                        return new ValidationInfo("Client port should be number.", this.getContent());
                    }
                }

                // check service key
                String dataId = this.serviceKeyField.getText();
                if (dataId == null || dataId.length() == 0) {
                    return new ValidationInfo("Service Key field is required.", this.getContent());
                }

                break;
            }
            case Filter: {

                if (!this.filterClientCheckBox.isSelected()
                        && !this.filterServerCheckBox.isSelected()) {
                    return new ValidationInfo("Apply Side field is required.", this.getContent());
                }

                break;
            }
            case Transcoder: {

                if (!this.transClientCheckBox.isSelected()
                        && !this.transServerCheckBox.isSelected()) {
                    return new ValidationInfo("Apply Side field is required.", this.getContent());
                }

                break;
            }
            case Trace: {
                String traceType = (String) this.traceTypeField.getSelectedItem();
                if (traceType == null || traceType.length() == 0) {
                    return new ValidationInfo("Trace Type is required.", this.getContent());
                }

                switch (traceType) {
                    case TraceOption.SKY_WALKING:
                    case TraceOption.ZIP_KIN: {
                        String reporter = (String) this.reporterField.getSelectedItem();
                        if (reporter == null || reporter.length() == 0) {
                            return new ValidationInfo("Reporter is required.", this.getContent());
                        }
                        break;
                    }
                }
                break;
            }
        }

        return null;
    }

    public void updatePluginModel(GoNewProjectSettings settings) {

        String name = protoField.getText();
        String orgName = orgField.getText().trim();

        PluginOption option = null;

        PluginType pluginType = (PluginType) this.pluginTypeField.getSelectedItem();
        switch (Objects.requireNonNull(pluginType)) {
            case Protocol: {
                option = settings.protocol;
                settings.protocol.setPluginName(name.trim());
                settings.protocol.setOrganization(orgName.trim());

                settings.protocol.setHttp(this.httpField.isSelected());
                settings.protocol.setInjectHead(this.httpField.isSelected() && this.injectHeadCheckBox.isSelected());


                String serverPort = this.serverPortField.getText();

                try {
                    settings.protocol.setServerPort(Integer.parseInt(serverPort.trim()));
                } catch (Exception ignored) {
                }

                String clientPort = this.clientPortField.getText();
                if (clientPort != null && clientPort.length() > 0) {
                    try {
                        settings.protocol.setClientPort(Integer.parseInt(clientPort.trim()));
                    } catch (Exception ignored) {
                    }
                }

                if (!settings.protocol.isHttp()) {
                    settings.protocol.setPoolMode((PoolMode) this.poolModelField.getSelectedItem());

                    settings.protocol.setStringRequestId(this.requestIdIsStringCheckBox.isSelected());

                    CodecType codecType = (CodecType) this.codecTypeField.getSelectedItem();
                    if (codecType == FixedLength) {

                        int length = 0;
                        if (this.codecSpinnerField.getValue() != null) {
                            length = ((Integer) this.codecSpinnerField.getValue()).intValue();
                        }

                        if (this.codecPrefixField.getText() != null && this.codecPrefixField.getText().length() > 0) {
                            settings.protocol.setCodecOption(
                                    new CodecOption(true, length, this.codecPrefixField.getText().trim()));
                        }
                    }
                }

                // check service key
                String dataId = this.serviceKeyField.getText();

                // service key
                settings.protocol.addRequired(X_MOSN_DATA_ID, COMMA_SPLIT_PATTERN.split(dataId));

                if (this.serviceMethodField.getText() != null) {
                    settings.protocol.addOptional(X_MOSN_METHOD, COMMA_SPLIT_PATTERN.split(this.serviceMethodField.getText()));
                }
                if (this.serviceTraceField.getText() != null) {
                    settings.protocol.addOptional(X_MOSN_TRACE_ID, COMMA_SPLIT_PATTERN.split(this.serviceTraceField.getText()));
                }
                if (this.serviceSpanField.getText() != null) {
                    settings.protocol.addOptional(X_MOSN_SPAN_ID, COMMA_SPLIT_PATTERN.split(this.serviceSpanField.getText()));
                }
                if (this.serviceCallerAppFiled.getText() != null) {
                    settings.protocol.addOptional(X_MOSN_CALLER_APP, COMMA_SPLIT_PATTERN.split(this.serviceCallerAppFiled.getText()));
                }

                ArrayList<ProtocolOption> opts = new ArrayList<>();
                if (this.boltCheckBox.isSelected()) {
                    opts.add(new BoltOptionImpl());
                }
                if (this.dubboCheckBox.isSelected()) {
                    opts.add(new DubboOptionImpl());
                }
                if (this.springCloudCheckBox.isSelected()) {
                    opts.add(new SpringCloudOptionImpl());
                }

                settings.protocol.setEmbedded(opts);

                break;
            }
            case Filter: {
                option = settings.filter;
                settings.filter.setPluginName(name);
                settings.filter.setOrganization(orgName);

                boolean selectAll = this.filterClientCheckBox.isSelected() && this.filterServerCheckBox.isSelected();
                settings.filter.setActiveMode(
                        selectAll ? ActiveMode.ALL : (
                                this.filterClientCheckBox.isSelected()
                                        ? ActiveMode.Client : ActiveMode.Server));

                break;
            }
            case Transcoder: {
                option = settings.transcoder;
                settings.transcoder.setPluginName(name);
                settings.transcoder.setOrganization(orgName);

                settings.transcoder.setActiveMode(
                        this.filterClientCheckBox.isSelected()
                                ? ActiveMode.Client : ActiveMode.Server);

                String src = (String) this.srcProtocolField.getSelectedItem();
                settings.transcoder.setSrcProtocol(src);

                String dst = (String) this.dstProtocolField.getSelectedItem();
                settings.transcoder.setDstProtocol(dst);
                break;
            }
            case Trace: {
                option = settings.trace;
                settings.trace.setPluginName(name);
                settings.trace.setOrganization(orgName);

                String traceType = (String) this.traceTypeField.getSelectedItem();
                settings.trace.setRealTraceType(traceType);

                String reporterType = (String) this.reporterField.getSelectedItem();
                settings.trace.setReporterType(reporterType);

                break;
            }
        }

        if (this.apiFiled.getText() != null) {
            option.setApi(this.apiFiled.getText());
        }
        if (this.packageField.getText() != null) {
            option.setPkg(this.packageField.getText());
        }
    }

    public Runnable getCheckValid() {
        return checkValid;
    }

    public void setCheckValid(Runnable checkValid) {
        this.checkValid = checkValid;

        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (checkValid != null) {
                    checkValid.run();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (checkValid != null) {
                    checkValid.run();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (checkValid != null) {
                    checkValid.run();
                }
            }
        };

        // prepare changed listener
        protoField.getDocument().addDocumentListener(listener);
        orgField.getDocument().addDocumentListener(listener);
        serverPortField.getDocument().addDocumentListener(listener);
        clientPortField.getDocument().addDocumentListener(listener);
        serviceKeyField.getDocument().addDocumentListener(listener);

        codecPrefixField.getDocument().addDocumentListener(listener);
    }

    public PluginType selectedPluginType() {
        return (PluginType) this.pluginTypeField.getSelectedItem();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        content = new JPanel();
        content.setLayout(new GridLayoutManager(14, 17, new Insets(0, 0, 0, 0), -1, -1));
        protoField = new JTextField();
        protoField.setToolTipText("创建mosn插件名称, 全小写字母，用来编译/打包名称，eg: make codec|filter|trans plugin=插件名称");
        content.add(protoField, new GridConstraints(0, 1, 1, 16, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        pluginNameField = new JLabel();
        pluginNameField.setText("Plugin Name:");
        content.add(pluginNameField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverPortLabel = new JLabel();
        serverPortLabel.setText("Server Port:");
        content.add(serverPortLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clientPortLabel = new JLabel();
        clientPortLabel.setText("Client Port:");
        content.add(clientPortLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clientPortField = new JTextField();
        clientPortField.setText("");
        clientPortField.setToolTipText("客户端mosn监听端口, 处理本地客户端app流量");
        content.add(clientPortField, new GridConstraints(4, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        serverPortField = new JTextField();
        serverPortField.setText("");
        serverPortField.setToolTipText("服务端mosn监听端口，处理远端请求流量");
        content.add(serverPortField, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Plugin Type:");
        content.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pluginTypeField = new JComboBox();
        pluginTypeField.setEditable(false);
        pluginTypeField.setEnabled(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        pluginTypeField.setModel(defaultComboBoxModel1);
        content.add(pluginTypeField, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        httpField = new JCheckBox();
        httpField.setEnabled(true);
        httpField.setText("http");
        content.add(httpField, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        boltCheckBox = new JCheckBox();
        boltCheckBox.setText("bolt");
        content.add(boltCheckBox, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateCodeLabel = new JLabel();
        generateCodeLabel.setText(" Generate standard protocol code");
        content.add(generateCodeLabel, new GridConstraints(3, 3, 1, 9, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        content.add(spacer1, new GridConstraints(4, 8, 1, 9, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        content.add(spacer2, new GridConstraints(3, 12, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        filterField = new JLabel();
        filterField.setText("Apply Side:");
        content.add(filterField, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        filterClientCheckBox = new JCheckBox();
        filterClientCheckBox.setSelected(true);
        filterClientCheckBox.setText("client");
        content.add(filterClientCheckBox, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        filterServerCheckBox = new JCheckBox();
        filterServerCheckBox.setSelected(true);
        filterServerCheckBox.setText("server");
        content.add(filterServerCheckBox, new GridConstraints(7, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        transField = new JLabel();
        transField.setText("Apply Side:");
        content.add(transField, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        transClientCheckBox = new JCheckBox();
        transClientCheckBox.setSelected(true);
        transClientCheckBox.setText("client");
        content.add(transClientCheckBox, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        transServerCheckBox = new JCheckBox();
        transServerCheckBox.setText("server");
        content.add(transServerCheckBox, new GridConstraints(8, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serviceKeyLabel = new JLabel();
        serviceKeyLabel.setText("Service Key:");
        content.add(serviceKeyLabel, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serviceKeyField = new JTextField();
        serviceKeyField.setText("");
        serviceKeyField.setToolTipText("服务标识: 用于从请求中提取该字段用于组成dataId，支持服务调用、服务粒度限流、熔断、降级、故障注入、故障隔离能力");
        content.add(serviceKeyField, new GridConstraints(10, 1, 1, 16, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        orgLabel = new JLabel();
        orgLabel.setText("Organization:");
        content.add(orgLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        orgField = new JTextField();
        orgField.setText("");
        orgField.setToolTipText("go.mod模块名前缀，和插件名构成完整模块名。填写示例：github.com/zonghaishang");
        content.add(orgField, new GridConstraints(1, 1, 1, 16, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        govPanel = new JPanel();
        govPanel.setLayout(new GridLayoutManager(5, 5, new Insets(10, 10, 10, 10), -1, -1));
        content.add(govPanel, new GridConstraints(11, 0, 1, 17, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        govPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Service Method Key:");
        govPanel.add(label2, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serviceMethodField = new JTextField();
        serviceMethodField.setToolTipText("服务方法标识：用于从请求中提取该字段用于组成调用方法名称，支持方法级别粒度限流、熔断、降级、故障注入、故障隔离能力");
        govPanel.add(serviceMethodField, new GridConstraints(1, 3, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Service Trace Key:");
        govPanel.add(label3, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serviceTraceField = new JTextField();
        serviceTraceField.setToolTipText("服务trace标识：用于从请求中提取该字段用于组成链路trace唯一标识");
        govPanel.add(serviceTraceField, new GridConstraints(2, 3, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Service Span Key:");
        govPanel.add(label4, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serviceSpanField = new JTextField();
        serviceSpanField.setToolTipText("服务span标识：用于从请求中提取该字段用于组成链路span调用顺序信息");
        govPanel.add(serviceSpanField, new GridConstraints(3, 3, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        serviceCallerAppFiled = new JTextField();
        serviceCallerAppFiled.setToolTipText("服务调用方app标识：用于从请求中提取该字段用于组成调用方app名称，支持方法级别粒度限流、熔断、降级、故障注入、故障隔离能力");
        govPanel.add(serviceCallerAppFiled, new GridConstraints(4, 3, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Service Caller App Key:");
        govPanel.add(label5, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Service Governance capability ( Optional )");
        govPanel.add(label6, new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        content.add(spacer3, new GridConstraints(13, 12, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 5, new Insets(10, 10, 10, 10), -1, -1));
        content.add(panel1, new GridConstraints(12, 0, 1, 17, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label7 = new JLabel();
        label7.setText("Mosn Api Version:");
        panel1.add(label7, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        apiFiled = new JTextField();
        apiFiled.setToolTipText("插件依赖api的版本");
        panel1.add(apiFiled, new GridConstraints(1, 3, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Mosn Package Version:");
        panel1.add(label8, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        packageField = new JTextField();
        packageField.setToolTipText("插件依赖pkg的版本");
        panel1.add(packageField, new GridConstraints(2, 3, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Plugin Dependencies ( Optional )");
        panel1.add(label9, new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        poolModelLabel = new JLabel();
        poolModelLabel.setText("Pool Mode:");
        content.add(poolModelLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        poolModelField = new JComboBox();
        content.add(poolModelField, new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        requestIdIsStringCheckBox = new JCheckBox();
        requestIdIsStringCheckBox.setText("request id is string type");
        content.add(requestIdIsStringCheckBox, new GridConstraints(5, 3, 1, 7, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        codecTypeLabel = new JLabel();
        codecTypeLabel.setText("Codec Type:");
        content.add(codecTypeLabel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        codecTypeField = new JComboBox();
        content.add(codecTypeField, new GridConstraints(6, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        codecLengthLabel = new JLabel();
        codecLengthLabel.setText("Fixed Length:");
        content.add(codecLengthLabel, new GridConstraints(6, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        codecSpinnerField = new JSpinner();
        content.add(codecSpinnerField, new GridConstraints(6, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        content.add(spacer4, new GridConstraints(6, 8, 1, 9, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        dubboCheckBox = new JCheckBox();
        dubboCheckBox.setText("dubbo");
        content.add(dubboCheckBox, new GridConstraints(4, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        springCloudCheckBox = new JCheckBox();
        springCloudCheckBox.setText("spring cloud");
        content.add(springCloudCheckBox, new GridConstraints(4, 5, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        codecPrefixLabel = new JLabel();
        codecPrefixLabel.setText("Append Prefix:");
        content.add(codecPrefixLabel, new GridConstraints(6, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        codecPrefixField = new JTextField();
        codecPrefixField.setText("0");
        content.add(codecPrefixField, new GridConstraints(6, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        urlLabel = new LinkLabel();
        urlLabel.setHorizontalAlignment(4);
        urlLabel.setText("plugin development help guide");
        content.add(urlLabel, new GridConstraints(2, 5, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        injectHeadCheckBox = new JCheckBox();
        injectHeadCheckBox.setText("inject head");
        content.add(injectHeadCheckBox, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dstProtocolLabel = new JLabel();
        dstProtocolLabel.setText("Dst Protocol:");
        content.add(dstProtocolLabel, new GridConstraints(8, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        srcProtocolField = new JComboBox();
        srcProtocolField.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("dubbo");
        defaultComboBoxModel2.addElement("bolt");
        defaultComboBoxModel2.addElement("springcloud");
        srcProtocolField.setModel(defaultComboBoxModel2);
        content.add(srcProtocolField, new GridConstraints(8, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        srcProtocolLabel = new JLabel();
        srcProtocolLabel.setText("Src Protocol:");
        content.add(srcProtocolLabel, new GridConstraints(8, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        content.add(spacer5, new GridConstraints(8, 8, 1, 9, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        dstProtocolField = new JComboBox();
        dstProtocolField.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        defaultComboBoxModel3.addElement("springcloud");
        defaultComboBoxModel3.addElement("dubbo");
        defaultComboBoxModel3.addElement("bolt");
        dstProtocolField.setModel(defaultComboBoxModel3);
        content.add(dstProtocolField, new GridConstraints(8, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        transSplitLabel = new JLabel();
        transSplitLabel.setText("    |    ");
        content.add(transSplitLabel, new GridConstraints(8, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        traceTypeLabel = new JLabel();
        traceTypeLabel.setText("Trace Type:");
        content.add(traceTypeLabel, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        traceTypeField = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        defaultComboBoxModel4.addElement("sky-walking");
        defaultComboBoxModel4.addElement("zipkin");
        traceTypeField.setModel(defaultComboBoxModel4);
        content.add(traceTypeField, new GridConstraints(9, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reporterLabel = new JLabel();
        reporterLabel.setText("Reporter:");
        content.add(reporterLabel, new GridConstraints(9, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reporterField = new JComboBox();
        content.add(reporterField, new GridConstraints(9, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(transClientCheckBox);
        buttonGroup.add(transServerCheckBox);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return content;
    }

}
