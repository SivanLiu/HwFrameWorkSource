package com.android.server.am;

import android.app.ActivityOptions;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.os.IResultReceiver;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.pm.DumpState;
import com.android.server.wm.SafeActivityOptions;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Objects;

public final class PendingIntentRecord extends IIntentSender.Stub {
    public static final int FLAG_ACTIVITY_SENDER = 1;
    public static final int FLAG_BROADCAST_SENDER = 2;
    public static final int FLAG_SERVICE_SENDER = 4;
    private static final int GET_CONTROLLER_LOCK_TIMEOUT = 5000;
    private static final String TAG = "ActivityManager";
    boolean canceled = false;
    final PendingIntentController controller;
    final Key key;
    String lastTag;
    String lastTagPrefix;
    private ArraySet<IBinder> mAllowBgActivityStartsForActivitySender = new ArraySet<>();
    private ArraySet<IBinder> mAllowBgActivityStartsForBroadcastSender = new ArraySet<>();
    private ArraySet<IBinder> mAllowBgActivityStartsForServiceSender = new ArraySet<>();
    private RemoteCallbackList<IResultReceiver> mCancelCallbacks;
    public final WeakReference<PendingIntentRecord> ref;
    long sendTime = 0;
    boolean sent = false;
    String stringName;
    final int uid;
    private ArrayMap<IBinder, Long> whitelistDuration;

    static final class Key {
        private static final int ODD_PRIME_NUMBER = 37;
        final IBinder activity;
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

