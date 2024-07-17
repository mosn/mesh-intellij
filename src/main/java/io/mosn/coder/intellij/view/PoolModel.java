package io.mosn.coder.intellij.view;

import io.mosn.coder.intellij.option.PoolMode;

import javax.swing.*;
import java.util.ArrayList;

public class PoolModel extends AbstractListModel<PoolMode> implements ComboBoxModel<PoolMode> {

    PoolMode selected = PoolMode.Multiplex;

    public PoolModel() {
        this.types.add(PoolMode.Multiplex);
        this.types.add(PoolMode.PingPong);
    }

    ArrayList<PoolMode> types = new ArrayList<>();

    @Override
    public int getSize() {
        return types.size();
    }

    @Override
    public PoolMode getElementAt(int index) {
        return types.get(index);
    }

    @Override
    public void setSelectedItem(Object anItem) {
        selected = (PoolMode) anItem;
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }
}
