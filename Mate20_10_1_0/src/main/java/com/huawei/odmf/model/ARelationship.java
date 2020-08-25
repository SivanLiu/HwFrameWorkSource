package com.huawei.odmf.model;

import com.huawei.odmf.model.api.Relationship;

public class ARelationship extends AField implements Relationship {
    public static final String DELETE_CASCADE = "delete";
    public static final String EXCEPTION = "exception";
    public static final String IGNORE = "ignore";
    public static final int MANY_TO_MANY = 0;
    public static final int MANY_TO_ONE = 2;
    public static final String NONE_CASCADE = "none";
    public static final int ONE_TO_MANY = 4;
    public static final int ONE_TO_ONE = 6;
    private AEntity baseEntity;
    private String cascade;
    private ARelationship inverseRelationship;
    private boolean isLazy;
    private boolean isMajor;
    private String notFound;
    private String relatedColumnName;
    private AEntity relatedEntity;
    private int relationShipType;

    public ARelationship() {
    }

    public ARelationship(String fieldName, String relatedColumnName2, int relationShipType2, AEntity baseEntity2, AEntity relatedEntity2, String cascade2, boolean isLazy2, String notFound2, ARelationship inverseRelationship2, boolean isMajor2) {
        super(fieldName);
        this.relatedColumnName = relatedColumnName2;
        this.relationShipType = relationShipType2;
        this.baseEntity = baseEntity2;
        this.relatedEntity = relatedEntity2;
        this.cascade = cascade2;
        this.isLazy = isLazy2;
        this.notFound = notFound2;
        this.inverseRelationship = inverseRelationship2;
        this.isMajor = isMajor2;
    }

    @Override // com.huawei.odmf.model.api.Relationship
    public String getForeignKeyName() {
        if (this.isMajor) {
            return getFieldName();
        }
        return getInverseRelationship().getFieldName();
    }

    @Override // com.huawei.odmf.model.api.Relationship
    public boolean isMajor() {
        return this.isMajor;
    }

    public void setMajor(boolean isMajor2) {
        this.isMajor = isMajor2;
    }

    @Override // com.huawei.odmf.model.api.Relationship
    public ARelationship getInverseRelationship() {
        return this.inverseRelationship;
    }

    public void setInverseRelationship(ARelationship inverseRelationship2) {
        this.inverseRelationship = inverseRelationship2;
    }

    @Override // com.huawei.odmf.model.api.Relationship
    public int getRelationShipType() {
        return this.relationShipType;
    }

    public void setRelationShipType(int relationShipType2) {
        this.relationShipType = relationShipType2;
    }

    @Override // com.huawei.odmf.model.api.Relationship
    public AEntity getBaseEntity() {
        return this.baseEntity;
    }

    public void setBaseEntity(AEntity basedEntity) {
        this.baseEntity = basedEntity;
    }

    @Override // com.huawei.odmf.model.api.Relationship
    public AEntity getRelatedEntity() {
        return this.relatedEntity;
    }

    public void setRelatedEntity(AEntity relatedEntity2) {
        this.relatedEntity = relatedEntity2;
    }

    @Override // com.huawei.odmf.model.api.Relationship
    public String getRelatedColumnName() {
        return this.relatedColumnName;
    }

    public void setRelatedColumnName(String relatedColumnName2) {
        this.relatedColumnName = relatedColumnName2;
    }

    @Override // com.huawei.odmf.model.api.Relationship
    public String getCascade() {
        return this.cascade;
    }

    public void setCascade(String cascade2) {
        this.cascade = cascade2;
    }

    @Override // com.huawei.odmf.model.api.Relationship
    public boolean isLazy() {
        return this.isLazy;
    }

    public void setLazy(boolean isLazy2) {
        this.isLazy = isLazy2;
    }

    @Override // com.huawei.odmf.model.api.Relationship
    public String getNotFound() {
        return this.notFound;
    }

    public void setNotFound(String notFound2) {
        this.notFound = notFound2;
    }

    @Override // com.huawei.odmf.model.AField
    public String toString() {
        return "ARelationship{, relatedColumnName='" + this.relatedColumnName + '\'' + ", relationShipType='" + this.relationShipType + '\'' + ", baseEntity=" + this.baseEntity + ", relatedEntity=" + this.relatedEntity + ", cascade='" + this.cascade + '\'' + ", isLazy=" + this.isLazy + ", notFound='" + this.notFound + '\'' + '}';
    }
}
