package com.android.server.rms.iaware.appmng;

import android.rms.iaware.AwareLog;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class AwareAppMngDfxData {
    public static final int DFX_DATA_TYPE_KILLINFO = 1;
    public static final int DFX_DATA_TYPE_STARTINFO = 2;
    private static final int MASK_CLASSRATE = 983040;
    private static final int MASK_CLEANRES = 1;
    private static final int MASK_RESTART = 2;
    private static final int MASK_SUBCLASS = 3840;
    private static final String SEPARATOR = "#";
    private static final String TAG = "AwareAppMngDfxData";
    public long mAppColdStartCount;
    public long mAppTotalStartCount;
    public long mAwareBigMemForceStop;
    public long mAwareBigMemKill;
    public long mAwareLowMemForceStop;
    public long mAwareLowMemKill;
    public int mHwAdj;
    public String mName;

    public AwareAppMngDfxData(String packageName, String processName) {
        StringBuffer buf = new StringBuffer();
        buf.append(packageName == null ? "" : packageName);
        buf.append("#");
        if (processName != null) {
            buf.append(processName);
        }
        this.mName = buf.toString();
        this.mHwAdj = 0;
    }

    public AwareAppMngDfxData(List<String> packageName, String processName, int subClsRate, int clsRate, boolean restart, boolean cleanRes) {
        if (packageName != null) {
            StringBuffer buf = new StringBuffer();
            for (String pkg : packageName) {
                buf.append(pkg);
                buf.append("#");
            }
            if (processName != null) {
                buf.append(processName);
            }
            this.mName = buf.toString();
            this.mHwAdj = 0;
            if (cleanRes) {
                this.mHwAdj = 1;
            }
            if (restart) {
                this.mHwAdj += 2;
            }
            this.mHwAdj += subClsRate << 8;
            this.mHwAdj += clsRate << 16;
        }
    }

    public String toString() {
        boolean restart = true;
        boolean cleanRes = (this.mHwAdj & 1) != 0;
        if ((this.mHwAdj & 2) == 0) {
            restart = false;
        }
        int i = this.mHwAdj;
        return "name:" + this.mName + ",cleanRes:" + cleanRes + ",restart:" + restart + ",classRate:" + ((i & 983040) >> 16) + ",subClass:" + ((i & 3840) >> 8);
    }

    public JSONObject makeJson(boolean killInfo) {
        JSONObject jsonObj = new JSONObject();
        if (killInfo) {
            try {
                jsonObj.put("AppName", this.mName);
                jsonObj.put("hwAdj", this.mHwAdj);
                jsonObj.put("bmk", this.mAwareBigMemKill);
                jsonObj.put("bmf", this.mAwareBigMemForceStop);
                jsonObj.put("lmk", this.mAwareLowMemKill);
                jsonObj.put("lmf", this.mAwareLowMemForceStop);
            } catch (JSONException e) {
                AwareLog.e(TAG, "makeJson error!");
            }
        } else {
            jsonObj.put("AppName", this.mName);
            jsonObj.put("cNum", this.mAppColdStartCount);
            jsonObj.put("hNum", this.mAppTotalStartCount > this.mAppColdStartCount ? this.mAppTotalStartCount - this.mAppColdStartCount : 0);
        }
        return jsonObj;
    }

    public String getKey() {
        StringBuilder sb = new StringBuilder();
        String str = this.mName;
        if (str == null) {
            str = "";
        }
        sb.append(str);
        sb.append(this.mHwAdj);
        return sb.toString();
    }

    public int hashCode() {
        return super.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AwareAppMngDfxData)) {
            return false;
        }
        AwareAppMngDfxData data = (AwareAppMngDfxData) obj;
        String str = this.mName;
        if (str == null || !str.equals(data.mName) || this.mHwAdj != data.mHwAdj) {
            return false;
        }
        return true;
    }

    public void addTrackeKillInfo(int type) {
        if (type == 0) {
            this.mAwareBigMemKill++;
        } else if (type == 1) {
            this.mAwareBigMemForceStop++;
        } else if (type == 2) {
            this.mAwareLowMemKill++;
        } else if (type == 3) {
            this.mAwareLowMemForceStop++;
        }
    }

    public void trackeAppStartInfo(int type) {
        if (type == 10) {
            this.mAppColdStartCount++;
        } else if (type == 11) {
            this.mAppTotalStartCount++;
        }
    }
}
