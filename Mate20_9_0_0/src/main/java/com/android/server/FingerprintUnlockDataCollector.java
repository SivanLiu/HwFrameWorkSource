package com.android.server;

import android.common.HwFrameworkFactory;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.IPowerManager.Stub;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.LogException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;

public class FingerprintUnlockDataCollector {
    static final int AUTHENTICATE_FAIL = 2;
    static final int AUTHENTICATE_NONE = 0;
    static final int AUTHENTICATE_SUCCESS = 1;
    private static boolean DEBUG = false;
    private static boolean DEBUG_FPLOG = false;
    static String ENROLL_LOG_FULL_PATH = "/data/log/fingerprint/fpc_enroll.json";
    private static final long SLEEP_TIME = 50;
    static String STATS_UNLOCK_FILE = "/data/log/fingerprint/fp_unlock";
    private static final String SYNC_NODE = "/sys/devices/platform/fingerprint/read_image_flag";
    private static final long SYNC_TIMEOUT = 2000;
    public static final String TAG = "FpDataCollector";
    static String UNLOCK_LOG_FULL_PATH = "/data/log/fingerprint/fpc_unlock.json";
    static int UPLOAD_HOUR = 24;
    static String UPLOAD_TAG = "fingerprint";
    static long UPLOAD_TIME_MILL_SEC = ((long) (((UPLOAD_HOUR * 60) * 60) * 1000));
    private static FingerprintUnlockDataCollector instance = null;
    private static boolean isUseImonitorUpload = false;
    static final Object mLock = new Object[0];
    private static LogException mLogException = HwFrameworkFactory.getLogException();
    private final int CODE_SEND_UNLOCK_LIGHTBRIGHT = 1121;
    private final String DESCRIPTOR_FINGERPRINT_SERVICE = "android.hardware.fingerprint.IFingerprintService";
    private final int SCREENOFF_UNLOCK = 1;
    private final int SCREENON_UNLOCK = 2;
    private int isAuthenticated = 0;
    private boolean isScreenStateOn;
    private String mAuthenticatedTime;
    private String mAuthenticatedTimeToWrite;
    private String mCaptureCompletedTime;
    private String mCaptureCompletedTimeToWrite;
    private String mFingerDownTime;
    private String mFingerDownTimeToWrite;
    private long mLastUploadTime = 0;
    private boolean mScreenOnAuthenticated;
    private boolean mScreenOnCaptureCompleted;
    private boolean mScreenOnFingerDown;
    private String mScreenOnTime;
    private String mScreenOnTimeToWrite;

    static {
        boolean z = true;
        boolean z2 = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z2;
        if (!DEBUG) {
            z = false;
        }
        DEBUG_FPLOG = z;
    }

    public static FingerprintUnlockDataCollector getInstance() {
        FingerprintUnlockDataCollector fingerprintUnlockDataCollector;
        if (DEBUG_FPLOG) {
            Log.d(TAG, "FingerprintUnlockDataCollector.getInstance()");
        }
        synchronized (mLock) {
            if (instance == null) {
                if (DEBUG_FPLOG) {
                    Log.d(TAG, "new intance in getInstance");
                }
                instance = new FingerprintUnlockDataCollector();
            }
            fingerprintUnlockDataCollector = instance;
        }
        return fingerprintUnlockDataCollector;
    }

    public void reportFingerDown() {
        if (DEBUG_FPLOG) {
            Log.d(TAG, "receive finger press down");
        }
        SimpleDateFormat sdfMicrosecond = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        long timeStamp = System.currentTimeMillis();
        boolean ScreenOnTmp = isScreenOn();
        synchronized (this) {
            this.mFingerDownTime = sdfMicrosecond.format(Long.valueOf(timeStamp));
            this.mScreenOnFingerDown = ScreenOnTmp;
            this.mScreenOnAuthenticated = false;
            this.mScreenOnCaptureCompleted = false;
            this.mScreenOnTime = null;
            this.mAuthenticatedTime = null;
            this.mCaptureCompletedTime = null;
        }
    }

    public void reportCaptureCompleted() {
        if (DEBUG_FPLOG) {
            Log.d(TAG, "fingerprint capture completed");
        }
        SimpleDateFormat sdfMicrosecond = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        long timeStamp = System.currentTimeMillis();
        boolean ScreenOnTmp = isScreenOn();
        synchronized (this) {
            this.mCaptureCompletedTime = sdfMicrosecond.format(Long.valueOf(timeStamp));
            this.mScreenOnCaptureCompleted = ScreenOnTmp;
            this.mScreenOnAuthenticated = false;
            this.mScreenOnTime = null;
            this.mAuthenticatedTime = null;
        }
    }

