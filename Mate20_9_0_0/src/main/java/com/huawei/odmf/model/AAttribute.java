package com.huawei.odmf.model;

import com.huawei.odmf.model.api.Attribute;

public class AAttribute extends AField implements Attribute {
    protected String columnName;
    protected String default_value;
    protected boolean index;
    protected boolean lazy;
    protected boolean notNull;
    protected int type;
    protected boolean unique;

    public AAttribute(String fieldName, int type, boolean index, boolean unique, boolean notNull, boolean lazy, String default_value) {
        super(fieldName);
        this.columnName = fieldName;
        this.type = type;
        this.index = index;
        this.unique = unique;
        this.notNull = notNull;
        this.lazy = lazy;
        this.default_value = default_value;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isUnique() {
        return this.unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean hasIndex() {
        return this.index;
    }

    public boolean isNotNull() {
        return this.notNull;
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    public boolean isLazy() {
        return this.lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public String getDefault_value() {
        return this.default_value;
    }

    public void setDefault_value(String default_value) {
        this.default_value = default_value;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AAttribute that = (AAttribute) o;
        if (this.type != that.type || this.unique != that.unique || this.notNull != that.notNull || this.lazy != that.lazy) {
            return false;
        }
        if (this.default_value != null) {
            if (!this.default_value.equals(that.default_value)) {
                return false;
            }
        } else if (that.default_value != null) {
            return false;
        }
        if (this.columnName != null) {
            z = this.columnName.equals(that.columnName);
        } else if (that.columnName != null) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        int i;
        int i2 = 1;
        int i3 = 0;
        int i4 = ((this.type * 31) + (this.unique ? 1 : 0)) * 31;
        if (this.notNull) {
            i = 1;
        } else {
            i = 0;
        }
        i = (i4 + i) * 31;
        if (!this.lazy) {
            i2 = 0;
        }
        i2 = (i + i2) * 31;
        if (this.default_value != null) {
            i = this.default_value.hashCode();
        } else {
            i = 0;
        }
        i = (i2 + i) * 31;
        if (this.columnName != null) {
            i3 = this.columnName.hashCode();
        }
        return i + i3;
    }
}
