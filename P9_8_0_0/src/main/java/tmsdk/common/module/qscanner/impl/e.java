package tmsdk.common.module.qscanner.impl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class e implements Parcelable, Serializable {
    public static final Creator<e> CREATOR = new Creator<e>() {
        public e[] bc(int i) {
            return new e[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return m(parcel);
        }

        public e m(Parcel parcel) {
            boolean z = false;
            Object -l_2_R = new e();
            -l_2_R.packageName = parcel.readString();
            -l_2_R.softName = parcel.readString();
            -l_2_R.version = parcel.readString();
            -l_2_R.versionCode = parcel.readInt();
            -l_2_R.path = parcel.readString();
            -l_2_R.BQ = parcel.readInt();
            -l_2_R.bZ = parcel.readString();
            -l_2_R.Cb = parcel.readString();
            -l_2_R.size = parcel.readInt();
            -l_2_R.cc = parcel.readString();
            -l_2_R.plugins = parcel.createTypedArrayList(b.CREATOR);
            -l_2_R.name = parcel.readString();
            -l_2_R.type = parcel.readInt();
            -l_2_R.lL = parcel.readInt();
            -l_2_R.BU = parcel.readInt();
            -l_2_R.name = parcel.readString();
            -l_2_R.label = parcel.readString();
            -l_2_R.BT = parcel.readString();
            -l_2_R.url = parcel.readString();
            -l_2_R.gS = parcel.readInt();
            -l_2_R.Cc = parcel.readString();
            int -l_3_I = parcel.readInt();
            if (-l_3_I > 0) {
                -l_2_R.Cd = new ArrayList(-l_3_I);
                parcel.readStringList(-l_2_R.Cd);
            }
            -l_2_R.Ce = parcel.readInt();
            -l_2_R.Cf = parcel.readInt();
            -l_2_R.Cg = parcel.readByte() == (byte) 1;
            -l_2_R.Ch = parcel.readByte() == (byte) 1;
            -l_2_R.Ci = parcel.readByte() == (byte) 1;
            -l_2_R.Cj = parcel.readByte() == (byte) 1;
            -l_2_R.dp = parcel.readInt();
            -l_2_R.category = parcel.readInt();
            -l_2_R.official = parcel.readInt();
            -l_2_R.Ck = parcel.readString();
            -l_2_R.Cl = parcel.readString();
            -l_2_R.Cm = parcel.readByte() == (byte) 1;
            -l_2_R.lastModified = parcel.readLong();
            -l_2_R.Cn = parcel.readByte() == (byte) 1;
            if (parcel.readByte() == (byte) 1) {
                z = true;
            }
            -l_2_R.Co = z;
            return -l_2_R;
        }

        public /* synthetic */ Object[] newArray(int i) {
            return bc(i);
        }
    };
    public int BQ;
    public String BT;
    public int BU;
    public String Cb;
    public String Cc;
    public List<String> Cd;
    public int Ce = -1;
    public int Cf = -1;
    public boolean Cg = false;
    public boolean Ch = false;
    public boolean Ci = false;
    public boolean Cj = false;
    public String Ck;
    public String Cl;
    public boolean Cm;
    public boolean Cn;
    public boolean Co = false;
    public String bZ;
    public int category = 0;
    public String cc;
    public int dp = 0;
    public String fA;
    public int gS;
    public int lL;
    public String label;
    public long lastModified;
    public String name;
    public int official = 0;
    public String packageName;
    public String path;
    public ArrayList<b> plugins;
    public int size;
    public String softName;
    public int type;
    public String url;
    public String version;
    public int versionCode;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        int i2 = 0;
        parcel.writeString(this.packageName);
        parcel.writeString(this.softName);
        parcel.writeString(this.version);
        parcel.writeInt(this.versionCode);
        parcel.writeString(this.path);
        parcel.writeInt(this.BQ);
        parcel.writeString(this.bZ);
        parcel.writeString(this.Cb);
        parcel.writeInt(this.size);
        parcel.writeString(this.cc);
        parcel.writeTypedList(this.plugins);
        parcel.writeString(this.name);
        parcel.writeInt(this.type);
        parcel.writeInt(this.lL);
        parcel.writeInt(this.BU);
        parcel.writeString(this.name);
        parcel.writeString(this.label);
        parcel.writeString(this.BT);
        parcel.writeString(this.url);
        parcel.writeInt(this.gS);
        parcel.writeString(this.Cc);
        if (this.Cd == null || this.Cd.size() == 0) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(this.Cd.size());
            parcel.writeStringList(this.Cd);
        }
        parcel.writeInt(this.Ce);
        parcel.writeInt(this.Cf);
        parcel.writeByte((byte) (!this.Cg ? 0 : 1));
        parcel.writeByte((byte) (!this.Ch ? 0 : 1));
        parcel.writeByte((byte) (!this.Ci ? 0 : 1));
        parcel.writeByte((byte) (!this.Cj ? 0 : 1));
        parcel.writeInt(this.dp);
        parcel.writeInt(this.category);
        parcel.writeInt(this.official);
        parcel.writeString(this.Ck);
        parcel.writeString(this.Cl);
        parcel.writeByte((byte) (!this.Cm ? 0 : 1));
        parcel.writeLong(this.lastModified);
        parcel.writeByte((byte) (!this.Cn ? 0 : 1));
        if (this.Co) {
            i2 = 1;
        }
        parcel.writeByte((byte) i2);
    }
}
