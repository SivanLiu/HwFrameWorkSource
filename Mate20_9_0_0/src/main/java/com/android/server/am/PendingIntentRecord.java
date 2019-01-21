package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.IIntentReceiver;
import android.content.IIntentSender.Stub;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.os.IResultReceiver;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Objects;

final class PendingIntentRecord extends Stub {
    private static final String TAG = "ActivityManager";
    boolean canceled = false;
    final Key key;
    String lastTag;
    String lastTagPrefix;
    private RemoteCallbackList<IResultReceiver> mCancelCallbacks;
    final ActivityManagerService owner;
    final WeakReference<PendingIntentRecord> ref;
    boolean sent = false;
    String stringName;
    final int uid;
    private ArrayMap<IBinder, Long> whitelistDuration;

    static final class Key {
        private static final int ODD_PRIME_NUMBER = 37;
        final ActivityRecord activity;
        Intent[] allIntents;
        String[] allResolvedTypes;
        final int flags;
        final int hashCode;
        final SafeActivityOptions options;
        final String packageName;
        final int requestCode;
        final Intent requestIntent;
        final String requestResolvedType;
        final int type;
        final int userId;
        final String who;

        Key(int _t, String _p, ActivityRecord _a, String _w, int _r, Intent[] _i, String[] _it, int _f, SafeActivityOptions _o, int _userId) {
            this.type = _t;
            this.packageName = _p;
            this.activity = _a;
            this.who = _w;
            this.requestCode = _r;
            String str = null;
            this.requestIntent = _i != null ? _i[_i.length - 1] : null;
            if (_it != null) {
                str = _it[_it.length - 1];
            }
            this.requestResolvedType = str;
            this.allIntents = _i;
            this.allResolvedTypes = _it;
            this.flags = _f;
            this.options = _o;
            this.userId = _userId;
            int hash = (37 * ((37 * ((37 * 23) + _f)) + _r)) + _userId;
            if (_w != null) {
                hash = (37 * hash) + _w.hashCode();
            }
            if (_a != null) {
                hash = (37 * hash) + _a.hashCode();
            }
            if (this.requestIntent != null) {
                hash = (37 * hash) + this.requestIntent.filterHashCode();
            }
            if (this.requestResolvedType != null) {
                hash = (37 * hash) + this.requestResolvedType.hashCode();
            }
            this.hashCode = (37 * ((37 * hash) + (_p != null ? _p.hashCode() : 0))) + _t;
        }

        public boolean equals(Object otherObj) {
            if (otherObj == null) {
                return false;
            }
            try {
                Key other = (Key) otherObj;
                if (this.type != other.type || this.userId != other.userId || !Objects.equals(this.packageName, other.packageName) || this.activity != other.activity || !Objects.equals(this.who, other.who) || this.requestCode != other.requestCode) {
                    return false;
                }
                if (this.requestIntent != other.requestIntent) {
                    if (this.requestIntent != null) {
                        if (!(this.requestIntent.filterEquals(other.requestIntent) && this.requestIntent.getHwFlags() == other.requestIntent.getHwFlags())) {
                            return false;
                        }
                    } else if (other.requestIntent != null) {
                        return false;
                    }
                }
                if (Objects.equals(this.requestResolvedType, other.requestResolvedType) && this.flags == other.flags) {
                    return true;
                }
                return false;
            } catch (ClassCastException e) {
                return false;
            }
        }

