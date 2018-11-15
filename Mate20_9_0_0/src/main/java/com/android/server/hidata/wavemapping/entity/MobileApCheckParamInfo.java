package com.android.server.hidata.wavemapping.entity;

public class MobileApCheckParamInfo {
    private int mobileApKRecord = 5;
    private int mobileApMinRange = 5;
    private int mobileApMinStd = 5;
    private float mobileApWeightParam = 0.3f;

    public int getMobileApKRecord() {
        return this.mobileApKRecord;
    }

    public void setMobileApKRecord(int mobileApKRecord) {
        this.mobileApKRecord = mobileApKRecord;
    }

    public int getMobileApMinStd() {
        return this.mobileApMinStd;
    }

    public void setMobileApMinStd(int mobileApMinStd) {
        this.mobileApMinStd = mobileApMinStd;
    }

    public int getMobileApMinRange() {
        return this.mobileApMinRange;
    }

    public void setMobileApMinRange(int mobileApMinRange) {
        this.mobileApMinRange = mobileApMinRange;
    }

    public float getMobileApWeightParam() {
        return this.mobileApWeightParam;
    }

    public void setMobileApWeightParam(float mobileApWeightParam) {
        this.mobileApWeightParam = mobileApWeightParam;
    }
}
