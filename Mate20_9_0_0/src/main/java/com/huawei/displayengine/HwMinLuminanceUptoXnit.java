package com.huawei.displayengine;

import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Xml;
import com.huawei.displayengine.IDisplayEngineServiceEx.Stub;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwMinLuminanceUptoXnit {
    private static final int FIRST_ADDED_BRIGHTNESS = 156;
    private static final int FOURTH_ADDED_BRIGHTNESS = 274;
    private static final int MAX_BRIGHTNESS = 10000;
    private static final int MIN_BRIGHTNESS = 156;
    private static final int NORMALIZED_DEFAULT_MAX_BRIGHTNESS = 255;
    private static final int NORMALIZED_DEFAULT_MIN_BRIGHTNESS = 4;
    private static final int NORMALIZED_MAX_BRIGHTNESS = 10000;
    private static final int SECOND_ADDED_BRIGHTNESS = 196;
    private static String TAG = "HwMinLuminanceUptoXnit";
    private static final int THIRD_ADDED_BRIGHTNESS = 235;
    private static final String XNIT_CONFIG_NAME = "XnitConfig.xml";
    private static final String XNIT_CONFIG_NAME_NOEXT = "XnitConfig";
    private int mActualMaxLuminance = 360;
    private int mActualMinLuminance = 4;
    private int mAddedPointForExpectedMinLum = 3;
    private DisplayEngineManager mDisplayEngineManager;
    private int mExpectedMinLuminance = 2;
    private String mLcdPanelName = null;
    private boolean mNeedAdjustMinLum = false;
    private int mSupportXCC = 0;
    private float[][] mXccCoefForExpectedMinLum = new float[][]{new float[]{1.0f, 1.0f, 1.0f}, new float[]{1.0f, 1.0f, 1.0f}, new float[]{1.0f, 1.0f, 1.0f}};
    private boolean mXccCoef_is_reseted = false;

    private String getLcdPanelName() {
        IBinder binder = ServiceManager.getService(DisplayEngineManager.SERVICE_NAME);
        if (binder == null) {
            DElog.i(TAG, "getLcdModelName() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx service = Stub.asInterface(binder);
        if (service == null) {
            DElog.i(TAG, "getLcdModelName() service is null!");
            return null;
        }
        byte[] name = new byte[128];
        int ret = 0;
        try {
            int ret2 = service.getEffect(14, 0, name, name.length);
            if (ret2 == 0) {
                return new String(name).trim().replace(' ', '_').replace("'", "");
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getLcdModelName() getEffect failed! ret=");
            stringBuilder.append(ret2);
            DElog.i(str, stringBuilder.toString());
            return null;
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getLcdModelName() RemoteException ");
            stringBuilder2.append(e);
            DElog.e(str2, stringBuilder2.toString());
            return null;
        }
    }

    private String getXmlPath() {
        String lcdXnitConfigFile = new StringBuilder();
        lcdXnitConfigFile.append("XnitConfig_");
        lcdXnitConfigFile.append(this.mLcdPanelName);
        lcdXnitConfigFile.append(".xml");
        File xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/display/effect/displayengine/%s", new Object[]{lcdXnitConfigFile.toString()}), 0);
        if (xmlFile == null) {
            xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/display/effect/displayengine/%s", new Object[]{XNIT_CONFIG_NAME}), 0);
            if (xmlFile == null) {
                DElog.i(TAG, "get xmlFile failed!");
                return null;
            }
        }
        return xmlFile.getAbsolutePath();
    }

    public HwMinLuminanceUptoXnit(DisplayEngineManager context) {
        this.mDisplayEngineManager = context;
        try {
            this.mSupportXCC = this.mDisplayEngineManager.getSupported(16);
            this.mLcdPanelName = getLcdPanelName();
            if (!getConfig(getXmlPath())) {
                DElog.i(TAG, "getConfig failed! loadDefaultConfig");
                loadDefaultConfig();
            }
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getConfig error! Exception ");
            stringBuilder.append(e);
            DElog.e(str, stringBuilder.toString());
        }
    }

    private boolean getConfig(String configFilePath) throws IOException {
        String str;
        StringBuilder stringBuilder;
        IOException e1;
        if (configFilePath == null || configFilePath.length() == 0) {
            DElog.i(TAG, "getConfig configFilePath is null! use default config");
            return false;
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(configFilePath));
            if (getConfigFromXML(inputStream)) {
                checkConfigLoadedFromXML();
                inputStream.close();
                try {
                    inputStream.close();
                } catch (IOException e12) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("e1 is ");
                    stringBuilder.append(e12);
                    DElog.e(str, stringBuilder.toString());
                }
                return true;
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e12 = e;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
            return false;
        } catch (IOException e122) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getConfig error! Exception ");
            stringBuilder.append(e122);
            DElog.e(str, stringBuilder.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e2) {
                    e122 = e2;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1222) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("e1 is ");
                    stringBuilder.append(e1222);
                    DElog.e(str, stringBuilder.toString());
                }
            }
        }
        stringBuilder.append("e1 is ");
        stringBuilder.append(e1222);
        DElog.e(str, stringBuilder.toString());
        return false;
    }

    private void checkConfigLoadedFromXML() {
        if (this.mActualMinLuminance > this.mActualMaxLuminance) {
            loadDefaultConfig();
            DElog.e(TAG, "checkConfig failed for mActualMinLuminance > mActualMaxLuminance , LoadDefaultConfig!");
        } else if (this.mExpectedMinLuminance > this.mActualMaxLuminance) {
            loadDefaultConfig();
            DElog.e(TAG, "checkConfig failed for mExpectedMinLuminance > mActualMaxLuminance , LoadDefaultConfig!");
        } else if (this.mAddedPointForExpectedMinLum > 3 || ((this.mAddedPointForExpectedMinLum >= 1 && (this.mXccCoefForExpectedMinLum[0][0] <= 0.0f || this.mXccCoefForExpectedMinLum[0][0] > 1.0f)) || ((this.mAddedPointForExpectedMinLum >= 2 && (this.mXccCoefForExpectedMinLum[1][0] <= 0.0f || this.mXccCoefForExpectedMinLum[1][0] > 1.0f)) || (this.mAddedPointForExpectedMinLum >= 3 && (this.mXccCoefForExpectedMinLum[2][0] <= 0.0f || this.mXccCoefForExpectedMinLum[2][0] > 1.0f))))) {
            loadDefaultConfig();
            DElog.e(TAG, "checkConfig failed for mAddedPointForExpectedMinLum > 4 , LoadDefaultConfig!");
        } else {
            DElog.i(TAG, "checkConfig LoadedFromXML success!");
        }
    }

    private void loadDefaultConfig() {
        DElog.i(TAG, "loadDefaultConfig");
        this.mNeedAdjustMinLum = false;
        this.mActualMaxLuminance = 350;
        this.mActualMinLuminance = 4;
        this.mExpectedMinLuminance = 2;
        this.mAddedPointForExpectedMinLum = 2;
        this.mXccCoefForExpectedMinLum[0][0] = 1.0f;
        this.mXccCoefForExpectedMinLum[0][1] = 1.0f;
        this.mXccCoefForExpectedMinLum[0][2] = 1.0f;
        this.mXccCoefForExpectedMinLum[1][0] = 1.0f;
        this.mXccCoefForExpectedMinLum[1][1] = 1.0f;
        this.mXccCoefForExpectedMinLum[1][2] = 1.0f;
        this.mXccCoefForExpectedMinLum[2][0] = 1.0f;
        this.mXccCoefForExpectedMinLum[2][1] = 1.0f;
        this.mXccCoefForExpectedMinLum[2][2] = 1.0f;
    }

    /* JADX WARNING: Missing block: B:10:0x0029, code skipped:
            r18 = r0;
            r19 = r8;
     */
    /* JADX WARNING: Missing block: B:14:0x003d, code skipped:
            r18 = r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getConfigFromXML(InputStream inStream) {
        XmlPullParserException e;
        IOException e2;
        InputStream inputStream;
        DElog.i(TAG, "getConfigFromXML");
        boolean NeedAdjustMinLumLoaded = false;
        boolean ActualMaxLuminanceLoaded = false;
        boolean ActualMinLuminanceLoaded = false;
        boolean ExpectedMinLuminanceLoaded = false;
        boolean AddedPointForExpectedMinLumLoaded = false;
        boolean XccCoefForExpectedMinLumLoaded = false;
        boolean XccCoefForExpectedMinLumLoadStarted = false;
        int index = 0;
        XmlPullParser parser = Xml.newPullParser();
        String str;
        StringBuilder stringBuilder;
        try {
            try {
                boolean z;
                int i;
                parser.setInput(inStream, "UTF-8");
                int eventType = parser.getEventType();
                while (eventType != 1) {
                    if (eventType != 0) {
                        switch (eventType) {
                            case 2:
                                try {
                                    String name = parser.getName();
                                    if (name == null) {
                                        z = XccCoefForExpectedMinLumLoadStarted;
                                    } else if (name.length() == 0) {
                                        i = eventType;
                                        z = XccCoefForExpectedMinLumLoadStarted;
                                    } else if (name.equals("NeedAdjustMinLum")) {
                                        this.mNeedAdjustMinLum = Boolean.parseBoolean(parser.nextText());
                                        NeedAdjustMinLumLoaded = true;
                                        String str2 = TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("NeedAdjustMinLum = ");
                                        stringBuilder2.append(this.mNeedAdjustMinLum);
                                        DElog.i(str2, stringBuilder2.toString());
                                        break;
                                    } else if (name.equals("ActualMaxLuminance")) {
                                        this.mActualMaxLuminance = Integer.parseInt(parser.nextText());
                                        ActualMaxLuminanceLoaded = true;
                                        str = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("ActualMaxLuminance = ");
                                        stringBuilder.append(this.mActualMaxLuminance);
                                        DElog.i(str, stringBuilder.toString());
                                        break;
                                    } else if (name.equals("ActualMinLuminance")) {
                                        this.mActualMinLuminance = Integer.parseInt(parser.nextText());
                                        ActualMinLuminanceLoaded = true;
                                        str = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("ActualMinLuminance = ");
                                        stringBuilder.append(this.mActualMinLuminance);
                                        DElog.i(str, stringBuilder.toString());
                                        break;
                                    } else if (name.equals("ExpectedMinLuminance")) {
                                        this.mExpectedMinLuminance = Integer.parseInt(parser.nextText());
                                        ExpectedMinLuminanceLoaded = true;
                                        str = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("ExpectedMinLuminance = ");
                                        stringBuilder.append(this.mExpectedMinLuminance);
                                        DElog.i(str, stringBuilder.toString());
                                        break;
                                    } else if (name.equals("AddedPointsForExpectedMinLum")) {
                                        this.mAddedPointForExpectedMinLum = Integer.parseInt(parser.nextText());
                                        AddedPointForExpectedMinLumLoaded = true;
                                        str = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("AddedPointsForExpectedMinLum = ");
                                        stringBuilder.append(this.mAddedPointForExpectedMinLum);
                                        DElog.i(str, stringBuilder.toString());
                                        break;
                                    } else if (name.equals("XccCoefForExpectedMinLum")) {
                                        XccCoefForExpectedMinLumLoadStarted = true;
                                        break;
                                    } else if (!name.equals("XccCoef") || !AddedPointForExpectedMinLumLoaded || !XccCoefForExpectedMinLumLoadStarted) {
                                        z = XccCoefForExpectedMinLumLoadStarted;
                                        break;
                                    } else {
                                        String str3;
                                        str = parser.nextText();
                                        String[] XccForRGBSplited = str.split(",");
                                        if (XccForRGBSplited != null) {
                                            i = eventType;
                                            if (XccForRGBSplited.length != 3) {
                                                z = XccCoefForExpectedMinLumLoadStarted;
                                            } else {
                                                z = XccCoefForExpectedMinLumLoadStarted;
                                                try {
                                                    this.mXccCoefForExpectedMinLum[index][0] = Float.parseFloat(XccForRGBSplited[0]);
                                                    this.mXccCoefForExpectedMinLum[index][1] = Float.parseFloat(XccForRGBSplited[1]);
                                                    this.mXccCoefForExpectedMinLum[index][2] = Float.parseFloat(XccForRGBSplited[2]);
                                                    str3 = TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("mXccCoefForExpectedMinLum x = ");
                                                    stringBuilder.append(this.mXccCoefForExpectedMinLum[index][0]);
                                                    stringBuilder.append(", y = ");
                                                    stringBuilder.append(this.mXccCoefForExpectedMinLum[index][1]);
                                                    stringBuilder.append(", z = ");
                                                    stringBuilder.append(this.mXccCoefForExpectedMinLum[index][2]);
                                                    DElog.i(str3, stringBuilder.toString());
                                                    index++;
                                                    XccCoefForExpectedMinLumLoadStarted = z;
                                                    break;
                                                } catch (XmlPullParserException e3) {
                                                    e = e3;
                                                    XccCoefForExpectedMinLumLoadStarted = z;
                                                    str = TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("getConfigFromXML error! Exception ");
                                                    stringBuilder.append(e);
                                                    DElog.e(str, stringBuilder.toString());
                                                    DElog.e(TAG, "getConfigFromXML failed!");
                                                    return false;
                                                } catch (IOException e4) {
                                                    e2 = e4;
                                                    XccCoefForExpectedMinLumLoadStarted = z;
                                                    str = TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("getConfigFromXML error! Exception ");
                                                    stringBuilder.append(e2);
                                                    DElog.e(str, stringBuilder.toString());
                                                    DElog.e(TAG, "getConfigFromXML failed!");
                                                    return false;
                                                }
                                            }
                                        }
                                        z = XccCoefForExpectedMinLumLoadStarted;
                                        str3 = TAG;
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("split failed! s = ");
                                        stringBuilder3.append(str);
                                        DElog.e(str3, stringBuilder3.toString());
                                        return false;
                                    }
                                    return false;
                                } catch (XmlPullParserException e5) {
                                    e = e5;
                                    z = XccCoefForExpectedMinLumLoadStarted;
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("getConfigFromXML error! Exception ");
                                    stringBuilder.append(e);
                                    DElog.e(str, stringBuilder.toString());
                                    DElog.e(TAG, "getConfigFromXML failed!");
                                    return false;
                                } catch (IOException e6) {
                                    e2 = e6;
                                    z = XccCoefForExpectedMinLumLoadStarted;
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("getConfigFromXML error! Exception ");
                                    stringBuilder.append(e2);
                                    DElog.e(str, stringBuilder.toString());
                                    DElog.e(TAG, "getConfigFromXML failed!");
                                    return false;
                                }
                            case 3:
                                if (parser.getName().equals("XccCoefForExpectedMinLum")) {
                                    XccCoefForExpectedMinLumLoadStarted = false;
                                    XccCoefForExpectedMinLumLoaded = true;
                                    break;
                                }
                                break;
                        }
                    }
                    i = eventType;
                    z = XccCoefForExpectedMinLumLoadStarted;
                    XccCoefForExpectedMinLumLoadStarted = z;
                    eventType = parser.next();
                }
                i = eventType;
                z = XccCoefForExpectedMinLumLoadStarted;
                if (NeedAdjustMinLumLoaded && ActualMaxLuminanceLoaded && ActualMinLuminanceLoaded && AddedPointForExpectedMinLumLoaded && ExpectedMinLuminanceLoaded && XccCoefForExpectedMinLumLoaded) {
                    DElog.i(TAG, "xnit getConfigFromeXML success!");
                    return true;
                }
                XccCoefForExpectedMinLumLoadStarted = z;
                DElog.e(TAG, "getConfigFromXML failed!");
                return false;
            } catch (XmlPullParserException e7) {
                e = e7;
            } catch (IOException e8) {
                e2 = e8;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getConfigFromXML error! Exception ");
                stringBuilder.append(e2);
                DElog.e(str, stringBuilder.toString());
                DElog.e(TAG, "getConfigFromXML failed!");
                return false;
            }
        } catch (XmlPullParserException e9) {
            e = e9;
            inputStream = inStream;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getConfigFromXML error! Exception ");
            stringBuilder.append(e);
            DElog.e(str, stringBuilder.toString());
            DElog.e(TAG, "getConfigFromXML failed!");
            return false;
        } catch (IOException e10) {
            e2 = e10;
            inputStream = inStream;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getConfigFromXML error! Exception ");
            stringBuilder.append(e2);
            DElog.e(str, stringBuilder.toString());
            DElog.e(TAG, "getConfigFromXML failed!");
            return false;
        }
    }

    private void xccCoef_setting(float xccCoef_R, float xccCoef_G, float xccCoef_B) {
        if (xccCoef_R <= 1.0f && xccCoef_R >= 0.1f && xccCoef_G <= 1.0f && xccCoef_G >= 0.1f && xccCoef_B <= 1.0f && xccCoef_B >= 0.1f) {
            int[] xccCoef = new int[]{(int) (xccCoef_R * 32768.0f), (int) (xccCoef_G * 32768.0f), (int) (32768.0f * xccCoef_B)};
            PersistableBundle bundle = new PersistableBundle();
            bundle.putIntArray("Buffer", xccCoef);
            bundle.putInt("BufferLength", 12);
            this.mDisplayEngineManager.setData(5, bundle);
        }
    }

    public int setXnit(int mNormalizedMinBrightness, int mNormalizedMaxBrightness, int level) {
        int brightnessvalue = level;
        if (this.mSupportXCC == 0 || !this.mNeedAdjustMinLum || this.mAddedPointForExpectedMinLum == 0 || 0.0f == this.mXccCoefForExpectedMinLum[0][0]) {
            return (((level - 156) * (mNormalizedMaxBrightness - mNormalizedMinBrightness)) / 9844) + mNormalizedMinBrightness;
        }
        float xccCoef_R;
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        switch (this.mAddedPointForExpectedMinLum) {
            case 0:
                brightnessvalue = mNormalizedMinBrightness + (((level - 156) * (mNormalizedMaxBrightness - mNormalizedMinBrightness)) / 9844);
                break;
            case 1:
                if (level > 196) {
                    if (!this.mXccCoef_is_reseted) {
                        xccCoef_setting(1.0f, 1.0f, 1.0f);
                    }
                    this.mXccCoef_is_reseted = true;
                    brightnessvalue = mNormalizedMinBrightness + (((level - 196) * (mNormalizedMaxBrightness - mNormalizedMinBrightness)) / 9804);
                    break;
                }
                brightnessvalue = mNormalizedMinBrightness;
                xccCoef_R = this.mXccCoefForExpectedMinLum[0][0] + ((((float) (level - 156)) / 40.0f) * (1.0f - this.mXccCoefForExpectedMinLum[0][0]));
                xccCoef_setting(xccCoef_R, this.mXccCoefForExpectedMinLum[0][1] + ((((float) (level - 156)) / 40.0f) * (1.0f - this.mXccCoefForExpectedMinLum[0][1])), this.mXccCoefForExpectedMinLum[0][2] + ((((float) (level - 156)) / 40.0f) * (1.0f - this.mXccCoefForExpectedMinLum[0][2])));
                this.mXccCoef_is_reseted = false;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("xnit xccCoef_R = ");
                stringBuilder.append(xccCoef_R);
                stringBuilder.append(", input_level = ");
                stringBuilder.append(level);
                stringBuilder.append(", addedXccNumbers = ");
                stringBuilder.append(this.mAddedPointForExpectedMinLum);
                DElog.i(str, stringBuilder.toString());
                break;
            case 2:
                if (level >= 196) {
                    if (level > 235) {
                        if (!this.mXccCoef_is_reseted) {
                            xccCoef_setting(1.0f, 1.0f, 1.0f);
                        }
                        this.mXccCoef_is_reseted = true;
                        brightnessvalue = mNormalizedMinBrightness + (((level - 235) * (mNormalizedMaxBrightness - mNormalizedMinBrightness)) / 9765);
                        break;
                    }
                    brightnessvalue = mNormalizedMinBrightness;
                    xccCoef_R = this.mXccCoefForExpectedMinLum[1][0] + ((((float) (level - 196)) / 39.0f) * (1.0f - this.mXccCoefForExpectedMinLum[1][0]));
                    xccCoef_setting(xccCoef_R, this.mXccCoefForExpectedMinLum[1][1] + ((((float) (level - 196)) / 39.0f) * (1.0f - this.mXccCoefForExpectedMinLum[1][1])), this.mXccCoefForExpectedMinLum[1][2] + ((((float) (level - 196)) / 39.0f) * (1.0f - this.mXccCoefForExpectedMinLum[1][2])));
                    this.mXccCoef_is_reseted = false;
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("xnit xccCoef_R = ");
                    stringBuilder2.append(xccCoef_R);
                    stringBuilder2.append(", input_level = ");
                    stringBuilder2.append(level);
                    stringBuilder2.append(", addedXccNumbers = ");
                    stringBuilder2.append(this.mAddedPointForExpectedMinLum);
                    DElog.i(str, stringBuilder2.toString());
                    break;
                }
                brightnessvalue = mNormalizedMinBrightness;
                xccCoef_R = this.mXccCoefForExpectedMinLum[0][0] + ((((float) (level - 156)) * (this.mXccCoefForExpectedMinLum[1][0] - this.mXccCoefForExpectedMinLum[0][0])) / 40.0f);
                xccCoef_setting(xccCoef_R, this.mXccCoefForExpectedMinLum[0][1] + ((((float) (level - 156)) * (this.mXccCoefForExpectedMinLum[1][1] - this.mXccCoefForExpectedMinLum[0][1])) / 40.0f), this.mXccCoefForExpectedMinLum[0][2] + ((((float) (level - 156)) * (this.mXccCoefForExpectedMinLum[1][2] - this.mXccCoefForExpectedMinLum[0][2])) / 40.0f));
                this.mXccCoef_is_reseted = false;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("xnit xccCoef_R = ");
                stringBuilder.append(xccCoef_R);
                stringBuilder.append(", input_level = ");
                stringBuilder.append(level);
                stringBuilder.append(", addedXccNumbers = ");
                stringBuilder.append(this.mAddedPointForExpectedMinLum);
                DElog.i(str, stringBuilder.toString());
                break;
            case 3:
                if (level >= 196) {
                    if (level >= 235) {
                        if (level > FOURTH_ADDED_BRIGHTNESS) {
                            if (!this.mXccCoef_is_reseted) {
                                xccCoef_setting(1.0f, 1.0f, 1.0f);
                            }
                            this.mXccCoef_is_reseted = true;
                            brightnessvalue = mNormalizedMinBrightness + (((level - 274) * (mNormalizedMaxBrightness - mNormalizedMinBrightness)) / 9726);
                            break;
                        }
                        brightnessvalue = mNormalizedMinBrightness;
                        xccCoef_R = this.mXccCoefForExpectedMinLum[2][0] + ((((float) (level - 235)) / 39.0f) * (1.0f - this.mXccCoefForExpectedMinLum[2][0]));
                        xccCoef_setting(xccCoef_R, this.mXccCoefForExpectedMinLum[2][1] + ((((float) (level - 235)) / 39.0f) * (1.0f - this.mXccCoefForExpectedMinLum[2][1])), this.mXccCoefForExpectedMinLum[2][2] + ((((float) (level - 235)) / 39.0f) * (1.0f - this.mXccCoefForExpectedMinLum[2][2])));
                        this.mXccCoef_is_reseted = false;
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("xnit xccCoef_R = ");
                        stringBuilder2.append(xccCoef_R);
                        stringBuilder2.append(", input_level = ");
                        stringBuilder2.append(level);
                        stringBuilder2.append(", addedXccNumbers = ");
                        stringBuilder2.append(this.mAddedPointForExpectedMinLum);
                        DElog.i(str, stringBuilder2.toString());
                        break;
                    }
                    brightnessvalue = mNormalizedMinBrightness;
                    xccCoef_R = this.mXccCoefForExpectedMinLum[1][0] + ((((float) (level - 196)) * (this.mXccCoefForExpectedMinLum[2][0] - this.mXccCoefForExpectedMinLum[1][0])) / 39.0f);
                    xccCoef_setting(xccCoef_R, this.mXccCoefForExpectedMinLum[1][1] + ((((float) (level - 196)) * (this.mXccCoefForExpectedMinLum[2][1] - this.mXccCoefForExpectedMinLum[1][1])) / 39.0f), this.mXccCoefForExpectedMinLum[1][2] + ((((float) (level - 196)) * (this.mXccCoefForExpectedMinLum[2][2] - this.mXccCoefForExpectedMinLum[1][2])) / 39.0f));
                    this.mXccCoef_is_reseted = false;
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("xnit xccCoef_R = ");
                    stringBuilder2.append(xccCoef_R);
                    stringBuilder2.append(", input_level = ");
                    stringBuilder2.append(level);
                    stringBuilder2.append(", addedXccNumbers = ");
                    stringBuilder2.append(this.mAddedPointForExpectedMinLum);
                    DElog.i(str, stringBuilder2.toString());
                    break;
                }
                brightnessvalue = mNormalizedMinBrightness;
                xccCoef_R = this.mXccCoefForExpectedMinLum[0][0] + ((((float) (level - 156)) * (this.mXccCoefForExpectedMinLum[1][0] - this.mXccCoefForExpectedMinLum[0][0])) / 40.0f);
                xccCoef_setting(xccCoef_R, this.mXccCoefForExpectedMinLum[0][1] + ((((float) (level - 156)) * (this.mXccCoefForExpectedMinLum[1][1] - this.mXccCoefForExpectedMinLum[0][1])) / 40.0f), this.mXccCoefForExpectedMinLum[0][2] + ((((float) (level - 156)) * (this.mXccCoefForExpectedMinLum[1][2] - this.mXccCoefForExpectedMinLum[0][2])) / 40.0f));
                this.mXccCoef_is_reseted = false;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("xnit xccCoef_R = ");
                stringBuilder.append(xccCoef_R);
                stringBuilder.append(", input_level = ");
                stringBuilder.append(level);
                stringBuilder.append(", addedXccNumbers = ");
                stringBuilder.append(this.mAddedPointForExpectedMinLum);
                DElog.i(str, stringBuilder.toString());
                break;
        }
        return brightnessvalue;
    }
}
