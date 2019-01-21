package huawei.android.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwCustEyeProtectionSplineImpl extends HwCustEyeProtectionSpline {
    private static boolean DEBUG = false;
    private static final float DEFAULT_BRIGHTNESS = 100.0f;
    private static final int EYE_PROTECTIION_MODE = SystemProperties.getInt("ro.config.hw_eyes_protection", EYE_PROTECTIION_MODE);
    private static final String EYE_PROTECTION_SCENE_SWITCH = "eye_protection_scene_switch";
    private static final String HW_LABC_CONFIG_FILE = "LABCConfig.xml";
    private static final String KEY_EYES_PROTECTION = "eyes_protection_mode";
    private static final int MODE_BACKLIGHT = 2;
    private static final String TAG = "EyeProtectionSpline";
    private boolean mBootCompleted = false;
    private BootCompletedReceiver mBootCompletedReceiver;
    private String mConfigFilePath = null;
    private Context mContext;
    List<Point> mEyeProtectionBrighnessLinePointsList = null;
    private ContentObserver mEyeProtectionModeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            HwCustEyeProtectionSplineImpl.this.updateBrightnessMode();
        }
    };
    private boolean mIsEyeProtectionBrightnessLineOK = false;
    private boolean mIsEyeProtectionMode = false;

    private class BootCompletedReceiver extends BroadcastReceiver {
        public BootCompletedReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            filter.setPriority(1000);
            HwCustEyeProtectionSplineImpl.this.mContext.registerReceiver(this, filter);
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String str = HwCustEyeProtectionSplineImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive intent action = ");
                stringBuilder.append(intent.getAction());
                Slog.i(str, stringBuilder.toString());
                if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                    HwCustEyeProtectionSplineImpl.this.mBootCompleted = true;
                    if ((HwCustEyeProtectionSplineImpl.EYE_PROTECTIION_MODE & HwCustEyeProtectionSplineImpl.MODE_BACKLIGHT) != 0) {
                        HwCustEyeProtectionSplineImpl.this.updateBrightnessMode();
                    }
                }
            }
        }
    }

    private class Point {
        float x;
        float y;

        public Point(float inx, float iny) {
            this.x = inx;
            this.y = iny;
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public HwCustEyeProtectionSplineImpl(Context context) {
        super(context);
        this.mContext = context;
        if ((EYE_PROTECTIION_MODE & MODE_BACKLIGHT) != 0) {
            this.mBootCompletedReceiver = new BootCompletedReceiver();
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor(KEY_EYES_PROTECTION), true, this.mEyeProtectionModeObserver, -1);
            try {
                if (!getConfig()) {
                    Slog.e(TAG, "getConfig failed!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateBrightnessMode() {
        boolean z = true;
        if (System.getIntForUser(this.mContext.getContentResolver(), KEY_EYES_PROTECTION, EYE_PROTECTIION_MODE, -2) != 1) {
            z = false;
        }
        this.mIsEyeProtectionMode = z;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateBrightnessMode mIsEyeProtectionMode = ");
        stringBuilder.append(this.mIsEyeProtectionMode);
        Slog.i(str, stringBuilder.toString());
    }

    public boolean IsEyeProtectionMode() {
        return this.mIsEyeProtectionMode && this.mIsEyeProtectionBrightnessLineOK;
    }

    /* JADX WARNING: Missing block: B:43:0x00cf, code skipped:
            if (r1 == null) goto L_0x00d2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getConfig() throws IOException {
        FileInputStream inputStream = null;
        String version = SystemProperties.get("ro.build.version.emui", inputStream);
        if (version == null || version.length() == 0) {
            Slog.e(TAG, "get ro.build.version.emui failed!");
            return false;
        }
        String[] versionSplited = version.split("EmotionUI_");
        StringBuilder stringBuilder;
        if (versionSplited.length < MODE_BACKLIGHT) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("split failed! version = ");
            stringBuilder.append(version);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
        String emuiVersion = versionSplited[1];
        if (emuiVersion == null || emuiVersion.length() == 0) {
            Slog.e(TAG, "get emuiVersion failed!");
            return false;
        }
        String xmlPath = new Object[MODE_BACKLIGHT];
        xmlPath[EYE_PROTECTIION_MODE] = emuiVersion;
        xmlPath[1] = HW_LABC_CONFIG_FILE;
        xmlPath = String.format("/xml/lcd/%s/%s", xmlPath);
        File xmlFile = HwCfgFilePolicy.getCfgFile(xmlPath, EYE_PROTECTIION_MODE);
        if (xmlFile == null) {
            inputStream = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile :");
            stringBuilder.append(xmlPath);
            stringBuilder.append(" failed!");
            Slog.e(inputStream, stringBuilder.toString());
            return false;
        }
        try {
            inputStream = new FileInputStream(xmlFile);
            if (getConfigFromXML(inputStream)) {
                if (true == checkConfigLoadedFromXML() && DEBUG) {
                    printConfigFromXML();
                }
                this.mConfigFilePath = xmlFile.getAbsolutePath();
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("get xmlFile :");
                    stringBuilder2.append(this.mConfigFilePath);
                    Slog.i(str2, stringBuilder2.toString());
                }
                inputStream.close();
                inputStream.close();
                return true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
            if (inputStream != null) {
            }
        } catch (Exception e3) {
            e3.printStackTrace();
            if (inputStream != null) {
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        inputStream.close();
        return false;
    }

    private boolean checkConfigLoadedFromXML() {
        if (checkPointsListIsOK(this.mEyeProtectionBrighnessLinePointsList)) {
            this.mIsEyeProtectionBrightnessLineOK = true;
            if (DEBUG) {
                Slog.i(TAG, "checkConfigLoadedFromXML success!");
            }
            return true;
        }
        this.mIsEyeProtectionBrightnessLineOK = false;
        Slog.e(TAG, "checkPointsList mEyeProtectionBrighnessLinePointsList is wrong, use DefaultBrighnessLine!");
        return false;
    }

    private boolean checkPointsListIsOK(List<Point> LinePointsList) {
        List<Point> mLinePointsList = LinePointsList;
        if (mLinePointsList == null) {
            Slog.e(TAG, "LoadXML false for mLinePointsList == null");
            return false;
        } else if (mLinePointsList.size() <= MODE_BACKLIGHT || mLinePointsList.size() >= 100) {
            Slog.e(TAG, "LoadXML false for mLinePointsList number is wrong");
            return false;
        } else {
            int mDrkenNum = EYE_PROTECTIION_MODE;
            Point lastPoint = null;
            for (Point tmpPoint : mLinePointsList) {
                if (mDrkenNum != 0 && lastPoint.x >= tmpPoint.x) {
                    Slog.e(TAG, "LoadXML false for mLinePointsList is wrong");
                    return false;
                }
                lastPoint = tmpPoint;
                mDrkenNum++;
            }
            return true;
        }
    }

    private void printConfigFromXML() {
        for (Point temp : this.mEyeProtectionBrighnessLinePointsList) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LoadXMLConfig_EyeProtectionBrighnessLinePoints x = ");
            stringBuilder.append(temp.x);
            stringBuilder.append(", y = ");
            stringBuilder.append(temp.y);
            Slog.i(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x00a9 A:{Catch:{ XmlPullParserException -> 0x00c4, IOException -> 0x00bf, NumberFormatException -> 0x00ba, Exception -> 0x00b5 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getConfigFromXML(InputStream inStream) {
        if (DEBUG) {
            Slog.i(TAG, "getConfigFromeXML");
        }
        boolean EyeProtectionBrighnessLinePointsListsLoadStarted = false;
        boolean EyeProtectionBrighnessLinePointsListLoaded = false;
        boolean loadFinished = false;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                String name;
                switch (eventType) {
                    case MODE_BACKLIGHT /*2*/:
                        name = parser.getName();
                        if (!name.equals("EyeProtectionBrightnessPoints")) {
                            if (name.equals("Point") && EyeProtectionBrighnessLinePointsListsLoadStarted) {
                                Point curPoint = new Point();
                                String s = parser.nextText();
                                curPoint.x = Float.parseFloat(s.split(",")[EYE_PROTECTIION_MODE]);
                                curPoint.y = Float.parseFloat(s.split(",")[1]);
                                if (this.mEyeProtectionBrighnessLinePointsList == null) {
                                    this.mEyeProtectionBrighnessLinePointsList = new ArrayList();
                                }
                                this.mEyeProtectionBrighnessLinePointsList.add(curPoint);
                                break;
                            }
                        }
                        EyeProtectionBrighnessLinePointsListsLoadStarted = true;
                        break;
                    case 3:
                        name = parser.getName();
                        if (name.equals("LABCConfig") && false) {
                            loadFinished = true;
                            break;
                        } else if (name.equals("EyeProtectionBrightnessPoints")) {
                            EyeProtectionBrighnessLinePointsListsLoadStarted = false;
                            if (this.mEyeProtectionBrighnessLinePointsList != null) {
                                EyeProtectionBrighnessLinePointsListLoaded = true;
                                break;
                            }
                            Slog.e(TAG, "no EyeProtectionBrightnessPoints loaded!");
                            return false;
                        }
                        break;
                    default:
                        break;
                }
                if (loadFinished) {
                    if (EyeProtectionBrighnessLinePointsListLoaded) {
                        if (DEBUG) {
                            Slog.i(TAG, "getConfigFromeXML success!");
                        }
                        return true;
                    }
                    Slog.e(TAG, "getConfigFromeXML false!");
                    return false;
                }
            }
            if (EyeProtectionBrighnessLinePointsListLoaded) {
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (NumberFormatException e3) {
            e3.printStackTrace();
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        Slog.e(TAG, "getConfigFromeXML false!");
        return false;
    }

    public float getEyeProtectionBrightnessLevel(float lux) {
        int count = EYE_PROTECTIION_MODE;
        float brightnessLevel = DEFAULT_BRIGHTNESS;
        Point temp1 = null;
        for (Point temp : this.mEyeProtectionBrighnessLinePointsList) {
            if (count == 0) {
                temp1 = temp;
            }
            if (lux < temp.x) {
                Point temp2 = temp;
                if (temp2.x > temp1.x) {
                    return (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (lux - temp1.x)) + temp1.y;
                }
                if (!DEBUG) {
                    return DEFAULT_BRIGHTNESS;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Brighness_temp1.x <= temp2.x,x");
                stringBuilder.append(temp.x);
                stringBuilder.append(", y = ");
                stringBuilder.append(temp.y);
                Slog.i(str, stringBuilder.toString());
                return DEFAULT_BRIGHTNESS;
            }
            temp1 = temp;
            brightnessLevel = temp1.y;
            count++;
        }
        return brightnessLevel;
    }
}
