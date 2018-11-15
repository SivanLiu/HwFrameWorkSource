package com.huawei.odmf.core;

import com.huawei.odmf.exception.ODMFIllegalArgumentException;

public class Configuration {
    public static final int CONFIGURATION_DATABASE_ANDROID = 302;
    public static final int CONFIGURATION_DATABASE_ODMF = 301;
    public static final int CONFIGURATION_STORAGE_MODE_DISK = 402;
    public static final int CONFIGURATION_STORAGE_MODE_MEMORY = 401;
    public static final int CONFIGURATION_TYPE_LOCAL = 200;
    public static final int CONFIGURATION_TYPE_PROVIDER = 201;
    private int databaseType;
    private boolean detectDelete;
    private String path;
    private int storageMode;
    private boolean throwException;
    private int type;

    public Configuration(String path, int type, int databaseType, int storageMode) {
        configurationImpl(path, type, databaseType, storageMode);
    }

    public Configuration() {
        configurationImpl(null, 200, CONFIGURATION_DATABASE_ANDROID, CONFIGURATION_STORAGE_MODE_DISK);
    }

    public Configuration(String path) {
        configurationImpl(path, 200, CONFIGURATION_DATABASE_ANDROID, CONFIGURATION_STORAGE_MODE_DISK);
    }

    public Configuration(String path, int type) {
        configurationImpl(path, type, CONFIGURATION_DATABASE_ANDROID, CONFIGURATION_STORAGE_MODE_DISK);
    }

    public Configuration(String path, int type, int databaseType) {
        configurationImpl(path, type, databaseType, CONFIGURATION_STORAGE_MODE_DISK);
    }

    public Configuration(int storageMode) {
        configurationImpl(null, 200, CONFIGURATION_DATABASE_ANDROID, storageMode);
    }

    private void configurationImpl(String path, int type, int databaseType, int storageMode) {
        if (checkConfig(type, databaseType, storageMode)) {
            this.path = path;
            this.type = type;
            this.databaseType = databaseType;
            this.storageMode = storageMode;
            return;
        }
        throw new ODMFIllegalArgumentException("The configuration is incorrect.");
    }

    private boolean checkConfig(int type, int databaseType, int storageMode) {
        if ((type == 200 || type == CONFIGURATION_TYPE_PROVIDER) && ((databaseType == CONFIGURATION_DATABASE_ANDROID || databaseType == CONFIGURATION_DATABASE_ODMF) && (storageMode == CONFIGURATION_STORAGE_MODE_MEMORY || storageMode == CONFIGURATION_STORAGE_MODE_DISK))) {
            return true;
        }
        return false;
    }

    public String getPath() {
        return this.path;
    }

    public int getType() {
        return this.type;
    }

    public int getDatabaseType() {
        return this.databaseType;
    }

    public int getStorageMode() {
        return this.storageMode;
    }

    void setType(int type) {
        this.type = type;
    }

    void setDatabaseType(int databaseType) {
        this.databaseType = databaseType;
    }

    void setStorageMode(int storageMode) {
        this.storageMode = storageMode;
    }

    private static String modeToString(int mode) {
        switch (mode) {
            case CONFIGURATION_STORAGE_MODE_MEMORY /*401*/:
                return "CONFIGURATION_STORAGE_MODE_MEMORY";
            case CONFIGURATION_STORAGE_MODE_DISK /*402*/:
                return "CONFIGURATION_STORAGE_MODE_DISK";
            default:
                return Integer.toString(mode);
        }
    }

    private static String typeToString(int type) {
        switch (type) {
            case 200:
                return "CONFIGURATION_TYPE_LOCAL";
            case CONFIGURATION_TYPE_PROVIDER /*201*/:
                return "CONFIGURATION_TYPE_PROVIDER";
            default:
                return Integer.toString(type);
        }
    }

    private static String databaseTypeToString(int databaseType) {
        switch (databaseType) {
            case CONFIGURATION_DATABASE_ODMF /*301*/:
                return "CONFIGURATION_DATABASE_ODMF";
            case CONFIGURATION_DATABASE_ANDROID /*302*/:
                return "CONFIGURATION_DATABASE_ANDROID";
            default:
                return Integer.toString(databaseType);
        }
    }

    public boolean isThrowException() {
        return this.throwException;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }

    public boolean isDetectDelete() {
        return this.detectDelete;
    }

    public void setDetectDelete(boolean detectDelete) {
        this.detectDelete = detectDelete;
    }

    public String toString() {
        return "Configuration {Path :" + this.path + ", Mode:" + modeToString(this.storageMode) + ", Type:" + typeToString(this.type) + ", DatabaseType:" + databaseTypeToString(this.databaseType) + "}";
    }
}
