package huawei.android.utils;

import android.content.Context;
import android.os.FileUtils;
import android.os.SystemProperties;
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

public class HwEyeProtectionSplineImpl extends HwEyeProtectionSpline {
    private static boolean DEBUG = false;
    private static final float DEFAULT_BRIGHTNESS = 100.0f;
    private static final int EYE_PROTECTIION_MODE = SystemProperties.getInt("ro.config.hw_eyes_protection", 7);
    private static final String HW_EYEPROTECTION_CONFIG_FILE = "EyeProtectionConfig.xml";
    private static final String HW_EYEPROTECTION_CONFIG_FILE_NAME = "EyeProtectionConfig";
    private static final String LCD_PANEL_TYPE_PATH = "/sys/class/graphics/fb0/lcd_model";
    private static final int MODE_BACKLIGHT = 2;
    private static final String TAG = "EyeProtectionSpline";
    List<Point> mEyeProtectionBrighnessLinePointsList = null;
    private boolean mIsEyeProtectionBrightnessLineOK = false;
    private String mLcdPanelName = null;

    private static class Point {
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

    public HwEyeProtectionSplineImpl(Context context) {
        super(context);
        if ((EYE_PROTECTIION_MODE & 2) != 0) {
            this.mLcdPanelName = getLcdPanelName();
            try {
                if (!getConfig()) {
                    Slog.e(TAG, "getConfig failed!");
                }
            } catch (Exception e) {
            }
        }
    }

    public boolean isEyeProtectionMode() {
        return this.mEyeProtectionControlFlag && this.mIsEyeProtectionBrightnessLineOK;
    }

    /* JADX WARNING: Missing block: B:46:0x00f1, code skipped:
            if (r1 == null) goto L_0x00f4;
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
        if (versionSplited.length < 2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
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
        String lcdEyeProtectionConfigFile = new StringBuilder();
        lcdEyeProtectionConfigFile.append("EyeProtectionConfig_");
        lcdEyeProtectionConfigFile.append(this.mLcdPanelName);
        lcdEyeProtectionConfigFile.append(".xml");
        lcdEyeProtectionConfigFile = lcdEyeProtectionConfigFile.toString();
        File xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/lcd/%s/%s", new Object[]{emuiVersion, HW_EYEPROTECTION_CONFIG_FILE}), 0);
        if (xmlFile == null) {
            xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/lcd/%s", new Object[]{HW_EYEPROTECTION_CONFIG_FILE}), 0);
            if (xmlFile == null) {
                xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/lcd/%s/%s", new Object[]{emuiVersion, lcdEyeProtectionConfigFile}), 0);
                if (xmlFile == null) {
                    xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/lcd/%s", new Object[]{lcdEyeProtectionConfigFile}), 0);
                    if (xmlFile == null) {
                        Slog.w(TAG, "get xmlFile failed!");
                        return false;
                    }
                }
            }
        }
        try {
            inputStream = new FileInputStream(xmlFile);
            if (getConfigFromXML(inputStream)) {
                if (true == checkConfigLoadedFromXML() && DEBUG) {
                    printConfigFromXML();
                }
                inputStream.close();
                inputStream.close();
                return true;
            }
        } catch (FileNotFoundException e) {
            Slog.i(TAG, "get xmlFile file not found");
        } catch (IOException e2) {
            Slog.i(TAG, "get xmlFile has IO exception");
            if (inputStream != null) {
            }
        } catch (Exception e3) {
            Slog.i(TAG, "get xmlFile has exception");
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
        } else if (mLinePointsList.size() <= 2 || mLinePointsList.size() >= 100) {
            Slog.e(TAG, "LoadXML false for mLinePointsList number is wrong");
            return false;
        } else {
            int mDrkenNum = 0;
            Point lastPoint = null;
            for (Point tmpPoint : mLinePointsList) {
                if (mDrkenNum == 0 || lastPoint == null || lastPoint.x < tmpPoint.x) {
                    lastPoint = tmpPoint;
                    mDrkenNum++;
                } else {
                    Slog.e(TAG, "LoadXML false for mLinePointsList is wrong");
                    return false;
                }
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

    private String getLcdPanelName() {
        String panelName = null;
        try {
            panelName = FileUtils.readTextFile(new File(LCD_PANEL_TYPE_PATH), 0, null).trim();
            return panelName.replace(' ', '_');
        } catch (IOException e) {
            Slog.e(TAG, "Error reading lcd panel name", e);
            return panelName;
        }
    }

    private boolean getConfigFromXML(InputStream inStream) {
        if (DEBUG) {
            Slog.i(TAG, "getConfigFromeXML");
        }
        boolean EyeProtectionBrighnessLinePointsListsLoadStarted = false;
        boolean EyeProtectionBrighnessLinePointsListLoaded = false;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                switch (eventType) {
                    case 2:
                        String name = parser.getName();
                        if (!name.equals("EyeProtectionBrightnessPoints")) {
                            if (name.equals("Point") && EyeProtectionBrighnessLinePointsListsLoadStarted) {
                                Point curPoint = new Point();
                                String s = parser.nextText();
                                curPoint.x = Float.parseFloat(s.split(",")[0]);
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
                        if (parser.getName().equals("EyeProtectionBrightnessPoints")) {
                            EyeProtectionBrighnessLinePointsListsLoadStarted = false;
                            if (this.mEyeProtectionBrighnessLinePointsList != null) {
                                EyeProtectionBrighnessLinePointsListLoaded = true;
                                break;
                            }
                            Slog.e(TAG, "no EyeProtectionBrightnessPoints loaded!");
                            return false;
                        }
                        continue;
                    default:
                        break;
                }
            }
            if (EyeProtectionBrighnessLinePointsListLoaded) {
                Slog.i(TAG, "getConfigFromeXML success!");
                return true;
            }
        } catch (XmlPullParserException e) {
            Slog.i(TAG, "getConfigFromeXML has XmlPullParserException!");
        } catch (IOException e2) {
            Slog.i(TAG, "getConfigFromeXML has IOException!");
        } catch (NumberFormatException e3) {
            Slog.i(TAG, "getConfigFromeXML has NumberFormatException!");
        } catch (Exception e4) {
            Slog.i(TAG, "getConfigFromeXML has Exception!");
        }
        Slog.e(TAG, "getConfigFromeXML false!");
        return false;
    }

    public float getEyeProtectionBrightnessLevel(float lux) {
        int count = 0;
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
