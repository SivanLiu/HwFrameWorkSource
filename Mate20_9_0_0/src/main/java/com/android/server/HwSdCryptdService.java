package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IVold;
import android.os.IVold.Stub;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.storage.IStorageManager;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.HexDump;

public final class HwSdCryptdService {
    private static final String BROADCAST_HWSDCRYPTD_STATE = "com.huawei.android.HWSDCRYPTD_STATE";
    private static final String BROADCAST_PERSSION = "com.huawei.hwSdCryptd.permission.RECV_HWSDCRYPTD_RESULT";
    private static final String CRYPTD_TAG = "SdCryptdConnector";
    private static final boolean DEBUG = true;
    private static final String EXTRA_ENABLE = "enable";
    private static final String EXTRA_EVENT_CODE = "code";
    private static final String EXTRA_EVENT_MSG = "message";
    private static final int MAX_CONTAINERS = 250;
    private static final int MSG_DO_BROADCAST = 1;
    private static final int MSG_DO_MOUNT = 0;
    private static final int RESPONSE_CODE_ERROR = -1;
    private static final int RESPONSE_CODE_OK = 0;
    private static final int RESPONSE_SDCRYPTD_FAILED = 906;
    private static final int RESPONSE_SDCRYPTD_MOUNT = 905;
    private static final int RESPONSE_SDCRYPTD_SUCCESS = 907;
    private static final String TAG = "HwSdCryptdService";
    private static volatile HwSdCryptdService mInstance = null;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mThread = new HandlerThread("HwSdCryptdHandler");
    private volatile IVold mVold;

    private final class HwSdCryptdHandler extends Handler {
        public HwSdCryptdHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Log.i(HwSdCryptdService.TAG, "handleMessage MSG_DO_MOUNT");
                    String volId = msg.obj;
                    IStorageManager mountService = HwSdCryptdService.getMountService();
                    if (mountService == null) {
                        Log.e(HwSdCryptdService.TAG, "unMount cannot get IMountService service.");
                        return;
                    }
                    try {
                        mountService.mount(volId);
                        return;
                    } catch (Exception e) {
                        String str = HwSdCryptdService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("HwSdCryptdHandler mountService has exception : ");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                        return;
                    }
                case 1:
                    Log.i(HwSdCryptdService.TAG, "handleMessage MSG_DO_BROADCAST");
                    HwSdCryptdService.this.sendBroadcast(msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    public static synchronized HwSdCryptdService getInstance(Context context) {
        HwSdCryptdService hwSdCryptdService;
        synchronized (HwSdCryptdService.class) {
            if (mInstance == null) {
                mInstance = new HwSdCryptdService(context);
            }
            hwSdCryptdService = mInstance;
        }
        return hwSdCryptdService;
    }

    public HwSdCryptdService(Context context) {
        this.mContext = context;
        this.mThread.start();
        this.mHandler = new HwSdCryptdHandler(this.mThread.getLooper());
        connect();
    }

    private void connect() {
        IBinder binder = ServiceManager.getService("vold");
        if (binder != null) {
            try {
                binder.linkToDeath(new DeathRecipient() {
                    public void binderDied() {
                        Slog.w(HwSdCryptdService.TAG, "vold died; reconnecting");
                        HwSdCryptdService.this.mVold = null;
                        HwSdCryptdService.this.connect();
                    }
                }, 0);
            } catch (RemoteException e) {
                binder = null;
            }
        }
        if (binder != null) {
            this.mVold = Stub.asInterface(binder);
        } else {
            Slog.w(TAG, "vold not found; trying again");
        }
        if (this.mVold == null) {
            BackgroundThread.getHandler().postDelayed(new -$$Lambda$HwSdCryptdService$dI4z0u8m27l3cJ8TMrOeHHSTzZg(this), 1000);
        }
    }

    public int setSdCardCryptdEnable(boolean enable, String volId) {
        if (!checkPermission("setSdCardCryptdEnable")) {
            return -1;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCryptdEnable: ");
        stringBuilder.append(enable);
        stringBuilder.append(",volId: ");
        stringBuilder.append(volId);
        Log.i(str, stringBuilder.toString());
        int code = -1;
        try {
            if (this.mVold != null) {
                if (enable) {
                    this.mVold.cryptsdEnable(volId);
                } else {
                    this.mVold.cryptsdDisable(volId);
                }
                code = 0;
            }
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setSdCardCryptdEnable has exception : ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
        }
        return code;
    }

    public int addSdCardUserKeyAuth(int userId, int serialNumber, byte[] token, byte[] secret) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addSdCardUserKeyAuth,userId: ");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (!checkPermission("addSdCardUserKeyAuth")) {
            return -1;
        }
        int code = -1;
        try {
            if (this.mVold != null) {
                this.mVold.cryptsdAddKeyAuth(userId, serialNumber, encodeBytes(token), encodeBytes(secret));
                code = 0;
            }
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("addSdCardUserKeyAuth has exception : ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
        }
        return code;
    }

    public int unlockSdCardKey(int userId, int serialNumber, byte[] token, byte[] secret) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unlockSdCardKey,userId: ");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        if (!checkPermission("unlockSdCardKey")) {
            return -1;
        }
        int code = -1;
        try {
            if (this.mVold != null) {
                this.mVold.cryptsdUnlockKey(userId, serialNumber, encodeBytes(token), encodeBytes(secret));
                code = 0;
            }
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("unlockSdCardKey has exception : ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
        }
        return code;
    }

