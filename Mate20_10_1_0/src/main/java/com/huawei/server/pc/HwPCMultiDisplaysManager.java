package com.huawei.server.pc;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.HwPCUtils;
import android.view.Display;
import com.huawei.android.os.UEventObserverExt;
import com.huawei.android.view.DisplayEx;
import com.huawei.server.hwmultidisplay.hicar.HiCarManager;
import java.util.ArrayList;

public final class HwPCMultiDisplaysManager {
    private static final String DP_STATE_DEVPATH = "DEVPATH=/devices/virtual/hw_typec/typec";
    public static final int FIRST_DISPLAY = 0;
    private static final int INVALID_ARG = -1;
    public static final int SECOND_DISPLAY = 1;
    private static final String TAG = "HwPCMultiDisplaysManager";
    private boolean isInitialDpConnectAfterBoot = true;
    private ArrayList<CastingDisplay> mCastingDisplays = new ArrayList<>();
    private Context mContext;
    private DisplayManager mDisplayManager;
    private final UEventObserverExt mDpObserver = new UEventObserverExt() {
        /* class com.huawei.server.pc.HwPCMultiDisplaysManager.AnonymousClass1 */

        public void onUEvent(UEventObserverExt.UEvent event) {
            HwPCUtils.log(HwPCMultiDisplaysManager.TAG, "DP state event = " + event);
            if (event != null) {
                String DP_STATE = event.get("DP_STATE");
                HwPCUtils.log(HwPCMultiDisplaysManager.TAG, "DP state DP_STATE: " + DP_STATE + ", mIsDpState = " + HwPCMultiDisplaysManager.this.mIsDpState);
                if ("ON".equals(DP_STATE) && !HwPCMultiDisplaysManager.this.mIsDpState) {
                    HwPCMultiDisplaysManager.this.mService.notifyDpState(true);
                } else if (!"OFF".equals(DP_STATE) || !HwPCMultiDisplaysManager.this.mIsDpState) {
                    HwPCUtils.log(HwPCMultiDisplaysManager.TAG, "onUEvent wrong state");
                } else {
                    HwPCMultiDisplaysManager.this.mService.notifyDpState(false);
                }
            }
        }
    };
    private Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mIsDpState = false;
    /* access modifiers changed from: private */
    public HwPCManagerService mService;

    public static class CastingDisplay {
        public int mDisplayId;
        public int mType;

        public CastingDisplay(int displayId, int type) {
            this.mDisplayId = displayId;
            this.mType = type;
        }
    }

    public HwPCMultiDisplaysManager(Context context, Handler handler, HwPCManagerService service) {
        this.mContext = context;
        this.mHandler = handler;
        this.mService = service;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        CastingDisplay firstCastDisplay = new CastingDisplay(-1, 0);
        CastingDisplay secondCastDisplay = new CastingDisplay(-1, 0);
        this.mCastingDisplays.add(firstCastDisplay);
        this.mCastingDisplays.add(secondCastDisplay);
        this.mDpObserver.startObserving(DP_STATE_DEVPATH);
        HwPCUtils.log(TAG, "HwPCMultiDisplaysManager, mIsDpState = " + this.mIsDpState);
    }

    public boolean isExternalDisplay(int displayId) {
        return (displayId == -1 || displayId == 0) ? false : true;
    }

    public int getDisplayType(int displayid) {
        Display display;
        if (displayid == -1 || displayid == 0) {
            return 0;
        }
        if (this.mDisplayManager == null) {
            this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        }
        DisplayManager displayManager = this.mDisplayManager;
        if (displayManager == null || (display = displayManager.getDisplay(displayid)) == null) {
            return 0;
        }
        return DisplayEx.getType(display);
    }

    public CastingDisplay get1stDisplay() {
        return this.mCastingDisplays.get(0);
    }

    public CastingDisplay get2ndDisplay() {
        return this.mCastingDisplays.get(1);
    }

