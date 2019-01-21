package com.huawei.displayengine;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.IPowerManager;
import android.os.IPowerManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.huawei.displayengine.DisplayEngineDBManager.AlgorithmESCWKey;
import com.huawei.displayengine.DisplayEngineDBManager.BrightnessCurveKey;
import com.huawei.displayengine.DisplayEngineDBManager.DragInformationKey;
import java.util.ArrayList;

/* compiled from: BrigntnessTrainingAlgoImpl */
class BrightnessTrainingAlgoImpl {
    public static final int ABORT_TRAINING = 5;
    public static final int CREATE_TRAINING_ENGINE = 0;
    public static final int CURVE_COUNT = 35;
    public static final int DESTROY_TRAINING_ENGINE = 2;
    public static final int ESCW_COUNT = 9;
    public static final int GETPARAM_TRAINING_ENGINE = 4;
    public static final int MAX_ALGO_RESULT = 2000;
    public static final int PROCESS_TRAINING_ENGINE = 1;
    public static final int SETPARAM_TRAINING_ENGINE = 3;
    private static final String TAG = "DE J BrightnessTrainingAlgoImpl";
    private static String mAlgoXmlPath = "";
    private AlgoParam mAlgoInfo;
    private final Context mContext;
    private int mHandle = -1;
    private Object mLockJNI = new Object();

    public BrightnessTrainingAlgoImpl(Context context) {
        DElog.i(TAG, "BrightnessTrainingAlgoImpl enter");
        this.mContext = context;
    }

    private int readDragInfo() {
        StringBuilder stringBuilder;
        DisplayEngineDBManager dbManager = DisplayEngineDBManager.getInstance(this.mContext);
        ArrayList<Bundle> items = dbManager.getAllRecords(DragInformationKey.TAG, new Bundle());
        DisplayEngineDataCleaner dataCleaner = DisplayEngineDataCleaner.getInstance(this.mContext);
        items = dataCleaner.cleanData(items, 0);
        this.mAlgoInfo.mDragCount = 0;
        this.mAlgoInfo.mDragSize = 0;
        this.mAlgoInfo.mDragInfo = "";
        ArrayList<Bundle> items2;
        DisplayEngineDataCleaner dataCleaner2;
        if (items != null) {
            int i = 0;
            while (i < items.size()) {
                Bundle data = (Bundle) items.get(i);
                long time = data.getLong("TimeStamp");
                float start = data.getFloat(DragInformationKey.STARTPOINT);
                float stop = data.getFloat(DragInformationKey.STOPPOINT);
                int al = data.getInt("AmbientLight");
                boolean cover_falg = data.getBoolean(DragInformationKey.PROXIMITYPOSITIVE);
                int appType = data.getInt("AppType");
                int gameState = data.getInt(DragInformationKey.GAMESTATE);
                String pkgName = data.getString(DragInformationKey.PACKAGE);
                DisplayEngineDBManager dbManager2 = dbManager;
                AlgoParam algoParam = this.mAlgoInfo;
                items2 = items;
                algoParam.mDragCount++;
                algoParam = this.mAlgoInfo;
                stringBuilder = new StringBuilder();
                dataCleaner2 = dataCleaner;
                stringBuilder.append(this.mAlgoInfo.mDragInfo);
                stringBuilder.append("[");
                stringBuilder.append(time);
                stringBuilder.append(", ");
                stringBuilder.append(start);
                stringBuilder.append(", ");
                stringBuilder.append(stop);
                stringBuilder.append(", ");
                stringBuilder.append(al);
                stringBuilder.append(", ");
                stringBuilder.append(cover_falg);
                stringBuilder.append(", ");
                stringBuilder.append(appType);
                stringBuilder.append("]");
                algoParam.mDragInfo = stringBuilder.toString();
                i++;
                dbManager = dbManager2;
                items = items2;
                dataCleaner = dataCleaner2;
            }
            items2 = items;
            dataCleaner2 = dataCleaner;
            this.mAlgoInfo.mDragSize = this.mAlgoInfo.mDragInfo.length();
        } else {
            items2 = items;
            dataCleaner2 = dataCleaner;
        }
        if (this.mAlgoInfo.mDragCount >= 2) {
            return 0;
        }
        String str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("DragInfo number is not enough ! count: ");
        stringBuilder.append(this.mAlgoInfo.mDragCount);
        DElog.i(str, stringBuilder.toString());
        return -1;
    }

