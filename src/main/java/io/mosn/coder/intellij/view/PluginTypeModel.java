package io.mosn.coder.intellij.view;

import io.mosn.coder.intellij.option.PluginType;

import javax.swing.*;
import java.util.ArrayList;

public class PluginTypeModel extends AbstractListModel<PluginType> implements ComboBoxModel<PluginType> {

    PluginType selected = PluginType.Protocol;

    public PluginTypeModel() {
        this.types.add(PluginType.Protocol);
        this.types.add(PluginType.Filter);
        this.types.add(PluginType.Transcoder);

        this.types.add(PluginType.Trace);
    }

    ArrayList<PluginType> types = new ArrayList<>();

    @Override
    public int getSize() {
        return types.size();
    }

    @Override
    public PluginType getElementAt(int index) {
        return types.get(index);
    }

    @Override
    public void setSelectedItem(Object anItem) {
        selected = (PluginType) anItem;
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }
}