        public int hashCode() {
            return this.hashCode;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Key{");
            stringBuilder.append(typeName());
            stringBuilder.append(" pkg=");
            stringBuilder.append(this.packageName);
            stringBuilder.append(" intent=");
            stringBuilder.append(this.requestIntent != null ? this.requestIntent.toShortString(true, true, false, false) : "<null>");
            stringBuilder.append(" flags=0x");
            stringBuilder.append(Integer.toHexString(this.flags));
            stringBuilder.append(" u=");
            stringBuilder.append(this.userId);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        String typeName() {
            switch (this.type) {
                case 1:
                    return "broadcastIntent";
                case 2:
                    return "startActivity";
                case 3:
                    return "activityResult";
                case 4:
                    return "startService";
                case 5:
                    return "startForegroundService";
                default:
                    return Integer.toString(this.type);
            }
        }
    }

    PendingIntentRecord(ActivityManagerService _owner, Key _k, int _u) {
        this.owner = _owner;
        this.key = _k;
        this.uid = _u;
        this.ref = new WeakReference(this);
    }

    void setWhitelistDurationLocked(IBinder whitelistToken, long duration) {
        if (duration > 0) {
            if (this.whitelistDuration == null) {
                this.whitelistDuration = new ArrayMap();
            }
            this.whitelistDuration.put(whitelistToken, Long.valueOf(duration));
        } else if (this.whitelistDuration != null) {
            this.whitelistDuration.remove(whitelistToken);
            if (this.whitelistDuration.size() <= 0) {
                this.whitelistDuration = null;
            }
        }
        this.stringName = null;
    }

    public void registerCancelListenerLocked(IResultReceiver receiver) {
        if (this.mCancelCallbacks == null) {
            this.mCancelCallbacks = new RemoteCallbackList();
        }
        this.mCancelCallbacks.register(receiver);
    }

    public void unregisterCancelListenerLocked(IResultReceiver receiver) {
        if (this.mCancelCallbacks != null) {
            this.mCancelCallbacks.unregister(receiver);
            if (this.mCancelCallbacks.getRegisteredCallbackCount() <= 0) {
                this.mCancelCallbacks = null;
            }
        }
    }

    public RemoteCallbackList<IResultReceiver> detachCancelListenersLocked() {
        RemoteCallbackList<IResultReceiver> listeners = this.mCancelCallbacks;
        this.mCancelCallbacks = null;
        return listeners;
    }

    public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
        sendInner(code, intent, resolvedType, whitelistToken, finishedReceiver, requiredPermission, null, null, 0, 0, 0, options);
    }

    public int sendWithResult(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
        return sendInner(code, intent, resolvedType, whitelistToken, finishedReceiver, requiredPermission, null, null, 0, 0, 0, options);
    }