    private int readCurveByTag(DisplayEngineDBManager dbManager, StringBuffer text, String name) {
        if (text == null || name == null) {
            DElog.i(TAG, "text is null");
            return -1;
        }
        ArrayList<Bundle> items = dbManager.getAllRecords(name, new Bundle());
        if (items == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Read ");
            stringBuilder.append(name);
            stringBuilder.append(" failed !");
            DElog.i(str, stringBuilder.toString());
            return -1;
        } else if (items.size() == 0) {
            DElog.i(TAG, "DisplayEngineDB low curve size is 0");
            return -1;
        } else {
            for (int i = 0; i < items.size(); i++) {
                Bundle data = (Bundle) items.get(i);
                float al = data.getFloat("AmbientLight");
                float bl = data.getFloat(BrightnessCurveKey.BL);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[");
                stringBuilder2.append(al);
                stringBuilder2.append(",");
                stringBuilder2.append(bl);
                stringBuilder2.append("]");
                text.append(stringBuilder2.toString());
            }
            return items.size();
        }
    }

    private int readBrightnessCurve() {
        DisplayEngineDBManager dbManager = DisplayEngineDBManager.getInstance(this.mContext);
        if (dbManager == null) {
            DElog.e(TAG, "dbManager is null");
            return -1;
        }
        StringBuffer low_text = new StringBuffer();
        int count = readCurveByTag(dbManager, low_text, "BrightnessCurveLow");
        if (count <= 0) {
            DElog.i(TAG, "Read low failed!");
            return -1;
        }
        this.mAlgoInfo.mBLCurveTypeLowLuma = low_text.toString();
        this.mAlgoInfo.mLowLumaCount = count;
        StringBuffer middle_text = new StringBuffer();
        count = readCurveByTag(dbManager, middle_text, "BrightnessCurveMiddle");
        if (count <= 0) {
            DElog.i(TAG, "Read middle failed!");
            return -1;
        }
        this.mAlgoInfo.mBLCurveTypeMedialLuma = middle_text.toString();
        this.mAlgoInfo.mMedialLumaCount = count;
        StringBuffer hight_text = new StringBuffer();
        count = readCurveByTag(dbManager, hight_text, "BrightnessCurveHigh");
        if (count <= 0) {
            DElog.i(TAG, "Read hight failed!");
            return -1;
        }
        this.mAlgoInfo.mBLCurveTypeHighLuma = hight_text.toString();
        this.mAlgoInfo.mHighLumaCount = count;
        StringBuffer default_text = new StringBuffer();
        count = readCurveByTag(dbManager, default_text, "BrightnessCurveDefault");
        if (count <= 0) {
            DElog.i(TAG, "Read default failed!");
            return -1;
        }
        this.mAlgoInfo.mBLCurveTypeDefaultLuma = default_text.toString();
        this.mAlgoInfo.mDefaultLumaCount = count;
        ArrayList<Bundle> items = dbManager.getAllRecords("AlgorithmESCW", new Bundle());
        if (items == null) {
            DElog.i(TAG, "DisplayEngineDB ESCW = null");
            return -1;
        } else if (items.size() == 0) {
            DElog.i(TAG, "DisplayEngineDB ESCW size is 0");
            return -1;
        } else {
            StringBuffer text = new StringBuffer();
            text.append("[");
            for (int i = 0; i < items.size(); i++) {
                float escw = ((Bundle) items.get(i)).getFloat(AlgorithmESCWKey.ESCW);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(escw);
                stringBuilder.append(",");
                text.append(stringBuilder.toString());
            }
            this.mAlgoInfo.mESCW = text.toString();
            this.mAlgoInfo.mESCWCount = 1;
            return 0;
        }
    }

    private static float getFloat(byte[] b, int offset) {
        int accum = 0;
        for (int shiftBy = 0; shiftBy < 4; shiftBy++) {
            accum |= (b[shiftBy + offset] & 255) << (shiftBy * 8);
        }
        return Float.intBitsToFloat(accum);
    }

    private int writeCurveByTag(DisplayEngineDBManager dbManager, byte[] curve, String name, int buffer_offset) {
        if (dbManager == null || name == null || curve == null) {
            return -1;
        }
        Bundle data = new Bundle();
        float[] alValues = new float[35];
        float[] blValues = new float[35];
        for (int i = 0; i < 35; i++) {
            int offset = (i * 8) + buffer_offset;
            alValues[i] = getFloat(curve, offset);
            blValues[i] = getFloat(curve, offset + 4);
        }
        buffer_offset += 280;
        data.putFloatArray("AmbientLight", alValues);
        data.putFloatArray(BrightnessCurveKey.BL, blValues);
        dbManager.addorUpdateRecord(name, data);
        return buffer_offset;
    }

    private int writeBrightnessCurve(byte[] curve) {
        DisplayEngineDBManager dbManager = DisplayEngineDBManager.getInstance(this.mContext);
        if (dbManager == null) {
            DElog.e(TAG, "dbManager is null");
            return -1;
        }
        int i;
        Bundle data = new Bundle();
        float[] escwValues = new float[9];
        int offset = 0;
        for (i = 0; i < 9; i++) {
            escwValues[i] = getFloat(curve, 0 + (i * 4));
        }
        data.putFloatArray(AlgorithmESCWKey.ESCW, escwValues);
        dbManager.addorUpdateRecord("AlgorithmESCW", data);
        i = writeCurveByTag(dbManager, curve, "BrightnessCurveLow", 36);
        if (i <= 0) {
            DElog.e(TAG, "Write  low curvefailed! ");
            return -1;
        }
        i = writeCurveByTag(dbManager, curve, "BrightnessCurveMiddle", i);
        if (i <= 0) {
            DElog.e(TAG, "Write  middle curve failed! ");
            return -1;
        }
        i = writeCurveByTag(dbManager, curve, "BrightnessCurveHigh", i);
        if (i <= 0) {
            DElog.e(TAG, "Write  middle high failed! ");
            return -1;
        } else if (writeCurveByTag(dbManager, curve, "BrightnessCurveDefault", i) > 0) {
            return 0;
        } else {
            DElog.e(TAG, "Write  middle default failed! ");
            return -1;
        }
    }

    private int processAlgo() {
        if (DisplayEngineLibraries.nativeProcessAlgorithm(1, this.mHandle, 3, this.mAlgoInfo, null) != 0) {
            DElog.e(TAG, " SETPARAM_TRAINING_ENGINE failed");
            return -1;
        } else if (DisplayEngineLibraries.nativeProcessAlgorithm(1, this.mHandle, 0, null, null) != 0) {
            DElog.e(TAG, " CREATE_TRAINING_ENGINE failed");
            return -1;
        } else {
            byte[] outbuffer = new byte[2000];
            if (DisplayEngineLibraries.nativeProcessAlgorithm(1, this.mHandle, 1, null, outbuffer) != 0) {
                DElog.i(TAG, " PROCESS_TRAINING_ENGINE failed!");
                if (DisplayEngineLibraries.nativeProcessAlgorithm(1, this.mHandle, 2, null, null) != 0) {
                    DElog.e(TAG, " DESTROY_TRAINING_ENGINE failed");
                }
                return -1;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ESCW ");
            stringBuilder.append(outbuffer[0]);
            stringBuilder.append(", ");
            stringBuilder.append(outbuffer[1]);
            stringBuilder.append(", ");
            stringBuilder.append(outbuffer[2]);
            stringBuilder.append(", ");
            stringBuilder.append(outbuffer[3]);
            stringBuilder.append(", eswValue = ");
            stringBuilder.append(outbuffer[4]);
            DElog.i(str, stringBuilder.toString());
            writeBrightnessCurve(outbuffer);
            try {
                IPowerManager power = Stub.asInterface(ServiceManager.getService("power"));
                if (power == null) {
                    DElog.i(TAG, "power is null !");
                    if (DisplayEngineLibraries.nativeProcessAlgorithm(1, this.mHandle, 2, null, null) != 0) {
                        DElog.e(TAG, " DESTROY_TRAINING_ENGINE failed");
                    }
                    return -1;
                }
                Bundle data = new Bundle();
                data.putInt("CurveUpdateFlag", 1);
                power.hwBrightnessSetData("PersonalizedBrightness", data);
                if (DisplayEngineLibraries.nativeProcessAlgorithm(1, this.mHandle, 2, null, null) == 0) {
                    return 0;
                }
                DElog.e(TAG, " DESTROY_TRAINING_ENGINE failed");
                return -1;
            } catch (RemoteException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to call hwbrightness error:");
                stringBuilder.append(e.getMessage());
                DElog.i(str, stringBuilder.toString());
            }
        }
    }

    /* JADX WARNING: Missing block: B:39:0x02b1, code skipped:
            if (processAlgo() == 0) goto L_0x02ca;
     */
    /* JADX WARNING: Missing block: B:40:0x02b3, code skipped:
            com.huawei.displayengine.DElog.i(TAG, " processAlgo failed! ");
     */
    /* JADX WARNING: Missing block: B:41:0x02c0, code skipped:
            if (com.huawei.displayengine.DisplayEngineLibraries.nativeDeinitAlgorithm(1, r14.mHandle) == 0) goto L_0x02c9;
     */
    /* JADX WARNING: Missing block: B:42:0x02c2, code skipped:
            com.huawei.displayengine.DElog.e(TAG, " nativeDeinitAlgorithm failed! ");
     */
    /* JADX WARNING: Missing block: B:43:0x02c9, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:44:0x02ca, code skipped:
            r5 = r14.mLockJNI;
     */
    /* JADX WARNING: Missing block: B:45:0x02cc, code skipped:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            r0 = com.huawei.displayengine.DisplayEngineLibraries.nativeDeinitAlgorithm(1, r14.mHandle);
            r14.mHandle = -1;
     */
    /* JADX WARNING: Missing block: B:48:0x02d6, code skipped:
            if (r0 == 0) goto L_0x02e1;
     */
    /* JADX WARNING: Missing block: B:49:0x02d8, code skipped:
            com.huawei.displayengine.DElog.e(TAG, " nativeDeinitAlgorithm failed! ");
     */
    /* JADX WARNING: Missing block: B:50:0x02df, code skipped:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:51:0x02e0, code skipped:
            return -1;
     */
    /* JADX WARNING: Missing block: B:52:0x02e1, code skipped:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:53:0x02e2, code skipped:
            com.huawei.displayengine.DElog.i(TAG, "processTraining  stop ");
     */
    /* JADX WARNING: Missing block: B:54:0x02e9, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int processTraining() {
        DElog.i(TAG, "processTraining  start !! ");
        this.mAlgoInfo = new AlgoParam();
        if (readDragInfo() != 0) {
            DElog.i(TAG, "no DragInfo ! ");
            return -1;
        }
        if (readBrightnessCurve() != 0) {
            DElog.i(TAG, "no BrightnessCurve  in DataBase! ");
            try {
                IPowerManager power = Stub.asInterface(ServiceManager.getService("power"));
                if (power == null) {
                    DElog.i(TAG, "power is null");
                    return -1;
                }
                Bundle data = new Bundle();
                int ret_value = power.hwBrightnessGetData("PersonalizedBrightness", data);
                ArrayList<PointF> list = data.getParcelableArrayList("DefaultCurve");
                StringBuffer text = new StringBuffer();
                for (int i = 0; i < list.size(); i++) {
                    float al = ((PointF) list.get(i)).x;
                    float bl = ((PointF) list.get(i)).y;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[");
                    stringBuilder.append(al);
                    stringBuilder.append(",");
                    stringBuilder.append(bl);
                    stringBuilder.append("]");
                    text.append(stringBuilder.toString());
                }
                this.mAlgoInfo.mBLCurveTypeLowLuma = text.toString();
                this.mAlgoInfo.mLowLumaCount = list.size();
                this.mAlgoInfo.mBLCurveTypeMedialLuma = text.toString();
                this.mAlgoInfo.mMedialLumaCount = list.size();
                this.mAlgoInfo.mBLCurveTypeHighLuma = text.toString();
                this.mAlgoInfo.mHighLumaCount = list.size();
                this.mAlgoInfo.mBLCurveTypeDefaultLuma = text.toString();
                this.mAlgoInfo.mDefaultLumaCount = list.size();
                this.mAlgoInfo.mESCW = "[0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0]";
                this.mAlgoInfo.mESCWCount = 1;
                this.mAlgoInfo.mFirstInital = 1;
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to call hwbrightness error:");
                stringBuilder2.append(e.getMessage());
                DElog.i(str, stringBuilder2.toString());
                return -1;
            }
        }
        if (this.mAlgoInfo.mLowLumaCount == 35 && this.mAlgoInfo.mMedialLumaCount == 35 && this.mAlgoInfo.mHighLumaCount == 35 && this.mAlgoInfo.mDefaultLumaCount == 35 && this.mAlgoInfo.mESCWCount == 1) {
            this.mAlgoInfo.mLowLumaSize = this.mAlgoInfo.mBLCurveTypeLowLuma.length();
            this.mAlgoInfo.mMedialLumaSize = this.mAlgoInfo.mBLCurveTypeMedialLuma.length();
            this.mAlgoInfo.mHighLumaSize = this.mAlgoInfo.mBLCurveTypeHighLuma.length();
            this.mAlgoInfo.mDefaultLumaSize = this.mAlgoInfo.mBLCurveTypeDefaultLuma.length();
            this.mAlgoInfo.mESCWSize = this.mAlgoInfo.mESCW.length();
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mDragInfo: ");
            stringBuilder3.append(this.mAlgoInfo.mDragInfo);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mDragSize: ");
            stringBuilder3.append(this.mAlgoInfo.mDragSize);
            stringBuilder3.append(", mDragCount ");
            stringBuilder3.append(this.mAlgoInfo.mDragCount);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mBLCurveTypeLowLuma: ");
            stringBuilder3.append(this.mAlgoInfo.mBLCurveTypeLowLuma);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mLowLumaCount: ");
            stringBuilder3.append(this.mAlgoInfo.mLowLumaCount);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mBLCurveTypeMedialLuma: ");
            stringBuilder3.append(this.mAlgoInfo.mBLCurveTypeMedialLuma);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mMedialLumaCount: ");
            stringBuilder3.append(this.mAlgoInfo.mMedialLumaCount);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mBLCurveTypeHighLuma: ");
            stringBuilder3.append(this.mAlgoInfo.mBLCurveTypeHighLuma);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mHighLumaCount: ");
            stringBuilder3.append(this.mAlgoInfo.mHighLumaCount);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mBLCurveTypeDefaultLuma: ");
            stringBuilder3.append(this.mAlgoInfo.mBLCurveTypeDefaultLuma);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mDefaultLumaCount: ");
            stringBuilder3.append(this.mAlgoInfo.mDefaultLumaCount);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mESCW: ");
            stringBuilder3.append(this.mAlgoInfo.mESCW);
            DElog.i(str2, stringBuilder3.toString());
            str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("input mESCWCount: ");
            stringBuilder3.append(this.mAlgoInfo.mESCWCount);
            DElog.i(str2, stringBuilder3.toString());
            synchronized (this.mLockJNI) {
                int ret = DisplayEngineLibraries.nativeInitAlgorithm(1);
                if (ret >= 0) {
                    this.mHandle = ret;
                } else {
                    this.mHandle = -1;
                    DElog.e(TAG, "nativeInitAlgorithm failed! ");
                    return -1;
                }
            }
        }
        DElog.i(TAG, "Count is not correct ! ");
        String str3 = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("LowLumaCount = ");
        stringBuilder4.append(this.mAlgoInfo.mLowLumaCount);
        DElog.i(str3, stringBuilder4.toString());
        str3 = TAG;
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("MedialLumaCount = ");
        stringBuilder4.append(this.mAlgoInfo.mMedialLumaCount);
        DElog.i(str3, stringBuilder4.toString());
        str3 = TAG;
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("HighLumaCount = ");
        stringBuilder4.append(this.mAlgoInfo.mHighLumaCount);
        DElog.i(str3, stringBuilder4.toString());
        str3 = TAG;
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("DefaultLumaCount = ");
        stringBuilder4.append(this.mAlgoInfo.mDefaultLumaCount);
        DElog.i(str3, stringBuilder4.toString());
        str3 = TAG;
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("ESCWCount = ");
        stringBuilder4.append(this.mAlgoInfo.mESCWCount);
        DElog.i(str3, stringBuilder4.toString());
        return -1;
    }

    /* JADX WARNING: Missing block: B:14:0x0036, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int abortTraining() {
        synchronized (this.mLockJNI) {
            if (this.mHandle == -1) {
                DElog.i(TAG, "abortTraining: PROCESS_TRAINING_ENGINE is not running.");
                return 0;
            } else if (DisplayEngineLibraries.nativeProcessAlgorithm(1, this.mHandle, 5, null, null) != 0) {
                DElog.e(TAG, " ABORT_TRAINING failed");
                if (DisplayEngineLibraries.nativeProcessAlgorithm(1, this.mHandle, 2, null, null) != 0) {
                    DElog.e(TAG, " DESTROY_TRAINING_ENGINE failed");
                }
            } else {
                return 0;
            }
        }
    }
}
