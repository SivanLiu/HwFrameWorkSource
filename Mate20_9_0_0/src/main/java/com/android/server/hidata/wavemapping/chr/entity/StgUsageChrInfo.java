package com.android.server.hidata.wavemapping.chr.entity;

public class StgUsageChrInfo {
    private float backupsize;
    private float dataSize;
    private float dbSize;
    private float logSize;
    private float modelSize;
    private float rawdataSize;

    public StgUsageChrInfo(float dbSize, float rawdataSize, float modelSize, float logSize, float dataSize, float backupsize) {
        this.dbSize = dbSize;
        this.rawdataSize = rawdataSize;
        this.modelSize = modelSize;
        this.logSize = logSize;
        this.dataSize = dataSize;
        this.backupsize = backupsize;
    }

    public float getDbSize() {
        return this.dbSize;
    }

    public void setDbSize(float dbSize) {
        this.dbSize = dbSize;
    }

    public float getRawdataSize() {
        return this.rawdataSize;
    }

    public void setRawdataSize(float rawdataSize) {
        this.rawdataSize = rawdataSize;
    }

    public float getModelSize() {
        return this.modelSize;
    }

    public void setModelSize(float modelSize) {
        this.modelSize = modelSize;
    }

    public float getLogSize() {
        return this.logSize;
    }

    public void setLogSize(float logSize) {
        this.logSize = logSize;
    }

    public float getDataSize() {
        return this.dataSize;
    }

    public void setDataSize(float dataSize) {
        this.dataSize = dataSize;
    }

    public float getBackupsize() {
        return this.backupsize;
    }

    public void setBackupsize(float backupsize) {
        this.backupsize = backupsize;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("StgUsageChrInfo{dbSize=");
        stringBuilder.append(this.dbSize);
        stringBuilder.append(", rawdataSize=");
        stringBuilder.append(this.rawdataSize);
        stringBuilder.append(", modelSize=");
        stringBuilder.append(this.modelSize);
        stringBuilder.append(", logSize=");
        stringBuilder.append(this.logSize);
        stringBuilder.append(", dataSize=");
        stringBuilder.append(this.dataSize);
        stringBuilder.append(", backupsize=");
        stringBuilder.append(this.backupsize);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
