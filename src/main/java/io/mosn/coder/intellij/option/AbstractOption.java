package io.mosn.coder.intellij.option;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author yiji@apache.org
 */
public abstract class AbstractOption extends PluginOption {

    public static final OptionKey<String> X_MOSN_DATA_ID = new OptionKey<>("x-mosn-data-id");

    public static final OptionKey<String> X_MOSN_METHOD = new OptionKey<>("x-mosn-method");

    public static final OptionKey<String> X_MOSN_TRACE_ID = new OptionKey<>("x-mosn-trace-id");

    public static final OptionKey<String> X_MOSN_SPAN_ID = new OptionKey<>("x-mosn-span-id");

    public static final OptionKey<String> X_MOSN_TARGET_APP = new OptionKey<>("x-mosn-target-app");

    public static final OptionKey<String> X_MOSN_CALLER_APP = new OptionKey<>("x-mosn-caller-app");

    public static final OptionKey<String> X_MOSN_CALLER_IP = new OptionKey<>("x-mosn-caller-ip");

    public enum ActiveMode {
        /**
         * only generate client config
         */
        Client,
        /**
         * only generate server config
         */
        Server,
        ALL,
    }

    public static class CodecOption {

        public boolean fixedLengthCodec;

        public int length;

        /**
         * append prefix character.
         */
        public String prefix;

        public CodecOption(boolean fixedLengthCodec, int length, String prefix) {
            this.fixedLengthCodec = fixedLengthCodec;
            this.length = length;
            this.prefix = prefix;
        }
    }

    public static class OptionKey<T extends Comparable> implements Comparable<OptionKey<T>> {
        T key;

        private OptionKey(T key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OptionKey optionKey = (OptionKey) o;
            return Objects.equals(key, optionKey.key);
        }

        public T getName() {
            return key;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }

        @Override
        public int compareTo(@NotNull OptionKey<T> o) {
            return this.key.compareTo(o.key);
        }
    }

    public static class OptionValue<V extends Comparable> {

        Set<V> items = new HashSet<>();

        void add(V item) {
            items.add(item);
        }

        void remove(V item) {
            items.remove(item);
        }

        void removeAll() {
            items.clear();
        }

        public Set<V> getItems() {
            return items;
        }

        public V first() {
            if (items != null) {
                Iterator<V> i = items.iterator();
                return i.hasNext() ? i.next() : null;
            }

            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OptionValue<?> that = (OptionValue<?>) o;
            return items.equals(that.items);
        }

        @Override
        public int hashCode() {
            return Objects.hash(items);
        }
    }

    protected String name;

    protected String organization;

    protected TreeMap<OptionKey<String>, OptionValue<String>> required = new TreeMap<>();

    protected TreeMap<OptionKey<String>, OptionValue<String>> optional = new TreeMap<>();

    @Override
    public String getPluginName() {
        return this.name;
    }

    public void setPluginName(String name) {
        this.name = name;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }


    public void addRequired(OptionKey<String> key, String... headers) {
        OptionValue<String> items = required.get(key);
        if (items == null) {
            items = new OptionValue<>();
            required.put(key, items);
        }

        removeRequired(key);

        if (headers != null) {
            for (String head : headers) {
                if (head != null && head.trim().length() > 0) {
                    items.add(head);
                }
            }
        }
    }

    public Set<Map.Entry<OptionKey<String>, OptionValue<String>>> getRequiredKeys() {
        return required.entrySet();
    }

    public Set<Map.Entry<OptionKey<String>, OptionValue<String>>> getOptionalKeys() {
        return optional.entrySet();
    }

    public void removeRequired(OptionKey<String> key) {
        OptionValue<String> items = required.get(key);
        if (items != null) {
            items.removeAll();
        }
    }

    public void removeOptional(OptionKey<String> key) {
        OptionValue<String> items = optional.get(key);
        if (items != null) {
            items.removeAll();
        }
    }

    public void addOptional(OptionKey<String> key, String... headers) {
        OptionValue<String> items = optional.get(key);
        if (items == null) {
            items = new OptionValue<>();
            optional.put(key, items);
        }

        removeOptional(key);

        if (headers != null) {
            for (String head : headers) {
                if (head != null && head.trim().length() > 0) {
                    items.add(head);
                }
            }
        }
    }

    @Override
    void destroy() {
    }
}
