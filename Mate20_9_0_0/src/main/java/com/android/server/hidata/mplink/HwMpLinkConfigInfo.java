package com.android.server.hidata.mplink;

public class HwMpLinkConfigInfo {
    private String mAppName;
    private String mCondition;
    private String mCustMac;
    private String mEncryptType;
    private String mGatewayType;
    private String mMultNetwork;
    private String mReserved;
    private String mVendorOui;

    public String getCondition() {
        return this.mCondition;
    }

    public void setCondition(String mCondition) {
        this.mCondition = mCondition;
    }

    public String getmReserved() {
        return this.mReserved;
    }

    public String getVendorOui() {
        return this.mVendorOui;
    }

    public void setmVendorOui(String mVendorOui) {
        this.mVendorOui = mVendorOui;
    }

    public String getCustMac() {
        return this.mCustMac;
    }

    public void setmCustMac(String mCustMac) {
        this.mCustMac = mCustMac;
    }

    public String getAppName() {
        return this.mAppName;
    }

    public void setmAppName(String mAppName) {
        this.mAppName = mAppName;
    }

    public String getMultNetwork() {
        return this.mMultNetwork;
    }

    public void setmMultNetwork(String mMultNetwork) {
        this.mMultNetwork = mMultNetwork;
    }

    public String getGatewayType() {
        return this.mGatewayType;
    }

    public void setmGatewayType(String mGatewayType) {
        this.mGatewayType = mGatewayType;
    }

    public String getEncryptType() {
        return this.mEncryptType;
    }

    public void setmEncryptType(String mEncryptType) {
        this.mEncryptType = mEncryptType;
    }

    public String getReserved() {
        return this.mReserved;
    }

    public void setmReserved(String mReserved) {
        this.mReserved = mReserved;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("VendorOui: ");
        sb.append(this.mVendorOui);
        sb.append(" ,MultNetwork: ");
        sb.append(this.mMultNetwork);
        sb.append(" ,GatewayType: ");
        sb.append(this.mGatewayType);
        sb.append(" ,CustMac: ");
        sb.append(this.mCustMac);
        sb.append(" ,Reserved: ");
        sb.append(this.mReserved);
        sb.append(" ,EncryptType: ");
        sb.append(this.mEncryptType);
        sb.append(" ,Condition: ");
        sb.append(this.mCondition);
        return sb.toString();
    }
}
