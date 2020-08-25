package com.huawei.server.security.securitydiagnose;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UEventObserver;
import android.text.TextUtils;
import android.util.IMonitor;
import android.util.Log;
import com.android.server.display.HwUibcReceiver;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MalAppDetectReport {
    private static final int BUFFER_LENGTH = 8192;
    private static final int BYTE_TO_HEX_CHAR_NUMBER = 2;
    private static final int BYTE_TO_HEX_MASK = 15;
    private static final int BYTE_TO_HEX_OFFSET = 4;
    private static final int EVT_MALAPP_REPORT = 2000;
    private static final int EVT_MALAPP_REPORT_ID = 940000004;
    private static final int INITIAL_SIZE = 16;
    private static final int INVALID_DATA = -1;
    private static final int INVALID_UID = -1;
    /* access modifiers changed from: private */
    public static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String ROOT_STATE_MATCH = "DEVPATH=/kernel/oases_attack";
    private static final Object S_LOCK = new Object();
    private static final String TAG = "MalAppDetectReport";
    private static MalAppDetectReport sInstance;
    private Context mContext;
    /* access modifiers changed from: private */
    public UeventHandler mHandler;
    private HandlerThread mHandlerThread;
    private final UEventObserver mUEventObserver = new UEventObserver() {
        /* class com.huawei.server.security.securitydiagnose.MalAppDetectReport.AnonymousClass1 */

        public void onUEvent(UEventObserver.UEvent event) {
            if (MalAppDetectReport.IS_HW_DEBUG) {
                Log.d(MalAppDetectReport.TAG, "onUEvent event: " + event);
            }
            MalAppDetectReport.this.mHandler.sendMessage(MalAppDetectReport.this.mHandler.obtainMessage(2000, event));
        }
    };

    private MalAppDetectReport(Context context) {
        this.mContext = context;
        this.mUEventObserver.startObserving(ROOT_STATE_MATCH);
        this.mHandlerThread = new HandlerThread("uevent handler : MalAppDetect");
        this.mHandlerThread.start();
        this.mHandler = new UeventHandler(this.mHandlerThread.getLooper());
    }

    public static void init(Context context) {
        synchronized (S_LOCK) {
            if (sInstance == null) {
                sInstance = new MalAppDetectReport(context);
            }
        }
    }

    /* access modifiers changed from: private */
    public void reportData(UEventObserver.UEvent event) {
        JSONObject json;
        if (event != null) {
            try {
                int uid = Integer.parseInt(event.get("uid"));
                String patchId = event.get(HwSecDiagnoseConstant.MALAPP_PATCH_ID_OLD);
                if (uid != -1 && (json = getApkInfo(uid, patchId)) != null) {
                    onMalAppReport(json);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "some data is not of the type integer during parsing UEvent");
            }
        }
    }

    private void onMalAppReport(JSONObject json) {
        JSONArray jsonArray = null;
        try {
            jsonArray = json.getJSONArray(HwSecDiagnoseConstant.MALAPP_APK_PACKAGES);
        } catch (JSONException e) {
            Log.e(TAG, "getJSONArray JSONException!");
        }
        if (jsonArray == null) {
            Log.w(TAG, "jsonArray is null!");
            return;
        }
        int arrayLength = jsonArray.length();
        for (int index = 0; index < arrayLength; index++) {
            try {
                JSONObject object = jsonArray.getJSONObject(index);
                if (object == null) {
                    Log.w(TAG, "object is null!");
                } else {
                    if (IS_HW_DEBUG) {
                        Log.i(TAG, object.toString());
                    }
                    IMonitor.EventStream eventStream = IMonitor.openEventStream((int) EVT_MALAPP_REPORT_ID);
                    eventStream.setParam(0, object.optString(HwSecDiagnoseConstant.MALAPP_APK_NAME));
                    eventStream.setParam(1, object.optString(HwSecDiagnoseConstant.MALAPP_APK_HASH));
                    eventStream.setParam(2, object.optString(HwSecDiagnoseConstant.MALAPP_APK_CERT));
                    eventStream.setParam(3, object.optString(HwSecDiagnoseConstant.MALAPP_PATCH_ID));
                    if (IMonitor.sendEvent(eventStream) && IS_HW_DEBUG) {
                        Log.i(TAG, "sendEvent data success");
                    }
                    IMonitor.closeEventStream(eventStream);
                }
            } catch (JSONException e2) {
                Log.e(TAG, "getJSONObject JSONException!");
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0060, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0061, code lost:
        if (r5 != null) goto L_0x0063;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0063, code lost:
        $closeResource(r4, r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0066, code lost:
        throw r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0069, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x006a, code lost:
        $closeResource(r4, r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x006d, code lost:
        throw r5;
     */
    private String calculateApkHash(String apkName) {
        String apkDigest = null;
        try {
            String sourceDir = this.mContext.getPackageManager().getApplicationInfo(apkName, 0).sourceDir;
            if (TextUtils.isEmpty(sourceDir)) {
                Log.w(TAG, "calculateApkHash sourceDir is empty!");
                return null;
            }
            try {
                ZipFile file = new ZipFile(sourceDir);
                InputStream manifest = file.getInputStream(file.getEntry("META-INF/MANIFEST.MF"));
                MessageDigest msgDigest = MessageDigest.getInstance(HwSecDiagnoseConstant.MALAPP_APK_SHA256);
                byte[] bytes = new byte[8192];
                boolean isSucceed = false;
                for (int numRead = manifest.read(bytes); numRead != -1; numRead = manifest.read(bytes)) {
                    msgDigest.update(bytes, 0, numRead);
                    isSucceed = true;
                }
                if (isSucceed) {
                    apkDigest = byteArray2Hex(msgDigest.digest());
                }
                $closeResource(null, manifest);
                $closeResource(null, file);
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "calculateApkHash no such algorithm!");
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "calculateApkHash file not found!");
            } catch (IOException e3) {
                Log.e(TAG, "calculateApkHash IOException!");
            }
            return apkDigest;
        } catch (PackageManager.NameNotFoundException e4) {
            Log.e(TAG, "calculateApkHash application not found!");
            return null;
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
            } catch (Throwable th) {
                x0.addSuppressed(th);
            }
        } else {
            x1.close();
        }
    }

    private String calculateCertHash(String apkName) {
        try {
            Signature[] signatures = this.mContext.getPackageManager().getPackageInfo(apkName, 64).signatures;
            MessageDigest msgDigest = MessageDigest.getInstance(HwSecDiagnoseConstant.MALAPP_APK_SHA256);
            if (signatures.length == 1) {
                return byteArray2Hex(msgDigest.digest(signatures[0].toByteArray()));
            }
            List<String> sigList = new ArrayList<>(16);
            for (Signature signature : signatures) {
                msgDigest.reset();
                sigList.add(byteArray2Hex(msgDigest.digest(signature.toByteArray())));
            }
            Collections.sort(sigList);
            StringBuffer mergeBuffer = new StringBuffer();
            int sigListSize = sigList.size();
            for (int k = 0; k < sigListSize; k++) {
                mergeBuffer.append(sigList.get(k));
            }
            String mergeString = mergeBuffer.toString();
            msgDigest.reset();
            return byteArray2Hex(msgDigest.digest(mergeString.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "calculateCertHash unsupported encoding!");
            return null;
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e(TAG, "calculateCertHash package not found!");
            return null;
        } catch (NoSuchAlgorithmException e3) {
            Log.e(TAG, "calculateCertHash no such algorithm!");
            return null;
        }
    }

    private JSONObject getApkInfo(int uid, String patchId) {
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
        JSONArray jsonArray = new JSONArray();
        if (packages == null) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(HwSecDiagnoseConstant.MALAPP_APK_NAME, "uid = " + uid);
                jsonObject.put(HwSecDiagnoseConstant.MALAPP_APK_HASH, "");
                jsonObject.put(HwSecDiagnoseConstant.MALAPP_APK_CERT, "");
                jsonObject.put(HwSecDiagnoseConstant.MALAPP_PATCH_ID, patchId);
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                Log.e(TAG, "JSON put JSONException!");
            }
        } else {
            for (String pkg : packages) {
                String apkDigest = calculateApkHash(pkg);
                String certDigest = calculateCertHash(pkg);
                if (!(apkDigest == null || certDigest == null)) {
                    try {
                        JSONObject jsonObject2 = new JSONObject();
                        jsonObject2.put(HwSecDiagnoseConstant.MALAPP_APK_NAME, pkg);
                        jsonObject2.put(HwSecDiagnoseConstant.MALAPP_APK_HASH, apkDigest);
                        jsonObject2.put(HwSecDiagnoseConstant.MALAPP_APK_CERT, certDigest);
                        jsonObject2.put(HwSecDiagnoseConstant.MALAPP_PATCH_ID, patchId);
                        jsonArray.put(jsonObject2);
                    } catch (JSONException e2) {
                        Log.e(TAG, "JSON put JSONException!");
                    }
                }
            }
        }
        JSONObject pkgJson = new JSONObject();
        try {
            pkgJson.put(HwSecDiagnoseConstant.MALAPP_APK_PACKAGES, jsonArray);
        } catch (JSONException e3) {
            Log.e(TAG, "pkgJson put JSONException!");
        }
        return pkgJson;
    }

    private String byteArray2Hex(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        if (byteArray == null) {
            return null;
        }
        char[] resultCharArray = new char[(byteArray.length * 2)];
        int index = 0;
        for (byte arrayByte : byteArray) {
            int index2 = index + 1;
            resultCharArray[index] = hexDigits[(arrayByte >>> 4) & 15];
            index = index2 + 1;
            resultCharArray[index2] = hexDigits[arrayByte & HwUibcReceiver.CurrentPacket.INPUT_MASK];
        }
        return String.valueOf(resultCharArray);
    }

    /* access modifiers changed from: private */
    public class UeventHandler extends Handler {
        UeventHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 2000 && (msg.obj instanceof UEventObserver.UEvent)) {
                MalAppDetectReport.this.reportData((UEventObserver.UEvent) msg.obj);
            }
        }
    }
}
