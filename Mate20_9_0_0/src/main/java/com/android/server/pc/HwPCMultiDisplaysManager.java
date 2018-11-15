package com.android.server.pc;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Message;
import android.os.UEventObserver;
import android.os.UEventObserver.UEvent;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.HwPCUtils;
import android.util.HwPCUtils.ProjectionMode;
import android.view.Display;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import java.util.ArrayList;

public final class HwPCMultiDisplaysManager {
    private static final String DP_STATE_DEVPATH = "DEVPATH=/devices/virtual/hw_typec/typec";
    public static final int FIRST_DISPLAY = 0;
    public static final int SECOND_DISPLAY = 1;
    private static final String TAG = "HwPCMultiDisplaysManager";
    private ArrayList<CastingDisplay> mCastingDisplays = new ArrayList();
    private Context mContext;
    private DisplayManager mDisplayManager;
    private final UEventObserver mDpObserver = new UEventObserver() {
        public void onUEvent(UEvent event) {
            String str = HwPCMultiDisplaysManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DP state event = ");
            stringBuilder.append(event);
            HwPCUtils.log(str, stringBuilder.toString());
            if (event != null) {
                str = event.get("DP_STATE");
                String str2 = HwPCMultiDisplaysManager.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DP state DP_STATE: ");
                stringBuilder2.append(str);
                stringBuilder2.append(", mDpState = ");
                stringBuilder2.append(HwPCMultiDisplaysManager.this.mDpState);
                HwPCUtils.log(str2, stringBuilder2.toString());
                if (AwareJobSchedulerConstants.BAR_STATUS_ON.equals(str) && !HwPCMultiDisplaysManager.this.mDpState) {
                    HwPCMultiDisplaysManager.this.mService.notifyDpState(true);
                } else if (AwareJobSchedulerConstants.BAR_STATUS_OFF.equals(str) && HwPCMultiDisplaysManager.this.mDpState) {
                    HwPCMultiDisplaysManager.this.mService.notifyDpState(false);
                } else {
                    HwPCUtils.log(HwPCMultiDisplaysManager.TAG, "onUEvent wrong state");
                }
            }
        }
    };
    private boolean mDpState = false;
    private Handler mHandler;
    private boolean mInitialDpConnectAfterBoot = true;
    private HwPCManagerService mService;

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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwPCMultiDisplaysManager, mDpState = ");
        stringBuilder.append(this.mDpState);
        HwPCUtils.log(str, stringBuilder.toString());
    }

    public boolean isExternalDisplay(int displayId) {
        return (displayId == -1 || displayId == 0) ? false : true;
    }

    public int getDisplayType(int displayid) {
        if (displayid == -1 || displayid == 0) {
            return 0;
        }
        if (this.mDisplayManager == null) {
            this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        }
        if (this.mDisplayManager != null) {
            Display display = this.mDisplayManager.getDisplay(displayid);
            if (display != null) {
                return display.getType();
            }
        }
        return 0;
    }

    public CastingDisplay get1stDisplay() {
        return (CastingDisplay) this.mCastingDisplays.get(0);
    }

    public CastingDisplay get2ndDisplay() {
        return (CastingDisplay) this.mCastingDisplays.get(1);
    }

    public void notifyDpState(boolean dpState) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyDpState dpState = ");
        stringBuilder.append(dpState);
        stringBuilder.append(", mDpState = ");
        stringBuilder.append(this.mDpState);
        HwPCUtils.log(str, stringBuilder.toString());
        if (this.mDpState != dpState) {
            this.mDpState = dpState;
            if (!dpState) {
                if (((CastingDisplay) this.mCastingDisplays.get(0)).mType == 2) {
                    ((CastingDisplay) this.mCastingDisplays.get(0)).mType = 0;
                }
                if (((CastingDisplay) this.mCastingDisplays.get(1)).mType == 2) {
                    ((CastingDisplay) this.mCastingDisplays.get(1)).mType = 0;
                }
            }
        }
    }

    public void checkInitialDpConnectAfterBoot(int displayid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkInitialDpConnectAfterBoot mInitialDpConnectAfterBoot = ");
        stringBuilder.append(this.mInitialDpConnectAfterBoot);
        HwPCUtils.log(str, stringBuilder.toString());
        if (this.mInitialDpConnectAfterBoot && getDisplayType(displayid) == 2) {
            if (!this.mDpState) {
                this.mService.notifyDpState(true);
            }
            this.mInitialDpConnectAfterBoot = false;
        }
    }

    public boolean handleTwoDisplaysInDisplayAdded(int displayId) {
        int type = getDisplayType(displayId);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleTwoDisplaysInDisplayAdded type = ");
        stringBuilder.append(type);
        HwPCUtils.log(str, stringBuilder.toString());
        if (displayId == ((CastingDisplay) this.mCastingDisplays.get(0)).mDisplayId && type == ((CastingDisplay) this.mCastingDisplays.get(0)).mType) {
            HwPCUtils.log(TAG, "handleTwoDisplaysInDisplayAdded add the same display as 1st display");
            return true;
        } else if (displayId == ((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId && type == ((CastingDisplay) this.mCastingDisplays.get(1)).mType) {
            HwPCUtils.log(TAG, "handleTwoDisplaysInDisplayAdded add the same display as 2nd display");
            return false;
        } else {
            boolean needContinueToHandleProjMode = false;
            if (type == 2) {
                if (isExternalDisplay(((CastingDisplay) this.mCastingDisplays.get(0)).mDisplayId)) {
                    if (isExternalDisplay(((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId)) {
                        HwPCUtils.log(TAG, "unkown error during HDMI display added.");
                    } else if (((CastingDisplay) this.mCastingDisplays.get(0)).mType == 3 || ((CastingDisplay) this.mCastingDisplays.get(0)).mType == 5) {
                        ((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId = displayId;
                        ((CastingDisplay) this.mCastingDisplays.get(1)).mType = type;
                        HwPCUtils.log(TAG, "adding HDMI display when WIFI display added.");
                    } else {
                        HwPCUtils.log(TAG, "adding HDMI display when HDMI display added, is onDisplayRemoved call lost ?");
                        needContinueToHandleProjMode = true;
                    }
                    if (!needContinueToHandleProjMode) {
                        return false;
                    }
                }
            } else if (type == 3 || type == 5) {
                if (!isExternalDisplay(((CastingDisplay) this.mCastingDisplays.get(0)).mDisplayId) && this.mDpState && ((CastingDisplay) this.mCastingDisplays.get(0)).mType == 2) {
                    if (isExternalDisplay(((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId)) {
                        HwPCUtils.log(TAG, "unkown error during WIFI display added.");
                    } else {
                        ((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId = displayId;
                        ((CastingDisplay) this.mCastingDisplays.get(1)).mType = type;
                        HwPCUtils.log(TAG, "adding WIFI display when HDMI display removed but DP still connected.");
                    }
                    return false;
                } else if (isExternalDisplay(((CastingDisplay) this.mCastingDisplays.get(0)).mDisplayId) && !isExternalDisplay(((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId)) {
                    if (((CastingDisplay) this.mCastingDisplays.get(0)).mType == 2) {
                        ((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId = displayId;
                        ((CastingDisplay) this.mCastingDisplays.get(1)).mType = type;
                        HwPCUtils.log(TAG, "adding WIFI display when HDMI display added.");
                    } else if (!this.mDpState) {
                        HwPCUtils.log(TAG, "adding WIFI display when WIFI display added, is onDisplayRemoved call lost ?");
                        needContinueToHandleProjMode = true;
                    }
                    if (!needContinueToHandleProjMode) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public boolean is4KHdmi1stDisplayRemoved(int displayId) {
        if (this.mDpState && displayId == ((CastingDisplay) this.mCastingDisplays.get(0)).mDisplayId && ((CastingDisplay) this.mCastingDisplays.get(0)).mType == 2) {
            return true;
        }
        return false;
    }

    public void handlelstDisplayInDisplayRemoved() {
        if (isExternalDisplay(((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handlelstDisplayInDisplayRemoved SECOND_DISPLAY.mDisplayId = ");
            stringBuilder.append(((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId);
            HwPCUtils.log(str, stringBuilder.toString());
            boolean z = false;
            ((CastingDisplay) this.mCastingDisplays.get(0)).mDisplayId = ((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId;
            ((CastingDisplay) this.mCastingDisplays.get(1)).mDisplayId = -1;
            ((CastingDisplay) this.mCastingDisplays.get(0)).mType = ((CastingDisplay) this.mCastingDisplays.get(1)).mType;
            ((CastingDisplay) this.mCastingDisplays.get(1)).mType = 0;
            HwPCUtils.setPhoneDisplayID(((CastingDisplay) this.mCastingDisplays.get(0)).mDisplayId);
            Secure.putInt(this.mContext.getContentResolver(), "selected-proj-mode", 0);
            Global.putInt(this.mContext.getContentResolver(), "is_display_device_connected", 0);
            if (((CastingDisplay) this.mCastingDisplays.get(0)).mType == 3) {
                z = true;
            }
            HwPCUtils.setIsWifiMode(z);
            this.mService.mProjMode = ProjectionMode.PHONE_MODE;
            this.mHandler.removeMessages(1);
            Message msg = this.mHandler.obtainMessage(1);
            msg.obj = this.mService.mProjMode;
            this.mHandler.sendMessage(msg);
        }
    }
}
