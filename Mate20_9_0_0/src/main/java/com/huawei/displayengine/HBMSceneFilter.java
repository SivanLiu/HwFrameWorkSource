package com.huawei.displayengine;

import android.os.Bundle;

public class HBMSceneFilter {
    private static final String TAG = "DE J HBMSceneFilter";
    private int mBacklightLevel = 0;
    private boolean mDimming = true;
    private int mDimmingThreshould = 3921;
    private DisplayEngineManager mDisplayEngineManager = new DisplayEngineManager();
    private float mDurationThreshould = 0.5f;
    private volatile boolean mInitialized = false;
    private Object mLock = new Object();
    private float mSpeedThreshould = 0.0f;
    private boolean mSupport = false;
    private int mThreshould = 7254;

    private void initialize() {
        if (!this.mInitialized) {
            synchronized (this.mLock) {
                if (!this.mInitialized) {
                    byte[] info = new byte[3];
                    if (this.mDisplayEngineManager.getEffect(25, 1, info, info.length) != 0) {
                        DElog.w(TAG, "Failed to get HBM information and use default value!");
                    } else {
                        boolean z = false;
                        if (info[0] == (byte) 1) {
                            z = true;
                        }
                        this.mSupport = z;
                        this.mThreshould = ((info[1] & 255) * 10000) / 255;
                        this.mDimmingThreshould = ((info[2] & 255) * 10000) / 255;
                        this.mSpeedThreshould = ((float) (this.mThreshould - this.mDimmingThreshould)) / this.mDurationThreshould;
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Get information from hal: mSupport=");
                        stringBuilder.append(this.mSupport);
                        stringBuilder.append(" mThreshould=");
                        stringBuilder.append(this.mThreshould);
                        stringBuilder.append(" mDimmingThreshould=");
                        stringBuilder.append(this.mDimmingThreshould);
                        stringBuilder.append(" mSpeedThreshould=");
                        stringBuilder.append(this.mSpeedThreshould);
                        DElog.i(str, stringBuilder.toString());
                    }
                    this.mInitialized = true;
                }
            }
        }
    }

    public boolean check(int scene, int action) {
        initialize();
        if (scene == 26 && this.mSupport) {
            this.mBacklightLevel = action >> 16;
        }
        return false;
    }

    public int setData(Bundle data) {
        initialize();
        if (this.mSupport) {
            int target = data.getInt("target");
            int rate = data.getInt("rate");
            float duration = data.getFloat("duration");
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hbm_dimming: target=");
            stringBuilder.append(target);
            stringBuilder.append(" rate=");
            stringBuilder.append(rate);
            stringBuilder.append(" duration=");
            stringBuilder.append(duration);
            stringBuilder.append(" backlight10000=");
            stringBuilder.append(this.mBacklightLevel);
            DElog.d(str, stringBuilder.toString());
            if (target >= this.mThreshould) {
                if (!this.mDimming) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("hbm_dimming on: target=");
                    stringBuilder2.append(target);
                    DElog.i(str, stringBuilder2.toString());
                    this.mDisplayEngineManager.setScene(28, 16);
                    this.mDimming = true;
                }
            } else if (this.mBacklightLevel >= this.mThreshould) {
                boolean dimming = true;
                if (rate == 0 || ((double) Math.abs(duration)) < 1.0E-6d) {
                    dimming = false;
                } else if (target < this.mDimmingThreshould) {
                    float speed = ((float) (this.mBacklightLevel - target)) / duration;
                    String str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("gz hbm_dimming check speed=");
                    stringBuilder3.append(speed);
                    stringBuilder3.append(" mSpeedThreshould=");
                    stringBuilder3.append(this.mSpeedThreshould);
                    DElog.i(str2, stringBuilder3.toString());
                    if (speed > this.mSpeedThreshould) {
                        dimming = false;
                    }
                }
                if (this.mDimming && !dimming) {
                    String str3 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("gz hbm_dimming off: target=");
                    stringBuilder4.append(target);
                    stringBuilder4.append(" rate=");
                    stringBuilder4.append(rate);
                    stringBuilder4.append(" duration=");
                    stringBuilder4.append(duration);
                    stringBuilder4.append(" backlight10000=");
                    stringBuilder4.append(this.mBacklightLevel);
                    DElog.i(str3, stringBuilder4.toString());
                    this.mDisplayEngineManager.setScene(28, 17);
                    this.mDimming = false;
                }
            }
        }
        return 0;
    }
}
