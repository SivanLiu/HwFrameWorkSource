package com.huawei.android.smcs;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import java.util.HashSet;
import java.util.Iterator;

public final class STProcessRecord implements Parcelable {
    public static final Creator<STProcessRecord> CREATOR = new Creator<STProcessRecord>() {
        public STProcessRecord createFromParcel(Parcel source) {
            return new STProcessRecord(source);
        }

        public STProcessRecord[] newArray(int size) {
            return new STProcessRecord[size];
        }
    };
    private static final String TAG = "STProcessRecord";
    private static final boolean mDebugLocalClass = false;
    public int curAdj;
    public int pid;
    public HashSet<String> pkgList = null;
    public String processName = null;
    public int uid;

    public STProcessRecord(String processName, int uid, int pid, int curAdj, HashSet<String> pkgList) {
        this.processName = processName;
        this.uid = uid;
        this.pid = pid;
        this.curAdj = curAdj;
        this.pkgList = pkgList;
    }

    STProcessRecord(Parcel source) {
        readFromParcel(source);
    }

    public int hashCode() {
        return this.processName.hashCode();
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        try {
            if ((o instanceof STProcessRecord) && ((STProcessRecord) o).processName.equals(this.processName)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("STProcessRecord.equals: catch exception ");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("STProcessRecord: \n");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("    processName: ");
        stringBuilder.append(this.processName);
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("\n    curAdj ");
        stringBuilder.append(this.curAdj);
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("\n    pkgs: ");
        stringBuilder.append(this.pkgList);
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("\n    uid ");
        stringBuilder.append(this.uid);
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("\n    pid ");
        stringBuilder.append(this.pid);
        sb.append(stringBuilder.toString());
        return sb.toString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i = 0;
        dest.writeString(this.processName);
        dest.writeInt(this.uid);
        dest.writeInt(this.pid);
        dest.writeInt(this.curAdj);
        String[] pkgs = new String[this.pkgList.size()];
        Iterator<String> it = this.pkgList.iterator();
        while (it.hasNext()) {
            pkgs[i] = (String) it.next();
            i++;
        }
        dest.writeStringArray(pkgs);
    }

    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel source) {
        this.processName = source.readString();
        this.uid = source.readInt();
        this.pid = source.readInt();
        this.curAdj = source.readInt();
        String[] pkgs = source.readStringArray();
        if (pkgs != null) {
            this.pkgList = new HashSet();
            for (Object add : pkgs) {
                this.pkgList.add(add);
            }
        }
    }
}
