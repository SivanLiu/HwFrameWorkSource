package com.android.server.display;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.IPowerManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Xml;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.huawei.android.location.activityrecognition.HwActivityChangedEvent;
import com.huawei.android.location.activityrecognition.HwActivityChangedExtendEvent;
import com.huawei.android.location.activityrecognition.HwActivityRecognition;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionEvent;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionExtendEvent;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionHardwareSink;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionServiceConnection;
import com.huawei.android.location.activityrecognition.HwEnvironmentChangedEvent;
import com.huawei.displayengine.DElog;
import com.huawei.displayengine.DisplayEngineDBManager;
import com.huawei.displayengine.DisplayEngineManager;
import com.huawei.msdp.devicestatus.DeviceStatusConstant;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwBrightnessSceneRecognition {
    private static final int BINDER_REBUILD_COUNT_MAX = 10;
    private static final String CONTROL_XML_FILE = "/display/effect/displayengine/LABC_SR_control.xml";
    private static final int DEFAULT_APP_TYPE = 3;
    private static final int GAME_ENTER = 21;
    private static final int GAME_EXIT = 20;
    private static final String TAG = "DE J HwBrightnessSceneRecognition";
    private HwActivityRecognitionHardwareSink hwArSink = new HwActivityRecognitionHardwareSink() {
        public void onActivityChanged(HwActivityChangedEvent activityChangedEvent) {
            StringBuffer bf = new StringBuffer();
            for (HwActivityRecognitionEvent event : activityChangedEvent.getActivityRecognitionEvents()) {
                String activityType = event.getActivity();
                int eventType = event.getEventType();
                bf.append(event.getActivity());
                bf.append(",");
                bf.append(HwBrightnessSceneRecognition.this.getAREventType(event.getEventType()));
                bf.append(",");
                bf.append(event.getTimestampNs());
                bf.append(",");
                HwBrightnessSceneRecognition.this.updateARStatus(activityType, eventType);
            }
            DElog.d(HwBrightnessSceneRecognition.TAG, bf.toString());
            HwBrightnessSceneRecognition.this.setARSceneToBLControllerIfNeeded();
        }

        public void onActivityExtendChanged(HwActivityChangedExtendEvent activityChangedExtendEvent) {
            DElog.d(HwBrightnessSceneRecognition.TAG, "onActivityExtendChanged .....");
        }

        public void onEnvironmentChanged(HwEnvironmentChangedEvent environmentChangedEvent) {
            DElog.d(HwBrightnessSceneRecognition.TAG, "onEnvironmentChanged .....");
        }
    };
    private Map<String, ARActivity> mARActivities;
    private int mARConfidenceTH = 30;
    private int mARMonitorConfidenceSampleMaxNum = 0;
    private int mARMonitorConfidenceSampleTimeStepMs = HwAPPQoEUtils.APP_TYPE_STREAMING;
    private boolean mARSceneEnable = false;
    private int mARScreenOnTimeTHMs = 200;
    private int mARWaitTimeMs = 10000;
    private int mBinderRebuildCount = 0;
    private HwActivityRecognitionServiceConnection mConnect = new HwActivityRecognitionServiceConnection() {
        public void onServiceConnected() {
            DElog.i(HwBrightnessSceneRecognition.TAG, "onServiceConnected()");
            HwBrightnessSceneRecognition.this.mIsARConnected = true;
            HwBrightnessSceneRecognition.this.getSupportedARActivities();
            HwBrightnessSceneRecognition.this.enableAR();
        }

        public void onServiceDisconnected() {
            DElog.i(HwBrightnessSceneRecognition.TAG, "onServiceDisconnected()");
            HwBrightnessSceneRecognition.this.mIsARConnected = false;
            HwBrightnessSceneRecognition.this.disableAR();
        }
    };
    private final Context mContext;
    private DisplayEngineDBManager mDBManager;
    private int mDBUserDragMaxSize = 100;
    private DisplayEffectMonitor mDisplayEffectMonitor;
    private DisplayEngineManager mDisplayEngineManager;
    private boolean mEnable = false;
    private boolean mGameBLSceneEnable = false;
    private Map<String, Integer> mGameBrightnessLevelWhiteList;
    private int mGameBrightnessState;
    private boolean mGameDESceneEnable = false;
    private int mGameHDRState;
    private Map<String, Integer> mGameHDRWhiteList;
    private HwActivityRecognition mHwActivityRecognition;
    boolean mIsARConnected = false;
    private volatile boolean mIsBinderBuilding = false;
    private boolean mIsScreenOn = true;
    private Object mLockBinderBuilding;
    private Object mLockService;
    private boolean mNeedNotifyGameCurveChange = false;
    private boolean mNeedNotifyPersonalizedCurveChange = false;
    private boolean mPersonalizedCurveEnable = false;
    private String mPkgName = "";
    private volatile IPowerManager mPowerManagerService;
    private volatile boolean mPowerManagerServiceInitialized = false;
    private volatile boolean mScreenOffStateCleanFlag = true;
    private long mScreenOnTimeMs;
    private Map<String, Integer> mTopApkBrightnessLevelWhiteList;
    private int mTopApkState = 3;
    private int mUserId = 0;

    private static class ARActivity {
        public final int mAction;
        public int mStatus = 2;

        public ARActivity(int action) {
            this.mAction = action;
        }
    }

    public class ARMonitorThread extends Thread {
        String mARTag;
        List mConfidenceSamples = new LinkedList();
        int mSampleMaxNum;
        long mSampleTimeStepMs;

        ARMonitorThread(String ARTag, long sampleTimeStepMs, int sampleMaxNum) {
            super("ARMonitorThread");
            this.mARTag = ARTag;
            this.mSampleTimeStepMs = sampleTimeStepMs;
            this.mSampleMaxNum = sampleMaxNum;
        }

        public void run() {
            DElog.i(HwBrightnessSceneRecognition.TAG, "ARMonitorThread: start sampling...");
            int i = 0;
            while (i < this.mSampleMaxNum) {
                try {
                    Thread.sleep(this.mSampleTimeStepMs);
                } catch (InterruptedException e) {
                    String str = HwBrightnessSceneRecognition.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ARThread Exception ");
                    stringBuilder.append(e);
                    DElog.i(str, stringBuilder.toString());
                }
                if (!HwBrightnessSceneRecognition.this.mIsScreenOn) {
                    DElog.d(HwBrightnessSceneRecognition.TAG, "ARMonitorThread breaked due to screen off");
                    break;
                }
                HwActivityChangedExtendEvent activityChangedEvent = HwBrightnessSceneRecognition.this.getCurrentActivityExtend();
                if (activityChangedEvent != null) {
                    for (HwActivityRecognitionExtendEvent event : activityChangedEvent.getActivityRecognitionExtendEvents()) {
                        if (event != null) {
                            String activityType = event.getActivity();
                            int confidence = event.getConfidence();
                            if (activityType != null && activityType.equals(this.mARTag)) {
                                String str2 = HwBrightnessSceneRecognition.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("ARMonitorThread, confidence ");
                                stringBuilder2.append(i);
                                stringBuilder2.append(" = ");
                                stringBuilder2.append(confidence);
                                DElog.d(str2, stringBuilder2.toString());
                                this.mConfidenceSamples.add(Short.valueOf((short) confidence));
                            }
                        }
                    }
                    if (this.mConfidenceSamples != null && this.mConfidenceSamples.size() < i + 1) {
                        HwBrightnessSceneRecognition.this.sendAbnormalActivityRecognitionToMonitor(this.mARTag, this.mConfidenceSamples);
                        DElog.d(HwBrightnessSceneRecognition.TAG, "ARMonitorThread breaked due to activity no longer exist.");
                        break;
                    }
                }
                i++;
            }
            DElog.i(HwBrightnessSceneRecognition.TAG, "ARMonitorThread: finished.");
        }
    }

    public static class ARTag {
        private static final String ACTIVITY_FAST_WALKING = "android.activity_recognition.fast_walking";
        private static final String ACTIVITY_IN_VEHICLE = "android.activity_recognition.in_vehicle";
        private static final String ACTIVITY_ON_BICYCLE = "android.activity_recognition.on_bicycle";
        private static final String ACTIVITY_ON_FOOT = "android.activity_recognition.on_foot";
        private static final String ACTIVITY_OUTDOOR = "android.activity_recognition.outdoor";
        private static final String ACTIVITY_RELATIVE_STILL = "android.activity_recognition.relative_still";
        private static final String ACTIVITY_RUNNING = "android.activity_recognition.running";
        private static final String ACTIVITY_STILL = "android.activity_recognition.still";
        private static final String ACTIVITY_TYPE_VE_HIGH_SPEED_RAIL = "android.activity_recognition.high_speed_rail";
        private static final String ACTIVITY_UNKNOWN = "android.activity_recognition.unknown";
        private static final String ACTIVITY_WALKING = "android.activity_recognition.walking";
        private static final long MIN_REPORT_TIME = 1000000000;
    }

    public class ARThread extends Thread {
        String mARTag;
        ARActivity mActivityOnQuest;
        int mConfidenceThreshold;
        long mWaitTimeMs;

        ARThread(String ARTag, ARActivity activity, long waitTime, int confidenceThreshold) {
            super("ARThread");
            this.mActivityOnQuest = activity;
            this.mWaitTimeMs = waitTime;
            this.mARTag = ARTag;
            this.mConfidenceThreshold = confidenceThreshold;
        }

        public void run() {
            String str = HwBrightnessSceneRecognition.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Wait ");
            stringBuilder.append(this.mWaitTimeMs);
            stringBuilder.append("ms to verify ");
            stringBuilder.append(this.mARTag);
            stringBuilder.append("with confidence threshold ");
            stringBuilder.append(this.mConfidenceThreshold);
            DElog.i(str, stringBuilder.toString());
            try {
                Thread.sleep(this.mWaitTimeMs);
            } catch (InterruptedException e) {
                String str2 = HwBrightnessSceneRecognition.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ARThread Exception ");
                stringBuilder2.append(e);
                DElog.i(str2, stringBuilder2.toString());
            }
            if (HwBrightnessSceneRecognition.this.mIsScreenOn) {
                HwActivityChangedExtendEvent activityChangedEvent = HwBrightnessSceneRecognition.this.getCurrentActivityExtend();
                if (activityChangedEvent != null) {
                    for (HwActivityRecognitionExtendEvent event : activityChangedEvent.getActivityRecognitionExtendEvents()) {
                        if (event != null) {
                            String activityType = event.getActivity();
                            int confidence = event.getConfidence();
                            if (activityType != null) {
                                String str3;
                                StringBuilder stringBuilder3;
                                if (!activityType.equals(this.mARTag) || confidence <= this.mConfidenceThreshold) {
                                    str3 = HwBrightnessSceneRecognition.TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("confidence < mConfidenceThreshold ");
                                    stringBuilder3.append(this.mConfidenceThreshold);
                                    stringBuilder3.append(", AR scene ignored.");
                                    DElog.d(str3, stringBuilder3.toString());
                                } else {
                                    str3 = HwBrightnessSceneRecognition.TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("confidence:");
                                    stringBuilder3.append(confidence);
                                    stringBuilder3.append(" > mConfidenceThreshold:");
                                    stringBuilder3.append(this.mConfidenceThreshold);
                                    DElog.i(str3, stringBuilder3.toString());
                                    HwBrightnessSceneRecognition.this.setTopApkLevelToBLControllerIfNeeded(this.mActivityOnQuest.mAction);
                                    HwBrightnessSceneRecognition.this.sendActivityRecognitionToMonitor(this.mARTag);
                                }
                            }
                        }
                    }
                }
            }
            DElog.i(HwBrightnessSceneRecognition.TAG, "ARThread: finished.");
        }
    }

    public static class FeatureTag {
        public static final String TAG_AR_SCENE = "ARScene";
        public static final String TAG_GAME_BRIGHTNESS_SCENE = "GameBrightnessScene";
        public static final String TAG_GAME_DE_SCENE = "GameDEScene";
        public static final String TAG_PERSONALIZED_CURVE = "PersonalizedCurve";
    }

    public HwBrightnessSceneRecognition(Context context) {
        this.mContext = context;
        this.mLockService = new Object();
        this.mLockBinderBuilding = new Object();
        this.mGameBrightnessLevelWhiteList = new HashMap();
        this.mGameHDRWhiteList = new HashMap();
        this.mTopApkBrightnessLevelWhiteList = new HashMap();
        this.mARActivities = new HashMap();
        this.mDisplayEngineManager = new DisplayEngineManager();
        getConfigParam();
        this.mDBManager = DisplayEngineDBManager.getInstance(this.mContext);
        this.mDisplayEffectMonitor = DisplayEffectMonitor.getInstance(this.mContext);
    }

    private void setDefaultConfigValue() {
    }

    private void printConfigValue() {
        String str;
        StringBuilder stringBuilder;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("printConfigValue: mEnable = ");
        stringBuilder2.append(this.mEnable);
        DElog.i(str2, stringBuilder2.toString());
        if (this.mGameBrightnessLevelWhiteList != null) {
            for (Entry<String, Integer> entry : this.mGameBrightnessLevelWhiteList.entrySet()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printConfigValue: mGameBrightnessLevelWhiteList = ");
                stringBuilder.append((String) entry.getKey());
                stringBuilder.append(", ");
                stringBuilder.append(entry.getValue());
                DElog.i(str, stringBuilder.toString());
            }
        } else {
            DElog.i(TAG, "printConfigValue: mGameBrightnessLevelWhiteList is null.");
        }
        if (this.mGameHDRWhiteList != null) {
            for (Entry<String, Integer> entry2 : this.mGameHDRWhiteList.entrySet()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printConfigValue: mGameHDRWhiteList = ");
                stringBuilder.append((String) entry2.getKey());
                stringBuilder.append(", ");
                stringBuilder.append(entry2.getValue());
                DElog.i(str, stringBuilder.toString());
            }
        } else {
            DElog.i(TAG, "printConfigValue: mGameHDRWhiteList is null.");
        }
        if (this.mTopApkBrightnessLevelWhiteList != null) {
            for (Entry<String, Integer> entry22 : this.mTopApkBrightnessLevelWhiteList.entrySet()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printConfigValue: mTopApkBrightnessLevelWhiteList = ");
                stringBuilder.append((String) entry22.getKey());
                stringBuilder.append(", ");
                stringBuilder.append(entry22.getValue());
                DElog.i(str, stringBuilder.toString());
            }
        } else {
            DElog.i(TAG, "printConfigValue: mTopApkBrightnessLevelWhiteList is null.");
        }
        if (this.mARActivities != null) {
            for (Entry<String, ARActivity> entry3 : this.mARActivities.entrySet()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printConfigValue: mARActivities = ");
                stringBuilder.append((String) entry3.getKey());
                stringBuilder.append(", ");
                stringBuilder.append(((ARActivity) entry3.getValue()).mAction);
                DElog.i(str, stringBuilder.toString());
            }
        } else {
            DElog.i(TAG, "printConfigValue: mARActivities is null.");
        }
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("printConfigValue: mARSceneEnable = ");
        stringBuilder2.append(this.mARSceneEnable);
        DElog.i(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("printConfigValue: mARScreenOnTimeTHMs = ");
        stringBuilder2.append(this.mARScreenOnTimeTHMs);
        DElog.i(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("printConfigValue: mARConfidenceTH = ");
        stringBuilder2.append(this.mARConfidenceTH);
        DElog.i(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("printConfigValue: mARScreenOnTimeTHMs = ");
        stringBuilder2.append(this.mARScreenOnTimeTHMs);
        DElog.i(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("printConfigValue: mARWaitTimeMs = ");
        stringBuilder2.append(this.mARWaitTimeMs);
        DElog.i(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("printConfigValue: mARMonitorConfidenceSampleMaxNum = ");
        stringBuilder2.append(this.mARMonitorConfidenceSampleMaxNum);
        DElog.i(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("printConfigValue: mARMonitorConfidenceSampleTimeStepMs = ");
        stringBuilder2.append(this.mARMonitorConfidenceSampleTimeStepMs);
        DElog.i(str2, stringBuilder2.toString());
    }

    private void getConfigParam() {
        try {
            if (!getConfig()) {
                DElog.e(TAG, "getConfig failed!");
                setDefaultConfigValue();
            }
            printConfigValue();
        } catch (IOException e) {
            DElog.e(TAG, "getConfig failed setDefaultConfigValue!");
            setDefaultConfigValue();
            printConfigValue();
        }
    }

    private boolean getConfig() throws IOException {
        String str;
        StringBuilder stringBuilder;
        IOException e;
        DElog.d(TAG, "getConfig");
        File xmlFile = HwCfgFilePolicy.getCfgFile(CONTROL_XML_FILE, 0);
        if (xmlFile == null) {
            DElog.w(TAG, "get xmlFile :/display/effect/displayengine/LABC_SR_control.xml failed!");
            return false;
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(xmlFile);
            if (getConfigFromXML(inputStream)) {
                inputStream.close();
                try {
                    inputStream.close();
                } catch (IOException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("get xmlFile error: ");
                    stringBuilder.append(e2);
                    DElog.e(str, stringBuilder.toString());
                }
                return true;
            }
            DElog.i(TAG, "get xmlFile error");
            inputStream.close();
            try {
                inputStream.close();
            } catch (IOException e22) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("get xmlFile error: ");
                stringBuilder.append(e22);
                DElog.e(str, stringBuilder.toString());
            }
            return false;
        } catch (RuntimeException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e3);
            DElog.e(str, stringBuilder.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    e22 = e4;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
            return false;
        } catch (FileNotFoundException e5) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e5);
            DElog.e(str, stringBuilder.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e6) {
                    e22 = e6;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
            return false;
        } catch (IOException e222) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e222);
            DElog.e(str, stringBuilder.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e7) {
                    e222 = e7;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
            return false;
        } catch (Exception e8) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e8);
            DElog.e(str, stringBuilder.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e9) {
                    e222 = e9;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
            return false;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e2222) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("get xmlFile error: ");
                    stringBuilder2.append(e2222);
                    DElog.e(TAG, stringBuilder2.toString());
                }
            }
        }
        stringBuilder.append("get xmlFile error: ");
        stringBuilder.append(e2222);
        DElog.e(str, stringBuilder.toString());
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:75:0x0227 A:{Catch:{ XmlPullParserException -> 0x0277, IOException -> 0x025f, NumberFormatException -> 0x0247, Exception -> 0x022f }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getConfigFromXML(InputStream inStream) {
        String str;
        StringBuilder stringBuilder;
        DElog.d(TAG, "getConfigFromeXML");
        boolean configGroupLoadStarted = false;
        boolean loadFinished = false;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                switch (eventType) {
                    case 2:
                        String name = parser.getName();
                        if (!name.equals("LABCSRControl")) {
                            if (!name.equals("Enable")) {
                                if (!name.equals("DBUserDragMaxSize")) {
                                    String[] values;
                                    String str2;
                                    StringBuilder stringBuilder2;
                                    if (!name.equals("GameBrightnessLevelWhiteList")) {
                                        if (!name.equals("GameHDRWhiteList")) {
                                            if (!name.equals("TopApkBrightnessLevelWhiteList")) {
                                                if (!name.equals("GameDESceneEnable")) {
                                                    if (!name.equals("GameBLSceneEnable")) {
                                                        if (!name.equals("PersonalizedCurveEnable")) {
                                                            if (!name.equals("ARSceneEnable")) {
                                                                if (!name.equals("ARActivity")) {
                                                                    if (!name.equals("ARScreenOnTimeTHMs")) {
                                                                        if (!name.equals("ARConfidenceTH")) {
                                                                            if (!name.equals("ARWaitTimeMs")) {
                                                                                if (!name.equals("ARMonitorConfidenceSampleMaxNum")) {
                                                                                    if (name.equals("ARMonitorConfidenceSampleTimeStepMs")) {
                                                                                        this.mARMonitorConfidenceSampleTimeStepMs = Integer.parseInt(parser.nextText());
                                                                                        break;
                                                                                    }
                                                                                }
                                                                                this.mARMonitorConfidenceSampleMaxNum = Integer.parseInt(parser.nextText());
                                                                                break;
                                                                            }
                                                                            this.mARWaitTimeMs = Integer.parseInt(parser.nextText());
                                                                            break;
                                                                        }
                                                                        this.mARConfidenceTH = Integer.parseInt(parser.nextText());
                                                                        break;
                                                                    }
                                                                    this.mARScreenOnTimeTHMs = Integer.parseInt(parser.nextText());
                                                                    break;
                                                                }
                                                                values = parser.nextText().split(",");
                                                                if (values.length == 2) {
                                                                    this.mARActivities.put(values[0], new ARActivity(Integer.parseInt(values[1])));
                                                                    break;
                                                                }
                                                                str2 = TAG;
                                                                stringBuilder2 = new StringBuilder();
                                                                stringBuilder2.append("getConfigFromXML find illegal param, tag name = ");
                                                                stringBuilder2.append(name);
                                                                DElog.d(str2, stringBuilder2.toString());
                                                                break;
                                                            }
                                                            this.mARSceneEnable = Boolean.parseBoolean(parser.nextText());
                                                            break;
                                                        }
                                                        this.mPersonalizedCurveEnable = Boolean.parseBoolean(parser.nextText());
                                                        break;
                                                    }
                                                    this.mGameBLSceneEnable = Boolean.parseBoolean(parser.nextText());
                                                    break;
                                                }
                                                this.mGameDESceneEnable = Boolean.parseBoolean(parser.nextText());
                                                break;
                                            }
                                            values = parser.nextText().split(",");
                                            if (values.length == 2) {
                                                this.mTopApkBrightnessLevelWhiteList.put(values[0], Integer.valueOf(Integer.parseInt(values[1])));
                                                break;
                                            }
                                            str2 = TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("getConfigFromXML find illegal param, tag name = ");
                                            stringBuilder2.append(name);
                                            DElog.d(str2, stringBuilder2.toString());
                                            break;
                                        }
                                        values = parser.nextText().split(",");
                                        if (values.length == 2) {
                                            this.mGameHDRWhiteList.put(values[0], Integer.valueOf(Integer.parseInt(values[1])));
                                            break;
                                        }
                                        str2 = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("getConfigFromXML find illegal param, tag name = ");
                                        stringBuilder2.append(name);
                                        DElog.d(str2, stringBuilder2.toString());
                                        break;
                                    }
                                    values = parser.nextText().split(",");
                                    if (values.length == 2) {
                                        this.mGameBrightnessLevelWhiteList.put(values[0], Integer.valueOf(Integer.parseInt(values[1])));
                                        break;
                                    }
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("getConfigFromXML find illegal param, tag name = ");
                                    stringBuilder2.append(name);
                                    DElog.d(str2, stringBuilder2.toString());
                                    break;
                                }
                                this.mDBUserDragMaxSize = Integer.parseInt(parser.nextText());
                                break;
                            }
                            this.mEnable = Boolean.parseBoolean(parser.nextText());
                            break;
                        }
                        configGroupLoadStarted = true;
                        break;
                        break;
                    case 3:
                        if (parser.getName().equals("LABCSRControl") && configGroupLoadStarted) {
                            loadFinished = true;
                            configGroupLoadStarted = false;
                            break;
                        }
                    default:
                        break;
                }
                if (loadFinished) {
                    if (loadFinished) {
                        DElog.i(TAG, "getConfigFromeXML success!");
                        return true;
                    }
                    DElog.e(TAG, "getConfigFromeXML false!");
                    return false;
                }
            }
            if (loadFinished) {
            }
        } catch (XmlPullParserException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e);
            DElog.e(str, stringBuilder.toString());
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e2);
            DElog.e(str, stringBuilder.toString());
        } catch (NumberFormatException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e3);
            DElog.e(str, stringBuilder.toString());
        } catch (Exception e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile error: ");
            stringBuilder.append(e4);
            DElog.e(str, stringBuilder.toString());
        }
        DElog.e(TAG, "getConfigFromeXML false!");
        return false;
    }

    public boolean isEnable() {
        return this.mEnable;
    }

    private boolean isFeatureEnable(String tag) {
        if (!this.mEnable) {
            return false;
        }
        boolean z = true;
        int hashCode = tag.hashCode();
        if (hashCode != -1081926945) {
            if (hashCode != -204695479) {
                if (hashCode == 2145497145 && tag.equals(FeatureTag.TAG_GAME_DE_SCENE)) {
                    z = false;
                }
            } else if (tag.equals(FeatureTag.TAG_GAME_BRIGHTNESS_SCENE)) {
                z = true;
            }
        } else if (tag.equals(FeatureTag.TAG_PERSONALIZED_CURVE)) {
            z = true;
        }
        switch (z) {
            case false:
                return this.mGameDESceneEnable;
            case true:
                return this.mGameBLSceneEnable;
            case true:
                return this.mPersonalizedCurveEnable;
            default:
                return false;
        }
    }

    private IPowerManager getPowerManagerService() throws RemoteException {
        if (this.mPowerManagerService == null && !this.mPowerManagerServiceInitialized) {
            synchronized (this.mLockService) {
                if (this.mPowerManagerService == null && !this.mPowerManagerServiceInitialized) {
                    buildBinder();
                    if (this.mPowerManagerService != null) {
                        this.mPowerManagerServiceInitialized = true;
                    }
                }
            }
        }
        if (this.mPowerManagerService != null || !this.mPowerManagerServiceInitialized) {
            return this.mPowerManagerService;
        }
        if (this.mBinderRebuildCount < 10) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Try to rebuild binder ");
            stringBuilder.append(this.mBinderRebuildCount);
            stringBuilder.append(" times.");
            throw new RemoteException(stringBuilder.toString());
        }
        throw new RemoteException("binder rebuilding failed!");
    }

    private void buildBinder() {
        IBinder binder = ServiceManager.getService("power");
        if (binder != null) {
            this.mPowerManagerService = Stub.asInterface(binder);
            if (this.mPowerManagerService == null) {
                DElog.w(TAG, "service is null!");
                return;
            }
            return;
        }
        this.mPowerManagerService = null;
        DElog.w(TAG, "binder is null!");
    }

    private void rebuildBinder() {
        DElog.i(TAG, "wait 800ms to rebuild binder...");
        SystemClock.sleep(800);
        DElog.i(TAG, "rebuild binder...");
        synchronized (this.mLockService) {
            buildBinder();
            if (this.mPowerManagerService != null) {
                DElog.i(TAG, "rebuild binder success.");
            } else {
                DElog.i(TAG, "rebuild binder failed!");
                this.mBinderRebuildCount++;
            }
        }
        synchronized (this.mLockBinderBuilding) {
            if (this.mBinderRebuildCount < 10) {
                this.mIsBinderBuilding = false;
            }
        }
    }

    private void rebuildBinderDelayed() {
        if (!this.mIsBinderBuilding) {
            synchronized (this.mLockBinderBuilding) {
                if (!this.mIsBinderBuilding) {
                    new Thread(new Runnable() {
                        public void run() {
                            HwBrightnessSceneRecognition.this.rebuildBinder();
                        }
                    }).start();
                    this.mIsBinderBuilding = true;
                }
            }
        }
    }

    public void initBootCompleteValues() {
        if (this.mDBManager != null) {
            this.mDBManager.setMaxSize("DragInfo", this.mDBUserDragMaxSize);
        }
        if (this.mARSceneEnable) {
            connectARService();
        }
    }

    public void notifyTopApkChange(String pkgName) {
        if (pkgName == null || pkgName.length() <= 0) {
            DElog.i(TAG, "pkgName is null || pkgName.length() <= 0!");
        }
        this.mPkgName = pkgName;
        updateTopApkSceneIfNeeded(pkgName);
        updateGameSceneIfNeeded(pkgName);
        int topApkLevel = -1;
        if (this.mNeedNotifyGameCurveChange) {
            this.mNeedNotifyGameCurveChange = false;
            if (this.mGameBrightnessState == 21) {
                setTopApkLevelToBLControllerIfNeeded(21);
                return;
            }
            topApkLevel = 20;
        }
        if (this.mNeedNotifyPersonalizedCurveChange) {
            this.mNeedNotifyPersonalizedCurveChange = false;
            topApkLevel = this.mTopApkState;
        }
        if (topApkLevel > 0) {
            setTopApkLevelToBLControllerIfNeeded(topApkLevel);
        }
    }

    public void notifyScreenStatus(final boolean isScreenOn) {
        new Thread(new Runnable() {
            public void run() {
                boolean lastState = HwBrightnessSceneRecognition.this.mIsScreenOn;
                HwBrightnessSceneRecognition.this.mIsScreenOn = isScreenOn;
                if (!HwBrightnessSceneRecognition.this.mIsScreenOn && lastState) {
                    HwBrightnessSceneRecognition.this.mScreenOffStateCleanFlag = true;
                }
                HwBrightnessSceneRecognition.this.mScreenOnTimeMs = SystemClock.uptimeMillis();
                if (!HwBrightnessSceneRecognition.this.isARConnected()) {
                    return;
                }
                if (isScreenOn) {
                    HwBrightnessSceneRecognition.this.enableAR();
                } else {
                    HwBrightnessSceneRecognition.this.disableAR();
                }
            }
        }).start();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyScreenStatus = ");
        stringBuilder.append(isScreenOn);
        DElog.i(str, stringBuilder.toString());
    }

    public void notifyAutoBrightnessAdj() {
        String str;
        if (isFeatureEnable(FeatureTag.TAG_PERSONALIZED_CURVE)) {
            int userId = this.mUserId;
            Bundle data = new Bundle();
            int retService = -1;
            try {
                IPowerManager service = getPowerManagerService();
                if (service != null) {
                    retService = service.hwBrightnessGetData("SceneRecognition", data);
                }
            } catch (RemoteException e) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hwBrightnessGetData(SceneRecognition) has remote exception:");
                stringBuilder.append(e.getMessage());
                DElog.e(str, stringBuilder.toString());
                rebuildBinderDelayed();
            }
            int retService2 = retService;
            if (retService2 == 0) {
                String str2;
                StringBuilder stringBuilder2;
                int startBrightness = data.getInt("StartBrightness");
                int endBrightness = data.getInt("EndBrightness");
                int alLux = data.getInt("FilteredAmbientLight");
                boolean proximityPositive = data.getBoolean("ProximityPositive");
                boolean isDeltaValid = data.getBoolean("DeltaValid");
                int i;
                if (startBrightness < 4 || endBrightness < 4 || startBrightness > 255) {
                    i = alLux;
                    retService2 = endBrightness;
                } else if (endBrightness > 255) {
                    int i2 = retService2;
                    i = alLux;
                    retService2 = endBrightness;
                } else if (alLux < 0) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("hwBrightnessGetData return invalid alLux, alLux = ");
                    stringBuilder2.append(alLux);
                    DElog.i(str2, stringBuilder2.toString());
                    return;
                } else if (isDeltaValid) {
                    long currentTimeMillis = System.currentTimeMillis();
                    str = this.mPkgName;
                    int i3 = this.mGameBrightnessState;
                    int i4 = i3;
                    retService2 = endBrightness;
                    writeDataBaseUserDrag(userId, currentTimeMillis, startBrightness, endBrightness, str, alLux, proximityPositive, i4, this.mTopApkState);
                    return;
                } else {
                    DElog.i(TAG, "hwBrightnessGetData return delta invalid");
                    return;
                }
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("hwBrightnessGetData return invalid brightness, startBrightness = ");
                stringBuilder2.append(startBrightness);
                stringBuilder2.append(", endBrightness = ");
                stringBuilder2.append(retService2);
                DElog.i(str2, stringBuilder2.toString());
                return;
            }
            DElog.w(TAG, "hwBrightnessGetData return false");
            return;
        }
        DElog.i(TAG, "notifyAutoBrightnessAdj returned, isFeatureEnable(FeatureTag.TAG_PERSONALIZED_CURVE) returned false");
    }

    public void notifyUserChange(int userId) {
        this.mUserId = userId;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyUserChange, new id = ");
        stringBuilder.append(this.mUserId);
        DElog.i(str, stringBuilder.toString());
    }

    private void updateGameSceneIfNeeded(String pkgName) {
        if (this.mIsScreenOn) {
            boolean screenOffCleanFlag = this.mScreenOffStateCleanFlag;
            this.mScreenOffStateCleanFlag = false;
            int lastGameHDRState = this.mGameHDRState;
            int lastGameBrightnessState = this.mGameBrightnessState;
            if (pkgName == null || pkgName.length() <= 0) {
                this.mGameBrightnessState = 20;
            } else {
                StringBuilder stringBuilder;
                String str;
                if (this.mGameBrightnessLevelWhiteList.get(pkgName) != null) {
                    this.mGameBrightnessState = 21;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("apk ");
                    stringBuilder.append(pkgName);
                    stringBuilder.append(" is in the game brightness whitelist, state = ");
                    stringBuilder.append(this.mGameBrightnessState);
                    DElog.d(str, stringBuilder.toString());
                } else {
                    this.mGameBrightnessState = 20;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("apk ");
                    stringBuilder.append(pkgName);
                    stringBuilder.append(" is NOT in the game brightness whitelist, state = ");
                    stringBuilder.append(this.mGameBrightnessState);
                    DElog.d(str, stringBuilder.toString());
                }
                String str2;
                if (this.mGameHDRWhiteList.get(pkgName) != null) {
                    this.mGameHDRState = 21;
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("apk ");
                    stringBuilder.append(pkgName);
                    stringBuilder.append(" is in the game hdr whitelist, state = ");
                    stringBuilder.append(this.mGameHDRState);
                    DElog.d(str2, stringBuilder.toString());
                } else {
                    this.mGameHDRState = 20;
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("apk ");
                    stringBuilder.append(pkgName);
                    stringBuilder.append(" is NOT in the game hdr whitelist, state = ");
                    stringBuilder.append(this.mGameHDRState);
                    DElog.d(str2, stringBuilder.toString());
                }
            }
            if (isFeatureEnable(FeatureTag.TAG_GAME_BRIGHTNESS_SCENE) && (this.mGameBrightnessState != lastGameBrightnessState || screenOffCleanFlag)) {
                this.mNeedNotifyGameCurveChange = true;
            }
            if (this.mDisplayEngineManager == null) {
                DElog.w(TAG, "mDisplayEngineManager is null !");
            } else if (isFeatureEnable(FeatureTag.TAG_GAME_DE_SCENE) && (this.mGameHDRState != lastGameHDRState || screenOffCleanFlag)) {
                if (this.mGameHDRState == 21) {
                    this.mDisplayEngineManager.setScene(36, 16);
                    DElog.d(TAG, "setScene DE_SCENE_GAME DE_ACTION_MODE_ON");
                } else {
                    this.mDisplayEngineManager.setScene(36, 17);
                    DElog.d(TAG, "setScene DE_SCENE_GAME DE_ACTION_MODE_OFF");
                }
            }
        }
    }

    private void updateTopApkSceneIfNeeded(String pkgName) {
        if (this.mIsScreenOn) {
            if (pkgName == null || pkgName.length() <= 0) {
                this.mTopApkState = 3;
            } else {
                Integer value = (Integer) this.mTopApkBrightnessLevelWhiteList.get(pkgName);
                String str;
                StringBuilder stringBuilder;
                if (value != null) {
                    this.mTopApkState = value.intValue();
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("apk ");
                    stringBuilder.append(pkgName);
                    stringBuilder.append(" is in the top apk whitelist, state = ");
                    stringBuilder.append(this.mTopApkState);
                    DElog.d(str, stringBuilder.toString());
                } else {
                    this.mTopApkState = 3;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("apk ");
                    stringBuilder.append(pkgName);
                    stringBuilder.append(" is NOT in the top apk whitelist, state = ");
                    stringBuilder.append(this.mTopApkState);
                    DElog.d(str, stringBuilder.toString());
                }
            }
            if (!isFeatureEnable(FeatureTag.TAG_PERSONALIZED_CURVE)) {
                DElog.d(TAG, "updateTopApkSceneIfNeeded returned, isFeatureEnable(FeatureTag.TAG_PERSONALIZED_CURVE) returned false");
            } else if (this.mDBManager != null && this.mDBManager.getSize("BrightnessCurveDefault", new Bundle()) > 0) {
                this.mNeedNotifyPersonalizedCurveChange = true;
            }
        }
    }

    private void setTopApkLevelToBLControllerIfNeeded(int topApkState) {
        Bundle data = new Bundle();
        data.putInt("TopApkLevel", topApkState);
        int retService = -1;
        String str;
        StringBuilder stringBuilder;
        try {
            IPowerManager service = getPowerManagerService();
            if (service != null) {
                retService = service.hwBrightnessSetData("PersonalizedBrightnessCurveLevel", data);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("setTopApkLevelToBLControllerIfNeeded, topApkState = ");
                stringBuilder.append(topApkState);
                DElog.i(str, stringBuilder.toString());
            }
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("hwBrightnessGetData(PersonalizedBrightnessCurveLevel) has remote exception:");
            stringBuilder.append(e.getMessage());
            DElog.e(str, stringBuilder.toString());
            rebuildBinderDelayed();
        }
        if (retService != 0) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("hwBrightnessGetData(PersonalizedBrightnessCurveLevel) returned ");
            stringBuilder2.append(retService);
            DElog.w(str2, stringBuilder2.toString());
        }
    }

    private void writeDataBaseUserDrag(int userId, long timeMillis, int startBrightness, int endBrightness, String pkgName, int alLux, boolean proximityPositive, int gameState, int topApkState) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("writeDataBaseUserDrag, userId = ");
        stringBuilder.append(userId);
        stringBuilder.append(", timeMillis = ");
        stringBuilder.append(timeMillis);
        stringBuilder.append(", startPoint = ");
        stringBuilder.append(startBrightness);
        stringBuilder.append(", endPoint = ");
        stringBuilder.append(endBrightness);
        stringBuilder.append(", pkgName = ");
        stringBuilder.append(pkgName);
        stringBuilder.append(", alLux = ");
        stringBuilder.append(alLux);
        stringBuilder.append(", proximityPositive = ");
        stringBuilder.append(proximityPositive);
        stringBuilder.append(", topApkState = ");
        stringBuilder.append(topApkState);
        stringBuilder.append(", gameState = ");
        stringBuilder.append(gameState);
        DElog.d(str, stringBuilder.toString());
        if (this.mDBManager != null) {
            Bundle data = new Bundle();
            data.putLong("TimeStamp", System.currentTimeMillis());
            data.putInt("UserID", userId);
            data.putFloat("StartPoint", (float) startBrightness);
            data.putFloat("StopPoint", (float) endBrightness);
            data.putInt("AmbientLight", alLux);
            data.putBoolean("ProximityPositive", proximityPositive);
            data.putInt("GameState", gameState == 21 ? 1 : -1);
            data.putInt("AppType", topApkState);
            data.putString("PackageName", pkgName);
            this.mDBManager.addorUpdateRecord("DragInfo", data);
        }
    }

    public void connectARService() {
        if (this.mContext == null) {
            DElog.w(TAG, "mContext is null! connect failed.");
            return;
        }
        this.mHwActivityRecognition = new HwActivityRecognition(this.mContext);
        DElog.i(TAG, "connectARService");
        this.mHwActivityRecognition.connectService(this.hwArSink, this.mConnect);
    }

    public void disconnectARService() {
        if (this.mHwActivityRecognition != null && this.mIsARConnected) {
            this.mHwActivityRecognition.disconnectService();
            this.mIsARConnected = false;
        }
    }

    public boolean isARConnected() {
        return this.mIsARConnected;
    }

    public boolean enableAR() {
        boolean retall = true;
        if (!this.mIsARConnected || this.mHwActivityRecognition == null || this.mARActivities == null) {
            DElog.i(TAG, "enableAR failed, mHwActivityRecognition == null || mARActivities == null!");
        } else {
            for (String key : this.mARActivities.keySet()) {
                boolean ret = enableARActivity(key, 1);
                boolean z = retall && ret;
                retall = z;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enableAR ");
                stringBuilder.append(key);
                stringBuilder.append(", ret = ");
                stringBuilder.append(ret);
                DElog.i(str, stringBuilder.toString());
            }
        }
        return retall;
    }

    private boolean enableARActivity(String activity, long reportTime) {
        if (this.mHwActivityRecognition == null) {
            return true;
        }
        long reportLatencyNs = DeviceStatusConstant.MIN_DELAY_TIME * reportTime;
        boolean z = true;
        boolean z2 = enableActivityEvent(activity, 1, reportLatencyNs) && 1 != null;
        boolean ret = z2;
        if (!(enableActivityEvent(activity, 2, reportLatencyNs) && ret)) {
            z = false;
        }
        return z;
    }

    public boolean disableAR() {
        boolean retall = true;
        if (!this.mIsARConnected || this.mHwActivityRecognition == null || this.mARActivities == null) {
            DElog.i(TAG, "disableAR failed, mHwActivityRecognition == null || mARActivities == null!");
        } else {
            for (Entry<String, ARActivity> entry : this.mARActivities.entrySet()) {
                String key = (String) entry.getKey();
                boolean ret = disableARActivity(key);
                boolean z = retall && ret;
                retall = z;
                ((ARActivity) entry.getValue()).mStatus = 2;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("disableARActivity ");
                stringBuilder.append(key);
                stringBuilder.append(", ret = ");
                stringBuilder.append(ret);
                DElog.i(str, stringBuilder.toString());
            }
        }
        return retall;
    }

    private boolean disableARActivity(String activity) {
        if (this.mHwActivityRecognition == null) {
            return true;
        }
        boolean z = true;
        boolean z2 = disableActivityEvent(activity, 1) && 1 != null;
        boolean ret = z2;
        if (!(disableActivityEvent(activity, 2) && ret)) {
            z = false;
        }
        return z;
    }

    private void updateARStatus(String activityType, int eventType) {
        if (this.mARActivities == null) {
            DElog.i(TAG, "mARActivities == null");
            return;
        }
        ARActivity activity = (ARActivity) this.mARActivities.get(activityType);
        String str;
        StringBuilder stringBuilder;
        if (activity != null) {
            activity.mStatus = eventType;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateARStatus, activityType:");
            stringBuilder.append(activityType);
            stringBuilder.append(" update status: ");
            stringBuilder.append(eventType);
            DElog.i(str, stringBuilder.toString());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateARStatus, activity == null! activityType == ");
            stringBuilder.append(activityType);
            stringBuilder.append(", eventType = ");
            stringBuilder.append(eventType);
            DElog.w(str, stringBuilder.toString());
        }
    }

    private void setARSceneToBLControllerIfNeeded() {
        for (Entry<String, ARActivity> entry : this.mARActivities.entrySet()) {
            ARActivity activity = (ARActivity) entry.getValue();
            String tag = (String) entry.getKey();
            if (activity.mStatus == 1) {
                if (SystemClock.uptimeMillis() - this.mScreenOnTimeMs > ((long) this.mARScreenOnTimeTHMs)) {
                    new ARThread(tag, activity, (long) this.mARWaitTimeMs, this.mARConfidenceTH).start();
                    if (this.mARMonitorConfidenceSampleMaxNum > 0) {
                        new ARMonitorThread((String) entry.getKey(), (long) this.mARMonitorConfidenceSampleTimeStepMs, this.mARMonitorConfidenceSampleMaxNum).start();
                    }
                } else {
                    setTopApkLevelToBLControllerIfNeeded(activity.mAction);
                    sendActivityRecognitionToMonitor(tag);
                }
                return;
            }
        }
        setTopApkLevelToBLControllerIfNeeded(18);
        sendActivityRecognitionToMonitor("android.activity_recognition.unknown");
    }

    private void sendAbnormalActivityRecognitionToMonitor(String tag, List confidenceSamples) {
        if (this.mDisplayEffectMonitor != null && confidenceSamples != null && tag != null) {
            int monitorScene = getMonitorUserScene(tag);
            if (monitorScene >= 0) {
                ArrayMap<String, Object> params = new ArrayMap();
                params.put(MonitorModule.PARAM_TYPE, "userSceneMisrecognition");
                params.put("userScene", Integer.valueOf(monitorScene));
                params.put("confidence", confidenceSamples);
                this.mDisplayEffectMonitor.sendMonitorParam(params);
            }
        }
    }

    private void sendActivityRecognitionToMonitor(String tag) {
        if (this.mDisplayEffectMonitor != null && tag != null) {
            int monitorScene = getMonitorUserScene(tag);
            if (monitorScene >= 0) {
                ArrayMap<String, Object> params = new ArrayMap();
                params.put(MonitorModule.PARAM_TYPE, "userScene");
                params.put("userScene", Integer.valueOf(monitorScene));
                this.mDisplayEffectMonitor.sendMonitorParam(params);
            }
        }
    }

    private int getMonitorUserScene(String tag) {
        if (tag == null) {
            return -1;
        }
        if (tag.equals("android.activity_recognition.still")) {
            return 1;
        }
        if (tag.equals(HwActivityRecognition.ACTIVITY_ON_FOOT)) {
            return 2;
        }
        if (tag.equals("android.activity_recognition.in_vehicle")) {
            return 3;
        }
        return 0;
    }

    protected Object getAREventType(int type) {
        if (type == 1) {
            return "enter";
        }
        if (type == 2) {
            return "exit";
        }
        return String.valueOf(type);
    }

    public boolean enableActivityEvent(String activity, int eventType, long reportLatencyNs) {
        if (this.mHwActivityRecognition != null) {
            return this.mHwActivityRecognition.enableActivityEvent(activity, eventType, reportLatencyNs);
        }
        return false;
    }

    public boolean disableActivityEvent(String activity, int eventType) {
        if (this.mHwActivityRecognition != null) {
            return this.mHwActivityRecognition.disableActivityEvent(activity, eventType);
        }
        return false;
    }

    public void getSupportedARActivities() {
        DElog.d(TAG, "getSupportedARActivities....");
    }

    public boolean flushAR() {
        if (this.mHwActivityRecognition != null) {
            return this.mHwActivityRecognition.flush();
        }
        return false;
    }

    public HwActivityChangedExtendEvent getCurrentActivityExtend() {
        if (this.mHwActivityRecognition != null) {
            return this.mHwActivityRecognition.getCurrentActivityExtend();
        }
        return null;
    }

    public String getCurrentActivity() {
        if (this.mHwActivityRecognition != null) {
            return this.mHwActivityRecognition.getCurrentActivity();
        }
        return "android.activity_recognition.unknown";
    }
}