    public int backupSecretkey() {
        Log.i(TAG, "backupSecretkey");
        if (!checkPermission("backupSecretkey")) {
            return -1;
        }
        int code = -1;
        try {
            if (this.mVold != null) {
                this.mVold.cryptsdBackupInfo();
                code = 0;
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("backupSecretkey has exception : ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
        return code;
    }

    private String encodeBytes(byte[] bytes) {
        if (ArrayUtils.isEmpty(bytes)) {
            return "!";
        }
        return HexDump.toHexString(bytes);
    }

    private void sendBroadcast(Intent intent) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendBroadcast:");
        stringBuilder.append(intent.getAction());
        Log.i(str, stringBuilder.toString());
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BROADCAST_PERSSION);
    }

    private boolean checkPermission(String method) {
        if (UserHandle.getAppId(Binder.getCallingUid()) == 1000) {
            return true;
        }
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(method);
        stringBuilder.append("called no permission, callingPid is ");
        stringBuilder.append(pid);
        stringBuilder.append(",callingUid is ");
        stringBuilder.append(uid);
        Log.i(str, stringBuilder.toString());
        return false;
    }

    public void onCryptsdMessage(String message) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("receive event: ");
        stringBuilder.append(message);
        Log.i(str, stringBuilder.toString());
        if (message != null) {
            String[] cooked = NativeDaemonEvent.unescapeArgs(message);
            if (!ArrayUtils.isEmpty(cooked)) {
                int code = -1;
                try {
                    code = Integer.parseInt(cooked[0]);
                } catch (NumberFormatException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("parseInt has exception : ");
                    stringBuilder2.append(e);
                    Log.e(str2, stringBuilder2.toString());
                }
                if (RESPONSE_SDCRYPTD_MOUNT == code) {
                    if (cooked.length == 2) {
                        this.mHandler.obtainMessage(0, cooked[1]).sendToTarget();
                    }
                } else if ((RESPONSE_SDCRYPTD_SUCCESS == code || RESPONSE_SDCRYPTD_FAILED == code) && cooked.length == 3) {
                    Intent intent = new Intent(BROADCAST_HWSDCRYPTD_STATE);
                    intent.putExtra(EXTRA_EVENT_CODE, code);
                    intent.putExtra(EXTRA_ENABLE, cooked[1]);
                    intent.putExtra(EXTRA_EVENT_MSG, cooked[2]);
                    this.mHandler.obtainMessage(1, intent).sendToTarget();
                }
            }
        }
    }

    public static IStorageManager getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IStorageManager.Stub.asInterface(service);
        }
        Log.e(TAG, "getMountService ERROR");
        return null;
    }
}
