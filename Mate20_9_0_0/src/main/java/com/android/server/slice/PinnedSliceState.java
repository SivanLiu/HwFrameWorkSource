package com.android.server.slice;

import android.app.slice.SliceSpec;
import android.content.ContentProviderClient;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Objects;

public class PinnedSliceState {
    private static final long SLICE_TIMEOUT = 5000;
    private static final String TAG = "PinnedSliceState";
    private final DeathRecipient mDeathRecipient = new -$$Lambda$PinnedSliceState$KzxFkvfomRuMb5PD8_pIHDIhUUE(this);
    @GuardedBy("mLock")
    private final ArrayMap<IBinder, ListenerInfo> mListeners = new ArrayMap();
    private final Object mLock;
    @GuardedBy("mLock")
    private final ArraySet<String> mPinnedPkgs = new ArraySet();
    private final String mPkg;
    private final SliceManagerService mService;
    private boolean mSlicePinned;
    @GuardedBy("mLock")
    private SliceSpec[] mSupportedSpecs = null;
    private final Uri mUri;

    private class ListenerInfo {
        private int callingPid;
        private int callingUid;
        private boolean hasPermission;
        private String pkg;
        private IBinder token;

        public ListenerInfo(IBinder token, String pkg, boolean hasPermission, int callingUid, int callingPid) {
            this.token = token;
            this.pkg = pkg;
            this.hasPermission = hasPermission;
            this.callingUid = callingUid;
            this.callingPid = callingPid;
        }
    }

    public PinnedSliceState(SliceManagerService service, Uri uri, String pkg) {
        this.mService = service;
        this.mUri = uri;
        this.mPkg = pkg;
        this.mLock = this.mService.getLock();
    }

    public String getPkg() {
        return this.mPkg;
    }

    public SliceSpec[] getSpecs() {
        return this.mSupportedSpecs;
    }

    public void mergeSpecs(SliceSpec[] supportedSpecs) {
        synchronized (this.mLock) {
            if (this.mSupportedSpecs == null) {
                this.mSupportedSpecs = supportedSpecs;
            } else {
                this.mSupportedSpecs = (SliceSpec[]) Arrays.asList(this.mSupportedSpecs).stream().map(new -$$Lambda$PinnedSliceState$j_JfEZwPCa729MjgsTSd8MAItIw(this, supportedSpecs)).filter(-$$Lambda$PinnedSliceState$2PaYhOaggf1E5xg82LTTEwxmLE4.INSTANCE).toArray(-$$Lambda$PinnedSliceState$vxnx7v9Z67Tj9aywVmtdX48br1M.INSTANCE);
            }
        }
    }

    public static /* synthetic */ SliceSpec lambda$mergeSpecs$0(PinnedSliceState pinnedSliceState, SliceSpec[] supportedSpecs, SliceSpec s) {
        SliceSpec other = pinnedSliceState.findSpec(supportedSpecs, s.getType());
        if (other == null) {
            return null;
        }
        if (other.getRevision() < s.getRevision()) {
            return other;
        }
        return s;
    }

    static /* synthetic */ boolean lambda$mergeSpecs$1(SliceSpec s) {
        return s != null;
    }

    private SliceSpec findSpec(SliceSpec[] specs, String type) {
        for (SliceSpec spec : specs) {
            if (Objects.equals(spec.getType(), type)) {
                return spec;
            }
        }
        return null;
    }

    public Uri getUri() {
        return this.mUri;
    }

    public void destroy() {
        setSlicePinned(false);
    }

    /* JADX WARNING: Missing block: B:12:0x002b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setSlicePinned(boolean pinned) {
        synchronized (this.mLock) {
            if (this.mSlicePinned == pinned) {
                return;
            }
            this.mSlicePinned = pinned;
            if (pinned) {
                this.mService.getHandler().post(new -$$Lambda$PinnedSliceState$TZdoqC_LDA8If7sQ7WXz9LM6VHg(this));
            } else {
                this.mService.getHandler().post(new -$$Lambda$PinnedSliceState$t5Vl61Ns1u_83c4ri7920sczEu0(this));
            }
        }
    }

    public void pin(String pkg, SliceSpec[] specs, IBinder token) {
        synchronized (this.mLock) {
            this.mListeners.put(token, new ListenerInfo(token, pkg, true, Binder.getCallingUid(), Binder.getCallingPid()));
            try {
                token.linkToDeath(this.mDeathRecipient, 0);
            } catch (RemoteException e) {
            }
            mergeSpecs(specs);
            setSlicePinned(true);
        }
    }

    public boolean unpin(String pkg, IBinder token) {
        synchronized (this.mLock) {
            token.unlinkToDeath(this.mDeathRecipient, 0);
            this.mListeners.remove(token);
        }
        return hasPinOrListener() ^ 1;
    }

    public boolean isListening() {
        int isEmpty;
        synchronized (this.mLock) {
            isEmpty = this.mListeners.isEmpty() ^ 1;
        }
        return isEmpty;
    }

    @VisibleForTesting
    public boolean hasPinOrListener() {
        boolean z;
        synchronized (this.mLock) {
            if (this.mPinnedPkgs.isEmpty()) {
                if (this.mListeners.isEmpty()) {
                    z = false;
                }
            }
            z = true;
        }
        return z;
    }

    ContentProviderClient getClient() {
        ContentProviderClient client = this.mService.getContext().getContentResolver().acquireContentProviderClient(this.mUri);
        if (client == null) {
            return null;
        }
        client.setDetectNotResponding(SLICE_TIMEOUT);
        return client;
    }

    private void checkSelfRemove() {
        if (!hasPinOrListener()) {
            this.mService.removePinnedSlice(this.mUri);
        }
    }

    private void handleRecheckListeners() {
        if (hasPinOrListener()) {
            synchronized (this.mLock) {
                for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                    if (!((ListenerInfo) this.mListeners.valueAt(i)).token.isBinderAlive()) {
                        this.mListeners.removeAt(i);
                    }
                }
                checkSelfRemove();
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0044, code skipped:
            if (r0 != null) goto L_0x0046;
     */
    /* JADX WARNING: Missing block: B:20:0x0046, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleSendPinned() {
        ContentProviderClient client = getClient();
        if (client == null) {
            if (client != null) {
                $closeResource(null, client);
            }
            return;
        }
        Bundle b = new Bundle();
        b.putParcelable("slice_uri", this.mUri);
        try {
            client.call("pin", null, b);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to contact ");
            stringBuilder.append(this.mUri);
            Log.w(str, stringBuilder.toString(), e);
        }
        if (client != null) {
            $closeResource(null, client);
        }
        return;
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    /* JADX WARNING: Missing block: B:19:0x0044, code skipped:
            if (r0 != null) goto L_0x0046;
     */
    /* JADX WARNING: Missing block: B:20:0x0046, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleSendUnpinned() {
        ContentProviderClient client = getClient();
        if (client == null) {
            if (client != null) {
                $closeResource(null, client);
            }
            return;
        }
        Bundle b = new Bundle();
        b.putParcelable("slice_uri", this.mUri);
        try {
            client.call("unpin", null, b);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to contact ");
            stringBuilder.append(this.mUri);
            Log.w(str, stringBuilder.toString(), e);
        }
        if (client != null) {
            $closeResource(null, client);
        }
        return;
    }
}
