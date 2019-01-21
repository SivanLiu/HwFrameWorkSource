package com.huawei.displayengine;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;
import com.huawei.displayengine.DisplayEngineDBManager.DataCleanerKey;
import com.huawei.displayengine.DisplayEngineDBManager.DragInformationKey;
import com.huawei.displayengine.DisplayEngineDataCleanerXMLLoader.Data;
import com.huawei.displayengine.IDisplayEngineServiceEx.Stub;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DisplayEngineDataCleaner {
    private static float COMFORT_ZONE_COUNTER_WEIGHT = 0.2f;
    private static float COUNTER_WEIGHT_THRES = 0.5f;
    private static boolean DEBUG = false;
    private static float OUTLIER_ZONE_COUNTER_WEIGHT = 0.5f;
    private static final int RANGE_FLAG_COMFORT = 1;
    private static final int RANGE_FLAG_DEFAULT = 0;
    private static final int RANGE_FLAG_INVALID = -1;
    private static final int RANGE_FLAG_OUTLIER = 3;
    private static final int RANGE_FLAG_SAFE = 2;
    private static float SAFE_ZONE_COUNTER_WEIGHT = 0.3f;
    private static final String TAG = "DE J DisplayEngineDataCleaner";
    private static int THRES_AL_DARK = 10;
    private static int THRES_HBM = 3000;
    private static final String XML_DIR = "/product/etc/display/effect/displayengine/";
    private static final String XML_EXT = ".xml";
    private static final String XML_FILENAME = "DataCleanerConfig.xml";
    private static final String XML_FILENAME_WITHOUT_EXT = "DataCleanerConfig";
    private static ArrayList<Integer> mAmbientLightLUT;
    private static ArrayList<Float> mBrightnessLevelLUT;
    private static ArrayList<Integer> mDarkLevelLUT;
    private static ArrayList<Float> mDarkLevelRoofLUT;
    private static volatile DisplayEngineDataCleaner mInstance;
    private static Object mLock = new Object();
    private static int mOutdoorLevelFloor;
    private static Data mParameters = DisplayEngineDataCleanerXMLLoader.getData(getXmlFileName());
    private Context mContext;

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
        setParameters();
    }

    private DisplayEngineDataCleaner(Context context) {
        this.mContext = context;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Default ambient curve: ");
        stringBuilder.append(Arrays.toString(mAmbientLightLUT.toArray()));
        DElog.v(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Default brightness curve: ");
        stringBuilder.append(Arrays.toString(mBrightnessLevelLUT.toArray()));
        DElog.v(str, stringBuilder.toString());
    }

    public static DisplayEngineDataCleaner getInstance(Context context) {
        if (mInstance == null) {
            synchronized (mLock) {
                if (mInstance == null) {
                    mInstance = new DisplayEngineDataCleaner(context);
                    DElog.d(TAG, "DisplayEngineDataCleaner initialized.");
                }
            }
        }
        return mInstance;
    }

    public ArrayList<Bundle> cleanData(ArrayList<Bundle> records, int userId) {
        DElog.d(TAG, "enter cleanData func.");
        StringBuilder stringBuilder;
        if (userId < 0 || records == null || records.isEmpty()) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("clean data error! userId=");
            stringBuilder.append(userId);
            DElog.i(str, stringBuilder.toString());
            return null;
        }
        DElog.v(TAG, "start to  cleanData");
        int rangeFlag = calculateRangeFlag(records, userId);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("rangeFlag=");
        stringBuilder2.append(rangeFlag);
        DElog.d(str2, stringBuilder2.toString());
        if (rangeFlag == 0) {
            return null;
        }
        String str3 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("raw records size:");
        stringBuilder.append(records.size());
        DElog.v(str3, stringBuilder.toString());
        int curInd = 0;
        while (curInd < records.size()) {
            Bundle data = (Bundle) records.get(curInd);
            if (data != null) {
                if (data.getInt(DragInformationKey.GAMESTATE) == 1) {
                    records.remove(curInd);
                    curInd--;
                    String str4 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("records index ");
                    stringBuilder3.append(curInd);
                    stringBuilder3.append(" removed because of game state == 1");
                    DElog.i(str4, stringBuilder3.toString());
                } else {
                    float orgBrightnessLevel = getOriginalBrightnessLevel(data.getInt("AmbientLight"));
                    if (Float.compare(orgBrightnessLevel, 0.0f) > 0) {
                        data = cleanDataWithHumanFactorPolicy(data, rangeFlag, orgBrightnessLevel);
                        if (data != null) {
                            data = cleanDataWithDarkEnvironmentPolicy(data, orgBrightnessLevel);
                        }
                        if (data != null) {
                            data = cleanDataWithHighBrightnessEnvironmentPolicy(data);
                        }
                    } else {
                        String str5 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("orgBrightnessLevel=");
                        stringBuilder4.append(orgBrightnessLevel);
                        DElog.v(str5, stringBuilder4.toString());
                    }
                }
            }
            curInd++;
        }
        str3 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("cleaned records size:");
        stringBuilder.append(records.size());
        DElog.v(str3, stringBuilder.toString());
        return records;
    }

    private int calculateRangeFlag(List<Bundle> records, int userId) {
        List list = records;
        int i = userId;
        int ind = 0;
        if (i < 0 || list == null || records.isEmpty()) {
            return 0;
        }
        int previousRangeFlag = getRangeFlagByUserId(i);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("previoudRangeFlag=");
        stringBuilder.append(previousRangeFlag);
        DElog.v(str, stringBuilder.toString());
        int safeZoneCounter = 0;
        int comfortZoneCounter = 0;
        int outlierZoneCounter = 0;
        float weightedCounter = 0.0f;
        int rangeFlag = 0;
        DisplayEngineDBManager dbManager = DisplayEngineDBManager.getInstance(this.mContext);
        if (dbManager == null) {
            return 0;
        }
        int rangeFlag2;
        String str2;
        StringBuilder stringBuilder2;
        float orgBrightnessLevel;
        String str3;
        StringBuilder stringBuilder3;
        while (ind < records.size()) {
            float weightedCounter2;
            Bundle data = (Bundle) list.get(ind);
            boolean isCovered = data.getBoolean(DragInformationKey.PROXIMITYPOSITIVE);
            if (isCovered) {
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("isCovered=");
                stringBuilder4.append(isCovered);
                DElog.d(str4, stringBuilder4.toString());
                weightedCounter2 = weightedCounter;
                rangeFlag2 = rangeFlag;
            } else {
                float startPoint = data.getFloat(DragInformationKey.STARTPOINT);
                float stopPoint = data.getFloat(DragInformationKey.STOPPOINT);
                int ambientLight = data.getInt("AmbientLight");
                str2 = TAG;
                weightedCounter2 = weightedCounter;
                stringBuilder2 = new StringBuilder();
                rangeFlag2 = rangeFlag;
                stringBuilder2.append("startPoint=");
                stringBuilder2.append(startPoint);
                stringBuilder2.append(" stopPoint=");
                stringBuilder2.append(stopPoint);
                stringBuilder2.append(" ambientLight=");
                stringBuilder2.append(ambientLight);
                DElog.v(str2, stringBuilder2.toString());
                orgBrightnessLevel = getOriginalBrightnessLevel(ambientLight);
                str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("orgBrightnessLevel=");
                stringBuilder3.append(orgBrightnessLevel);
                DElog.v(str3, stringBuilder3.toString());
                if (previousRangeFlag == 0) {
                    if (isDataInComfortZone(stopPoint, orgBrightnessLevel)) {
                        comfortZoneCounter++;
                    } else if (isDataInSafeZone(stopPoint, orgBrightnessLevel)) {
                        safeZoneCounter++;
                    } else {
                        outlierZoneCounter++;
                    }
                } else if (previousRangeFlag != 1) {
                    str3 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("previousRangeFlag=");
                    stringBuilder3.append(previousRangeFlag);
                    DElog.v(str3, stringBuilder3.toString());
                } else if (!isDataInComfortZone(stopPoint, orgBrightnessLevel)) {
                    if (isDataInSafeZone(stopPoint, orgBrightnessLevel)) {
                        safeZoneCounter++;
                    } else {
                        outlierZoneCounter++;
                    }
                }
            }
            ind++;
            weightedCounter = weightedCounter2;
            rangeFlag = rangeFlag2;
            List<Bundle> list2 = records;
        }
        rangeFlag2 = rangeFlag;
        str2 = TAG;
        StringBuilder stringBuilder5 = new StringBuilder();
        stringBuilder5.append("comfortZoneCounter=");
        stringBuilder5.append(comfortZoneCounter);
        stringBuilder5.append(" safeZoneCounter=");
        stringBuilder5.append(safeZoneCounter);
        stringBuilder5.append(" outlierZoneCounter=");
        stringBuilder5.append(outlierZoneCounter);
        DElog.d(str2, stringBuilder5.toString());
        orgBrightnessLevel = getWeightedCounter(comfortZoneCounter, safeZoneCounter, outlierZoneCounter);
        String str5 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("weightedCounter=");
        stringBuilder2.append(orgBrightnessLevel);
        stringBuilder2.append(" COUNTER_WEIGHT_THRES=");
        stringBuilder2.append(COUNTER_WEIGHT_THRES);
        DElog.d(str5, stringBuilder2.toString());
        if (Float.compare(orgBrightnessLevel, COUNTER_WEIGHT_THRES) > 0) {
            ind = 2;
        } else {
            ind = 1;
        }
        str3 = TAG;
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("rangeFlag=");
        stringBuilder3.append(ind);
        DElog.d(str3, stringBuilder3.toString());
        Bundle data2 = new Bundle();
        data2.putInt("UserID", i);
        data2.putInt(DataCleanerKey.RANGEFLAG, ind);
        data2.putLong("TimeStamp", System.currentTimeMillis());
        dbManager.addorUpdateRecord("DataCleaner", data2);
        return ind;
    }

    private int getRangeFlagByUserId(int userId) {
        int rangeFlag = 0;
        if (userId < 0) {
            return 0;
        }
        List<Bundle> records = DisplayEngineDBManager.getInstance(this.mContext).getAllRecords("DataCleaner", new Bundle());
        if (records != null) {
            for (int curInd = 0; curInd < records.size(); curInd++) {
                Bundle data = (Bundle) records.get(curInd);
                if (userId == data.getInt("UserID")) {
                    rangeFlag = data.getInt(DataCleanerKey.RANGEFLAG);
                    break;
                }
            }
        }
        return rangeFlag;
    }

    private float getOriginalBrightnessLevel(int currentLight) {
        int ambientLutSize = mAmbientLightLUT.size();
        if (mAmbientLightLUT.isEmpty() || mBrightnessLevelLUT.isEmpty() || ambientLutSize != mBrightnessLevelLUT.size()) {
            DElog.i(TAG, "getOriginalBrightnessLevel invalid input!");
            return -1.0f;
        }
        float orgBrightnessLevel;
        int ind = 0;
        while (ind < ambientLutSize && ((Integer) mAmbientLightLUT.get(ind)).intValue() <= currentLight) {
            ind++;
        }
        if (ind >= 1 && ind <= ambientLutSize - 1) {
            int a = ((Integer) mAmbientLightLUT.get(ind - 1)).intValue();
            float alpha = ((float) (currentLight - a)) / ((float) (((Integer) mAmbientLightLUT.get(ind)).intValue() - a));
            orgBrightnessLevel = (((Float) mBrightnessLevelLUT.get(ind)).floatValue() * alpha) + ((1.0f - alpha) * ((Float) mBrightnessLevelLUT.get(ind - 1)).floatValue());
        } else if (ind == 0) {
            orgBrightnessLevel = ((Float) mBrightnessLevelLUT.get(0)).floatValue();
        } else if (ind > ambientLutSize - 1) {
            orgBrightnessLevel = ((Float) mBrightnessLevelLUT.get(ambientLutSize - 1)).floatValue();
        } else {
            DElog.i(TAG, "Invalid ind!");
            orgBrightnessLevel = -1.0f;
        }
        return orgBrightnessLevel;
    }

    private boolean isDataInComfortZone(float stopPoint, float originalBrightnessLevel) {
        boolean z = false;
        if (Float.compare(stopPoint, 0.0f) < 0 || Float.compare(originalBrightnessLevel, 0.0f) < 0) {
            return false;
        }
        float comfortRoof = originalBrightnessLevel * 1.5f;
        if (stopPoint >= originalBrightnessLevel / 1.5f && stopPoint <= comfortRoof) {
            z = true;
        }
        return z;
    }

    private boolean isDataInSafeZone(float stopPoint, float originalBrightnessLevel) {
        boolean z = false;
        if (Float.compare(stopPoint, 0.0f) < 0 || Float.compare(originalBrightnessLevel, 0.0f) < 0) {
            return false;
        }
        float safeRoof = originalBrightnessLevel * 3.0f;
        if (stopPoint >= originalBrightnessLevel / 3.0f && stopPoint <= safeRoof) {
            z = true;
        }
        return z;
    }

    private float getWeightedCounter(int comfortZoneCounter, int safeZoneCounter, int outlierZoneCounter) {
        int sumCounter = (comfortZoneCounter + safeZoneCounter) + outlierZoneCounter;
        if (sumCounter == 0) {
            return 0.0f;
        }
        return (((COMFORT_ZONE_COUNTER_WEIGHT * ((float) comfortZoneCounter)) + (SAFE_ZONE_COUNTER_WEIGHT * ((float) safeZoneCounter))) + (OUTLIER_ZONE_COUNTER_WEIGHT * ((float) outlierZoneCounter))) / ((float) sumCounter);
    }

    private static void setParameters() {
        COMFORT_ZONE_COUNTER_WEIGHT = mParameters.comfortZoneCounterWeight;
        SAFE_ZONE_COUNTER_WEIGHT = mParameters.safeZoneCounterWeight;
        OUTLIER_ZONE_COUNTER_WEIGHT = mParameters.outlierZoneCounterWeight;
        COUNTER_WEIGHT_THRES = mParameters.counterWeightThresh;
        THRES_AL_DARK = mParameters.alDarkThresh;
        THRES_HBM = mParameters.hbmTresh;
        mOutdoorLevelFloor = mParameters.outDoorLevelFloor;
        mAmbientLightLUT = mParameters.ambientLightLUT;
        mBrightnessLevelLUT = mParameters.brightnessLevelLUT;
        mDarkLevelLUT = mParameters.darkLevelLUT;
        mDarkLevelRoofLUT = mParameters.darkLevelRoofLUT;
    }

    private Bundle cleanDataWithHumanFactorPolicy(Bundle rawData, int rangeFlag, float orgBrightnessLevel) {
        if (rawData == null) {
            return null;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startPoint=");
        stringBuilder.append(rawData.getFloat(DragInformationKey.STARTPOINT));
        stringBuilder.append(" stopPoint=");
        stringBuilder.append(rawData.getFloat(DragInformationKey.STOPPOINT));
        DElog.d(str, stringBuilder.toString());
        if (rangeFlag == 2) {
            rawData.putFloat(DragInformationKey.STOPPOINT, clamp(3.0f * orgBrightnessLevel, orgBrightnessLevel / 3.0f, rawData.getFloat(DragInformationKey.STOPPOINT)));
        } else if (rangeFlag == 1) {
            rawData.putFloat(DragInformationKey.STOPPOINT, clamp(1.5f * orgBrightnessLevel, orgBrightnessLevel / 1.5f, rawData.getFloat(DragInformationKey.STOPPOINT)));
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("cleaned stopPoint=");
        stringBuilder.append(rawData.getFloat(DragInformationKey.STOPPOINT));
        DElog.d(str, stringBuilder.toString());
        return rawData;
    }

    private Bundle cleanDataWithDarkEnvironmentPolicy(Bundle rawData, float orgBrightnessLevel) {
        if (rawData == null) {
            DElog.i(TAG, "rawData is null.");
            return null;
        }
        int ambientLight = rawData.getInt("AmbientLight");
        float stopPoint = rawData.getFloat(DragInformationKey.STOPPOINT);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cleanDataWithDarkEnvironmentPolicy stopPoint=");
        stringBuilder.append(stopPoint);
        stringBuilder.append(" ambientLight=");
        stringBuilder.append(ambientLight);
        DElog.v(str, stringBuilder.toString());
        if (ambientLight >= 0 && ambientLight < THRES_AL_DARK) {
            int index = mDarkLevelLUT.indexOf(Integer.valueOf((int) Math.floor((double) orgBrightnessLevel)));
            if (index >= 0) {
                stopPoint = Float.compare(stopPoint, ((Float) mDarkLevelRoofLUT.get(index)).floatValue()) > 0 ? ((Float) mDarkLevelRoofLUT.get(index)).floatValue() : stopPoint;
                rawData.putFloat(DragInformationKey.STOPPOINT, stopPoint);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("cleanDataWithDarkEnvironmentPolicy new stopPoint=");
                stringBuilder2.append(stopPoint);
                stringBuilder2.append(" roof=");
                stringBuilder2.append(mDarkLevelRoofLUT.get(index));
                DElog.v(str2, stringBuilder2.toString());
            }
        }
        return rawData;
    }

    private Bundle cleanDataWithHighBrightnessEnvironmentPolicy(Bundle rawData) {
        if (rawData == null) {
            DElog.i(TAG, "rawData is null.");
            return null;
        }
        int ambientLight = rawData.getInt("AmbientLight");
        float stopPoint = rawData.getFloat(DragInformationKey.STOPPOINT);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cleanDataWithHighBrightnessEnvironmentPolicy stopPoint=");
        stringBuilder.append(stopPoint);
        DElog.v(str, stringBuilder.toString());
        if (ambientLight >= 0 && ambientLight >= THRES_HBM) {
            stopPoint = Float.compare(stopPoint, (float) mOutdoorLevelFloor) > 0 ? stopPoint : (float) mOutdoorLevelFloor;
            rawData.putFloat(DragInformationKey.STOPPOINT, stopPoint);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("cleanDataWithHighBrightnessEnvironmentPolicy new stopPoint=");
            stringBuilder.append(stopPoint);
            DElog.v(str, stringBuilder.toString());
        }
        return rawData;
    }

    private float clamp(float roof, float floor, float value) {
        float ret = value;
        if (Float.compare(ret, roof) > 0) {
            ret = roof;
        }
        if (Float.compare(ret, floor) < 0) {
            return floor;
        }
        return ret;
    }

    private static String getXmlFileName() {
        String panelName = getLcdPanelName();
        String panelVersion = getVersionFromLCD();
        String xmlPathWithPanelName = String.format("%s%s_%s%s", new Object[]{XML_DIR, XML_FILENAME_WITHOUT_EXT, panelName, XML_EXT});
        String xmlPathWithoutPanelName = String.format("%s%s", new Object[]{XML_DIR, XML_FILENAME});
        String xmlPathWithPanelNameAndPanelVersion = String.format("%s%s_%s_%s%s", new Object[]{XML_DIR, XML_FILENAME_WITHOUT_EXT, panelName, panelVersion, XML_EXT});
        File xmlFileWithPanelName = new File(xmlPathWithPanelName);
        File xmlFileWithoutPanelName = new File(xmlPathWithoutPanelName);
        if (new File(xmlPathWithPanelNameAndPanelVersion).exists()) {
            return xmlPathWithPanelNameAndPanelVersion;
        }
        if (xmlFileWithPanelName.exists()) {
            return xmlPathWithPanelName;
        }
        if (xmlFileWithoutPanelName.exists()) {
            return xmlPathWithoutPanelName;
        }
        Slog.i(TAG, "DataCleanerConfig.xml missing.");
        return null;
    }

    private static String getVersionFromLCD() {
        IBinder binder = ServiceManager.getService(DisplayEngineManager.SERVICE_NAME);
        String panelVersion = null;
        if (binder == null) {
            Slog.i(TAG, "getLcdPanelName() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx mService = Stub.asInterface(binder);
        if (mService == null) {
            Slog.e(TAG, "getLcdPanelName() mService is null!");
            return null;
        }
        byte[] name = new byte[32];
        String key;
        try {
            int ret = mService.getEffect(14, 3, name, name.length);
            String str;
            StringBuilder stringBuilder;
            if (ret != 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getLcdPanelName() getEffect failed! ret=");
                stringBuilder.append(ret);
                Slog.e(str, stringBuilder.toString());
                return null;
            }
            try {
                str = new String(name, "UTF-8").trim();
                key = "VER:";
                int index = str.indexOf(key);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getVersionFromLCD() index=");
                stringBuilder2.append(index);
                stringBuilder2.append(",lcdVersion=");
                stringBuilder2.append(str);
                Slog.i(str2, stringBuilder2.toString());
                if (index != -1) {
                    panelVersion = str.substring(key.length() + index);
                }
            } catch (UnsupportedEncodingException e) {
                Slog.e(TAG, "Unsupported encoding type!");
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getVersionFromLCD() panelVersion=");
            stringBuilder.append(panelVersion);
            Slog.i(str, stringBuilder.toString());
            return panelVersion;
        } catch (RemoteException e2) {
            key = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getLcdPanelName() RemoteException ");
            stringBuilder3.append(e2);
            Slog.e(key, stringBuilder3.toString());
            return null;
        }
    }

    private static String getLcdPanelName() {
        IBinder binder = ServiceManager.getService(DisplayEngineManager.SERVICE_NAME);
        String panelName = null;
        if (binder == null) {
            Slog.i(TAG, "getLcdPanelName() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx mService = Stub.asInterface(binder);
        if (mService == null) {
            Slog.e(TAG, "getLcdPanelName() mService is null!");
            return null;
        }
        byte[] name = new byte[256];
        int ret = 0;
        try {
            int ret2 = mService.getEffect(14, 0, name, name.length);
            if (ret2 != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getLcdPanelName() getEffect failed! ret=");
                stringBuilder.append(ret2);
                Slog.e(str, stringBuilder.toString());
                return null;
            }
            try {
                panelName = new String(name, "UTF-8").trim().replace(' ', '_');
                panelName = panelName.replace('\'', '_');
            } catch (UnsupportedEncodingException e) {
                Slog.e(TAG, "Unsupported encoding type!");
            }
            return panelName;
        } catch (RemoteException e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getLcdPanelName() RemoteException ");
            stringBuilder2.append(e2);
            Slog.e(str2, stringBuilder2.toString());
            return null;
        }
    }
}
