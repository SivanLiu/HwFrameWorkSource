package com.android.server.lights;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings.System;
import android.util.Log;
import android.util.Slog;
import com.huawei.displayengine.DisplayEngineManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class HwLightsService extends LightsService {
    private static boolean DEBUG = false;
    private static final int MAX_BRIGHTNESS = 10000;
    private static String MAX_BRIGHTNESS_NODE = "/sys/class/leds/lcd_backlight0/max_brightness";
    private static final int MIN_BRIGHTNESS = 156;
    private static final int MSG_UPDATE_BRIGHTNESS = 0;
    private static final int NORMALIZED_DEFAULT_MAX_BRIGHTNESS = 255;
    private static final int NORMALIZED_DEFAULT_MIN_BRIGHTNESS = 4;
    private static final int NORMALIZED_MAX_BRIGHTNESS = 10000;
    private static String PANEL_INFO_NODE = "/sys/class/graphics/fb0/panel_info";
    private static final int REFRESH_FRAMES_CMD = 1;
    private static final int SRE_REFRESH_FRAMES_CMD = 1;
    static final String TAG = "HwLightsService";
    private boolean mAutoBrightnessEnabled = false;
    private BackLightLevelLogPrinter mBackLightLevelPrinter = null;
    private ContentResolver mContentResolver = getContext().getContentResolver();
    private int mCurrentUserId = 0;
    private int mDeviceActualBrightnessNit = 0;
    private int mDeviceBrightnessLevel = 0;
    private int mDeviceStandardBrightnessNit = 0;
    private DisplayEngineManager mDisplayEngineManager;
    private int mNormalizedMaxBrightness = -1;
    private int mNormalizedMinBrightness = -1;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && "android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                Log.i(HwLightsService.TAG, "handle ACTION_SHUTDOWN broadcast");
                LightsService.mHasShutDown = true;
            }
        }
    };
    private Handler mRefreshFramesHandler = new Handler() {
        public void handleMessage(Message msg) {
            HwLightsService.this.mRefreshFramesHandler.removeMessages(1);
            LightsService.refreshFrames_native();
            HwLightsService.this.mSBLFrameCount = HwLightsService.this.mSBLFrameCount - 1;
            if (HwLightsService.this.mSBLFrameCount > 0) {
                HwLightsService.this.mRefreshFramesHandler.sendEmptyMessageDelayed(1, 16);
            } else if (HwLightsService.this.mSBLSetAfterRefresh) {
                HwLightsService.this.sendSmartBackLightWithRefreshFramesImpl(HwLightsService.this.mSBLId, HwLightsService.this.mSBLEnable, HwLightsService.this.mSBLLevel, HwLightsService.this.mSBLValue, 0, false, 0, 0);
                HwLightsService.this.mSBLSetAfterRefresh = false;
            }
        }
    };
    private int mSBLEnable;
    private int mSBLFrameCount;
    private int mSBLId;
    private int mSBLLevel;
    private boolean mSBLSetAfterRefresh;
    private int mSBLValue;
    private int mSREFrameCount;
    private int mSREId;
    private Handler mSRERefreshFramesHandler = new Handler() {
        public void handleMessage(Message msg) {
            HwLightsService.this.mSRERefreshFramesHandler.removeMessages(1);
            LightsService.refreshFrames_native();
            HwLightsService.this.mSREFrameCount = HwLightsService.this.mSREFrameCount - 1;
            if (HwLightsService.this.mSREFrameCount > 0) {
                HwLightsService.this.mSRERefreshFramesHandler.sendEmptyMessageDelayed(1, 16);
            } else if (HwLightsService.this.mSRESetAfterRefresh) {
                LightsService.setLight_native(HwLightsService.this.mSREId, HwLightsService.this.mSREValue, 0, 0, 0, 0);
                HwLightsService.this.mSRESetAfterRefresh = false;
            }
        }
    };
    private boolean mSRESetAfterRefresh;
    private int mSREValue;
    private int mSupportAmoled = 0;
    private boolean mSupportAmoled_isloaded = false;
    private int mSupportGammaFix = 0;
    private boolean mSupportGammaFix_isloaded = false;
    private int mSupportRGLed = 0;
    private boolean mSupportRGLed_isloaded = false;
    private int mSupportXCC = 0;
    private boolean mSupportXCC_isloaded = false;
    private Handler mUpdateBrightnessHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                int brightnessOut = (int) Math.ceil((double) ((((float) (65535 & HwLightsService.this.getLcdBrightnessMode())) * 255.0f) / 10000.0f));
                System.putIntForUser(HwLightsService.this.mContentResolver, "screen_auto_brightness", brightnessOut, HwLightsService.this.mCurrentUserId);
                if (brightnessOut != 0) {
                    System.putIntForUser(HwLightsService.this.mContentResolver, "screen_brightness", brightnessOut, HwLightsService.this.mCurrentUserId);
                }
            }
        }
    };

    private static class BackLightLevelLogPrinter {
        private String mLogTag = null;
        private int mPrintedLevel = 0;
        private float mThresholdPercent = 0.1f;

        public BackLightLevelLogPrinter(String logTag, float thresholdPercent) {
            this.mThresholdPercent = thresholdPercent;
            this.mLogTag = logTag;
        }

        public void printLevel(int level) {
            if (!HwLightsService.DEBUG) {
                return;
            }
            if ((level != 0 || this.mPrintedLevel == 0) && (level == 0 || this.mPrintedLevel != 0)) {
                int threshold = (int) (((float) this.mPrintedLevel) * this.mThresholdPercent);
                int i = 2;
                if (threshold >= 2) {
                    i = threshold;
                }
                threshold = i;
                i = level - this.mPrintedLevel;
                if ((i < 0 ? -i : i) > threshold) {
                    String str = HwLightsService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mLogTag);
                    stringBuilder.append(" = ");
                    stringBuilder.append(level);
                    Slog.i(str, stringBuilder.toString());
                    this.mPrintedLevel = level;
                }
                return;
            }
            String str2 = HwLightsService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mLogTag);
            stringBuilder2.append(" = ");
            stringBuilder2.append(level);
            Slog.i(str2, stringBuilder2.toString());
            this.mPrintedLevel = level;
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public HwLightsService(Context context) {
        super(context);
        boolean z = false;
        getNormalizedBrightnessRangeFromKernel();
        if (this.mNormalizedMaxBrightness > 255) {
            z = true;
        }
        this.mIsHighPrecision = z;
        setBackLightMaxLevel_native(this.mNormalizedMaxBrightness);
        this.mDisplayEngineManager = new DisplayEngineManager();
    }

    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (phase == 1000) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.ACTION_SHUTDOWN");
            getContext().registerReceiver(this.mReceiver, filter);
        }
    }

    private void refreshSmartBackLightFrames(int id, int count, boolean setAfterRefresh, int enable, int level, int value) {
        this.mSBLId = id;
        this.mSBLFrameCount = count;
        this.mSBLSetAfterRefresh = setAfterRefresh;
        this.mSBLEnable = enable;
        this.mSBLLevel = level;
        this.mSBLValue = value;
        if (count > 0) {
            this.mRefreshFramesHandler.sendEmptyMessage(1);
        }
    }

    public void sendSmartBackLightWithRefreshFramesImpl(int id, int enable, int level, int value, int frames, boolean setAfterRefresh, int enable2, int value2) {
        int i = id;
        if (i != 257) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("id = ");
            stringBuilder.append(i);
            stringBuilder.append(", error! this mothod only used for SBL!");
            Slog.e(str, stringBuilder.toString());
            return;
        }
        synchronized (this) {
            int i2 = value;
            int i3 = level;
            setLight_native(i, (65535 & (i2 > 65535 ? 65535 : i2)) | (((enable & 1) << 24) | ((i3 & 255) << 16)), 0, 0, 0, 0);
            refreshSmartBackLightFrames(i, frames, setAfterRefresh, enable2, i3, value2);
        }
    }

    private void refreshSREFrames(int id, int count, boolean setAfterRefresh, int value) {
        this.mSREId = id;
        this.mSREFrameCount = count;
        this.mSRESetAfterRefresh = setAfterRefresh;
        this.mSREValue = value;
        if (count > 0) {
            this.mSRERefreshFramesHandler.sendEmptyMessage(1);
        }
    }

    public void sendSREWithRefreshFramesImpl(int id, int enable, int ambientLightThreshold, int ambientLight, int frames, boolean setAfterRefresh, int enable2, int ambientLight2) {
        Throwable th;
        synchronized (this) {
            int ambientLightThresholdMin = 4095;
            int i = ambientLightThreshold;
            if (i <= 4095) {
                ambientLightThresholdMin = i;
            }
            int i2 = ambientLight;
            int ambientLightMin = i2 > 65535 ? 65535 : i2;
            int sreValue2 = (65535 & ambientLightMin) | (((enable2 & 1) << 28) | ((ambientLightThresholdMin & 4095) << 16));
            try {
                setLight_native(id, (((enable & 1) << 28) | ((ambientLightThresholdMin & 4095) << 16)) | (ambientLightMin & 65535), 0, 0, 0, 0);
                refreshSREFrames(id, frames, setAfterRefresh, sreValue2);
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    protected void sendUpdateaAutoBrightnessDbMsg() {
        if (this.mWriteAutoBrightnessDbEnable && this.mAutoBrightnessEnabled && this.mUpdateBrightnessHandler != null) {
            if (this.mUpdateBrightnessHandler.hasMessages(0)) {
                this.mUpdateBrightnessHandler.removeMessages(0);
            }
            this.mUpdateBrightnessHandler.sendEmptyMessage(0);
        }
    }

    protected void updateBrightnessMode(boolean mode) {
        this.mAutoBrightnessEnabled = mode;
    }

    protected void updateCurrentUserId(int userId) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("user change from  ");
            stringBuilder.append(this.mCurrentUserId);
            stringBuilder.append(" into ");
            stringBuilder.append(userId);
            Slog.d(str, stringBuilder.toString());
        }
        this.mCurrentUserId = userId;
    }

    private boolean getBrightnessRangeFromPanelInfo() {
        File file = new File(PANEL_INFO_NODE);
        if (file.exists()) {
            BufferedReader reader = null;
            FileInputStream fis = null;
            String readLine;
            StringBuilder stringBuilder;
            try {
                fis = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                readLine = reader.readLine();
                String tempString = readLine;
                if (readLine != null) {
                    if (DEBUG) {
                        readLine = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("getBrightnessRangeFromPanelInfo String = ");
                        stringBuilder.append(tempString);
                        Slog.i(readLine, stringBuilder.toString());
                    }
                    if (tempString.length() == 0) {
                        Slog.e(TAG, "getBrightnessRangeFromPanelInfo error! String is null");
                        reader.close();
                        close(reader, fis);
                        return false;
                    }
                    String[] stringSplited = tempString.split(",");
                    if (stringSplited.length < 2) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("split failed! String = ");
                        stringBuilder2.append(tempString);
                        Slog.e(str, stringBuilder2.toString());
                        reader.close();
                        close(reader, fis);
                        return false;
                    } else if (parsePanelInfo(stringSplited)) {
                        reader.close();
                        close(reader, fis);
                        return true;
                    }
                }
            } catch (FileNotFoundException e) {
                Slog.e(TAG, "getBrightnessRangeFromPanelInfo error! FileNotFoundException");
            } catch (IOException e2) {
                readLine = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getBrightnessRangeFromPanelInfo error! IOException ");
                stringBuilder.append(e2);
                Slog.e(readLine, stringBuilder.toString());
            } catch (Exception e3) {
                readLine = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getBrightnessRangeFromPanelInfo error! Exception ");
                stringBuilder.append(e3);
                Slog.e(readLine, stringBuilder.toString());
            } catch (Throwable th) {
                close(reader, fis);
            }
            close(reader, fis);
            return false;
        }
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getBrightnessRangeFromPanelInfo PANEL_INFO_NODE:");
            stringBuilder3.append(PANEL_INFO_NODE);
            stringBuilder3.append(" isn't exist");
            Slog.w(str2, stringBuilder3.toString());
        }
        return false;
    }

    private boolean parsePanelInfo(String[] stringSplited) {
        if (stringSplited == null) {
            return false;
        }
        String key = null;
        int index = -1;
        int standardMaxNit = 0;
        int actualMaxNit = 0;
        int deviceLevel = 0;
        int min = -1;
        int max = -1;
        int i = 0;
        while (i < stringSplited.length) {
            try {
                key = "blmax:";
                index = stringSplited[i].indexOf(key);
                if (index != -1) {
                    max = Integer.parseInt(stringSplited[i].substring(key.length() + index));
                } else {
                    key = "blmin:";
                    index = stringSplited[i].indexOf(key);
                    if (index != -1) {
                        min = Integer.parseInt(stringSplited[i].substring(key.length() + index));
                    } else {
                        key = "bldevicelevel:";
                        index = stringSplited[i].indexOf(key);
                        if (index != -1) {
                            deviceLevel = Integer.parseInt(stringSplited[i].substring(key.length() + index));
                        } else {
                            key = "blmax_nit_actual:";
                            index = stringSplited[i].indexOf(key);
                            if (index != -1) {
                                actualMaxNit = Integer.parseInt(stringSplited[i].substring(key.length() + index));
                            } else {
                                key = "blmax_nit_standard:";
                                index = stringSplited[i].indexOf(key);
                                if (index != -1) {
                                    standardMaxNit = Integer.parseInt(stringSplited[i].substring(key.length() + index));
                                }
                            }
                        }
                    }
                }
                i++;
            } catch (NumberFormatException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("parsePanelInfo() error! ");
                stringBuilder.append(key);
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
                return false;
            }
        }
        if (max == -1 || min == -1) {
            return false;
        }
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getBrightnessRangeFromPanelInfo success! min = ");
            stringBuilder2.append(min);
            stringBuilder2.append(", max = ");
            stringBuilder2.append(max);
            stringBuilder2.append(", deviceLevel = ");
            stringBuilder2.append(deviceLevel);
            stringBuilder2.append(",actualMaxNit=");
            stringBuilder2.append(actualMaxNit);
            stringBuilder2.append(",standardMaxNit=");
            stringBuilder2.append(standardMaxNit);
            Slog.i(str2, stringBuilder2.toString());
        }
        this.mNormalizedMaxBrightness = max;
        this.mNormalizedMinBrightness = min;
        this.mDeviceBrightnessLevel = deviceLevel;
        this.mDeviceActualBrightnessNit = actualMaxNit;
        this.mDeviceStandardBrightnessNit = standardMaxNit;
        return true;
    }

    private boolean getBrightnessRangeFromMaxBrightness() {
        File file = new File(MAX_BRIGHTNESS_NODE);
        if (file.exists()) {
            BufferedReader reader = null;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                String readLine = reader.readLine();
                String tempString = readLine;
                if (readLine != null) {
                    this.mNormalizedMaxBrightness = Integer.parseInt(tempString);
                    this.mNormalizedMinBrightness = (this.mNormalizedMaxBrightness * 4) / 255;
                    if (DEBUG) {
                        readLine = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getBrightnessRangeFromMaxBrightness success! min = ");
                        stringBuilder.append(this.mNormalizedMinBrightness);
                        stringBuilder.append(", max = ");
                        stringBuilder.append(this.mNormalizedMaxBrightness);
                        Slog.i(readLine, stringBuilder.toString());
                    }
                    reader.close();
                    close(reader, fis);
                    return true;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            } catch (Exception e3) {
                e3.printStackTrace();
            } catch (Throwable th) {
                close(null, null);
            }
            close(reader, fis);
            return false;
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getBrightnessRangeFromMaxBrightness MAX_BRIGHTNESS_NODE:");
            stringBuilder2.append(MAX_BRIGHTNESS_NODE);
            stringBuilder2.append(" isn't exist");
            Slog.w(str, stringBuilder2.toString());
        }
        return false;
    }

    private void checkNormalizedBrightnessRange() {
        String str;
        StringBuilder stringBuilder;
        if (this.mNormalizedMinBrightness < 0 || this.mNormalizedMinBrightness >= this.mNormalizedMaxBrightness || this.mNormalizedMaxBrightness > 10000) {
            this.mNormalizedMinBrightness = 4;
            this.mNormalizedMaxBrightness = 255;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkNormalizedBrightnessRange failed! load default brightness range: min = ");
            stringBuilder.append(this.mNormalizedMinBrightness);
            stringBuilder.append(", max = ");
            stringBuilder.append(this.mNormalizedMaxBrightness);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("checkNormalizedBrightnessRange success! range: min = ");
        stringBuilder.append(this.mNormalizedMinBrightness);
        stringBuilder.append(", max = ");
        stringBuilder.append(this.mNormalizedMaxBrightness);
        Slog.i(str, stringBuilder.toString());
    }

    protected void getNormalizedBrightnessRangeFromKernel() {
        try {
            if (getBrightnessRangeFromPanelInfo()) {
                checkNormalizedBrightnessRange();
                return;
            }
            if (DEBUG) {
                Slog.w(TAG, "getBrightnessRangeFromPanelInfo failed");
            }
            if (getBrightnessRangeFromMaxBrightness()) {
                checkNormalizedBrightnessRange();
                return;
            }
            if (DEBUG) {
                Slog.w(TAG, "getBrightnessRangeFromMaxBrightness failed");
            }
            checkNormalizedBrightnessRange();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int mapIntoRealBacklightLevelIfNeedXNit(int level) {
        String str;
        StringBuilder stringBuilder;
        int brightnessvalue = level;
        if (!this.mSupportXCC_isloaded) {
            this.mSupportXCC = this.mDisplayEngineManager.getSupported(16);
            this.mSupportXCC_isloaded = true;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mSupportXCC  = ");
            stringBuilder.append(this.mSupportXCC);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mSupportXCC == 1) {
            PersistableBundle bundle = new PersistableBundle();
            bundle.putInt("MinBrightness", this.mNormalizedMinBrightness);
            bundle.putInt("MaxBrightness", this.mNormalizedMaxBrightness);
            bundle.putInt("brightnesslevel", brightnessvalue);
            brightnessvalue = this.mDisplayEngineManager.setData(6, bundle);
            if (brightnessvalue <= 0) {
                brightnessvalue = this.mNormalizedMinBrightness + (((level - 156) * (this.mNormalizedMaxBrightness - this.mNormalizedMinBrightness)) / 9844);
            }
        } else {
            brightnessvalue = this.mNormalizedMinBrightness + (((level - 156) * (this.mNormalizedMaxBrightness - this.mNormalizedMinBrightness)) / 9844);
        }
        if (!this.mSupportAmoled_isloaded) {
            this.mSupportAmoled = this.mDisplayEngineManager.getSupported(25);
            this.mSupportAmoled_isloaded = true;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mSupportAmoled  = ");
            stringBuilder.append(this.mSupportAmoled);
            Slog.i(str, stringBuilder.toString());
        }
        if (!this.mSupportGammaFix_isloaded) {
            String str2;
            StringBuilder stringBuilder2;
            byte[] status = new byte[1];
            if (this.mDisplayEngineManager.getEffect(7, 0, status, 1) == 0) {
                this.mSupportGammaFix = status[0];
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[effect] getEffect(DE_FEATURE_GAMMA):");
                stringBuilder2.append(this.mSupportGammaFix);
                Slog.i(str2, stringBuilder2.toString());
            }
            this.mSupportGammaFix_isloaded = true;
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mSupportGammaFix  = ");
            stringBuilder2.append(this.mSupportGammaFix);
            Slog.i(str2, stringBuilder2.toString());
        }
        if (!this.mSupportRGLed_isloaded) {
            this.mSupportRGLed = this.mDisplayEngineManager.getSupported(19);
            this.mSupportRGLed_isloaded = true;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mSupportRGLed  = ");
            stringBuilder.append(this.mSupportRGLed);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mSupportAmoled == 1 || this.mSupportGammaFix == 1 || this.mSupportRGLed == 1) {
            this.mDisplayEngineManager.setScene(26, (level << 16) | brightnessvalue);
        }
        return brightnessvalue;
    }

    protected int mapIntoRealBacklightLevel(int level) {
        if (this.mBackLightLevelPrinter != null) {
            this.mBackLightLevelPrinter.printLevel(level);
        } else {
            this.mBackLightLevelPrinter = new BackLightLevelLogPrinter("back light level before map", 0.1f);
        }
        if (level == 0) {
            return 0;
        }
        if (level < MIN_BRIGHTNESS) {
            return this.mNormalizedMinBrightness;
        }
        if (level > 10000) {
            return this.mNormalizedMaxBrightness;
        }
        return mapIntoRealBacklightLevelIfNeedXNit(level);
    }

    public int getDeviceActualBrightnessLevelImpl() {
        return this.mDeviceBrightnessLevel;
    }

    public int getDeviceActualBrightnessNitImpl() {
        return this.mDeviceActualBrightnessNit;
    }

    public int getDeviceStandardBrightnessNitImpl() {
        return this.mDeviceStandardBrightnessNit;
    }

    protected int getNormalizedMaxBrightness() {
        return this.mNormalizedMaxBrightness;
    }

    private void close(BufferedReader reader, FileInputStream fis) {
        if (reader != null || fis != null) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }
}
