package io.mosn.coder.intellij.view;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.EnvVariablesTable;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.table.TableView;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.io.IdeUtilIoBundle;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.*;

public class MiniConfigDialog  extends DialogWrapper {
    private final MiniConfigBrowseButton myParent;
    private final EnvVariablesTable myUserTable;

    private JPanel myWholePanel;

    protected MiniConfigDialog(MiniConfigBrowseButton parent) {
        super(parent, true);
        myParent = parent;

        Map<String, String> userMap = new LinkedHashMap<String, String>(myParent.getEnvs());

        Map<String, String> parentMap = new TreeMap<>(new GeneralCommandLine().getParentEnvironment());

        //myParent.myParentDefaults.putAll(parentMap);
        for (Iterator<Map.Entry<String, String>> iterator = userMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, String> entry = iterator.next();
            if (parentMap.containsKey(entry.getKey())) { //User overrides system variable, we have to show it in 'parent' table as bold
                parentMap.put(entry.getKey(), entry.getValue());
                iterator.remove();
            }
        }

        List<EnvironmentVariable> userList = MiniConfigBrowseButton.convertToVariables(userMap, false);
        myUserTable = new MiniConfigDialog.MyEnvVariablesTable(userList, true);


        JLabel label = new JLabel(ExecutionBundle.message("env.vars.user.title"));
        label.setLabelFor(myUserTable.getTableView().getComponent());

        myWholePanel = new JPanel(new MigLayout("fill, ins 0, gap 0, hidemode 3"));
        myWholePanel.add(label, "hmax pref, wrap");
        myWholePanel.add(myUserTable.getComponent(), "push, grow, wrap, gaptop 5");


        setTitle(ExecutionBundle.message("environment.variables.dialog.title"));
        init();
    }



    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "EnvironmentVariablesDialog";
    }

    @NotNull
    @Override
    protected JComponent createCenterPanel() {
        return myWholePanel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        for (EnvironmentVariable variable : myUserTable.getEnvironmentVariables()) {
            String name = variable.getName(), value = variable.getValue();
            if (StringUtil.isEmpty(name) && StringUtil.isEmpty(value)) continue;

            if (!EnvironmentUtil.isValidName(name)) {
                return new ValidationInfo(IdeUtilIoBundle.message("run.configuration.invalid.env.name", name));
            }
            if (!EnvironmentUtil.isValidValue(value)) {
                return new ValidationInfo(IdeUtilIoBundle.message("run.configuration.invalid.env.value", name, value));
            }
        }
        return super.doValidate();
    }

    @Override
    protected void doOKAction() {
        myUserTable.stopEditing();
        final Map<String, String> envs = new LinkedHashMap<>();
        for (EnvironmentVariable variable : myUserTable.getEnvironmentVariables()) {
            if (StringUtil.isEmpty(variable.getName()) && StringUtil.isEmpty(variable.getValue())) continue;
            envs.put(variable.getName(), variable.getValue());
        }

         myParent.setEnvs(envs);

        super.doOKAction();
    }

    private class MyEnvVariablesTable extends EnvVariablesTable {
        private final boolean myUserList;

        MyEnvVariablesTable(List<EnvironmentVariable> list, boolean userList) {
            myUserList = userList;
            TableView<EnvironmentVariable> tableView = getTableView();
            tableView.setVisibleRowCount(JBTable.PREFERRED_SCROLLABLE_VIEWPORT_HEIGHT_IN_ROWS);
            setValues(list);
            setPasteActionEnabled(myUserList);
        }

        @Nullable
        @Override
        protected AnActionButtonRunnable createAddAction() {
            return myUserList ? super.createAddAction() : null;
        }

        @Nullable
        @Override
        protected AnActionButtonRunnable createRemoveAction() {
            return myUserList ? super.createRemoveAction() : null;
        }

        @Override
        protected AnActionButton @NotNull [] createExtraActions() {
            return super.createExtraActions();
        }

        @Override
        protected ListTableModel createListModel() {
            return new ListTableModel(new MiniConfigDialog.MyEnvVariablesTable.MyNameColumnInfo(), new MiniConfigDialog.MyEnvVariablesTable.MyValueColumnInfo());
        }

        protected class MyNameColumnInfo extends NameColumnInfo {
            private final DefaultTableCellRenderer myModifiedRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value,
                                                               boolean isSelected,
                                                               boolean hasFocus,
                                                               int row,
                                                               int column) {
                    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    component.setEnabled(table.isEnabled() && (hasFocus || isSelected));
                    return component;
                }
            };

            @Override
            public TableCellRenderer getCustomizedRenderer(EnvironmentVariable o, TableCellRenderer renderer) {
                return o.getNameIsWriteable() ? renderer : myModifiedRenderer;
            }
        }

        protected class MyValueColumnInfo extends ValueColumnInfo {
            private final DefaultTableCellRenderer myModifiedRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value,
                                                               boolean isSelected,
                                                               boolean hasFocus,
                                                               int row,
                                                               int column) {
                    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    component.setFont(component.getFont().deriveFont(Font.BOLD));
                    if (!hasFocus && !isSelected) {
                        component.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
                    }
                    return component;
                }
            };

            @Override
            public boolean isCellEditable(EnvironmentVariable environmentVariable) {
                return true;
            }

            @Override
            public TableCellRenderer getCustomizedRenderer(EnvironmentVariable o, TableCellRenderer renderer) {
                return renderer;//myParent.isModifiedSysEnv(o) ? myModifiedRenderer : renderer;
            }
        }
    }
}
