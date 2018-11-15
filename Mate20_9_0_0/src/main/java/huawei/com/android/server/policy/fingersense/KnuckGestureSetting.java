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
    public static final short LCDINFO = (short) 25;
    private static final String LCD_PATH = "sys/class/graphics/fb0/lcd_model";
    public static final short LG_F_T = (short) 15;
    public static final short LG_S_T = (short) 3;
    public static final short L_C_S_T = (short) 7;
    public static final short L_E_S_T = (short) 8;
    public static final short L_L_S_T = (short) 12;
    public static final short L_M_S_T = (short) 9;
    public static final short L_S_S_T = (short) 10;
    public static final short L_W_S_T = (short) 11;
    public static final short RG_S_T = (short) 2;
    public static final short SCRORIENTATION = (short) 26;
    public static final short SKF_T_I = (short) 16;
    public static final short SKS_T_I = (short) 4;
    public static final short SK_F_T = (short) 13;
    public static final short SK_S_T = (short) 0;
    public static final int STATUS_S_F = 1;
    public static final short S_INFO = (short) 24;
    private static final String TAG = "KnuckGestureSetting";
    public static final short TP_INFO = (short) 23;
    private static final String TP_PATH = "/sys/touchscreen/touch_chip_info";
    public static final long reportIntervalMS = 1000;
    private static KnuckGestureSetting sInstance;
    private String accVendorName = getVendorName(ACC_PATH);
    private long lastReportFSTime;
    private String lcdInfo = getVendorName(LCD_PATH);
    private int mOrientation = -1;
    private String tpVendorName = getVendorName(TP_PATH);

    public long getLastReportFSTime() {
        return this.lastReportFSTime;
    }

    public void setLastReportFSTime(long reportTime) {
        this.lastReportFSTime = reportTime;
    }

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
        FileReader reader = null;
        String vendorName = null;
        try {
            reader = new FileReader(vendorPath);
            char[] buf = new char[100];
            int n = reader.read(buf, 0, 100);
            if (n > 1) {
                vendorName = new String(buf, 0, n);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getVendorName: path:");
                stringBuilder.append(vendorPath);
                stringBuilder.append(", value:");
                stringBuilder.append(vendorName);
                Log.d(str, stringBuilder.toString());
            }
            try {
                reader.close();
            } catch (IOException e) {
            }
        } catch (IOException ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("couldn't read vendor name from ");
            stringBuilder2.append(vendorPath);
            stringBuilder2.append(":");
            stringBuilder2.append(ex);
            Log.e(str2, stringBuilder2.toString());
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e2) {
                }
            }
        }
        return vendorName;
    }

    public String getTpVendorName() {
        return this.tpVendorName;
    }

    public String getAccVendorName() {
        return this.accVendorName;
    }

    public String getLcdInfo() {
        return this.lcdInfo;
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setOrientation(int orientation) {
        switch (orientation) {
            case 0:
            case 2:
                this.mOrientation = 0;
                return;
            case 1:
            case 3:
                this.mOrientation = 1;
                return;
            default:
                this.mOrientation = -1;
                return;
        }
    }
}
