package com.android.server.sip;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.sip.ISipService.Stub;
import android.net.sip.ISipSession;
import android.net.sip.ISipSessionListener;
import android.net.sip.SipErrorCode;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipProfile.Builder;
import android.net.sip.SipSessionAdapter;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.telephony.Rlog;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import javax.sip.SipException;

public final class SipService extends Stub {
    static final boolean DBG = true;
    private static final int DEFAULT_KEEPALIVE_INTERVAL = 10;
    private static final int DEFAULT_MAX_KEEPALIVE_INTERVAL = 120;
    private static final int EXPIRY_TIME = 3600;
    private static final int MIN_EXPIRY_TIME = 60;
    private static final int SHORT_EXPIRY_TIME = 10;
    static final String TAG = "SipService";
    private final AppOpsManager mAppOps;
    private ConnectivityReceiver mConnectivityReceiver;
    private Context mContext;
    private MyExecutor mExecutor = new MyExecutor();
    private int mKeepAliveInterval;
    private int mLastGoodKeepAliveInterval = 10;
    private String mLocalIp;
    private SipWakeLock mMyWakeLock;
    private int mNetworkType = -1;
    private Map<String, ISipSession> mPendingSessions = new HashMap();
    private Map<String, SipSessionGroupExt> mSipGroups = new HashMap();
    private SipKeepAliveProcessCallback mSipKeepAliveProcessCallback;
    private boolean mSipOnWifiOnly;
    private SipWakeupTimer mTimer;
    private WifiLock mWifiLock;

