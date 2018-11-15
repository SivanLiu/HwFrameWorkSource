package com.huawei.nearbysdk;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.huawei.nearbysdk.ISubscribeListener.Stub;

public class SubscribeListenerTransport extends Stub {
    static final String TAG = "SubscribeListenerTransport";
    private static final int TYPE_STATUS_CHANGED = 1;
    private SubscribeListener mListener;
    private final Handler mListenerHandler;

    SubscribeListenerTransport(SubscribeListener listener, Looper looper) {
        this.mListener = listener;
        this.mListenerHandler = new Handler(looper) {
            public void handleMessage(Message msg) {
                SubscribeListenerTransport.this._handleMessage(msg);
            }
        };
    }

    public void onStatusChanged(int status) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onStatusChanged status = ");
        stringBuilder.append(status);
        HwLog.d(str, stringBuilder.toString());
        Message msg = Message.obtain();
        msg.what = 1;
        msg.arg1 = status;
        if (!this.mListenerHandler.sendMessage(msg)) {
            HwLog.e(TAG, "onStatusChanged: handler quitting,remove the listener. ");
        }
    }

    private void _handleMessage(Message msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("_handleMessage: ");
        stringBuilder.append(msg.toString());
        HwLog.d(str, stringBuilder.toString());
        if (msg.what != 1) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknow message id:");
            stringBuilder.append(msg.what);
            stringBuilder.append(", can not be here!");
            HwLog.e(str, stringBuilder.toString());
            return;
        }
        HwLog.d(TAG, "TYPE_STATUS_CHANGED");
        this.mListener.onStatusChanged(msg.arg1);
    }
}
