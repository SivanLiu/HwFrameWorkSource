package com.huawei.permission;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class MaliInfoBean implements Parcelable {
    public static final Creator<MaliInfoBean> CREATOR = new Creator<MaliInfoBean>() {
        public MaliInfoBean createFromParcel(Parcel in) {
            return new MaliInfoBean(in, null);
        }

        public MaliInfoBean[] newArray(int size) {
            return new MaliInfoBean[size];
        }
    };
    public static final int NOT_RESTRICTED = 1;
    public static final int RESTRICTED = 0;
    public static final int RISK_HIGH = 3;
    public static final int RISK_LOW = 1;
    public static final int RISK_MEDIUM = 2;
    public static final int RISK_NONE = 0;
    public static final int RISK_UNKNOWN = 4;
    public String appId;
    public int category;
    public String reportSource;
    public int restrictStatus;
    public int riskLevel;

    /* synthetic */ MaliInfoBean(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public MaliInfoBean(String appId, String reportSource, int riskLevel, int category, int restrictStatus) {
        this.appId = appId;
        this.reportSource = reportSource;
        this.riskLevel = riskLevel;
        this.category = category;
        this.restrictStatus = restrictStatus;
    }

    private MaliInfoBean(Parcel in) {
        this.appId = in.readString();
        this.reportSource = in.readString();
        this.riskLevel = in.readInt();
        this.category = in.readInt();
        this.restrictStatus = in.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.appId);
        out.writeString(this.reportSource);
        out.writeInt(this.riskLevel);
        out.writeInt(this.category);
        out.writeInt(this.restrictStatus);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ appId = ");
        stringBuilder.append(this.appId);
        stringBuilder.append(", reportSource = ");
        stringBuilder.append(this.reportSource);
        stringBuilder.append(", riskLevel = ");
        stringBuilder.append(this.riskLevel);
        stringBuilder.append(", category = ");
        stringBuilder.append(this.category);
        stringBuilder.append(", restrict = ");
        stringBuilder.append(this.restrictStatus);
        stringBuilder.append(" }");
        return stringBuilder.toString();
    }
}
