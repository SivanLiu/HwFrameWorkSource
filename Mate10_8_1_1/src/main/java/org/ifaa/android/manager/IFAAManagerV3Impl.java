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
    public static final int IFAA_AUTH_INNER_FINGERPRINT = 16;
    private static final String TAG = "IFAAManagerV3Impl";
    private Context mContext = null;
    private IFAAManagerV2Impl mV2Impl = new IFAAManagerV2Impl();

    public IFAAManagerV3Impl(Context context) {
        this.mContext = context;
    }

    public int getSupportBIOTypes(Context context) {
        int type = this.mV2Impl.getSupportBIOTypes(context);
        if (FingerprintManagerEx.hasFingerprintInScreen()) {
            Log.i(TAG, "support inner fingerprint");
            type |= 16;
        }
        Log.i(TAG, "getSupportBIOTypes is " + type);
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
            if (FINGERPRINT_SITE_ELEMETTS_COUNT != position.length) {
                Log.e(TAG, "position sites length error as" + position.length);
                return EXT_INFO_FAILURE;
            } else if (position[2] - position[0] <= 0 || position[ENABLE_FINGERPRINT_VIEW_ICON_ONLY] - position[1] <= 0 || position[0] <= 0 || position[1] <= 0 || position[2] <= 0 || position[ENABLE_FINGERPRINT_VIEW_ICON_ONLY] <= 0) {
                Log.e(TAG, "position sites logical error " + position[0] + " " + position[1] + " " + position[2] + " " + position[ENABLE_FINGERPRINT_VIEW_ICON_ONLY]);
                return EXT_INFO_FAILURE;
            } else {
                int DefaultDisplayHeight = Global.getInt(this.mContext.getContentResolver(), APS_INIT_HEIGHT, DEFAULT_INIT_HEIGHT);
                float scale = ((float) SystemPropertiesEx.getInt("persist.sys.rog.height", DefaultDisplayHeight)) / ((float) DefaultDisplayHeight);
                Log.i(TAG, "scale is" + scale);
                int startX = (int) (((float) (((position[2] + position[0]) / 2) - 160)) * scale);
                int startY = (int) (((float) (((position[ENABLE_FINGERPRINT_VIEW_ICON_ONLY] + position[1]) / 2) - 160)) * scale);
                int width = (int) (320.0f * scale);
                int height = (int) (320.0f * scale);
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
                    Log.e(TAG, "json expection " + ex);
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
                    fpManager.enableFingerprintView(true, ENABLE_FINGERPRINT_VIEW_ICON_ONLY);
                }
            }
        }
        Log.i(TAG, "setExtInfo finish");
    }
}
