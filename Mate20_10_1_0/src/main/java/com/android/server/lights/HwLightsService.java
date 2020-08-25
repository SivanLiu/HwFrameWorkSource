package com.android.server.lights;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HwBrightnessProcessor;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.server.hidata.hinetwork.HwHiNetworkParmStatistics;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.policy.HwPolicyFactory;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import com.huawei.displayengine.DisplayEngineManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class HwLightsService extends LightsService {
    private static final int BACKLIGHT_THRESHOLD = 2;
    private static final int BL_DEFAULT = -1;
    /* access modifiers changed from: private */
    public static final boolean DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final int FRAMES_INTERVAL = 16;
    private static final int INDEX_BL_MAX = 0;
    private static final int INDEX_BL_MAX_NIT_ACTUAL = 3;
    private static final int INDEX_BL_MAX_NIT_STANDARD = 4;
    private static final int INDEX_BL_MIN = 1;
    private static final int INDEX_DEVICE_LEVEL = 2;
    private static final int MANUFACTURE_BRIGHTNESS_DEFUALT_VALUE = -1;
    private static final int MANUFACTURE_BRIGHTNESS_SIZE = 4;
    private static final int MAX_BRIGHTNESS = 10000;
    private static final String MAX_BRIGHTNESS_NODE = "/sys/class/leds/lcd_backlight0/max_brightness";
    private static final int MIN_BRIGHTNESS = 156;
    private static final int MSG_UPDATE_BRIGHTNESS = 0;
    private static final int NORMALIZED_DEFAULT_MAX_BRIGHTNESS = 255;
    private static final int NORMALIZED_DEFAULT_MIN_BRIGHTNESS = 4;
    private static final int NORMALIZED_MAX_BRIGHTNESS = 10000;
    private static final String PANEL_INFO_NODE = "/sys/class/graphics/fb0/panel_info";
    private static final int PANEL_INFO_SIZE = 5;
    private static final int REFRESH_FRAMES_CMD = 1;
    private static final int STRING_SPLITED_LENGTH = 2;
    private static final String TAG = "HwLightsService";
    /* access modifiers changed from: private */
    public static boolean sHasShutDown = false;
    private static boolean sInMirrorLinkBrightnessMode = false;
    private boolean mAutoBrightnessEnabled = false;
    private BackLightLevelLogPrinter mBackLightLevelPrinter = null;
    private int mBrightnessLevel = -1;
    /* access modifiers changed from: private */
    public ContentResolver mContentResolver = getContext().getContentResolver();
    private int mCurrentBrightnessLevelForHighPrecision = 0;
    /* access modifiers changed from: private */
    public int mCurrentUserId = 0;
    private int mDeviceActualBrightnessNit = 0;
    private int mDeviceBrightnessLevel = 0;
    private int mDeviceStandardBrightnessNit = 0;
    private DisplayEngineManager mDisplayEngineManager;
    private final ArrayMap<String, HwBrightnessProcessor> mHwBrightnessProcessors = new ArrayMap<>();
    private HwNormalizedBrightnessMapping mHwNormalizedBrightnessMapping;
    private boolean mHwNormalizedBrightnessMappingEnableLoaded = false;
    /* access modifiers changed from: private */
    public volatile boolean mLightsBypass = false;
    private boolean mNeedBrightnessMappingEnable = false;
    private int mNormalizedMaxBrightness = -1;
    private int mNormalizedMinBrightness = -1;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.server.lights.HwLightsService.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (SmartDualCardConsts.SYSTEM_STATE_NAME_ACTION_SHUTDOWN.equals(action)) {
                    Log.i(HwLightsService.TAG, "handle ACTION_SHUTDOWN broadcast");
                    boolean unused = HwLightsService.sHasShutDown = true;
                } else if (SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF.equals(action)) {
                    if (HwLightsService.DEBUG) {
                        Slog.i(HwLightsService.TAG, "handle ACTION_SCREEN_OFF broadcast");
                    }
                    if (HwLightsService.this.mLightsBypass) {
                        int unused2 = HwLightsService.this.setManufactureBrightness(-1, -1, -1, -1);
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public Handler mRefreshFramesHandler = new Handler() {
        /* class com.android.server.lights.HwLightsService.AnonymousClass2 */

        public void handleMessage(Message msg) {
            HwLightsService.this.mRefreshFramesHandler.removeMessages(1);
            AbsLightsService.refreshFrames_native();
            HwLightsService.access$510(HwLightsService.this);
            if (HwLightsService.this.mSblFrameCount > 0) {
                HwLightsService.this.mRefreshFramesHandler.sendEmptyMessageDelayed(1, 16);
            } else if (HwLightsService.this.mSblSetAfterRefresh) {
                HwLightsService hwLightsService = HwLightsService.this;
                hwLightsService.sendSmartBackLightWithRefreshFramesImpl(hwLightsService.mSblId, HwLightsService.this.mSblEnable, HwLightsService.this.mSblLevel, HwLightsService.this.mSblValue, 0, false, 0, 0);
                boolean unused = HwLightsService.this.mSblSetAfterRefresh = false;
            }
        }
    };
    /* access modifiers changed from: private */
    public int mSblEnable;
    /* access modifiers changed from: private */
    public int mSblFrameCount;
    /* access modifiers changed from: private */
    public int mSblId;
    /* access modifiers changed from: private */
    public int mSblLevel;
    /* access modifiers changed from: private */
    public boolean mSblSetAfterRefresh;
    /* access modifiers changed from: private */
    public int mSblValue;
    private boolean mSupportAmoled;
    private boolean mSupportAmoledLoaded;
    private boolean mSupportGammaFix;
    private boolean mSupportGammaFixLoaded;
    private boolean mSupportRgLed;
    private boolean mSupportRgLedLoaded;
    private boolean mSupportXcc;
    private boolean mSupportXccLoaded;
    private Handler mUpdateBrightnessHandler = new Handler() {
        /* class com.android.server.lights.HwLightsService.AnonymousClass3 */

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                int brightnessOut = (int) Math.ceil((double) ((((float) (65535 & HwLightsService.this.mLcdBrightness)) * 255.0f) / 10000.0f));
                Settings.System.putIntForUser(HwLightsService.this.mContentResolver, "screen_auto_brightness", brightnessOut, HwLightsService.this.mCurrentUserId);
                if (brightnessOut != 0 && Settings.System.getIntForUser(HwLightsService.this.mContentResolver, "screen_brightness_mode", 1, HwLightsService.this.mCurrentUserId) == 1) {
                    Settings.System.putIntForUser(HwLightsService.this.mContentResolver, "screen_brightness", brightnessOut, HwLightsService.this.mCurrentUserId);
                }
            }
        }
    };

    static /* synthetic */ int access$510(HwLightsService x0) {
        int i = x0.mSblFrameCount;
        x0.mSblFrameCount = i - 1;
        return i;
    }

    public HwLightsService(Context context) {
        super(context);
        boolean z = false;
        getNormalizedBrightnessRangeFromKernel();
        this.mIsHighPrecision = this.mNormalizedMaxBrightness > 255 ? true : z;
        this.mDisplayEngineManager = new DisplayEngineManager();
        loadHwBrightnessProcessors();
        this.mHwNormalizedBrightnessMapping = new HwNormalizedBrightnessMapping(MIN_BRIGHTNESS, 10000, this.mNormalizedMinBrightness, this.mNormalizedMaxBrightness);
    }

    public void onBootPhase(int phase) {
        HwLightsService.super.onBootPhase(phase);
        if (phase == 1000) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_ACTION_SHUTDOWN);
            filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF);
            getContext().registerReceiver(this.mReceiver, filter);
        }
    }

    public void sendSmartBackLightWithRefreshFramesImpl(int id, int enable, int level, int value, int frames, boolean setAfterRefresh, int enable2, int value2) {
        if (id != 257) {
            Slog.e(TAG, "id = " + id + ", error! this mothod only used for SBL!");
            return;
        }
        synchronized (this) {
            try {
                setLight_native(id, (65535 & (value > 65535 ? 65535 : value)) | ((enable & 1) << 24) | ((level & 255) << 16), 0, 0, 0, 0);
                this.mSblId = id;
                this.mSblFrameCount = frames;
                try {
                    this.mSblSetAfterRefresh = setAfterRefresh;
                    try {
                        this.mSblEnable = enable2;
                        this.mSblLevel = level;
                        this.mSblValue = value2;
                        if (frames > 0) {
                            this.mRefreshFramesHandler.sendEmptyMessage(1);
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void sendUpdateaAutoBrightnessDbMsg() {
        Handler handler;
        if (this.mWriteAutoBrightnessDbEnable && this.mAutoBrightnessEnabled && (handler = this.mUpdateBrightnessHandler) != null) {
            if (handler.hasMessages(0)) {
                this.mUpdateBrightnessHandler.removeMessages(0);
            }
            this.mUpdateBrightnessHandler.sendEmptyMessage(0);
        }
    }

    /* access modifiers changed from: protected */
    public void updateBrightnessMode(boolean mode) {
        this.mAutoBrightnessEnabled = mode;
    }

    /* access modifiers changed from: protected */
    public void updateCurrentUserId(int userId) {
        if (DEBUG) {
            Slog.i(TAG, "user change from  " + this.mCurrentUserId + " into " + userId);
        }
        this.mCurrentUserId = userId;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:34:0x009d, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x009e, code lost:
        $closeResource(r5, r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x00a1, code lost:
        throw r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x00a4, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00a5, code lost:
        $closeResource(r4, r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00a8, code lost:
        throw r5;
     */
    private boolean getBrightnessRangeFromPanelInfo() {
        File file = new File(PANEL_INFO_NODE);
        if (!file.exists()) {
            if (DEBUG) {
                Slog.w(TAG, "getBrightnessRangeFromPanelInfo PANEL_INFO_NODE:/sys/class/graphics/fb0/panel_info isn't exist");
            }
            return false;
        }
        try {
            FileInputStream stream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String text = reader.readLine();
            if (text != null) {
                if (!text.isEmpty()) {
                    if (DEBUG) {
                        Slog.i(TAG, "getBrightnessRangeFromPanelInfo text = " + text);
                    }
                    String[] stringSplited = text.split(",");
                    if (stringSplited.length < 2) {
                        Slog.e(TAG, "split failed! text = " + text);
                        $closeResource(null, reader);
                        $closeResource(null, stream);
                        return false;
                    } else if (parsePanelInfo(stringSplited)) {
                        $closeResource(null, reader);
                        $closeResource(null, stream);
                        return true;
                    } else {
                        $closeResource(null, reader);
                        $closeResource(null, stream);
                        return false;
                    }
                }
            }
            Slog.e(TAG, "getBrightnessRangeFromPanelInfo error! file is empty");
            $closeResource(null, reader);
            $closeResource(null, stream);
            return false;
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "getBrightnessRangeFromPanelInfo error!");
        } catch (IOException e2) {
            Slog.e(TAG, "getBrightnessRangeFromPanelInfo error! IOException");
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
            } catch (Throwable th) {
                x0.addSuppressed(th);
            }
        } else {
            x1.close();
        }
    }

    private boolean parsePanelInfo(String[] stringSplited) {
        if (stringSplited == null) {
            return false;
        }
        String panelInfo = null;
        String[] panelInfos = {"blmax:", "blmin:", "bldevicelevel:", "blmax_nit_actual:", "blmax_nit_standard:"};
        int[] data = {-1, -1, 0, 0, 0};
        int i = 0;
        while (i < panelInfos.length) {
            try {
                panelInfo = panelInfos[i];
                for (int j = 0; j < stringSplited.length; j++) {
                    int index = stringSplited[j].indexOf(panelInfo);
                    if (index != -1) {
                        data[i] = Integer.parseInt(stringSplited[j].substring(panelInfo.length() + index));
                    }
                }
                i++;
            } catch (NumberFormatException e) {
                Slog.e(TAG, "parsePanelInfo() error! " + panelInfo + e);
                return false;
            }
        }
        if (data[0] == -1 || data[1] == -1 || data.length != 5) {
            return false;
        }
        if (DEBUG) {
            Slog.i(TAG, "BrightnessRange success! max = " + data[0] + ", min = " + data[1] + ", deviceLevel = " + data[2] + ",actualMaxNit=" + data[3] + ",standardMaxNit=" + data[4]);
        }
        this.mNormalizedMaxBrightness = data[0];
        this.mNormalizedMinBrightness = data[1];
        this.mDeviceBrightnessLevel = data[2];
        this.mDeviceActualBrightnessNit = data[3];
        this.mDeviceStandardBrightnessNit = data[4];
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0083, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0084, code lost:
        $closeResource(r5, r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0087, code lost:
        throw r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x008a, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x008b, code lost:
        $closeResource(r4, r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x008e, code lost:
        throw r5;
     */
    private boolean getBrightnessRangeFromMaxBrightness() {
        File file = new File(MAX_BRIGHTNESS_NODE);
        if (!file.exists()) {
            if (DEBUG) {
                Slog.w(TAG, "getBrightnessRangeFromMaxBrightness MAX_BRIGHTNESS_NODE:/sys/class/leds/lcd_backlight0/max_brightness isn't exist");
            }
            return false;
        }
        try {
            FileInputStream stream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String text = reader.readLine();
            if (text != null) {
                if (!text.isEmpty()) {
                    this.mNormalizedMaxBrightness = Integer.parseInt(text);
                    this.mNormalizedMinBrightness = (this.mNormalizedMaxBrightness * 4) / 255;
                    if (DEBUG) {
                        Slog.i(TAG, "getBrightnessRangeFromMaxBrightness success! min = " + this.mNormalizedMinBrightness + ", max = " + this.mNormalizedMaxBrightness);
                    }
                    $closeResource(null, reader);
                    $closeResource(null, stream);
                    return true;
                }
            }
            Slog.e(TAG, "getBrightnessRangeFromMaxBrightness error! file is empty");
            $closeResource(null, reader);
            $closeResource(null, stream);
            return false;
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "getBrightnessRangeFromMaxBrightness error!");
            return false;
        } catch (IOException e2) {
            Slog.e(TAG, "getBrightnessRangeFromMaxBrightness error! IOException");
            return false;
        } catch (NumberFormatException e3) {
            Slog.e(TAG, "getBrightnessRangeFromMaxBrightness error! NumberFormatException");
            return false;
        }
    }

    private void checkNormalizedBrightnessRange() {
        int i;
        int i2 = this.mNormalizedMinBrightness;
        if (i2 < 0 || i2 >= (i = this.mNormalizedMaxBrightness) || i > 10000) {
            this.mNormalizedMinBrightness = 4;
            this.mNormalizedMaxBrightness = 255;
            Slog.e(TAG, "checkNormalizedBrightnessRange failed! load default brightness range: min = " + this.mNormalizedMinBrightness + ", max = " + this.mNormalizedMaxBrightness);
            return;
        }
        Slog.i(TAG, "checkNormalizedBrightnessRange success! range: min = " + this.mNormalizedMinBrightness + ", max = " + this.mNormalizedMaxBrightness);
    }

    private void getNormalizedBrightnessRangeFromKernel() {
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
    }

    private int getXccMapIntoRealBacklight(int level) {
        if (!this.mSupportXccLoaded) {
            this.mSupportXcc = this.mDisplayEngineManager.getSupported(16) == 1;
            this.mSupportXccLoaded = true;
            Slog.i(TAG, "mSupportXcc  = " + this.mSupportXcc);
        }
        if (!this.mSupportXcc || this.mLightsBypass) {
            return convertPrecisionHighToLow(level, false);
        }
        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt("MinBrightness", this.mNormalizedMinBrightness);
        bundle.putInt("MaxBrightness", this.mNormalizedMaxBrightness);
        bundle.putInt("brightnesslevel", level);
        int brightnessValue = this.mDisplayEngineManager.setData(6, bundle);
        if (brightnessValue <= 0) {
            return convertPrecisionHighToLow(level, false);
        }
        return brightnessValue;
    }

    private int mapIntoRealBacklightLevelIfNeedXnit(int level) {
        this.mBrightnessLevel = level;
        if (!this.mHwNormalizedBrightnessMappingEnableLoaded) {
            this.mNeedBrightnessMappingEnable = this.mHwNormalizedBrightnessMapping.needBrightnessMappingEnable();
            this.mHwNormalizedBrightnessMappingEnableLoaded = true;
        }
        int brightnessValue = getXccMapIntoRealBacklight(level);
        boolean z = false;
        if (!this.mSupportAmoledLoaded) {
            this.mSupportAmoled = this.mDisplayEngineManager.getSupported(25) == 1;
            this.mSupportAmoledLoaded = true;
            Slog.i(TAG, "mSupportAmoled  = " + this.mSupportAmoled);
        }
        if (!this.mSupportGammaFixLoaded) {
            byte[] status = new byte[1];
            if (this.mDisplayEngineManager.getEffect(7, 0, status, 1) == 0) {
                this.mSupportGammaFix = status[0] == 1;
                Slog.i(TAG, "[effect] getEffect(DE_FEATURE_GAMMA):" + this.mSupportGammaFix);
            }
            this.mSupportGammaFixLoaded = true;
            Slog.i(TAG, "mSupportGammaFix  = " + this.mSupportGammaFix);
        }
        if (!this.mSupportRgLedLoaded) {
            if (this.mDisplayEngineManager.getSupported(19) == 1) {
                z = true;
            }
            this.mSupportRgLed = z;
            this.mSupportRgLedLoaded = true;
            Slog.i(TAG, "mSupportRgLed  = " + this.mSupportRgLed);
        }
        if (!this.mLightsBypass && (this.mSupportAmoled || this.mSupportGammaFix || this.mSupportRgLed)) {
            int brightnessHighPrecision = level;
            if (this.mNeedBrightnessMappingEnable) {
                brightnessHighPrecision = this.mHwNormalizedBrightnessMapping.getMappingBrightnessHighPrecision(level);
            }
            this.mDisplayEngineManager.setScene(26, (brightnessHighPrecision << 16) | brightnessValue);
        }
        return brightnessValue;
    }

    /* access modifiers changed from: protected */
    public int mapIntoRealBacklightLevel(int level) {
        this.mCurrentBrightnessLevelForHighPrecision = level;
        BackLightLevelLogPrinter backLightLevelLogPrinter = this.mBackLightLevelPrinter;
        if (backLightLevelLogPrinter != null) {
            backLightLevelLogPrinter.printLevel(level);
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
        return mapIntoRealBacklightLevelIfNeedXnit(level);
    }

    private static class BackLightLevelLogPrinter {
        private String mLogTag = null;
        private int mPrintedLevel = 0;
        private float mThresholdPercent = 0.1f;

        public BackLightLevelLogPrinter(String logTag, float thresholdPercent) {
            this.mThresholdPercent = thresholdPercent;
            this.mLogTag = logTag;
        }

        /* access modifiers changed from: private */
        public void printLevel(int level) {
            if (HwLightsService.DEBUG) {
                if ((level != 0 || this.mPrintedLevel == 0) && (level == 0 || this.mPrintedLevel != 0)) {
                    int threshold = (int) (((float) this.mPrintedLevel) * this.mThresholdPercent);
                    int threshold2 = 2;
                    if (threshold >= 2) {
                        threshold2 = threshold;
                    }
                    int delta = level - this.mPrintedLevel;
                    if ((delta < 0 ? -delta : delta) > threshold2) {
                        Slog.i(HwLightsService.TAG, this.mLogTag + " = " + level);
                        this.mPrintedLevel = level;
                        return;
                    }
                    return;
                }
                Slog.i(HwLightsService.TAG, this.mLogTag + " = " + level);
                this.mPrintedLevel = level;
            }
        }
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

    /* access modifiers changed from: protected */
    public int getNormalizedMaxBrightness() {
        return this.mNormalizedMaxBrightness;
    }

    public void setMirrorLinkBrightnessStatusInternal(boolean status) {
        Slog.i(TAG, "setMirrorLinkBrightnessStatus  status is " + status);
        sInMirrorLinkBrightnessMode = status;
    }

    /* access modifiers changed from: protected */
    public boolean shouldIgnoreSetBrightness(int brightness, int brightnessMode) {
        if (sInMirrorLinkBrightnessMode) {
            return true;
        }
        if (HwPolicyFactory.isHwFastShutdownEnable()) {
            if (brightness <= 0) {
                return false;
            }
            Slog.i(TAG, "Ignore brightness " + brightness + " during fast shutdown");
            return true;
        } else if (!sHasShutDown) {
            return false;
        } else {
            Slog.i(TAG, "Ignore brightness " + brightness + " during shutdown");
            return true;
        }
    }

    public boolean setHwBrightnessDataImpl(String name, Bundle data, int[] result) {
        HwBrightnessProcessor processor = this.mHwBrightnessProcessors.get(name);
        if (processor != null) {
            return processor.setData(data, result);
        }
        return false;
    }

    public boolean getHwBrightnessDataImpl(String name, Bundle data, int[] result) {
        HwBrightnessProcessor processor = this.mHwBrightnessProcessors.get(name);
        if (processor != null) {
            return processor.getData(data, result);
        }
        return false;
    }

    private void loadHwBrightnessProcessors() {
        this.mHwBrightnessProcessors.put("ManufactureBrightness", new ManufactureBrightnessProcessor());
        this.mHwBrightnessProcessors.put("CurrentBrightness", new CurrentBrightnessProcessor());
    }

    private final class ManufactureBrightnessProcessor extends HwBrightnessProcessor {
        public ManufactureBrightnessProcessor() {
        }

        public boolean setData(Bundle data, int[] retValue) {
            if (retValue == null || retValue.length <= 0) {
                return false;
            }
            if (data == null) {
                Slog.w(HwLightsService.TAG, "setData data == null!");
                int unused = HwLightsService.this.setManufactureBrightness(-1, -1, -1, -1);
                retValue[0] = -1;
                return true;
            }
            retValue[0] = HwLightsService.this.setManufactureBrightness(data.getInt("ManufactureProcess", -1), data.getInt("Scene", -1), data.getInt(HwHiNetworkParmStatistics.LEVEL, -1), data.getInt("AnimationTime", -1));
            return true;
        }
    }

    /* access modifiers changed from: private */
    public int setManufactureBrightness(int manufactureProcess, int scene, int level, int animationTime) {
        boolean isOverrideLight;
        int devicePrecisionLevel;
        int highPrecisionLevel;
        if (DEBUG) {
            Slog.i(TAG, "ManufactureBrightness: manufactureProcess=" + manufactureProcess + ",scene=" + scene + ",level=" + level + ",animationTime=" + animationTime);
        }
        if (manufactureProcess < 0 || scene < 0 || level < 0 || animationTime < 0) {
            highPrecisionLevel = this.mBrightnessLevel;
            devicePrecisionLevel = mapIntoRealBacklightLevelIfNeedXnit(highPrecisionLevel);
            if (this.mNeedBrightnessMappingEnable) {
                highPrecisionLevel = this.mHwNormalizedBrightnessMapping.getMappingBrightnessHighPrecision(highPrecisionLevel);
            }
            isOverrideLight = false;
        } else if (level == 0) {
            highPrecisionLevel = 0;
            devicePrecisionLevel = 0;
            isOverrideLight = true;
        } else {
            int brightness = level;
            if (level < 4) {
                brightness = 4;
            }
            if (level > 255) {
                brightness = 255;
            }
            int highPrecisionLevel2 = convertPrecisionLowToHigh(brightness);
            boolean isFactoryMode = getManufactureBrightnessHbmMode(scene) == ManufactureBrightnessMode.OFF;
            int devicePrecisionLevel2 = convertPrecisionHighToLow(highPrecisionLevel2, isFactoryMode);
            if (!isFactoryMode && this.mNeedBrightnessMappingEnable) {
                highPrecisionLevel2 = this.mHwNormalizedBrightnessMapping.getMappingBrightnessHighPrecision(highPrecisionLevel2);
            }
            isOverrideLight = true;
            highPrecisionLevel = highPrecisionLevel2;
            devicePrecisionLevel = devicePrecisionLevel2;
        }
        setLightsBrightnessOverride(isOverrideLight, devicePrecisionLevel);
        setManufactureBrightnessToDisplayEngine(manufactureProcess, scene, (highPrecisionLevel << 16) | devicePrecisionLevel);
        return 0;
    }

    public boolean isLightsBypassed() {
        return this.mLightsBypass;
    }

    private void setLightsBrightnessOverride(boolean isBypass, int devicePrecisionBrightness) {
        if (DEBUG) {
            Slog.i(TAG, "ManufactureBrightness: setLightsBrightnessOverride isBypass = " + isBypass + ", devicePrecisionBrightness =" + devicePrecisionBrightness);
        }
        this.mLightsBypass = isBypass;
        synchronized (this) {
            setLight_native(AwareDefaultConfigList.HW_PERCEPTIBLE_APP_ADJ, devicePrecisionBrightness, 0, 0, 0, 0);
        }
    }

    private int convertPrecisionHighToLow(int highPrecisionBrightness, boolean isManufactureMode) {
        HwNormalizedBrightnessMapping hwNormalizedBrightnessMapping;
        int brightnessValue = -1;
        if (this.mNeedBrightnessMappingEnable && (hwNormalizedBrightnessMapping = this.mHwNormalizedBrightnessMapping) != null) {
            brightnessValue = isManufactureMode ? hwNormalizedBrightnessMapping.getMappingBrightnessForManufacture(highPrecisionBrightness) : hwNormalizedBrightnessMapping.getMappingBrightness(highPrecisionBrightness);
        }
        if (brightnessValue >= 0) {
            return brightnessValue;
        }
        int i = this.mNormalizedMinBrightness;
        return i + (((highPrecisionBrightness - 156) * (this.mNormalizedMaxBrightness - i)) / 9844);
    }

    private int convertPrecisionLowToHigh(int lowPrecisionBrightness) {
        return (lowPrecisionBrightness * 10000) / 255;
    }

    private ManufactureBrightnessMode getManufactureBrightnessHbmMode(int scene) {
        if (scene == -1) {
            return ManufactureBrightnessMode.DEFAULT;
        }
        if (scene == 0) {
            return ManufactureBrightnessMode.OFF;
        }
        if (scene == 1) {
            return ManufactureBrightnessMode.ON;
        }
        if (scene == 2) {
            return ManufactureBrightnessMode.ON;
        }
        Slog.w(TAG, "ManufactureBrightness: Unsupported scene, using default param!");
        return ManufactureBrightnessMode.DEFAULT;
    }

    private ManufactureBrightnessMode getOledDimmingMode(int scene) {
        if (scene == -1) {
            return ManufactureBrightnessMode.DEFAULT;
        }
        if (scene == 0) {
            return ManufactureBrightnessMode.OFF;
        }
        if (scene == 1) {
            return ManufactureBrightnessMode.OFF;
        }
        if (scene == 2) {
            return ManufactureBrightnessMode.ON;
        }
        Slog.w(TAG, "ManufactureBrightness: Unsupported scene, using default param!");
        return ManufactureBrightnessMode.DEFAULT;
    }

    private void setManufactureBrightnessToDisplayEngine(int manufactureProcess, int scene, int brightness) {
        int[] param = {this.mNormalizedMaxBrightness, brightness, getManufactureBrightnessHbmMode(scene).getValue(), getOledDimmingMode(scene).getValue()};
        PersistableBundle bundle = new PersistableBundle();
        bundle.putIntArray("Buffer", param);
        bundle.putInt("BufferLength", param.length * 4);
        this.mDisplayEngineManager.setData(11, bundle);
    }

    private enum ManufactureBrightnessMode {
        DEFAULT(-1),
        OFF(0),
        ON(1);
        
        private final int mValue;

        private ManufactureBrightnessMode(int value) {
            this.mValue = value;
        }

        /* access modifiers changed from: private */
        public int getValue() {
            return this.mValue;
        }
    }

    private final class CurrentBrightnessProcessor extends HwBrightnessProcessor {
        public CurrentBrightnessProcessor() {
        }

        public boolean getData(Bundle data, int[] retValue) {
            data.putInt("Brightness", HwLightsService.this.getCurrentBrightess());
            return true;
        }
    }

    /* access modifiers changed from: private */
    public int getCurrentBrightess() {
        int currentBrightness = 0;
        int i = this.mCurrentBrightnessLevelForHighPrecision;
        if (i == 0) {
            return 0;
        }
        if (i > 0) {
            currentBrightness = (int) (((((float) i) * 255.0f) / 10000.0f) + 0.5f);
        }
        if (currentBrightness < 4) {
            currentBrightness = 4;
        }
        if (currentBrightness > 255) {
            currentBrightness = 255;
        }
        Slog.i(TAG, "mCurrentlForHighPrecision=" + this.mCurrentBrightnessLevelForHighPrecision + ",currentBrightness=" + currentBrightness);
        return currentBrightness;
    }
}
