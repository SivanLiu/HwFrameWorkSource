package android.aps;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ApsAppInfo implements Parcelable {
    public static final int BRIGHTNESS = 8;
    public static final Creator<ApsAppInfo> CREATOR = new Creator<ApsAppInfo>() {
        public ApsAppInfo createFromParcel(Parcel in) {
            return new ApsAppInfo(in);
        }

        public ApsAppInfo[] newArray(int size) {
            return new ApsAppInfo[size];
        }
    };
    public static final int DEFAULT_VALUE_BRIGHTNESS = 100;
    public static final boolean DEFAULT_VALUE_FB_SKIP = false;
    public static final int DEFAULT_VALUE_FRAMERATE = 60;
    public static final boolean DEFAULT_VALUE_HIGHP_TO_LOWP = false;
    public static final int DEFAULT_VALUE_MIPMAP = 0;
    public static final float DEFAULT_VALUE_RESOLUTION = 1.0f;
    public static final int DEFAULT_VALUE_SHADOWMAP = 0;
    public static final int DEFAULT_VALUE_TEXTURE = 100;
    public static final int FB_SKIP = 16;
    public static final int FRAMERATE = 2;
    public static final int HIGHP_TO_LOWP = 32;
    public static final int MIPMAP = 128;
    public static final int RESOLUTION = 1;
    public static final int SHADOWMAP = 64;
    private static final String TAG = "ApsAppInfo";
    public static final int TEXTURE = 4;
    private String mBasePackageName = "";
    private int mBrightnessPercent = 100;
    private boolean mFBSkipSwitch = false;
    private int mFrameRate = 60;
    private boolean mHighpToLowpSwitch = false;
    private int mMipMapSwitch = 0;
    private float mResolutionRatio = 1.0f;
    private int mShadowMapSwitch = 0;
    private boolean mSwitchable = false;
    private int mTexturePercent = 100;

    private ApsAppInfo() {
    }

    public ApsAppInfo(Parcel parcel) {
        readFromParcel(parcel);
    }

    public ApsAppInfo(ApsAppInfo info) {
        this.mBasePackageName = info.mBasePackageName;
        this.mResolutionRatio = info.mResolutionRatio;
        this.mFrameRate = info.mFrameRate;
        this.mTexturePercent = info.mTexturePercent;
        this.mBrightnessPercent = info.mBrightnessPercent;
        this.mSwitchable = info.mSwitchable;
    }

    public ApsAppInfo(String pkgName, float resolutionRatio, int frameRatio, int texturePercent, int brightnessPercent, boolean switchable) {
        this.mBasePackageName = pkgName;
        this.mResolutionRatio = resolutionRatio;
        this.mFrameRate = frameRatio;
        this.mTexturePercent = texturePercent;
        this.mBrightnessPercent = brightnessPercent;
        this.mSwitchable = switchable;
    }

    public ApsAppInfo(String pkgName, float resolutionRatio, int frameRatio, int texturePercent, int brightnessPercent, boolean switchable, boolean fbSkipSwitch, boolean highpToLowpSwitch, int shadowMapSwitch, int mipMapSwitch) {
        this.mBasePackageName = pkgName;
        this.mResolutionRatio = resolutionRatio;
        this.mFrameRate = frameRatio;
        this.mTexturePercent = texturePercent;
        this.mBrightnessPercent = brightnessPercent;
        this.mSwitchable = switchable;
        this.mFBSkipSwitch = fbSkipSwitch;
        this.mHighpToLowpSwitch = highpToLowpSwitch;
        this.mShadowMapSwitch = shadowMapSwitch;
        this.mMipMapSwitch = mipMapSwitch;
    }

    public void setBasePackageName(String pkgName) {
        this.mBasePackageName = pkgName;
    }

    public void setResolutionRatio(float resolutionRatio, boolean switchable) {
        this.mResolutionRatio = resolutionRatio;
        this.mSwitchable = switchable;
    }

    public void setFps(int frameRate) {
        this.mFrameRate = frameRate;
    }

    public void setTexturePercent(int texturePercent) {
        this.mTexturePercent = texturePercent;
    }

    public void setBrightnessPercent(int brightnessPercent) {
        this.mBrightnessPercent = brightnessPercent;
    }

    public String getBasePackageName() {
        return this.mBasePackageName;
    }

    public float getResolutionRatio() {
        return this.mResolutionRatio;
    }

    public int getFrameRatio() {
        return this.mFrameRate;
    }

    public int getTexturePercent() {
        return this.mTexturePercent;
    }

    public int getBrightnessPercent() {
        return this.mBrightnessPercent;
    }

    public boolean getSwitchable() {
        return this.mSwitchable;
    }

    public boolean getFBSkipSwitch() {
        return this.mFBSkipSwitch;
    }

    public void setFBSkipSwitch(boolean fbSkipSwitch) {
        this.mFBSkipSwitch = fbSkipSwitch;
    }

    public boolean getHighpToLowpSwitch() {
        return this.mHighpToLowpSwitch;
    }

    public void setHighpToLowpSwitch(boolean highpToLowpSwitch) {
        this.mHighpToLowpSwitch = highpToLowpSwitch;
    }

    public int getShadowMapSwitch() {
        return this.mShadowMapSwitch;
    }

    public void setShadowMapSwitch(int shadowMapSwitch) {
        this.mShadowMapSwitch = shadowMapSwitch;
    }

    public int getMipMapSwitch() {
        return this.mMipMapSwitch;
    }

    public void setMipMapSwitch(int mipMapSwitch) {
        this.mMipMapSwitch = mipMapSwitch;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(this.mBasePackageName);
        dest.writeFloat(this.mResolutionRatio);
        dest.writeInt(this.mFrameRate);
        dest.writeInt(this.mTexturePercent);
        dest.writeInt(this.mBrightnessPercent);
        dest.writeByte((byte) (this.mSwitchable ? 1 : 0));
    }

    private void readFromParcel(Parcel in) {
        boolean z = false;
        this.mBasePackageName = in.readString();
        this.mResolutionRatio = in.readFloat();
        this.mFrameRate = in.readInt();
        this.mTexturePercent = in.readInt();
        this.mBrightnessPercent = in.readInt();
        if (in.readByte() != (byte) 0) {
            z = true;
        }
        this.mSwitchable = z;
    }

    public String toString() {
        return "ApsAppInfo [mBasePackageName=" + this.mBasePackageName + ", mResolutionRatio=" + this.mResolutionRatio + ", mFrameRate=" + this.mFrameRate + ", mTexturePercent=" + this.mTexturePercent + ", mBrightnessPercent=" + this.mBrightnessPercent + ", mSwitchable=" + this.mSwitchable + ", mFBSkipSwitch=" + this.mFBSkipSwitch + ", mHighpToLowpSwitch=" + this.mHighpToLowpSwitch + ", mShadowMapSwitch=" + this.mShadowMapSwitch + ", mMipMapSwitch=" + this.mMipMapSwitch + "]";
    }

    public int hashCode() {
        int i;
        int i2 = 1231;
        int hashCode = ((((this.mBasePackageName == null ? 0 : this.mBasePackageName.hashCode()) + 31) * 31) + this.mBrightnessPercent) * 31;
        if (this.mFBSkipSwitch) {
            i = 1231;
        } else {
            i = 1237;
        }
        hashCode = (((hashCode + i) * 31) + this.mFrameRate) * 31;
        if (this.mHighpToLowpSwitch) {
            i = 1231;
        } else {
            i = 1237;
        }
        i = (((((((hashCode + i) * 31) + this.mMipMapSwitch) * 31) + Float.floatToIntBits(this.mResolutionRatio)) * 31) + this.mShadowMapSwitch) * 31;
        if (!this.mSwitchable) {
            i2 = 1237;
        }
        return ((i + i2) * 31) + this.mTexturePercent;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ApsAppInfo other = (ApsAppInfo) obj;
        if (this.mBasePackageName == null) {
            if (other.mBasePackageName != null) {
                return false;
            }
        } else if (!this.mBasePackageName.equals(other.mBasePackageName)) {
            return false;
        }
        return this.mBrightnessPercent == other.mBrightnessPercent && this.mFBSkipSwitch == other.mFBSkipSwitch && this.mFrameRate == other.mFrameRate && this.mHighpToLowpSwitch == other.mHighpToLowpSwitch && this.mMipMapSwitch == other.mMipMapSwitch && Float.floatToIntBits(this.mResolutionRatio) == Float.floatToIntBits(other.mResolutionRatio) && this.mShadowMapSwitch == other.mShadowMapSwitch && this.mSwitchable == other.mSwitchable && this.mTexturePercent == other.mTexturePercent;
    }
}
