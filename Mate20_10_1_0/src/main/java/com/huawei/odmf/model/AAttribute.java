package com.huawei.odmf.model;

import com.huawei.odmf.model.api.Attribute;

public class AAttribute extends AField implements Attribute {
    private static final int MULTIPLIER = 31;
    private String columnName;
    private String defaultValue;
    private boolean hasIndex;
    private boolean isLazy;
    private boolean isNotNull;
    private boolean isUnique;
    private int type;

    public AAttribute() {
    }

    public AAttribute(String fieldName, int type2, boolean hasIndex2, boolean isUnique2, boolean isNotNull2, boolean isLazy2, String defaultValue2) {
        super(fieldName);
        this.columnName = fieldName;
        this.type = type2;
        this.hasIndex = hasIndex2;
        this.isUnique = isUnique2;
        this.isNotNull = isNotNull2;
        this.isLazy = isLazy2;
        this.defaultValue = defaultValue2;
    }

    @Override // com.huawei.odmf.model.api.Attribute
    public int getType() {
        return this.type;
    }

    public void setType(int type2) {
        this.type = type2;
    }

    @Override // com.huawei.odmf.model.api.Attribute
    public boolean isUnique() {
        return this.isUnique;
    }

    public void setUnique(boolean unique) {
        this.isUnique = unique;
    }

    @Override // com.huawei.odmf.model.api.Attribute
    public boolean hasIndex() {
        return this.hasIndex;
    }

    @Override // com.huawei.odmf.model.api.Attribute
    public boolean isNotNull() {
        return this.isNotNull;
    }

    public void setNotNull(boolean isNotNull2) {
        this.isNotNull = isNotNull2;
    }

    @Override // com.huawei.odmf.model.api.Attribute
    public boolean isLazy() {
        return this.isLazy;
    }

    public void setLazy(boolean isLazy2) {
        this.isLazy = isLazy2;
    }

    @Override // com.huawei.odmf.model.api.Attribute
    public String getDefaultValue() {
        return this.defaultValue;
    }

    public void setDefaultValue(String defaultValue2) {
        this.defaultValue = defaultValue2;
    }

    @Override // com.huawei.odmf.model.api.Attribute
    public String getColumnName() {
        return this.columnName;
    }

    public void setColumnName(String columnName2) {
        this.columnName = columnName2;
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
        if (this.type != that.type || this.isUnique != that.isUnique || this.isNotNull != that.isNotNull || this.isLazy != that.isLazy) {
            return false;
        }
        if (this.defaultValue != null) {
            if (!this.defaultValue.equals(that.defaultValue)) {
                return false;
            }
        } else if (that.defaultValue != null) {
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
        int i2;
        int i3 = 1;
        int i4 = 0;
        int i5 = ((this.type * MULTIPLIER) + (this.isUnique ? 1 : 0)) * MULTIPLIER;
        if (this.isNotNull) {
            i = 1;
        } else {
            i = 0;
        }
        int i6 = (i5 + i) * MULTIPLIER;
        if (!this.isLazy) {
            i3 = 0;
        }
        int i7 = (i6 + i3) * MULTIPLIER;
        if (this.defaultValue != null) {
            i2 = this.defaultValue.hashCode();
        } else {
            i2 = 0;
        }
        int i8 = (i7 + i2) * MULTIPLIER;
        if (this.columnName != null) {
            i4 = this.columnName.hashCode();
        }
        return i8 + i4;
    }
}
