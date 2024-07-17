package io.mosn.coder.intellij.view;

import com.intellij.codeInsight.lookup.impl.LookupCellRenderer;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

public class CheckBoxModel extends AbstractListModel<JCheckBox> implements ComboBoxModel<JCheckBox> {

    private JCheckBox selected;

    private JComboBox<JCheckBox> parent;

    public CheckBoxModel(JComboBox<JCheckBox> parent) {

        this.parent = parent;
        this.types.add(new JCheckBox("meshserver", true));
        this.types.add(new JCheckBox("operator", true));
        this.types.add(new JCheckBox("mosn", true));
        this.types.add(new JCheckBox("mysql", true));
        this.types.add(new JCheckBox("nacos", true));
        this.types.add(new JCheckBox("prometheus", true));
        this.types.add(new JCheckBox("citadel", false));
        this.types.add(new JCheckBox("dubbo", false));

        this.selected = this.types.get(0);
    }

    ArrayList<JCheckBox> types = new ArrayList<>();

    @Override
    public int getSize() {
        return types.size();
    }

    @Override
    public JCheckBox getElementAt(int index) {
        return types.get(index);
    }

    @Override
    public void setSelectedItem(Object anItem) {
        selected = (JCheckBox) anItem;

        //selected.setSelected(!selected.isSelected());
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }

    final class CheckBoxCellRenderer implements ListCellRenderer<JCheckBox> {
        private final Border mySelectedBorder;
        private final Border myBorder;
        private final Insets myBorderInsets;

        CheckBoxCellRenderer() {
            mySelectedBorder = UIManager.getBorder("List.focusCellHighlightBorder");
            myBorderInsets = mySelectedBorder.getBorderInsets(new JCheckBox());
            myBorder = new EmptyBorder(myBorderInsets);
        }

        @Override
        public Component getListCellRendererComponent(JList list, JCheckBox checkbox, int index, boolean isSelected, boolean cellHasFocus) {
            Color textColor = getForeground(isSelected);
            Color backgroundColor = getBackground(isSelected);
            Font font = CheckBoxModel.this.parent.getFont();
            checkbox.setBackground(backgroundColor);
            checkbox.setForeground(textColor);
            checkbox.setEnabled(isEnabled(index));
            checkbox.setFont(font);
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(false);
            checkbox.setOpaque(true);

            checkbox.setBorder(isSelected ? mySelectedBorder : myBorder);

            boolean isRollOver = checkbox.getModel().isRollover();
            checkbox.getModel().setRollover(isRollOver);

            return checkbox;
        }

        boolean isEnabled(int index) {
            return true;
        }

        Color getBackground(final boolean isSelected) {
            return isSelected ? LookupCellRenderer.SELECTED_BACKGROUND_COLOR : parent.getBackground();
        }

        Color getForeground(final boolean isSelected) {
            return isSelected ? JBColor.foreground() : parent.getForeground();
        }

        @NotNull
        private Insets getBorderInsets() {
            return myBorderInsets;
        }
    }


}