    public void notifyDpState(boolean isDpState) {
        HwPCUtils.log(TAG, "notifyDpState isDpState = " + isDpState + ", mIsDpState = " + this.mIsDpState);
        if (this.mIsDpState != isDpState) {
            this.mIsDpState = isDpState;
            if (!isDpState) {
                if (this.mCastingDisplays.get(0).mType == 2) {
                    this.mCastingDisplays.get(0).mType = 0;
                }
                if (this.mCastingDisplays.get(1).mType == 2) {
                    this.mCastingDisplays.get(1).mType = 0;
                }
            }
        }
    }

    public void checkInitialDpConnectAfterBoot(int displayid) {
        HwPCUtils.log(TAG, "checkInitialDpConnectAfterBoot isInitialDpConnectAfterBoot = " + this.isInitialDpConnectAfterBoot);
        if (this.isInitialDpConnectAfterBoot && getDisplayType(displayid) == 2) {
            if (!this.mIsDpState) {
                this.mService.notifyDpState(true);
            }
            this.isInitialDpConnectAfterBoot = false;
        }
    }

    private boolean handleTwoDisplaysInHiCar(int displayId, int type) {
        boolean isHandleTwoDisplaysInHiCar = false;
        if (isExternalDisplay(this.mCastingDisplays.get(0).mDisplayId) && HwPCUtils.isHiCarCastMode()) {
            HwPCUtils.log(TAG, "handleTwoDisplaysInHiCar add PC display.");
            isHandleTwoDisplaysInHiCar = true;
        }
        if (isExternalDisplay(this.mCastingDisplays.get(0).mDisplayId) && HiCarManager.isConnToHiCar(this.mContext, displayId)) {
            HwPCUtils.log(TAG, "handleTwoDisplaysInHiCar add HiCar's display.");
            isHandleTwoDisplaysInHiCar = true;
        }
        if (isHandleTwoDisplaysInHiCar) {
            this.mCastingDisplays.get(1).mDisplayId = displayId;
            this.mCastingDisplays.get(1).mType = type;
        }
        return isHandleTwoDisplaysInHiCar;
    }

