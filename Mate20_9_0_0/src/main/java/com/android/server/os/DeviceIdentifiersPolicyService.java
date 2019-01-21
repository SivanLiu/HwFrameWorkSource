package com.android.server.os;

import android.content.Context;
import android.os.Binder;
import android.os.IDeviceIdentifiersPolicyService.Stub;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ByteStringUtils;
import android.util.Log;
import com.android.server.SystemService;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class DeviceIdentifiersPolicyService extends SystemService {
    public static final String PERMISSION_ACCESS_UDID = "com.huawei.permission.sec.ACCESS_UDID";
    private static final String TAG = "DeviceIdentifiers";
    private static final String UDID_EXCEPTION = "AndroidRuntimeException";
    private static final Object sLock = new Object();
    private static UDIDModelWhiteConfig udidModelWhiteConfig = null;

    private static final class DeviceIdentifiersPolicy extends Stub {
        static boolean isInWhiteList = false;
        static String udid = null;
        private final Context mContext;

        public DeviceIdentifiersPolicy(Context context) {
            this.mContext = context;
        }

        public String getSerial() throws RemoteException {
            if (UserHandle.getAppId(Binder.getCallingUid()) == 1000 || this.mContext.checkCallingOrSelfPermission("android.permission.READ_PHONE_STATE") == 0 || this.mContext.checkCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE") == 0) {
                return SystemProperties.get("ro.serialno", Shell.NIGHT_MODE_STR_UNKNOWN);
            }
            throw new SecurityException("getSerial requires READ_PHONE_STATE or READ_PRIVILEGED_PHONE_STATE permission");
        }

        public String getUDID() throws RemoteException {
            this.mContext.enforceCallingOrSelfPermission(DeviceIdentifiersPolicyService.PERMISSION_ACCESS_UDID, "does not have access udid permission!");
            synchronized (DeviceIdentifiersPolicyService.sLock) {
                String readResult = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                String str;
                if (TextUtils.isEmpty(udid)) {
                    udid = GetUDIDNative.getUDID();
                    if (TextUtils.isEmpty(udid)) {
                        Log.e(DeviceIdentifiersPolicyService.TAG, "GetUDIDNative getUDID return null, generated it");
                        if (DeviceIdentifiersPolicyService.udidModelWhiteConfig == null) {
                            DeviceIdentifiersPolicyService.udidModelWhiteConfig = UDIDModelWhiteConfig.getInstance();
                            isInWhiteList = DeviceIdentifiersPolicyService.udidModelWhiteConfig.isWhiteModelForUDID(SystemProperties.get("ro.product.model", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
                        }
                        if (isInWhiteList) {
                            Log.w(DeviceIdentifiersPolicyService.TAG, "the phone is in udid_model_whitelist, return null!");
                            return null;
                        }
                        String serNum;
                        MessageDigest messageDigest = null;
                        try {
                            messageDigest = MessageDigest.getInstance("SHA256");
                            serNum = SystemProperties.get("ro.serialno", Shell.NIGHT_MODE_STR_UNKNOWN);
                            if (serNum != null) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(readResult);
                                stringBuilder.append(serNum.toUpperCase(Locale.US));
                                readResult = stringBuilder.toString();
                            }
                            String emmcId = GetUDIDNative.getEmmcId();
                            if (emmcId != null) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(readResult);
                                stringBuilder2.append(emmcId.toUpperCase(Locale.US));
                                readResult = stringBuilder2.toString();
                            }
                            String btMacAddress = GetUDIDNative.getBtMacAddress();
                            if (btMacAddress != null) {
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(readResult);
                                stringBuilder3.append(btMacAddress.toUpperCase(Locale.US));
                                readResult = stringBuilder3.toString();
                            }
                            String wifiMacAddress = GetUDIDNative.getWifiMacAddress();
                            if (wifiMacAddress != null) {
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append(readResult);
                                stringBuilder4.append(wifiMacAddress.toUpperCase(Locale.US));
                                readResult = stringBuilder4.toString();
                            }
                            if (serNum == null && emmcId == null && btMacAddress == null && wifiMacAddress == null) {
                                udid = DeviceIdentifiersPolicyService.UDID_EXCEPTION;
                            } else {
                                messageDigest.update(readResult.getBytes(Charset.forName("UTF-8")));
                                udid = ByteStringUtils.toHexString(messageDigest.digest());
                            }
                        } catch (NoSuchAlgorithmException e) {
                            Log.w(DeviceIdentifiersPolicyService.TAG, "MessageDigest throw Exception!");
                        }
                        serNum = udid;
                        return serNum;
                    }
                    str = udid;
                    return str;
                }
                Log.i(DeviceIdentifiersPolicyService.TAG, "udid has been read success, return!");
                str = udid;
                return str;
            }
        }
    }

    public DeviceIdentifiersPolicyService(Context context) {
        super(context);
    }

    public void onStart() {
        publishBinderService("device_identifiers", new DeviceIdentifiersPolicy(getContext()));
    }
}
