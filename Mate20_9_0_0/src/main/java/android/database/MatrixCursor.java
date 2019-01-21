package android.database;

import java.util.ArrayList;

public class MatrixCursor extends AbstractCursor {
    private final int columnCount;
    private final String[] columnNames;
    private Object[] data;
    private int rowCount;

    public class RowBuilder {
        private final int endIndex;
        private int index;
        private final int row;

        RowBuilder(int row) {
            this.row = row;
            this.index = MatrixCursor.this.columnCount * row;
            this.endIndex = this.index + MatrixCursor.this.columnCount;
        }

        public RowBuilder add(Object columnValue) {
            if (this.index != this.endIndex) {
                Object[] access$100 = MatrixCursor.this.data;
                int i = this.index;
                this.index = i + 1;
                access$100[i] = columnValue;
                return this;
            }
            throw new CursorIndexOutOfBoundsException("No more columns left.");
        }

        public RowBuilder add(String columnName, Object value) {
            for (int i = 0; i < MatrixCursor.this.columnNames.length; i++) {
                if (columnName.equals(MatrixCursor.this.columnNames[i])) {
                    MatrixCursor.this.data[(this.row * MatrixCursor.this.columnCount) + i] = value;
                }
            }
            return this;
        }
    }

    public MatrixCursor(String[] columnNames, int initialCapacity) {
        this.rowCount = 0;
        this.columnNames = columnNames;
        this.columnCount = columnNames.length;
        if (initialCapacity < 1) {
            initialCapacity = 1;
        }
        this.data = new Object[(this.columnCount * initialCapacity)];
    }

    public MatrixCursor(String[] columnNames) {
        this(columnNames, 16);
    }

    private Object get(int column) {
        if (column < 0 || column >= this.columnCount) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Requested column: ");
            stringBuilder.append(column);
            stringBuilder.append(", # of columns: ");
            stringBuilder.append(this.columnCount);
            throw new CursorIndexOutOfBoundsException(stringBuilder.toString());
        } else if (this.mPos < 0) {
            throw new CursorIndexOutOfBoundsException("Before first row.");
        } else if (this.mPos < this.rowCount) {
            return this.data[(this.mPos * this.columnCount) + column];
        } else {
            throw new CursorIndexOutOfBoundsException("After last row.");
        }
    }

    public RowBuilder newRow() {
        int row = this.rowCount;
        this.rowCount = row + 1;
        ensureCapacity(this.rowCount * this.columnCount);
        return new RowBuilder(row);
    }

    public void addRow(Object[] columnValues) {
        if (columnValues.length == this.columnCount) {
            int start = this.rowCount;
            this.rowCount = start + 1;
            start *= this.columnCount;
            ensureCapacity(this.columnCount + start);
            System.arraycopy(columnValues, 0, this.data, start, this.columnCount);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("columnNames.length = ");
        stringBuilder.append(this.columnCount);
        stringBuilder.append(", columnValues.length = ");
        stringBuilder.append(columnValues.length);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void addRow(Iterable<?> columnValues) {
        int start = this.rowCount * this.columnCount;
        int end = this.columnCount + start;
        ensureCapacity(end);
        if (columnValues instanceof ArrayList) {
            addRow((ArrayList) columnValues, start);
            return;
        }
        int current = start;
        Object[] localData = this.data;
        for (Object columnValue : columnValues) {
            if (current != end) {
                int current2 = current + 1;
                localData[current] = columnValue;
                current = current2;
            } else {
                throw new IllegalArgumentException("columnValues.size() > columnNames.length");
            }
        }
        if (current == end) {
            this.rowCount++;
            return;
        }
        throw new IllegalArgumentException("columnValues.size() < columnNames.length");
    }

    private void addRow(ArrayList<?> columnValues, int start) {
        int size = columnValues.size();
        if (size == this.columnCount) {
            this.rowCount++;
            Object[] localData = this.data;
            for (int i = 0; i < size; i++) {
                localData[start + i] = columnValues.get(i);
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("columnNames.length = ");
        stringBuilder.append(this.columnCount);
        stringBuilder.append(", columnValues.size() = ");
        stringBuilder.append(size);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void ensureCapacity(int size) {
        if (size > this.data.length) {
            Object[] oldData = this.data;
            int newSize = this.data.length * 2;
            if (newSize < size) {
                newSize = size;
            }
            this.data = new Object[newSize];
            System.arraycopy(oldData, 0, this.data, 0, oldData.length);
        }
    }

    public int getCount() {
        return this.rowCount;
    }

    public String[] getColumnNames() {
        return this.columnNames;
    }

    public String getString(int column) {
        Object value = get(column);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public short getShort(int column) {
        Object value = get(column);
        if (value == null) {
            return (short) 0;
        }
        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }
        return Short.parseShort(value.toString());
    }

    public int getInt(int column) {
        Object value = get(column);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    public long getLong(int column) {
        Object value = get(column);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    public float getFloat(int column) {
        Object value = get(column);
        if (value == null) {
            return 0.0f;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return Float.parseFloat(value.toString());
    }

    public double getDouble(int column) {
        Object value = get(column);
        if (value == null) {
            return 0.0d;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    public byte[] getBlob(int column) {
        return (byte[]) get(column);
    }

    public int getType(int column) {
        return DatabaseUtils.getTypeOfObject(get(column));
    }

    public boolean isNull(int column) {
        return get(column) == null;
    }
}
