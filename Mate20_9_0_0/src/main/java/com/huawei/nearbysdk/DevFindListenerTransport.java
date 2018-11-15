package com.huawei.nearbysdk;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.huawei.nearbysdk.IDevFindListener.Stub;

public class DevFindListenerTransport extends Stub {
    static final String TAG = "ListenerTransport";
    private static final int TYPE_DEVICE_FOUND = 1;
    private static final int TYPE_DEVICE_LOST = 2;
    private DevFindListener mListener;
    private final Handler mListenerHandler;

    DevFindListenerTransport(DevFindListener listener, Looper looper) {
        this.mListener = listener;
        this.mListenerHandler = new Handler(looper) {
            public void handleMessage(Message msg) {
                DevFindListenerTransport.this._handleMessage(msg);
            }
        };
    }

    public void onDeviceFound(NearbyDevice device) {
        HwLog.d(TAG, "onDeviceFound");
        Message msg = Message.obtain();
        msg.what = 1;
        msg.obj = device;
        if (!this.mListenerHandler.sendMessage(msg)) {
            HwLog.e(TAG, "onDeviceFound: handler quitting,remove the listener. ");
        }
    }

    public void onDeviceLost(NearbyDevice device) {
        HwLog.d(TAG, "onDeviceLost");
        Message msg = Message.obtain();
        msg.what = 2;
        msg.obj = device;
        if (!this.mListenerHandler.sendMessage(msg)) {
            HwLog.e(TAG, "onDeviceLost: handler quitting,remove the listener. ");
        }
    }

    private void _handleMessage(Message msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("_handleMessage: ");
        stringBuilder.append(msg.toString());
        HwLog.d(str, stringBuilder.toString());
        switch (msg.what) {
            case 1:
                HwLog.d(TAG, "TYPE_STATUS_CHANGED Listener.onDeviceFound");
                this.mListener.onDeviceFound((NearbyDevice) msg.obj);
                return;
            case 2:
                HwLog.d(TAG, "TYPE_STATUS_CHANGED Listener.onDeviceLost");
                this.mListener.onDeviceLost((NearbyDevice) msg.obj);
                return;
            default:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknow message id:");
                stringBuilder.append(msg.what);
                stringBuilder.append(", can not be here!");
                HwLog.e(str, stringBuilder.toString());
                return;
        }
    }
}
