package com.huawei.android.hwaps;

import android.app.HwApsInterface;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import java.util.HashMap;

public class SmartLowpowerBrowser implements ISmartLowpowerBrowser {
    private static final int ACTION_DOWN = 0;
    public static final int ACTION_MOVE = 2;
    private static final int ACTION_UP = 1;
    private static final HashMap<String, Integer> BROWSERS_MAP = new HashMap<String, Integer>() {
        {
            put("com.baidu.browser.apps", Integer.valueOf(2));
            put("com.ijinshan.browser_fast", Integer.valueOf(3));
        }
    };
    private static final int BROWSER_BAIDU = 2;
    private static final int BROWSER_LIEBAO = 3;
    private static final int BROWSER_QQ = 1;
    private static final int CHECK_ACTION_INTERVAL_MILLIS = 167;
    private static final boolean CONSEQUENT_FLAG = true;
    private static final boolean DEBUG = true;
    private static final int DETECT_INTERVAL_MILLIS = 500;
    private static final int DETECT_NEWFRAME_BEGIN = 0;
    private static final int DETECT_NEWFRAME_CONTINUE = 3;
    private static final int DETECT_NEWFRAME_INFRAME = 2;
    private static final int DETECT_NEWFRAME_NEW = 1;
    private static final int DETECT_STATE_DETECTING = 2;
    private static final int DETECT_STATE_NOTHING = 4;
    private static final int DETECT_STATE_RECORDING = 1;
    private static final int DETECT_STATE_SKIP = 3;
    private static final int DETECT_STATE_TOUCHING = 0;
    private static final int DIFF_FRAME_COUNT_UP_LIMIT = 3;
    private static final int FLAG_SLB_MODULE = 262144;
    private static final int FRAME_COUNT_UP_LIMIT = 27;
    private static final int GTCC_DETECT_INTERVAL_MILLIS = 3000;
    private static final boolean LOG = false;
    private static final int MAX_DETECT_CYCLE = 3000;
    private static final int MAX_FRAME_ANIMATION_DURATION = 700;
    public static final int MAX_KEYEVENT_DELAY_MS = 200;
    private static final int RESET_INTERVAL_MILLIS = 30000;
    private static final int RETRY_COUNT_UP_LIMIT = 100;
    private static final int STEP_LENGTH = 5;
    private static final String TAG = "Hwaps";
    private static final int UNSAFE_COUNT_UP_LIMIT = 10;
    private static final int VSYNC_INTERVAL_MILLIS = 17;
    private static final int WAIT_BEFORE_DETECT_MILLIS = 2000;
    private static boolean sIsBrowser = LOG;
    private static boolean sIsSLBTurnedOn = LOG;
    private long GTCCStartTimeStamp = 0;
    private boolean GTCCSwitch = LOG;
    private int animation_autofling = 0;
    private int animation_autofling_last = 0;
    private int animation_cssstyle = 0;
    private int animation_cssstyle_last = 0;
    private int animation_frame = 0;
    private int animation_frame_last = 0;
    private int animation_move = 0;
    private int animation_move_last = 0;
    private int animation_script = 0;
    private int animation_script_last = 0;
    private boolean continueGTCCDetect = LOG;
    private int detect1 = 0;
    private int detect1_rec = 0;
    private int detect2 = 0;
    private int detect2_rec = 0;
    private int detect3 = 0;
    private int detect3_rec = 0;
    private int detect4 = 0;
    private int detect4_rec = 0;
    private int detect5 = 0;
    private int detect6 = 0;
    private int detect6_rec = 0;
    private int detectState = 0;
    private int detectStateDiffFrameCount = 0;
    private long detect_frameAnimate_oldMs = 0;
    private int detect_newFrame = 0;
    private int detect_newFrame_last = 0;
    private long detect_oldMs_save = 0;
    private long detestStartTime = 0;
    private int factor_script = 0;
    private int frameCount = 0;
    private long last_drawTime = 0;
    private boolean mDoSkip = true;
    private long mDrawTimeStamp = 0;
    private boolean mDrawing = LOG;
    private boolean mErrorFlag = LOG;
    private boolean mIsFirstCheckBrowserProcess = true;
    private boolean mIsFirstCheckSwitch = true;
    private boolean mIsFrameScheduled = true;
    private boolean mLastDoSkip = true;
    private String mLastKeyCodeMsg = "";
    private int mMessageGroupCount = 0;
    private String mPackageName = "";
    private int mRetryCount = 0;
    private boolean mSafeSkip = true;
    private long mTouchTimeStamp = 0;
    private boolean mTouching = LOG;
    private boolean mTrySkip = true;
    private int mUnsafeCount = 0;
    private int mWhichBrowser = -1;
    private int ncount = 0;
    private int ncount_ani_css = 0;
    private int ncount_ani_script = 0;
    private boolean needTurnOver = LOG;
    private int playingVideo = 0;
    private int skipStateDiffFrameCount = 0;
    private int stepCount = 0;
    private boolean thisTurnSkip = true;
    private long this_drawTime = 0;

