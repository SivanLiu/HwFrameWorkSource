package com.huawei.odmf.model;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.model.api.ObjectModel;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class AObjectModel implements ObjectModel {
    private String databaseName = null;
    private String databaseVersion = null;
    private int databaseVersionCode;
    private Map<String, AEntity> entities = null;
    private String modelName = null;

    public AObjectModel(String modelName, String databaseVersion, int databaseVersionCode, String databaseName, Map<String, AEntity> entities) {
        this.modelName = modelName;
        this.databaseVersion = databaseVersion;
        this.databaseName = databaseName;
        this.entities = entities;
        this.databaseVersionCode = databaseVersionCode;
    }

    public String getModelName() {
        return this.modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getDatabaseVersion() {
        return this.databaseVersion;
    }

    public void setDatabaseVersion(String databaseVersion) {
        this.databaseVersion = databaseVersion;
    }

    public int getDatabaseVersionCode() {
        return this.databaseVersionCode;
    }

    public void setDatabaseVersionCode(int databaseVersionCode) {
        this.databaseVersionCode = databaseVersionCode;
    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public Map<String, AEntity> getEntities() {
        return this.entities;
    }

    public void setEntities(Map<String, AEntity> entities) {
        this.entities = entities;
    }

    public static AObjectModel parse(String fileDir, String fileName) {
        if (!TextUtils.isEmpty(fileDir) && !TextUtils.isEmpty(fileName)) {
            return XmlParser.parseToModel(fileDir, fileName);
        }
        throw new ODMFIllegalArgumentException("fileDir or fileName is null");
    }

    public static AObjectModel parse(File file) throws IOException {
        if (file != null) {
            return XmlParser.parseToModel(file);
        }
        throw new ODMFIllegalArgumentException("file is null");
    }

    public static AObjectModel parse(Context context, String assetsFileName) {
        if (!TextUtils.isEmpty(assetsFileName) && context != null) {
            return XmlParser.parseToModel(context, assetsFileName);
        }
        throw new ODMFIllegalArgumentException("parameter assetsFileName or context error");
    }

    public AEntity getEntity(String entityName) {
        if (!TextUtils.isEmpty(entityName)) {
            return (AEntity) this.entities.get(entityName);
        }
        throw new ODMFIllegalArgumentException("entityName is null");
    }

    public AEntity getEntity(Class className) {
        if (className != null) {
            return (AEntity) this.entities.get(className.getName());
        }
        throw new ODMFIllegalArgumentException("class is null");
    }

    public String toString() {
        return "AObjectModel{modelName='" + this.modelName + '\'' + ", databaseVersion=" + this.databaseVersion + ", databaseName='" + this.databaseName + '\'' + ", entities=" + this.entities + '}';
    }
}
