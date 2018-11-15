package com.android.server.policy;

import android.util.Log;
import android.util.Xml;
import com.android.server.gesture.GestureNavConst;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class EasyWakeUpXmlParse {
    public static final String Cover_Screen = "Cover_Screen";
    private static final boolean DEBUG = false;
    public static final String Double_Touch = "Double_Touch";
    private static final int EASYWAKE_ENABLE_DEFAULT_VALUES = 0;
    private static final int EASYWAKE_ENABLE_SURPPORT_VALUSE = 0;
    public static final String EasyWakeUp_Flick_ALL = "EasyWakeUp_Flick_ALL";
    public static final String EasyWakeUp_Flick_DOWN = "EasyWakeUp_Flick_Down";
    public static final String EasyWakeUp_Flick_LEFT = "EasyWakeUp_Flick_left";
    public static final String EasyWakeUp_Flick_RIGHT = "EasyWakeUp_Flick_Right";
    public static final String EasyWakeUp_Flick_UP = "EasyWakeUp_Flick_Up";
    public static final String EasyWakeUp_Letter_ALL = "EasyWakeUp_Letter_ALL";
    public static final String EasyWakeUp_Letter_C = "EasyWakeUp_Letter_C";
    public static final String EasyWakeUp_Letter_E = "EasyWakeUp_Letter_E";
    public static final String EasyWakeUp_Letter_M = "EasyWakeUp_Letter_M";
    public static final String EasyWakeUp_Letter_W = "EasyWakeUp_Letter_W";
    private static final String HWEASYWAKEUP_MOTION_CONFIG_CUST_PATH = "/data/cust/xml/hw_easywakeupmotion_config.xml";
    private static final String HWEASYWAKEUP_MOTION_CONFIG_SYSTEM_PATH = "/system/etc/xml/hw_easywakeupmotion_config.xml";
    private static String TAG = "EasyWakeUpXmlParse";
    private static int coverScreenCode = -1;
    private static int coverScreenIndex = -1;
    private static int doubleTouchCode = -1;
    private static int doubleTouchIndex = -1;
    private static int driverFileLength = 0;
    private static String driverGesturePath = "";
    private static String driverPostionPath = "";
    private static int flickALLIndex = -1;
    private static int flickDownCode = -1;
    private static int flickDownIndex = -1;
    private static int flickLeftCode = -1;
    private static int flickLeftIndex = -1;
    private static int flickRightCode = -1;
    private static int flickRightIndex = -1;
    private static int flickUpCode = -1;
    private static int flickUpIndex = -1;
    private static List<HwEasyWakeUpDate> hweasywakeupdates = null;
    private static int letterAllIndex = -1;
    private static int letterCCode = -1;
    private static int letterCIndex = -1;
    private static int letterECode = -1;
    private static int letterEIndex = -1;
    private static int letterMCode = -1;
    private static int letterMIndex = -1;
    private static int letterWCode = -1;
    private static int letterWIndex = -1;
    private static int maxKeyCode = -1;
    private static int minKeyCode = -1;
    private static int powerOptimize = 0;
    private static int sensorCheckTimes = 6;
    private static float sensorFarValue = 5.0f;
    private static float sensorNearValue = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private static long sensorWatchTime = 50;

    public static class HwEasyWakeUpDate {
        private int flag = 0;
        private String name = "";
        private int support = 0;
        private int value = 0;

        public HwEasyWakeUpDate(String name, int support, int value, int flag) {
            this.name = name;
            this.support = support;
            this.value = value;
            this.flag = flag;
        }

        public String getName() {
            return this.name;
        }

        public int getValue() {
            return this.value;
        }

        public int getSupport() {
            return this.support;
        }

        public int getFlag() {
            return this.flag;
        }
    }

    public static int getDefaultSupportValueFromCust() {
        int defaultsupportvalue = 0;
        if (hweasywakeupdates == null) {
            try {
                parseHwEasyWakeUpdatesFile();
            } catch (Exception e) {
                Log.e(TAG, "used default support value = 0");
                return 0;
            }
        }
        if (hweasywakeupdates != null) {
            for (HwEasyWakeUpDate hweasywakeupdate : hweasywakeupdates) {
                int support = hweasywakeupdate.getSupport();
                int flag = hweasywakeupdate.getFlag();
                if (flag < 20) {
                    defaultsupportvalue += support << flag;
                }
            }
        }
        return defaultsupportvalue;
    }

    public static int getDefaultValueFromCust() {
        int defaultvalue = 0;
        if (hweasywakeupdates == null) {
            try {
                parseHwEasyWakeUpdatesFile();
            } catch (Exception e) {
                Log.e(TAG, "used default value = 0");
                return 0;
            }
        }
        if (hweasywakeupdates != null) {
            for (HwEasyWakeUpDate hweasywakeupdate : hweasywakeupdates) {
                defaultvalue += hweasywakeupdate.getValue() << hweasywakeupdate.getFlag();
            }
        }
        return defaultvalue;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.server.policy.EasyWakeUpXmlParse.parseHwEasyWakeUpdatesFile():void, dom blocks: [B:5:0x000e, B:12:0x0028]
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:89)
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private static void parseHwEasyWakeUpdatesFile() throws java.lang.Exception {
        /*
        r0 = 0;
        r1 = r0;
        r2 = "xml/hw_easywakeupmotion_config.xml";	 Catch:{ NoClassDefFoundError -> 0x0027 }
        r3 = 0;	 Catch:{ NoClassDefFoundError -> 0x0027 }
        r2 = huawei.cust.HwCfgFilePolicy.getCfgFile(r2, r3);	 Catch:{ NoClassDefFoundError -> 0x0027 }
        r0 = r2;
        if (r0 != 0) goto L_0x0047;
    L_0x000e:
        r2 = new java.io.File;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r3 = "/data/cust/xml/hw_easywakeupmotion_config.xml";	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r2.<init>(r3);	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r0 = r2;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r2 = r0.exists();	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        if (r2 != 0) goto L_0x0047;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x001c:
        r2 = new java.io.File;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r3 = "/system/etc/xml/hw_easywakeupmotion_config.xml";	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r2.<init>(r3);	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x0023:
        r0 = r2;
        goto L_0x0047;
    L_0x0025:
        r2 = move-exception;
        goto L_0x0056;
    L_0x0027:
        r2 = move-exception;
        r3 = TAG;	 Catch:{ all -> 0x0025 }
        r4 = "HwCfgFilePolicy NoClassDefFoundError";	 Catch:{ all -> 0x0025 }
        android.util.Log.d(r3, r4);	 Catch:{ all -> 0x0025 }
        if (r0 != 0) goto L_0x0047;
    L_0x0031:
        r2 = new java.io.File;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r3 = "/data/cust/xml/hw_easywakeupmotion_config.xml";	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r2.<init>(r3);	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r0 = r2;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r2 = r0.exists();	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        if (r2 != 0) goto L_0x0047;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x003f:
        r2 = new java.io.File;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r3 = "/system/etc/xml/hw_easywakeupmotion_config.xml";	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r2.<init>(r3);	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        goto L_0x0023;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x0047:
        r2 = new java.io.FileInputStream;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r2.<init>(r0);	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r1 = r2;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        parseHwEasyWakeUpDate(r1);	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x0051:
        r1.close();
        r0 = 0;
        goto L_0x00a5;
    L_0x0056:
        if (r0 != 0) goto L_0x0079;
    L_0x0058:
        r3 = new java.io.File;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r4 = "/data/cust/xml/hw_easywakeupmotion_config.xml";	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r3.<init>(r4);	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r0 = r3;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r3 = r0.exists();	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        if (r3 != 0) goto L_0x0079;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x0066:
        r3 = new java.io.File;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r4 = "/system/etc/xml/hw_easywakeupmotion_config.xml";	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r3.<init>(r4);	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r0 = r3;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        goto L_0x0079;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x006f:
        r0 = move-exception;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        goto L_0x00a6;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x0071:
        r0 = move-exception;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        goto L_0x007a;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x0073:
        r0 = move-exception;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        goto L_0x0083;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x0075:
        r0 = move-exception;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        goto L_0x008e;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x0077:
        r0 = move-exception;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        goto L_0x0099;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
    L_0x0079:
        throw r2;	 Catch:{ FileNotFoundException -> 0x0077, XmlPullParserException -> 0x0075, IOException -> 0x0073, Exception -> 0x0071 }
        r2 = new java.lang.Exception;	 Catch:{ all -> 0x006f }
        r3 = "Exception";	 Catch:{ all -> 0x006f }
        r2.<init>(r3);	 Catch:{ all -> 0x006f }
        throw r2;	 Catch:{ all -> 0x006f }
        r2 = TAG;	 Catch:{ all -> 0x006f }
        r3 = "hw_easywakeupmotion_config.xml IOException";	 Catch:{ all -> 0x006f }
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x006f }
        if (r1 == 0) goto L_0x00a4;	 Catch:{ all -> 0x006f }
    L_0x008d:
        goto L_0x0051;	 Catch:{ all -> 0x006f }
        r2 = TAG;	 Catch:{ all -> 0x006f }
        r3 = "hw_easywakeupmotion_config.xml XmlPullParserException";	 Catch:{ all -> 0x006f }
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x006f }
        if (r1 == 0) goto L_0x00a4;	 Catch:{ all -> 0x006f }
    L_0x0098:
        goto L_0x0051;	 Catch:{ all -> 0x006f }
        r2 = TAG;	 Catch:{ all -> 0x006f }
        r3 = "hw_easywakeupmotion_config.xml FileNotFoundException";	 Catch:{ all -> 0x006f }
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x006f }
        if (r1 == 0) goto L_0x00a4;
    L_0x00a3:
        goto L_0x0051;
    L_0x00a4:
        r0 = r1;
    L_0x00a5:
        return;
    L_0x00a6:
        if (r1 == 0) goto L_0x00ac;
    L_0x00a8:
        r1.close();
        r1 = 0;
    L_0x00ac:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.EasyWakeUpXmlParse.parseHwEasyWakeUpdatesFile():void");
    }

    private static void parseHwEasyWakeUpDate(InputStream inputstream) throws XmlPullParserException, IOException {
        HwEasyWakeUpDate hweasywakeupdate = null;
        XmlPullParser pullParser = Xml.newPullParser();
        pullParser.setInput(inputstream, "UTF-8");
        for (int event = pullParser.getEventType(); event != 1; event = pullParser.next()) {
            if (event != 0) {
                switch (event) {
                    case 2:
                        if ("EasyWakeUpMotion".equals(pullParser.getName())) {
                            String name = pullParser.getAttributeValue(null);
                            int support = Integer.parseInt(pullParser.getAttributeValue(1));
                            int value = Integer.parseInt(pullParser.getAttributeValue(2));
                            int flag = Integer.parseInt(pullParser.getAttributeValue(3));
                            getCodeAndIndexByName(name, Integer.parseInt(pullParser.getAttributeValue(4)), flag);
                            hweasywakeupdate = new HwEasyWakeUpDate(name, support, value, flag);
                        }
                        getValueByName(pullParser);
                        break;
                    case 3:
                        if (!"EasyWakeUpMotion".equals(pullParser.getName())) {
                            break;
                        }
                        hweasywakeupdates.add(hweasywakeupdate);
                        hweasywakeupdate = null;
                        break;
                    default:
                        break;
                }
            }
            hweasywakeupdates = new ArrayList();
        }
    }

    private static void getValueByName(XmlPullParser pullParser) {
        if ("MaxKeyCode".equals(pullParser.getName())) {
            maxKeyCode = Integer.parseInt(pullParser.getAttributeValue(null, "value"));
        }
        if ("MinKeyCode".equals(pullParser.getName())) {
            minKeyCode = Integer.parseInt(pullParser.getAttributeValue(null, "value"));
        }
        if ("DriverFileLength".equals(pullParser.getName())) {
            driverFileLength = Integer.parseInt(pullParser.getAttributeValue(null, "value"));
        }
        if ("DriverPostionPath".equals(pullParser.getName())) {
            driverPostionPath = String.valueOf(pullParser.getAttributeValue(null, "value"));
        }
        if ("DriverGesturePath".equals(pullParser.getName())) {
            driverGesturePath = String.valueOf(pullParser.getAttributeValue(null, "value"));
        }
        if ("SensorNear".equals(pullParser.getName())) {
            sensorNearValue = Float.parseFloat(pullParser.getAttributeValue(null, "value"));
        }
        if ("SensorFar".equals(pullParser.getName())) {
            sensorFarValue = Float.parseFloat(pullParser.getAttributeValue(null, "value"));
        }
        if ("SensorWatchTime".equals(pullParser.getName())) {
            sensorWatchTime = Long.parseLong(pullParser.getAttributeValue(null, "support"));
        }
        if ("SensorCheckTimes".equals(pullParser.getName())) {
            sensorCheckTimes = Integer.parseInt(pullParser.getAttributeValue(null, "support"));
        }
        if ("PowerOptimize".equals(pullParser.getName())) {
            powerOptimize = Integer.parseInt(pullParser.getAttributeValue(null, "support"));
        }
    }

    private static void getCodeAndIndexByName(String name, int keyCode, int flag) {
        if (Cover_Screen.equals(name)) {
            coverScreenCode = keyCode;
            coverScreenIndex = flag;
        }
        if (Double_Touch.equals(name)) {
            doubleTouchCode = keyCode;
            doubleTouchIndex = flag;
        }
        if (EasyWakeUp_Flick_ALL.equals(name)) {
            flickALLIndex = flag;
        }
        if (EasyWakeUp_Flick_UP.equals(name)) {
            flickUpCode = keyCode;
            flickUpIndex = flag;
        }
        if (EasyWakeUp_Flick_DOWN.equals(name)) {
            flickDownCode = keyCode;
            flickDownIndex = flag;
        }
        if (EasyWakeUp_Flick_LEFT.equals(name)) {
            flickLeftCode = keyCode;
            flickLeftIndex = flag;
        }
        if (EasyWakeUp_Flick_RIGHT.equals(name)) {
            flickRightCode = keyCode;
            flickRightIndex = flag;
        }
        if (EasyWakeUp_Letter_ALL.equals(name)) {
            letterAllIndex = flag;
        }
        if (EasyWakeUp_Letter_C.equals(name)) {
            letterCCode = keyCode;
            letterCIndex = flag;
        }
        if (EasyWakeUp_Letter_E.equals(name)) {
            letterECode = keyCode;
            letterEIndex = flag;
        }
        if (EasyWakeUp_Letter_M.equals(name)) {
            letterMCode = keyCode;
            letterMIndex = flag;
        }
        if (EasyWakeUp_Letter_W.equals(name)) {
            letterWCode = keyCode;
            letterWIndex = flag;
        }
    }

    public static int getKeyCodeByString(String str) {
        if ("maxKeyCode".equals(str)) {
            return maxKeyCode;
        }
        if ("minKeyCode".equals(str)) {
            return minKeyCode;
        }
        if (Cover_Screen.equals(str)) {
            return coverScreenCode;
        }
        if (Double_Touch.equals(str)) {
            return doubleTouchCode;
        }
        if (EasyWakeUp_Flick_UP.equals(str)) {
            return flickUpCode;
        }
        if (EasyWakeUp_Flick_DOWN.equals(str)) {
            return flickDownCode;
        }
        if (EasyWakeUp_Flick_LEFT.equals(str)) {
            return flickLeftCode;
        }
        if (EasyWakeUp_Flick_RIGHT.equals(str)) {
            return flickRightCode;
        }
        if (EasyWakeUp_Letter_C.equals(str)) {
            return letterCCode;
        }
        if (EasyWakeUp_Letter_E.equals(str)) {
            return letterECode;
        }
        if (EasyWakeUp_Letter_M.equals(str)) {
            return letterMCode;
        }
        if (EasyWakeUp_Letter_W.equals(str)) {
            return letterWCode;
        }
        return -1;
    }

    public static int getCoverScreenIndex() {
        return coverScreenIndex;
    }

    public static int getDoubleTouchIndex() {
        return doubleTouchIndex;
    }

    public static int getFlickAllIndex() {
        return flickALLIndex;
    }

    public static int getFlickUpIndex() {
        return flickUpIndex;
    }

    public static int getFlickDownEIndex() {
        return flickDownIndex;
    }

    public static int getFlickLeftIndex() {
        return flickLeftIndex;
    }

    public static int getFlickRightIndex() {
        return flickRightIndex;
    }

    public static int getLetterAllIndex() {
        return letterAllIndex;
    }

    public static int getLetterCIndex() {
        return letterCIndex;
    }

    public static int getLetterEIndex() {
        return letterEIndex;
    }

    public static int getLetterMIndex() {
        return letterMIndex;
    }

    public static int getLetterWIndex() {
        return letterWIndex;
    }

    public static int getDriverFileLength() {
        return driverFileLength;
    }

    public static String getDriverPostionPath() {
        return driverPostionPath;
    }

    public static String getDriverGesturePath() {
        return driverGesturePath;
    }

    public static float getSensorNearValue() {
        return sensorNearValue;
    }

    public static float getSensorFarValue() {
        return sensorFarValue;
    }

    public static long getSensorWatchTime() {
        return sensorWatchTime;
    }

    public static int getSensorCheckTimes() {
        return sensorCheckTimes;
    }

    public static int getPowerOptimizeState() {
        return powerOptimize;
    }
}
