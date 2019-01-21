package android.database.sqlite;

public class SQLInfo {
    private long primaryKey;
    private String table;

    public SQLInfo(String mTable, long mPrimaryKey) {
        this.table = mTable;
        this.primaryKey = mPrimaryKey;
    }

    public String getTable() {
        return this.table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public long getPrimaryKey() {
        return this.primaryKey;
    }

    public void setPrimaryKey(long primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().isInstance(o)) {
            return false;
        }
        SQLInfo compare = (SQLInfo) o;
        if (this.table.equals(compare.table) && this.primaryKey == compare.primaryKey) {
            return true;
        }
        return false;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.table);
        stringBuilder.append(",");
        stringBuilder.append(this.primaryKey);
        return stringBuilder.toString();
    }

    public int hashCode() {
        return (31 * (1 * 31)) + (this.table == null ? 0 : this.table.hashCode());
    }
}
