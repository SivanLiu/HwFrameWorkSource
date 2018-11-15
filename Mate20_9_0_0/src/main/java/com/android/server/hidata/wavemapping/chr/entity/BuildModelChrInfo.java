package com.android.server.hidata.wavemapping.chr.entity;

public class BuildModelChrInfo extends ChrInfo {
    private ApChrStatInfo APType = new ApChrStatInfo();
    private int batchAll;
    private int batchCell;
    private int batchMain;
    private int configVerAll;
    private int configVerCell;
    private int configVerMain;
    private int fingerAll;
    private int fingerCell;
    private int fingerMain;
    private int firstTimeAll;
    private int firstTimeCell;
    private int firstTimeMain;
    private float maxDistAll;
    private float maxDistCell;
    private int maxDistMain;
    private int modelCell;
    private int modelMain;
    private int ref;
    private int retAll;
    private int retCell;
    private int retMain;
    private StgUsageChrInfo storage;
    private int testDataAll;
    private int testDataCell;
    private int testDataMain;
    private int trainDataAll;
    private int trainDataCell;
    private int trainDataMain;
    private int updateAll;
    private int updateCell;
    private int updateMain;

    public int getBatchAll() {
        return this.batchAll;
    }

    public void setBatchAll(int batchAll) {
        this.batchAll = batchAll;
    }

    public int getFingerAll() {
        return this.fingerAll;
    }

    public void setFingerAll(int fingerAll) {
        this.fingerAll = fingerAll;
    }

    public int getRetAll() {
        return this.retAll;
    }

    public void setRetAll(int retAll) {
        this.retAll = retAll;
    }

    public int getTrainDataAll() {
        return this.trainDataAll;
    }

    public void setTrainDataAll(int trainDataAll) {
        this.trainDataAll = trainDataAll;
    }

    public int getTestDataAll() {
        return this.testDataAll;
    }

    public void setTestDataAll(int testDataAll) {
        this.testDataAll = testDataAll;
    }

    public int getUpdateAll() {
        return this.updateAll;
    }

    public void setUpdateAll(int updateAll) {
        this.updateAll = updateAll;
    }

    public int getFirstTimeAll() {
        return this.firstTimeAll;
    }

    public void setFirstTimeAll(int firstTimeAll) {
        this.firstTimeAll = firstTimeAll;
    }

    public float getMaxDistAll() {
        return this.maxDistAll;
    }

    public void setMaxDistAll(float maxDistAll) {
        this.maxDistAll = maxDistAll;
    }

    public int getConfigVerAll() {
        return this.configVerAll;
    }

    public void setConfigVerAll(int configVerAll) {
        this.configVerAll = configVerAll;
    }

    public ApChrStatInfo getAPType() {
        return this.APType;
    }

    public void setAPType(ApChrStatInfo APType) {
        this.APType = APType;
    }

    public int getBatchMain() {
        return this.batchMain;
    }

    public void setBatchMain(int batchMain) {
        this.batchMain = batchMain;
    }

    public int getFingerMain() {
        return this.fingerMain;
    }

    public void setFingerMain(int fingerMain) {
        this.fingerMain = fingerMain;
    }

    public int getRetMain() {
        return this.retMain;
    }

    public void setRetMain(int retMain) {
        this.retMain = retMain;
    }

    public int getTrainDataMain() {
        return this.trainDataMain;
    }

    public void setTrainDataMain(int trainDataMain) {
        this.trainDataMain = trainDataMain;
    }

    public int getTestDataMain() {
        return this.testDataMain;
    }

    public void setTestDataMain(int testDataMain) {
        this.testDataMain = testDataMain;
    }

    public int getUpdateMain() {
        return this.updateMain;
    }

    public void setUpdateMain(int updateMain) {
        this.updateMain = updateMain;
    }

    public int getFirstTimeMain() {
        return this.firstTimeMain;
    }

    public void setFirstTimeMain(int firstTimeMain) {
        this.firstTimeMain = firstTimeMain;
    }

    public int getModelMain() {
        return this.modelMain;
    }

    public void setModelMain(int modelMain) {
        this.modelMain = modelMain;
    }

    public int getMaxDistMain() {
        return this.maxDistMain;
    }

    public void setMaxDistMain(int maxDistMain) {
        this.maxDistMain = maxDistMain;
    }

    public int getConfigVerMain() {
        return this.configVerMain;
    }

    public void setConfigVerMain(int configVerMain) {
        this.configVerMain = configVerMain;
    }

    public int getBatchCell() {
        return this.batchCell;
    }

    public void setBatchCell(int batchCell) {
        this.batchCell = batchCell;
    }

    public int getFingerCell() {
        return this.fingerCell;
    }

