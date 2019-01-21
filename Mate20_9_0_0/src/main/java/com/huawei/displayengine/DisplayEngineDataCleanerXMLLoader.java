package com.huawei.displayengine;

import android.rms.iaware.AppTypeInfo;
import android.util.Log;
import android.util.Slog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class DisplayEngineDataCleanerXMLLoader {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String TAG = "DisplayEngineDataCleanerXMLLoader";
    private static Data mData = new Data();
    private static DisplayEngineDataCleanerXMLLoader mLoader;
    private static final Object mLock = new Object();

    public static class Data implements Cloneable {
        public int alDarkThresh = 10;
        public ArrayList<Integer> ambientLightLUT;
        public ArrayList<Float> brightnessLevelLUT;
        public float comfortZoneCounterWeight = 0.2f;
        public float counterWeightThresh = 0.5f;
        public ArrayList<Integer> darkLevelLUT;
        public ArrayList<Float> darkLevelRoofLUT;
        public int hbmTresh = 3000;
        public int outDoorLevelFloor = 139;
        public float outlierZoneCounterWeight = 0.5f;
        public float safeZoneCounterWeight = 0.3f;

        protected Object clone() throws CloneNotSupportedException {
            Data newData = (Data) super.clone();
            newData.ambientLightLUT = cloneIntegerList(this.ambientLightLUT);
            newData.brightnessLevelLUT = cloneFloatList(this.brightnessLevelLUT);
            newData.darkLevelLUT = cloneIntegerList(this.darkLevelLUT);
            newData.darkLevelRoofLUT = cloneFloatList(this.darkLevelRoofLUT);
            return newData;
        }

        private ArrayList<Integer> cloneIntegerList(ArrayList<Integer> list) {
            if (list == null) {
                return null;
            }
            try {
                return new ArrayList(list);
            } catch (Exception e) {
                String str = DisplayEngineDataCleanerXMLLoader.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cloneList() error!");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
                return list;
            }
        }

        private ArrayList<Float> cloneFloatList(ArrayList<Float> list) {
            if (list == null) {
                return null;
            }
            try {
                return new ArrayList(list);
            } catch (Exception e) {
                String str = DisplayEngineDataCleanerXMLLoader.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cloneList() error!");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
                return list;
            }
        }

        public void printData() {
            if (DisplayEngineDataCleanerXMLLoader.HWFLOW) {
                String str = DisplayEngineDataCleanerXMLLoader.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("printData() comfortZoneCounterWeight=");
                stringBuilder.append(this.comfortZoneCounterWeight);
                stringBuilder.append(", safeZoneCounterWeight=");
                stringBuilder.append(this.safeZoneCounterWeight);
                stringBuilder.append(", outlierZoneCounterWeight=");
                stringBuilder.append(this.outlierZoneCounterWeight);
                stringBuilder.append(", counterWeightThresh=");
                stringBuilder.append(this.counterWeightThresh);
                Slog.i(str, stringBuilder.toString());
                str = DisplayEngineDataCleanerXMLLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("alDarkThresh=");
                stringBuilder.append(this.alDarkThresh);
                stringBuilder.append(", hbmTresh=");
                stringBuilder.append(this.hbmTresh);
                stringBuilder.append(", outDoorLevelFloor=");
                stringBuilder.append(this.outDoorLevelFloor);
                Slog.i(str, stringBuilder.toString());
                str = DisplayEngineDataCleanerXMLLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ambientLightLUT=");
                stringBuilder.append(this.ambientLightLUT);
                Slog.i(str, stringBuilder.toString());
                str = DisplayEngineDataCleanerXMLLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("brightnessLevelLUT=");
                stringBuilder.append(this.brightnessLevelLUT);
                Slog.i(str, stringBuilder.toString());
                str = DisplayEngineDataCleanerXMLLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("darkLevelLUT=");
                stringBuilder.append(this.darkLevelLUT);
                Slog.i(str, stringBuilder.toString());
                str = DisplayEngineDataCleanerXMLLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("darkLevelRoofLUT=");
                stringBuilder.append(this.darkLevelRoofLUT);
                Slog.i(str, stringBuilder.toString());
            }
        }

        public void loadDefaultConfig() {
            if (DisplayEngineDataCleanerXMLLoader.HWFLOW) {
                Slog.i(DisplayEngineDataCleanerXMLLoader.TAG, "loadDefaultConfig()");
            }
            this.comfortZoneCounterWeight = 0.2f;
            this.safeZoneCounterWeight = 0.3f;
            this.outlierZoneCounterWeight = 0.5f;
            this.counterWeightThresh = 0.5f;
            this.alDarkThresh = 10;
            this.hbmTresh = 3000;
            this.outDoorLevelFloor = 139;
            if (this.ambientLightLUT != null) {
                this.ambientLightLUT.clear();
            }
            this.ambientLightLUT = new ArrayList(Arrays.asList(new Integer[]{Integer.valueOf(0), Integer.valueOf(2), Integer.valueOf(5), Integer.valueOf(10), Integer.valueOf(15), Integer.valueOf(20), Integer.valueOf(30), Integer.valueOf(50), Integer.valueOf(70), Integer.valueOf(100), Integer.valueOf(150), Integer.valueOf(200), Integer.valueOf(250), Integer.valueOf(AppTypeInfo.PG_TYPE_BASE), Integer.valueOf(350), Integer.valueOf(400), Integer.valueOf(500), Integer.valueOf(600), Integer.valueOf(700), Integer.valueOf(800), Integer.valueOf(900), Integer.valueOf(1000), Integer.valueOf(1200), Integer.valueOf(1400), Integer.valueOf(1800), Integer.valueOf(2400), Integer.valueOf(3000), Integer.valueOf(4000), Integer.valueOf(5000), Integer.valueOf(6000), Integer.valueOf(8000), Integer.valueOf(10000), Integer.valueOf(15000), Integer.valueOf(20000), Integer.valueOf(30000)}));
            if (this.brightnessLevelLUT != null) {
                this.brightnessLevelLUT.clear();
            }
            this.brightnessLevelLUT = new ArrayList(Arrays.asList(new Float[]{Float.valueOf(5.0f), Float.valueOf(6.7f), Float.valueOf(9.25f), Float.valueOf(13.5f), Float.valueOf(17.75f), Float.valueOf(22.0f), Float.valueOf(26.333f), Float.valueOf(35.0f), Float.valueOf(35.6316f), Float.valueOf(36.5789f), Float.valueOf(38.1579f), Float.valueOf(39.7368f), Float.valueOf(41.3158f), Float.valueOf(42.8947f), Float.valueOf(44.4737f), Float.valueOf(46.0526f), Float.valueOf(49.2105f), Float.valueOf(52.3684f), Float.valueOf(55.5263f), Float.valueOf(58.6824f), Float.valueOf(61.8421f), Float.valueOf(65.0f), Float.valueOf(71.3f), Float.valueOf(77.6f), Float.valueOf(90.2f), Float.valueOf(109.1f), Float.valueOf(128.0f), Float.valueOf(158.0f), Float.valueOf(160.8333f), Float.valueOf(163.6667f), Float.valueOf(169.3333f), Float.valueOf(175.0f), Float.valueOf(195.0f), Float.valueOf(215.0f), Float.valueOf(255.0f)}));
            if (this.darkLevelLUT != null) {
                this.darkLevelLUT.clear();
            }
            this.darkLevelLUT = new ArrayList(Arrays.asList(new Integer[]{Integer.valueOf(5), Integer.valueOf(6), Integer.valueOf(7), Integer.valueOf(8), Integer.valueOf(9), Integer.valueOf(10), Integer.valueOf(11), Integer.valueOf(12), Integer.valueOf(13), Integer.valueOf(14), Integer.valueOf(15), Integer.valueOf(16)}));
            if (this.darkLevelRoofLUT != null) {
                this.darkLevelRoofLUT.clear();
            }
            this.darkLevelRoofLUT = new ArrayList(Arrays.asList(new Float[]{Float.valueOf(6.0f), Float.valueOf(7.0f), Float.valueOf(9.0f), Float.valueOf(10.0f), Float.valueOf(12.0f), Float.valueOf(13.0f), Float.valueOf(15.0f), Float.valueOf(16.0f), Float.valueOf(18.0f), Float.valueOf(19.0f), Float.valueOf(21.0f), Float.valueOf(22.0f)}));
        }
    }

    private static class Element_AlDarkThresh extends HwXmlElement {
        private Element_AlDarkThresh() {
        }

        public String getName() {
            return "AlDarkThresh";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.alDarkThresh = Integer.parseInt(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return DisplayEngineDataCleanerXMLLoader.mData.alDarkThresh >= 0;
        }
    }

    private static class Element_AmbientLightLUT extends HwXmlElement {
        private Element_AmbientLightLUT() {
        }

        public String getName() {
            return "AmbientLightLUT";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.ambientLightLUT = DisplayEngineDataCleanerXMLLoader.parseIntegerList(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return DisplayEngineDataCleanerXMLLoader.mData.ambientLightLUT.size() > 0;
        }
    }

    private static class Element_BrightnessLevelLUT extends HwXmlElement {
        private Element_BrightnessLevelLUT() {
        }

        public String getName() {
            return "BrightnessLevelLUT";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.brightnessLevelLUT = DisplayEngineDataCleanerXMLLoader.parseFloatList(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return DisplayEngineDataCleanerXMLLoader.mData.brightnessLevelLUT.size() > 0;
        }
    }

    private static class Element_ComfortZoneCounterWeight extends HwXmlElement {
        private Element_ComfortZoneCounterWeight() {
        }

        public String getName() {
            return "ComfortZoneCounterWeight";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.comfortZoneCounterWeight = Float.parseFloat(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return Float.compare(DisplayEngineDataCleanerXMLLoader.mData.comfortZoneCounterWeight, 0.0f) >= 0 && Float.compare(DisplayEngineDataCleanerXMLLoader.mData.comfortZoneCounterWeight, 1.0f) <= 0;
        }
    }

    private static class Element_CounterWeightThresh extends HwXmlElement {
        private Element_CounterWeightThresh() {
        }

        public String getName() {
            return "CounterWeightThresh";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.counterWeightThresh = Float.parseFloat(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return Float.compare(DisplayEngineDataCleanerXMLLoader.mData.counterWeightThresh, 0.0f) >= 0 && Float.compare(DisplayEngineDataCleanerXMLLoader.mData.counterWeightThresh, 1.0f) <= 0;
        }
    }

    private static class Element_DarkLevelLUT extends HwXmlElement {
        private Element_DarkLevelLUT() {
        }

        public String getName() {
            return "DarkLevelLUT";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.darkLevelLUT = DisplayEngineDataCleanerXMLLoader.parseIntegerList(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return DisplayEngineDataCleanerXMLLoader.mData.darkLevelLUT.size() > 0;
        }
    }

    private static class Element_DarkLevelRoofLUT extends HwXmlElement {
        private Element_DarkLevelRoofLUT() {
        }

        public String getName() {
            return "DarkLevelRoofLUT";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.darkLevelRoofLUT = DisplayEngineDataCleanerXMLLoader.parseFloatList(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return DisplayEngineDataCleanerXMLLoader.mData.darkLevelRoofLUT.size() > 0;
        }
    }

    private static class Element_DataCleanerConfig extends HwXmlElement {
        private Element_DataCleanerConfig() {
        }

        public String getName() {
            return "DataCleanerConfig";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_HBMThresh extends HwXmlElement {
        private Element_HBMThresh() {
        }

        public String getName() {
            return "HBMThresh";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.hbmTresh = Integer.parseInt(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return DisplayEngineDataCleanerXMLLoader.mData.hbmTresh >= 0;
        }
    }

    private static class Element_OutDoorLevelThresh extends HwXmlElement {
        private Element_OutDoorLevelThresh() {
        }

        public String getName() {
            return "OutDoorLevelThresh";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.outDoorLevelFloor = Integer.parseInt(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return DisplayEngineDataCleanerXMLLoader.mData.outDoorLevelFloor >= 0;
        }
    }

    private static class Element_OutlierZoneCounterWeight extends HwXmlElement {
        private Element_OutlierZoneCounterWeight() {
        }

        public String getName() {
            return "OutlierZoneCounterWeight";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.outlierZoneCounterWeight = Float.parseFloat(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return Float.compare(DisplayEngineDataCleanerXMLLoader.mData.outlierZoneCounterWeight, 0.0f) >= 0 && Float.compare(DisplayEngineDataCleanerXMLLoader.mData.outlierZoneCounterWeight, 1.0f) <= 0;
        }
    }

    private static class Element_SafeZoneCounterWeight extends HwXmlElement {
        private Element_SafeZoneCounterWeight() {
        }

        public String getName() {
            return "SafeZoneCounterWeight";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            DisplayEngineDataCleanerXMLLoader.mData.safeZoneCounterWeight = Float.parseFloat(parser.nextText());
            return true;
        }

        protected boolean isOptional() {
            return false;
        }

        protected boolean checkValue() {
            return Float.compare(DisplayEngineDataCleanerXMLLoader.mData.safeZoneCounterWeight, 0.0f) >= 0 && Float.compare(DisplayEngineDataCleanerXMLLoader.mData.safeZoneCounterWeight, 1.0f) <= 0;
        }
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:9:0x001a, B:14:0x0027] */
    /* JADX WARNING: Missing block: B:12:0x0024, code skipped:
            r2 = th;
     */
    /* JADX WARNING: Missing block: B:21:0x0047, code skipped:
            if (null == null) goto L_0x0049;
     */
    /* JADX WARNING: Missing block: B:22:0x0049, code skipped:
            new com.huawei.displayengine.DisplayEngineDataCleanerXMLLoader.Data().loadDefaultConfig();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static Data getData(String xmlFilePath) {
        Data retData = null;
        synchronized (mLock) {
            Data th;
            try {
                if (mLoader == null) {
                    mLoader = new DisplayEngineDataCleanerXMLLoader(xmlFilePath);
                }
                retData = (Data) mData.clone();
                if (retData == null) {
                    th = new Data();
                    retData = th;
                    retData.loadDefaultConfig();
                }
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getData() error!");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
                if (null == null) {
                    th = new Data();
                }
            }
        }
        return retData;
    }

    private DisplayEngineDataCleanerXMLLoader(String xmlFilePath) {
        if (HWDEBUG) {
            Slog.d(TAG, "DisplayEngineDataCleanerXMLLoader()");
        }
        if (!parseXml(xmlFilePath)) {
            mData.loadDefaultConfig();
        }
        mData.printData();
    }

    private boolean parseXml(String xmlFilePath) {
        if (xmlFilePath == null) {
            Slog.e(TAG, "parseXml() error! xml file path is null");
            return false;
        }
        HwXmlParser xmlParser = new HwXmlParser(xmlFilePath);
        registerElement(xmlParser);
        if (!xmlParser.parse()) {
            Slog.e(TAG, "parseXml() error! xmlParser.parse() failed!");
            return false;
        } else if (xmlParser.check()) {
            if (HWFLOW) {
                Slog.i(TAG, "parseXml() load success!");
            }
            return true;
        } else {
            Slog.e(TAG, "parseXml() error! xmlParser.check() failed!");
            return false;
        }
    }

    private void registerElement(HwXmlParser parser) {
        HwXmlElement rootElement = parser.registerRootElement(new Element_DataCleanerConfig());
        rootElement.registerChildElement(new Element_ComfortZoneCounterWeight());
        rootElement.registerChildElement(new Element_SafeZoneCounterWeight());
        rootElement.registerChildElement(new Element_OutlierZoneCounterWeight());
        rootElement.registerChildElement(new Element_CounterWeightThresh());
        rootElement.registerChildElement(new Element_AlDarkThresh());
        rootElement.registerChildElement(new Element_HBMThresh());
        rootElement.registerChildElement(new Element_OutDoorLevelThresh());
        rootElement.registerChildElement(new Element_AmbientLightLUT());
        rootElement.registerChildElement(new Element_BrightnessLevelLUT());
        rootElement.registerChildElement(new Element_DarkLevelLUT());
        rootElement.registerChildElement(new Element_DarkLevelRoofLUT());
    }

    private static ArrayList<Float> parseFloatList(String srcString) {
        if (srcString == null) {
            return null;
        }
        String[] s = srcString.split(",");
        ArrayList<Float> parsedList = new ArrayList();
        for (String parseFloat : s) {
            parsedList.add(Float.valueOf(Float.parseFloat(parseFloat)));
        }
        return parsedList;
    }

    private static ArrayList<Integer> parseIntegerList(String srcString) {
        if (srcString == null) {
            return null;
        }
        String[] s = srcString.split(",");
        ArrayList<Integer> parsedList = new ArrayList();
        for (String parseInt : s) {
            parsedList.add(Integer.valueOf(Integer.parseInt(parseInt)));
        }
        return parsedList;
    }
}
