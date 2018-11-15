package com.android.server.rms.iaware.cpu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.rms.iaware.AwareLog;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;

public final class CPUGameFreq {
    private static final int MSG_SET_GAME_ENTER = 151;
    private static final int MSG_SET_GAME_LEVEL = 150;
    private static final String TAG = "AwareGameFreq";
    private TimerCleanHandler mTimerCleanHandler = new TimerCleanHandler();

    private static class TimerCleanHandler extends Handler {
        private TimerCleanHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 150:
                    if (msg.obj != null && (msg.obj instanceof Bundle)) {
                        IAwareMode.getInstance().gameLevel((Bundle) msg.obj);
                        break;
                    }
                    return;
                case 151:
                    if (msg.obj != null && (msg.obj instanceof Bundle)) {
                        boolean z = true;
                        if (msg.arg1 != 1) {
                            z = false;
                        }
                        IAwareMode.getInstance().gameEnter((Bundle) msg.obj, z);
                        break;
                    }
                    return;
                    break;
            }
        }
    }

    public int gameFreq(Bundle bundle, boolean isEasEnable) {
        if (bundle == null) {
            AwareLog.e(TAG, "empty bundle!");
            return -1;
        }
        int type = bundle.getInt(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE, -1);
        Message msg;
        if (type == 1) {
            msg = Message.obtain();
            msg.obj = bundle;
            msg.what = 151;
            msg.arg1 = isEasEnable;
            this.mTimerCleanHandler.sendMessage(msg);
        } else if (type == 3) {
            msg = Message.obtain();
            msg.obj = bundle;
            msg.what = 150;
            this.mTimerCleanHandler.sendMessage(msg);
        }
        return 0;
    }
}
