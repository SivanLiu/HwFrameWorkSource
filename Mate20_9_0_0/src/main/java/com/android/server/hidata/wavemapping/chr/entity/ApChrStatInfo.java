package com.android.server.hidata.wavemapping.chr.entity;

public class ApChrStatInfo {
    private int enterpriseAp;
    private int finalUsed;
    private int mobileApSrc1;
    private int mobileApSrc2;
    private int mobileApSrc3;
    private int totalFound;
    private int update;

    public int getEnterpriseAp() {
        return this.enterpriseAp;
    }

    public void setEnterpriseAp(int enterpriseAp) {
        this.enterpriseAp = enterpriseAp;
    }

    public int getMobileApSrc1() {
        return this.mobileApSrc1;
    }

    public void setMobileApSrc1(int mobileApSrc1) {
        this.mobileApSrc1 = mobileApSrc1;
    }

    public int getMobileApSrc2() {
        return this.mobileApSrc2;
    }

    public void setMobileApSrc2(int mobileApSrc2) {
        this.mobileApSrc2 = mobileApSrc2;
    }

    public int getMobileApSrc3() {
        return this.mobileApSrc3;
    }

    public void setMobileApSrc3(int mobileApSrc3) {
        this.mobileApSrc3 = mobileApSrc3;
    }

    public int getUpdate() {
        return this.update;
    }

    public void setUpdate(int update) {
        this.update = update;
    }

    public int getTotalFound() {
        return this.totalFound;
    }

    public void setTotalFound(int totalFound) {
        this.totalFound = totalFound;
    }

    public int getFinalUsed() {
        return this.finalUsed;
    }

    public void setFinalUsed(int finalUsed) {
        this.finalUsed = finalUsed;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ApChrStatInfo{enterpriseAp=");
        stringBuilder.append(this.enterpriseAp);
        stringBuilder.append(", mobileApSrc1=");
        stringBuilder.append(this.mobileApSrc1);
        stringBuilder.append(", mobileApSrc2=");
        stringBuilder.append(this.mobileApSrc2);
        stringBuilder.append(", mobileApSrc3=");
        stringBuilder.append(this.mobileApSrc3);
        stringBuilder.append(", update=");
        stringBuilder.append(this.update);
        stringBuilder.append(", totalFound=");
        stringBuilder.append(this.totalFound);
        stringBuilder.append(", finalUsed=");
        stringBuilder.append(this.finalUsed);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