    public void setPlayingVideoSLB(boolean isPlayingVideo) {
        this.playingVideo += isPlayingVideo ? 1 : -1;
        if (this.playingVideo < 0) {
            this.playingVideo = 0;
        }
    }

    private void setPkgName(String pkgName) {
        this.mPackageName = pkgName;
    }

    public void setFrameScheduledSLB() {
        this.mIsFrameScheduled = true;
    }

    private void setDrawState() {
        this.mDrawing = true;
        this.mDrawTimeStamp = SystemClock.uptimeMillis();
    }

    private boolean getDrawState() {
        if (!this.mDrawing) {
            return LOG;
        }
        this.mDrawing = LOG;
        return true;
    }

    public boolean getTouchState() {
        return this.mTouching;
    }

    public boolean isSLBSwitchOn() {
        if (this.mIsFirstCheckSwitch) {
            boolean z = true;
            if (!(1 == SystemProperties.getInt("sys.aps.slbswitch", 1) && FLAG_SLB_MODULE == (SystemProperties.getInt("sys.aps.support", 0) & FLAG_SLB_MODULE))) {
                z = LOG;
            }
            sIsSLBTurnedOn = z;
            this.mIsFirstCheckSwitch = LOG;
        }
        if (SystemProperties.getInt("debug.aps.slb", 0) == 4) {
            return LOG;
        }
        return sIsSLBTurnedOn;
    }

    private boolean isBrowserProcess() {
        if (this.mIsFirstCheckBrowserProcess) {
            if (this.mPackageName == null || this.mPackageName.length() == 0) {
                return LOG;
            }
            Integer k = (Integer) BROWSERS_MAP.get(this.mPackageName);
            if (k != null) {
                this.mWhichBrowser = k.intValue();
                sIsBrowser = true;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("APS: SLB: working state: working, mWhichBrowser:");
                stringBuilder.append(this.mWhichBrowser);
                Log.i(str, stringBuilder.toString());
            }
            this.mIsFirstCheckBrowserProcess = LOG;
        }
        return sIsBrowser;
    }

    public void setTouchState(int touchState) {
        boolean z = true;
        if (touchState == 0 || touchState == 1 || touchState == 2) {
            if (touchState == 1) {
                z = LOG;
            }
            this.mTouching = z;
            this.mTouchTimeStamp = SystemClock.uptimeMillis();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS: SLB: mTouchTimeStamp:");
            stringBuilder.append(this.mTouchTimeStamp);
            ApsCommon.logI(str, stringBuilder.toString());
        }
    }

    public static boolean needTouchState() {
        boolean z = LOG;
        if (SystemProperties.getInt("debug.aps.slb", 0) == 4) {
            return LOG;
        }
        if (sIsBrowser && sIsSLBTurnedOn) {
            z = true;
        }
        return z;
    }

    public boolean initSLB(String pkgName) {
        if (!isSLBSwitchOn()) {
            return LOG;
        }
        setPkgName(pkgName);
        return isBrowserProcess();
    }

    public boolean doProcessDrawSLB(long drawingTime, boolean viewScrollChanged, boolean handlingPointerEvent) {
        switch (this.mWhichBrowser) {
            case EventAnalyzed.ACTION_UP /*1*/:
            case 2:
            case 3:
                return skipDraw(drawingTime, viewScrollChanged, handlingPointerEvent);
            default:
                return LOG;
        }
    }