    public void setFingerCell(int fingerCell) {
        this.fingerCell = fingerCell;
    }

    public int getRetCell() {
        return this.retCell;
    }

    public void setRetCell(int retCell) {
        this.retCell = retCell;
    }

    public int getTrainDataCell() {
        return this.trainDataCell;
    }

    public void setTrainDataCell(int trainDataCell) {
        this.trainDataCell = trainDataCell;
    }

    public int getTestDataCell() {
        return this.testDataCell;
    }

    public void setTestDataCell(int testDataCell) {
        this.testDataCell = testDataCell;
    }

    public int getUpdateCell() {
        return this.updateCell;
    }

    public void setUpdateCell(int updateCell) {
        this.updateCell = updateCell;
    }

    public int getFirstTimeCell() {
        return this.firstTimeCell;
    }

    public void setFirstTimeCell(int firstTimeCell) {
        this.firstTimeCell = firstTimeCell;
    }

    public int getModelCell() {
        return this.modelCell;
    }

    public void setModelCell(int modelCell) {
        this.modelCell = modelCell;
    }

    public float getMaxDistCell() {
        return this.maxDistCell;
    }

    public void setMaxDistCell(float maxDistCell) {
        this.maxDistCell = maxDistCell;
    }

    public int getConfigVerCell() {
        return this.configVerCell;
    }

    public void setConfigVerCell(int configVerCell) {
        this.configVerCell = configVerCell;
    }

    public int getRef() {
        return this.ref;
    }

    public void setRef(int ref) {
        this.ref = ref;
    }

    public StgUsageChrInfo getStorage() {
        return this.storage;
    }

    public void setStorage(StgUsageChrInfo storage) {
        this.storage = storage;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BuildModelChrInfo{batchAll=");
        stringBuilder.append(this.batchAll);
        stringBuilder.append(", fingerAll=");
        stringBuilder.append(this.fingerAll);
        stringBuilder.append(", retAll=");
        stringBuilder.append(this.retAll);
        stringBuilder.append(", trainDataAll=");
        stringBuilder.append(this.trainDataAll);
        stringBuilder.append(", testDataAll=");
        stringBuilder.append(this.testDataAll);
        stringBuilder.append(", updateAll=");
        stringBuilder.append(this.updateAll);
        stringBuilder.append(", firstTimeAll=");
        stringBuilder.append(this.firstTimeAll);
        stringBuilder.append(", maxDistAll=");
        stringBuilder.append(this.maxDistAll);
        stringBuilder.append(", configVerAll=");
        stringBuilder.append(this.configVerAll);
        stringBuilder.append(", APType=");
        stringBuilder.append(this.APType.toString());
        stringBuilder.append(", batchMain=");
        stringBuilder.append(this.batchMain);
        stringBuilder.append(", fingerMain=");
        stringBuilder.append(this.fingerMain);
        stringBuilder.append(", retMain=");
        stringBuilder.append(this.retMain);
        stringBuilder.append(", trainDataMain=");
        stringBuilder.append(this.trainDataMain);
        stringBuilder.append(", testDataMain=");
        stringBuilder.append(this.testDataMain);
        stringBuilder.append(", updateMain=");
        stringBuilder.append(this.updateMain);
        stringBuilder.append(", firstTimeMain=");
        stringBuilder.append(this.firstTimeMain);
        stringBuilder.append(", modelMain=");
        stringBuilder.append(this.modelMain);
        stringBuilder.append(", maxDistMain=");
        stringBuilder.append(this.maxDistMain);
        stringBuilder.append(", configVerMain=");
        stringBuilder.append(this.configVerMain);
        stringBuilder.append(", batchCell=");
        stringBuilder.append(this.batchCell);
        stringBuilder.append(", fingerCell=");
        stringBuilder.append(this.fingerCell);
        stringBuilder.append(", retCell=");
        stringBuilder.append(this.retCell);
        stringBuilder.append(", trainDataCell=");
        stringBuilder.append(this.trainDataCell);
        stringBuilder.append(", testDataCell=");
        stringBuilder.append(this.testDataCell);
        stringBuilder.append(", updateCell=");
        stringBuilder.append(this.updateCell);
        stringBuilder.append(", firstTimeCell=");
        stringBuilder.append(this.firstTimeCell);
        stringBuilder.append(", modelCell=");
        stringBuilder.append(this.modelCell);
        stringBuilder.append(", maxDistCell=");
        stringBuilder.append(this.maxDistCell);
        stringBuilder.append(", configVerCell=");
        stringBuilder.append(this.configVerCell);
        stringBuilder.append(", ref=");
        stringBuilder.append(this.ref);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