    private class ConnectivityReceiver extends BroadcastReceiver {
        private ConnectivityReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                final NetworkInfo info = (NetworkInfo) bundle.get("networkInfo");
                SipService.this.mExecutor.execute(new Runnable() {
                    public void run() {
                        SipService.this.onConnectivityChanged(info);
                    }
                });
            }
        }
    }

    private class MyExecutor extends Handler implements Executor {
        MyExecutor() {
            super(SipService.createLooper());
        }

        public void execute(Runnable task) {
            SipService.this.mMyWakeLock.acquire((Object) task);
            Message.obtain(this, 0, task).sendToTarget();
        }

        public void handleMessage(Message msg) {
            if (msg.obj instanceof Runnable) {
                executeInternal((Runnable) msg.obj);
                return;
            }
            SipService sipService = SipService.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage: not Runnable ignore msg=");
            stringBuilder.append(msg);
            sipService.log(stringBuilder.toString());
        }

        private void executeInternal(Runnable task) {
            try {
                task.run();
            } catch (Throwable th) {
                SipService.this.mMyWakeLock.release(task);
            }
            SipService.this.mMyWakeLock.release(task);
        }
    }

    private class SipKeepAliveProcessCallback implements Runnable, KeepAliveProcessCallback {
        private static final int MIN_INTERVAL = 5;
        private static final int NAT_MEASUREMENT_RETRY_INTERVAL = 120;
        private static final int PASS_THRESHOLD = 10;
        private static final boolean SKAI_DBG = true;
        private static final String SKAI_TAG = "SipKeepAliveProcessCallback";
        private SipSessionGroupExt mGroup;
        private int mInterval;
        private SipProfile mLocalProfile;
        private int mMaxInterval;
        private int mMinInterval;
        private int mPassCount;
        private SipSessionImpl mSession;

        public SipKeepAliveProcessCallback(SipProfile localProfile, int minInterval, int maxInterval) {
            this.mMaxInterval = maxInterval;
            this.mMinInterval = minInterval;
            this.mLocalProfile = localProfile;
        }

        public void start() {
            synchronized (SipService.this) {
                if (this.mSession != null) {
                    return;
                }
                this.mInterval = (this.mMaxInterval + this.mMinInterval) / 2;
                this.mPassCount = 0;
                StringBuilder stringBuilder;
                if (this.mInterval < 10 || checkTermination()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("start: measurement aborted; interval=[");
                    stringBuilder.append(this.mMinInterval);
                    stringBuilder.append(",");
                    stringBuilder.append(this.mMaxInterval);
                    stringBuilder.append("]");
                    log(stringBuilder.toString());
                    return;
                }
                try {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("start: interval=");
                    stringBuilder.append(this.mInterval);
                    log(stringBuilder.toString());
                    this.mGroup = new SipSessionGroupExt(this.mLocalProfile, null, null);
                    this.mGroup.setWakeupTimer(new SipWakeupTimer(SipService.this.mContext, SipService.this.mExecutor));
                    this.mSession = (SipSessionImpl) this.mGroup.createSession(null);
                    this.mSession.startKeepAliveProcess(this.mInterval, this);
                } catch (Throwable t) {
                    onError(-4, t.toString());
                }
            }
        }

        public void stop() {
            synchronized (SipService.this) {
                if (this.mSession != null) {
                    this.mSession.stopKeepAliveProcess();
                    this.mSession = null;
                }
                if (this.mGroup != null) {
                    this.mGroup.close();
                    this.mGroup = null;
                }
                SipService.this.mTimer.cancel(this);
                log("stop");
            }
        }

        private void restart() {
            synchronized (SipService.this) {
                if (this.mSession == null) {
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("restart: interval=");
                stringBuilder.append(this.mInterval);
                log(stringBuilder.toString());
                try {
                    this.mSession.stopKeepAliveProcess();
                    this.mPassCount = 0;
                    this.mSession.startKeepAliveProcess(this.mInterval, this);
                } catch (SipException e) {
                    loge("restart", e);
                }
            }
        }

        private boolean checkTermination() {
            return this.mMaxInterval - this.mMinInterval < 5 ? SKAI_DBG : false;
        }

        /* JADX WARNING: Missing block: B:20:0x00b7, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onResponse(boolean portChanged) {
            synchronized (SipService.this) {
                StringBuilder stringBuilder;
                if (portChanged) {
                    this.mMaxInterval = this.mInterval;
                } else {
                    try {
                        int i = this.mPassCount + 1;
                        this.mPassCount = i;
                        if (i != 10) {
                            return;
                        }
                        if (SipService.this.mKeepAliveInterval > 0) {
                            SipService.this.mLastGoodKeepAliveInterval = SipService.this.mKeepAliveInterval;
                        }
                        SipService sipService = SipService.this;
                        int i2 = this.mInterval;
                        this.mMinInterval = i2;
                        sipService.mKeepAliveInterval = i2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("onResponse: portChanged=");
                        stringBuilder.append(portChanged);
                        stringBuilder.append(" mKeepAliveInterval=");
                        stringBuilder.append(SipService.this.mKeepAliveInterval);
                        log(stringBuilder.toString());
                        SipService.this.onKeepAliveIntervalChanged();
                    } catch (Throwable th) {
                    }
                }
                if (checkTermination()) {
                    stop();
                    SipService.this.mKeepAliveInterval = this.mMinInterval;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onResponse: checkTermination mKeepAliveInterval=");
                    stringBuilder.append(SipService.this.mKeepAliveInterval);
                    log(stringBuilder.toString());
                } else {
                    this.mInterval = (this.mMaxInterval + this.mMinInterval) / 2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onResponse: mKeepAliveInterval=");
                    stringBuilder.append(SipService.this.mKeepAliveInterval);
                    stringBuilder.append(", new mInterval=");
                    stringBuilder.append(this.mInterval);
                    log(stringBuilder.toString());
                    restart();
                }
            }
        }

        public void onError(int errorCode, String description) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onError: errorCode=");
            stringBuilder.append(errorCode);
            stringBuilder.append(" desc=");
            stringBuilder.append(description);
            loge(stringBuilder.toString());
            restartLater();
        }

        public void run() {
            SipService.this.mTimer.cancel(this);
            restart();
        }

        private void restartLater() {
            synchronized (SipService.this) {
                SipService.this.mTimer.cancel(this);
                SipService.this.mTimer.set(NAT_MEASUREMENT_RETRY_INTERVAL * 1000, this);
            }
        }

        private void log(String s) {
            Rlog.d(SKAI_TAG, s);
        }

        private void loge(String s) {
            Rlog.d(SKAI_TAG, s);
        }

        private void loge(String s, Throwable t) {
            Rlog.d(SKAI_TAG, s, t);
        }
    }

    private class SipAutoReg extends SipSessionAdapter implements Runnable, KeepAliveProcessCallback {
        private static final int MIN_KEEPALIVE_SUCCESS_COUNT = 10;
        private static final boolean SAR_DBG = true;
        private String SAR_TAG;
        private int mBackoff;
        private int mErrorCode;
        private String mErrorMessage;
        private long mExpiryTime;
        private SipSessionImpl mKeepAliveSession;
        private int mKeepAliveSuccessCount;
        private SipSessionListenerProxy mProxy;
        private boolean mRegistered;
        private boolean mRunning;
        private SipSessionImpl mSession;

        private SipAutoReg() {
            this.mProxy = new SipSessionListenerProxy();
            this.mBackoff = 1;
            this.mRunning = false;
            this.mKeepAliveSuccessCount = 0;
        }

        public void start(SipSessionGroup group) {
            if (!this.mRunning) {
                this.mRunning = SAR_DBG;
                this.mBackoff = 1;
                this.mSession = (SipSessionImpl) group.createSession(this);
                if (this.mSession != null) {
                    SipService.this.mMyWakeLock.acquire(this.mSession);
                    this.mSession.unregister();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("SipAutoReg:");
                    stringBuilder.append(SipService.obfuscateSipUri(this.mSession.getLocalProfile().getUriString()));
                    this.SAR_TAG = stringBuilder.toString();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("start: group=");
                    stringBuilder.append(group);
                    log(stringBuilder.toString());
                }
            }
        }

        private void startKeepAliveProcess(int interval) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startKeepAliveProcess: interval=");
            stringBuilder.append(interval);
            log(stringBuilder.toString());
            if (this.mKeepAliveSession == null) {
                this.mKeepAliveSession = this.mSession.duplicate();
            } else {
                this.mKeepAliveSession.stopKeepAliveProcess();
            }
            try {
                this.mKeepAliveSession.startKeepAliveProcess(interval, this);
            } catch (SipException e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("startKeepAliveProcess: interval=");
                stringBuilder2.append(interval);
                loge(stringBuilder2.toString(), e);
            }
        }

        private void stopKeepAliveProcess() {
            if (this.mKeepAliveSession != null) {
                this.mKeepAliveSession.stopKeepAliveProcess();
                this.mKeepAliveSession = null;
            }
            this.mKeepAliveSuccessCount = 0;
        }

        /* JADX WARNING: Missing block: B:22:0x0097, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onResponse(boolean portChanged) {
            synchronized (SipService.this) {
                if (portChanged) {
                    try {
                        int interval = SipService.this.getKeepAliveInterval();
                        StringBuilder stringBuilder;
                        if (this.mKeepAliveSuccessCount < 10) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onResponse: keepalive doesn't work with interval ");
                            stringBuilder.append(interval);
                            stringBuilder.append(", past success count=");
                            stringBuilder.append(this.mKeepAliveSuccessCount);
                            log(stringBuilder.toString());
                            if (interval > 10) {
                                SipService.this.restartPortMappingLifetimeMeasurement(this.mSession.getLocalProfile(), interval);
                                this.mKeepAliveSuccessCount = 0;
                            }
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("keep keepalive going with interval ");
                            stringBuilder.append(interval);
                            stringBuilder.append(", past success count=");
                            stringBuilder.append(this.mKeepAliveSuccessCount);
                            log(stringBuilder.toString());
                            this.mKeepAliveSuccessCount /= 2;
                        }
                    } finally {
                    }
                } else {
                    SipService.this.startPortMappingLifetimeMeasurement(this.mSession.getLocalProfile());
                    this.mKeepAliveSuccessCount++;
                }
                if (this.mRunning) {
                    if (portChanged) {
                        this.mKeepAliveSession = null;
                        SipService.this.mMyWakeLock.acquire(this.mSession);
                        this.mSession.register(SipService.EXPIRY_TIME);
                    }
                }
            }
        }

        public void onError(int errorCode, String description) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onError: errorCode=");
            stringBuilder.append(errorCode);
            stringBuilder.append(" desc=");
            stringBuilder.append(description);
            loge(stringBuilder.toString());
            onResponse(SAR_DBG);
        }

        public void stop() {
            if (this.mRunning) {
                this.mRunning = false;
                SipService.this.mMyWakeLock.release(this.mSession);
                if (this.mSession != null) {
                    this.mSession.setListener(null);
                    if (SipService.this.mNetworkType != -1 && this.mRegistered) {
                        this.mSession.unregister();
                    }
                }
                SipService.this.mTimer.cancel(this);
                stopKeepAliveProcess();
                this.mRegistered = false;
                setListener(this.mProxy.getListener());
            }
        }

        public void onKeepAliveIntervalChanged() {
            if (this.mKeepAliveSession != null) {
                int newInterval = SipService.this.getKeepAliveInterval();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onKeepAliveIntervalChanged: interval=");
                stringBuilder.append(newInterval);
                log(stringBuilder.toString());
                this.mKeepAliveSuccessCount = 0;
                startKeepAliveProcess(newInterval);
            }
        }

        public void setListener(ISipSessionListener listener) {
            synchronized (SipService.this) {
                this.mProxy.setListener(listener);
                try {
                    int state;
                    if (this.mSession == null) {
                        state = 0;
                    } else {
                        state = this.mSession.getState();
                    }
                    if (state != 1) {
                        if (state != 2) {
                            if (this.mRegistered) {
                                this.mProxy.onRegistrationDone(this.mSession, (int) (this.mExpiryTime - SystemClock.elapsedRealtime()));
                            } else if (this.mErrorCode != 0) {
                                if (this.mErrorCode == -5) {
                                    this.mProxy.onRegistrationTimeout(this.mSession);
                                } else {
                                    this.mProxy.onRegistrationFailed(this.mSession, this.mErrorCode, this.mErrorMessage);
                                }
                            } else if (SipService.this.mNetworkType == -1) {
                                this.mProxy.onRegistrationFailed(this.mSession, -10, "no data connection");
                            } else if (this.mRunning) {
                                this.mProxy.onRegistrationFailed(this.mSession, -9, String.valueOf(state));
                            } else {
                                this.mProxy.onRegistrationFailed(this.mSession, -4, "registration not running");
                            }
                        }
                    }
                    this.mProxy.onRegistering(this.mSession);
                } catch (Throwable t) {
                    loge("setListener: ", t);
                }
            }
        }

        public boolean isRegistered() {
            return this.mRegistered;
        }

        /* JADX WARNING: Missing block: B:11:0x0030, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (SipService.this) {
                if (this.mRunning) {
                    this.mErrorCode = 0;
                    this.mErrorMessage = null;
                    log("run: registering");
                    if (SipService.this.mNetworkType != -1) {
                        SipService.this.mMyWakeLock.acquire(this.mSession);
                        this.mSession.register(SipService.EXPIRY_TIME);
                    }
                }
            }
        }

        private void restart(int duration) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("restart: duration=");
            stringBuilder.append(duration);
            stringBuilder.append("s later.");
            log(stringBuilder.toString());
            SipService.this.mTimer.cancel(this);
            SipService.this.mTimer.set(duration * 1000, this);
        }

        private int backoffDuration() {
            int duration = 10 * this.mBackoff;
            if (duration > SipService.EXPIRY_TIME) {
                return SipService.EXPIRY_TIME;
            }
            this.mBackoff *= 2;
            return duration;
        }

        public void onRegistering(ISipSession session) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRegistering: ");
            stringBuilder.append(session);
            log(stringBuilder.toString());
            synchronized (SipService.this) {
                if (notCurrentSession(session)) {
                    return;
                }
                this.mRegistered = false;
                this.mProxy.onRegistering(session);
            }
        }

        private boolean notCurrentSession(ISipSession session) {
            if (session == this.mSession) {
                return this.mRunning ^ 1;
            }
            ((SipSessionImpl) session).setListener(null);
            SipService.this.mMyWakeLock.release(session);
            return SAR_DBG;
        }

        /* JADX WARNING: Missing block: B:24:0x0083, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onRegistrationDone(ISipSession session, int duration) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRegistrationDone: ");
            stringBuilder.append(session);
            log(stringBuilder.toString());
            synchronized (SipService.this) {
                if (notCurrentSession(session)) {
                    return;
                }
                this.mProxy.onRegistrationDone(session, duration);
                if (duration > 0) {
                    this.mExpiryTime = SystemClock.elapsedRealtime() + ((long) (duration * 1000));
                    if (!this.mRegistered) {
                        this.mRegistered = SAR_DBG;
                        duration -= 60;
                        if (duration < SipService.MIN_EXPIRY_TIME) {
                            duration = SipService.MIN_EXPIRY_TIME;
                        }
                        restart(duration);
                        SipProfile localProfile = this.mSession.getLocalProfile();
                        if (this.mKeepAliveSession == null && (SipService.this.isBehindNAT(SipService.this.mLocalIp) || localProfile.getSendKeepAlive())) {
                            startKeepAliveProcess(SipService.this.getKeepAliveInterval());
                        }
                    }
                    SipService.this.mMyWakeLock.release(session);
                } else {
                    this.mRegistered = false;
                    this.mExpiryTime = -1;
                    log("Refresh registration immediately");
                    run();
                }
            }
        }

        public void onRegistrationFailed(ISipSession session, int errorCode, String message) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRegistrationFailed: ");
            stringBuilder.append(session);
            stringBuilder.append(": ");
            stringBuilder.append(SipErrorCode.toString(errorCode));
            stringBuilder.append(": ");
            stringBuilder.append(message);
            log(stringBuilder.toString());
            synchronized (SipService.this) {
                if (notCurrentSession(session)) {
                    return;
                }
                if (errorCode == -12 || errorCode == -8) {
                    log("   pause auto-registration");
                    stop();
                } else {
                    restartLater();
                }
                this.mErrorCode = errorCode;
                this.mErrorMessage = message;
                this.mProxy.onRegistrationFailed(session, errorCode, message);
                SipService.this.mMyWakeLock.release(session);
            }
        }

        public void onRegistrationTimeout(ISipSession session) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRegistrationTimeout: ");
            stringBuilder.append(session);
            log(stringBuilder.toString());
            synchronized (SipService.this) {
                if (notCurrentSession(session)) {
                    return;
                }
                this.mErrorCode = -5;
                this.mProxy.onRegistrationTimeout(session);
                restartLater();
                SipService.this.mMyWakeLock.release(session);
            }
        }

        private void restartLater() {
            loge("restartLater");
            this.mRegistered = false;
            restart(backoffDuration());
        }

        private void log(String s) {
            Rlog.d(this.SAR_TAG, s);
        }

        private void loge(String s) {
            Rlog.e(this.SAR_TAG, s);
        }

        private void loge(String s, Throwable e) {
            Rlog.e(this.SAR_TAG, s, e);
        }
    }

    private class SipSessionGroupExt extends SipSessionAdapter {
        private static final boolean SSGE_DBG = true;
        private static final String SSGE_TAG = "SipSessionGroupExt";
        private SipAutoReg mAutoRegistration = new SipAutoReg();
        private PendingIntent mIncomingCallPendingIntent;
        private boolean mOpenedToReceiveCalls;
        private SipSessionGroup mSipGroup;

        public SipSessionGroupExt(SipProfile localProfile, PendingIntent incomingCallPendingIntent, ISipSessionListener listener) throws SipException {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SipSessionGroupExt: profile=");
            stringBuilder.append(localProfile);
            log(stringBuilder.toString());
            this.mSipGroup = new SipSessionGroup(duplicate(localProfile), localProfile.getPassword(), SipService.this.mTimer, SipService.this.mMyWakeLock);
            this.mIncomingCallPendingIntent = incomingCallPendingIntent;
            this.mAutoRegistration.setListener(listener);
        }

        public SipProfile getLocalProfile() {
            return this.mSipGroup.getLocalProfile();
        }

        public boolean containsSession(String callId) {
            return this.mSipGroup.containsSession(callId);
        }

        public void onKeepAliveIntervalChanged() {
            this.mAutoRegistration.onKeepAliveIntervalChanged();
        }

        void setWakeupTimer(SipWakeupTimer timer) {
            this.mSipGroup.setWakeupTimer(timer);
        }

        private SipProfile duplicate(SipProfile p) {
            try {
                return new Builder(p).setPassword("*").build();
            } catch (Exception e) {
                loge("duplicate()", e);
                throw new RuntimeException("duplicate profile", e);
            }
        }

        public void setListener(ISipSessionListener listener) {
            this.mAutoRegistration.setListener(listener);
        }

        public void setIncomingCallPendingIntent(PendingIntent pIntent) {
            this.mIncomingCallPendingIntent = pIntent;
        }

        public void openToReceiveCalls() {
            this.mOpenedToReceiveCalls = SSGE_DBG;
            if (SipService.this.mNetworkType != -1) {
                this.mSipGroup.openToReceiveCalls(this);
                this.mAutoRegistration.start(this.mSipGroup);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("openToReceiveCalls: ");
            stringBuilder.append(SipService.obfuscateSipUri(getUri()));
            stringBuilder.append(": ");
            stringBuilder.append(this.mIncomingCallPendingIntent);
            log(stringBuilder.toString());
        }

        public void onConnectivityChanged(boolean connected) throws SipException {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onConnectivityChanged: connected=");
            stringBuilder.append(connected);
            stringBuilder.append(" uri=");
            stringBuilder.append(SipService.obfuscateSipUri(getUri()));
            stringBuilder.append(": ");
            stringBuilder.append(this.mIncomingCallPendingIntent);
            log(stringBuilder.toString());
            this.mSipGroup.onConnectivityChanged();
            if (connected) {
                this.mSipGroup.reset();
                if (this.mOpenedToReceiveCalls) {
                    openToReceiveCalls();
                    return;
                }
                return;
            }
            this.mSipGroup.close();
            this.mAutoRegistration.stop();
        }

        public void close() {
            this.mOpenedToReceiveCalls = false;
            this.mSipGroup.close();
            this.mAutoRegistration.stop();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("close: ");
            stringBuilder.append(SipService.obfuscateSipUri(getUri()));
            stringBuilder.append(": ");
            stringBuilder.append(this.mIncomingCallPendingIntent);
            log(stringBuilder.toString());
        }

        public ISipSession createSession(ISipSessionListener listener) {
            log("createSession");
            return this.mSipGroup.createSession(listener);
        }

        public void onRinging(ISipSession s, SipProfile caller, String sessionDescription) {
            SipSessionImpl session = (SipSessionImpl) s;
            synchronized (SipService.this) {
                try {
                    if (isRegistered()) {
                        if (!SipService.this.callingSelf(this, session)) {
                            SipService.this.addPendingSession(session);
                            Intent intent = SipManager.createIncomingCallBroadcast(session.getCallId(), sessionDescription);
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("onRinging: uri=");
                            stringBuilder.append(getUri());
                            stringBuilder.append(": ");
                            stringBuilder.append(caller.getUri());
                            stringBuilder.append(": ");
                            stringBuilder.append(session.getCallId());
                            stringBuilder.append(" ");
                            stringBuilder.append(this.mIncomingCallPendingIntent);
                            log(stringBuilder.toString());
                            this.mIncomingCallPendingIntent.send(SipService.this.mContext, 101, intent);
                        }
                    }
                    log("onRinging: end notReg or self");
                    session.endCall();
                    return;
                } catch (CanceledException e) {
                    loge("onRinging: pendingIntent is canceled, drop incoming call", e);
                    session.endCall();
                } catch (Throwable th) {
                }
            }
        }

        public void onError(ISipSession session, int errorCode, String message) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onError: errorCode=");
            stringBuilder.append(errorCode);
            stringBuilder.append(" desc=");
            stringBuilder.append(SipErrorCode.toString(errorCode));
            stringBuilder.append(": ");
            stringBuilder.append(message);
            log(stringBuilder.toString());
        }

        public boolean isOpenedToReceiveCalls() {
            return this.mOpenedToReceiveCalls;
        }

        public boolean isRegistered() {
            return this.mAutoRegistration.isRegistered();
        }

        private String getUri() {
            return this.mSipGroup.getLocalProfileUri();
        }

        private void log(String s) {
            Rlog.d(SSGE_TAG, s);
        }

        private void loge(String s, Throwable t) {
            Rlog.e(SSGE_TAG, s, t);
        }
    }

    public static void start(Context context) {
        if (SipManager.isApiSupported(context) && ServiceManager.getService("sip") == null) {
            ServiceManager.addService("sip", new SipService(context));
            context.sendBroadcast(new Intent(SipManager.ACTION_SIP_SERVICE_UP));
            slog("start:");
        }
    }

    private SipService(Context context) {
        log("SipService: started!");
        this.mContext = context;
        this.mConnectivityReceiver = new ConnectivityReceiver();
        this.mWifiLock = ((WifiManager) context.getSystemService("wifi")).createWifiLock(1, TAG);
        this.mWifiLock.setReferenceCounted(false);
        this.mSipOnWifiOnly = SipManager.isSipWifiOnly(context);
        this.mMyWakeLock = new SipWakeLock((PowerManager) context.getSystemService("power"));
        this.mTimer = new SipWakeupTimer(context, this.mExecutor);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
    }

    public synchronized SipProfile[] getListOfProfiles(String opPackageName) {
        if (canUseSip(opPackageName, "getListOfProfiles")) {
            boolean isCallerRadio = isCallerRadio();
            ArrayList<SipProfile> profiles = new ArrayList();
            for (SipSessionGroupExt group : this.mSipGroups.values()) {
                if (isCallerRadio || isCallerCreator(group)) {
                    profiles.add(group.getLocalProfile());
                }
            }
            return (SipProfile[]) profiles.toArray(new SipProfile[profiles.size()]);
        }
        return new SipProfile[0];
    }

    public synchronized void open(SipProfile localProfile, String opPackageName) {
        if (canUseSip(opPackageName, "open")) {
            localProfile.setCallingUid(Binder.getCallingUid());
            try {
                createGroup(localProfile);
            } catch (SipException e) {
                loge("openToMakeCalls()", e);
            }
        } else {
            return;
        }
        return;
    }

    public synchronized void open3(SipProfile localProfile, PendingIntent incomingCallPendingIntent, ISipSessionListener listener, String opPackageName) {
        if (canUseSip(opPackageName, "open3")) {
            localProfile.setCallingUid(Binder.getCallingUid());
            if (incomingCallPendingIntent == null) {
                log("open3: incomingCallPendingIntent cannot be null; the profile is not opened");
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("open3: ");
            stringBuilder.append(obfuscateSipUri(localProfile.getUriString()));
            stringBuilder.append(": ");
            stringBuilder.append(incomingCallPendingIntent);
            stringBuilder.append(": ");
            stringBuilder.append(listener);
            log(stringBuilder.toString());
            try {
                SipSessionGroupExt group = createGroup(localProfile, incomingCallPendingIntent, listener);
                if (localProfile.getAutoRegistration()) {
                    group.openToReceiveCalls();
                    updateWakeLocks();
                }
            } catch (SipException e) {
                loge("open3:", e);
            }
        } else {
            return;
        }
    }

    private boolean isCallerCreator(SipSessionGroupExt group) {
        return group.getLocalProfile().getCallingUid() == Binder.getCallingUid() ? DBG : false;
    }

    private boolean isCallerCreatorOrRadio(SipSessionGroupExt group) {
        return (isCallerRadio() || isCallerCreator(group)) ? DBG : false;
    }

    private boolean isCallerRadio() {
        return Binder.getCallingUid() == 1001 ? DBG : false;
    }

    public synchronized void close(String localProfileUri, String opPackageName) {
        if (canUseSip(opPackageName, "close")) {
            SipSessionGroupExt group = (SipSessionGroupExt) this.mSipGroups.get(localProfileUri);
            if (group != null) {
                if (isCallerCreatorOrRadio(group)) {
                    group = (SipSessionGroupExt) this.mSipGroups.remove(localProfileUri);
                    notifyProfileRemoved(group.getLocalProfile());
                    group.close();
                    updateWakeLocks();
                    return;
                }
                log("only creator or radio can close this profile");
            }
        }
    }

    public synchronized boolean isOpened(String localProfileUri, String opPackageName) {
        if (!canUseSip(opPackageName, "isOpened")) {
            return false;
        }
        SipSessionGroupExt group = (SipSessionGroupExt) this.mSipGroups.get(localProfileUri);
        if (group == null) {
            return false;
        }
        if (isCallerCreatorOrRadio(group)) {
            return DBG;
        }
        log("only creator or radio can query on the profile");
        return false;
    }

    public synchronized boolean isRegistered(String localProfileUri, String opPackageName) {
        if (!canUseSip(opPackageName, "isRegistered")) {
            return false;
        }
        SipSessionGroupExt group = (SipSessionGroupExt) this.mSipGroups.get(localProfileUri);
        if (group == null) {
            return false;
        }
        if (isCallerCreatorOrRadio(group)) {
            return group.isRegistered();
        }
        log("only creator or radio can query on the profile");
        return false;
    }

    /* JADX WARNING: Missing block: B:17:0x0027, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void setRegistrationListener(String localProfileUri, ISipSessionListener listener, String opPackageName) {
        if (canUseSip(opPackageName, "setRegistrationListener")) {
            SipSessionGroupExt group = (SipSessionGroupExt) this.mSipGroups.get(localProfileUri);
            if (group != null) {
                if (isCallerCreator(group)) {
                    group.setListener(listener);
                } else {
                    log("only creator can set listener on the profile");
                }
            }
        }
    }

    public synchronized ISipSession createSession(SipProfile localProfile, ISipSessionListener listener, String opPackageName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createSession: profile");
        stringBuilder.append(localProfile);
        log(stringBuilder.toString());
        if (!canUseSip(opPackageName, "createSession")) {
            return null;
        }
        localProfile.setCallingUid(Binder.getCallingUid());
        if (this.mNetworkType == -1) {
            log("createSession: mNetworkType==-1 ret=null");
            return null;
        }
        try {
            return createGroup(localProfile).createSession(listener);
        } catch (SipException e) {
            loge("createSession;", e);
            return null;
        }
    }

    public synchronized ISipSession getPendingSession(String callId, String opPackageName) {
        if (!canUseSip(opPackageName, "getPendingSession")) {
            return null;
        }
        if (callId == null) {
            return null;
        }
        return (ISipSession) this.mPendingSessions.get(callId);
    }

    private String determineLocalIp() {
        try {
            DatagramSocket s = new DatagramSocket();
            s.connect(InetAddress.getByName("192.168.1.1"), 80);
            return s.getLocalAddress().getHostAddress();
        } catch (IOException e) {
            loge("determineLocalIp()", e);
            return null;
        }
    }

    private SipSessionGroupExt createGroup(SipProfile localProfile) throws SipException {
        String key = localProfile.getUriString();
        SipSessionGroupExt group = (SipSessionGroupExt) this.mSipGroups.get(key);
        if (group == null) {
            group = new SipSessionGroupExt(localProfile, null, null);
            this.mSipGroups.put(key, group);
            notifyProfileAdded(localProfile);
            return group;
        } else if (isCallerCreator(group)) {
            return group;
        } else {
            throw new SipException("only creator can access the profile");
        }
    }

    private SipSessionGroupExt createGroup(SipProfile localProfile, PendingIntent incomingCallPendingIntent, ISipSessionListener listener) throws SipException {
        String key = localProfile.getUriString();
        SipSessionGroupExt group = (SipSessionGroupExt) this.mSipGroups.get(key);
        if (group == null) {
            group = new SipSessionGroupExt(localProfile, incomingCallPendingIntent, listener);
            this.mSipGroups.put(key, group);
            notifyProfileAdded(localProfile);
            return group;
        } else if (isCallerCreator(group)) {
            group.setIncomingCallPendingIntent(incomingCallPendingIntent);
            group.setListener(listener);
            return group;
        } else {
            throw new SipException("only creator can access the profile");
        }
    }

    private void notifyProfileAdded(SipProfile localProfile) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notify: profile added: ");
        stringBuilder.append(localProfile);
        log(stringBuilder.toString());
        Intent intent = new Intent(SipManager.ACTION_SIP_ADD_PHONE);
        intent.putExtra(SipManager.EXTRA_LOCAL_URI, localProfile.getUriString());
        this.mContext.sendBroadcast(intent);
        if (this.mSipGroups.size() == 1) {
            registerReceivers();
        }
    }

    private void notifyProfileRemoved(SipProfile localProfile) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notify: profile removed: ");
        stringBuilder.append(localProfile);
        log(stringBuilder.toString());
        Intent intent = new Intent(SipManager.ACTION_SIP_REMOVE_PHONE);
        intent.putExtra(SipManager.EXTRA_LOCAL_URI, localProfile.getUriString());
        this.mContext.sendBroadcast(intent);
        if (this.mSipGroups.size() == 0) {
            unregisterReceivers();
        }
    }

    private void stopPortMappingMeasurement() {
        if (this.mSipKeepAliveProcessCallback != null) {
            this.mSipKeepAliveProcessCallback.stop();
            this.mSipKeepAliveProcessCallback = null;
        }
    }

    private void startPortMappingLifetimeMeasurement(SipProfile localProfile) {
        startPortMappingLifetimeMeasurement(localProfile, DEFAULT_MAX_KEEPALIVE_INTERVAL);
    }

    private void startPortMappingLifetimeMeasurement(SipProfile localProfile, int maxInterval) {
        if (this.mSipKeepAliveProcessCallback == null && this.mKeepAliveInterval == -1 && isBehindNAT(this.mLocalIp)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startPortMappingLifetimeMeasurement: profile=");
            stringBuilder.append(localProfile.getUriString());
            log(stringBuilder.toString());
            int minInterval = this.mLastGoodKeepAliveInterval;
            if (minInterval >= maxInterval) {
                this.mLastGoodKeepAliveInterval = 10;
                minInterval = 10;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  reset min interval to ");
                stringBuilder2.append(minInterval);
                log(stringBuilder2.toString());
            }
            this.mSipKeepAliveProcessCallback = new SipKeepAliveProcessCallback(localProfile, minInterval, maxInterval);
            this.mSipKeepAliveProcessCallback.start();
        }
    }

    private void restartPortMappingLifetimeMeasurement(SipProfile localProfile, int maxInterval) {
        stopPortMappingMeasurement();
        this.mKeepAliveInterval = -1;
        startPortMappingLifetimeMeasurement(localProfile, maxInterval);
    }

    private synchronized void addPendingSession(ISipSession session) {
        try {
            cleanUpPendingSessions();
            this.mPendingSessions.put(session.getCallId(), session);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("#pending sess=");
            stringBuilder.append(this.mPendingSessions.size());
            log(stringBuilder.toString());
        } catch (RemoteException e) {
            loge("addPendingSession()", e);
        }
        return;
    }

    private void cleanUpPendingSessions() throws RemoteException {
        for (Entry<String, ISipSession> entry : (Entry[]) this.mPendingSessions.entrySet().toArray(new Entry[this.mPendingSessions.size()])) {
            if (((ISipSession) entry.getValue()).getState() != 3) {
                this.mPendingSessions.remove(entry.getKey());
            }
        }
    }

    private synchronized boolean callingSelf(SipSessionGroupExt ringingGroup, SipSessionImpl ringingSession) {
        String callId = ringingSession.getCallId();
        for (SipSessionGroupExt group : this.mSipGroups.values()) {
            if (group != ringingGroup && group.containsSession(callId)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("call self: ");
                stringBuilder.append(ringingSession.getLocalProfile().getUriString());
                stringBuilder.append(" -> ");
                stringBuilder.append(group.getLocalProfile().getUriString());
                log(stringBuilder.toString());
                return DBG;
            }
        }
        return false;
    }

    private synchronized void onKeepAliveIntervalChanged() {
        for (SipSessionGroupExt group : this.mSipGroups.values()) {
            group.onKeepAliveIntervalChanged();
        }
    }

    private int getKeepAliveInterval() {
        if (this.mKeepAliveInterval < 0) {
            return this.mLastGoodKeepAliveInterval;
        }
        return this.mKeepAliveInterval;
    }

    private boolean isBehindNAT(String address) {
        try {
            byte[] d = InetAddress.getByName(address).getAddress();
            if (d[0] == (byte) 10 || (((d[0] & 255) == 172 && (240 & d[1]) == 16) || ((d[0] & 255) == 192 && (d[1] & 255) == 168))) {
                return DBG;
            }
            return false;
        } catch (UnknownHostException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isBehindAT()");
            stringBuilder.append(address);
            loge(stringBuilder.toString(), e);
        }
    }

    private boolean canUseSip(String packageName, String message) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.USE_SIP", message);
        return this.mAppOps.noteOp(53, Binder.getCallingUid(), packageName) == 0 ? DBG : false;
    }

    private void registerReceivers() {
        this.mContext.registerReceiver(this.mConnectivityReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        log("registerReceivers:");
    }

    private void unregisterReceivers() {
        this.mContext.unregisterReceiver(this.mConnectivityReceiver);
        log("unregisterReceivers:");
        this.mWifiLock.release();
        this.mNetworkType = -1;
    }

    private void updateWakeLocks() {
        for (SipSessionGroupExt group : this.mSipGroups.values()) {
            if (group.isOpenedToReceiveCalls()) {
                if (this.mNetworkType == 1 || this.mNetworkType == -1) {
                    this.mWifiLock.acquire();
                } else {
                    this.mWifiLock.release();
                }
                return;
            }
        }
        this.mWifiLock.release();
        this.mMyWakeLock.reset();
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x0041 A:{SYNTHETIC, Splitter:B:25:0x0041} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x003f A:{SKIP} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void onConnectivityChanged(NetworkInfo info) {
        int networkType;
        if (info != null) {
            try {
                if (!info.isConnected()) {
                    if (info.getType() != this.mNetworkType) {
                    }
                    networkType = (info == null && info.isConnected()) ? info.getType() : -1;
                    if (this.mSipOnWifiOnly && networkType != 1) {
                        networkType = -1;
                    }
                    if (this.mNetworkType == networkType) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onConnectivityChanged: ");
                        stringBuilder.append(this.mNetworkType);
                        stringBuilder.append(" -> ");
                        stringBuilder.append(networkType);
                        log(stringBuilder.toString());
                        if (this.mNetworkType != -1) {
                            this.mLocalIp = null;
                            stopPortMappingMeasurement();
                            for (SipSessionGroupExt group : this.mSipGroups.values()) {
                                group.onConnectivityChanged(false);
                            }
                        }
                        this.mNetworkType = networkType;
                        if (this.mNetworkType != -1) {
                            this.mLocalIp = determineLocalIp();
                            this.mKeepAliveInterval = -1;
                            this.mLastGoodKeepAliveInterval = 10;
                            for (SipSessionGroupExt group2 : this.mSipGroups.values()) {
                                group2.onConnectivityChanged(DBG);
                            }
                        }
                        updateWakeLocks();
                    } else {
                        return;
                    }
                }
            } catch (SipException e) {
                loge("onConnectivityChanged()", e);
            } catch (Throwable th) {
            }
        }
        info = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getActiveNetworkInfo();
        if (info == null) {
        }
        networkType = -1;
        if (this.mNetworkType == networkType) {
        }
    }

    private static Looper createLooper() {
        HandlerThread thread = new HandlerThread("SipService.Executor");
        thread.start();
        return thread.getLooper();
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private static void slog(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s, Throwable e) {
        Rlog.e(TAG, s, e);
    }

    public static String obfuscateSipUri(String sipUri) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        sipUri = sipUri.trim();
        if (sipUri.startsWith("sip:")) {
            start = 4;
            sb.append("sip:");
        }
        int len = sipUri.length();
        char prevC = 0;
        int i = start;
        while (i < len) {
            char c = sipUri.charAt(i);
            char nextC = i + 1 < len ? sipUri.charAt(i + 1) : 0;
            char charToAppend = '*';
            if (i - start < 1 || i + 1 == len || isAllowedCharacter(c) || prevC == '@' || nextC == '@') {
                charToAppend = c;
            }
            sb.append(charToAppend);
            prevC = c;
            i++;
        }
        return sb.toString();
    }

    private static boolean isAllowedCharacter(char c) {
        return (c == '@' || c == '.') ? DBG : false;
    }
}
