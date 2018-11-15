package com.android.server.voiceinteraction;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.IVoiceInteractionSessionService;
import android.util.Slog;
import android.view.IWindowManager;
import com.android.internal.app.AssistUtils;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractionSessionShowCallback.Stub;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.LocalServices;
import com.android.server.am.AssistDataRequester;
import com.android.server.am.AssistDataRequester.AssistDataRequesterCallbacks;
import com.android.server.statusbar.StatusBarManagerInternal;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

final class VoiceInteractionSessionConnection implements ServiceConnection, AssistDataRequesterCallbacks {
    static final String TAG = "VoiceInteractionServiceManager";
    final IActivityManager mAm;
    final AppOpsManager mAppOps;
    AssistDataRequester mAssistDataRequester;
    final Intent mBindIntent;
    boolean mBound;
    final Callback mCallback;
    final int mCallingUid;
    boolean mCanceled;
    final Context mContext;
    final ServiceConnection mFullConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };
    boolean mFullyBound;
    final Handler mHandler;
    final IWindowManager mIWindowManager;
    IVoiceInteractor mInteractor;
    final Object mLock;
    ArrayList<IVoiceInteractionSessionShowCallback> mPendingShowCallbacks = new ArrayList();
    final IBinder mPermissionOwner;
    IVoiceInteractionSessionService mService;
    IVoiceInteractionSession mSession;
    final ComponentName mSessionComponentName;
    Bundle mShowArgs;
    private Runnable mShowAssistDisclosureRunnable = new Runnable() {
        public void run() {
            StatusBarManagerInternal statusBarInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            if (statusBarInternal != null) {
                statusBarInternal.showAssistDisclosure();
            }
        }
    };
    IVoiceInteractionSessionShowCallback mShowCallback = new Stub() {
        public void onFailed() throws RemoteException {
            synchronized (VoiceInteractionSessionConnection.this.mLock) {
                VoiceInteractionSessionConnection.this.notifyPendingShowCallbacksFailedLocked();
            }
        }

        public void onShown() throws RemoteException {
            synchronized (VoiceInteractionSessionConnection.this.mLock) {
                VoiceInteractionSessionConnection.this.notifyPendingShowCallbacksShownLocked();
            }
        }
    };
    int mShowFlags;
    boolean mShown;
    final IBinder mToken = new Binder();
    final int mUser;

    public interface Callback {
        void onSessionHidden(VoiceInteractionSessionConnection voiceInteractionSessionConnection);

        void onSessionShown(VoiceInteractionSessionConnection voiceInteractionSessionConnection);

        void sessionConnectionGone(VoiceInteractionSessionConnection voiceInteractionSessionConnection);
    }

    public VoiceInteractionSessionConnection(Object lock, ComponentName component, int user, Context context, Callback callback, int callingUid, Handler handler) {
        StringBuilder stringBuilder;
        Context context2 = context;
        this.mLock = lock;
        this.mSessionComponentName = component;
        this.mUser = user;
        this.mContext = context2;
        this.mCallback = callback;
        this.mCallingUid = callingUid;
        this.mHandler = handler;
        this.mAm = ActivityManager.getService();
        this.mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        this.mAppOps = (AppOpsManager) context2.getSystemService(AppOpsManager.class);
        this.mAssistDataRequester = new AssistDataRequester(this.mContext, this.mAm, this.mIWindowManager, (AppOpsManager) this.mContext.getSystemService("appops"), this, this.mLock, 49, 50);
        IBinder permOwner = null;
        try {
            IActivityManager iActivityManager = this.mAm;
            stringBuilder = new StringBuilder();
            stringBuilder.append("voicesession:");
            stringBuilder.append(component.flattenToShortString());
            permOwner = iActivityManager.newUriPermissionOwner(stringBuilder.toString());
        } catch (RemoteException e) {
            Slog.w("voicesession", "AM dead", e);
        }
        this.mPermissionOwner = permOwner;
        this.mBindIntent = new Intent("android.service.voice.VoiceInteractionService");
        this.mBindIntent.setComponent(this.mSessionComponentName);
        this.mBound = this.mContext.bindServiceAsUser(this.mBindIntent, this, 49, new UserHandle(this.mUser));
        if (this.mBound) {
            try {
                this.mIWindowManager.addWindowToken(this.mToken, 2031, 0);
                return;
            } catch (RemoteException e2) {
                Slog.w(TAG, "Failed adding window token", e2);
                return;
            }
        }
        String str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failed binding to voice interaction session service ");
        stringBuilder.append(this.mSessionComponentName);
        Slog.w(str, stringBuilder.toString());
    }

    public int getUserDisabledShowContextLocked() {
        int flags = 0;
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "assist_structure_enabled", 1, this.mUser) == 0) {
            flags = 0 | 1;
        }
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "assist_screenshot_enabled", 1, this.mUser) == 0) {
            return flags | 2;
        }
        return flags;
    }

    public boolean showLocked(Bundle args, int flags, int disabledContext, IVoiceInteractionSessionShowCallback showCallback, List<IBinder> topActivities) {
        if (this.mBound) {
            if (!this.mFullyBound) {
                this.mFullyBound = this.mContext.bindServiceAsUser(this.mBindIntent, this.mFullConnection, 201326593, new UserHandle(this.mUser));
            }
            this.mShown = true;
            this.mShowArgs = args;
            this.mShowFlags = flags;
            disabledContext |= getUserDisabledShowContextLocked();
            this.mAssistDataRequester.requestAssistData(topActivities, (flags & 1) != 0, (flags & 2) != 0, (disabledContext & 1) == 0, (disabledContext & 2) == 0, this.mCallingUid, this.mSessionComponentName.getPackageName());
            boolean needDisclosure = this.mAssistDataRequester.getPendingDataCount() > 0 || this.mAssistDataRequester.getPendingScreenshotCount() > 0;
            if (needDisclosure && AssistUtils.shouldDisclose(this.mContext, this.mSessionComponentName)) {
                this.mHandler.post(this.mShowAssistDisclosureRunnable);
            }
            if (this.mSession != null) {
                try {
                    this.mSession.show(this.mShowArgs, this.mShowFlags, showCallback);
                    this.mShowArgs = null;
                    this.mShowFlags = 0;
                } catch (RemoteException e) {
                }
                this.mAssistDataRequester.processPendingAssistData();
            } else if (showCallback != null) {
                this.mPendingShowCallbacks.add(showCallback);
            }
            this.mCallback.onSessionShown(this);
            return true;
        }
        if (showCallback != null) {
            try {
                showCallback.onFailed();
            } catch (RemoteException e2) {
            }
        }
        return false;
    }

    public boolean canHandleReceivedAssistDataLocked() {
        return this.mSession != null;
    }

    public void onAssistDataReceivedLocked(Bundle data, int activityIndex, int activityCount) {
        Bundle bundle = data;
        if (this.mSession != null) {
            if (bundle == null) {
                try {
                    this.mSession.handleAssist(null, null, null, 0, 0);
                } catch (RemoteException e) {
                }
            } else {
                Bundle assistData = bundle.getBundle("data");
                AssistStructure structure = (AssistStructure) bundle.getParcelable("structure");
                AssistContent content = (AssistContent) bundle.getParcelable("content");
                int uid = bundle.getInt("android.intent.extra.ASSIST_UID", -1);
                if (uid >= 0 && content != null) {
                    ClipData clipData;
                    Intent intent = content.getIntent();
                    if (intent != null) {
                        clipData = intent.getClipData();
                        if (clipData != null && Intent.isAccessUriMode(intent.getFlags())) {
                            grantClipDataPermissions(clipData, intent.getFlags(), uid, this.mCallingUid, this.mSessionComponentName.getPackageName());
                        }
                    }
                    clipData = content.getClipData();
                    if (clipData != null) {
                        grantClipDataPermissions(clipData, 1, uid, this.mCallingUid, this.mSessionComponentName.getPackageName());
                    }
                }
                try {
                    this.mSession.handleAssist(assistData, structure, content, activityIndex, activityCount);
                } catch (RemoteException e2) {
                }
            }
        }
    }

    public void onAssistScreenshotReceivedLocked(Bitmap screenshot) {
        if (this.mSession != null) {
            try {
                this.mSession.handleScreenshot(screenshot);
            } catch (RemoteException e) {
            }
        }
    }

    void grantUriPermission(Uri uri, int mode, int srcUid, int destUid, String destPkg) {
        SecurityException e;
        Throwable th;
        Uri uri2 = uri;
        if ("content".equals(uri2.getScheme())) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mAm.checkGrantUriPermission(srcUid, null, ContentProvider.getUriWithoutUserId(uri2), mode, ContentProvider.getUserIdFromUri(uri2, UserHandle.getUserId(srcUid)));
                int sourceUserId = ContentProvider.getUserIdFromUri(uri2, this.mUser);
                Uri uri3 = ContentProvider.getUriWithoutUserId(uri2);
                try {
                    this.mAm.grantUriPermissionFromOwner(this.mPermissionOwner, srcUid, destPkg, uri3, 1, sourceUserId, this.mUser);
                    Binder.restoreCallingIdentity(ident);
                    uri2 = uri3;
                } catch (RemoteException e2) {
                    Binder.restoreCallingIdentity(ident);
                } catch (SecurityException e3) {
                    e = e3;
                    uri2 = uri3;
                    try {
                        Slog.w(TAG, "Can't propagate permission", e);
                        Binder.restoreCallingIdentity(ident);
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    uri2 = uri3;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            } catch (RemoteException e4) {
                Binder.restoreCallingIdentity(ident);
            } catch (SecurityException e5) {
                e = e5;
                Slog.w(TAG, "Can't propagate permission", e);
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    void grantClipDataItemPermission(Item item, int mode, int srcUid, int destUid, String destPkg) {
        if (item.getUri() != null) {
            grantUriPermission(item.getUri(), mode, srcUid, destUid, destPkg);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            grantUriPermission(intent.getData(), mode, srcUid, destUid, destPkg);
        }
    }

    void grantClipDataPermissions(ClipData data, int mode, int srcUid, int destUid, String destPkg) {
        int N = data.getItemCount();
        for (int i = 0; i < N; i++) {
            grantClipDataItemPermission(data.getItemAt(i), mode, srcUid, destUid, destPkg);
        }
    }

    public boolean hideLocked() {
        if (!this.mBound) {
            return false;
        }
        if (this.mShown) {
            this.mShown = false;
            this.mShowArgs = null;
            this.mShowFlags = 0;
            this.mAssistDataRequester.cancel();
            this.mPendingShowCallbacks.clear();
            if (this.mSession != null) {
                try {
                    this.mSession.hide();
                } catch (RemoteException e) {
                }
            }
            try {
                this.mAm.revokeUriPermissionFromOwner(this.mPermissionOwner, null, 3, this.mUser);
            } catch (RemoteException e2) {
            }
            if (this.mSession != null) {
                try {
                    this.mAm.finishVoiceTask(this.mSession);
                } catch (RemoteException e3) {
                }
            }
            this.mCallback.onSessionHidden(this);
        }
        if (this.mFullyBound) {
            this.mContext.unbindService(this.mFullConnection);
            this.mFullyBound = false;
        }
        return true;
    }

    public void cancelLocked(boolean finishTask) {
        hideLocked();
        this.mCanceled = true;
        if (this.mBound) {
            if (this.mSession != null) {
                try {
                    this.mSession.destroy();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Voice interation session already dead");
                }
            }
            if (finishTask && this.mSession != null) {
                try {
                    this.mAm.finishVoiceTask(this.mSession);
                } catch (RemoteException e2) {
                }
            }
            try {
                this.mContext.unbindService(this);
            } catch (IllegalArgumentException e3) {
                Slog.w(TAG, "Service not registered: com.android.server.voiceinteraction.VoiceInteractionSessionConnection", e3);
            }
            try {
                this.mIWindowManager.removeWindowToken(this.mToken, 0);
            } catch (RemoteException e4) {
                Slog.w(TAG, "Failed removing window token", e4);
            }
            this.mBound = false;
            this.mService = null;
            this.mSession = null;
            this.mInteractor = null;
        }
        if (this.mFullyBound) {
            this.mContext.unbindService(this.mFullConnection);
            this.mFullyBound = false;
        }
    }

    public boolean deliverNewSessionLocked(IVoiceInteractionSession session, IVoiceInteractor interactor) {
        this.mSession = session;
        this.mInteractor = interactor;
        if (this.mShown) {
            try {
                session.show(this.mShowArgs, this.mShowFlags, this.mShowCallback);
                this.mShowArgs = null;
                this.mShowFlags = 0;
            } catch (RemoteException e) {
            }
            this.mAssistDataRequester.processPendingAssistData();
        }
        return true;
    }

    private void notifyPendingShowCallbacksShownLocked() {
        for (int i = 0; i < this.mPendingShowCallbacks.size(); i++) {
            try {
                ((IVoiceInteractionSessionShowCallback) this.mPendingShowCallbacks.get(i)).onShown();
            } catch (RemoteException e) {
            }
        }
        this.mPendingShowCallbacks.clear();
    }

    private void notifyPendingShowCallbacksFailedLocked() {
        for (int i = 0; i < this.mPendingShowCallbacks.size(); i++) {
            try {
                ((IVoiceInteractionSessionShowCallback) this.mPendingShowCallbacks.get(i)).onFailed();
            } catch (RemoteException e) {
            }
        }
        this.mPendingShowCallbacks.clear();
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (this.mLock) {
            this.mService = IVoiceInteractionSessionService.Stub.asInterface(service);
            if (!this.mCanceled) {
                try {
                    this.mService.newSession(this.mToken, this.mShowArgs, this.mShowFlags);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed adding window token", e);
                }
            }
        }
    }

    public void onServiceDisconnected(ComponentName name) {
        this.mCallback.sessionConnectionGone(this);
        synchronized (this.mLock) {
            this.mService = null;
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mToken=");
        pw.println(this.mToken);
        pw.print(prefix);
        pw.print("mShown=");
        pw.println(this.mShown);
        pw.print(prefix);
        pw.print("mShowArgs=");
        pw.println(this.mShowArgs);
        pw.print(prefix);
        pw.print("mShowFlags=0x");
        pw.println(Integer.toHexString(this.mShowFlags));
        pw.print(prefix);
        pw.print("mBound=");
        pw.println(this.mBound);
        if (this.mBound) {
            pw.print(prefix);
            pw.print("mService=");
            pw.println(this.mService);
            pw.print(prefix);
            pw.print("mSession=");
            pw.println(this.mSession);
            pw.print(prefix);
            pw.print("mInteractor=");
            pw.println(this.mInteractor);
        }
        this.mAssistDataRequester.dump(prefix, pw);
    }
}
