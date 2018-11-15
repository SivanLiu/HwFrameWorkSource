package com.android.server.display;

import android.graphics.PointF;
import android.util.Log;
import android.util.Slog;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class HwEyeProtectionXmlLoader {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String TAG = "HwEyeProtectionXmlLoader";
    private static Data mData = new Data();
    private static HwEyeProtectionXmlLoader mLoader;
    private static final Object mLock = new Object();

    public static class Data {
        public int brightTimeDelay = 1000;
        public boolean brightTimeDelayEnable = true;
        public float brightTimeDelayLuxThreshold = 30.0f;
        public float brightenDebounceTime = 1000.0f;
        public List<PointF> brightenlinePoints = new ArrayList();
        public int darkTimeDelay = 10000;
        public float darkTimeDelayBeta0 = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public float darkTimeDelayBeta1 = 1.0f;
        public float darkTimeDelayBeta2 = 0.333f;
        public boolean darkTimeDelayEnable = false;
        public float darkTimeDelayLuxThreshold = 50.0f;
        public int darkenDebounceTime = HwAPPQoEUtils.APP_TYPE_STREAMING;
        public List<PointF> darkenlinePoints = new ArrayList();
        public int postMaxMinAvgFilterNoFilterNum = 6;
        public int postMaxMinAvgFilterNum = 5;
        public int postMeanFilterNoFilterNum = 4;
        public int postMeanFilterNum = 3;
        public int postMethodNum = 1;
        public int preMeanFilterNoFilterNum = 7;
        public int preMeanFilterNum = 3;
        public int preMethodNum = 0;
        public float preWeightedMeanFilterAlpha = 0.5f;
        public float preWeightedMeanFilterLuxTh = 12.0f;
        public int preWeightedMeanFilterMaxFuncLuxNum = 3;
        public int preWeightedMeanFilterNoFilterNum = 7;
        public int preWeightedMeanFilterNum = 1;

        public void printData() {
            if (HwEyeProtectionXmlLoader.HWFLOW) {
                String str = HwEyeProtectionXmlLoader.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("printData() preMeanFilterNum=");
                stringBuilder.append(this.preMeanFilterNum);
                stringBuilder.append(", preMeanFilterNoFilterNum=");
                stringBuilder.append(this.preMeanFilterNoFilterNum);
                stringBuilder.append(", preWeightedMeanFilterNum=");
                stringBuilder.append(this.preWeightedMeanFilterNum);
                stringBuilder.append(", preWeightedMeanFilterMaxFuncLuxNum=");
                stringBuilder.append(this.preWeightedMeanFilterMaxFuncLuxNum);
                stringBuilder.append(", preWeightedMeanFilterLuxTh=");
                stringBuilder.append(this.preWeightedMeanFilterLuxTh);
                stringBuilder.append(", preWeightedMeanFilterAlpha=");
                stringBuilder.append(this.preWeightedMeanFilterAlpha);
                stringBuilder.append(", preWeightedMeanFilterNoFilterNum=");
                stringBuilder.append(this.preWeightedMeanFilterNoFilterNum);
                stringBuilder.append(", postMeanFilterNum=");
                stringBuilder.append(this.postMeanFilterNum);
                stringBuilder.append(", postMeanFilterNoFilterNum=");
                stringBuilder.append(this.postMeanFilterNoFilterNum);
                stringBuilder.append(", postMaxMinAvgFilterNum=");
                stringBuilder.append(this.postMaxMinAvgFilterNum);
                stringBuilder.append(", postMeanFilterNoFilterNum=");
                stringBuilder.append(this.postMeanFilterNoFilterNum);
                stringBuilder.append(", preMethodNum=");
                stringBuilder.append(this.preMethodNum);
                stringBuilder.append(", postMethodNum=");
                stringBuilder.append(this.postMethodNum);
                stringBuilder.append(", brightTimeDelay=");
                stringBuilder.append(this.brightTimeDelay);
                stringBuilder.append(", brightenDebounceTime=");
                stringBuilder.append(this.brightenDebounceTime);
                stringBuilder.append(", brightTimeDelayLuxThreshold=");
                stringBuilder.append(this.brightTimeDelayLuxThreshold);
                stringBuilder.append(", brightTimeDelayEnable=");
                stringBuilder.append(this.brightTimeDelayEnable);
                stringBuilder.append(", darkTimeDelayLuxThreshold=");
                stringBuilder.append(this.darkTimeDelayLuxThreshold);
                stringBuilder.append(", darkTimeDelayEnable=");
                stringBuilder.append(this.darkTimeDelayEnable);
                stringBuilder.append(", darkTimeDelayBeta2=");
                stringBuilder.append(this.darkTimeDelayBeta2);
                stringBuilder.append(", darkTimeDelayBeta1=");
                stringBuilder.append(this.darkTimeDelayBeta1);
                stringBuilder.append(", darkTimeDelayBeta0=");
                stringBuilder.append(this.darkTimeDelayBeta0);
                stringBuilder.append(", darkTimeDelay=");
                stringBuilder.append(this.darkTimeDelay);
                stringBuilder.append(", brightenlinePoints=");
                stringBuilder.append(this.brightenlinePoints);
                stringBuilder.append(", darkenlinePoints=");
                stringBuilder.append(this.darkenlinePoints);
                Slog.i(str, stringBuilder.toString());
            }
        }

        public void loadDefaultConfig() {
            if (HwEyeProtectionXmlLoader.HWFLOW) {
                Slog.i(HwEyeProtectionXmlLoader.TAG, "loadDefaultConfig()");
            }
            this.brightenDebounceTime = 1000.0f;
            this.brightTimeDelay = 1000;
            this.brightTimeDelayEnable = false;
            this.brightTimeDelayLuxThreshold = 30.0f;
            this.darkenDebounceTime = HwAPPQoEUtils.APP_TYPE_STREAMING;
            this.darkTimeDelay = 10000;
            this.darkTimeDelayBeta0 = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.darkTimeDelayBeta1 = 1.0f;
            this.darkTimeDelayBeta2 = 0.333f;
            this.darkTimeDelayEnable = false;
            this.darkTimeDelayLuxThreshold = 50.0f;
            this.postMaxMinAvgFilterNoFilterNum = 6;
            this.postMaxMinAvgFilterNum = 5;
            this.postMeanFilterNoFilterNum = 4;
            this.postMeanFilterNum = 3;
            this.postMethodNum = 1;
            this.preMeanFilterNoFilterNum = 7;
            this.preMeanFilterNum = 3;
            this.preMethodNum = 0;
            this.preWeightedMeanFilterAlpha = 0.5f;
            this.preWeightedMeanFilterLuxTh = 12.0f;
            this.preWeightedMeanFilterMaxFuncLuxNum = 3;
            this.preWeightedMeanFilterNoFilterNum = 7;
            this.preWeightedMeanFilterNum = 1;
            this.brightenlinePoints.clear();
            this.brightenlinePoints.add(new PointF(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 2.0f));
            this.brightenlinePoints.add(new PointF(5.0f, 10.0f));
            this.brightenlinePoints.add(new PointF(10.0f, 19.0f));
            this.brightenlinePoints.add(new PointF(20.0f, 89.0f));
            this.brightenlinePoints.add(new PointF(30.0f, 200.0f));
            this.brightenlinePoints.add(new PointF(100.0f, 439.0f));
            this.brightenlinePoints.add(new PointF(500.0f, 739.0f));
            this.brightenlinePoints.add(new PointF(1000.0f, 989.0f));
            this.brightenlinePoints.add(new PointF(3000.0f, 1000.0f));
            this.brightenlinePoints.add(new PointF(4000.0f, 2000.0f));
            this.brightenlinePoints.add(new PointF(10000.0f, 3000.0f));
            this.brightenlinePoints.add(new PointF(20000.0f, 10000.0f));
            this.brightenlinePoints.add(new PointF(40000.0f, 40000.0f));
            this.darkenlinePoints.clear();
            this.darkenlinePoints.add(new PointF(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 1.0f));
            this.darkenlinePoints.add(new PointF(1.0f, 1.0f));
            this.darkenlinePoints.add(new PointF(50.0f, 35.0f));
            this.darkenlinePoints.add(new PointF(100.0f, 80.0f));
            this.darkenlinePoints.add(new PointF(200.0f, 170.0f));
            this.darkenlinePoints.add(new PointF(300.0f, 225.0f));
            this.darkenlinePoints.add(new PointF(500.0f, 273.0f));
            this.darkenlinePoints.add(new PointF(600.0f, 322.0f));
            this.darkenlinePoints.add(new PointF(1200.0f, 600.0f));
            this.darkenlinePoints.add(new PointF(1800.0f, 600.0f));
            this.darkenlinePoints.add(new PointF(4000.0f, 2000.0f));
            this.darkenlinePoints.add(new PointF(8000.0f, 4000.0f));
            this.darkenlinePoints.add(new PointF(12000.0f, 6000.0f));
            this.darkenlinePoints.add(new PointF(40000.0f, 20000.0f));
        }
    }

    private static class Element_BrightenLinePoints extends HwXmlElement {
        private Element_BrightenLinePoints() {
        }

        public String getName() {
            return "BrightenLinePoints";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_BrightenLinePoints_Point extends HwXmlElement {
        private Element_BrightenLinePoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            HwEyeProtectionXmlLoader.mData.brightenlinePoints = HwXmlElement.parsePointFList(parser, HwEyeProtectionXmlLoader.mData.brightenlinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwEyeProtectionXmlLoader.checkPointsListIsOK(HwEyeProtectionXmlLoader.mData.brightenlinePoints);
        }
    }

    private static class Element_DarkenLinePoints extends HwXmlElement {
        private Element_DarkenLinePoints() {
        }

        public String getName() {
            return "DarkenLinePoints";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_DarkenLinePoints_Point extends HwXmlElement {
        private Element_DarkenLinePoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            HwEyeProtectionXmlLoader.mData.darkenlinePoints = HwXmlElement.parsePointFList(parser, HwEyeProtectionXmlLoader.mData.darkenlinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwEyeProtectionXmlLoader.checkPointsListIsOK(HwEyeProtectionXmlLoader.mData.darkenlinePoints);
        }
    }

    private static class Element_EyeProtectionConfig extends HwXmlElement {
        private Element_EyeProtectionConfig() {
        }

        public String getName() {
            return Utils.HW_EYEPROTECTION_CONFIG_FILE_NAME;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_FilterConfig extends HwXmlElement {
        private Element_FilterConfig() {
        }

        public String getName() {
            return "FilterConfig";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_FilterGroup extends HwXmlElement {
        private Element_FilterGroup() {
        }

        public String getName() {
            return "FilterGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"BrightenDebounceTime", "DarkenDebounceTime", "DarkTimeDelayEnable", "PostMeanFilterNoFilterNum", "PostMeanFilterNum", "PostMethodNum", "PreMeanFilterNoFilterNum", "PreMeanFilterNum", "PreMethodNum"});
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1216473595:
                    if (valueName.equals("PostMethodNum")) {
                        z = true;
                        break;
                    }
                case -806082151:
                    if (valueName.equals("BrightenDebounceTime")) {
                        z = false;
                        break;
                    }
                case -599365355:
                    if (valueName.equals("DarkenDebounceTime")) {
                        z = true;
                        break;
                    }
                case 157937894:
                    if (valueName.equals("PreMeanFilterNum")) {
                        z = true;
                        break;
                    }
                case 620980681:
                    if (valueName.equals("PostMeanFilterNum")) {
                        z = true;
                        break;
                    }
                case 1431493488:
                    if (valueName.equals("PostMeanFilterNoFilterNum")) {
                        z = true;
                        break;
                    }
                case 1443034509:
                    if (valueName.equals("PreMeanFilterNoFilterNum")) {
                        z = true;
                        break;
                    }
                case 1521603939:
                    if (valueName.equals("DarkTimeDelayEnable")) {
                        z = true;
                        break;
                    }
                case 1524020130:
                    if (valueName.equals("PreMethodNum")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwEyeProtectionXmlLoader.mData.brightenDebounceTime = Float.parseFloat(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.darkenDebounceTime = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.darkTimeDelayEnable = Boolean.parseBoolean(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.postMeanFilterNoFilterNum = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.postMeanFilterNum = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.postMethodNum = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.preMeanFilterNoFilterNum = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.preMeanFilterNum = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.preMethodNum = Integer.parseInt(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwEyeProtectionXmlLoader.mData.brightenDebounceTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwEyeProtectionXmlLoader.mData.postMeanFilterNum > 0 && HwEyeProtectionXmlLoader.mData.postMeanFilterNum <= HwEyeProtectionXmlLoader.mData.postMeanFilterNoFilterNum && HwEyeProtectionXmlLoader.mData.preMeanFilterNum > 0 && HwEyeProtectionXmlLoader.mData.preMeanFilterNum <= HwEyeProtectionXmlLoader.mData.preMeanFilterNoFilterNum;
        }
    }

    private static class Element_FilterOptionalGroup1 extends HwXmlElement {
        private Element_FilterOptionalGroup1() {
        }

        public String getName() {
            return "FilterOptionalGroup1";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"BrightTimeDelay", "BrightTimeDelayEnable", "BrightenTimeDelayLuxThreshold", "DarkenTimeDelay", "DarkenTimeDelayBeta0", "DarkenTimeDelayBeta1", "DarkenTimeDelayBeta2", "DarkTimeDelayLuxThreshold", "PostMaxMinAvgFilterNoFilterNum", "PostMaxMinAvgFilterNum"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1912197839:
                    if (valueName.equals("PostMaxMinAvgFilterNoFilterNum")) {
                        z = true;
                        break;
                    }
                case -1203123972:
                    if (valueName.equals("DarkTimeDelayLuxThreshold")) {
                        z = true;
                        break;
                    }
                case -591649441:
                    if (valueName.equals("BrightTimeDelayEnable")) {
                        z = true;
                        break;
                    }
                case -157474449:
                    if (valueName.equals("BrightenTimeDelayLuxThreshold")) {
                        z = true;
                        break;
                    }
                case 248648375:
                    if (valueName.equals("DarkenTimeDelay")) {
                        z = true;
                        break;
                    }
                case 1373278396:
                    if (valueName.equals("BrightTimeDelay")) {
                        z = false;
                        break;
                    }
                case 1472315337:
                    if (valueName.equals("DarkenTimeDelayBeta0")) {
                        z = true;
                        break;
                    }
                case 1472315338:
                    if (valueName.equals("DarkenTimeDelayBeta1")) {
                        z = true;
                        break;
                    }
                case 1472315339:
                    if (valueName.equals("DarkenTimeDelayBeta2")) {
                        z = true;
                        break;
                    }
                case 1960905866:
                    if (valueName.equals("PostMaxMinAvgFilterNum")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwEyeProtectionXmlLoader.mData.brightTimeDelay = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.brightTimeDelayEnable = Boolean.parseBoolean(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.brightTimeDelayLuxThreshold = Float.parseFloat(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.darkTimeDelay = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.darkTimeDelayBeta0 = Float.parseFloat(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.darkTimeDelayBeta1 = Float.parseFloat(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.darkTimeDelayBeta2 = Float.parseFloat(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.darkTimeDelayLuxThreshold = Float.parseFloat(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.postMaxMinAvgFilterNoFilterNum = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.postMaxMinAvgFilterNum = Integer.parseInt(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwEyeProtectionXmlLoader.mData.brightTimeDelay > 0 && HwEyeProtectionXmlLoader.mData.darkTimeDelay > 0 && HwEyeProtectionXmlLoader.mData.postMaxMinAvgFilterNum > 0 && HwEyeProtectionXmlLoader.mData.postMaxMinAvgFilterNum <= HwEyeProtectionXmlLoader.mData.postMaxMinAvgFilterNoFilterNum;
        }
    }

    private static class Element_FilterOptionalGroup2 extends HwXmlElement {
        private Element_FilterOptionalGroup2() {
        }

        public String getName() {
            return "FilterOptionalGroup2";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"PreWeightedMeanFilterAlpha", "PreWeightedMeanFilterLuxTh", "PreWeightedMeanFilterMaxFuncLuxNum", "PreWeightedMeanFilterNoFilterNum", "PreWeightedMeanFilterNum"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1627681866:
                    if (valueName.equals("PreWeightedMeanFilterNoFilterNum")) {
                        z = true;
                        break;
                    }
                case 246136847:
                    if (valueName.equals("PreWeightedMeanFilterNum")) {
                        z = true;
                        break;
                    }
                case 302040999:
                    if (valueName.equals("PreWeightedMeanFilterAlpha")) {
                        z = false;
                        break;
                    }
                case 312474924:
                    if (valueName.equals("PreWeightedMeanFilterLuxTh")) {
                        z = true;
                        break;
                    }
                case 1095802536:
                    if (valueName.equals("PreWeightedMeanFilterMaxFuncLuxNum")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwEyeProtectionXmlLoader.mData.preWeightedMeanFilterAlpha = Float.parseFloat(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.preWeightedMeanFilterLuxTh = Float.parseFloat(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.preWeightedMeanFilterMaxFuncLuxNum = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.preWeightedMeanFilterNoFilterNum = Integer.parseInt(parser.nextText());
                    break;
                case true:
                    HwEyeProtectionXmlLoader.mData.preWeightedMeanFilterNum = Integer.parseInt(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwEyeProtectionXmlLoader.mData.preWeightedMeanFilterMaxFuncLuxNum > 0 && HwEyeProtectionXmlLoader.mData.preWeightedMeanFilterNoFilterNum > 0 && HwEyeProtectionXmlLoader.mData.preWeightedMeanFilterNum > 0 && HwEyeProtectionXmlLoader.mData.preWeightedMeanFilterNum <= HwEyeProtectionXmlLoader.mData.preWeightedMeanFilterNoFilterNum;
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

    public static Data getData(String xmlFilePath) {
        Data retData = null;
        synchronized (mLock) {
            Data data;
            try {
                if (mLoader == null) {
                    mLoader = new HwEyeProtectionXmlLoader(xmlFilePath);
                }
                retData = mData;
                if (retData == null) {
                    data = new Data();
                    retData = data;
                    retData.loadDefaultConfig();
                }
            } catch (Exception e) {
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getData() error!");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                    if (null == null) {
                        data = new Data();
                    }
                } catch (Throwable th) {
                    if (null == null) {
                        new Data().loadDefaultConfig();
                    }
                }
            }
        }
        return retData;
    }

    private HwEyeProtectionXmlLoader(String xmlFilePath) {
        if (HWDEBUG) {
            Slog.d(TAG, "HwEyeProtectionXmlLoader()");
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
        HwXmlElement filterConfigElement = parser.registerRootElement(new Element_EyeProtectionConfig()).registerChildElement(new Element_FilterConfig());
        filterConfigElement.registerChildElement(new Element_FilterGroup());
        filterConfigElement.registerChildElement(new Element_FilterOptionalGroup1());
        filterConfigElement.registerChildElement(new Element_FilterOptionalGroup2());
        filterConfigElement.registerChildElement(new Element_BrightenLinePoints()).registerChildElement(new Element_BrightenLinePoints_Point());
        filterConfigElement.registerChildElement(new Element_DarkenLinePoints()).registerChildElement(new Element_DarkenLinePoints_Point());
    }

    private static boolean checkPointsListIsOK(List<PointF> list) {
        if (list == null) {
            Slog.e(TAG, "checkPointsListIsOK() error! list is null");
            return false;
        } else if (list.size() < 3 || list.size() >= 100) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkPointsListIsOK() error! list size=");
            stringBuilder.append(list.size());
            stringBuilder.append(" is out of range");
            Slog.e(str, stringBuilder.toString());
            return false;
        } else {
            PointF lastPoint = null;
            for (PointF point : list) {
                if (lastPoint == null || point.x > lastPoint.x) {
                    lastPoint = point;
                } else {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("checkPointsListIsOK() error! x in list isn't a increasing sequence, ");
                    stringBuilder2.append(point.x);
                    stringBuilder2.append("<=");
                    stringBuilder2.append(lastPoint.x);
                    Slog.e(str2, stringBuilder2.toString());
                    return false;
                }
            }
            return true;
        }
    }
}
