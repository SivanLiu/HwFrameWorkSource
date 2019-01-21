package android.content.pm;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ConfigurationInfo implements Parcelable {
    public static final Creator<ConfigurationInfo> CREATOR = new Creator<ConfigurationInfo>() {
        public ConfigurationInfo createFromParcel(Parcel source) {
            return new ConfigurationInfo(source, null);
        }

        public ConfigurationInfo[] newArray(int size) {
            return new ConfigurationInfo[size];
        }
    };
    public static final int GL_ES_VERSION_UNDEFINED = 0;
    public static final int INPUT_FEATURE_FIVE_WAY_NAV = 2;
    public static final int INPUT_FEATURE_HARD_KEYBOARD = 1;
    public int reqGlEsVersion;
    public int reqInputFeatures;
    public int reqKeyboardType;
    public int reqNavigation;
    public int reqTouchScreen;

    /* synthetic */ ConfigurationInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public ConfigurationInfo() {
        this.reqInputFeatures = 0;
    }

    public ConfigurationInfo(ConfigurationInfo orig) {
        this.reqInputFeatures = 0;
        this.reqTouchScreen = orig.reqTouchScreen;
        this.reqKeyboardType = orig.reqKeyboardType;
        this.reqNavigation = orig.reqNavigation;
        this.reqInputFeatures = orig.reqInputFeatures;
        this.reqGlEsVersion = orig.reqGlEsVersion;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ConfigurationInfo{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" touchscreen = ");
        stringBuilder.append(this.reqTouchScreen);
        stringBuilder.append(" inputMethod = ");
        stringBuilder.append(this.reqKeyboardType);
        stringBuilder.append(" navigation = ");
        stringBuilder.append(this.reqNavigation);
        stringBuilder.append(" reqInputFeatures = ");
        stringBuilder.append(this.reqInputFeatures);
        stringBuilder.append(" reqGlEsVersion = ");
        stringBuilder.append(this.reqGlEsVersion);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeInt(this.reqTouchScreen);
        dest.writeInt(this.reqKeyboardType);
        dest.writeInt(this.reqNavigation);
        dest.writeInt(this.reqInputFeatures);
        dest.writeInt(this.reqGlEsVersion);
    }

    private ConfigurationInfo(Parcel source) {
        this.reqInputFeatures = 0;
        this.reqTouchScreen = source.readInt();
        this.reqKeyboardType = source.readInt();
        this.reqNavigation = source.readInt();
        this.reqInputFeatures = source.readInt();
        this.reqGlEsVersion = source.readInt();
    }

    public String getGlEsVersion() {
        int major = (this.reqGlEsVersion & Color.RED) >> 16;
        int minor = this.reqGlEsVersion & 65535;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.valueOf(major));
        stringBuilder.append(".");
        stringBuilder.append(String.valueOf(minor));
        return stringBuilder.toString();
    }
}
