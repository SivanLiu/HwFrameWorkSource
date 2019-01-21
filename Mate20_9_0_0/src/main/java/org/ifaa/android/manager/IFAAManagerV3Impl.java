package org.ifaa.android.manager;

import android.content.Context;
import android.provider.Settings.Global;
import android.util.Log;
import com.huawei.android.os.SystemPropertiesEx;
import huawei.android.hardware.fingerprint.FingerprintManagerEx;
import org.json.JSONException;
import org.json.JSONObject;

public class IFAAManagerV3Impl extends IFAAManagerV3 {
    private static final String APS_INIT_HEIGHT = "aps_init_height";
    private static final int DEFAULT_INIT_HEIGHT = 2880;
    private static final int ENABLE_FINGERPRINT_VIEW_ICON_ONLY = 3;
    private static final String EXT_INFO_FAILURE = "NULL";
    private static final int FINGERPRINT_LOGO_COVER_HEIGHT = 320;
    private static final int FINGERPRINT_LOGO_COVER_WIDTH = 320;
    private static final int FINGERPRINT_SITE_ELEMETTS_COUNT = 4;
    private static final String TAG = "IFAAManagerV3Impl";
    private Context mContext = null;
    private IFAAManagerV2Impl mV2Impl;

    public IFAAManagerV3Impl(Context context) {
        this.mContext = context;
        this.mV2Impl = new IFAAManagerV2Impl(context);
    }

    public int getSupportBIOTypes(Context context) {
        int type = this.mV2Impl.getSupportBIOTypes(context);
        if (FingerprintManagerEx.hasFingerprintInScreen()) {
            Log.i(TAG, "support inner fingerprint");
            type |= 16;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("V3 getSupportBIOTypes is ");
        stringBuilder.append(type);
        Log.i(str, stringBuilder.toString());
        return type;
    }

    public int startBIOManager(Context context, int authType) {
        return this.mV2Impl.startBIOManager(context, authType);
    }

    public String getDeviceModel() {
        return this.mV2Impl.getDeviceModel();
    }

    public int getVersion() {
        return this.mV2Impl.getVersion();
    }

    public byte[] processCmdV2(Context context, byte[] param) {
        return this.mV2Impl.processCmdV2(context, param);
    }

    private String getInScreenFingerprintLocation() {
        if (FingerprintManagerEx.hasFingerprintInScreen()) {
            int[] position = FingerprintManagerEx.getHardwarePosition();
            if (4 != position.length) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("position sites length error as");
                stringBuilder.append(position.length);
                Log.e(str, stringBuilder.toString());
                return EXT_INFO_FAILURE;
            } else if (position[2] - position[0] <= 0 || position[3] - position[1] <= 0 || position[0] <= 0 || position[1] <= 0 || position[2] <= 0 || position[3] <= 0) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("position sites logical error ");
                stringBuilder2.append(position[0]);
                stringBuilder2.append(" ");
                stringBuilder2.append(position[1]);
                stringBuilder2.append(" ");
                stringBuilder2.append(position[2]);
                stringBuilder2.append(" ");
                stringBuilder2.append(position[3]);
                Log.e(str2, stringBuilder2.toString());
                return EXT_INFO_FAILURE;
            } else {
                int DefaultDisplayHeight = Global.getInt(this.mContext.getContentResolver(), APS_INIT_HEIGHT, DEFAULT_INIT_HEIGHT);
                float scale = ((float) SystemPropertiesEx.getInt("persist.sys.rog.height", DefaultDisplayHeight)) / ((float) DefaultDisplayHeight);
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("scale is");
                stringBuilder3.append(scale);
                Log.i(str3, stringBuilder3.toString());
                int startX = (int) (((float) (((position[2] + position[0]) / 2) - 160)) * scale);
                int startY = (int) (((float) (((position[3] + position[1]) / 2) - 160)) * scale);
                int width = (int) (1134559232 * scale);
                int height = (int) (1134559232 * scale);
                JSONObject location = new JSONObject();
                JSONObject fullView = new JSONObject();
                try {
                    fullView.put("startX", startX);
                    fullView.put("startY", startY);
                    fullView.put("width", width);
                    fullView.put("height", height);
                    fullView.put("unit", "px");
                    fullView.put("navConflict", false);
                    location.put("type", 0);
                    location.put("fullView", fullView);
                    return location.toString();
                } catch (JSONException ex) {
                    String str4 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("json expection ");
                    stringBuilder4.append(ex);
                    Log.e(str4, stringBuilder4.toString());
                    return EXT_INFO_FAILURE;
                }
            }
        }
        Log.e(TAG, "don't support inside fingerprint");
        return EXT_INFO_FAILURE;
    }

    public String getExtInfo(int authType, String keyExtInfo) {
        Log.i(TAG, "getExtInfo in v3 ");
        if (1 == authType && keyExtInfo.equals(IFAAManagerV3.KEY_GET_SENSOR_LOCATION)) {
            return getInScreenFingerprintLocation();
        }
        return EXT_INFO_FAILURE;
    }

    public void setExtInfo(int authType, String keyExtInfo, String valExtInfo) {
        if (1 == authType && keyExtInfo.equals(IFAAManagerV3.KEY_FINGERPRINT_FULLVIEW)) {
            if (!FingerprintManagerEx.hasFingerprintInScreen()) {
                Log.e(TAG, "don't support inside fingerprint");
                return;
            } else if (this.mContext == null) {
                Log.e(TAG, "mContext empty!");
                return;
            } else {
                FingerprintManagerEx fpManager = new FingerprintManagerEx(this.mContext);
                if (valExtInfo.equals(IFAAManagerV3.VALUE_FINGERPRINT_DISABLE)) {
                    fpManager.disableFingerprintView(true);
                } else if (valExtInfo.equals(IFAAManagerV3.VLAUE_FINGERPRINT_ENABLE)) {
                    fpManager.enableFingerprintView(true, 3);
                }
            }
        }
        Log.i(TAG, "setExtInfo finish");
    }
}
