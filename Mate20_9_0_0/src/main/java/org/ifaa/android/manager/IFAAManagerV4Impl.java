package org.ifaa.android.manager;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.os.SystemProperties;
import android.util.Log;
import com.huawei.facerecognition.FaceRecognizeManager;
import huawei.android.hardware.fingerprint.FingerprintManagerEx;
import java.util.List;
import org.ifaa.android.manager.face.IFAAFaceRecognizeManager;

public class IFAAManagerV4Impl extends IFAAManagerV4 {
    private static final int DEVICE_ALL = -1;
    private static final int ENABLE_TO_PAY = 1000;
    private static final int ID_NOT_ENROLLED = 1002;
    private static final int IFAA_VERSION_V4 = 4;
    private static final int MIN_SUPPORT_3D_FACE_MODE = 3;
    private static final int PHONE_NOT_SET_PASSWORD = 1003;
    private static final String SETTINGS_FACE_CLASS = "com.android.settings.facechecker.unlock.FaceUnLockSettingsActivity";
    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final int SYSTEM_LOCKED = 1001;
    private static final String TAG = "IFAAManagerV4Impl";
    private static final int mUserID = 0;
    private Context mContext = null;
    public FaceRecognizeManager mFaceManager = null;
    private FingerprintManagerEx mFingerManagerEx = null;
    private IFAAManagerV3Impl mV3Impl;

    public IFAAManagerV4Impl(Context context) {
        this.mContext = context;
        this.mV3Impl = new IFAAManagerV3Impl(this.mContext);
        IFAAFaceRecognizeManager.createInstance(this.mContext);
        this.mFaceManager = IFAAFaceRecognizeManager.getFRManager();
        this.mFingerManagerEx = new FingerprintManagerEx(this.mContext);
    }

    public int getSupportBIOTypes(Context context) {
        int type = this.mV3Impl.getSupportBIOTypes(context);
        int face_mode = SystemProperties.getInt("ro.config.support_face_mode", 0);
        boolean mIsChinaArea = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("face_mode is ");
        stringBuilder.append(face_mode);
        Log.e(str, stringBuilder.toString());
        if ((face_mode == 3 && mIsChinaArea) || face_mode > 3) {
            Log.e(TAG, "adding type");
            type |= 4;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("v4 type ");
        stringBuilder.append(type);
        Log.i(str, stringBuilder.toString());
        return type;
    }

    public int startBIOManager(Context context, int authType) {
        if (authType != 4) {
            return this.mV3Impl.startBIOManager(context, authType);
        }
        Intent i = new Intent();
        i.setClassName(SETTINGS_PACKAGE, SETTINGS_FACE_CLASS);
        i.setFlags(268435456);
        context.startActivity(i);
        return 0;
    }

    public String getDeviceModel() {
        return this.mV3Impl.getDeviceModel();
    }

    public int getVersion() {
        return 4;
    }

    public byte[] processCmdV2(Context context, byte[] param) {
        return this.mV3Impl.processCmdV2(context, param);
    }

    public String getExtInfo(int authType, String keyExtInfo) {
        return this.mV3Impl.getExtInfo(authType, keyExtInfo);
    }

    public void setExtInfo(int authType, String keyExtInfo, String valExtInfo) {
        this.mV3Impl.setExtInfo(authType, keyExtInfo, valExtInfo);
    }

    private boolean isBioTypeOK(int type) {
        if (type == 1 || type == 4) {
            return true;
        }
        return false;
    }

    public int getEnabled(int type) {
        if (!isBioTypeOK(type)) {
            Log.e(TAG, "unsupport type");
            return -1;
        } else if (!isBiometricsExsit(type, 0)) {
            Log.i(TAG, "getEnabled no id exist");
            return ID_NOT_ENROLLED;
        } else if (getRemainingNum(type) < 1) {
            Log.i(TAG, "getEnabled sys locked");
            return SYSTEM_LOCKED;
        } else {
            Log.i(TAG, "getEnabled ok");
            return ENABLE_TO_PAY;
        }
    }

    public int[] getIDList(int type) {
        Log.i(TAG, "getIDList enter");
        int[] result = new int[0];
        if (isBioTypeOK(type)) {
            result = getBiometricsID(type, 0);
            if (result.length > 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("id list 0 is ");
                stringBuilder.append(result[0]);
                Log.i(str, stringBuilder.toString());
            } else {
                Log.w(TAG, "no id list exist");
            }
            return result;
        }
        Log.e(TAG, "unsupport type");
        return result;
    }

    private int[] getBiometricsID(int type, int userID) {
        if (type != 1) {
            return this.mFaceManager.getEnrolledFaceIDs();
        }
        List<Fingerprint> items = this.mFingerManagerEx.getEnrolledFingerprints(-1, userID);
        int fingerprintCount = items.size();
        int[] result = new int[fingerprintCount];
        for (int i = 0; i < fingerprintCount; i++) {
            result[i] = ((Fingerprint) items.get(i)).getFingerId();
        }
        return result;
    }

    private boolean isBiometricsExsit(int type, int userID) {
        int[] ret = getBiometricsID(type, userID);
        if (ret != null && ret.length >= 1) {
            return true;
        }
        Log.e(TAG, "Biometrics ID not enrolled!");
        return false;
    }

    private int getRemainingNum(int type) {
        if (type == 1) {
            return this.mFingerManagerEx.getRemainingNum();
        }
        return this.mFaceManager.getRemainingNum();
    }
}
