package com.huawei.android.smcs;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.util.Log;
import java.util.HashSet;
import java.util.StringTokenizer;

public final class SmartTrimProcessAddRelation extends SmartTrimProcessEvent {
    public static final Creator<SmartTrimProcessAddRelation> CREATOR = new Creator<SmartTrimProcessAddRelation>() {
        public SmartTrimProcessAddRelation createFromParcel(Parcel source) {
            return new SmartTrimProcessAddRelation(source);
        }

        public SmartTrimProcessAddRelation[] newArray(int size) {
            return new SmartTrimProcessAddRelation[size];
        }
    };
    private static final String TAG = "SmartTrimProcessAddRelation";
    private static final boolean mDebugLocalClass = false;
    public HashSet<String> mClientPkgList = null;
    public String mClientProc;
    public HashSet<String> mServerPkgList = null;
    public String mServerProc;

    public SmartTrimProcessAddRelation(String clientProc, HashSet<String> clientPkgList, String serverProc, HashSet<String> serverPkgList) {
        super(0);
        this.mClientProc = clientProc;
        this.mClientPkgList = clientPkgList;
        this.mServerProc = serverProc;
        this.mServerPkgList = serverPkgList;
    }

    SmartTrimProcessAddRelation(Parcel source) {
        super(source);
        readFromParcel(source);
    }

    SmartTrimProcessAddRelation(Parcel source, int event) {
        super(event);
        readFromParcel(source);
    }

    SmartTrimProcessAddRelation(StringTokenizer stzer) {
        super(0);
    }

    public int hashCode() {
        String hashCodeStr;
        try {
            hashCodeStr = new StringBuilder();
            hashCodeStr.append(this.mClientProc);
            hashCodeStr.append(this.mServerProc);
            return hashCodeStr.toString().hashCode();
        } catch (Exception e) {
            hashCodeStr = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SmartTrimProcessAddRelation.hashCode: catch exception ");
            stringBuilder.append(e.toString());
            Log.e(hashCodeStr, stringBuilder.toString());
            return -1;
        }
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        try {
            if (!(o instanceof SmartTrimProcessAddRelation)) {
                return false;
            }
            SmartTrimProcessAddRelation input = (SmartTrimProcessAddRelation) o;
            boolean clientEqual = input.mClientProc.equals(this.mClientProc);
            boolean serverEqual = input.mServerProc.equals(this.mServerProc);
            if (clientEqual && serverEqual) {
                return true;
            }
            return false;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SmartTrimProcessAddRelation.equals: catch exception ");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("SmartTrimProcessAddRelation:\n");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("client process: ");
        stringBuilder.append(this.mClientProc);
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("client pkg list: ");
        stringBuilder.append(this.mClientPkgList);
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("server process: ");
        stringBuilder.append(this.mServerProc);
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("server pkg list: ");
        stringBuilder.append(this.mServerPkgList);
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        return sb.toString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.mClientProc);
        dest.writeStringArray(hashSet2strings(this.mClientPkgList));
        dest.writeString(this.mServerProc);
        dest.writeStringArray(hashSet2strings(this.mServerPkgList));
    }

    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel source) {
        this.mClientProc = source.readString();
        this.mClientPkgList = strings2hashSet(source.readStringArray());
        this.mServerProc = source.readString();
        this.mServerPkgList = strings2hashSet(source.readStringArray());
    }
}
