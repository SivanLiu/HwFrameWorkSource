package huawei.android.hardware.fingerprint;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.fingerprint.IFingerprintService.Stub;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FingerprintManagerEx {
    private static final int CODE_DISABLE_FINGERPRINT_VIEW = 1114;
    private static final int CODE_ENABLE_FINGERPRINT_VIEW = 1115;
    private static final int CODE_GET_FINGERPRINT_LIST_ENROLLED = 1118;
    private static final int CODE_GET_HARDWARE_POSITION = 1110;
    private static final int CODE_GET_HARDWARE_TYPE = 1109;
    private static final int CODE_GET_HOVER_SUPPORT = 1113;
    private static final int CODE_GET_TOKEN_LEN = 1103;
    private static final int CODE_IS_FINGERPRINT_HARDWARE_DETECTED = 1119;
    private static final int CODE_IS_FP_NEED_CALIBRATE = 1101;
    private static final int CODE_IS_SUPPORT_DUAL_FINGERPRINT = 1120;
    private static final int CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION = 1116;
    private static final int CODE_NOTIFY_OPTICAL_CAPTURE = 1111;
    private static final int CODE_REMOVE_FINGERPRINT = 1107;
    private static final int CODE_REMOVE_MASK_AND_SHOW_CANCEL = 1117;
    private static final int CODE_SET_CALIBRATE_MODE = 1102;
    private static final int CODE_SET_FINGERPRINT_MASK_VIEW = 1104;
    private static final int CODE_SET_HOVER_SWITCH = 1112;
    private static final int CODE_SHOW_FINGERPRINT_BUTTON = 1106;
    private static final int CODE_SHOW_FINGERPRINT_VIEW = 1105;
    private static final int CODE_SUSPEND_AUTHENTICATE = 1108;
    private static final String DESCRIPTOR_FINGERPRINT_SERVICE = "android.hardware.fingerprint.IFingerprintService";
    private static final int FINGERPRINT_BACK_ULTRASONIC = 0;
    private static final int FINGERPRINT_FRONT_ULTRASONIC = 1;
    private static final int FINGERPRINT_HARDWARE_OPTICAL = 1;
    private static final int FINGERPRINT_HARDWARE_OUTSCREEN = 0;
    private static final int FINGERPRINT_HARDWARE_ULTRASONIC = 2;
    private static final int FINGERPRINT_NOT_ULTRASONIC = -1;
    private static final int FINGERPRINT_SLIDE_ULTRASONIC = 3;
    private static final int FINGERPRINT_UNDER_DISPLAY_ULTRASONIC = 2;
    private static final int FLAG_FINGERPRINT_LOCATION_BACK = 1;
    private static final int FLAG_FINGERPRINT_LOCATION_FRONT = 2;
    private static final int FLAG_FINGERPRINT_LOCATION_SLIDE = 8;
    private static final int FLAG_FINGERPRINT_LOCATION_UNDER_DISPLAY = 4;
    private static final int FLAG_FINGERPRINT_POSITION_MASK = 65535;
    private static final int FLAG_FINGERPRINT_TYPE_CAPACITANCE = 1;
    private static final int FLAG_FINGERPRINT_TYPE_MASK = 15;
    private static final int FLAG_FINGERPRINT_TYPE_OPTICAL = 2;
    private static final int FLAG_FINGERPRINT_TYPE_ULTRASONIC = 3;
    private static final boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    private static final int FRONT_FINGERPRINT_NAVIGATION_TRIKEY = SystemProperties.getInt("ro.config.hw_front_fp_trikey", 0);
    private static final int HOVER_HARDWARE_NOT_SUPPORT = 0;
    private static final int HOVER_HARDWARE_SUPPORT = 1;
    private static final int INVALID_VALUE = -1;
    private static final String TAG = "FingerprintManagerEx";
    private static int mDetailsType = -1;
    private static HashMap<Integer, Integer> mHardwareInfo = new HashMap();
    private static int[] mPosition = new int[]{-1, -1, -1, -1};
    private static int mType = -1;
    private Context mContext;
    private IFingerprintService mService = Stub.asInterface(ServiceManager.getService("fingerprint"));

    public FingerprintManagerEx(Context context) {
        this.mContext = context;
    }

    public int getRemainingNum() {
        if (this.mService != null) {
            try {
                return this.mService.getRemainingNum();
            } catch (RemoteException e) {
                Log.w(TAG, "Remote exception in getRemainingNum: ", e);
            }
        }
        return -1;
    }

    public long getRemainingTime() {
        if (this.mService != null) {
            try {
                return this.mService.getRemainingTime();
            } catch (RemoteException e) {
                Log.w(TAG, "Remote exception in getRemainingTime: ", e);
            }
        }
        return 0;
    }

    public static boolean isFpNeedCalibrate() {
        boolean z;
        boolean z2 = true;
        if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0) {
            z = FRONT_FINGERPRINT_NAVIGATION;
        } else {
            z = false;
        }
        if (!z) {
            return false;
        }
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        int result = -1;
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(1101, _data, _reply, 0);
                _reply.readException();
                result = _reply.readInt();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
        Log.d(TAG, "isFpNeedCalibrate result: " + result);
        if (result != 1) {
            z2 = false;
        }
        return z2;
    }

    public static void setCalibrateMode(int mode) {
        Log.d(TAG, "setCalibrateMode: " + mode);
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                _data.writeInt(mode);
                b.transact(CODE_SET_CALIBRATE_MODE, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public static int getTokenLen() {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        int len = -1;
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(CODE_GET_TOKEN_LEN, _data, _reply, 0);
                _reply.readException();
                len = _reply.readInt();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
        Log.d(TAG, "getTokenLen len: " + len);
        return len;
    }

    public static void setFingerprintMaskView(Bundle bundle) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                _data.writeBundle(bundle);
                b.transact(CODE_SET_FINGERPRINT_MASK_VIEW, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public static void showFingerprintView() {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(CODE_SHOW_FINGERPRINT_VIEW, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public static void showSuspensionButton(int centerX, int centerY) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                _data.writeInt(centerX);
                _data.writeInt(centerY);
                b.transact(CODE_SHOW_FINGERPRINT_BUTTON, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public static void removeFingerView() {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(CODE_REMOVE_FINGERPRINT, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public static void suspendAuthentication(int status) {
        if (hasFingerprintInScreen()) {
            Log.d(TAG, "suspendAuthentication: " + status);
        } else {
            Log.w(TAG, "do not have UD device suspend invalid");
        }
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                _data.writeInt(status);
                b.transact(CODE_SUSPEND_AUTHENTICATE, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public static int getHardwareType() {
        int type = mType;
        if (type != -1) {
            return type;
        }
        mHardwareInfo = getHardwareInfo();
        if (mHardwareInfo.isEmpty()) {
            type = SystemProperties.getInt("persist.sys.fingerprint.hardwareType", -1);
            Log.d(TAG, "use SystemProperties type :" + type);
            return type;
        }
        if (mHardwareInfo.containsKey(Integer.valueOf(4))) {
            int physical = Integer.parseInt(((Integer) mHardwareInfo.get(Integer.valueOf(4))).toString());
            if (physical == 2) {
                type = 1;
            } else if (physical == 3) {
                type = 2;
            }
            Log.d(TAG, "LOCATION_UNDER_DISPLAY :" + physical);
        } else {
            type = 0;
        }
        mType = type;
        Log.d(TAG, "type:" + type);
        return type;
    }

    public static int getUltrasonicFingerprintType() {
        int type = -1;
        if (mHardwareInfo.isEmpty()) {
            mHardwareInfo = getHardwareInfo();
        }
        if (mHardwareInfo.containsKey(Integer.valueOf(2))) {
            if (((Integer) mHardwareInfo.get(Integer.valueOf(2))).intValue() == 3) {
                type = 1;
            }
        } else if (mHardwareInfo.containsKey(Integer.valueOf(1))) {
            if (((Integer) mHardwareInfo.get(Integer.valueOf(1))).intValue() == 3) {
                type = 0;
            }
        } else if (mHardwareInfo.containsKey(Integer.valueOf(4))) {
            if (((Integer) mHardwareInfo.get(Integer.valueOf(4))).intValue() == 3) {
                type = 2;
            }
        } else if (mHardwareInfo.containsKey(Integer.valueOf(8)) && ((Integer) mHardwareInfo.get(Integer.valueOf(8))).intValue() == 3) {
            type = 3;
        }
        Log.d(TAG, "getUltrasonicFingerprintType :" + type);
        return type;
    }

    public static boolean isSupportDualFingerprint() {
        Log.d(TAG, "isSupportDualFingerprint called.");
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(CODE_IS_SUPPORT_DUAL_FINGERPRINT, _data, _reply, 0);
                _reply.readException();
                boolean isSupportDualFp = _reply.readBoolean();
                Log.d(TAG, "isSupportDualFingerprint is: " + isSupportDualFp);
                return isSupportDualFp;
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        } else {
            _reply.recycle();
            _data.recycle();
            return false;
        }
    }

    public List<Fingerprint> getEnrolledFingerprints(int targetDevice) {
        return getEnrolledFingerprints(targetDevice, UserHandle.myUserId());
    }

    public List<Fingerprint> getEnrolledFingerprints(int targetDevice, int userId) {
        List<Fingerprint> fingerprints = new ArrayList();
        String opPackageName = this.mContext.getOpPackageName();
        if (opPackageName == null || "".equals(opPackageName)) {
            Log.d(TAG, "calling opPackageName is invalid");
            return fingerprints;
        }
        Log.d(TAG, "getEnrolledFingerprints calling package: " + opPackageName + " targetDevice: " + targetDevice);
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                _data.writeInt(targetDevice);
                _data.writeString(opPackageName);
                _data.writeInt(userId);
                b.transact(CODE_GET_FINGERPRINT_LIST_ENROLLED, _data, _reply, 0);
                _reply.readException();
                _reply.readTypedList(fingerprints, Fingerprint.CREATOR);
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
        return fingerprints;
    }

    public boolean hasEnrolledFingerprints(int targetDevice) {
        boolean z = false;
        String opPackageName = this.mContext.getOpPackageName();
        if (opPackageName == null || "".equals(opPackageName)) {
            Log.d(TAG, "calling opPackageName is invalid");
            return false;
        }
        Log.d(TAG, "hasEnrolledFingerprints calling package: " + opPackageName + " targetDevice: " + targetDevice);
        if (getEnrolledFingerprints(targetDevice).size() > 0) {
            z = true;
        }
        return z;
    }

    public boolean isHardwareDetected(int targetDevice) {
        String opPackageName = this.mContext.getOpPackageName();
        if (opPackageName == null || "".equals(opPackageName)) {
            Log.d(TAG, "calling opPackageName is invalid");
            return false;
        }
        Log.d(TAG, "isHardwareDetected calling package: " + opPackageName + " targetDevice: " + targetDevice);
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                _data.writeInt(targetDevice);
                _data.writeString(opPackageName);
                b.transact(CODE_IS_FINGERPRINT_HARDWARE_DETECTED, _data, _reply, 0);
                _reply.readException();
                boolean isHardwareDetected = _reply.readBoolean();
                Log.d(TAG, "isHardwareDetected is: " + isHardwareDetected);
                return isHardwareDetected;
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        } else {
            _reply.recycle();
            _data.recycle();
            return false;
        }
    }

    private static HashMap<Integer, Integer> getHardwareInfo() {
        HashMap<Integer, Integer> hardwareInfo = new HashMap();
        int typeDetails = getHardwareTypeDetailsFromHal();
        Log.d(TAG, "typeDetails:" + typeDetails);
        if (typeDetails != -1) {
            int physicalType;
            int offset = -1;
            if ((typeDetails & 1) != 0) {
                offset = 0;
                physicalType = (typeDetails >> 8) & 15;
                hardwareInfo.put(Integer.valueOf(1), Integer.valueOf(physicalType));
                Log.d(TAG, "LOCATION_BACK physicalType :" + physicalType);
            }
            if ((typeDetails & 2) != 0) {
                offset++;
                physicalType = (typeDetails >> ((offset * 4) + 8)) & 15;
                hardwareInfo.put(Integer.valueOf(2), Integer.valueOf(physicalType));
                Log.d(TAG, "LOCATION_FRONT physicalType :" + physicalType);
            }
            if ((typeDetails & 4) != 0) {
                offset++;
                physicalType = (typeDetails >> ((offset * 4) + 8)) & 15;
                hardwareInfo.put(Integer.valueOf(4), Integer.valueOf(physicalType));
                Log.d(TAG, "LOCATION_UNDER_DISPLAY physicalType :" + physicalType);
            }
            if ((typeDetails & 8) != 0) {
                physicalType = (typeDetails >> (((offset + 1) * 4) + 8)) & 15;
                hardwareInfo.put(Integer.valueOf(8), Integer.valueOf(physicalType));
                Log.d(TAG, "LOCATION_SLIDE physicalType :" + physicalType);
            }
        }
        return hardwareInfo;
    }

    private static int getHardwareTypeDetailsFromHal() {
        Log.d(TAG, "getHardwareType  mDetailsType:" + mDetailsType);
        int type = mDetailsType;
        if (type != -1) {
            return type;
        }
        type = -1;
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(CODE_GET_HARDWARE_TYPE, _data, _reply, 0);
                _reply.readException();
                type = _reply.readInt();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
        Log.d(TAG, "getHardwareType from Hal: " + type);
        mDetailsType = type;
        return type;
    }

    public static boolean hasFingerprintInScreen() {
        int hardHardwareType = getHardwareType();
        if (hardHardwareType == 0 || hardHardwareType == -1) {
            return false;
        }
        return true;
    }

    private static int[] getHardwarePositionFromHal() {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        int[] position = new int[]{-1, -1, -1, -1};
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(CODE_GET_HARDWARE_POSITION, _data, _reply, 0);
                _reply.readException();
                position[0] = _reply.readInt();
                position[1] = _reply.readInt();
                position[2] = _reply.readInt();
                position[3] = _reply.readInt();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
        return position;
    }

    public static Rect getFingerprintRect() {
        int[] position = getHardwarePosition();
        return new Rect(position[0], position[1], position[2], position[3]);
    }

    public static int[] getHardwarePosition() {
        Log.d(TAG, "getHardwarePosition mPosition[0] " + mPosition[0]);
        int[] position = mPosition;
        if (position[0] != -1) {
            return position;
        }
        int[] pxPosition = getHardwarePositionFromHal();
        for (int i = 0; i < 4; i++) {
            Log.d(TAG, "from hal after covert: " + pxPosition[i]);
        }
        mPosition = pxPosition;
        return pxPosition;
    }

    public static void notifyCaptureOpticalImage() {
        if (getHardwareType() != 1) {
            Log.d(TAG, "not Optical sensor notifyCapture failed");
            return;
        }
        Log.d(TAG, "notifyCaptureOpticalImage");
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(CODE_NOTIFY_OPTICAL_CAPTURE, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public static void setHoverEventSwitch(int enabled) {
        Log.d(TAG, "setHoverEventSwitch: " + enabled);
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                _data.writeInt(enabled);
                b.transact(CODE_SET_HOVER_SWITCH, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public static boolean isHoverEventSupport() {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        int type = -1;
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(CODE_GET_HOVER_SUPPORT, _data, _reply, 0);
                _reply.readException();
                type = _reply.readInt();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
        Log.d(TAG, "isHoverEventSupport from Hal: " + type);
        if (type == -1) {
            type = SystemProperties.getInt("persist.sys.fingerprint.hoverSupport", 0);
            Log.d(TAG, "isHoverEventSupport use SystemProperties type :" + type);
        }
        if (type == 1) {
            return true;
        }
        return false;
    }

    public void disableFingerprintView(boolean hasAnimation) {
        Log.d(TAG, "disableFingerprintView: " + hasAnimation);
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                _data.writeBoolean(hasAnimation);
                b.transact(CODE_DISABLE_FINGERPRINT_VIEW, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public void enableFingerprintView(boolean hasAnimation, int initStatus) {
        Log.d(TAG, "enableFingerprintView: hasAnimation =" + hasAnimation + ",initStatus = " + initStatus);
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                _data.writeBoolean(hasAnimation);
                _data.writeInt(initStatus);
                b.transact(CODE_ENABLE_FINGERPRINT_VIEW, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public static void keepMaskShowAfterAuthentication() {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(CODE_KEEP_MASK_SHOW_AFTER_AUTHENTICATION, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public static void removeMaskAndShowButton() {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_FINGERPRINT_SERVICE);
                b.transact(CODE_REMOVE_MASK_AND_SHOW_CANCEL, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                return;
            } finally {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }
}