        Key(int _t, String _p, IBinder _a, String _w, int _r, Intent[] _i, String[] _it, int _f, SafeActivityOptions _o, int _userId) {
            this.type = _t;
            this.packageName = _p;
            this.activity = _a;
            this.who = _w;
            this.requestCode = _r;
            String str = null;
            this.requestIntent = _i != null ? _i[_i.length - 1] : null;
            this.requestResolvedType = _it != null ? _it[_it.length - 1] : str;
            this.allIntents = _i;
            this.allResolvedTypes = _it;
            this.flags = _f;
            this.options = _o;
            this.userId = _userId;
            int hash = (((((23 * 37) + _f) * 37) + _r) * 37) + _userId;
            hash = _w != null ? (hash * 37) + _w.hashCode() : hash;
            hash = _a != null ? (hash * 37) + _a.hashCode() : hash;
            Intent intent = this.requestIntent;
            hash = intent != null ? (hash * 37) + intent.filterHashCode() : hash;
            String str2 = this.requestResolvedType;
            this.hashCode = ((((str2 != null ? (hash * 37) + str2.hashCode() : hash) * 37) + (_p != null ? _p.hashCode() : 0)) * 37) + _t;
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
                Intent intent = this.requestIntent;
                Intent intent2 = other.requestIntent;
                if (intent != intent2) {
                    if (this.requestIntent != null) {
                        if (!this.requestIntent.filterEquals(intent2) || this.requestIntent.getHwFlags() != other.requestIntent.getHwFlags()) {
                            return false;
                        }
                    } else if (intent2 != null) {
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
            String str;
            StringBuilder sb = new StringBuilder();
            sb.append("Key{");
            sb.append(typeName());
            sb.append(" pkg=");
            sb.append(this.packageName);
            sb.append(" intent=");
            Intent intent = this.requestIntent;
            if (intent != null) {
                str = intent.toShortStringWithoutClip(true, true, false);
            } else {
                str = "<null>";
            }
            sb.append(str);
            sb.append(" flags=0x");
            sb.append(Integer.toHexString(this.flags));
            sb.append(" u=");
            sb.append(this.userId);
            sb.append("}");
            return sb.toString();
        }

        /* access modifiers changed from: package-private */
        public String typeName() {
            int i = this.type;
            if (i == 1) {
                return "broadcastIntent";
            }
            if (i == 2) {
                return "startActivity";
            }
            if (i == 3) {
                return "activityResult";
            }
            if (i == 4) {
                return "startService";
            }
            if (i != 5) {
                return Integer.toString(i);
            }
            return "startForegroundService";
        }
    }

    PendingIntentRecord(PendingIntentController _controller, Key _k, int _u) {
        this.controller = _controller;
        this.key = _k;
        this.uid = _u;
        this.ref = new WeakReference<>(this);
    }

    /* access modifiers changed from: package-private */
    public void setWhitelistDurationLocked(IBinder whitelistToken, long duration) {
        if (duration > 0) {
            if (this.whitelistDuration == null) {
                this.whitelistDuration = new ArrayMap<>();
            }
            this.whitelistDuration.put(whitelistToken, Long.valueOf(duration));
        } else {
            ArrayMap<IBinder, Long> arrayMap = this.whitelistDuration;
            if (arrayMap != null) {
                arrayMap.remove(whitelistToken);
                if (this.whitelistDuration.size() <= 0) {
                    this.whitelistDuration = null;
                }
            }
        }
        this.stringName = null;
    }

    /* access modifiers changed from: package-private */
    public void setAllowBgActivityStarts(IBinder token, int flags) {
        if (token != null) {
            if ((flags & 1) != 0) {
                this.mAllowBgActivityStartsForActivitySender.add(token);
            }
            if ((flags & 2) != 0) {
                this.mAllowBgActivityStartsForBroadcastSender.add(token);
            }
            if ((flags & 4) != 0) {
                this.mAllowBgActivityStartsForServiceSender.add(token);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void clearAllowBgActivityStarts(IBinder token) {
        if (token != null) {
            this.mAllowBgActivityStartsForActivitySender.remove(token);
            this.mAllowBgActivityStartsForBroadcastSender.remove(token);
            this.mAllowBgActivityStartsForServiceSender.remove(token);
        }
    }

    public void registerCancelListenerLocked(IResultReceiver receiver) {
        if (this.mCancelCallbacks == null) {
            this.mCancelCallbacks = new RemoteCallbackList<>();
        }
        this.mCancelCallbacks.register(receiver);
    }

    public void unregisterCancelListenerLocked(IResultReceiver receiver) {
        RemoteCallbackList<IResultReceiver> remoteCallbackList = this.mCancelCallbacks;
        if (remoteCallbackList != null) {
            remoteCallbackList.unregister(receiver);
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

    /* JADX WARNING: Code restructure failed: missing block: B:112:0x01d5, code lost:
        r11 = android.os.Binder.getCallingUid();
        r44 = android.os.Binder.getCallingPid();
        r45 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:113:0x01e3, code lost:
        if (r34 == null) goto L_0x0275;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:115:?, code lost:
        r0 = r48.controller.mAmInternal.getUidProcessState(r11);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:116:0x01f1, code lost:
        if (android.app.ActivityManager.isProcStateBackground(r0) != false) goto L_0x024f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:117:0x01f3, code lost:
        r1 = new java.lang.StringBuilder(64);
        r1.append("pendingintent:");
        android.os.UserHandle.formatUid(r1, r11);
        r1.append(":");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:118:0x020c, code lost:
        if (r10.getAction() == null) goto L_0x0216;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:119:0x020e, code lost:
        r1.append(r10.getAction());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:121:0x021a, code lost:
        if (r10.getComponent() == null) goto L_0x0224;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:122:0x021c, code lost:
        r10.getComponent().appendShortString(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:124:0x0228, code lost:
        if (r10.getData() == null) goto L_0x0235;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:125:0x022a, code lost:
        r1.append(r10.getData().toSafeString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:126:0x0235, code lost:
        r48.controller.mAmInternal.tempWhitelistForPendingIntent(r44, r11, r48.uid, r34.longValue(), r1.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:127:0x024f, code lost:
        android.util.Slog.w("ActivityManager", "Not doing whitelist " + r48 + ": caller state=" + r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:128:0x026e, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:130:0x0275, code lost:
        if (r53 == null) goto L_0x0279;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:131:0x0277, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:132:0x0279, code lost:
        r0 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:133:0x027a, code lost:
        r37 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:135:?, code lost:
        r0 = r48.key.userId;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:137:0x0281, code lost:
        if (r0 != -2) goto L_0x028f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:138:0x0283, code lost:
        r38 = r48.controller.mUserController.getCurrentOrTargetUserId();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:139:0x028f, code lost:
        r38 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:141:0x0293, code lost:
        if (r48.uid == r11) goto L_0x02a1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:143:0x029d, code lost:
        if (r48.controller.mAtmInternal.isUidForeground(r11) == false) goto L_0x02a1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:144:0x029f, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:145:0x02a1, code lost:
        r0 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:146:0x02a2, code lost:
        r0 = r48.key.type;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:147:0x02a9, code lost:
        if (r0 == 1) goto L_0x03dd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:149:0x02ac, code lost:
        if (r0 == 2) goto L_0x0337;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:151:0x02af, code lost:
        if (r0 == 3) goto L_0x0310;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:153:0x02b3, code lost:
        if (r0 == 4) goto L_0x02bd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:154:0x02b5, code lost:
        if (r0 == 5) goto L_0x02bd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:155:0x02b7, code lost:
        r22 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:157:?, code lost:
        r0 = r48.controller.mAmInternal;
        r1 = r48.uid;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:158:0x02c7, code lost:
        if (r48.key.type != 5) goto L_0x02cc;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:159:0x02c9, code lost:
        r25 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:160:0x02cc, code lost:
        r25 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:161:0x02ce, code lost:
        r2 = r48.key.packageName;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:162:0x02d8, code lost:
        if (r48.mAllowBgActivityStartsForServiceSender.contains(r52) != false) goto L_0x02e0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:163:0x02da, code lost:
        if (r0 == false) goto L_0x02dd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:165:0x02dd, code lost:
        r28 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:166:0x02e0, code lost:
        r28 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:167:0x02e2, code lost:
        r0.startServiceInPackage(r1, r10, r29, r25, r2, r38, r28);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:168:0x02f1, code lost:
        r22 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:170:0x02f8, code lost:
        r22 = r10;
        r9 = -96;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:171:0x0302, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:172:0x0303, code lost:
        android.util.Slog.w("ActivityManager", "Unable to send startService intent", r0);
        r22 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:173:0x0310, code lost:
        r48.controller.mAtmInternal.sendActivityResult(-1, r48.key.activity, r48.key.who, r48.key.requestCode, r49, r10);
        r22 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:176:0x033b, code lost:
        if (r48.key.allIntents == null) goto L_0x0386;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:179:0x0343, code lost:
        if (r48.key.allIntents.length <= 1) goto L_0x0386;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:181:0x0356, code lost:
        r22 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:184:0x0371, code lost:
        r47 = r48.controller.mAtmInternal.startActivitiesInPackage(r48.uid, r44, r11, r48.key.packageName, r35, r36, r55, r33, r38, false, r48, r48.mAllowBgActivityStartsForActivitySender.contains(r52));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:185:0x0374, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:187:0x0378, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:189:0x037f, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:190:0x0380, code lost:
        r22 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:191:0x0386, code lost:
        r22 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:194:0x03be, code lost:
        r47 = r48.controller.mAtmInternal.startActivityInPackage(r48.uid, r44, r11, r48.key.packageName, r22, r29, r55, r56, r57, 0, r33, r38, (com.android.server.wm.TaskRecord) null, "PendingIntentRecord", false, r48, r48.mAllowBgActivityStartsForActivitySender.contains(r52));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:195:0x03c0, code lost:
        r9 = r47;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:196:0x03c4, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:197:0x03c6, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:199:0x03ce, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:200:0x03cf, code lost:
        r22 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:203:?, code lost:
        android.util.Slog.w("ActivityManager", "Unable to send startActivity intent", r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:204:0x03dd, code lost:
        r22 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:206:?, code lost:
        r0 = r48.controller.mAmInternal;
        r2 = r48.key.packageName;
        r3 = r48.uid;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:207:0x03ef, code lost:
        if (r53 == null) goto L_0x03f3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:208:0x03f1, code lost:
        r14 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:209:0x03f3, code lost:
        r14 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:211:0x03fe, code lost:
        if (r48.mAllowBgActivityStartsForBroadcastSender.contains(r52) != false) goto L_0x0406;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:212:0x0400, code lost:
        if (r0 == false) goto L_0x0403;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:214:0x0403, code lost:
        r17 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:215:0x0406, code lost:
        r17 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:217:0x0421, code lost:
        if (r0.broadcastIntentInPackage(r2, r3, r11, r44, r22, r29, r53, r49, (java.lang.String) null, (android.os.Bundle) null, r54, r32, r14, false, r38, r17) != 0) goto L_0x0425;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:218:0x0423, code lost:
        r37 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:219:0x0425, code lost:
        r9 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:220:0x0428, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:223:0x042f, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:226:?, code lost:
        android.util.Slog.w("ActivityManager", "Unable to send startActivity intent", r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:227:0x0438, code lost:
        r9 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:228:0x043a, code lost:
        if (r37 != false) goto L_0x043c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:238:?, code lost:
        r53.performReceive(new android.content.Intent(r22), 0, (java.lang.String) null, (android.os.Bundle) null, false, false, r48.key.userId);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:239:0x0458, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:241:0x045c, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:246:0x0466, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:251:0x0478, code lost:
        android.os.Binder.restoreCallingIdentity(r45);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:252:0x047c, code lost:
        return r9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:253:0x047d, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:255:0x0483, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:257:0x0488, code lost:
        android.os.Binder.restoreCallingIdentity(r45);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:258:0x048b, code lost:
        throw r0;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:229:0x043c  */
    public int sendInner(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, IBinder resultTo, String resultWho, int requestCode, int flagsMask, int flagsValues, Bundle options) {
        String resolvedType2;
        SafeActivityOptions mergedOptions;
        Bundle options2;
        Long duration;
        String[] allResolvedTypes;
        Intent[] allIntents;
        boolean z;
        String resolvedType3;
        Bundle options3 = options;
        if (intent != null) {
            intent.setDefusable(true);
        }
        if (options3 != null) {
            options3.setDefusable(true);
        }
        long startGetLockTime = System.currentTimeMillis();
        synchronized (this.controller.mLock) {
            try {
                if (this.canceled) {
                    try {
                        return -96;
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                } else {
                    this.sent = true;
                    this.sendTime = System.currentTimeMillis();
                    long getLockCost = this.sendTime - startGetLockTime;
                    if (getLockCost > 5000) {
                        Slog.w("ActivityManager", "get controller.mLock cost:" + getLockCost);
                    }
                    if ((this.key.flags & 1073741824) != 0) {
                        this.controller.cancelIntentSender(this, true);
                    }
                    Intent finalIntent = this.key.requestIntent != null ? new Intent(this.key.requestIntent) : new Intent();
                    try {
                        if (!((this.key.flags & DumpState.DUMP_HANDLE) != 0)) {
                            if (intent != null) {
                                try {
                                    if ((finalIntent.fillIn(intent, this.key.flags) & 2) == 0) {
                                        resolvedType3 = this.key.requestResolvedType;
                                    } else {
                                        resolvedType3 = resolvedType;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            } else {
                                try {
                                    resolvedType3 = this.key.requestResolvedType;
                                } catch (Throwable th4) {
                                    th = th4;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            }
                            int flagsMask2 = flagsMask & -196;
                            try {
                                finalIntent.setFlags((finalIntent.getFlags() & (~flagsMask2)) | (flagsValues & flagsMask2));
                                resolvedType2 = resolvedType3;
                            } catch (Throwable th5) {
                                th = th5;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        } else {
                            try {
                                resolvedType2 = this.key.requestResolvedType;
                            } catch (Throwable th6) {
                                th = th6;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                    try {
                        finalIntent.addHwFlags(256);
                        ActivityOptions opts = ActivityOptions.fromBundle(options);
                        if (opts != null) {
                            try {
                                finalIntent.addFlags(opts.getPendingIntentLaunchFlags());
                            } catch (Throwable th8) {
                                th = th8;
                            }
                        }
                        SafeActivityOptions mergedOptions2 = this.key.options;
                        if (mergedOptions2 == null) {
                            mergedOptions2 = new SafeActivityOptions(opts);
                        } else {
                            mergedOptions2.setCallerOptions(opts);
                        }
                        if (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.enabledInPad() || this.key.type != 2) {
                            options2 = options3;
                            mergedOptions = mergedOptions2;
                        } else {
                            if (options3 == null) {
                                options3 = new Bundle();
                            }
                            try {
                                options3.putInt("android.activity.launchDisplayId", HwPCUtils.getPCDisplayID());
                                options2 = options3;
                                mergedOptions = new SafeActivityOptions(ActivityOptions.fromBundle(options3));
                            } catch (Throwable th9) {
                                th = th9;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                    try {
                        if (this.whitelistDuration != null) {
                            try {
                                duration = this.whitelistDuration.get(whitelistToken);
                            } catch (Throwable th11) {
                                th = th11;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        } else {
                            duration = null;
                        }
                        try {
                            if (this.key.type == 2) {
                                try {
                                    if (this.key.allIntents != null && this.key.allIntents.length > 1) {
                                        Intent[] allIntents2 = new Intent[this.key.allIntents.length];
                                        String[] allResolvedTypes2 = new String[this.key.allIntents.length];
                                        System.arraycopy(this.key.allIntents, 0, allIntents2, 0, this.key.allIntents.length);
                                        if (this.key.allResolvedTypes != null) {
                                            z = false;
                                            System.arraycopy(this.key.allResolvedTypes, 0, allResolvedTypes2, 0, this.key.allResolvedTypes.length);
                                        } else {
                                            z = false;
                                        }
                                        allIntents2[allIntents2.length - 1] = finalIntent;
                                        allResolvedTypes2[allResolvedTypes2.length - 1] = resolvedType2;
                                        allIntents = allIntents2;
                                        allResolvedTypes = allResolvedTypes2;
                                    }
                                } catch (Throwable th12) {
                                    th = th12;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            }
                            z = false;
                            allIntents = null;
                            allResolvedTypes = null;
                        } catch (Throwable th13) {
                            th = th13;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                        try {
                        } catch (Throwable th14) {
                            th = th14;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    } catch (Throwable th15) {
                        th = th15;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
            } catch (Throwable th16) {
                th = th16;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void finalize() throws Throwable {
        try {
            if (!this.canceled) {
                this.controller.mH.sendMessage(PooledLambda.obtainMessage($$Lambda$PendingIntentRecord$hlEHdgdG_SS5n3v7IRr7e6QZgLQ.INSTANCE, this));
            }
        } finally {
            PendingIntentRecord.super.finalize();
        }
    }

    /* access modifiers changed from: private */
    public void completeFinalize() {
        synchronized (this.controller.mLock) {
            if (this.controller.mIntentSenderRecords.get(this.key) == this.ref) {
                this.controller.mIntentSenderRecords.remove(this.key);
            }
        }
    }

    public void dump(PrintWriter pw, String prefix) {
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
        if (this.whitelistDuration != null) {
            pw.print(prefix);
            pw.print("whitelistDuration=");
            for (int i = 0; i < this.whitelistDuration.size(); i++) {
                if (i != 0) {
                    pw.print(", ");
                }
                pw.print(Integer.toHexString(System.identityHashCode(this.whitelistDuration.keyAt(i))));
                pw.print(":");
                TimeUtils.formatDuration(this.whitelistDuration.valueAt(i).longValue(), pw);
            }
            pw.println();
        }
        if (this.mCancelCallbacks != null) {
            pw.print(prefix);
            pw.println("mCancelCallbacks:");
            for (int i2 = 0; i2 < this.mCancelCallbacks.getRegisteredCallbackCount(); i2++) {
                pw.print(prefix);
                pw.print("  #");
                pw.print(i2);
                pw.print(": ");
                pw.println(this.mCancelCallbacks.getRegisteredCallbackItem(i2));
            }
        }
    }

    public String toString() {
        String str = this.stringName;
        if (str != null) {
            return str;
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
                TimeUtils.formatDuration(this.whitelistDuration.valueAt(i).longValue(), sb);
            }
            sb.append(")");
        }
        sb.append('}');
        String sb2 = sb.toString();
        this.stringName = sb2;
        return sb2;
    }
}
