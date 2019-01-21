package com.android.internal.telephony.cdma;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RegistrantListUtils;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.internal.telephony.AbstractGsmCdmaCallTracker.CdmaCallTrackerReference;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.GsmCdmaCallTracker;
import com.android.internal.telephony.GsmCdmaConnection;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaLineControlInfoRec;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HwCdmaCallTrackerReference implements CdmaCallTrackerReference {
    private static final int EVENT_CALL_LINE_CONTROL_INFO = 500;
    protected static final int EVENT_SWITCH_CALLBACKGROUND_STATE = 200;
    private static final String LOG_TAG = "HwCdmaCallTrackerReference";
    protected static final int SWITCH_VOICEBG_STATE_TIMER = 10;
    public static final boolean mIsLocalHangupSpeedUp = SystemProperties.get("ro.config.hw_hangup_speedup", "true").equals("true");
    private boolean hasRegisterForLineControlInfo = false;
    HwCdmaCallTrackerReferenceHandler hwCdmaCallTrackerReferenceHandler;
    RegistrantList lineControlInfoRegistrants = new RegistrantList();
    private GsmCdmaCallTracker mCdmaCallTracker;

    public class HwCdmaCallTrackerReferenceHandler extends Handler {
        public void handleMessage(Message msg) {
            if (msg.what == 500) {
                AsyncResult ar = msg.obj;
                if (ar.exception == null) {
                    HwCdmaCallTrackerReference.this.handleLineControlInfo((CdmaLineControlInfoRec) ar.result);
                    Rlog.i(HwCdmaCallTrackerReference.LOG_TAG, "EVENT_CALL_LINE_CONTROL_INFO Received");
                    return;
                }
                Rlog.i(HwCdmaCallTrackerReference.LOG_TAG, "EVENT_CALL_LINE_CONTROL_INFO Received but there is some exception!");
            }
        }
    }

    private static class SwitchVoiceBgStateHandler extends Handler {
        static final int EVENT_SWITCH_VOCIE_BG_STATE_DONE = 0;
        private AtomicBoolean mIsFinished = new AtomicBoolean(false);

        SwitchVoiceBgStateHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what != 0) {
                getLooper().quit();
                return;
            }
            Rlog.d(HwCdmaCallTrackerReference.LOG_TAG, "EVENT_SWITCH_VOCIE_BG_STATE_DONE");
            AsyncResult ar = msg.obj;
            Object result = ar.userObj;
            synchronized (result) {
                if (ar.exception != null) {
                    String str = HwCdmaCallTrackerReference.LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ar.exception");
                    stringBuilder.append(ar.exception);
                    Rlog.d(str, stringBuilder.toString());
                }
                result.notifyAll();
            }
            this.mIsFinished.set(true);
            getLooper().quit();
        }

        public boolean isFinished() {
            return this.mIsFinished.get();
        }
    }

    public HwCdmaCallTrackerReference(GsmCdmaCallTracker cdmaCallTracker) {
        this.mCdmaCallTracker = cdmaCallTracker;
        initHandler();
    }

    protected void initHandler() {
        if (this.hwCdmaCallTrackerReferenceHandler == null) {
            this.hwCdmaCallTrackerReferenceHandler = new HwCdmaCallTrackerReferenceHandler();
        }
    }

    public void dispose() {
        Rlog.i(LOG_TAG, "CdmaCallTracker unregisterForLineControlInfo!");
        this.mCdmaCallTracker.mCi.unregisterForLineControlInfo(this.hwCdmaCallTrackerReferenceHandler);
    }

    public void registerForLineControlInfo(Handler h, int what, Object obj) {
        if (!this.hasRegisterForLineControlInfo) {
            Rlog.i(LOG_TAG, "CdmaCallTracker registerForLineControlInfo!");
            this.mCdmaCallTracker.mCi.registerForLineControlInfo(this.hwCdmaCallTrackerReferenceHandler, 500, null);
            this.hasRegisterForLineControlInfo = true;
        }
        Rlog.i(LOG_TAG, "Some one call me  registerForLineControlInfo!");
        this.lineControlInfoRegistrants.add(new Registrant(h, what, obj));
    }

    public void unregisterForLineControlInfo(Handler h) {
        Rlog.i(LOG_TAG, "Some one call me  unregisterForLineControlInfo!");
        this.lineControlInfoRegistrants.remove(h);
    }

    private void notifyLineControlInfo() {
        if (this.lineControlInfoRegistrants != null) {
            Rlog.i(LOG_TAG, "CdmaCallTracker  notifyLineControlInfo!");
            this.lineControlInfoRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        }
    }

    private void handleLineControlInfo(CdmaLineControlInfoRec lineConRec) {
        if (lineConRec.lineCtrlPolarityIncluded == (byte) 1) {
            Rlog.i(LOG_TAG, "CdmaCallTracker  lineCtrlPolarityIncluded==1");
            GsmCdmaConnection fgConn = (GsmCdmaConnection) this.mCdmaCallTracker.mForegroundCall.getLatestConnection();
            if (fgConn != null) {
                Rlog.i(LOG_TAG, "CdmaCallTracker  there is foreground connection!");
                fgConn.onLineControlInfo();
                notifyLineControlInfo();
                return;
            }
            return;
        }
        Rlog.i(LOG_TAG, "CdmaCallTracker  lineCtrlPolarityIncluded!=1");
    }

    public boolean notifyRegistrantsDelayed() {
        if (!mIsLocalHangupSpeedUp) {
            return true;
        }
        RegistrantListUtils.notifyRegistrantsDelayed(this.mCdmaCallTracker.mVoiceCallEndedRegistrants, new AsyncResult(null, null, null), 500);
        return false;
    }

    public void switchVoiceCallBackgroundState(int state) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("switchVoiceCallBackgroundState:");
        stringBuilder.append(state);
        Rlog.d(str, stringBuilder.toString());
        HandlerThread ht = new HandlerThread("switchVoiceCallBackground");
        ht.start();
        SwitchVoiceBgStateHandler handler = new SwitchVoiceBgStateHandler(ht.getLooper());
        Object result = new Object();
        int cnt = 0;
        Message msg = handler.obtainMessage(0, result);
        synchronized (result) {
            this.mCdmaCallTracker.mCi.switchVoiceCallBackgroundState(state, msg);
            while (!handler.isFinished() && cnt < 3) {
                try {
                    cnt++;
                    result.wait(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setConnEncryptCallByNumber(String number, boolean val) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ringingCall  size=");
        stringBuilder.append(this.mCdmaCallTracker.mRingingCall.getConnections().size());
        Rlog.d(str, stringBuilder.toString());
        setConnsEncryptCall(this.mCdmaCallTracker.mRingingCall.getConnections(), number, val);
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("foregroundCall  size=");
        stringBuilder.append(this.mCdmaCallTracker.mForegroundCall.getConnections().size());
        Rlog.d(str, stringBuilder.toString());
        setConnsEncryptCall(this.mCdmaCallTracker.mForegroundCall.getConnections(), number, val);
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("backgroundCall size=");
        stringBuilder.append(this.mCdmaCallTracker.mBackgroundCall.getConnections().size());
        Rlog.d(str, stringBuilder.toString());
        setConnsEncryptCall(this.mCdmaCallTracker.mBackgroundCall.getConnections(), number, val);
    }

    private void setConnsEncryptCall(List<Connection> conns, String number, boolean val) {
        for (Connection conn : conns) {
            if (conn != null && ((GsmCdmaConnection) conn).compareToNumber(number)) {
                ((GsmCdmaConnection) conn).setEncryptCall(val);
            }
        }
    }
}
