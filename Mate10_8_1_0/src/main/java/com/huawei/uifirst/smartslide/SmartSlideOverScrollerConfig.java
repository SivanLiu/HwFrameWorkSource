package com.huawei.uifirst.smartslide;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.util.Log;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class SmartSlideOverScrollerConfig {
    private static final String DECELERATION_TIME_CONSTANT = "decelerationTimeConstant";
    private static final String DECELERATION_TIME_CONSTANT_DEFAULT = "-0.405";
    private static final String DECELERATION_TIME_SLOPE = "decelerationTimeSlope";
    private static final String DECELERATION_TIME_SLOPE_DEFAULT = "0.528";
    private static final String DEVICE_SCREEN_SIZE_DEFAULT = "6.0";
    private static final String EXP_COEFFICIENT = "expCoefficient";
    private static final String EXP_COEFFICIENT_DEFAULT = "4.2";
    private static final String EXP_COFFICIENT_SLOW_DOWN = "expCofficientSlowDown";
    private static final String EXP_COFFICIENT_SLOW_DOWN_DEFAULT = "6";
    private static final String FLING_TIME_THRESHOLD = "flingTimeThreshold";
    private static final String FLING_TIME_THRESHOLD_DEFAULT = "900.0";
    private static final int ID_OVERSCROLLER_CONFIG = 34340864;
    private static final String IS_ENABLE = "isEnable";
    private static final String IS_ENABLE_DEFAULT = "false";
    private static final String LOG_TAG = "OverScrollerOptimization";
    private static final int MAX_READ_FROM_FILE = 3000;
    private static final String OVERSCROLLER_CONFIG = "overscroller_config";
    private static final String OVERSCROLLER_DEVICE_FILE_PATH = "sys/devices/virtual/graphics/fb0/lcd_model";
    private static final String PACKAGE_NAME = "packageName";
    private static final String SCREEN_DISPLAY_PIXELS = "screendisplaypixels";
    private static final String VELOCITY_MULTIPLIER = "velocityMultiplier";
    private static final String VELOCITY_MULTIPLIER_DEFAULT = "1.5";
    private HashMap<String, String> mReadConfigData = new HashMap();

    private java.util.HashMap<java.lang.String, java.lang.String> parseXML(android.content.Context r11, java.lang.String r12) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x006c in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r10 = this;
        r0 = new java.util.HashMap;
        r0.<init>();
        r6 = 0;
        r7 = r11.getResources();	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r8 = 34340864; // 0x20c0000 float:1.0285576E-37 double:1.6966641E-316;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r6 = r7.getXml(r8);	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r3 = r6.next();	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
    L_0x0014:
        r8 = 1;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        if (r3 == r8) goto L_0x0059;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
    L_0x0017:
        r8 = 2;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        if (r3 != r8) goto L_0x0054;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
    L_0x001a:
        r1 = r6.getAttributeCount();	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r8 = r6.getName();	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r9 = "overscroller_config";	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r8 = r8.equals(r9);	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        if (r8 == 0) goto L_0x0054;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
    L_0x002b:
        r5 = 0;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
    L_0x002c:
        if (r5 >= r1) goto L_0x003c;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
    L_0x002e:
        r8 = r6.getAttributeName(r5);	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r9 = r6.getAttributeValue(r5);	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r0.put(r8, r9);	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r5 = r5 + 1;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        goto L_0x002c;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
    L_0x003c:
        r8 = "packageName";	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r8 = r0.get(r8);	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r8 = (java.lang.String) r8;	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        r8 = r8.equals(r12);	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        if (r8 == 0) goto L_0x0051;
    L_0x004b:
        if (r6 == 0) goto L_0x0050;
    L_0x004d:
        r6.close();
    L_0x0050:
        return r0;
    L_0x0051:
        r0.clear();	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
    L_0x0054:
        r3 = r6.next();	 Catch:{ XmlPullParserException -> 0x0066, IOException -> 0x005f, all -> 0x006d }
        goto L_0x0014;
    L_0x0059:
        if (r6 == 0) goto L_0x005e;
    L_0x005b:
        r6.close();
    L_0x005e:
        return r0;
    L_0x005f:
        r4 = move-exception;
        if (r6 == 0) goto L_0x0065;
    L_0x0062:
        r6.close();
    L_0x0065:
        return r0;
    L_0x0066:
        r2 = move-exception;
        if (r6 == 0) goto L_0x006c;
    L_0x0069:
        r6.close();
    L_0x006c:
        return r0;
    L_0x006d:
        r8 = move-exception;
        if (r6 == 0) goto L_0x0073;
    L_0x0070:
        r6.close();
    L_0x0073:
        throw r8;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.huawei.uifirst.smartslide.SmartSlideOverScrollerConfig.parseXML(android.content.Context, java.lang.String):java.util.HashMap<java.lang.String, java.lang.String>");
    }

    private String getCallerProcessName(Context context) {
        String callingApp = "";
        int uid = Binder.getCallingUid();
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            callingApp = pm.getNameForUid(uid);
        }
        if (callingApp == null) {
            return "";
        }
        if (callingApp.indexOf(":") != -1) {
            callingApp = callingApp.split(":")[0].trim();
        }
        Log.e("xuquanfei", "xuquanfei_getCallerProcessName is used callingApp = " + callingApp);
        return callingApp;
    }

    public float getScreenHeight_ByResources(Context context) {
        return (float) context.getResources().getDisplayMetrics().heightPixels;
    }

    public float getScreenWidth_ByResources(Context context) {
        return (float) context.getResources().getDisplayMetrics().widthPixels;
    }

    public float getScreenPPI_ByResources(Context context) {
        float screenHeight = (float) context.getResources().getDisplayMetrics().heightPixels;
        float screenWidth = (float) context.getResources().getDisplayMetrics().widthPixels;
        float screenSize = Float.parseFloat(getScreenSize_ByDeviceFile());
        if (screenSize <= 0.0f) {
            screenSize = Float.parseFloat(DEVICE_SCREEN_SIZE_DEFAULT);
        }
        return (float) (Math.sqrt((double) ((screenHeight * screenHeight) + (screenWidth * screenWidth))) / ((double) screenSize));
    }

    private String readLcdDeviceFile(String lcdDeviceFile) throws Exception {
        Throwable th;
        Closeable closeable = null;
        StringBuilder lcdProp = null;
        try {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(new FileInputStream(lcdDeviceFile), StandardCharsets.UTF_8));
            try {
                StringBuilder lcdProp2 = new StringBuilder();
                while (true) {
                    int intC = stdin.read();
                    if (intC == -1) {
                        break;
                    }
                    char c = (char) intC;
                    if (c == '\n') {
                        break;
                    }
                    try {
                        if (lcdProp2.length() >= MAX_READ_FROM_FILE) {
                            throw new Exception("input too long");
                        }
                        lcdProp2.append(c);
                    } catch (Throwable th2) {
                        th = th2;
                        lcdProp = lcdProp2;
                        closeable = stdin;
                    }
                }
                String stringBuilder = lcdProp2.toString();
                if (lcdProp2 != null) {
                    lcdProp2.delete(0, lcdProp2.length());
                }
                closeFileStreamNotThrow(stdin);
                return stringBuilder;
            } catch (Throwable th3) {
                th = th3;
                Object stdin2 = stdin;
                if (lcdProp != null) {
                    lcdProp.delete(0, lcdProp.length());
                }
                closeFileStreamNotThrow(closeable);
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            if (lcdProp != null) {
                lcdProp.delete(0, lcdProp.length());
            }
            closeFileStreamNotThrow(closeable);
            throw th;
        }
    }

    private void closeFileStreamNotThrow(Closeable fis) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "can't delete FileStream");
            }
        }
    }

    public String getScreenSize_ByDeviceFile() {
        String lcdPropFromFile = "";
        String screenSize = DEVICE_SCREEN_SIZE_DEFAULT;
        try {
            String[] arr = readLcdDeviceFile(OVERSCROLLER_DEVICE_FILE_PATH).split(" ");
            for (int i = 0; i < arr.length; i++) {
                if (arr[i].indexOf("'") != -1) {
                    screenSize = arr[i].substring(0, arr[i].length() - 1);
                    break;
                }
            }
            if (screenSize == null || screenSize.isEmpty()) {
                screenSize = DEVICE_SCREEN_SIZE_DEFAULT;
            }
            return screenSize;
        } catch (Exception e) {
            return DEVICE_SCREEN_SIZE_DEFAULT;
        }
    }

    public HashMap<String, String> getOverScrollerConfig(Context context) {
        Log.i(LOG_TAG, "get the overscroller config");
        String packageName = getCallerProcessName(context);
        if (packageName.length() == 0) {
            this.mReadConfigData.put(EXP_COEFFICIENT, EXP_COEFFICIENT_DEFAULT);
            this.mReadConfigData.put(DECELERATION_TIME_SLOPE, DECELERATION_TIME_SLOPE_DEFAULT);
            this.mReadConfigData.put(DECELERATION_TIME_CONSTANT, DECELERATION_TIME_CONSTANT_DEFAULT);
            this.mReadConfigData.put(FLING_TIME_THRESHOLD, FLING_TIME_THRESHOLD_DEFAULT);
            this.mReadConfigData.put(EXP_COFFICIENT_SLOW_DOWN, EXP_COFFICIENT_SLOW_DOWN_DEFAULT);
            this.mReadConfigData.put(VELOCITY_MULTIPLIER, VELOCITY_MULTIPLIER_DEFAULT);
            this.mReadConfigData.put(IS_ENABLE, IS_ENABLE_DEFAULT);
        } else {
            this.mReadConfigData = parseXML(context, packageName);
            isKeyExist(EXP_COEFFICIENT, EXP_COEFFICIENT_DEFAULT);
            isKeyExist(DECELERATION_TIME_SLOPE, DECELERATION_TIME_SLOPE_DEFAULT);
            isKeyExist(DECELERATION_TIME_CONSTANT, DECELERATION_TIME_CONSTANT_DEFAULT);
            isKeyExist(FLING_TIME_THRESHOLD, FLING_TIME_THRESHOLD_DEFAULT);
            isKeyExist(EXP_COFFICIENT_SLOW_DOWN, EXP_COFFICIENT_SLOW_DOWN_DEFAULT);
            isKeyExist(VELOCITY_MULTIPLIER, VELOCITY_MULTIPLIER_DEFAULT);
            isKeyExist(IS_ENABLE, IS_ENABLE_DEFAULT);
        }
        return this.mReadConfigData;
    }

    private void isKeyExist(String keyInput, String defaultValueInput) {
        if (!this.mReadConfigData.containsKey(keyInput)) {
            this.mReadConfigData.put(keyInput, defaultValueInput);
        }
    }
}
