package huawei.cust.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayList;
import java.util.List;

public class SimFileInfo implements Parcelable {
    public static final Creator<SimFileInfo> CREATOR = new Creator<SimFileInfo>() {
        public SimFileInfo createFromParcel(Parcel in) {
            return new SimFileInfo(in);
        }

        public SimFileInfo[] newArray(int size) {
            return new SimFileInfo[size];
        }
    };
    private String gid1;
    private String gid2;
    private String iccid;
    private String imsi;
    private String mccMnc;
    List<SpecialFile> specialFiles = new ArrayList();
    private String spn;

    protected SimFileInfo(Parcel in) {
        this.mccMnc = in.readString();
        this.imsi = in.readString();
        this.iccid = in.readString();
        this.spn = in.readString();
        this.gid1 = in.readString();
        this.gid2 = in.readString();
        this.specialFiles = in.createTypedArrayList(SpecialFile.CREATOR);
    }

    public String getMccMnc() {
        return this.mccMnc;
    }

    public void setMccMnc(String mccMnc) {
        this.mccMnc = mccMnc;
    }

    public String getImsi() {
        return this.imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public String getIccid() {
        return this.iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public String getSpn() {
        return this.spn;
    }

    public void setSpn(String spn) {
        this.spn = spn;
    }

    public String getGid1() {
        return this.gid1;
    }

    public void setGid1(String gid1) {
        this.gid1 = gid1;
    }

    public String getGid2() {
        return this.gid2;
    }

    public void setGid2(String gid2) {
        this.gid2 = gid2;
    }

    public List<SpecialFile> getSpecialFiles() {
        return this.specialFiles;
    }

    public void setSpecialFiles(List<SpecialFile> specialFiles) {
        this.specialFiles = specialFiles;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mccMnc);
        dest.writeString(this.imsi);
        dest.writeString(this.iccid);
        dest.writeString(this.spn);
        dest.writeString(this.gid1);
        dest.writeString(this.gid2);
        dest.writeTypedList(this.specialFiles);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SimFileInfo{mccMnc='");
        stringBuilder.append(this.mccMnc);
        stringBuilder.append('\'');
        stringBuilder.append(", imsi='");
        stringBuilder.append(givePrintableMsg(this.imsi));
        stringBuilder.append('\'');
        stringBuilder.append(", iccid='");
        stringBuilder.append(givePrintableMsg(this.iccid));
        stringBuilder.append('\'');
        stringBuilder.append(", spn='");
        stringBuilder.append(this.spn);
        stringBuilder.append('\'');
        stringBuilder.append(", gid1='");
        stringBuilder.append(this.gid1);
        stringBuilder.append('\'');
        stringBuilder.append(", gid2='");
        stringBuilder.append(this.gid2);
        stringBuilder.append('\'');
        stringBuilder.append(", specialFiles=");
        stringBuilder.append(this.specialFiles);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    private static String givePrintableMsg(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= 6) {
            return value;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(value.substring(0, 6));
        stringBuilder.append("XXXXXXXXXXX");
        return stringBuilder.toString();
    }
}
