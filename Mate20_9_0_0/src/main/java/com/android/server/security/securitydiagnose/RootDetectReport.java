package com.android.server.security.securitydiagnose;

import android.content.Context;
import android.os.Environment;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import libcore.io.IoUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class RootDetectReport {
    private static final int BIT_RPROC_CLEAR = -1025;
    private static final int FILE_NOT_FOUND_ERR = -3;
    private static final String FILE_PROC_ROOT_SCAN = "/proc/root_scan";
    private static final int GENERIC_ERR = -1;
    private static final boolean HW_DEBUG;
    private static final int IO_EXCEPTION = -4;
    private static final String KERNEL_ROOT_STATUS_PATTER_MATCHER = "^\\d{1,5}$";
    private static final boolean RS_DEBUG = SystemProperties.get("ro.secure", "1").equals("0");
    private static final String TAG = "RootDetectReport";
    private static long mEndTime = 0;
    private static RootDetectReport mInstance;
    private static long mStartTime = 0;
    private Context mContext;
    private Listener mListener;
    private boolean rootScanHasTrigger = false;

    public interface Listener {
        void onRootReport(JSONObject jSONObject, boolean z);
    }

    private static class RootDataBundle {
        private static final int GID_UNCHANGED = -1;
        private static final int MAX_DATA_RECORDS = 100;
        private static final String RSCAN_LIST_FILE = "root_scan.list";
        private static final int RSCAN_LIST_FILE_MODE = 384;
        private static final String SYSTEM_DIR = "system/";
        private static final int UID_UNCHANGED = -1;
        private ArrayList<String> mRootDataList;

        public RootDataBundle() {
            this.mRootDataList = null;
            this.mRootDataList = new ArrayList();
        }

        private File getRootDataFile() {
            return new File(Environment.getDataDirectory(), "system/root_scan.list");
        }

        private String sha256(byte[] data) {
            if (data == null) {
                return null;
            }
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(data);
                return bytesToString(md.digest());
            } catch (NoSuchAlgorithmException e) {
                Log.e(RootDetectReport.TAG, "sha256 algorithm failed");
                return null;
            }
        }

        private String bytesToString(byte[] bytes) {
            if (bytes == null) {
                return null;
            }
            char[] hexChars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
            char[] chars = new char[(bytes.length * 2)];
            for (int j = 0; j < bytes.length; j++) {
                int byteValue = bytes[j] & 255;
                chars[j * 2] = hexChars[byteValue >>> 4];
                chars[(j * 2) + 1] = hexChars[byteValue & 15];
            }
            return new String(chars).toUpperCase(Locale.US);
        }

        private void writeRootData() {
            synchronized (this.mRootDataList) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(getRootDataFile());
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    StringBuilder sb = new StringBuilder();
                    Iterator it = this.mRootDataList.iterator();
                    while (it.hasNext()) {
                        String data = (String) it.next();
                        sb.setLength(0);
                        sb.append(data);
                        sb.append(10);
                        bos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    bos.flush();
                    IoUtils.closeQuietly(bos);
                } catch (IOException e) {
                    try {
                        Log.e(RootDetectReport.TAG, "Failed to write root result data");
                        IoUtils.closeQuietly(null);
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(null);
                        IoUtils.closeQuietly(null);
                    }
                }
                IoUtils.closeQuietly(fos);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0020, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void readRootData() {
            synchronized (this.mRootDataList) {
                this.mRootDataList.clear();
                File file = getRootDataFile();
                if (file.exists()) {
                    String reader = null;
                    try {
                        BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                        while (true) {
                            String hashCod = reader2.readLine();
                            if (!TextUtils.isEmpty(hashCod)) {
                                this.mRootDataList.add(hashCod);
                                continue;
                            }
                            if (hashCod == null) {
                                break;
                            }
                        }
                        IoUtils.closeQuietly(reader2);
                    } catch (FileNotFoundException e) {
                        Log.e(RootDetectReport.TAG, "file root result list cannot be found");
                        IoUtils.closeQuietly(reader);
                    } catch (IOException e2) {
                        try {
                            Log.e(RootDetectReport.TAG, "Failed to read root result list");
                        } finally {
                            IoUtils.closeQuietly(reader);
                        }
                    }
                } else if (RootDetectReport.HW_DEBUG) {
                    Log.e(RootDetectReport.TAG, "readRootData file NOT exist!");
                }
            }
        }

        public boolean hasSame(JSONObject data) {
            if (data == null) {
                Log.e(RootDetectReport.TAG, "hasSame The data is NULL!");
                return false;
            }
            readRootData();
            String rootDataHash = null;
            try {
                rootDataHash = sha256(data.toString().getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.e(RootDetectReport.TAG, "hasSame encoding unsupported");
            }
            if (TextUtils.isEmpty(rootDataHash)) {
                Log.e(RootDetectReport.TAG, "hasSame HASHCODE is null!");
                return false;
            }
            if (RootDetectReport.HW_DEBUG) {
                String str = RootDetectReport.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hasSame rootDataHash = ");
                stringBuilder.append(rootDataHash);
                Log.d(str, stringBuilder.toString());
            }
            synchronized (this.mRootDataList) {
                if (!this.mRootDataList.isEmpty() && this.mRootDataList.contains(rootDataHash)) {
                    if (RootDetectReport.HW_DEBUG) {
                        Log.d(RootDetectReport.TAG, "addDataList has existed");
                    }
                    return true;
                } else if (this.mRootDataList.size() < 100) {
                    this.mRootDataList.add(rootDataHash);
                } else {
                    try {
                        this.mRootDataList.remove(0);
                        this.mRootDataList.add(rootDataHash);
                    } catch (IndexOutOfBoundsException e2) {
                        Log.e(RootDetectReport.TAG, "IndexOutOfBoundsException");
                    }
                }
            }
            writeRootData();
            return false;
        }
    }

    static {
        boolean z = Log.HWINFO || RS_DEBUG || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HW_DEBUG = z;
    }

    private void setRootStatusProperty(int rootstatus) {
        try {
            SystemProperties.set("persist.sys.root.status", Integer.toString(rootstatus & BIT_RPROC_CLEAR));
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setRootStatusProperty failed, stpGetStatusAllIDRetValue = ");
            stringBuilder.append(rootstatus & BIT_RPROC_CLEAR);
            Log.e(str, stringBuilder.toString());
        }
    }

    public JSONObject parcelStpHidlRootData(int rootstatus) {
        JSONObject json = new JSONObject();
        try {
            json.put(HwSecDiagnoseConstant.ROOT_STATUS, rootstatus & BIT_RPROC_CLEAR);
            int i = 0;
            json.put(HwSecDiagnoseConstant.ROOT_ERR_CODE, 0);
            json.put(HwSecDiagnoseConstant.ROOT_CHECK_CODE, (rootstatus & 1) > 0 ? 1 : 0);
            json.put(HwSecDiagnoseConstant.ROOT_SYS_CALL, (rootstatus & 2) > 0 ? 1 : 0);
            json.put(HwSecDiagnoseConstant.ROOT_SE_HOOKS, (rootstatus & 4) > 0 ? 1 : 0);
            json.put(HwSecDiagnoseConstant.ROOT_SE_STATUS, (rootstatus & 8) > 0 ? 1 : 0);
            json.put(HwSecDiagnoseConstant.ROOT_CHECK_SU, (rootstatus & 16) > 0 ? 1 : 0);
            json.put(HwSecDiagnoseConstant.ROOT_SYS_RW, (rootstatus & 32) > 0 ? 1 : 0);
            json.put(HwSecDiagnoseConstant.ROOT_CHECK_ADBD, (rootstatus & 64) > 0 ? 1 : 0);
            json.put(HwSecDiagnoseConstant.ROOT_VB_STATUS, (rootstatus & 128) > 0 ? 1 : 0);
            json.put(HwSecDiagnoseConstant.ROOT_CHECK_PROP, (rootstatus & 256) > 0 ? 1 : 0);
            String str = HwSecDiagnoseConstant.ROOT_CHECK_SETIDS;
            if ((rootstatus & 512) > 0) {
                i = 1;
            }
            json.put(str, i);
            return json;
        } catch (JSONException e) {
            Log.e(TAG, "parcel root data, something wrong with the json object");
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c1  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x011c  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x0105  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c1  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x0105  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x011c  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c1  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x011c  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x0105  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c1  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x0105  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x011c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int triggerRootScan() {
        String kernelRootStatus;
        int ret = 0;
        boolean needReport = false;
        mStartTime = System.currentTimeMillis();
        int i = 1;
        if (!this.rootScanHasTrigger) {
            String readLine;
            BufferedReader reader = null;
            InputStreamReader mInputStreamReader = null;
            FileInputStream mFileInputStream = null;
            try {
                mFileInputStream = new FileInputStream(new File(FILE_PROC_ROOT_SCAN));
                mInputStreamReader = new InputStreamReader(mFileInputStream, "UTF-8");
                reader = new BufferedReader(mInputStreamReader);
                readLine = reader.readLine();
                kernelRootStatus = readLine;
                if (readLine != null && kernelRootStatus.matches(KERNEL_ROOT_STATUS_PATTER_MATCHER)) {
                    ret = Integer.parseInt(kernelRootStatus);
                    this.rootScanHasTrigger = true;
                }
                try {
                    mFileInputStream.close();
                    mInputStreamReader.close();
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "failed to close the trigger proc file");
                    ret = -4;
                    IoUtils.closeQuietly(reader);
                    if (HW_DEBUG) {
                    }
                    ret = AppLayerStpProxy.getInstance().getRootStatusSync();
                    if (ret < 0) {
                    }
                }
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "triggerRootScan, trigger file cannot be found");
                ret = -3;
                if (mFileInputStream != null) {
                    try {
                        mFileInputStream.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "failed to close the trigger proc file");
                        ret = -4;
                        IoUtils.closeQuietly(reader);
                        if (HW_DEBUG) {
                        }
                        ret = AppLayerStpProxy.getInstance().getRootStatusSync();
                        if (ret < 0) {
                        }
                    }
                }
                if (mInputStreamReader != null) {
                    mInputStreamReader.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e4) {
                Log.e(TAG, "failed to read the trigger proc file");
                ret = -4;
                if (mFileInputStream != null) {
                    try {
                        mFileInputStream.close();
                    } catch (IOException e5) {
                        Log.e(TAG, "failed to close the trigger proc file");
                        ret = -4;
                        IoUtils.closeQuietly(reader);
                        if (HW_DEBUG) {
                        }
                        ret = AppLayerStpProxy.getInstance().getRootStatusSync();
                        if (ret < 0) {
                        }
                    }
                }
                if (mInputStreamReader != null) {
                    mInputStreamReader.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (NumberFormatException e6) {
                Log.e(TAG, "some data is not of the type Integer during parsing trigger file");
                ret = -1;
                if (mFileInputStream != null) {
                    try {
                        mFileInputStream.close();
                    } catch (IOException e7) {
                        Log.e(TAG, "failed to close the trigger proc file");
                        ret = -4;
                        IoUtils.closeQuietly(reader);
                        if (HW_DEBUG) {
                        }
                        ret = AppLayerStpProxy.getInstance().getRootStatusSync();
                        if (ret < 0) {
                        }
                    }
                }
                if (mInputStreamReader != null) {
                    mInputStreamReader.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (Throwable th) {
                if (mFileInputStream != null) {
                    try {
                        mFileInputStream.close();
                    } catch (IOException e8) {
                        Log.e(TAG, "failed to close the trigger proc file");
                        IoUtils.closeQuietly(reader);
                    }
                }
                if (mInputStreamReader != null) {
                    mInputStreamReader.close();
                }
                if (reader != null) {
                    reader.close();
                }
                IoUtils.closeQuietly(reader);
            }
            IoUtils.closeQuietly(reader);
            if (HW_DEBUG) {
                readLine = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bootcompleted trigger return value = ");
                stringBuilder.append(ret);
                Log.d(readLine, stringBuilder.toString());
            }
        }
        ret = AppLayerStpProxy.getInstance().getRootStatusSync();
        StringBuilder stringBuilder2;
        if (ret < 0) {
            kernelRootStatus = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("get root status by category failed. ret = ");
            stringBuilder2.append(ret);
            Log.e(kernelRootStatus, stringBuilder2.toString());
            return ret;
        } else if (ret == 0) {
            setRootStatusProperty(ret);
            mEndTime = System.currentTimeMillis();
            if (HW_DEBUG) {
                kernelRootStatus = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("root status is ok, whole rootscan run TIME = ");
                stringBuilder2.append(mEndTime - mStartTime);
                stringBuilder2.append("ms");
                Log.d(kernelRootStatus, stringBuilder2.toString());
            }
            return ret;
        } else {
            ret = AppLayerStpProxy.getInstance().getEachItemRootStatus();
            if (ret < 0) {
                kernelRootStatus = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("get each item root status failed. ret = ");
                stringBuilder2.append(ret);
                Log.e(kernelRootStatus, stringBuilder2.toString());
                return ret;
            }
            StringBuilder stringBuilder3;
            if (HW_DEBUG) {
                kernelRootStatus = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("all item root scan result from hidl is:");
                stringBuilder3.append(ret);
                Log.d(kernelRootStatus, stringBuilder3.toString());
            }
            mEndTime = System.currentTimeMillis();
            if (HW_DEBUG) {
                kernelRootStatus = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("from trigger to receive the result run TIME =");
                stringBuilder3.append(mEndTime - mStartTime);
                stringBuilder3.append("ms");
                Log.d(kernelRootStatus, stringBuilder3.toString());
            }
            setRootStatusProperty(ret);
            JSONObject json = parcelStpHidlRootData(ret);
            if (ret > 0) {
                boolean hasSame = new RootDataBundle().hasSame(json);
                needReport = hasSame ^ 1;
                if (HW_DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("parcelStpHidlRootData hasSame = ");
                    stringBuilder4.append(hasSame);
                    stringBuilder4.append(" needReport = ");
                    stringBuilder4.append(needReport);
                    Log.d(str, stringBuilder4.toString());
                }
            }
            try {
                String str2;
                String str3 = HwSecDiagnoseConstant.ROOT_ROOT_PRO;
                if ((ret & 1024) <= 0) {
                    i = 0;
                }
                json.put(str3, i);
                if (HW_DEBUG) {
                    str2 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("parcelStpHidlRootData json = ");
                    stringBuilder3.append(json);
                    Log.d(str2, stringBuilder3.toString());
                }
                this.mListener.onRootReport(json, needReport);
                mEndTime = System.currentTimeMillis();
                if (HW_DEBUG) {
                    str2 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("trigger root scan success!, whole rootscan run TIME = ");
                    stringBuilder3.append(mEndTime - mStartTime);
                    stringBuilder3.append("ms");
                    Log.d(str2, stringBuilder3.toString());
                }
                return ret;
            } catch (JSONException e9) {
                Log.e(TAG, "proclist put error");
                return ret;
            }
        }
    }

    void setListener(Listener listener) {
        this.mListener = listener;
    }

    private RootDetectReport(Context context) {
        this.mContext = context;
    }

    public static void init(Context context) {
        synchronized (RootDetectReport.class) {
            if (mInstance == null) {
                mInstance = new RootDetectReport(context);
            }
        }
    }

    public static RootDetectReport getInstance() {
        RootDetectReport rootDetectReport;
        synchronized (RootDetectReport.class) {
            rootDetectReport = mInstance;
        }
        return rootDetectReport;
    }
}
