package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.WifiDisplay;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.huawei.android.hardware.display.HwWifiDisplayParameters;
import com.huawei.android.net.wifi.p2p.WifiP2pManagerCommonEx;
import com.huawei.android.pgmng.plug.PowerKit;
import com.huawei.displayengine.IDisplayEngineService;
import java.util.regex.Pattern;

public class HwWifiDisplayControllerEx implements IHwWifiDisplayControllerEx {
    private static final int BITRATE_2G_BASE = 6;
    private static final int BITRATE_2G_MAXIMUM = 8;
    private static final int BITRATE_2G_MINIMUM = 3;
    private static final int BITRATE_5G_BASE = 8;
    private static final int BITRATE_5G_MAXIMUM = 12;
    private static final int BITRATE_5G_MINIMUM = 5;
    private static final int BITRATE_BAND_2G = 1;
    private static final int BITRATE_BAND_5G = 2;
    private static final int BITRATE_COLOR_LIMITED = 5;
    private static final int BITRATE_NORMAL_LINKSPEED = 72;
    private static final String EXTRA_LINKSPEED = "linkSpeed";
    private static final boolean HWFLOW = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String TAG = "HwWifiDisplayControllerEx";
    private static final String VIDEO_PLAY_STATE_FULL_SCREEN = "HWE_VIDEO_START";
    private static final String VIDEO_PLAY_STATE_IDLE = "HWE_VIDEO_STOP";
    private static final String WFD_LINKSPEED_INFO = "com.huawei.net.wifi.p2p.LINK_SPEED";
    /* access modifiers changed from: private */
    public boolean isColorHoldNeeded = false;
    private int mBitrateMax;
    private int mBitrateMin;
    private Context mContext;
    /* access modifiers changed from: private */
    public int mConvertRatio = 9;
    private Handler mHandler;
    private boolean mIsContentProtectionRequired = true;
    private boolean mIsUibcRequired = true;
    boolean mIsWorkOn2G = false;
    /* access modifiers changed from: private */
    public int mLastBitrate;
    private int mLastScanChannelId;
    private int mMiracastConnectionErrorCode = -1;
    private PowerKit mPowerKit;
    private int mProjectionScene = 1;
    private PowerKit.Sink mStateRecognitionListener = new PowerKit.Sink() {
        /* class com.android.server.display.HwWifiDisplayControllerEx.AnonymousClass3 */

        public void onStateChanged(int stateType, int eventType, int pid, String pkg, int uid) {
            boolean colorNeedBk = HwWifiDisplayControllerEx.this.isColorHoldNeeded;
            if (stateType == 10016 || stateType == 10017) {
                boolean unused = HwWifiDisplayControllerEx.this.isColorHoldNeeded = false;
                if (stateType == 10016 && HwWifiDisplayControllerEx.this.mWfdc.getmRemoteDisplay() != null) {
                    Slog.i(HwWifiDisplayControllerEx.TAG, "video play idle.");
                    HwWifiDisplayControllerEx.this.mWfdc.getmRemoteDisplay().sendWifiDisplayAction(HwWifiDisplayControllerEx.VIDEO_PLAY_STATE_IDLE);
                }
            } else if (eventType == 1) {
                boolean unused2 = HwWifiDisplayControllerEx.this.isColorHoldNeeded = true;
                if (stateType == 10015 && HwWifiDisplayControllerEx.this.mWfdc.getmRemoteDisplay() != null) {
                    Slog.i(HwWifiDisplayControllerEx.TAG, "video play with full screen");
                    HwWifiDisplayControllerEx.this.mWfdc.getmRemoteDisplay().sendWifiDisplayAction(HwWifiDisplayControllerEx.VIDEO_PLAY_STATE_FULL_SCREEN);
                }
            } else {
                boolean unused3 = HwWifiDisplayControllerEx.this.isColorHoldNeeded = false;
            }
            if (colorNeedBk != HwWifiDisplayControllerEx.this.isColorHoldNeeded) {
                HwWifiDisplayControllerEx hwWifiDisplayControllerEx = HwWifiDisplayControllerEx.this;
                hwWifiDisplayControllerEx.changeVideoBitrate(hwWifiDisplayControllerEx.mLastBitrate * HwWifiDisplayControllerEx.this.mConvertRatio);
            }
            Slog.i(HwWifiDisplayControllerEx.TAG, "isColorHoldNeeded:" + HwWifiDisplayControllerEx.this.isColorHoldNeeded);
        }
    };
    private String mVerificaitonCode = null;
    /* access modifiers changed from: private */
    public IWifiDisplayControllerInner mWfdc;
    private final BroadcastReceiver mWifiP2pReceiver = new BroadcastReceiver() {
        /* class com.android.server.display.HwWifiDisplayControllerEx.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (HwWifiDisplayControllerEx.WFD_LINKSPEED_INFO.equals(intent.getAction())) {
                int linkSpeed = intent.getIntExtra(HwWifiDisplayControllerEx.EXTRA_LINKSPEED, 0);
                Slog.i(HwWifiDisplayControllerEx.TAG, "receive linkSpeed: " + linkSpeed);
                HwWifiDisplayControllerEx.this.changeVideoBitrate(linkSpeed);
            }
        }
    };

    public HwWifiDisplayControllerEx(IWifiDisplayControllerInner wfdc, Context context, Handler handler) {
        this.mWfdc = wfdc;
        this.mContext = context;
        this.mHandler = handler;
        this.mLastScanChannelId = 0;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WFD_LINKSPEED_INFO);
        this.mContext.registerReceiver(this.mWifiP2pReceiver, intentFilter, null, this.mHandler);
    }

    public void requestStartScan(int channelID) {
        this.mLastScanChannelId = channelID;
        this.mWfdc.requestStartScanInner();
        this.mLastScanChannelId = 0;
    }

    public boolean tryDiscoverPeersEx() {
        if (this.mLastScanChannelId == 0) {
            return false;
        }
        Slog.d(TAG, "used tryDiscoverPeersEx " + this.mLastScanChannelId);
        WifiP2pManagerCommonEx.discoverPeers(this.mWfdc.getWifiP2pChannelInner(), this.mLastScanChannelId, new WifiP2pManager.ActionListener() {
            /* class com.android.server.display.HwWifiDisplayControllerEx.AnonymousClass2 */

            public void onSuccess() {
                if (HwWifiDisplayControllerEx.this.mWfdc.getmDiscoverPeersInProgress()) {
                    HwWifiDisplayControllerEx.this.mWfdc.requestPeersEx();
                }
            }

            public void onFailure(int reason) {
                Slog.d(HwWifiDisplayControllerEx.TAG, "Discover peers failed with reason " + reason + ".");
            }
        });
        this.mWfdc.postDelayedDiscover();
        return true;
    }

    public void setConnectParameters(boolean isSupportHdcp, boolean isUibcError, HwWifiDisplayParameters parameters) {
        this.mIsContentProtectionRequired = !isSupportHdcp;
        this.mIsUibcRequired = !isUibcError;
        if (parameters != null) {
            this.mVerificaitonCode = parameters.getVerificaitonCode();
            this.mProjectionScene = parameters.getProjectionScene();
        }
    }

    public void setDisplayParameters() {
        if (this.mWfdc.getmRemoteDisplay() != null) {
            StringBuilder param = new StringBuilder();
            param.append("isCPRequired=");
            String str = "true";
            param.append(this.mIsContentProtectionRequired ? str : "false");
            param.append(";isUibcRequired=");
            if (!this.mIsUibcRequired) {
                str = "false";
            }
            param.append(str);
            String str2 = this.mVerificaitonCode;
            if (str2 != null && str2.length() > 0) {
                param.append(";vcode=");
                param.append(this.mVerificaitonCode);
            }
            param.append(";projectionScene=");
            param.append(this.mProjectionScene);
            this.mWfdc.getmRemoteDisplay().setDisplayParameters(param.toString());
        }
    }

    public void resetDisplayParameters() {
        this.mIsContentProtectionRequired = true;
        this.mIsUibcRequired = true;
        this.mVerificaitonCode = "";
        this.mProjectionScene = 1;
    }

    public void checkVerificationResult(boolean isRight) {
        if (this.mWfdc.getmRemoteDisplay() != null) {
            Slog.i(TAG, "VerificationResult " + isRight);
            this.mWfdc.getmRemoteDisplay().checkVerificationResult(isRight);
            if (!isRight) {
                this.mWfdc.disconnectInner();
            }
        }
    }

    public void setVideoBitrate() {
        if (this.mWfdc.getmRemoteDisplay() != null) {
            if (this.mIsWorkOn2G) {
                this.mLastBitrate = 6;
                this.mBitrateMin = 3;
                this.mBitrateMax = 8;
                this.mConvertRatio = 12;
            } else {
                this.mLastBitrate = 8;
                this.mBitrateMin = 5;
                this.mBitrateMax = 12;
                this.mConvertRatio = 9;
            }
            this.mWfdc.getmRemoteDisplay().setVideoBitrate(this.mLastBitrate);
        }
    }

    public void setWorkFrequency(int frequency) {
        boolean z = true;
        if (1 != convertFrequencyToBand(frequency)) {
            z = false;
        }
        this.mIsWorkOn2G = z;
    }

    private int convertFrequencyToBand(int frequency) {
        Slog.i(TAG, "miracast work on frequency : " + frequency);
        if (frequency < 2412 || frequency > 2484) {
            return 2;
        }
        return 1;
    }

    /* access modifiers changed from: private */
    public void changeVideoBitrate(int linkSpeed) {
        int usefulBandwith = linkSpeed / this.mConvertRatio;
        if (this.isColorHoldNeeded && usefulBandwith < 5) {
            usefulBandwith = 5;
        }
        if (SystemProperties.getInt("wfd.config.bitrate", 0) != 0) {
            usefulBandwith = SystemProperties.getInt("wfd.config.bitrate", 0);
        }
        Slog.i(TAG, "usefulBandwith: " + usefulBandwith + ", mLastBitrate " + this.mLastBitrate);
        int i = this.mLastBitrate;
        if (usefulBandwith < i) {
            if (usefulBandwith < this.mBitrateMin) {
                usefulBandwith = this.mBitrateMin;
            }
        } else if (usefulBandwith > i && usefulBandwith > this.mBitrateMax) {
            usefulBandwith = this.mBitrateMax;
        }
        if (usefulBandwith != this.mLastBitrate && this.mWfdc.getmRemoteDisplay() != null) {
            this.mLastBitrate = usefulBandwith;
            Slog.i(TAG, "update videoBitrate: " + this.mLastBitrate);
            this.mWfdc.getmRemoteDisplay().setVideoBitrate(usefulBandwith);
        }
    }

    public void registerPGStateEvent() {
        try {
            this.mPowerKit = PowerKit.getInstance();
            if (this.mPowerKit != null) {
                this.mPowerKit.enableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT);
                this.mPowerKit.enableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_2DGAME_FRONT);
                this.mPowerKit.enableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_START);
                this.mPowerKit.enableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_END);
                this.mPowerKit.enableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_CAMERA_FRONT);
                this.mPowerKit.enableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_CAMERA_END);
                this.mPowerKit.enableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_GALLERY_FRONT);
                return;
            }
            Slog.i(TAG, "mPowerKit is null, init failed !!!");
        } catch (RemoteException e) {
            this.mPowerKit = null;
            Slog.i(TAG, "enableStateEvent failed !!!");
        }
    }

    public void unregisterPGStateEvent() {
        PowerKit powerKit = this.mPowerKit;
        if (powerKit != null) {
            try {
                powerKit.disableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_3DGAME_FRONT);
                this.mPowerKit.disableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_2DGAME_FRONT);
                this.mPowerKit.disableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_START);
                this.mPowerKit.disableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_VIDEO_END);
                this.mPowerKit.disableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_CAMERA_FRONT);
                this.mPowerKit.disableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_CAMERA_END);
                this.mPowerKit.disableStateEvent(this.mStateRecognitionListener, (int) IDisplayEngineService.DE_ACTION_PG_GALLERY_FRONT);
            } catch (RemoteException e) {
                Slog.i(TAG, "disableStateEvent failed !!!");
            } catch (Throwable th) {
                this.mPowerKit = null;
                throw th;
            }
            this.mPowerKit = null;
        }
    }

    public void advertisDisplayCasting(final WifiDisplay display) {
        Slog.i(TAG, "advertisDisplayCasting ");
        this.mHandler.post(new Runnable() {
            /* class com.android.server.display.HwWifiDisplayControllerEx.AnonymousClass4 */

            public void run() {
                HwWifiDisplayControllerEx.this.mWfdc.getmListener().onDisplayCasting(display);
            }
        });
    }

    public void sendWifiDisplayAction(String action) {
        if (this.mWfdc.getmRemoteDisplay() != null) {
            Slog.i(TAG, "sendWifiDisplayAction " + action);
            this.mWfdc.getmRemoteDisplay().sendWifiDisplayAction(action);
        }
    }

    public void updateConnectionErrorCode(int errorCode) {
        this.mMiracastConnectionErrorCode = errorCode;
    }

    public int getConnectionErrorCode() {
        return this.mMiracastConnectionErrorCode;
    }

    private boolean checkVerificaitonCodeValid(String verificationCode) {
        if (TextUtils.isEmpty(verificationCode)) {
            Slog.e(TAG, "Verification code is empty.");
            return false;
        } else if (Pattern.compile("^[0-9A-Z]{4}$").matcher(verificationCode).matches()) {
            return true;
        } else {
            Slog.e(TAG, "The pattern of verification code is error.");
            return false;
        }
    }

    public void displayDataNotify(final String displayData) {
        Slog.i(TAG, "displayDataNotify ");
        this.mHandler.post(new Runnable() {
            /* class com.android.server.display.HwWifiDisplayControllerEx.AnonymousClass5 */

            public void run() {
                HwWifiDisplayControllerEx.this.mWfdc.getmListener().onDisplayDataInfo(displayData);
            }
        });
    }
}
