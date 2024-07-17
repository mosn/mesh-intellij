package io.mosn.coder.plugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class PluginTable {

    private Row header = new Row();

    private List<Row> rows = new ArrayList<>();

    private List<Integer> maxColumns = new ArrayList<>();

    public PluginTable addHeader(String name) {
        this.header.appendColumn(name);
        return this;
    }

    public PluginTable addRow(Row row) {
        this.rows.add(row);
        return this;
    }

    public String pretty() {

        StringBuilder buff = new StringBuilder();

        /**
         * insert default max column
         */
        if (maxColumns.isEmpty()) {
            for (Column column : header.columns) {
                maxColumns.add(column.length());
            }
        }

        if (maxColumns.isEmpty() && !this.rows.isEmpty()) {
            /**
             * Without a header, take the first row of the data column
             */
            for (Column column : this.rows.get(0).columns) {
                maxColumns.add(column.length());
            }
        }

        /**
         * update max column length.
         */
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row current = rows.get(i);
            for (int j = 0, m = current.columns.size(); j < m; j++) {
                Integer max = Math.max(maxColumns.get(j), current.columns.get(j).length());
                maxColumns.set(j, max);
            }
        }

        /**
         * print header
         */

        int count = 0;
        for (int i = 0, n = header.columns.size(); i < n; i++) {
            if (buff.length() > 0) {
                buff.append("    ");
            }

            wrap(header.columns.get(i), maxColumns.get(i), buff);

            count += maxColumns.get(i);
        }

        buff.append("\n");

        /**
         * print -----
         */

        if (count == 0) {
            if (this.header.columns.isEmpty() && !this.rows.isEmpty()) {
                /**
                 * Without a header, take the first row of the data column
                 */
                for (int i = 0, n = this.rows.get(0).columns.size(); i < n; i++) {
                    count += maxColumns.get(i);
                }
            }
        }

        for (int i = 0, n = count + (maxColumns.size() - 1) * 4; i < n; i++) {
            buff.append("-");
        }

        buff.append("\n");

        /**
         * print rows
         */
        String prev = null, last = null;

        for (int i = 0, n = rows.size(); i < n; i++) {
            Row current = rows.get(i);
            for (int j = 0, m = current.columns.size(); j < m; j++) {

                if (j > 0) {
                    buff.append("    ");
                }
                Column column = current.columns.get(j);

                if (column.split) {

                    /**
                     * first time ?
                     */
                    if (last == null) {
                        prev = column.value;
                    }

                    last = column.value;

                    if (!last.equals(prev)) {
                        for (int k = 0, l = count + (maxColumns.size() - 1) * 4; k < l; k++) {
                            buff.append("-");
                        }
                        buff.append("\n");
                    }
                }

                wrap(column, maxColumns.get(j), buff);
            }

            prev = last;

            buff.append("\n");
        }

        return buff.toString();
    }

    private void wrap(Column column, Integer width, StringBuilder buff) {
        int padding = width - column.length();
        buff.append(column.value);
        for (int i = 0; i < padding; i++) {
            buff.append(" ");
        }
    }

    public static class Row {

        private int maxColumn;

        private List<Column> columns = new ArrayList<>();

        public Row appendColumn(String value) {
            return appendColumn(value, false);
        }

        public Row appendColumn(String value, boolean split) {

            Column column = new Column(value, split);
            this.columns.add(column);

            maxColumn = Math.max(maxColumn, column.length());

            return this;
        }
    }

    static class Column {
        private String value;

        private boolean split;

        public Column(String value) {
            this(value, false);
        }

        public Column(String value, boolean split) {
            this.value = value == null ? "" : value;
            this.split = split;
        }

        int length() {
            return this.value.length();
        }
    }

}
