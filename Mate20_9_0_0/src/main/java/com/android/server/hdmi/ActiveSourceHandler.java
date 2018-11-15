package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;

final class ActiveSourceHandler {
    private static final String TAG = "ActiveSourceHandler";
    private final IHdmiControlCallback mCallback;
    private final HdmiControlService mService = this.mSource.getService();
    private final HdmiCecLocalDeviceTv mSource;

    static ActiveSourceHandler create(HdmiCecLocalDeviceTv source, IHdmiControlCallback callback) {
        if (source != null) {
            return new ActiveSourceHandler(source, callback);
        }
        Slog.e(TAG, "Wrong arguments");
        return null;
    }

    private ActiveSourceHandler(HdmiCecLocalDeviceTv source, IHdmiControlCallback callback) {
        this.mSource = source;
        this.mCallback = callback;
    }

    void process(ActiveSource newActive, int deviceType) {
        HdmiCecLocalDeviceTv tv = this.mSource;
        if (this.mService.getDeviceInfo(newActive.logicalAddress) == null) {
            tv.startNewDeviceAction(newActive, deviceType);
        }
        boolean notifyInputChange = true;
        ActiveSource current;
        if (tv.isProhibitMode()) {
            current = tv.getActiveSource();
            if (current.logicalAddress == getSourceAddress()) {
                this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(current.logicalAddress, current.physicalAddress));
                tv.updateActiveSource(current);
                invokeCallback(0);
                return;
            }
            tv.startRoutingControl(newActive.physicalAddress, current.physicalAddress, true, this.mCallback);
            return;
        }
        current = ActiveSource.of(tv.getActiveSource());
        tv.updateActiveSource(newActive);
        if (this.mCallback != null) {
            notifyInputChange = false;
        }
        if (!current.equals(newActive)) {
            tv.setPrevPortId(tv.getActivePortId());
        }
        tv.updateActiveInput(newActive.physicalAddress, notifyInputChange);
        invokeCallback(0);
    }

    private final int getSourceAddress() {
        return this.mSource.getDeviceInfo().getLogicalAddress();
    }

    private void invokeCallback(int result) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onComplete(result);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Callback failed:");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        }
    }
}
