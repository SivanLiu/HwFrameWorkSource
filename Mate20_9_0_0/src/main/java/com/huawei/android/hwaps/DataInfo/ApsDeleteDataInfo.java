package com.huawei.android.hwaps.DataInfo;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ApsDeleteDataInfo implements Parcelable {
    public static final Creator<ApsDeleteDataInfo> CREATOR = new Creator<ApsDeleteDataInfo>() {
        public ApsDeleteDataInfo createFromParcel(Parcel in) {
            return new ApsDeleteDataInfo(in.readString(), in.readString(), (String[]) in.readArray(null));
        }

        public ApsDeleteDataInfo[] newArray(int size) {
            return new ApsDeleteDataInfo[size];
        }
    };
    public String[] mSelectionArgs = null;
    public String mUri = null;
    public String mWhere = null;

    public ApsDeleteDataInfo(String strUri, String where, String[] selectionArgs) {
        this.mUri = strUri;
        this.mWhere = where;
        if (selectionArgs != null) {
            this.mSelectionArgs = (String[]) selectionArgs.clone();
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        synchronized (this) {
            dest.writeString(this.mUri);
            dest.writeString(this.mWhere);
            dest.writeArray(this.mSelectionArgs);
        }
    }

    public String toString() {
        StringBuilder stringBuilder;
        StringBuilder sb = new StringBuilder();
        if (this.mUri != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("ApsUpdateDataInfo: Uri:");
            stringBuilder.append(this.mUri);
            sb.append(stringBuilder.toString());
        }
        if (this.mWhere != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" where:");
            stringBuilder.append(this.mWhere);
            sb.append(stringBuilder.toString());
        }
        if (this.mSelectionArgs != null) {
            sb.append(" SelectionArgs:");
            for (String str : this.mSelectionArgs) {
                sb.append(str);
            }
        }
        return sb.toString();
    }
}
