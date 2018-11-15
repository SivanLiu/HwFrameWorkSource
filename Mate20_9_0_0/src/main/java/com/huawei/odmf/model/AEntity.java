package com.huawei.odmf.model;

import android.text.TextUtils;
import com.huawei.odmf.core.StatementsLoader;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFIllegalStateException;
import com.huawei.odmf.model.api.Entity;
import com.huawei.odmf.model.api.Index;
import com.huawei.odmf.model.api.ObjectModel;
import java.util.ArrayList;
import java.util.List;

public class AEntity implements Entity {
    private List<AAttribute> attributes;
    private List<AEntityId> entityId;
    private String entityName;
    private String entityVersion;
    private int entityVersionCode;
    private List<Index> indexes;
    private boolean isKeyAutoIncrement;
    private StatementsLoader mStatements;
    private ObjectModel model;
    private List<ARelationship> relationships;
    private String tableName;

    public AEntity() {
        this.entityName = null;
        this.tableName = null;
        this.entityVersion = null;
        this.entityId = null;
        this.attributes = null;
        this.relationships = null;
        this.indexes = null;
        this.model = null;
        this.isKeyAutoIncrement = false;
        this.mStatements = new StatementsLoader();
    }

    public AEntity(String entityName, String tableName, List<AEntityId> entityId, List<AAttribute> attributes, List<ARelationship> relationships, ObjectModel model, String entityVersion, int entityVersionCode) {
        this.entityName = null;
        this.tableName = null;
        this.entityVersion = null;
        this.entityId = null;
        this.attributes = null;
        this.relationships = null;
        this.indexes = null;
        this.model = null;
        this.isKeyAutoIncrement = false;
        this.entityName = entityName;
        this.tableName = tableName;
        this.entityId = entityId;
        this.attributes = attributes;
        this.relationships = relationships;
        this.model = model;
        this.entityVersion = entityVersion;
        this.entityVersionCode = entityVersionCode;
        this.mStatements = new StatementsLoader();
    }

    void setModel(ObjectModel model) {
        this.model = model;
    }

    void setIndexes(List<Index> indexes) {
        this.indexes = indexes;
    }

    public List<Index> getIndexes() {
        return this.indexes;
    }

    public boolean isKeyAutoIncrement() {
        return this.isKeyAutoIncrement;
    }

    public StatementsLoader getStatements() {
        return this.mStatements;
    }

    void setKeyAutoIncrement(boolean isKeyAutoIncrement) {
        this.isKeyAutoIncrement = isKeyAutoIncrement;
    }

    public String getEntityVersion() {
        return this.entityVersion;
    }

    void setEntityVersion(String entityVersion) {
        this.entityVersion = entityVersion;
    }

    public int getEntityVersionCode() {
        return this.entityVersionCode;
    }

    public void setEntityVersionCode(int entityVersionCode) {
        this.entityVersionCode = entityVersionCode;
    }

    public ObjectModel getModel() {
        return this.model;
    }

    public String getEntityName() {
        return this.entityName;
    }

    void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getTableName() {
        return this.tableName;
    }

    void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<AEntityId> getEntityId() {
        return this.entityId;
    }

    void setEntityId(List<AEntityId> entityId) {
        this.entityId = entityId;
    }

    public List<AAttribute> getAttributes() {
        return this.attributes;
    }

    void setAttributes(List<AAttribute> attributes) {
        this.attributes = attributes;
    }

    public List<String> getIdName() {
        if (this.entityId == null) {
            throw new ODMFIllegalStateException("entityId has not been initialized.");
        }
        List<String> result = new ArrayList();
        int size = this.entityId.size();
        for (int i = 0; i < size; i++) {
            result.add(((AEntityId) this.entityId.get(i)).getIdName());
        }
        return result;
    }

    static Class getClassType(String entityName) {
        if (TextUtils.isEmpty(entityName)) {
            throw new ODMFIllegalArgumentException("entityName is empty.");
        }
        try {
            return Class.forName(entityName);
        } catch (ClassNotFoundException e) {
            throw new ODMFIllegalArgumentException(entityName + "is not a valid entity name.");
        }
    }

    public List<ARelationship> getRelationships() {
        return this.relationships;
    }

    void setRelationships(List<ARelationship> relationships) {
        this.relationships = relationships;
    }

    public boolean isAttribute(String propertyName) {
        List<AAttribute> attributeList = this.attributes;
        int size = attributeList.size();
        for (int i = 0; i < size; i++) {
            if (((AAttribute) attributeList.get(i)).getFieldName().equals(propertyName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRelationship(String propertyName) {
        for (ARelationship relationship : this.relationships) {
            if (relationship.getFieldName().equals(propertyName)) {
                return true;
            }
        }
        return false;
    }

    public AAttribute getAttribute(String propertyName) {
        List<? extends AAttribute> aAttributeList = this.attributes;
        int size = aAttributeList.size();
        for (int i = 0; i < size; i++) {
            AAttribute attribute = (AAttribute) aAttributeList.get(i);
            if (attribute.getFieldName().equals(propertyName)) {
                return attribute;
            }
        }
        return null;
    }

    public ARelationship getRelationship(String propertyName) {
        for (ARelationship relationship : this.relationships) {
            if (relationship.getFieldName().equals(propertyName)) {
                return relationship;
            }
        }
        return null;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AEntity entity = (AEntity) o;
        if (this.entityName != null) {
            if (!this.entityName.equals(entity.entityName)) {
                return false;
            }
        } else if (entity.entityName != null) {
            return false;
        }
        if (this.tableName != null) {
            if (!this.tableName.equals(entity.tableName)) {
                return false;
            }
        } else if (entity.tableName != null) {
            return false;
        }
        if (!this.entityId.equals(entity.entityId)) {
            return false;
        }
        if (this.attributes != null) {
            if (!this.attributes.equals(entity.attributes)) {
                return false;
            }
        } else if (entity.attributes != null) {
            return false;
        }
        if (this.relationships != null) {
            z = this.relationships.equals(entity.relationships);
        } else if (entity.relationships != null) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        int result;
        int hashCode;
        int i = 0;
        if (this.entityName != null) {
            result = this.entityName.hashCode();
        } else {
            result = 0;
        }
        int i2 = result * 31;
        if (this.tableName != null) {
            hashCode = this.tableName.hashCode();
        } else {
            hashCode = 0;
        }
        i2 = (((i2 + hashCode) * 31) + this.entityId.hashCode()) * 31;
        if (this.attributes != null) {
            hashCode = this.attributes.hashCode();
        } else {
            hashCode = 0;
        }
        hashCode = (i2 + hashCode) * 31;
        if (this.relationships != null) {
            i = this.relationships.hashCode();
        }
        return hashCode + i;
    }

    public String toString() {
        return "AEntity{entityName='" + this.entityName + '\'' + ", tableName='" + this.tableName + '\'' + ", entityId=" + this.entityId + ", attributes=" + this.attributes + '}';
    }
}
