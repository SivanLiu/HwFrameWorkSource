package com.android.server.lights;

import android.graphics.PointF;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.huawei.displayengine.IDisplayEngineServiceEx;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwNormalizedBrightnessMapping {
    private static final int DEFAULT_BRIGHTNESS = -1;
    private static final boolean HWDEBUG = (Log.HWLog || (Log.HWModuleLog && Log.isLoggable("HwNormalizedBrightnessMapping", 3)));
    private static final boolean HWFLOW;
    private static final String TAG = "HwNormalizedBrightnessMapping";
    private static final String XML_EXT = ".xml";
    private static final String XML_NAME_NOEXT = "HwNormalizedBrightnessMapping";
    private int mBrightnessAfterMapMax = -1;
    private int mBrightnessAfterMapMaxForManufacture = -1;
    private int mBrightnessAfterMapMin = -1;
    private int mBrightnessAfterMapMinForManufacture = -1;
    private int mBrightnessBeforeMapMax = -1;
    private int mBrightnessBeforeMapMin = -1;
    private boolean mConfigLoaded = false;
    private List<PointF> mMappingLinePointsList;
    private boolean mNeedBrightnessMappingEnable = false;

    static {
        boolean z = false;
        if (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable("HwNormalizedBrightnessMapping", 4))) {
            z = true;
        }
        HWFLOW = z;
    }

    public HwNormalizedBrightnessMapping(int brightnessBeforeMapMin, int brightnessBeforeMapMax, int brightnessAfterMapMin, int brightnessAfterMapMax) {
        this.mBrightnessBeforeMapMin = brightnessBeforeMapMin;
        this.mBrightnessBeforeMapMax = brightnessBeforeMapMax;
        this.mBrightnessAfterMapMin = brightnessAfterMapMin;
        this.mBrightnessAfterMapMax = brightnessAfterMapMax;
        this.mBrightnessAfterMapMinForManufacture = brightnessAfterMapMin;
        this.mBrightnessAfterMapMaxForManufacture = brightnessAfterMapMax;
    }

    public boolean needBrightnessMappingEnable() {
        if (!this.mConfigLoaded) {
            configLoaded();
        }
        return this.mNeedBrightnessMappingEnable;
    }

    private void configLoaded() {
        boolean brightnessMappingEnable = false;
        try {
            if (!getConfig()) {
                Slog.i("HwNormalizedBrightnessMapping", "initBrightnessMapping,no need BrightnessMapping");
                loadDefaultConfig();
            } else {
                brightnessMappingEnable = true;
                Slog.i("HwNormalizedBrightnessMapping", "initBrightnessMapping,need BrightnessMapping,minB=" + this.mBrightnessBeforeMapMin + ",maxB=" + this.mBrightnessBeforeMapMax + ",min=" + this.mBrightnessAfterMapMin + ",max=" + this.mBrightnessAfterMapMax);
            }
        } catch (IOException e) {
            Slog.e("HwNormalizedBrightnessMapping", "initBrightnessMapping IOException: No need BrightnessMapping");
            loadDefaultConfig();
        }
        this.mConfigLoaded = true;
        this.mNeedBrightnessMappingEnable = brightnessMappingEnable;
    }

    private boolean getConfig() throws IOException {
        File xmlFile = getXmlFile();
        if (xmlFile == null) {
            return false;
        }
        FileInputStream inputStream = null;
        try {
            FileInputStream inputStream2 = new FileInputStream(xmlFile);
            if (!getConfigFromXML(inputStream2) || !checkConfigLoadedFromXML()) {
                try {
                    inputStream2.close();
                } catch (IOException e) {
                    Slog.e("HwNormalizedBrightnessMapping", "getConfig inputStream: IOException");
                }
                return false;
            }
            printConfigFromXML();
            try {
                inputStream2.close();
            } catch (IOException e2) {
                Slog.e("HwNormalizedBrightnessMapping", "getConfig inputStream: IOException");
            }
            return true;
        } catch (FileNotFoundException e3) {
            Slog.e("HwNormalizedBrightnessMapping", "getConfig : FileNotFoundException");
            if (0 != 0) {
                inputStream.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    Slog.e("HwNormalizedBrightnessMapping", "getConfig inputStream: IOException");
                }
            }
            throw th;
        }
    }

    private File getXmlFile() {
        String lcdname = getLcdPanelName();
        String lcdversion = getVersionFromLCD();
        ArrayList<String> xmlPathList = new ArrayList<>();
        if (lcdversion != null) {
            xmlPathList.add(String.format("/xml/lcd/%s_%s_%s%s", "HwNormalizedBrightnessMapping", lcdname, lcdversion, XML_EXT));
        }
        xmlPathList.add(String.format("/xml/lcd/%s_%s%s", "HwNormalizedBrightnessMapping", lcdname, XML_EXT));
        xmlPathList.add(String.format("/xml/lcd/%s%s", "HwNormalizedBrightnessMapping", XML_EXT));
        File xmlFile = null;
        int listSize = xmlPathList.size();
        for (int i = 0; i < listSize; i++) {
            String xmlPath = xmlPathList.get(i);
            xmlFile = HwCfgFilePolicy.getCfgFile(xmlPath, 2);
            if (xmlFile != null) {
                if (HWDEBUG) {
                    Slog.i("HwNormalizedBrightnessMapping", "lcdname=" + lcdname + ",lcdversion=" + lcdversion + ",xmlPath=" + xmlPath);
                }
                return xmlFile;
            }
        }
        return xmlFile;
    }

    private String getLcdPanelName() {
        IBinder binder = ServiceManager.getService("DisplayEngineExService");
        if (binder == null) {
            Slog.w("HwNormalizedBrightnessMapping", "getLcdPanelName() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx mService = IDisplayEngineServiceEx.Stub.asInterface(binder);
        if (mService == null) {
            Slog.w("HwNormalizedBrightnessMapping", "getLcdPanelName() mService is null!");
            return null;
        }
        byte[] name = new byte[128];
        try {
            int ret = mService.getEffect(14, 0, name, name.length);
            if (ret != 0) {
                Slog.e("HwNormalizedBrightnessMapping", "getLcdPanelName() getEffect failed! ret=" + ret);
                return null;
            }
            String panelName = null;
            try {
                panelName = new String(name, "UTF-8").trim().replace(' ', '_');
            } catch (UnsupportedEncodingException e) {
                Slog.e("HwNormalizedBrightnessMapping", "Unsupported encoding type!");
            }
            if (HWDEBUG) {
                Slog.i("HwNormalizedBrightnessMapping", "getLcdPanelName() panelName=" + panelName);
            }
            return panelName;
        } catch (RemoteException e2) {
            Slog.e("HwNormalizedBrightnessMapping", "getLcdPanelName() RemoteException " + e2);
            return null;
        }
    }

    private String getVersionFromLCD() {
        IBinder binder = ServiceManager.getService("DisplayEngineExService");
        if (binder == null) {
            Slog.w("HwNormalizedBrightnessMapping", "getVersionFromLCD() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx mService = IDisplayEngineServiceEx.Stub.asInterface(binder);
        if (mService == null) {
            Slog.w("HwNormalizedBrightnessMapping", "getVersionFromLCD() mService is null!");
            return null;
        }
        byte[] name = new byte[32];
        try {
            int ret = mService.getEffect(14, 3, name, name.length);
            if (ret != 0) {
                Slog.e("HwNormalizedBrightnessMapping", "getVersionFromLCD() getEffect failed! ret=" + ret);
                return null;
            }
            String panelVersion = null;
            try {
                String lcdVersion = new String(name, "UTF-8").trim();
                int index = lcdVersion.indexOf("VER:");
                if (index != -1) {
                    panelVersion = lcdVersion.substring("VER:".length() + index);
                }
            } catch (UnsupportedEncodingException e) {
                Slog.e("HwNormalizedBrightnessMapping", "Unsupported encoding type!");
            }
            if (HWFLOW) {
                Slog.i("HwNormalizedBrightnessMapping", "getVersionFromLCD() panelVersion=" + panelVersion);
            }
            return panelVersion;
        } catch (RemoteException e2) {
            Slog.e("HwNormalizedBrightnessMapping", "getVersionFromLCD() RemoteException " + e2);
            return null;
        }
    }

    private boolean getConfigFromXML(InputStream inStream) {
        boolean mappingBrighnessLinePointsListsLoadStarted = false;
        boolean mappingBrighnessLinePointsListLoaded = false;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType == 2) {
                    String name = parser.getName();
                    if (name.equals("BrightnessAfterMapMinForManufacture")) {
                        this.mBrightnessAfterMapMinForManufacture = Integer.parseInt(parser.nextText());
                    } else if (name.equals("BrightnessAfterMapMaxForManufacture")) {
                        this.mBrightnessAfterMapMaxForManufacture = Integer.parseInt(parser.nextText());
                    } else if (name.equals("MappingBrightnessPoints")) {
                        mappingBrighnessLinePointsListsLoadStarted = true;
                    } else if (name.equals("Point") && mappingBrighnessLinePointsListsLoadStarted) {
                        PointF currentPoint = new PointF();
                        String[] stringValue = parser.nextText().split(",");
                        if (stringValue.length == 2) {
                            currentPoint.x = Float.parseFloat(stringValue[0]);
                            currentPoint.y = Float.parseFloat(stringValue[1]);
                            if (!checkPointIsOK(currentPoint.x, currentPoint.y)) {
                                return false;
                            }
                            if (this.mMappingLinePointsList == null) {
                                this.mMappingLinePointsList = new ArrayList();
                            }
                            this.mMappingLinePointsList.add(currentPoint);
                        } else {
                            Slog.w("HwNormalizedBrightnessMapping", "stringValue.length is not ok,len=" + stringValue.length);
                            return false;
                        }
                    }
                } else if (eventType == 3 && parser.getName().equals("MappingBrightnessPoints")) {
                    mappingBrighnessLinePointsListsLoadStarted = false;
                    if (this.mMappingLinePointsList != null) {
                        mappingBrighnessLinePointsListLoaded = true;
                    } else {
                        Slog.e("HwNormalizedBrightnessMapping", "no MappingBrightnessPoints loaded!");
                        return false;
                    }
                }
            }
            if (mappingBrighnessLinePointsListLoaded) {
                if (HWFLOW) {
                    Slog.i("HwNormalizedBrightnessMapping", "getConfigFromeXML success!");
                }
                return true;
            }
        } catch (XmlPullParserException e) {
            Slog.e("HwNormalizedBrightnessMapping", "getConfigFromXML : XmlPullParserException");
        } catch (IOException e2) {
            Slog.e("HwNormalizedBrightnessMapping", "getConfigFromXML : IOException");
        } catch (NumberFormatException e3) {
            Slog.e("HwNormalizedBrightnessMapping", "getConfigFromXML : NumberFormatException");
        }
        Slog.w("HwNormalizedBrightnessMapping", "getConfigFromeXML false!");
        return false;
    }

    private boolean checkPointIsOK(float pointX, float pointY) {
        if (pointX < ((float) this.mBrightnessBeforeMapMin)) {
            Slog.w("HwNormalizedBrightnessMapping", "pointX < mBrightnessBeforeMapMin,pointX=" + pointX + ",mBrightnessBeforeMapMin=" + this.mBrightnessBeforeMapMin);
            return false;
        } else if (pointX > ((float) this.mBrightnessBeforeMapMax)) {
            Slog.w("HwNormalizedBrightnessMapping", "pointX > mBrightnessBeforeMapMax,pointX=" + pointX + ",mBrightnessBeforeMapMax=" + this.mBrightnessBeforeMapMax);
            return false;
        } else if (pointY < ((float) this.mBrightnessAfterMapMin)) {
            Slog.w("HwNormalizedBrightnessMapping", "pointY < mBrightnessAfterMapMin,pointY=" + pointY + ",mBrightnessAfterMapMin=" + this.mBrightnessAfterMapMin);
            return false;
        } else if (pointY <= ((float) this.mBrightnessAfterMapMax)) {
            return true;
        } else {
            Slog.w("HwNormalizedBrightnessMapping", "pointY > mBrightnessAfterMapMax,pointY=" + pointY + ",mBrightnessAfterMapMax=" + this.mBrightnessAfterMapMax);
            return false;
        }
    }

    private void loadDefaultConfig() {
        this.mBrightnessAfterMapMin = -1;
        this.mBrightnessAfterMapMax = -1;
        this.mBrightnessAfterMapMinForManufacture = -1;
        this.mBrightnessAfterMapMaxForManufacture = -1;
        List<PointF> list = this.mMappingLinePointsList;
        if (list != null) {
            list.clear();
        }
    }

    private boolean checkConfigLoadedFromXML() {
        int i;
        int i2 = this.mBrightnessAfterMapMinForManufacture;
        int i3 = this.mBrightnessAfterMapMin;
        if (i2 < i3 || i2 > (i = this.mBrightnessAfterMapMax)) {
            Slog.w("HwNormalizedBrightnessMapping", "MinForManufacture is wrong,=" + this.mBrightnessAfterMapMinForManufacture);
            return false;
        }
        int i4 = this.mBrightnessAfterMapMaxForManufacture;
        if (i4 < i3 || i4 > i) {
            Slog.w("HwNormalizedBrightnessMapping", "MaxForManufacture is wrong,=" + this.mBrightnessAfterMapMaxForManufacture);
            return false;
        } else if (i2 >= i4) {
            Slog.w("HwNormalizedBrightnessMapping", "MinMaxForManufacture is wrong,min=" + this.mBrightnessAfterMapMinForManufacture + ",max=" + this.mBrightnessAfterMapMaxForManufacture);
            return false;
        } else if (checkPointsListIsOK(this.mMappingLinePointsList)) {
            return true;
        } else {
            Slog.w("HwNormalizedBrightnessMapping", "checkPointsList mMappingLinePointsList is wrong!");
            return false;
        }
    }

    private boolean checkPointsListIsOK(List<PointF> linePointsList) {
        if (linePointsList == null) {
            Slog.e("HwNormalizedBrightnessMapping", "LoadXML false for linePointsListIn == null");
            return false;
        } else if (linePointsList.size() <= 1 || linePointsList.size() >= 100) {
            Slog.e("HwNormalizedBrightnessMapping", "LoadXML false for linePointsListIn number is wrong");
            return false;
        } else {
            PointF lastPoint = null;
            for (PointF pointItem : linePointsList) {
                if (lastPoint == null) {
                    lastPoint = pointItem;
                } else if (lastPoint.x >= pointItem.x) {
                    Slog.e("HwNormalizedBrightnessMapping", "LoadXML false,lastPoint.x=" + lastPoint.x + ",tmpPoint.x=" + pointItem.x);
                    return false;
                } else if (lastPoint.y > pointItem.y) {
                    Slog.e("HwNormalizedBrightnessMapping", "LoadXML false,lastPoint.y=" + lastPoint.y + ",tmpPoint.y=" + pointItem.y);
                    return false;
                } else {
                    lastPoint = pointItem;
                }
            }
            return true;
        }
    }

    private void printConfigFromXML() {
        if (HWFLOW) {
            Slog.i("HwNormalizedBrightnessMapping", "minForManufacture=" + this.mBrightnessAfterMapMinForManufacture + ",maxForManufacture=" + this.mBrightnessAfterMapMaxForManufacture);
            StringBuilder sb = new StringBuilder();
            sb.append("mMappingLinePointsList=");
            sb.append(this.mMappingLinePointsList);
            Slog.i("HwNormalizedBrightnessMapping", sb.toString());
        }
    }

    public int getMappingBrightness(int level) {
        int i;
        int i2;
        List<PointF> list = this.mMappingLinePointsList;
        if (list == null || (i = this.mBrightnessAfterMapMin) == -1 || (i2 = this.mBrightnessAfterMapMax) == -1 || i == i2) {
            return -1;
        }
        float mappingBrightness = -1.0f;
        PointF temp1 = null;
        Iterator<PointF> it = list.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PointF pointItem = it.next();
            if (temp1 == null) {
                temp1 = pointItem;
            }
            if (((float) level) >= pointItem.x) {
                temp1 = pointItem;
                mappingBrightness = temp1.y;
            } else if (pointItem.x <= temp1.x) {
                if (HWFLOW) {
                    Slog.d("HwNormalizedBrightnessMapping", "mapping_temp1.x <= temp2.x,x" + pointItem.x + ",y = " + pointItem.y);
                }
                return -1;
            } else {
                mappingBrightness = (((pointItem.y - temp1.y) * (((float) level) - temp1.x)) / (pointItem.x - temp1.x)) + temp1.y;
            }
        }
        if (HWDEBUG) {
            Slog.d("HwNormalizedBrightnessMapping", "levelBeforeMap=" + level + ",mappingBrightness=" + mappingBrightness);
        }
        return getValidBrightness((int) (0.5f + mappingBrightness));
    }

    private int getValidBrightness(int brightness) {
        int i = this.mBrightnessAfterMapMin;
        if (brightness < i) {
            return i;
        }
        int i2 = this.mBrightnessAfterMapMax;
        if (brightness > i2) {
            return i2;
        }
        return brightness;
    }

    public int getMappingBrightnessForManufacture(int level) {
        int i;
        float brightnessIn = (float) level;
        int i2 = this.mBrightnessAfterMapMinForManufacture;
        if (i2 == -1 || (i = this.mBrightnessAfterMapMaxForManufacture) == -1 || i2 == i) {
            return -1;
        }
        int i3 = this.mBrightnessBeforeMapMin;
        float mappingBrightness = ((float) i2) + (((brightnessIn - ((float) i3)) * ((float) (i - i2))) / ((float) (this.mBrightnessBeforeMapMax - i3)));
        if (HWDEBUG) {
            Slog.d("HwNormalizedBrightnessMapping", "levelBeforeMap=" + level + ",mappingBrightnessForManufacture=" + mappingBrightness);
        }
        return getValidBrightnessForManufacture((int) (0.5f + mappingBrightness));
    }

    private int getValidBrightnessForManufacture(int brightness) {
        int i = this.mBrightnessAfterMapMinForManufacture;
        if (brightness < i) {
            return i;
        }
        int i2 = this.mBrightnessAfterMapMaxForManufacture;
        if (brightness > i2) {
            return i2;
        }
        return brightness;
    }

    public int getMappingBrightnessHighPrecision(int level) {
        int i;
        int i2;
        List<PointF> list = this.mMappingLinePointsList;
        if (list == null || (i = this.mBrightnessAfterMapMin) == -1 || (i2 = this.mBrightnessAfterMapMax) == -1 || this.mBrightnessBeforeMapMin == -1 || this.mBrightnessBeforeMapMax == -1 || i == i2) {
            return level;
        }
        float mappingBrightness = (float) level;
        float tempBrightness = (float) level;
        int listSize = list.size();
        int i3 = 1;
        while (true) {
            if (i3 >= listSize) {
                break;
            }
            PointF temp1 = this.mMappingLinePointsList.get(i3 - 1);
            PointF temp2 = this.mMappingLinePointsList.get(i3);
            if (((float) level) < temp1.x || ((float) level) > temp2.x) {
                i3++;
            } else if (temp1.x >= temp2.x) {
                Slog.w("HwNormalizedBrightnessMapping", "origlevel=" + level + ",temp1.x=" + temp1.x + " >= temp2.x=" + temp2.x);
                return level;
            } else {
                tempBrightness = (((((float) level) - temp1.x) * (temp2.y - temp1.y)) / (temp2.x - temp1.x)) + temp1.y;
                int i4 = this.mBrightnessAfterMapMin;
                int i5 = this.mBrightnessBeforeMapMax;
                int i6 = this.mBrightnessBeforeMapMin;
                mappingBrightness = (((tempBrightness - ((float) i4)) * ((float) (i5 - i6))) / ((float) (this.mBrightnessAfterMapMax - i4))) + ((float) i6);
            }
        }
        if (HWDEBUG) {
            Slog.d("HwNormalizedBrightnessMapping", "level=" + level + ",mappingBrightnessHigh=" + mappingBrightness + ",tempBrightness=" + tempBrightness);
        }
        return getValidBrightnessHighPrecision((int) (0.5f + mappingBrightness));
    }

    private int getValidBrightnessHighPrecision(int brightness) {
        int i = this.mBrightnessBeforeMapMin;
        if (brightness < i) {
            return i;
        }
        int i2 = this.mBrightnessBeforeMapMax;
        if (brightness > i2) {
            return i2;
        }
        return brightness;
    }
}