    /* JADX WARNING: Removed duplicated region for block: B:149:0x02de  */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x02de  */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x02de  */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x02de  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int sendInner(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, IBinder resultTo, String resultWho, int requestCode, int flagsMask, int flagsValues, Bundle options) {
        int i;
        Throwable th;
        int flagsMask2;
        long origId;
        RuntimeException e;
        Intent intent2 = intent;
        Bundle bundle = options;
        boolean z = true;
        if (intent2 != null) {
            intent2.setDefusable(true);
        }
        if (bundle != null) {
            bundle.setDefusable(true);
        }
        ActivityManagerService activityManagerService = this.owner;
        synchronized (activityManagerService) {
            ActivityManagerService activityManagerService2;
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.canceled) {
                    i = flagsMask;
                    activityManagerService2 = activityManagerService;
                    try {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return -96;
                    } catch (Throwable th2) {
                        th = th2;
                        flagsMask2 = i;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                String resolvedType2;
                int flagsValues2;
                this.sent = true;
                if ((this.key.flags & 1073741824) != 0) {
                    try {
                        this.owner.cancelIntentSenderLocked(this, true);
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                Intent finalIntent = this.key.requestIntent != null ? new Intent(this.key.requestIntent) : new Intent();
                int i2;
                if ((this.key.flags & 67108864) != 0) {
                    i = flagsMask;
                    try {
                        i2 = flagsValues;
                        resolvedType2 = this.key.requestResolvedType;
                        flagsMask2 = i;
                    } catch (Throwable th4) {
                        th = th4;
                        activityManagerService2 = activityManagerService;
                        flagsMask2 = i;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                String resolvedType3;
                if (intent2 == null) {
                    try {
                        resolvedType3 = this.key.requestResolvedType;
                    } catch (Throwable th5) {
                        th = th5;
                        resolvedType2 = resolvedType;
                        i2 = flagsValues;
                        flagsMask2 = flagsMask;
                        activityManagerService2 = activityManagerService;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } else if ((finalIntent.fillIn(intent2, this.key.flags) & 2) == 0) {
                    resolvedType3 = this.key.requestResolvedType;
                } else {
                    resolvedType3 = resolvedType;
                }
                i = flagsMask & -196;
                flagsValues2 = flagsValues & i;
                try {
                    finalIntent.setFlags((finalIntent.getFlags() & (~i)) | flagsValues2);
                    resolvedType2 = resolvedType3;
                    flagsMask2 = i;
                    i2 = flagsValues2;
                } catch (Throwable th6) {
                    th = th6;
                    resolvedType2 = resolvedType3;
                    flagsMask2 = i;
                    i2 = flagsValues2;
                    activityManagerService2 = activityManagerService;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
                IBinder iBinder;
                try {
                    Intent finalIntent2;
                    finalIntent.addHwFlags(256);
                    int callingUid = Binder.getCallingUid();
                    int callingPid = Binder.getCallingPid();
                    SafeActivityOptions mergedOptions = this.key.options;
                    if (mergedOptions == null) {
                        mergedOptions = SafeActivityOptions.fromBundle(options);
                    } else {
                        mergedOptions.setCallerOptions(ActivityOptions.fromBundle(options));
                    }
                    SafeActivityOptions mergedOptions2 = mergedOptions;
                    long origId2 = Binder.clearCallingIdentity();
                    if (this.whitelistDuration != null) {
                        Long duration = (Long) this.whitelistDuration.get(whitelistToken);
                        if (duration != null) {
                            i = this.owner.getUidState(callingUid);
                            if (ActivityManager.isProcStateBackground(i)) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Not doing whitelist ");
                                stringBuilder.append(this);
                                stringBuilder.append(": caller state=");
                                stringBuilder.append(i);
                                Slog.w("ActivityManager", stringBuilder.toString());
                            } else {
                                StringBuilder tag = new StringBuilder(64);
                                tag.append("pendingintent:");
                                UserHandle.formatUid(tag, callingUid);
                                tag.append(":");
                                if (finalIntent.getAction() != null) {
                                    tag.append(finalIntent.getAction());
                                } else if (finalIntent.getComponent() != null) {
                                    finalIntent.getComponent().appendShortString(tag);
                                } else if (finalIntent.getData() != null) {
                                    tag.append(finalIntent.getData());
                                }
                                this.owner.tempWhitelistForPendingIntentLocked(callingPid, callingUid, this.uid, duration.longValue(), tag.toString());
                            }
                        }
                    } else {
                        iBinder = whitelistToken;
                    }
                    boolean sendFinish = finishedReceiver != null;
                    int userId = this.key.userId;
                    if (userId == -2) {
                        userId = this.owner.mUserController.getCurrentOrTargetUserId();
                    }
                    int userId2 = userId;
                    int res = 0;
                    ActivityManagerService activityManagerService3;
                    switch (this.key.type) {
                        case 1:
                            origId = origId2;
                            int i3;
                            try {
                                activityManagerService3 = this.owner;
                                String str = this.key.packageName;
                                flagsValues2 = this.uid;
                                if (finishedReceiver == null) {
                                    z = false;
                                }
                                i3 = callingUid;
                                finalIntent2 = finalIntent;
                                activityManagerService2 = activityManagerService;
                                try {
                                    if (activityManagerService3.broadcastIntentInPackage(str, flagsValues2, finalIntent, resolvedType2, finishedReceiver, code, null, null, requiredPermission, bundle, z, false, userId2) == 0) {
                                        sendFinish = false;
                                    }
                                } catch (RuntimeException e2) {
                                    e = e2;
                                    try {
                                        Slog.w("ActivityManager", "Unable to send startActivity intent", e);
                                        callingUid = res;
                                        if (sendFinish) {
                                        }
                                        Binder.restoreCallingIdentity(origId);
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                        return callingUid;
                                    } catch (Throwable th7) {
                                        th = th7;
                                        break;
                                    }
                                }
                            } catch (RuntimeException e3) {
                                e = e3;
                                i3 = callingUid;
                                finalIntent2 = finalIntent;
                                activityManagerService2 = activityManagerService;
                                Slog.w("ActivityManager", "Unable to send startActivity intent", e);
                                callingUid = res;
                                if (sendFinish) {
                                    break;
                                }
                                Binder.restoreCallingIdentity(origId);
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                return callingUid;
                            }
                        case 2:
                            origId = origId2;
                            try {
                                int startActivityInPackage;
                                if (this.key.allIntents == null || this.key.allIntents.length <= 1) {
                                    startActivityInPackage = this.owner.getActivityStartController().startActivityInPackage(this.uid, callingPid, callingUid, this.key.packageName, finalIntent, resolvedType2, resultTo, resultWho, requestCode, 0, mergedOptions2, userId2, null, "PendingIntentRecord", false);
                                } else {
                                    Intent[] allIntents = new Intent[this.key.allIntents.length];
                                    String[] allResolvedTypes = new String[this.key.allIntents.length];
                                    System.arraycopy(this.key.allIntents, 0, allIntents, 0, this.key.allIntents.length);
                                    if (this.key.allResolvedTypes != null) {
                                        System.arraycopy(this.key.allResolvedTypes, 0, allResolvedTypes, 0, this.key.allResolvedTypes.length);
                                    }
                                    allIntents[allIntents.length - 1] = finalIntent;
                                    allResolvedTypes[allResolvedTypes.length - 1] = resolvedType2;
                                    startActivityInPackage = this.owner.getActivityStartController().startActivitiesInPackage(this.uid, this.key.packageName, allIntents, allResolvedTypes, resultTo, mergedOptions2, userId2, false);
                                }
                                res = startActivityInPackage;
                                break;
                            } catch (RuntimeException e4) {
                                Slog.w("ActivityManager", "Unable to send startActivity intent", e4);
                                break;
                            }
                        case 3:
                            origId = origId2;
                            ActivityStack stack = this.key.activity.getStack();
                            if (stack != null) {
                                stack.sendActivityResultLocked(-1, this.key.activity, this.key.who, this.key.requestCode, code, finalIntent);
                                break;
                            }
                            break;
                        case 4:
                        case 5:
                            try {
                                activityManagerService3 = this.owner;
                                i = this.uid;
                                if (this.key.type != 5) {
                                    z = false;
                                }
                                origId = origId2;
                                try {
                                    activityManagerService3.startServiceInPackage(i, finalIntent, resolvedType2, z, this.key.packageName, userId2);
                                } catch (RuntimeException e5) {
                                    e4 = e5;
                                } catch (TransactionTooLargeException e6) {
                                    res = -96;
                                    finalIntent2 = finalIntent;
                                    activityManagerService2 = activityManagerService;
                                    callingUid = res;
                                    if (sendFinish) {
                                    }
                                    Binder.restoreCallingIdentity(origId);
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    return callingUid;
                                }
                            } catch (RuntimeException e7) {
                                e4 = e7;
                                origId = origId2;
                                Slog.w("ActivityManager", "Unable to send startService intent", e4);
                                finalIntent2 = finalIntent;
                                activityManagerService2 = activityManagerService;
                                callingUid = res;
                                if (sendFinish) {
                                }
                                Binder.restoreCallingIdentity(origId);
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                return callingUid;
                            } catch (TransactionTooLargeException e8) {
                                origId = origId2;
                                res = -96;
                                finalIntent2 = finalIntent;
                                activityManagerService2 = activityManagerService;
                                callingUid = res;
                                if (sendFinish) {
                                }
                                Binder.restoreCallingIdentity(origId);
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                return callingUid;
                            }
                            break;
                        default:
                            origId = origId2;
                            finalIntent2 = finalIntent;
                            activityManagerService2 = activityManagerService;
                    }
                    finalIntent2 = finalIntent;
                    activityManagerService2 = activityManagerService;
                    callingUid = res;
                    if (sendFinish || callingUid == -96) {
                    } else {
                        try {
                            try {
                                finishedReceiver.performReceive(new Intent(finalIntent2), 0, null, null, false, false, this.key.userId);
                            } catch (RemoteException e9) {
                            }
                        } catch (RemoteException e10) {
                            finalIntent = finalIntent2;
                        }
                    }
                    Binder.restoreCallingIdentity(origId);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return callingUid;
                } catch (Throwable th8) {
                    th = th8;
                }
            } catch (Throwable th9) {
                th = th9;
                i = flagsMask;
                activityManagerService2 = activityManagerService;
                flagsMask2 = i;
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (!this.canceled) {
                this.owner.mHandler.sendMessage(this.owner.mHandler.obtainMessage(23, this));
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    public void completeFinalize() {
        synchronized (this.owner) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (((WeakReference) this.owner.mIntentSenderRecords.get(this.key)) == this.ref) {
                    this.owner.mIntentSenderRecords.remove(this.key);
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    void dump(PrintWriter pw, String prefix) {
        int i;
        pw.print(prefix);
        pw.print("uid=");
        pw.print(this.uid);
        pw.print(" packageName=");
        pw.print(this.key.packageName);
        pw.print(" type=");
        pw.print(this.key.typeName());
        pw.print(" flags=0x");
        pw.println(Integer.toHexString(this.key.flags));
        if (!(this.key.activity == null && this.key.who == null)) {
            pw.print(prefix);
            pw.print("activity=");
            pw.print(this.key.activity);
            pw.print(" who=");
            pw.println(this.key.who);
        }
        if (!(this.key.requestCode == 0 && this.key.requestResolvedType == null)) {
            pw.print(prefix);
            pw.print("requestCode=");
            pw.print(this.key.requestCode);
            pw.print(" requestResolvedType=");
            pw.println(this.key.requestResolvedType);
        }
        if (this.key.requestIntent != null) {
            pw.print(prefix);
            pw.print("requestIntent=");
            pw.println(this.key.requestIntent.toShortString(true, true, true, true));
        }
        if (this.sent || this.canceled) {
            pw.print(prefix);
            pw.print("sent=");
            pw.print(this.sent);
            pw.print(" canceled=");
            pw.println(this.canceled);
        }
        int i2 = 0;
        if (this.whitelistDuration != null) {
            pw.print(prefix);
            pw.print("whitelistDuration=");
            for (i = 0; i < this.whitelistDuration.size(); i++) {
                if (i != 0) {
                    pw.print(", ");
                }
                pw.print(Integer.toHexString(System.identityHashCode(this.whitelistDuration.keyAt(i))));
                pw.print(":");
                TimeUtils.formatDuration(((Long) this.whitelistDuration.valueAt(i)).longValue(), pw);
            }
            pw.println();
        }
        if (this.mCancelCallbacks != null) {
            pw.print(prefix);
            pw.println("mCancelCallbacks:");
            while (true) {
                i = i2;
                if (i < this.mCancelCallbacks.getRegisteredCallbackCount()) {
                    pw.print(prefix);
                    pw.print("  #");
                    pw.print(i);
                    pw.print(": ");
                    pw.println(this.mCancelCallbacks.getRegisteredCallbackItem(i));
                    i2 = i + 1;
                } else {
                    return;
                }
            }
        }
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("PendingIntentRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.key.packageName);
        sb.append(' ');
        sb.append(this.key.typeName());
        if (this.whitelistDuration != null) {
            sb.append(" (whitelist: ");
            for (int i = 0; i < this.whitelistDuration.size(); i++) {
                if (i != 0) {
                    sb.append(",");
                }
                sb.append(Integer.toHexString(System.identityHashCode(this.whitelistDuration.keyAt(i))));
                sb.append(":");
                TimeUtils.formatDuration(((Long) this.whitelistDuration.valueAt(i)).longValue(), sb);
            }
            sb.append(")");
        }
        sb.append('}');
        String stringBuilder = sb.toString();
        this.stringName = stringBuilder;
        return stringBuilder;
    }
}
