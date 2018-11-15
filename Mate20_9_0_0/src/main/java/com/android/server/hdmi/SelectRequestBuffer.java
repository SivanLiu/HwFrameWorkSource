package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;

public class SelectRequestBuffer {
    public static final SelectRequestBuffer EMPTY_BUFFER = new SelectRequestBuffer() {
        public void process() {
        }
    };
    private static final String TAG = "SelectRequestBuffer";
    private SelectRequest mRequest;

    public static abstract class SelectRequest {
        protected final IHdmiControlCallback mCallback;
        protected final int mId;
        protected final HdmiControlService mService;

        public abstract void process();

        public SelectRequest(HdmiControlService service, int id, IHdmiControlCallback callback) {
            this.mService = service;
            this.mId = id;
            this.mCallback = callback;
        }

        protected HdmiCecLocalDeviceTv tv() {
            return this.mService.tv();
        }

        protected boolean isLocalDeviceReady() {
            if (tv() != null) {
                return true;
            }
            Slog.e(SelectRequestBuffer.TAG, "Local tv device not available");
            invokeCallback(2);
            return false;
        }

        private void invokeCallback(int reason) {
            try {
                if (this.mCallback != null) {
                    this.mCallback.onComplete(reason);
                }
            } catch (RemoteException e) {
                String str = SelectRequestBuffer.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invoking callback failed:");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        }
    }

    public static class DeviceSelectRequest extends SelectRequest {
        /* synthetic */ DeviceSelectRequest(HdmiControlService x0, int x1, IHdmiControlCallback x2, AnonymousClass1 x3) {
            this(x0, x1, x2);
        }

        private DeviceSelectRequest(HdmiControlService srv, int id, IHdmiControlCallback callback) {
            super(srv, id, callback);
        }

        public void process() {
            if (isLocalDeviceReady()) {
                String str = SelectRequestBuffer.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("calling delayed deviceSelect id:");
                stringBuilder.append(this.mId);
                Slog.v(str, stringBuilder.toString());
                tv().deviceSelect(this.mId, this.mCallback);
            }
        }
    }

    public static class PortSelectRequest extends SelectRequest {
        /* synthetic */ PortSelectRequest(HdmiControlService x0, int x1, IHdmiControlCallback x2, AnonymousClass1 x3) {
            this(x0, x1, x2);
        }

        private PortSelectRequest(HdmiControlService srv, int id, IHdmiControlCallback callback) {
            super(srv, id, callback);
        }

        public void process() {
            if (isLocalDeviceReady()) {
                String str = SelectRequestBuffer.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("calling delayed portSelect id:");
                stringBuilder.append(this.mId);
                Slog.v(str, stringBuilder.toString());
                tv().doManualPortSwitching(this.mId, this.mCallback);
            }
        }
    }

    public static DeviceSelectRequest newDeviceSelect(HdmiControlService srv, int id, IHdmiControlCallback callback) {
        return new DeviceSelectRequest(srv, id, callback, null);
    }

    public static PortSelectRequest newPortSelect(HdmiControlService srv, int id, IHdmiControlCallback callback) {
        return new PortSelectRequest(srv, id, callback, null);
    }

    public void set(SelectRequest request) {
        this.mRequest = request;
    }

    public void process() {
        if (this.mRequest != null) {
            this.mRequest.process();
            clear();
        }
    }

    public void clear() {
        this.mRequest = null;
    }
}
