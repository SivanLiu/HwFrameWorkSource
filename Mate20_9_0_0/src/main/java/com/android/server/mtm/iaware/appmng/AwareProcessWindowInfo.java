package com.android.server.mtm.iaware.appmng;

import android.util.ArrayMap;
import java.util.Map;
import java.util.Map.Entry;

public class AwareProcessWindowInfo {
    private static int mMinWindowHeight = 1;
    private static int mMinWindowWidth = 1;
    public boolean mInRestriction = false;
    public int mMode;
    public String mPkg;
    public int mUid;
    public Map<Integer, Boolean> mWindows = new ArrayMap();

    public AwareProcessWindowInfo(int mode, String pkg, int uid) {
        this.mMode = mode;
        this.mPkg = pkg;
        this.mUid = uid;
    }

    public boolean containsWindow(int code) {
        return this.mWindows.containsKey(Integer.valueOf(code));
    }

    public void addWindow(Integer code, boolean evil) {
        this.mWindows.put(code, Boolean.valueOf(evil));
    }

    public void removeWindow(Integer code) {
        this.mWindows.remove(code);
    }

    public boolean isEvil() {
        for (Entry<Integer, Boolean> window : this.mWindows.entrySet()) {
            if (!((Boolean) window.getValue()).booleanValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean isEvil(int code) {
        return ((Boolean) this.mWindows.get(Integer.valueOf(code))).booleanValue();
    }

    public static int getMinWindowWidth() {
        return mMinWindowWidth;
    }

    public static int getMinWindowHeight() {
        return mMinWindowHeight;
    }

    public static void setMinWindowWidth(int width) {
        mMinWindowWidth = width;
    }

    public static void setMinWindowHeight(int height) {
        mMinWindowHeight = height;
    }
}
