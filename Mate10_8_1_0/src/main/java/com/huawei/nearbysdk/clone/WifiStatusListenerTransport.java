package com.huawei.nearbysdk.clone;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.huawei.nearbysdk.HwLog;
import com.huawei.nearbysdk.IInternalConnectionListener.Stub;
import com.huawei.nearbysdk.NearbyConfig.BusinessTypeEnum;
import com.huawei.nearbysdk.NearbyConfiguration;
import com.huawei.nearbysdk.NearbyDevice;

public class WifiStatusListenerTransport extends Stub {
    private static final int QUIT_CALLBACK_TIMEOUT_MILLIS = 3500;
    private static final String TAG = "WifiStatusListenerTransport";
    private static final int TYPE_CONNECTION_CHANGE = 2;
    private static final int TYPE_STATE_CHANGE = 1;
    private int mBusinessId;
    private BusinessTypeEnum mBusinessType;
    private WifiStatusListener mListener;
    private final Handler mListenerHandler;
    private NearbyConfiguration mNearbyConfiguration;
    private NearbyDevice mNearbyDevice;

    WifiStatusListenerTransport(BusinessTypeEnum businessType, int businessId, NearbyConfiguration configuration, WifiStatusListener listener, Looper looper) {
        this(businessType, businessId, configuration, null, listener, looper);
    }

    WifiStatusListenerTransport(BusinessTypeEnum businessType, int businessId, NearbyDevice device, WifiStatusListener listener, Looper looper) {
        this(businessType, businessId, null, device, listener, looper);
    }

    private WifiStatusListenerTransport(BusinessTypeEnum businessType, int businessId, NearbyConfiguration configuration, NearbyDevice device, WifiStatusListener listener, Looper looper) {
        this.mListener = listener;
        this.mBusinessType = businessType;
        this.mBusinessId = businessId;
        this.mNearbyDevice = device;
        this.mNearbyConfiguration = configuration;
        this.mListenerHandler = new Handler(looper) {
            public void handleMessage(Message msg) {
                WifiStatusListenerTransport.this._handleMessage(msg);
            }
        };
    }

    synchronized void quit(boolean isWait) {
        this.mListener = null;
        if (isWait) {
            try {
                wait(3500);
            } catch (InterruptedException e) {
                HwLog.w(TAG, "CloneAdapter disableWifiAp ERROR" + e.getLocalizedMessage());
            }
        }
    }

    public BusinessTypeEnum getBusinessType() {
        return this.mBusinessType;
    }

    public int getBusinessId() {
        return this.mBusinessId;
    }

    public NearbyDevice getNearbyDevice() {
        return this.mNearbyDevice;
    }

    public synchronized void onStatusChange(int state) {
        HwLog.d(TAG, "onStatusChange state = " + state);
        if (this.mListener == null) {
            notifyAll();
        } else {
            sendMessage(1, null, state, 0);
        }
    }

    public void onConnectionChange(NearbyDevice device, int state) {
        HwLog.d(TAG, "onConnectionChange device=" + device + " state=" + state);
        sendMessage(2, device, state, 0);
    }

    public void onReceive(NearbyDevice device, byte[] recvMessage) {
        HwLog.e(TAG, "onReceive error here");
    }

    private void sendMessage(int msgWhat, Object obj, int state, long delayMillis) {
        Message msg = this.mListenerHandler.obtainMessage(msgWhat, obj);
        msg.arg1 = state;
        if (!this.mListenerHandler.sendMessageDelayed(msg, delayMillis)) {
            HwLog.e(TAG, "sendMessage error here");
        }
    }

    private void _handleMessage(Message msg) {
        NearbyDevice nearbyDevice = null;
        if (msg.obj instanceof NearbyDevice) {
            nearbyDevice = msg.obj;
        }
        int arg1 = msg.arg1;
        HwLog.d(TAG, "_handleMessage: " + msg.toString());
        switch (msg.what) {
            case 1:
                callBackOnStatusChange(arg1);
                return;
            case 2:
                callBackOnConnectionChange(nearbyDevice, arg1);
                return;
            default:
                HwLog.e(TAG, "_handleMessage: unknown message " + msg.what);
                return;
        }
    }

    private synchronized void callBackOnStatusChange(int status) {
        if (this.mListener == null) {
            HwLog.e(TAG, "callBackOnStatusChange: mListener null");
            return;
        }
        int state;
        if (status == -1) {
            state = -1;
        } else if (status == 0) {
            state = 0;
        } else if (status == 1) {
            state = 1;
        } else if (status == 2) {
            state = 3;
        } else {
            state = 2;
        }
        HwLog.i(TAG, String.format("callBackOnStatusChange: mListener=%s state=%d->%d", new Object[]{this.mListener, Integer.valueOf(status), Integer.valueOf(state)}));
        this.mListener.onStateChange(state);
    }

    private synchronized void callBackOnConnectionChange(NearbyDevice device, int status) {
        if (this.mListener == null) {
            HwLog.e(TAG, "callBackOnConnectionChange: mListener null");
            return;
        }
        int connectionState;
        if (status == 2) {
            connectionState = 0;
        } else if (status == 3) {
            connectionState = 1;
        } else if (status == 0) {
            connectionState = 3;
        } else {
            connectionState = 2;
        }
        int band = device.getWifiBand();
        String ipAddr = device.getRemoteIp();
        int port = device.getWifiPort();
        HwLog.i(TAG, String.format("callBackOnConnectionChange: mListener=%s state=%d->%d device=%s band=%d ipAddr=%s port=%d", new Object[]{this.mListener, Integer.valueOf(status), Integer.valueOf(connectionState), device, Integer.valueOf(band), String.valueOf(ipAddr), Integer.valueOf(port)}));
        this.mListener.onConnectionChange(connectionState, band, ipAddr, port);
    }
}
