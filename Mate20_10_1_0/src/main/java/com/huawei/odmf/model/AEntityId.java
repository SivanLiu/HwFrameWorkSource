package com.huawei.odmf.model;

import com.huawei.odmf.exception.ODMFNullPointerException;
import com.huawei.odmf.model.api.EntityId;

public class AEntityId extends AAttribute implements EntityId {
    public static final String INCREMENT = "increment";
    private static final int MULTIPLIER = 31;
    public static final String NATURAL_ID = "natural_id";
    private String generatorType;

    public AEntityId() {
    }

    public AEntityId(String fieldName, int type, boolean hasIndex, boolean isUnique, boolean isNotNull, boolean isLazy, String defaultValue, String generatorType2) {
        super(fieldName, type, hasIndex, isUnique, isNotNull, isLazy, defaultValue);
        this.generatorType = generatorType2;
    }

    @Override // com.huawei.odmf.model.api.EntityId
    public String getGeneratorType() {
        return this.generatorType;
    }

    public void setGeneratorType(String generatorType2) {
        this.generatorType = generatorType2;
    }

    @Override // com.huawei.odmf.model.api.EntityId
    public String getIdName() {
        if (getFieldName() != null) {
            return super.getFieldName();
        }
        throw new ODMFNullPointerException("Field name is null.");
    }

    @Override // com.huawei.odmf.model.AAttribute
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AEntityId entityId = (AEntityId) o;
        if (this.generatorType != null) {
            return this.generatorType.equals(entityId.generatorType);
        }
        return entityId.generatorType == null;
    }

    @Override // com.huawei.odmf.model.AAttribute
    public int hashCode() {
        return (super.hashCode() * MULTIPLIER) + (this.generatorType != null ? this.generatorType.hashCode() : 0);
    }
}
