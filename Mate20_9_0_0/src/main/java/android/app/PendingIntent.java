package android.app;

import android.content.Context;
import android.content.IIntentReceiver.Stub;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.AndroidException;
import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import com.android.internal.os.IResultReceiver;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class PendingIntent implements Parcelable {
    public static final Creator<PendingIntent> CREATOR = new Creator<PendingIntent>() {
        public PendingIntent createFromParcel(Parcel in) {
            IBinder target = in.readStrongBinder();
            if (target != null) {
                return new PendingIntent(target, in.getClassCookie(PendingIntent.class));
            }
            return null;
        }

        public PendingIntent[] newArray(int size) {
            return new PendingIntent[size];
        }
    };
    public static final int FLAG_CANCEL_CURRENT = 268435456;
    public static final int FLAG_IMMUTABLE = 67108864;
    public static final int FLAG_NO_CREATE = 536870912;
    public static final int FLAG_ONE_SHOT = 1073741824;
    public static final int FLAG_UPDATE_CURRENT = 134217728;
    static final String TAG = "PendingIntent";
    private static final ThreadLocal<OnMarshaledListener> sOnMarshaledListener = new ThreadLocal();
    private ArraySet<CancelListener> mCancelListeners;
    private IResultReceiver mCancelReceiver;
    private final IIntentSender mTarget;
    private IBinder mWhitelistToken;

    public interface CancelListener {
        void onCancelled(PendingIntent pendingIntent);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    public interface OnFinished {
        void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String str, Bundle bundle);
    }

    public interface OnMarshaledListener {
        void onMarshaled(PendingIntent pendingIntent, Parcel parcel, int i);
    }

    public static class CanceledException extends AndroidException {
        public CanceledException(String name) {
            super(name);
        }

        public CanceledException(Exception cause) {
            super(cause);
        }
    }

    private static class FinishedDispatcher extends Stub implements Runnable {
        private static Handler sDefaultSystemHandler;
        private final Handler mHandler;
        private Intent mIntent;
        private final PendingIntent mPendingIntent;
        private int mResultCode;
        private String mResultData;
        private Bundle mResultExtras;
        private final OnFinished mWho;

        FinishedDispatcher(PendingIntent pi, OnFinished who, Handler handler) {
            this.mPendingIntent = pi;
            this.mWho = who;
            if (handler == null && ActivityThread.isSystem()) {
                if (sDefaultSystemHandler == null) {
                    sDefaultSystemHandler = new Handler(Looper.getMainLooper());
                }
                this.mHandler = sDefaultSystemHandler;
                return;
            }
            this.mHandler = handler;
        }

        public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean serialized, boolean sticky, int sendingUser) {
            this.mIntent = intent;
            this.mResultCode = resultCode;
            this.mResultData = data;
            this.mResultExtras = extras;
            if (this.mHandler == null) {
                run();
            } else {
                this.mHandler.post(this);
            }
        }

        public void run() {
            this.mWho.onSendFinished(this.mPendingIntent, this.mIntent, this.mResultCode, this.mResultData, this.mResultExtras);
        }
    }

    public static void setOnMarshaledListener(OnMarshaledListener listener) {
        sOnMarshaledListener.set(listener);
    }

    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags) {
        return getActivity(context, requestCode, intent, flags, null);
    }

    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags, Bundle options) {
        String resolveTypeIfNeeded;
        RemoteException e;
        Intent intent2 = intent;
        String packageName = context.getPackageName();
        if (intent2 != null) {
            resolveTypeIfNeeded = intent2.resolveTypeIfNeeded(context.getContentResolver());
        } else {
            resolveTypeIfNeeded = null;
        }
        String resolvedType = resolveTypeIfNeeded;
        try {
            intent.migrateExtraStreamToClipData();
            try {
                String[] strArr;
                intent2.prepareToLeaveProcess(context);
                IIntentSender target = ActivityManager.getService();
                Intent[] intentArr = new Intent[]{intent2};
                if (resolvedType != null) {
                    strArr = new String[]{resolvedType};
                } else {
                    strArr = null;
                }
                target = target.getIntentSender(2, packageName, null, null, requestCode, intentArr, strArr, flags, options, context.getUserId());
                if (target != null) {
                    return new PendingIntent(target);
                }
                return null;
            } catch (RemoteException e2) {
                e = e2;
                throw e.rethrowFromSystemServer();
            }
        } catch (RemoteException e3) {
            e = e3;
            Context context2 = context;
            throw e.rethrowFromSystemServer();
        }
    }

    public static PendingIntent getActivityAsUser(Context context, int requestCode, Intent intent, int flags, Bundle options, UserHandle user) {
        String resolveTypeIfNeeded;
        RemoteException e;
        Intent intent2 = intent;
        String packageName = context.getPackageName();
        if (intent2 != null) {
            resolveTypeIfNeeded = intent2.resolveTypeIfNeeded(context.getContentResolver());
        } else {
            resolveTypeIfNeeded = null;
        }
        String resolvedType = resolveTypeIfNeeded;
        try {
            intent.migrateExtraStreamToClipData();
            try {
                String[] strArr;
                intent2.prepareToLeaveProcess(context);
                IIntentSender target = ActivityManager.getService();
                Intent[] intentArr = new Intent[]{intent2};
                if (resolvedType != null) {
                    strArr = new String[]{resolvedType};
                } else {
                    strArr = null;
                }
                target = target.getIntentSender(2, packageName, null, null, requestCode, intentArr, strArr, flags, options, user.getIdentifier());
                if (target != null) {
                    return new PendingIntent(target);
                }
                return null;
            } catch (RemoteException e2) {
                e = e2;
                throw e.rethrowFromSystemServer();
            }
        } catch (RemoteException e3) {
            e = e3;
            Context context2 = context;
            throw e.rethrowFromSystemServer();
        }
    }

    public static PendingIntent getActivities(Context context, int requestCode, Intent[] intents, int flags) {
        return getActivities(context, requestCode, intents, flags, null);
    }

    public static PendingIntent getActivities(Context context, int requestCode, Intent[] intents, int flags, Bundle options) {
        Intent[] intentArr = intents;
        String packageName = context.getPackageName();
        String[] resolvedTypes = new String[intentArr.length];
        for (int i = 0; i < intentArr.length; i++) {
            intentArr[i].migrateExtraStreamToClipData();
            intentArr[i].prepareToLeaveProcess(context);
            resolvedTypes[i] = intentArr[i].resolveTypeIfNeeded(context.getContentResolver());
        }
        Context context2 = context;
        try {
            IIntentSender target = ActivityManager.getService().getIntentSender(2, packageName, null, null, requestCode, intentArr, resolvedTypes, flags, options, context.getUserId());
            return target != null ? new PendingIntent(target) : null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static PendingIntent getActivitiesAsUser(Context context, int requestCode, Intent[] intents, int flags, Bundle options, UserHandle user) {
        Intent[] intentArr = intents;
        String packageName = context.getPackageName();
        String[] resolvedTypes = new String[intentArr.length];
        for (int i = 0; i < intentArr.length; i++) {
            intentArr[i].migrateExtraStreamToClipData();
            intentArr[i].prepareToLeaveProcess(context);
            resolvedTypes[i] = intentArr[i].resolveTypeIfNeeded(context.getContentResolver());
        }
        Context context2 = context;
        try {
            IIntentSender target = ActivityManager.getService().getIntentSender(2, packageName, null, null, requestCode, intentArr, resolvedTypes, flags, options, user.getIdentifier());
            return target != null ? new PendingIntent(target) : null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags) {
        return getBroadcastAsUser(context, requestCode, intent, flags, context.getUser());
    }

    public static PendingIntent getBroadcastAsUser(Context context, int requestCode, Intent intent, int flags, UserHandle userHandle) {
        String resolveTypeIfNeeded;
        Intent intent2 = intent;
        String packageName = context.getPackageName();
        if (intent2 != null) {
            resolveTypeIfNeeded = intent2.resolveTypeIfNeeded(context.getContentResolver());
        } else {
            resolveTypeIfNeeded = null;
        }
        String resolvedType = resolveTypeIfNeeded;
        try {
            String[] strArr;
            intent2.prepareToLeaveProcess(context);
            IIntentSender target = ActivityManager.getService();
            Intent[] intentArr = new Intent[]{intent2};
            if (resolvedType != null) {
                strArr = new String[]{resolvedType};
            } else {
                strArr = null;
            }
            target = target.getIntentSender(1, packageName, null, null, requestCode, intentArr, strArr, flags, null, userHandle.getIdentifier());
            if (target != null) {
                return new PendingIntent(target);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags) {
        return buildServicePendingIntent(context, requestCode, intent, flags, 4);
    }

    public static PendingIntent getForegroundService(Context context, int requestCode, Intent intent, int flags) {
        return buildServicePendingIntent(context, requestCode, intent, flags, 5);
    }

    private static PendingIntent buildServicePendingIntent(Context context, int requestCode, Intent intent, int flags, int serviceKind) {
        String resolveTypeIfNeeded;
        Intent intent2 = intent;
        String packageName = context.getPackageName();
        if (intent2 != null) {
            resolveTypeIfNeeded = intent2.resolveTypeIfNeeded(context.getContentResolver());
        } else {
            resolveTypeIfNeeded = null;
        }
        String resolvedType = resolveTypeIfNeeded;
        try {
            String[] strArr;
            intent2.prepareToLeaveProcess(context);
            IIntentSender target = ActivityManager.getService();
            Intent[] intentArr = new Intent[]{intent2};
            if (resolvedType != null) {
                strArr = new String[]{resolvedType};
            } else {
                strArr = null;
            }
            target = target.getIntentSender(serviceKind, packageName, null, null, requestCode, intentArr, strArr, flags, null, context.getUserId());
            if (target != null) {
                return new PendingIntent(target);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public IntentSender getIntentSender() {
        return new IntentSender(this.mTarget, this.mWhitelistToken);
    }

    public void cancel() {
        try {
            ActivityManager.getService().cancelIntentSender(this.mTarget);
        } catch (RemoteException e) {
        }
    }

    public void send() throws CanceledException {
        send(null, 0, null, null, null, null, null);
    }

    public void send(int code) throws CanceledException {
        send(null, code, null, null, null, null, null);
    }

    public void send(Context context, int code, Intent intent) throws CanceledException {
        send(context, code, intent, null, null, null, null);
    }

    public void send(int code, OnFinished onFinished, Handler handler) throws CanceledException {
        send(null, code, null, onFinished, handler, null, null);
    }

    public void send(Context context, int code, Intent intent, OnFinished onFinished, Handler handler) throws CanceledException {
        send(context, code, intent, onFinished, handler, null, null);
    }

    public void send(Context context, int code, Intent intent, OnFinished onFinished, Handler handler, String requiredPermission) throws CanceledException {
        send(context, code, intent, onFinished, handler, requiredPermission, null);
    }

    public void send(Context context, int code, Intent intent, OnFinished onFinished, Handler handler, String requiredPermission, Bundle options) throws CanceledException {
        if (sendAndReturnResult(context, code, intent, onFinished, handler, requiredPermission, options) < 0) {
            throw new CanceledException();
        }
    }

    public int sendAndReturnResult(Context context, int code, Intent intent, OnFinished onFinished, Handler handler, String requiredPermission, Bundle options) throws CanceledException {
        String resolvedType;
        Exception e;
        Handler handler2;
        Intent intent2 = intent;
        OnFinished onFinished2 = onFinished;
        FinishedDispatcher finishedDispatcher = null;
        if (intent2 != null) {
            try {
                resolvedType = intent2.resolveTypeIfNeeded(context.getContentResolver());
            } catch (RemoteException e2) {
                e = e2;
                handler2 = handler;
                throw new CanceledException(e);
            }
        }
        resolvedType = null;
        IActivityManager service = ActivityManager.getService();
        IIntentSender iIntentSender = this.mTarget;
        IBinder iBinder = this.mWhitelistToken;
        if (onFinished2 != null) {
            try {
                finishedDispatcher = new FinishedDispatcher(this, onFinished2, handler);
            } catch (RemoteException e3) {
                e = e3;
                throw new CanceledException(e);
            }
        }
        handler2 = handler;
        return service.sendIntentSender(iIntentSender, iBinder, code, intent2, resolvedType, finishedDispatcher, requiredPermission, options);
    }

    @Deprecated
    public String getTargetPackage() {
        try {
            return ActivityManager.getService().getPackageForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getCreatorPackage() {
        try {
            return ActivityManager.getService().getPackageForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getCreatorUid() {
        try {
            return ActivityManager.getService().getUidForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerCancelListener(CancelListener cancelListener) {
        synchronized (this) {
            if (this.mCancelReceiver == null) {
                this.mCancelReceiver = new IResultReceiver.Stub() {
                    public void send(int resultCode, Bundle resultData) throws RemoteException {
                        PendingIntent.this.notifyCancelListeners();
                    }
                };
            }
            if (this.mCancelListeners == null) {
                this.mCancelListeners = new ArraySet();
            }
            boolean wasEmpty = this.mCancelListeners.isEmpty();
            this.mCancelListeners.add(cancelListener);
            if (wasEmpty) {
                try {
                    ActivityManager.getService().registerIntentSenderCancelListener(this.mTarget, this.mCancelReceiver);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    private void notifyCancelListeners() {
        ArraySet<CancelListener> cancelListeners;
        synchronized (this) {
            cancelListeners = new ArraySet(this.mCancelListeners);
        }
        int size = cancelListeners.size();
        for (int i = 0; i < size; i++) {
            ((CancelListener) cancelListeners.valueAt(i)).onCancelled(this);
        }
    }

    /* JADX WARNING: Missing block: B:16:0x002f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void unregisterCancelListener(CancelListener cancelListener) {
        synchronized (this) {
            if (this.mCancelListeners == null) {
                return;
            }
            boolean wasEmpty = this.mCancelListeners.isEmpty();
            this.mCancelListeners.remove(cancelListener);
            if (this.mCancelListeners.isEmpty() && !wasEmpty) {
                try {
                    ActivityManager.getService().unregisterIntentSenderCancelListener(this.mTarget, this.mCancelReceiver);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    public UserHandle getCreatorUserHandle() {
        try {
            int uid = ActivityManager.getService().getUidForIntentSender(this.mTarget);
            return uid > 0 ? new UserHandle(UserHandle.getUserId(uid)) : null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isTargetedToPackage() {
        try {
            return ActivityManager.getService().isIntentSenderTargetedToPackage(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isActivity() {
        try {
            return ActivityManager.getService().isIntentSenderAnActivity(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isForegroundService() {
        try {
            return ActivityManager.getService().isIntentSenderAForegroundService(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Intent getIntent() {
        try {
            return ActivityManager.getService().getIntentForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getTag(String prefix) {
        try {
            return ActivityManager.getService().getTagForIntentSender(this.mTarget, prefix);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean equals(Object otherObj) {
        if (otherObj instanceof PendingIntent) {
            return this.mTarget.asBinder().equals(((PendingIntent) otherObj).mTarget.asBinder());
        }
        return false;
    }

    public int hashCode() {
        return this.mTarget.asBinder().hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("PendingIntent{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(": ");
        sb.append(this.mTarget != null ? this.mTarget.asBinder() : null);
        sb.append('}');
        return sb.toString();
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        if (this.mTarget != null) {
            proto.write(1138166333441L, this.mTarget.asBinder().toString());
        }
        proto.end(token);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeStrongBinder(this.mTarget.asBinder());
        OnMarshaledListener listener = (OnMarshaledListener) sOnMarshaledListener.get();
        if (listener != null) {
            listener.onMarshaled(this, out, flags);
        }
    }

    public static void writePendingIntentOrNullToParcel(PendingIntent sender, Parcel out) {
        out.writeStrongBinder(sender != null ? sender.mTarget.asBinder() : null);
        if (sender != null) {
            OnMarshaledListener listener = (OnMarshaledListener) sOnMarshaledListener.get();
            if (listener != null) {
                listener.onMarshaled(sender, out, 0);
            }
        }
    }

    public static PendingIntent readPendingIntentOrNullFromParcel(Parcel in) {
        IBinder b = in.readStrongBinder();
        return b != null ? new PendingIntent(b, in.getClassCookie(PendingIntent.class)) : null;
    }

    PendingIntent(IIntentSender target) {
        this.mTarget = target;
    }

    PendingIntent(IBinder target, Object cookie) {
        this.mTarget = IIntentSender.Stub.asInterface(target);
        if (cookie != null) {
            this.mWhitelistToken = (IBinder) cookie;
        }
    }

    public IIntentSender getTarget() {
        return this.mTarget;
    }

    public IBinder getWhitelistToken() {
        return this.mWhitelistToken;
    }
}
