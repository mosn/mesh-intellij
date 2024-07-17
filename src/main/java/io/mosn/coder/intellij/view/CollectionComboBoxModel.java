package io.mosn.coder.intellij.view;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class CollectionComboBoxModel<T>  extends CollectionListModel<T> implements ComboBoxModel<T> {
    protected T mySelection;

    public CollectionComboBoxModel() {
        super();
        mySelection = null;
    }

    public CollectionComboBoxModel(@NotNull List<T> items) {
        this(items, getFirstItem(items));
    }

    public CollectionComboBoxModel(@NotNull List<T> items, @Nullable T selection) {
        super(items, true);
        mySelection = selection;
    }

    @Override
    public void setSelectedItem(@Nullable Object item) {
        if (mySelection != item) {
            @SuppressWarnings("unchecked") T t = (T)item;
            mySelection = t;
            update();
        }
    }

    @Override
    @Nullable
    public Object getSelectedItem() {
        return mySelection;
    }

    @Nullable
    public T getSelected() {
        return mySelection;
    }

    public void update() {
        super.fireContentsChanged(this, -1, -1);
    }

    public static <T> T getFirstItem(@Nullable List<? extends T> items) {
        return items == null || items.isEmpty() ? null : items.get(0);
    }

}