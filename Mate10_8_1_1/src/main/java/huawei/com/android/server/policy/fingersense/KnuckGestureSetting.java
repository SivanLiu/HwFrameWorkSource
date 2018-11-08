package huawei.com.android.server.policy.fingersense;

import android.util.Log;
import java.io.FileReader;
import java.io.IOException;

public class KnuckGestureSetting {
    private static final String ACC_PATH = "/sys/devices/platform/huawei_sensor/acc_info";
    public static final short DKF_D_I = (short) 18;
    public static final short DKF_T_I = (short) 17;
    public static final short DKS_D_I = (short) 6;
    public static final short DKS_T_I = (short) 5;
    public static final short DK_F_T = (short) 14;
    public static final short DK_S_T = (short) 1;
    public static final short FSF_1S_2F_C = (short) 22;
    public static final short FSF_C_C = (short) 21;
    public static final short FSF_D_C = (short) 20;
    public static final short FSF_T_C = (short) 19;
    public static final int KNOCK_GESTURE_DATA_RECORD = 936006000;
    public static final short LG_F_T = (short) 15;
    public static final short LG_S_T = (short) 3;
    public static final short L_C_S_T = (short) 7;
    public static final short L_E_S_T = (short) 8;
    public static final short L_L_S_T = (short) 12;
    public static final short L_M_S_T = (short) 9;
    public static final short L_S_S_T = (short) 10;
    public static final short L_W_S_T = (short) 11;
    public static final short RG_S_T = (short) 2;
    public static final short SKF_T_I = (short) 16;
    public static final short SKS_T_I = (short) 4;
    public static final short SK_F_T = (short) 13;
    public static final short SK_S_T = (short) 0;
    public static final int STATUS_S_F = 1;
    public static final short S_INFO = (short) 24;
    private static final String TAG = "KnuckGestureSetting";
    public static final short TP_INFO = (short) 23;
    private static final String TP_PATH = "/sys/touchscreen/touch_chip_info";
    private static KnuckGestureSetting sInstance;
    private String accVendorName = getVendorName(ACC_PATH);
    private String tpVendorName = getVendorName(TP_PATH);

    private KnuckGestureSetting() {
    }

    public static synchronized KnuckGestureSetting getInstance() {
        KnuckGestureSetting knuckGestureSetting;
        synchronized (KnuckGestureSetting.class) {
            if (sInstance == null) {
                sInstance = new KnuckGestureSetting();
            }
            knuckGestureSetting = sInstance;
        }
        return knuckGestureSetting;
    }

    private String getVendorName(String vendorPath) {
        IOException ex;
        Throwable th;
        FileReader fileReader = null;
        String str = null;
        try {
            FileReader reader = new FileReader(vendorPath);
            try {
                char[] buf = new char[100];
                int n = reader.read(buf, 0, 100);
                if (n > 1) {
                    String vendorName = new String(buf, 0, n);
                    try {
                        Log.d(TAG, "getVendorName: path:" + vendorPath + ", value:" + vendorName);
                        str = vendorName;
                    } catch (IOException e) {
                        ex = e;
                        str = vendorName;
                        fileReader = reader;
                        try {
                            Log.e(TAG, "couldn't read vendor name from " + vendorPath + ":" + ex);
                            if (fileReader != null) {
                                try {
                                    fileReader.close();
                                } catch (IOException e2) {
                                }
                            }
                            return str;
                        } catch (Throwable th2) {
                            th = th2;
                            if (fileReader != null) {
                                try {
                                    fileReader.close();
                                } catch (IOException e3) {
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        fileReader = reader;
                        if (fileReader != null) {
                            fileReader.close();
                        }
                        throw th;
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
                fileReader = reader;
            } catch (IOException e5) {
                ex = e5;
                fileReader = reader;
                Log.e(TAG, "couldn't read vendor name from " + vendorPath + ":" + ex);
                if (fileReader != null) {
                    fileReader.close();
                }
                return str;
            } catch (Throwable th4) {
                th = th4;
                fileReader = reader;
                if (fileReader != null) {
                    fileReader.close();
                }
                throw th;
            }
        } catch (IOException e6) {
            ex = e6;
            Log.e(TAG, "couldn't read vendor name from " + vendorPath + ":" + ex);
            if (fileReader != null) {
                fileReader.close();
            }
            return str;
        }
        return str;
    }

    public String getTpVendorName() {
        return this.tpVendorName;
    }

    public String getAccVendorName() {
        return this.accVendorName;
    }

    public void reportTpAndAccVendor(int eventID, short tpParamId, short accParamId) {
        Reporter.reportUserData(eventID, tpParamId, this.tpVendorName);
        Reporter.reportUserData(eventID, accParamId, this.accVendorName);
    }
}
