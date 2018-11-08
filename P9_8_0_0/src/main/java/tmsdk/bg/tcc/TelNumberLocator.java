package tmsdk.bg.tcc;

import android.content.Context;
import java.util.ArrayList;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.update.UpdateConfig;
import tmsdkobf.lu;

public class TelNumberLocator {
    private static final String YELLOW_PAGE_NAME = "yd.sdb";
    private static TelNumberLocator mInstance = null;
    private Context mContext;
    private long object = newObject();

    static {
        TMSDKContext.registerNatives(3, TelNumberLocator.class);
    }

    protected TelNumberLocator(Context context) {
        this.mContext = context;
        if (0 == this.object) {
            throw new OutOfMemoryError();
        }
        reload();
    }

    private static native void deleteObject(long j);

    private static native int[] getAreaCode(long j);

    private static native int getAreaCodeLocation(long j, int i, StringBuffer stringBuffer);

    private static native int getCityNameList(long j, String str, ArrayList<String> arrayList);

    private static native int[] getCountryCode(long j);

    private static native int getCountryCodeLocation(long j, int i, StringBuffer stringBuffer);

    public static synchronized TelNumberLocator getDefault(Context context) {
        TelNumberLocator telNumberLocator;
        synchronized (TelNumberLocator.class) {
            if (mInstance == null) {
                mInstance = new TelNumberLocator(context);
            }
            telNumberLocator = mInstance;
        }
        return telNumberLocator;
    }

    private static native int getDetailYellowPages(long j, ArrayList<Integer> arrayList, ArrayList<String> arrayList2, ArrayList<String> arrayList3, ArrayList<String> arrayList4);

    private static native int getLocation(long j, StringBuffer stringBuffer, StringBuffer stringBuffer2, StringBuffer stringBuffer3, String str, boolean z);

    private static native int getMobileNumLocation(long j, int i, StringBuffer stringBuffer);

    private static native int getProvinceNameList(long j, ArrayList<String> arrayList);

    private static native int getYellowPages(long j, ArrayList<String> arrayList, ArrayList<String> arrayList2);

    private static native int init(long j, String str, String str2);

    private static native long newObject();

    private static native int patch(long j, String str, String str2, String str3);

    private void throwIfError(int i) {
        switch (i) {
            case -4:
                throw new OutOfMemoryError();
            case 0:
                return;
            default:
                throw new TelNumberLocatorException(i);
        }
    }

    protected void finalize() {
        if (this.object != 0) {
            deleteObject(this.object);
        }
        this.object = 0;
    }

    public int[] getAreaCode() {
        return getAreaCode(this.object);
    }

    public String getAreaCodeLocation(int i) {
        Object -l_2_R = new StringBuffer();
        int -l_3_I = getAreaCodeLocation(this.object, i, -l_2_R);
        if (-l_3_I < 0) {
            if (-l_3_I == -1) {
                return "";
            }
            throwIfError(-l_3_I);
        }
        return -l_2_R.toString();
    }

    public ArrayList<String> getCityNameList(String str) {
        Object -l_2_R = new ArrayList();
        throwIfError(getCityNameList(this.object, str, -l_2_R));
        return -l_2_R;
    }

    public int[] getCountryCode() {
        return getCountryCode(this.object);
    }

    public String getCountryCodeLocation(int i) {
        Object -l_2_R = new StringBuffer();
        int -l_3_I = getCountryCodeLocation(this.object, i, -l_2_R);
        if (-l_3_I < 0) {
            if (-l_3_I == -1) {
                return "";
            }
            throwIfError(-l_3_I);
        }
        return -l_2_R.toString();
    }

    public boolean getDetailYellowPages(ArrayList<Integer> arrayList, ArrayList<String> arrayList2, ArrayList<String> arrayList3, ArrayList<String> arrayList4) {
        if (arrayList2 == null || arrayList3 == null) {
            return false;
        }
        throwIfError(getDetailYellowPages(this.object, arrayList, arrayList2, arrayList3, arrayList4));
        return true;
    }

    public void getLocation(StringBuffer stringBuffer, StringBuffer stringBuffer2, StringBuffer stringBuffer3, String str, boolean z) {
        int -l_6_I = getLocation(this.object, stringBuffer, stringBuffer2, stringBuffer3, str, z);
        if (-l_6_I < 0) {
            if (-l_6_I != -1) {
                throwIfError(-l_6_I);
                return;
            }
            stringBuffer.replace(0, stringBuffer.length(), "");
            stringBuffer2.replace(0, stringBuffer2.length(), "");
            stringBuffer3.replace(0, stringBuffer3.length(), "");
        }
    }

    public String getMobileNumLocation(int i) {
        Object -l_2_R = new StringBuffer();
        int -l_3_I = getMobileNumLocation(this.object, i, -l_2_R);
        if (-l_3_I < 0) {
            if (-l_3_I == -1) {
                return "";
            }
            throwIfError(-l_3_I);
        }
        return -l_2_R.toString();
    }

    public ArrayList<String> getProvinceNameList() {
        Object -l_1_R = new ArrayList();
        throwIfError(getProvinceNameList(this.object, -l_1_R));
        return -l_1_R;
    }

    public boolean getYellowPages(ArrayList<String> arrayList, ArrayList<String> arrayList2) {
        if (arrayList == null || arrayList2 == null) {
            return false;
        }
        throwIfError(getYellowPages(this.object, arrayList, arrayList2));
        return true;
    }

    public int patchLocation(String str, String str2) {
        return patch(this.object, lu.b(this.mContext, UpdateConfig.LOCATION_NAME, null), str, str2);
    }

    public void reload() {
        Object -l_1_R;
        try {
            -l_1_R = lu.b(this.mContext, UpdateConfig.LOCATION_NAME, null);
            Object -l_2_R = lu.b(this.mContext, YELLOW_PAGE_NAME, null);
            if (-l_1_R != null || -l_2_R != null) {
                throwIfError(init(this.object, -l_1_R, -l_2_R));
            }
        } catch (Object -l_1_R2) {
            -l_1_R2.printStackTrace();
        }
    }
}
