package io.mosn.coder.intellij.view;

import io.mosn.coder.intellij.option.CodecType;

import javax.swing.*;
import java.util.ArrayList;

public class CodecTypeModel extends AbstractListModel<CodecType> implements ComboBoxModel<CodecType> {

    CodecType selected = CodecType.Customize;

    public CodecTypeModel() {
        this.types.add(CodecType.Customize);
        this.types.add(CodecType.FixedLength);
    }

    ArrayList<CodecType> types = new ArrayList<>();

    @Override
    public int getSize() {
        return types.size();
    }

    @Override
    public CodecType getElementAt(int index) {
        return types.get(index);
    }

    @Override
    public void setSelectedItem(Object anItem) {
        selected = (CodecType) anItem;
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }
}