    public boolean handleTwoDisplaysInDisplayAdded(int displayId) {
        int type = getDisplayType(displayId);
        HwPCUtils.log(TAG, "handleTwoDisplaysInDisplayAdded type = " + type + " displayId " + displayId);
        if (displayId == this.mCastingDisplays.get(0).mDisplayId && type == this.mCastingDisplays.get(0).mType) {
            HwPCUtils.log(TAG, "handleTwoDisplaysInDisplayAdded add the same display as 1st display");
            return true;
        } else if (displayId == this.mCastingDisplays.get(1).mDisplayId && type == this.mCastingDisplays.get(1).mType) {
            HwPCUtils.log(TAG, "handleTwoDisplaysInDisplayAdded add the same display as 2nd display");
            return false;
        } else if (handleTwoDisplaysInHiCar(displayId, type)) {
            return false;
        } else {
            boolean isNeedContinueToHandleProjMode = false;
            if (type == 2) {
                if (isExternalDisplay(this.mCastingDisplays.get(0).mDisplayId)) {
                    if (isExternalDisplay(this.mCastingDisplays.get(1).mDisplayId)) {
                        HwPCUtils.log(TAG, "unkown error during HDMI display added.");
                    } else if (this.mCastingDisplays.get(0).mType == 3 || this.mCastingDisplays.get(0).mType == 5) {
                        this.mCastingDisplays.get(1).mDisplayId = displayId;
                        this.mCastingDisplays.get(1).mType = type;
                        HwPCUtils.log(TAG, "adding HDMI display when WIFI display added.");
                    } else {
                        HwPCUtils.log(TAG, "adding HDMI display when HDMI display added, is onDisplayRemoved call lost ?");
                        isNeedContinueToHandleProjMode = true;
                    }
                    if (!isNeedContinueToHandleProjMode) {
                        return false;
                    }
                }
            } else if (type == 3 || type == 5) {
                if (!isExternalDisplay(this.mCastingDisplays.get(0).mDisplayId) && this.mIsDpState && this.mCastingDisplays.get(0).mType == 2) {
                    if (!isExternalDisplay(this.mCastingDisplays.get(1).mDisplayId)) {
                        this.mCastingDisplays.get(1).mDisplayId = displayId;
                        this.mCastingDisplays.get(1).mType = type;
                        HwPCUtils.log(TAG, "adding WIFI display when HDMI display removed but DP still connected.");
                    } else {
                        HwPCUtils.log(TAG, "unkown error during WIFI display added.");
                    }
                    return false;
                } else if (!isExternalDisplay(this.mCastingDisplays.get(0).mDisplayId) || isExternalDisplay(this.mCastingDisplays.get(1).mDisplayId)) {
                    HwPCUtils.log(TAG, "there is no other case,do nothing");
                } else {
                    if (this.mCastingDisplays.get(0).mType == 2) {
                        this.mCastingDisplays.get(1).mDisplayId = displayId;
                        this.mCastingDisplays.get(1).mType = type;
                        HwPCUtils.log(TAG, "adding WIFI display when HDMI display added.");
                    } else if (!this.mIsDpState) {
                        HwPCUtils.log(TAG, "adding WIFI display when WIFI display added, is onDisplayRemoved call lost ?");
                        isNeedContinueToHandleProjMode = true;
                    } else {
                        HwPCUtils.log(TAG, "unkown error during adding WIFI display when WIFI display added.");
                    }
                    if (!isNeedContinueToHandleProjMode) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public boolean is4KHdmi1stDisplayRemoved(int displayId) {
        if (this.mIsDpState && displayId == this.mCastingDisplays.get(0).mDisplayId && this.mCastingDisplays.get(0).mType == 2) {
            return true;
        }
        return false;
    }

    public void handlelstDisplayInDisplayRemoved() {
        if (isExternalDisplay(this.mCastingDisplays.get(1).mDisplayId)) {
            HwPCUtils.log(TAG, "handlelstDisplayInDisplayRemoved SECOND_DISPLAY.mDisplayId = " + this.mCastingDisplays.get(1).mDisplayId);
            boolean z = false;
            if (HiCarManager.isConnToHiCar(this.mContext, this.mCastingDisplays.get(1).mDisplayId) || HwPCUtils.isHiCarCastMode()) {
                this.mCastingDisplays.get(0).mDisplayId = -1;
                this.mCastingDisplays.get(1).mDisplayId = -1;
                this.mCastingDisplays.get(0).mType = 0;
                this.mCastingDisplays.get(1).mType = 0;
                this.mHandler.removeMessages(6);
                this.mHandler.sendMessage(this.mHandler.obtainMessage(6, this.mCastingDisplays.get(1).mDisplayId, -1));
                return;
            }
            this.mCastingDisplays.get(0).mDisplayId = this.mCastingDisplays.get(1).mDisplayId;
            this.mCastingDisplays.get(1).mDisplayId = -1;
            this.mCastingDisplays.get(0).mType = this.mCastingDisplays.get(1).mType;
            this.mCastingDisplays.get(1).mType = 0;
            HwPCUtils.setPhoneDisplayID(this.mCastingDisplays.get(0).mDisplayId);
            Settings.Secure.putInt(this.mContext.getContentResolver(), "selected-proj-mode", 0);
            Settings.Global.putInt(this.mContext.getContentResolver(), "is_display_device_connected", 0);
            if (this.mCastingDisplays.get(0).mType == 3) {
                z = true;
            }
            HwPCUtils.setIsWifiMode(z);
            this.mService.mProjMode = HwPCUtils.ProjectionMode.PHONE_MODE;
            this.mHandler.removeMessages(1);
            Message msg = this.mHandler.obtainMessage(1);
            msg.obj = this.mService.mProjMode;
            this.mHandler.sendMessage(msg);
        }
    }
}
