package tmsdk.common.module.qscanner.impl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayList;

public class b implements Parcelable {
    public static final Creator<b> CREATOR = new Creator<b>() {
        public b[] bb(int i) {
            return new b[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return l(parcel);
        }

        public b l(Parcel parcel) {
            Object -l_2_R = new b();
            -l_2_R.id = parcel.readInt();
            -l_2_R.type = parcel.readInt();
            -l_2_R.BR = parcel.readLong();
            -l_2_R.banUrls = parcel.createStringArrayList();
            -l_2_R.banIps = parcel.createStringArrayList();
            -l_2_R.name = parcel.readString();
            return -l_2_R;
        }

        public /* synthetic */ Object[] newArray(int i) {
            return bb(i);
        }
    };
    public long BR = 0;
    public ArrayList<String> banIps = null;
    public ArrayList<String> banUrls = null;
    public int id = 0;
    public String name = null;
    public int type = 0;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.id);
        parcel.writeInt(this.type);
        parcel.writeLong(this.BR);
        parcel.writeStringList(this.banUrls);
        parcel.writeStringList(this.banIps);
        parcel.writeString(this.name);
    }
}
