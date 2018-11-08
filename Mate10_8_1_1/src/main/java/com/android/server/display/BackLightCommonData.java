package com.android.server.display;

class BackLightCommonData {
    private BrightnessMode mBrightnessMode;
    private boolean mIsCommercialVersion;
    private boolean mIsProductEnable;
    private boolean mIsWindowManagerBrightnessMode;
    private int mSmoothAmbientLight;

    public enum BrightnessMode {
        AUTO,
        MANUAL
    }

    BackLightCommonData() {
    }

    public void setProductEnable(boolean isProductEnable) {
        this.mIsProductEnable = isProductEnable;
    }

    public boolean isProductEnable() {
        return this.mIsProductEnable;
    }

    public void setCommercialVersion(boolean isCommercialVersion) {
        this.mIsCommercialVersion = isCommercialVersion;
    }

    public boolean isCommercialVersion() {
        return this.mIsCommercialVersion;
    }

    public void setWindowManagerBrightnessMode(boolean isWindowManagerBrightnessMode) {
        this.mIsWindowManagerBrightnessMode = isWindowManagerBrightnessMode;
    }

    public boolean isWindowManagerBrightnessMode() {
        return this.mIsWindowManagerBrightnessMode;
    }

    public void setBrightnessMode(BrightnessMode mode) {
        this.mBrightnessMode = mode;
    }

    public BrightnessMode getBrightnessMode() {
        return this.mBrightnessMode;
    }

    public void setSmoothAmbientLight(int value) {
        this.mSmoothAmbientLight = value;
    }

    public int getSmoothAmbientLight() {
        return this.mSmoothAmbientLight;
    }
}
