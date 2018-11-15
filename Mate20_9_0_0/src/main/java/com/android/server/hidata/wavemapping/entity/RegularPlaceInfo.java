package com.android.server.hidata.wavemapping.entity;

public class RegularPlaceInfo {
    private int batch;
    private int beginTime;
    private int disNum;
    private int fingerNum;
    private int identifyNum;
    private boolean isMainAp;
    private String modelName;
    private String noOcurBssids;
    private String place;
    private int screenState;
    private int state;
    private int testDataNum;

    public boolean isMainAp() {
        return this.isMainAp;
    }

    public void setMainAp(boolean mainAp) {
        this.isMainAp = mainAp;
    }

    public String getNoOcurBssids() {
        return this.noOcurBssids;
    }

    public void setNoOcurBssids(String noOcurBssids) {
        this.noOcurBssids = noOcurBssids;
    }

    public int getIdentifyNum() {
        return this.identifyNum;
    }

    public void setIdentifyNum(int identifyNum) {
        this.identifyNum = identifyNum;
    }

    public int getTestDataNum() {
        return this.testDataNum;
    }

    public void setTestDataNum(int testDataNum) {
        this.testDataNum = testDataNum;
    }

    public int getDisNum() {
        return this.disNum;
    }

    public void setDisNum(int disNum) {
        this.disNum = disNum;
    }

    public int getScreenState() {
        return this.screenState;
    }

    public void setScreenState(int screenState) {
        this.screenState = screenState;
    }

    public int getBatch() {
        return this.batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }

    public RegularPlaceInfo(String place, int state, int batch, int fingerNum, int testDataNum, int disNum, int identifyNum, String noOcurBssids, int isMainAp) {
        this.place = place;
        this.state = state;
        this.batch = batch;
        this.fingerNum = fingerNum;
        this.testDataNum = testDataNum;
        this.disNum = disNum;
        this.identifyNum = identifyNum;
        this.noOcurBssids = noOcurBssids;
        boolean z = true;
        if (isMainAp != 1) {
            z = false;
        }
        this.isMainAp = z;
    }

    public RegularPlaceInfo(String place, int state, int batch, int fingerNum, int testDataNum, int disNum, int identifyNum, String noOcurBssids, boolean isMainAp) {
        this.place = place;
        this.state = state;
        this.batch = batch;
        this.fingerNum = fingerNum;
        this.testDataNum = testDataNum;
        this.disNum = disNum;
        this.identifyNum = identifyNum;
        this.noOcurBssids = noOcurBssids;
        this.isMainAp = isMainAp;
    }

    public int getFingerNum() {
        return this.fingerNum;
    }

    public void setFingerNum(int fingerNum) {
        this.fingerNum = fingerNum;
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getPlace() {
        return this.place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getModelName() {
        return this.modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getBeginTime() {
        return this.beginTime;
    }

    public void setBeginTime(int beginTime) {
        this.beginTime = beginTime;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RegularPlaceInfo{place='");
        stringBuilder.append(this.place);
        stringBuilder.append('\'');
        stringBuilder.append(", state=");
        stringBuilder.append(this.state);
        stringBuilder.append(", batch=");
        stringBuilder.append(this.batch);
        stringBuilder.append(", fingerNum=");
        stringBuilder.append(this.fingerNum);
        stringBuilder.append(", screenState=");
        stringBuilder.append(this.screenState);
        stringBuilder.append(", testDataNum=");
        stringBuilder.append(this.testDataNum);
        stringBuilder.append(", disNum=");
        stringBuilder.append(this.disNum);
        stringBuilder.append(", identifyNum=");
        stringBuilder.append(this.identifyNum);
        stringBuilder.append(", isMainAp=");
        stringBuilder.append(this.isMainAp);
        stringBuilder.append(", modelName='");
        stringBuilder.append(this.modelName);
        stringBuilder.append('\'');
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
