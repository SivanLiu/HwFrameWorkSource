package android.aps;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ApsColorInfo implements Parcelable {
    public static final Creator<ApsColorInfo> CREATOR = new Creator<ApsColorInfo>() {
        public ApsColorInfo createFromParcel(Parcel in) {
            return new ApsColorInfo(in);
        }

        public ApsColorInfo[] newArray(int size) {
            return new ApsColorInfo[size];
        }
    };
    private String actName;
    private int dstColor;
    private int srcColor;
    private int type;

    private ApsColorInfo() {
    }

    public ApsColorInfo(Parcel parcel) {
        readFromParcel(parcel);
    }

    public ApsColorInfo(int type, int srcColor, int dstColor, String actName) {
        this.type = type;
        this.srcColor = srcColor;
        this.dstColor = dstColor;
        this.actName = actName;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSrcColor() {
        return this.srcColor;
    }

    public void setSrcColor(int srcColor) {
        this.srcColor = srcColor;
    }

    public int getDstColor() {
        return this.dstColor;
    }

    public void setDstColor(int dstColor) {
        this.dstColor = dstColor;
    }

    public String getActName() {
        return this.actName;
    }

    public void setActName(String actName) {
        this.actName = actName;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flag) {
        dest.writeInt(this.type);
        dest.writeInt(this.srcColor);
        dest.writeInt(this.dstColor);
        dest.writeString(this.actName);
    }

    private void readFromParcel(Parcel in) {
        this.type = in.readInt();
        this.srcColor = in.readInt();
        this.dstColor = in.readInt();
        this.actName = in.readString();
    }
}
