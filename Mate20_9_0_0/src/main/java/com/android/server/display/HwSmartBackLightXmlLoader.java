package com.android.server.display;

import android.graphics.PointF;
import android.util.Log;
import android.util.Slog;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class HwSmartBackLightXmlLoader {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String TAG = "HwSmartBackLightXmlLoader";
    private static final String XML_NAME = "SBLConfig.xml";
    private static Data mData = new Data();
    private static HwSmartBackLightXmlLoader mLoader;
    private static final Object mLoaderLock = new Object();

    public static class Data {
        public int apicalADLevel = 128;
        public int brighenDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
        public List<PointF> brightenLinePoints = new ArrayList();
        public int darkenDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
        public List<PointF> darkenLinePoints = new ArrayList();
        public int darknessAmbidentBrightnessShift = 0;
        public int darknessApicalADLevel = 0;
        public int inDoorThreshold = 5000;
        public int indoorAmbidentBrightnessShift = 0;
        public int indoorApicalADLevel = 0;
        public int lightSensorRateMills = 300;
        public int outDoorThreshold = 8000;
        public int outdoorAmbidentBrightnessShift = 0;
        public int outdoorApicalADLevel = 0;
        public boolean sceneCameraEnable = false;
        public boolean sceneGalleryEnable = false;
        public boolean sceneVideoEnable = false;
        public int videoSceneDarknessThreshold = 0;
        public boolean videoSceneEnhanceEnabled = false;
        public int videoSceneIndoorThreshold = 0;

        public void printData() {
            if (HwSmartBackLightXmlLoader.HWFLOW) {
                String str = HwSmartBackLightXmlLoader.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("printData() lightSensorRateMills=");
                stringBuilder.append(this.lightSensorRateMills);
                stringBuilder.append(", apicalADLevel=");
                stringBuilder.append(this.apicalADLevel);
                stringBuilder.append(", sceneVideoEnable=");
                stringBuilder.append(this.sceneVideoEnable);
                stringBuilder.append(", sceneGalleryEnable=");
                stringBuilder.append(this.sceneGalleryEnable);
                stringBuilder.append(", sceneCameraEnable=");
                stringBuilder.append(this.sceneCameraEnable);
                stringBuilder.append(", outDoorThreshold=");
                stringBuilder.append(this.outDoorThreshold);
                stringBuilder.append(", inDoorThreshold=");
                stringBuilder.append(this.inDoorThreshold);
                stringBuilder.append(", brighenDebounceTime=");
                stringBuilder.append(this.brighenDebounceTime);
                stringBuilder.append(", darkenDebounceTime=");
                stringBuilder.append(this.darkenDebounceTime);
                Slog.i(str, stringBuilder.toString());
                str = HwSmartBackLightXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() brightenLinePoints=");
                stringBuilder.append(this.brightenLinePoints);
                Slog.i(str, stringBuilder.toString());
                str = HwSmartBackLightXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() darkenLinePoints=");
                stringBuilder.append(this.darkenLinePoints);
                Slog.i(str, stringBuilder.toString());
            }
        }

        public void loadDefaultConfig() {
            if (HwSmartBackLightXmlLoader.HWFLOW) {
                Slog.i(HwSmartBackLightXmlLoader.TAG, "loadDefaultConfig()");
            }
            this.lightSensorRateMills = 300;
            this.apicalADLevel = 128;
            this.sceneVideoEnable = false;
            this.sceneGalleryEnable = false;
            this.sceneCameraEnable = false;
            this.outDoorThreshold = 8000;
            this.inDoorThreshold = 5000;
            this.brighenDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
            this.darkenDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
            this.brightenLinePoints.clear();
            this.brightenLinePoints.add(new PointF(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 15.0f));
            this.brightenLinePoints.add(new PointF(2.0f, 15.0f));
            this.brightenLinePoints.add(new PointF(10.0f, 19.0f));
            this.brightenLinePoints.add(new PointF(100.0f, 239.0f));
            this.brightenLinePoints.add(new PointF(500.0f, 439.0f));
            this.brightenLinePoints.add(new PointF(1000.0f, 989.0f));
            this.brightenLinePoints.add(new PointF(40000.0f, 989.0f));
            this.darkenLinePoints.clear();
            this.darkenLinePoints.add(new PointF(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 1.0f));
            this.darkenLinePoints.add(new PointF(1.0f, 1.0f));
            this.darkenLinePoints.add(new PointF(15.0f, 15.0f));
            this.darkenLinePoints.add(new PointF(20.0f, 15.0f));
            this.darkenLinePoints.add(new PointF(85.0f, 80.0f));
            this.darkenLinePoints.add(new PointF(100.0f, 80.0f));
            this.darkenLinePoints.add(new PointF(420.0f, 400.0f));
            this.darkenLinePoints.add(new PointF(500.0f, 400.0f));
            this.darkenLinePoints.add(new PointF(600.0f, 500.0f));
            this.darkenLinePoints.add(new PointF(1000.0f, 500.0f));
            this.darkenLinePoints.add(new PointF(2000.0f, 1000.0f));
            this.darkenLinePoints.add(new PointF(40000.0f, 1000.0f));
        }
    }

    private static class Element_BaseGroup extends HwXmlElement {
        private Element_BaseGroup() {
        }

        public String getName() {
            return "BaseGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"LightSensorRateMills", "ApicalADLevel", "OutDoorThreshold", "InDoorThreshold", "BrighenDebounceTime", "DarkenDebounceTime"});
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -790346984:
                    if (valueName.equals("InDoorThreshold")) {
                        z = true;
                        break;
                    }
                case -599365355:
                    if (valueName.equals("DarkenDebounceTime")) {
                        z = true;
                        break;
                    }
                case -85825767:
                    if (valueName.equals("BrighenDebounceTime")) {
                        z = true;
                        break;
                    }
                case 701544399:
                    if (valueName.equals("OutDoorThreshold")) {
                        z = true;
                        break;
                    }
                case 1001305191:
                    if (valueName.equals("LightSensorRateMills")) {
                        z = false;
                        break;
                    }
                case 1593528077:
                    if (valueName.equals("ApicalADLevel")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwSmartBackLightXmlLoader.mData.lightSensorRateMills = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwSmartBackLightXmlLoader.mData.apicalADLevel = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwSmartBackLightXmlLoader.mData.outDoorThreshold = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwSmartBackLightXmlLoader.mData.inDoorThreshold = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwSmartBackLightXmlLoader.mData.brighenDebounceTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwSmartBackLightXmlLoader.mData.darkenDebounceTime = HwXmlElement.string2Int(parser.nextText());
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
            return HwSmartBackLightXmlLoader.mData.lightSensorRateMills > 0 && HwSmartBackLightXmlLoader.mData.apicalADLevel > 0 && HwSmartBackLightXmlLoader.mData.apicalADLevel <= 255 && HwSmartBackLightXmlLoader.mData.outDoorThreshold >= HwSmartBackLightXmlLoader.mData.inDoorThreshold && HwSmartBackLightXmlLoader.mData.inDoorThreshold >= 0 && HwSmartBackLightXmlLoader.mData.brighenDebounceTime < 10000 && HwSmartBackLightXmlLoader.mData.darkenDebounceTime < 10000;
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
            HwSmartBackLightXmlLoader.mData.brightenLinePoints = HwXmlElement.parsePointFList(parser, HwSmartBackLightXmlLoader.mData.brightenLinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwSmartBackLightXmlLoader.checkPointsListIsOK(HwSmartBackLightXmlLoader.mData.brightenLinePoints);
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
            HwSmartBackLightXmlLoader.mData.darkenLinePoints = HwXmlElement.parsePointFList(parser, HwSmartBackLightXmlLoader.mData.darkenLinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwSmartBackLightXmlLoader.checkPointsListIsOK(HwSmartBackLightXmlLoader.mData.darkenLinePoints);
        }
    }

    private static class Element_SBLConfig extends HwXmlElement {
        private boolean mParseStarted;

        private Element_SBLConfig() {
        }

        public String getName() {
            return "SBLConfig";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (this.mParseStarted) {
                return false;
            }
            this.mParseStarted = true;
            return true;
        }
    }

    private static class Element_SceneRecognition extends HwXmlElement {
        private Element_SceneRecognition() {
        }

        public String getName() {
            return "SceneRecognition";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_SceneRecognitionGroup extends HwXmlElement {
        private Element_SceneRecognitionGroup() {
        }

        public String getName() {
            return "SceneRecognitionGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"SceneVideo", "SceneGallery", "SceneCamera"});
        }

        /* JADX WARNING: Removed duplicated region for block: B:17:0x003c  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003c  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003c  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0054  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -229548177) {
                if (hashCode != 922867377) {
                    if (hashCode == 2093405510 && valueName.equals("SceneGallery")) {
                        z = true;
                        switch (z) {
                            case false:
                                HwSmartBackLightXmlLoader.mData.sceneVideoEnable = HwXmlElement.string2Boolean(parser.nextText());
                                break;
                            case true:
                                HwSmartBackLightXmlLoader.mData.sceneGalleryEnable = HwXmlElement.string2Boolean(parser.nextText());
                                break;
                            case true:
                                HwSmartBackLightXmlLoader.mData.sceneCameraEnable = HwXmlElement.string2Boolean(parser.nextText());
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
                } else if (valueName.equals("SceneCamera")) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            } else if (valueName.equals("SceneVideo")) {
                z = false;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private static class Element_VideoSceneEnhance extends HwXmlElement {
        private Element_VideoSceneEnhance() {
        }

        public String getName() {
            return "VideoSceneEnhance";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_VideoSceneEnhance_DarknessConfig extends HwXmlElement {
        private Element_VideoSceneEnhance_DarknessConfig() {
        }

        public String getName() {
            return "DarknessConfig";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_VideoSceneEnhance_DarknessConfigGroup extends HwXmlElement {
        private Element_VideoSceneEnhance_DarknessConfigGroup() {
        }

        public String getName() {
            return "VideoSceneEnhance_DarknessConfigGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"DarknessApicalADLevel", "DarknessAmbidentBrightnessShift"});
        }

        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -1707245654) {
                if (hashCode == -1232861888 && valueName.equals("DarknessApicalADLevel")) {
                    z = false;
                    switch (z) {
                        case false:
                            HwSmartBackLightXmlLoader.mData.darknessApicalADLevel = Integer.parseInt(parser.nextText());
                            break;
                        case true:
                            HwSmartBackLightXmlLoader.mData.darknessAmbidentBrightnessShift = Integer.parseInt(parser.nextText());
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
            } else if (valueName.equals("DarknessAmbidentBrightnessShift")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private static class Element_VideoSceneEnhance_Enabled extends HwXmlElement {
        private Element_VideoSceneEnhance_Enabled() {
        }

        public String getName() {
            return "VideoSceneEnhanceEnabled";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            HwSmartBackLightXmlLoader.mData.videoSceneEnhanceEnabled = Boolean.parseBoolean(parser.nextText());
            return true;
        }
    }

    private static class Element_VideoSceneEnhance_IndoorConfig extends HwXmlElement {
        private Element_VideoSceneEnhance_IndoorConfig() {
        }

        public String getName() {
            return "IndoorConfig";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_VideoSceneEnhance_IndoorConfigGroup extends HwXmlElement {
        private Element_VideoSceneEnhance_IndoorConfigGroup() {
        }

        public String getName() {
            return "VideoSceneEnhance_IndoorConfigGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"IndoorApicalADLevel", "IndoorAmbidentBrightnessShift"});
        }

        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -936208028) {
                if (hashCode == 1455497082 && valueName.equals("IndoorApicalADLevel")) {
                    z = false;
                    switch (z) {
                        case false:
                            HwSmartBackLightXmlLoader.mData.indoorApicalADLevel = Integer.parseInt(parser.nextText());
                            break;
                        case true:
                            HwSmartBackLightXmlLoader.mData.indoorAmbidentBrightnessShift = Integer.parseInt(parser.nextText());
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
            } else if (valueName.equals("IndoorAmbidentBrightnessShift")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private static class Element_VideoSceneEnhance_OutdoorConfig extends HwXmlElement {
        private Element_VideoSceneEnhance_OutdoorConfig() {
        }

        public String getName() {
            return "OutdoorConfig";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_VideoSceneEnhance_OutdoorConfigGroup extends HwXmlElement {
        private Element_VideoSceneEnhance_OutdoorConfigGroup() {
        }

        public String getName() {
            return "VideoSceneEnhance_OutdoorConfigGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"OutdoorApicalADLevel", "OutdoorAmbidentBrightnessShift"});
        }

        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -1138949669) {
                if (hashCode == -966369103 && valueName.equals("OutdoorApicalADLevel")) {
                    z = false;
                    switch (z) {
                        case false:
                            HwSmartBackLightXmlLoader.mData.outdoorApicalADLevel = Integer.parseInt(parser.nextText());
                            break;
                        case true:
                            HwSmartBackLightXmlLoader.mData.outdoorAmbidentBrightnessShift = Integer.parseInt(parser.nextText());
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
            } else if (valueName.equals("OutdoorAmbidentBrightnessShift")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private static class Element_VideoSceneEnhance_Recognition extends HwXmlElement {
        private Element_VideoSceneEnhance_Recognition() {
        }

        public String getName() {
            return "VideoSceneRecognition";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_VideoSceneEnhance_RecognitionGroup extends HwXmlElement {
        private Element_VideoSceneEnhance_RecognitionGroup() {
        }

        public String getName() {
            return "VideoSceneEnhance_RecognitionGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"VideoSceneDarknessThreshold", "VideoSceneIndoorThreshold"});
        }

        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -1682331859) {
                if (hashCode == -1039405529 && valueName.equals("VideoSceneIndoorThreshold")) {
                    z = true;
                    switch (z) {
                        case false:
                            HwSmartBackLightXmlLoader.mData.videoSceneDarknessThreshold = Integer.parseInt(parser.nextText());
                            break;
                        case true:
                            HwSmartBackLightXmlLoader.mData.videoSceneIndoorThreshold = Integer.parseInt(parser.nextText());
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
            } else if (valueName.equals("VideoSceneDarknessThreshold")) {
                z = false;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
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

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:10:0x0014, B:15:0x0021] */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            r2 = th;
     */
    /* JADX WARNING: Missing block: B:22:0x0041, code skipped:
            if (null == null) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:23:0x0043, code skipped:
            new com.android.server.display.HwSmartBackLightXmlLoader.Data().loadDefaultConfig();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static Data getData() {
        Data retData;
        synchronized (mLoaderLock) {
            retData = null;
            Data th;
            try {
                if (mLoader == null) {
                    mLoader = new HwSmartBackLightXmlLoader();
                }
                retData = mData;
                if (retData == null) {
                    th = new Data();
                    retData = th;
                    retData.loadDefaultConfig();
                }
            } catch (RuntimeException e) {
                th = e;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getData() failed! ");
                stringBuilder.append(th);
                Slog.e(str, stringBuilder.toString());
                if (null == null) {
                    th = new Data();
                }
            }
        }
        return retData;
    }

    private HwSmartBackLightXmlLoader() {
        if (HWDEBUG) {
            Slog.d(TAG, "HwSmartBackLightXmlLoader()");
        }
        if (!parseXml(getXmlPath())) {
            mData.loadDefaultConfig();
        }
        mData.printData();
    }

    private boolean parseXml(String xmlPath) {
        if (xmlPath == null) {
            Slog.e(TAG, "parseXml() error! xmlPath is null");
            return false;
        }
        HwXmlParser xmlParser = new HwXmlParser(xmlPath);
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
        HwXmlElement rootElement = parser.registerRootElement(new Element_SBLConfig());
        rootElement.registerChildElement(new Element_BaseGroup());
        rootElement.registerChildElement(new Element_SceneRecognition()).registerChildElement(new Element_SceneRecognitionGroup());
        rootElement.registerChildElement(new Element_BrightenLinePoints()).registerChildElement(new Element_BrightenLinePoints_Point());
        rootElement.registerChildElement(new Element_DarkenLinePoints()).registerChildElement(new Element_DarkenLinePoints_Point());
        HwXmlElement videoSceneEnhance = rootElement.registerChildElement(new Element_VideoSceneEnhance());
        videoSceneEnhance.registerChildElement(new Element_VideoSceneEnhance_Enabled());
        videoSceneEnhance.registerChildElement(new Element_VideoSceneEnhance_Recognition()).registerChildElement(new Element_VideoSceneEnhance_RecognitionGroup());
        videoSceneEnhance.registerChildElement(new Element_VideoSceneEnhance_DarknessConfig()).registerChildElement(new Element_VideoSceneEnhance_DarknessConfigGroup());
        videoSceneEnhance.registerChildElement(new Element_VideoSceneEnhance_IndoorConfig()).registerChildElement(new Element_VideoSceneEnhance_IndoorConfigGroup());
        videoSceneEnhance.registerChildElement(new Element_VideoSceneEnhance_OutdoorConfig()).registerChildElement(new Element_VideoSceneEnhance_OutdoorConfigGroup());
    }

    private String getXmlPath() {
        File xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/lcd/%s", new Object[]{XML_NAME}), 0);
        if (xmlFile != null) {
            return xmlFile.getAbsolutePath();
        }
        Slog.e(TAG, "getXmlPath() error! can't find xml file.");
        return null;
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
