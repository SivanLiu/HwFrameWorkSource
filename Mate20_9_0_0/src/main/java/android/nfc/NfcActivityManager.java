package android.nfc;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.nfc.IAppCallback.Stub;
import android.nfc.NfcAdapter.CreateBeamUrisCallback;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcAdapter.ReaderCallback;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class NfcActivityManager extends Stub implements ActivityLifecycleCallbacks {
    static final Boolean DBG = Boolean.valueOf(false);
    static final String TAG = "NFC";
    final List<NfcActivityState> mActivities = new LinkedList();
    final NfcAdapter mAdapter;
    final List<NfcApplicationState> mApps = new ArrayList(1);

    class NfcActivityState {
        Activity activity;
        int flags = 0;
        NdefMessage ndefMessage = null;
        CreateNdefMessageCallback ndefMessageCallback = null;
        OnNdefPushCompleteCallback onNdefPushCompleteCallback = null;
        ReaderCallback readerCallback = null;
        Bundle readerModeExtras = null;
        int readerModeFlags = 0;
        boolean resumed = false;
        Binder token;
        CreateBeamUrisCallback uriCallback = null;
        Uri[] uris = null;

        public NfcActivityState(Activity activity) {
            if (activity.getWindow().isDestroyed()) {
                throw new IllegalStateException("activity is already destroyed");
            }
            this.resumed = activity.isResumed();
            this.activity = activity;
            this.token = new Binder();
            NfcActivityManager.this.registerApplication(activity.getApplication());
        }

        public void destroy() {
            NfcActivityManager.this.unregisterApplication(this.activity.getApplication());
            this.resumed = false;
            this.activity = null;
            this.ndefMessage = null;
            this.ndefMessageCallback = null;
            this.onNdefPushCompleteCallback = null;
            this.uriCallback = null;
            this.uris = null;
            this.readerModeFlags = 0;
            this.token = null;
        }

        public String toString() {
            StringBuilder s = new StringBuilder("[").append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            s.append(this.ndefMessage);
            s.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            s.append(this.ndefMessageCallback);
            s.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            s.append(this.uriCallback);
            s.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            if (this.uris != null) {
                for (Uri uri : this.uris) {
                    s.append(this.onNdefPushCompleteCallback);
                    s.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    s.append(uri);
                    s.append("]");
                }
            }
            return s.toString();
        }
    }

    class NfcApplicationState {
        final Application app;
        int refCount = 0;

        public NfcApplicationState(Application app) {
            this.app = app;
        }

        public void register() {
            this.refCount++;
            if (this.refCount == 1) {
                this.app.registerActivityLifecycleCallbacks(NfcActivityManager.this);
            }
        }

        public void unregister() {
            this.refCount--;
            if (this.refCount == 0) {
                this.app.unregisterActivityLifecycleCallbacks(NfcActivityManager.this);
            } else if (this.refCount < 0) {
                String str = NfcActivityManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("-ve refcount for ");
                stringBuilder.append(this.app);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    NfcApplicationState findAppState(Application app) {
        for (NfcApplicationState appState : this.mApps) {
            if (appState.app == app) {
                return appState;
            }
        }
        return null;
    }

    void registerApplication(Application app) {
        NfcApplicationState appState = findAppState(app);
        if (appState == null) {
            appState = new NfcApplicationState(app);
            this.mApps.add(appState);
        }
        appState.register();
    }

    void unregisterApplication(Application app) {
        NfcApplicationState appState = findAppState(app);
        if (appState == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("app was not registered ");
            stringBuilder.append(app);
            Log.e(str, stringBuilder.toString());
            return;
        }
        appState.unregister();
    }

    synchronized NfcActivityState findActivityState(Activity activity) {
        for (NfcActivityState state : this.mActivities) {
            if (state.activity == activity) {
                return state;
            }
        }
        return null;
    }

    synchronized NfcActivityState getActivityState(Activity activity) {
        NfcActivityState state;
        state = findActivityState(activity);
        if (state == null) {
            state = new NfcActivityState(activity);
            this.mActivities.add(state);
        }
        return state;
    }

    synchronized NfcActivityState findResumedActivityState() {
        for (NfcActivityState state : this.mActivities) {
            if (state.resumed) {
                return state;
            }
        }
        return null;
    }

    synchronized void destroyActivityState(Activity activity) {
        NfcActivityState activityState = findActivityState(activity);
        if (activityState != null) {
            activityState.destroy();
            this.mActivities.remove(activityState);
        }
    }

    public NfcActivityManager(NfcAdapter adapter) {
        this.mAdapter = adapter;
    }

    public void enableReaderMode(Activity activity, ReaderCallback callback, int flags, Bundle extras) {
        Binder token;
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.readerCallback = callback;
            state.readerModeFlags = flags;
            state.readerModeExtras = extras;
            token = state.token;
            isResumed = state.resumed;
        }
        if (isResumed) {
            setReaderMode(token, flags, extras);
        }
    }

    public void disableReaderMode(Activity activity) {
        Binder token;
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.readerCallback = null;
            state.readerModeFlags = 0;
            state.readerModeExtras = null;
            token = state.token;
            isResumed = state.resumed;
        }
        if (isResumed) {
            setReaderMode(token, 0, null);
        }
    }

    public void setReaderMode(Binder token, int flags, Bundle extras) {
        if (DBG.booleanValue()) {
            Log.d(TAG, "Setting reader mode");
        }
        try {
            NfcAdapter.sService.setReaderMode(token, this, flags, extras);
        } catch (RemoteException e) {
            this.mAdapter.attemptDeadServiceRecovery(e);
        }
    }

    public void setNdefPushContentUri(Activity activity, Uri[] uris) {
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.uris = uris;
            isResumed = state.resumed;
        }
        if (isResumed) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setNdefPushContentUriCallback(Activity activity, CreateBeamUrisCallback callback) {
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.uriCallback = callback;
            isResumed = state.resumed;
        }
        if (isResumed) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setNdefPushMessage(Activity activity, NdefMessage message, int flags) {
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.ndefMessage = message;
            state.flags = flags;
            isResumed = state.resumed;
        }
        if (isResumed) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setNdefPushMessageCallback(Activity activity, CreateNdefMessageCallback callback, int flags) {
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.ndefMessageCallback = callback;
            state.flags = flags;
            isResumed = state.resumed;
        }
        if (isResumed) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setOnNdefPushCompleteCallback(Activity activity, OnNdefPushCompleteCallback callback) {
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.onNdefPushCompleteCallback = callback;
            isResumed = state.resumed;
        }
        if (isResumed) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    void requestNfcServiceCallback() {
        try {
            NfcAdapter.sService.setAppCallback(this);
        } catch (RemoteException e) {
            this.mAdapter.attemptDeadServiceRecovery(e);
        }
    }

    void verifyNfcPermission() {
        try {
            NfcAdapter.sService.verifyNfcPermission();
        } catch (RemoteException e) {
            this.mAdapter.attemptDeadServiceRecovery(e);
        }
    }

    /* JADX WARNING: Missing block: B:14:0x0028, code skipped:
            r10 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:15:0x002c, code skipped:
            if (r4 == null) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:18:0x0032, code skipped:
            r6 = r4.createNdefMessage(r2);
     */
    /* JADX WARNING: Missing block: B:19:0x0034, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:20:0x0035, code skipped:
            r16 = r2;
     */
    /* JADX WARNING: Missing block: B:21:0x0039, code skipped:
            if (r5 == null) goto L_0x00a1;
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            r7 = r5.createBeamUris(r2);
     */
    /* JADX WARNING: Missing block: B:24:0x0040, code skipped:
            if (r7 == null) goto L_0x00a1;
     */
    /* JADX WARNING: Missing block: B:25:0x0042, code skipped:
            r12 = new java.util.ArrayList();
            r13 = r7.length;
            r14 = 0;
     */
    /* JADX WARNING: Missing block: B:26:0x0049, code skipped:
            if (r14 >= r13) goto L_0x008d;
     */
    /* JADX WARNING: Missing block: B:27:0x004b, code skipped:
            r15 = r7[r14];
     */
    /* JADX WARNING: Missing block: B:28:0x004d, code skipped:
            if (r15 != null) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:30:0x0051, code skipped:
            r16 = r2;
     */
    /* JADX WARNING: Missing block: B:32:?, code skipped:
            android.util.Log.e(TAG, "Uri not allowed to be null.");
     */
    /* JADX WARNING: Missing block: B:33:0x0059, code skipped:
            r16 = r2;
            r0 = r15.getScheme();
     */
    /* JADX WARNING: Missing block: B:34:0x005f, code skipped:
            if (r0 == null) goto L_0x007e;
     */
    /* JADX WARNING: Missing block: B:36:0x0067, code skipped:
            if (r0.equalsIgnoreCase("file") != null) goto L_0x0072;
     */
    /* JADX WARNING: Missing block: B:38:0x006f, code skipped:
            if (r0.equalsIgnoreCase("content") != null) goto L_0x0072;
     */
    /* JADX WARNING: Missing block: B:40:0x0072, code skipped:
            r12.add(android.content.ContentProvider.maybeAddUserId(r15, r9.getUserId()));
     */
    /* JADX WARNING: Missing block: B:41:0x007e, code skipped:
            r17 = r0;
            android.util.Log.e(TAG, "Uri needs to have either scheme file or scheme content");
     */
    /* JADX WARNING: Missing block: B:42:0x0088, code skipped:
            r14 = r14 + 1;
            r2 = r16;
     */
    /* JADX WARNING: Missing block: B:43:0x008d, code skipped:
            r16 = r2;
            r7 = (android.net.Uri[]) r12.toArray(new android.net.Uri[r12.size()]);
     */
    /* JADX WARNING: Missing block: B:44:0x009d, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:45:0x009e, code skipped:
            r16 = r2;
     */
    /* JADX WARNING: Missing block: B:46:0x00a1, code skipped:
            r16 = r2;
     */
    /* JADX WARNING: Missing block: B:47:0x00a3, code skipped:
            if (r7 == null) goto L_0x00bc;
     */
    /* JADX WARNING: Missing block: B:49:0x00a6, code skipped:
            if (r7.length <= 0) goto L_0x00bc;
     */
    /* JADX WARNING: Missing block: B:50:0x00a8, code skipped:
            r0 = r7.length;
            r2 = 0;
     */
    /* JADX WARNING: Missing block: B:51:0x00aa, code skipped:
            if (r2 >= r0) goto L_0x00bc;
     */
    /* JADX WARNING: Missing block: B:52:0x00ac, code skipped:
            r9.grantUriPermission("com.android.nfc", r7[r2], 1);
     */
    /* JADX WARNING: Missing block: B:53:0x00b4, code skipped:
            r2 = r2 + 1;
     */
    /* JADX WARNING: Missing block: B:54:0x00b7, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:55:0x00b8, code skipped:
            android.os.Binder.restoreCallingIdentity(r10);
     */
    /* JADX WARNING: Missing block: B:56:0x00bb, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:57:0x00bc, code skipped:
            android.os.Binder.restoreCallingIdentity(r10);
     */
    /* JADX WARNING: Missing block: B:58:0x00c9, code skipped:
            return new android.nfc.BeamShareData(r6, r7, r9.getUser(), r8);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public BeamShareData createBeamShareData(byte peerLlcpVersion) {
        Throwable th;
        NfcEvent nfcEvent;
        NfcEvent event = new NfcEvent(this.mAdapter, peerLlcpVersion);
        synchronized (this) {
            try {
                NfcActivityState state = findResumedActivityState();
                if (state == null) {
                    try {
                        return null;
                    } catch (Throwable th2) {
                        th = th2;
                        nfcEvent = event;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                        throw th;
                    }
                }
                CreateNdefMessageCallback ndefCallback = state.ndefMessageCallback;
                CreateBeamUrisCallback urisCallback = state.uriCallback;
                NdefMessage message = state.ndefMessage;
                Uri[] uris = state.uris;
                int flags = state.flags;
                Activity activity = state.activity;
            } catch (Throwable th4) {
                th = th4;
                nfcEvent = event;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x000d, code skipped:
            r1 = new android.nfc.NfcEvent(r3.mAdapter, r4);
     */
    /* JADX WARNING: Missing block: B:9:0x0014, code skipped:
            if (r0 == null) goto L_0x0019;
     */
    /* JADX WARNING: Missing block: B:10:0x0016, code skipped:
            r0.onNdefPushComplete(r1);
     */
    /* JADX WARNING: Missing block: B:11:0x0019, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onNdefPushComplete(byte peerLlcpVersion) {
        synchronized (this) {
            NfcActivityState state = findResumedActivityState();
            if (state == null) {
                return;
            }
            OnNdefPushCompleteCallback callback = state.onNdefPushCompleteCallback;
        }
    }

    /* JADX WARNING: Missing block: B:8:0x000d, code skipped:
            if (r0 == null) goto L_0x0012;
     */
    /* JADX WARNING: Missing block: B:9:0x000f, code skipped:
            r0.onTagDiscovered(r3);
     */
    /* JADX WARNING: Missing block: B:10:0x0012, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onTagDiscovered(Tag tag) throws RemoteException {
        synchronized (this) {
            NfcActivityState state = findResumedActivityState();
            if (state == null) {
                return;
            }
            ReaderCallback callback = state.readerCallback;
        }
    }

    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    public void onActivityStarted(Activity activity) {
    }

    /* JADX WARNING: Missing block: B:11:0x003e, code skipped:
            if (r0 == 0) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:12:0x0040, code skipped:
            setReaderMode(r3, r0, r1);
     */
    /* JADX WARNING: Missing block: B:13:0x0043, code skipped:
            requestNfcServiceCallback();
     */
    /* JADX WARNING: Missing block: B:14:0x0046, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onActivityResumed(Activity activity) {
        synchronized (this) {
            NfcActivityState state = findActivityState(activity);
            if (DBG.booleanValue()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onResume() for ");
                stringBuilder.append(activity);
                stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                stringBuilder.append(state);
                Log.d(str, stringBuilder.toString());
            }
            if (state == null) {
                return;
            }
            state.resumed = true;
            Binder token = state.token;
            int readerModeFlags = state.readerModeFlags;
            Bundle readerModeExtras = state.readerModeExtras;
        }
    }

    /* JADX WARNING: Missing block: B:14:0x003e, code skipped:
            if (r0 == false) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:15:0x0040, code skipped:
            setReaderMode(r2, 0, null);
     */
    /* JADX WARNING: Missing block: B:16:0x0044, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onActivityPaused(Activity activity) {
        synchronized (this) {
            NfcActivityState state = findActivityState(activity);
            if (DBG.booleanValue()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onPause() for ");
                stringBuilder.append(activity);
                stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                stringBuilder.append(state);
                Log.d(str, stringBuilder.toString());
            }
            if (state == null) {
                return;
            }
            state.resumed = false;
            Binder token = state.token;
            boolean readerModeFlagsSet = state.readerModeFlags != 0;
        }
    }

    public void onActivityStopped(Activity activity) {
    }

    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    public void onActivityDestroyed(Activity activity) {
        synchronized (this) {
            NfcActivityState state = findActivityState(activity);
            if (DBG.booleanValue()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onDestroy() for ");
                stringBuilder.append(activity);
                stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                stringBuilder.append(state);
                Log.d(str, stringBuilder.toString());
            }
            if (state != null) {
                destroyActivityState(activity);
            }
        }
    }
}
