package io.mosn.coder.intellij.view;

import javax.swing.*;
import javax.swing.plaf.ListUI;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class MyBasicComboPopup extends BasicComboPopup {
    /**
     * Constructs a new instance of {@code BasicComboPopup}.
     *
     * @param combo an instance of {@code JComboBox}
     */
    public MyBasicComboPopup(JComboBox<Object> combo) {
        super(combo);

        if (getList() != null) {
            MouseListener[] ls = getList().getListeners(MouseListener.class);
            if (ls != null) {
                for (MouseListener l : ls) {
                    if (l.getClass().getName().contains("BasicListUI")) {
                        getList().removeMouseListener(l);
                    }
                }
            }
        }

        combo.addMouseListener(createListMouseListener());
    }

    protected MouseListener createListMouseListener() {
        return new DefaultMouseListener();
    }

    private class DefaultMouseListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getSource() != MyBasicComboPopup.this.list) {
                if (SwingUtilities.isLeftMouseButton(e) && MyBasicComboPopup.this.comboBox.isEnabled()) {
                    if (MyBasicComboPopup.this.comboBox.isEditable()) {
                        Component comp = MyBasicComboPopup.this.comboBox.getEditor().getEditorComponent();
                        if (!(comp instanceof JComponent) || ((JComponent) comp).isRequestFocusEnabled()) {
                            comp.requestFocus();
                        }
                    } else if (MyBasicComboPopup.this.comboBox.isRequestFocusEnabled()) {
                        MyBasicComboPopup.this.comboBox.requestFocus();
                    }

                    MyBasicComboPopup.this.show();
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getSource() == MyBasicComboPopup.this.list) {
                if (MyBasicComboPopup.this.list.getModel().getSize() > 0) {
                    if (MyBasicComboPopup.this.comboBox.getSelectedIndex() == MyBasicComboPopup.this.list.getSelectedIndex()) {
                        MyBasicComboPopup.this.comboBox.getEditor().setItem(MyBasicComboPopup.this.list.getSelectedValue());
                    }

                    MyBasicComboPopup.this.comboBox.setSelectedIndex(MyBasicComboPopup.this.list.getSelectedIndex());
                }

                MyBasicComboPopup.this.comboBox.setPopupVisible(true);
                if (MyBasicComboPopup.this.comboBox.isEditable() && MyBasicComboPopup.this.comboBox.getEditor() != null) {
                    MyBasicComboPopup.this.comboBox.configureEditor(MyBasicComboPopup.this.comboBox.getEditor(), MyBasicComboPopup.this.comboBox.getSelectedItem());
                }

            } else {
                Component source = (Component) e.getSource();
                Dimension size = source.getSize();
                Rectangle bounds = new Rectangle(0, 0, size.width, size.height);
                if (!bounds.contains(e.getPoint())) {
                    MouseEvent newEvent = MyBasicComboPopup.this.convertMouseEvent(e);
                    Point location = newEvent.getPoint();
                    Rectangle r = new Rectangle();
                    MyBasicComboPopup.this.list.computeVisibleRect(r);
                    if (r.contains(location)) {
                        if (MyBasicComboPopup.this.comboBox.getSelectedIndex() == MyBasicComboPopup.this.list.getSelectedIndex()) {
                            MyBasicComboPopup.this.comboBox.getEditor().setItem(MyBasicComboPopup.this.list.getSelectedValue());
                        }

                        MyBasicComboPopup.this.comboBox.setSelectedIndex(MyBasicComboPopup.this.list.getSelectedIndex());
                    }

                    MyBasicComboPopup.this.comboBox.setPopupVisible(true);
                }

                MyBasicComboPopup.this.hasEntered = false;
                MyBasicComboPopup.this.stopAutoScrolling();
            }

            if (!e.isConsumed())
                e.consume();
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

//        protected JList<Object> createList() {
//            return new MyList(comboBox.getModel());
//        }
    }

    class MyList extends JList {
        public MyList(Object model) {
            super((ListModel) model);
        }

        @Override
        public void setUI(ListUI ui) {
            // super.setUI(new MyBasicListUI(ui));
        }
    }
}
