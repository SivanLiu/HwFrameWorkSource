package com.huawei.odmf.model;

import com.huawei.odmf.model.api.Field;

public class AField implements Field {
    private String fieldName;

    public AField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String toString() {
        return "AField{fieldName='" + this.fieldName + '\'' + '}';
    }
}
