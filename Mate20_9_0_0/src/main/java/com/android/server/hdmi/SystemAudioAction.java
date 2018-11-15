package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.net.util.NetworkConstants;
import android.os.RemoteException;
import android.util.Slog;
import java.util.List;

abstract class SystemAudioAction extends HdmiCecFeatureAction {
    private static final int MAX_SEND_RETRY_COUNT = 2;
    private static final int OFF_TIMEOUT_MS = 2000;
    private static final int ON_TIMEOUT_MS = 5000;
    private static final int STATE_CHECK_ROUTING_IN_PRGRESS = 1;
    private static final int STATE_WAIT_FOR_SET_SYSTEM_AUDIO_MODE = 2;
    private static final String TAG = "SystemAudioAction";
    protected final int mAvrLogicalAddress;
    private final IHdmiControlCallback mCallback;
    private int mSendRetryCount = 0;
    protected boolean mTargetAudioStatus;

    SystemAudioAction(HdmiCecLocalDevice source, int avrAddress, boolean targetStatus, IHdmiControlCallback callback) {
        super(source);
        HdmiUtils.verifyAddressType(avrAddress, 5);
        this.mAvrLogicalAddress = avrAddress;
        this.mTargetAudioStatus = targetStatus;
        this.mCallback = callback;
    }

    protected void sendSystemAudioModeRequest() {
        List<RoutingControlAction> routingActions = getActions(RoutingControlAction.class);
        if (routingActions.isEmpty()) {
            sendSystemAudioModeRequestInternal();
            return;
        }
        this.mState = 1;
        ((RoutingControlAction) routingActions.get(0)).addOnFinishedCallback(this, new Runnable() {
            public void run() {
                SystemAudioAction.this.sendSystemAudioModeRequestInternal();
            }
        });
    }

    private void sendSystemAudioModeRequestInternal() {
        sendCommand(HdmiCecMessageBuilder.buildSystemAudioModeRequest(getSourceAddress(), this.mAvrLogicalAddress, getSystemAudioModeRequestParam(), this.mTargetAudioStatus), new SendMessageCallback() {
            public void onSendCompleted(int error) {
                if (error != 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to send <System Audio Mode Request>:");
                    stringBuilder.append(error);
                    HdmiLogger.debug(stringBuilder.toString(), new Object[0]);
                    SystemAudioAction.this.setSystemAudioMode(false);
                    SystemAudioAction.this.finishWithCallback(7);
                }
            }
        });
        this.mState = 2;
        addTimer(this.mState, this.mTargetAudioStatus ? ON_TIMEOUT_MS : 2000);
    }

    private int getSystemAudioModeRequestParam() {
        if (tv().getActiveSource().isValid()) {
            return tv().getActiveSource().physicalAddress;
        }
        int i;
        int param = tv().getActivePath();
        if (param != NetworkConstants.ARP_HWTYPE_RESERVED_HI) {
            i = param;
        } else {
            i = 0;
        }
        return i;
    }

    private void handleSendSystemAudioModeRequestTimeout() {
        if (this.mTargetAudioStatus) {
            int i = this.mSendRetryCount;
            this.mSendRetryCount = i + 1;
            if (i < 2) {
                sendSystemAudioModeRequest();
                return;
            }
        }
        HdmiLogger.debug("[T]:wait for <Set System Audio Mode>.", new Object[0]);
        setSystemAudioMode(false);
        finishWithCallback(1);
    }

    protected void setSystemAudioMode(boolean mode) {
        tv().setSystemAudioMode(mode);
    }

    final boolean processCommand(HdmiCecMessage cmd) {
        if (cmd.getSource() != this.mAvrLogicalAddress || this.mState != 2) {
            return false;
        }
        if (cmd.getOpcode() == 0 && (cmd.getParams()[0] & 255) == 112) {
            HdmiLogger.debug("Failed to start system audio mode request.", new Object[0]);
            setSystemAudioMode(false);
            finishWithCallback(5);
            return true;
        } else if (cmd.getOpcode() != 114 || !HdmiUtils.checkCommandSource(cmd, this.mAvrLogicalAddress, TAG)) {
            return false;
        } else {
            boolean receivedStatus = HdmiUtils.parseCommandParamSystemAudioStatus(cmd);
            if (receivedStatus == this.mTargetAudioStatus) {
                setSystemAudioMode(receivedStatus);
                startAudioStatusAction();
                return true;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected system audio mode request:");
            stringBuilder.append(receivedStatus);
            HdmiLogger.debug(stringBuilder.toString(), new Object[0]);
            finishWithCallback(5);
            return false;
        }
    }

    protected void startAudioStatusAction() {
        addAndStartAction(new SystemAudioStatusAction(tv(), this.mAvrLogicalAddress, this.mCallback));
        finish();
    }

    protected void removeSystemAudioActionInProgress() {
        removeActionExcept(SystemAudioActionFromTv.class, this);
        removeActionExcept(SystemAudioActionFromAvr.class, this);
    }

    final void handleTimerEvent(int state) {
        if (this.mState == state && this.mState == 2) {
            handleSendSystemAudioModeRequestTimeout();
        }
    }

    protected void finishWithCallback(int returnCode) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onComplete(returnCode);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to invoke callback.", e);
            }
        }
        finish();
    }
}