    public void reportFingerprintAuthenticated(boolean authenticated) {
        if (DEBUG_FPLOG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fingerprint authenticated result:");
            stringBuilder.append(authenticated);
            Log.d(str, stringBuilder.toString());
        }
        SimpleDateFormat sdfMicrosecond = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        long timeStamp = System.currentTimeMillis();
        boolean ScreenOnTmp = isScreenOn();
        synchronized (this) {
            this.isAuthenticated = authenticated ? 1 : 2;
            this.mAuthenticatedTime = sdfMicrosecond.format(Long.valueOf(timeStamp));
            this.mScreenOnAuthenticated = ScreenOnTmp;
            this.mScreenOnTime = null;
        }
        reportScreenTurnedOn();
    }

    public void reportScreenStateOn(String stateStr) {
        if (DEBUG_FPLOG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DisplayPowerState :");
            stringBuilder.append(stateStr);
            Log.d(str, stringBuilder.toString());
        }
        if ("ON".equals(stateStr)) {
            this.isScreenStateOn = true;
        } else {
            this.isScreenStateOn = false;
        }
    }

    private void sendUnlockAndLightbright(int unlockType) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("fingerprint");
        if (b != null) {
            try {
                _data.writeInterfaceToken("android.hardware.fingerprint.IFingerprintService");
                _data.writeInt(unlockType);
                b.transact(1121, _data, _reply, 0);
                _reply.readException();
                int result = _reply.readInt();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } catch (Throwable th) {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
    }

    public void reportScreenTurnedOn() {
        boolean isScreenStateOnCurr = isScreenOn();
        synchronized (this) {
            boolean mScreenOnInAuthenticating = this.mScreenOnFingerDown || this.mScreenOnCaptureCompleted || this.mScreenOnAuthenticated;
            if (this.isAuthenticated == 0) {
                Log.d(TAG, "case xxx, not a fingerprint unlock ");
                return;
            }
            if (mScreenOnInAuthenticating) {
                if (isScreenStateOnCurr) {
                    if (this.isAuthenticated == 2) {
                        Log.d(TAG, "case 110, unlock fail during screen on");
                        this.mScreenOnTime = "null";
                    } else {
                        Log.d(TAG, "case 111, unlock succ during screen on");
                        this.mScreenOnTime = "null";
                        new Thread(new Runnable() {
                            public void run() {
                                FingerprintUnlockDataCollector.this.sendUnlockAndLightbright(2);
                            }
                        }).start();
                    }
                } else if (this.isAuthenticated == 2) {
                    Log.d(TAG, "case 100, unlock fail and screen off by hand");
                    this.mScreenOnTime = "null";
                } else {
                    Log.d(TAG, "case 101, unlock succ but screen off by hand");
                    this.mScreenOnTime = "null";
                }
            } else if (isScreenStateOnCurr) {
                if (this.isAuthenticated == 2) {
                    Log.d(TAG, "case 010, screen on after unlock fail");
                    this.mScreenOnTime = "null";
                } else {
                    Log.d(TAG, "case 011, screen on after unlock succ");
                    this.mScreenOnTime = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(Long.valueOf(System.currentTimeMillis()));
                    new Thread(new Runnable() {
                        public void run() {
                            FingerprintUnlockDataCollector.this.sendUnlockAndLightbright(1);
                        }
                    }).start();
                }
            } else if (this.isAuthenticated == 2) {
                Log.d(TAG, "case 000, black unlock fail");
                this.mScreenOnTime = "null";
            } else {
                Log.d(TAG, "case 001, wait for unlock screen on report");
                return;
            }
            this.isAuthenticated = 0;
            this.mFingerDownTimeToWrite = this.mFingerDownTime;
            this.mCaptureCompletedTimeToWrite = this.mCaptureCompletedTime;
            this.mAuthenticatedTimeToWrite = this.mAuthenticatedTime;
            this.mScreenOnTimeToWrite = this.mScreenOnTime;
        }
    }

    private void resetTime() {
        synchronized (this) {
            this.mFingerDownTimeToWrite = null;
            this.mCaptureCompletedTimeToWrite = null;
            this.mAuthenticatedTimeToWrite = null;
            this.mScreenOnTimeToWrite = null;
        }
    }

    private void sendLog() {
        if (isTimeOverThreshold()) {
            String formatTime = new SimpleDateFormat("yyyyMMddHHmmss").format(Long.valueOf(System.currentTimeMillis()));
            String zipFilename = new StringBuilder();
            zipFilename.append(formatTime);
            zipFilename.append("_fingerprint");
            zipFilename = zipFilename.toString();
            String archiveMsg = new StringBuilder();
            archiveMsg.append("archive -i ");
            archiveMsg.append(UNLOCK_LOG_FULL_PATH);
            archiveMsg.append(" -i ");
            archiveMsg.append(ENROLL_LOG_FULL_PATH);
            archiveMsg.append(" -d ");
            archiveMsg.append(UNLOCK_LOG_FULL_PATH);
            archiveMsg.append(" -d ");
            archiveMsg.append(ENROLL_LOG_FULL_PATH);
            archiveMsg.append(" -o ");
            archiveMsg.append(zipFilename);
            archiveMsg.append(" -z zip");
            archiveMsg = archiveMsg.toString();
            if (DEBUG_FPLOG) {
                Log.d(TAG, archiveMsg);
            }
            if (mLogException != null) {
                mLogException.cmd(UPLOAD_TAG, archiveMsg);
            }
            this.mLastUploadTime = System.currentTimeMillis();
            return;
        }
        if (DEBUG_FPLOG) {
            Log.d(TAG, "time is not access threshold, don't send the log");
        }
    }

    private boolean isTimeOverThreshold() {
        if (System.currentTimeMillis() - this.mLastUploadTime >= UPLOAD_TIME_MILL_SEC) {
            return true;
        }
        return false;
    }

    private boolean isScreenOn() {
        try {
            IPowerManager power = Stub.asInterface(ServiceManager.getService("power"));
            if (power != null) {
                return power.isInteractive();
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "can not connect to powermanagerservice");
            return true;
        }
    }

    private boolean checkUseImonitorUpload() {
        if (new File(STATS_UNLOCK_FILE).exists()) {
            return true;
        }
        Log.e(TAG, "STATS_UNLOCK_FILE doesn't exist!");
        return false;
    }

    private String readSyncNode() {
        BufferedReader br = null;
        FileInputStream fis = null;
        InputStreamReader isr = null;
        String res = null;
        try {
            File file = new File(SYNC_NODE);
            if (file.exists()) {
                fis = new FileInputStream(file);
                isr = new InputStreamReader(fis, "UTF-8");
                br = new BufferedReader(isr);
                res = br.readLine();
                try {
                    br.close();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
                try {
                    isr.close();
                } catch (IOException e2) {
                    Log.e(TAG, e2.toString());
                }
                try {
                    fis.close();
                } catch (IOException e22) {
                    Log.e(TAG, e22.toString());
                }
                return res;
            }
            Log.e(TAG, "sync operation node doesn't exist! just return null");
            String str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e3) {
                    Log.e(TAG, e3.toString());
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e32) {
                    Log.e(TAG, e32.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e322) {
                    Log.e(TAG, e322.toString());
                }
            }
            return str;
        } catch (IOException e222) {
            Log.e(TAG, e222.toString());
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e2222) {
                    Log.e(TAG, e2222.toString());
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e22222) {
                    Log.e(TAG, e22222.toString());
                }
            }
            if (fis != null) {
                fis.close();
            }
        } catch (Throwable th) {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e4) {
                    Log.e(TAG, e4.toString());
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e42) {
                    Log.e(TAG, e42.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e422) {
                    Log.e(TAG, e422.toString());
                }
            }
        }
    }

    private void writeSyncNode() {
        BufferedWriter bw = null;
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        try {
            File file = new File(SYNC_NODE);
            if (file.exists()) {
                fos = new FileOutputStream(file);
                osw = new OutputStreamWriter(fos, "UTF-8");
                bw = new BufferedWriter(osw);
                bw.write("0");
                bw.flush();
                try {
                    bw.close();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
                try {
                    osw.close();
                } catch (IOException e2) {
                    Log.e(TAG, e2.toString());
                }
                try {
                    fos.close();
                } catch (IOException e22) {
                    Log.e(TAG, e22.toString());
                }
                return;
            }
            Log.e(TAG, "sync operation node doesn't exist! just return");
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e3) {
                    Log.e(TAG, e3.toString());
                }
            }
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e32) {
                    Log.e(TAG, e32.toString());
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e322) {
                    Log.e(TAG, e322.toString());
                }
            }
        } catch (IOException e222) {
            Log.e(TAG, e222.toString());
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e2222) {
                    Log.e(TAG, e2222.toString());
                }
            }
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e22222) {
                    Log.e(TAG, e22222.toString());
                }
            }
            if (fos != null) {
                fos.close();
            }
        } catch (Throwable th) {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e3222) {
                    Log.e(TAG, e3222.toString());
                }
            }
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e32222) {
                    Log.e(TAG, e32222.toString());
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e322222) {
                    Log.e(TAG, e322222.toString());
                }
            }
        }
    }
}