    private void init() {
        this.detect1 = 0;
        this.detect2 = 0;
        this.detect3 = 0;
        this.detect4 = 0;
        this.detect5 = 0;
        this.detect6 = 0;
        this.detect1_rec = 0;
        this.detect2_rec = 0;
        this.detect3_rec = 0;
        this.detect4_rec = 0;
        this.detect6_rec = 0;
        this.detect_newFrame = 0;
        this.detect_newFrame_last = this.detect_newFrame;
        this.this_drawTime = 0;
        this.last_drawTime = 0;
        this.detect_oldMs_save = 0;
        this.detect_frameAnimate_oldMs = 0;
        this.animation_script = 0;
        this.animation_frame = 0;
        this.animation_move = 0;
        this.animation_autofling = 0;
        this.animation_cssstyle = 0;
        this.ncount = 0;
        this.ncount_ani_script = 0;
        this.ncount_ani_css = 0;
        this.factor_script = 0;
    }

    /* JADX WARNING: Missing block: B:88:0x01f8, code skipped:
            r22 = r5;
            r23 = r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean detectAnimation(long drawingTime, boolean viewScrollChanged, boolean handlingPointerEvent) {
        StackTraceElement[] stackTraceElementArr;
        int i;
        long now_Ms;
        long j = drawingTime;
        boolean z = viewScrollChanged;
        boolean z2 = handlingPointerEvent;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        int DETECT_DEPTH = 7;
        if (!(stackTraceElements[7].getClassName().equals("org.chromium.android_webview.AwContents") && stackTraceElements[7].getMethodName().equals("postInvalidateOnAnimation"))) {
            if (this.mWhichBrowser != 2) {
                return LOG;
            }
            DETECT_DEPTH = 8;
            if (!stackTraceElements[8].getClassName().equals("org.chromium.android_webview.AwContents")) {
                stackTraceElements = null;
            } else if (!stackTraceElements[8].getMethodName().equals("postInvalidateOnAnimation")) {
                stackTraceElementArr = stackTraceElements;
                stackTraceElements = null;
            }
            return stackTraceElements;
        }
        int FEATURE_DEPTH = DETECT_DEPTH + 1;
        this.detect1_rec = 0;
        this.detect2_rec = 0;
        this.detect3_rec = 0;
        this.detect4_rec = 0;
        this.detect6_rec = 0;
        if (stackTraceElements[FEATURE_DEPTH].getClassName().equals("org.chromium.content.browser.ContentViewCore") && stackTraceElements[FEATURE_DEPTH].getMethodName().equals("nativeOnTouchEvent")) {
            this.detect1++;
            this.detect1_rec = 1;
        } else if (stackTraceElements[FEATURE_DEPTH].getClassName().equals("org.chromium.android_webview.AwContents") && stackTraceElements[FEATURE_DEPTH].getMethodName().equals("nativeScrollTo")) {
            this.detect2++;
            this.detect2_rec = 1;
        } else if (stackTraceElements[FEATURE_DEPTH].getClassName().equals("org.chromium.android_webview.AwContents") && stackTraceElements[FEATURE_DEPTH].getMethodName().equals("nativeOnDraw")) {
            this.detect3++;
            this.detect3_rec = 1;
        } else if (stackTraceElements[FEATURE_DEPTH].getClassName().equals("org.chromium.base.SystemMessageHandler") && stackTraceElements[FEATURE_DEPTH].getMethodName().equals("nativeDoRunLoopOnce")) {
            this.detect4++;
            this.detect4_rec = 1;
            this.detect5++;
        } else if (stackTraceElements[FEATURE_DEPTH].getClassName().equals("org.chromium.ui.base.WindowAndroid") && stackTraceElements[FEATURE_DEPTH].getMethodName().equals("nativeOnVSync")) {
            this.detect6++;
            this.detect6_rec = 1;
        } else {
            i = DETECT_DEPTH;
            return LOG;
        }
        long now_Ms2 = SystemClock.uptimeMillis();
        this.this_drawTime = j;
        this.detect_newFrame = 2;
        long now_Ms3 = now_Ms2;
        if (this.this_drawTime > this.last_drawTime) {
            if (0 == this.last_drawTime) {
                this.detect_newFrame = 0;
                this.detect_newFrame_last = this.detect_newFrame;
            } else if (now_Ms3 - this.last_drawTime > 3000) {
                init();
                this.detect_newFrame = 0;
                this.detect_newFrame_last = this.detect_newFrame;
            } else if (this.this_drawTime - this.last_drawTime >= 34 || now_Ms3 - this.this_drawTime >= 34) {
                this.detect_newFrame = 1;
            } else {
                this.detect_newFrame = 3;
            }
        }
        switch (this.detect_newFrame) {
            case EventAnalyzed.ACTION_UP /*1*/:
            case 3:
                int i2;
                now_Ms = now_Ms3;
                this.detect_newFrame_last = this.detect_newFrame;
                this.animation_move = 0;
                this.animation_autofling = 0;
                if (this.detect1 > 0) {
                    i2 = 1;
                    this.animation_move = 1;
                } else {
                    i2 = 1;
                    if (this.detect2 > 0) {
                        this.animation_autofling = 1;
                    }
                }
                this.detect1 = 0;
                this.detect2 = 0;
                if (i2 != this.animation_move) {
                    if (i2 != this.animation_autofling) {
                        int i3;
                        int i4;
                        long delta = this.mTouchTimeStamp - this.this_drawTime;
                        if (delta >= 0 && delta <= 17) {
                            i3 = 1;
                            this.animation_move = 1;
                        } else if (z || z2) {
                            i3 = 1;
                            this.animation_autofling = 1;
                        } else {
                            i3 = 1;
                        }
                        this.animation_script = 0;
                        if (this.detect3 > 0) {
                            this.animation_script = i3;
                        } else if (this.detect6 > 0) {
                            this.animation_script = i3;
                        }
                        this.detect3 = 0;
                        this.detect6 = 0;
                        this.animation_cssstyle = 0;
                        if (this.detect5 > 0) {
                            stackTraceElementArr = stackTraceElements;
                            i = DETECT_DEPTH;
                            if (this.this_drawTime - this.last_drawTime < 34 && now_Ms - this.this_drawTime < 34) {
                                this.animation_cssstyle = 1;
                            }
                        } else {
                            i = DETECT_DEPTH;
                        }
                        this.detect5 = 0;
                        if (now_Ms - this.detect_frameAnimate_oldMs > 700) {
                            this.detect_frameAnimate_oldMs = now_Ms;
                            if (this.animation_move != 0 || this.animation_autofling != 0) {
                                this.animation_frame = 3;
                                if (this.detect4 == 0) {
                                    i4 = 0;
                                    this.animation_frame = 0;
                                } else {
                                    i4 = 0;
                                }
                                if (1 == this.animation_cssstyle) {
                                    this.animation_frame = i4;
                                }
                            } else if (this.detect4 == 0) {
                                i4 = 0;
                                this.animation_frame = 0;
                            } else {
                                if (this.detect4 > 1 && this.detect4 < 12) {
                                    this.animation_frame = 1;
                                } else if (this.detect4 > 15 && this.detect4 < 22) {
                                    this.animation_frame = 2;
                                } else if (1 == this.animation_cssstyle) {
                                    i4 = 0;
                                    this.animation_frame = 0;
                                } else {
                                    this.animation_frame = 3;
                                }
                                i4 = 0;
                            }
                            this.detect4 = i4;
                        } else {
                            i4 = 0;
                        }
                        this.animation_frame = i4;
                        break;
                    }
                }
                i = DETECT_DEPTH;
                break;
                break;
            case 2:
                if (this.detect_newFrame_last != 0) {
                    if (now_Ms3 - this.last_drawTime >= 100) {
                        if (now_Ms3 - this.detect_frameAnimate_oldMs <= 700) {
                            now_Ms = now_Ms3;
                            stackTraceElementArr = stackTraceElements;
                            i = DETECT_DEPTH;
                            break;
                        }
                        now_Ms = now_Ms3;
                        this.detect_frameAnimate_oldMs = now_Ms;
                        if (this.animation_move != 0 || this.animation_autofling != 0) {
                            this.animation_frame = 12;
                        } else if (this.detect4 > 1 && this.detect4 < UNSAFE_COUNT_UP_LIMIT) {
                            this.animation_frame = UNSAFE_COUNT_UP_LIMIT;
                        } else if (this.detect4 == 0) {
                            this.animation_frame = 0;
                        } else {
                            this.animation_frame = 11;
                        }
                        this.detect4 = 0;
                    }
                } else {
                    this.detect_newFrame = 0;
                }
                now_Ms = now_Ms3;
                break;
            default:
                now_Ms = now_Ms3;
                break;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("APS: SLB: AnimateDetect@:[");
        stringBuilder.append(this.detect_newFrame);
        stringBuilder.append("] script:");
        stringBuilder.append(this.animation_script);
        stringBuilder.append(" css:");
        stringBuilder.append(this.animation_cssstyle);
        stringBuilder.append(" frame:");
        stringBuilder.append(this.animation_frame);
        stringBuilder.append(" move:");
        stringBuilder.append(this.animation_move);
        stringBuilder.append(" autofling:");
        stringBuilder.append(this.animation_autofling);
        stringBuilder.append(" [");
        stringBuilder.append(this.detect1_rec);
        stringBuilder.append(",");
        stringBuilder.append(this.detect2_rec);
        stringBuilder.append(",");
        stringBuilder.append(this.detect3_rec);
        stringBuilder.append(",");
        stringBuilder.append(this.detect4_rec);
        stringBuilder.append(",");
        stringBuilder.append(this.detect6_rec);
        stringBuilder.append("] now_Ms:");
        stringBuilder.append(now_Ms);
        stringBuilder.append(", drawingTime: ");
        stringBuilder.append(j);
        stringBuilder.append(", last_drawTime: ");
        stringBuilder.append(this.last_drawTime);
        stringBuilder.append(" ,viewScrollChanged:");
        stringBuilder.append(z);
        stringBuilder.append(" ,handlingPointerEvent:");
        stringBuilder.append(z2);
        stringBuilder.append(", mTouchTimeStamp:");
        stringBuilder.append(this.mTouchTimeStamp);
        ApsCommon.logI(str, stringBuilder.toString());
        this.last_drawTime = this.this_drawTime;
        return true;
    }

    private boolean skipDraw(long drawingTime, boolean viewScrollChanged, boolean handlingPointerEvent) {
        int i = 0;
        if (this.playingVideo > 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS: SLB: media playingVideo:");
            stringBuilder.append(this.playingVideo);
            ApsCommon.logI(str, stringBuilder.toString());
            this.detectState = 0;
            return LOG;
        }
        boolean discard = LOG;
        int diffValue = -2;
        long now_Ms = SystemClock.uptimeMillis();
        if (now_Ms - this.mTouchTimeStamp < 2000) {
            this.detectState = 0;
        } else if (this.detectState == 0) {
            this.detectState = 1;
        }
        switch (this.detectState) {
            case EventAnalyzed.ACTION_DOWN /*0*/:
                detectAnimation(drawingTime, viewScrollChanged, handlingPointerEvent);
                if (this.GTCCSwitch) {
                    switchGTCC(LOG);
                    break;
                }
                break;
            case EventAnalyzed.ACTION_UP /*1*/:
                if (detectAnimation(drawingTime, viewScrollChanged, handlingPointerEvent)) {
                    this.detestStartTime = now_Ms;
                    this.animation_script_last = this.animation_script;
                    this.animation_cssstyle_last = this.animation_cssstyle;
                    this.animation_frame_last = this.animation_frame;
                    this.animation_move_last = this.animation_move;
                    this.animation_autofling_last = this.animation_autofling;
                    this.detectState = 2;
                    this.frameCount = 0;
                    switchGTCC(true);
                    this.detectStateDiffFrameCount = 0;
                    break;
                }
                break;
            case 2:
                if (detectAnimation(drawingTime, viewScrollChanged, handlingPointerEvent)) {
                    if ((1 == this.animation_script || 1 == this.animation_cssstyle) && this.animation_frame == 0 && this.animation_move == 0 && this.animation_autofling == 0) {
                        this.frameCount++;
                    } else {
                        this.frameCount = 0;
                    }
                    diffValue = getGTCCDiffValue();
                    if (diffValue != 0) {
                        this.detectStateDiffFrameCount++;
                    }
                    if (now_Ms - this.detestStartTime > 500) {
                        if (this.frameCount < FRAME_COUNT_UP_LIMIT) {
                            switchGTCC(LOG);
                            this.detectState = 4;
                            break;
                        }
                        this.needTurnOver = true;
                        this.thisTurnSkip = true;
                        this.stepCount = 0;
                        this.continueGTCCDetect = this.detectStateDiffFrameCount < 3 ? true : LOG;
                        if (this.continueGTCCDetect) {
                            this.skipStateDiffFrameCount = 0;
                            this.GTCCStartTimeStamp = SystemClock.uptimeMillis();
                        } else {
                            switchGTCC(LOG);
                        }
                        this.detectState = 3;
                        discard = true;
                        break;
                    }
                }
                break;
            case 3:
                if (this.needTurnOver) {
                    this.thisTurnSkip ^= 1;
                } else {
                    this.stepCount++;
                    this.thisTurnSkip = true;
                    if (this.stepCount >= 5) {
                        this.thisTurnSkip = LOG;
                        this.stepCount = 0;
                    }
                }
                if (this.continueGTCCDetect) {
                    if (now_Ms - this.GTCCStartTimeStamp < 3000) {
                        diffValue = getGTCCDiffValue();
                        if (diffValue != 0) {
                            int i2 = this.skipStateDiffFrameCount + 1;
                            this.skipStateDiffFrameCount = i2;
                            if (i2 >= 3) {
                                this.continueGTCCDetect = LOG;
                                switchGTCC(LOG);
                            }
                        }
                    } else {
                        this.continueGTCCDetect = LOG;
                        this.needTurnOver = LOG;
                        this.thisTurnSkip = true;
                        switchGTCC(LOG);
                    }
                }
                if (4 != getKeyCode()) {
                    discard = this.thisTurnSkip;
                    break;
                }
                ApsCommon.logI(TAG, "APS: SLB: key event coming, stop skip !");
                this.detectState = 0;
                discard = LOG;
                break;
            default:
                discard = LOG;
                break;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("APS: SLB: discard5: ");
        if (discard) {
            i = 1;
        }
        stringBuilder2.append(i);
        stringBuilder2.append(" in:[");
        stringBuilder2.append(this.detectState);
        stringBuilder2.append("], playingVideo:");
        stringBuilder2.append(this.playingVideo);
        stringBuilder2.append(", needTurnOver:");
        stringBuilder2.append(this.needTurnOver);
        stringBuilder2.append("  [ script:");
        stringBuilder2.append(this.animation_script);
        stringBuilder2.append("|");
        stringBuilder2.append(this.animation_script_last);
        stringBuilder2.append(" css:");
        stringBuilder2.append(this.animation_cssstyle);
        stringBuilder2.append("|");
        stringBuilder2.append(this.animation_cssstyle_last);
        stringBuilder2.append(" frame:");
        stringBuilder2.append(this.animation_frame);
        stringBuilder2.append("|");
        stringBuilder2.append(this.animation_frame_last);
        stringBuilder2.append(" move:");
        stringBuilder2.append(this.animation_move);
        stringBuilder2.append("|");
        stringBuilder2.append(this.animation_move_last);
        stringBuilder2.append(" autofling:");
        stringBuilder2.append(this.animation_autofling);
        stringBuilder2.append("|");
        stringBuilder2.append(this.animation_autofling_last);
        stringBuilder2.append("], now_Ms: ");
        stringBuilder2.append(now_Ms);
        stringBuilder2.append(", detestStartTime:");
        stringBuilder2.append(this.detestStartTime);
        stringBuilder2.append(" , mTouchTimeStamp:");
        stringBuilder2.append(this.mTouchTimeStamp);
        stringBuilder2.append(" , handlingPointerEvent:");
        stringBuilder2.append(handlingPointerEvent);
        stringBuilder2.append(" , continueGTCCDetect:");
        stringBuilder2.append(this.continueGTCCDetect);
        stringBuilder2.append(" , detectStateDiffFrameCount:");
        stringBuilder2.append(this.detectStateDiffFrameCount);
        stringBuilder2.append(" , skipStateDiffFrameCount:");
        stringBuilder2.append(this.skipStateDiffFrameCount);
        stringBuilder2.append(" , diffValue:");
        stringBuilder2.append(diffValue);
        stringBuilder2.append(" , stepCount:");
        stringBuilder2.append(this.stepCount);
        ApsCommon.logI(str2, stringBuilder2.toString());
        return discard;
    }

    private void parseGTCCResult(String queryResultStr, int[] gtcc) {
        int i = 0;
        String str;
        try {
            gtcc[0] = -1;
            if (queryResultStr != null) {
                if (!queryResultStr.isEmpty()) {
                    String[] numbers = queryResultStr.split("\\|");
                    int length = numbers.length;
                    if (length != 5) {
                        str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("APS: SLB: parseGTCCResult fail! length:");
                        stringBuilder.append(length);
                        stringBuilder.append(", qr:");
                        stringBuilder.append(queryResultStr);
                        Log.e(str, stringBuilder.toString());
                        return;
                    }
                    while (i < length) {
                        if (!(numbers[i] == null || numbers[i].isEmpty() || !numbers[i].matches("[-\\+]?[0-9]+"))) {
                            gtcc[i] = Integer.valueOf(numbers[i]).intValue();
                        }
                        i++;
                    }
                    return;
                }
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("APS: SLB: qr is empty or null. qr:");
            stringBuilder2.append(queryResultStr);
            ApsCommon.logI(str2, stringBuilder2.toString());
        } catch (Exception e) {
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("APS: SLB: parseGTCCResult fail! qr:");
            stringBuilder3.append(queryResultStr);
            stringBuilder3.append(", Exception:");
            stringBuilder3.append(e);
            Log.e(str, stringBuilder3.toString());
        }
    }

    private void switchGTCC(boolean onoff) {
        try {
            HwApsInterface.nativeSetNeedGTCCResult(onoff);
            this.GTCCSwitch = onoff;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS: SLB: switchGTCC fail1! Exception:");
            stringBuilder.append(e);
            ApsCommon.logI(str, stringBuilder.toString());
        }
    }

    private int getGTCCDiffValue() {
        try {
            int[] gtccQueryResult = new int[5];
            parseGTCCResult(HwApsInterface.nativeGetGTCCResult(), gtccQueryResult);
            return gtccQueryResult[0];
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS: SLB: getGTCCDiffValue fail2! Exception:");
            stringBuilder.append(e);
            ApsCommon.logI(str, stringBuilder.toString());
            return -2;
        }
    }

    private int getKeyCode() {
        int keycode = -1;
        String msg;
        try {
            msg = SystemProperties.get("sys.aps.keycode", "");
            if (!(msg.length() == 0 || msg.equals(this.mLastKeyCodeMsg))) {
                String[] infos = msg.split("\\|");
                if (2 == infos.length && isCurrentKeyEvent(infos[0])) {
                    keycode = Integer.valueOf(infos[1]).intValue();
                }
                this.mLastKeyCodeMsg = msg;
            }
            return keycode;
        } catch (Exception e) {
            msg = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS: SLB: getKeyCode fail! Exception:");
            stringBuilder.append(e);
            Log.e(msg, stringBuilder.toString());
            return -1;
        }
    }

    private boolean isCurrentKeyEvent(String info) {
        boolean result = LOG;
        if (info != null) {
            try {
                if (!info.isEmpty() && info.matches("[-\\+]?[0-9]+")) {
                    long delay = SystemClock.uptimeMillis() - Long.valueOf(info).longValue();
                    if (delay >= 0 && delay <= 200) {
                        result = true;
                    }
                }
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("APS: SLB: isCurrentKeyEvent fail! Exception:");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return LOG;
            }
        }
        return result;
    }
}
