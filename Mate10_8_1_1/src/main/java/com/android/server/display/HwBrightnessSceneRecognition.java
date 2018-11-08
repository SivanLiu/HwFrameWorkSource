package com.android.server.display;

import android.util.Xml;
import com.huawei.displayengine.DElog;
import com.huawei.displayengine.DisplayEngineManager;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwBrightnessSceneRecognition {
    private static final String CONTROL_XML_FILE = "/display/effect/displayengine/LABC_SR_control.xml";
    private static final String TAG = "DE J HwBrightnessSceneRecognition";
    private DisplayEngineManager mDisplayEngineManager = new DisplayEngineManager();
    private boolean mEnable;
    private Map<String, Integer> mGameBrightnessLevelWhiteList = new HashMap();
    private int mGameState = -1;
    private boolean mIsScreenOn = true;
    private Object mLockGameState = new Object();
    private Object mLockScreenState = new Object();
    private boolean mScreenOffStateCleanFlag = true;

    public HwBrightnessSceneRecognition() {
        getConfigParam();
    }

    private void setDefaultConfigValue() {
        this.mEnable = false;
    }

    private void printConfigValue() {
        DElog.i(TAG, "printConfigValue: mEnable = " + this.mEnable);
        if (this.mGameBrightnessLevelWhiteList != null) {
            for (Entry<String, Integer> entry : this.mGameBrightnessLevelWhiteList.entrySet()) {
                DElog.i(TAG, "printConfigValue: mGameBrightnessLevelWhiteList = " + ((String) entry.getKey()) + ", " + entry.getValue());
            }
            return;
        }
        DElog.i(TAG, "printConfigValue: mGameBrightnessLevelWhiteList is null.");
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
        RuntimeException e;
        FileNotFoundException e2;
        IOException e3;
        Exception e4;
        Throwable th;
        DElog.i(TAG, "getConfig");
        File xmlFile = HwCfgFilePolicy.getCfgFile(CONTROL_XML_FILE, 0);
        if (xmlFile == null) {
            DElog.w(TAG, "get xmlFile :/display/effect/displayengine/LABC_SR_control.xml failed!");
            return false;
        }
        FileInputStream fileInputStream = null;
        try {
            FileInputStream inputStream = new FileInputStream(xmlFile);
            try {
                if (getConfigFromXML(inputStream)) {
                    inputStream.close();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    return true;
                }
                DElog.i(TAG, "get xmlFile error");
                inputStream.close();
                if (inputStream != null) {
                    inputStream.close();
                }
                return false;
            } catch (RuntimeException e5) {
                e = e5;
                fileInputStream = inputStream;
                throw e;
            } catch (FileNotFoundException e6) {
                e2 = e6;
                fileInputStream = inputStream;
                DElog.e(TAG, "get xmlFile error: " + e2);
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return false;
            } catch (IOException e7) {
                e3 = e7;
                fileInputStream = inputStream;
                DElog.e(TAG, "get xmlFile error: " + e3);
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return false;
            } catch (Exception e8) {
                e4 = e8;
                fileInputStream = inputStream;
                try {
                    DElog.e(TAG, "get xmlFile error: " + e4);
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
                fileInputStream = inputStream;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
        } catch (RuntimeException e9) {
            e = e9;
            throw e;
        } catch (FileNotFoundException e10) {
            e2 = e10;
            DElog.e(TAG, "get xmlFile error: " + e2);
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return false;
        } catch (IOException e11) {
            e3 = e11;
            DElog.e(TAG, "get xmlFile error: " + e3);
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return false;
        } catch (Exception e12) {
            e4 = e12;
            DElog.e(TAG, "get xmlFile error: " + e4);
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return false;
        }
    }

    private boolean getConfigFromXML(InputStream inStream) {
        DElog.i(TAG, "getConfigFromeXML");
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
                                if (name.equals("GameBrightnessLevelWhiteList")) {
                                    String[] values = parser.nextText().split(",");
                                    if (values.length == 2) {
                                        this.mGameBrightnessLevelWhiteList.put(values[0], Integer.valueOf(Integer.parseInt(values[1])));
                                        break;
                                    }
                                    DElog.d(TAG, "getConfigFromXML find illegal param, tag name = " + name);
                                    break;
                                }
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
                DElog.i(TAG, "getConfigFromeXML success!");
                return true;
            }
        } catch (XmlPullParserException e) {
            DElog.e(TAG, "get xmlFile error: " + e);
        } catch (IOException e2) {
            DElog.e(TAG, "get xmlFile error: " + e2);
        } catch (NumberFormatException e3) {
            DElog.e(TAG, "get xmlFile error: " + e3);
        } catch (Exception e4) {
            DElog.e(TAG, "get xmlFile error: " + e4);
        }
        DElog.e(TAG, "getConfigFromeXML false!");
        return false;
    }

    public boolean isEnable() {
        return this.mEnable;
    }

    public void notifyTopApkChange(String pkgName) {
        if (pkgName == null || pkgName.length() <= 0) {
            DElog.i(TAG, "pkgName is null || pkgName.length() <= 0!");
        } else {
            notifyGameSceneIfNeeded(pkgName);
        }
    }

    public void notifyScreenStatus(boolean isScreenOn) {
        boolean lastState = this.mIsScreenOn;
        this.mIsScreenOn = isScreenOn;
        if (!this.mIsScreenOn && lastState) {
            synchronized (this.mLockScreenState) {
                this.mScreenOffStateCleanFlag = true;
            }
        }
        DElog.d(TAG, "notifyScreenStatus = " + isScreenOn);
    }

    private void notifyGameSceneIfNeeded(String pkgName) {
        if (this.mIsScreenOn) {
            synchronized (this.mLockScreenState) {
                boolean screenOffCleanFlag = this.mScreenOffStateCleanFlag;
                this.mScreenOffStateCleanFlag = false;
            }
            synchronized (this.mLockGameState) {
                int lastGameState = this.mGameState;
                if (pkgName == null || pkgName.length() <= 0) {
                    this.mGameState = -1;
                } else if (this.mGameBrightnessLevelWhiteList.get(pkgName) != null) {
                    this.mGameState = 1;
                    DElog.d(TAG, "apk " + pkgName + " is in the game whitelist, state = " + this.mGameState);
                } else {
                    this.mGameState = -1;
                    DElog.d(TAG, "apk " + pkgName + " is NOT in the game whitelist, state = " + this.mGameState);
                }
                if (this.mDisplayEngineManager == null) {
                    DElog.w(TAG, "mDisplayEngineManager is null");
                } else if (this.mGameState != lastGameState || screenOffCleanFlag) {
                    if (this.mGameState == 1) {
                        this.mDisplayEngineManager.setScene(33, 16);
                        DElog.d(TAG, "setScene DE_SCENE_GAME DE_ACTION_MODE_ON");
                    } else {
                        this.mDisplayEngineManager.setScene(33, 17);
                        DElog.d(TAG, "setScene DE_SCENE_GAME DE_ACTION_MODE_OFF");
                    }
                }
            }
        }
    }
}
