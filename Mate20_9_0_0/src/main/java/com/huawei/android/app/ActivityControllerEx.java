package com.huawei.android.app;

import android.app.ActivityManagerNative;
import android.app.IActivityController.Stub;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import com.huawei.android.os.HwTransCodeEx;

public abstract class ActivityControllerEx {
    private static final int SET_CUSTOM_ACTIVITY_CONTROLLER_TRANSACTION = 2101;
    private static final String TAG = "ActivityControllerEx";
    private InnerActivityController innerActivityController = new InnerActivityController();

    private class InnerActivityController extends Stub {
        private InnerActivityController() {
        }

        public boolean activityResuming(String pkgName) throws RemoteException {
            return ActivityControllerEx.this.activityResuming(pkgName);
        }

        public boolean activityStarting(Intent pIntent, String pkgName) throws RemoteException {
            return ActivityControllerEx.this.activityStarting(pIntent, pkgName);
        }

        public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg, long timeMillis, String stackTrace) throws RemoteException {
            return ActivityControllerEx.this.appCrashed(processName, pid, shortMsg, longMsg, timeMillis, stackTrace);
        }

        public int appEarlyNotResponding(String processName, int pid, String annotation) throws RemoteException {
            return ActivityControllerEx.this.appEarlyNotResponding(processName, pid, annotation);
        }

        public int appNotResponding(String processName, int pid, String processStats) throws RemoteException {
            return ActivityControllerEx.this.appNotResponding(processName, pid, processStats);
        }

        public int systemNotResponding(String msg) throws RemoteException {
            return ActivityControllerEx.this.systemNotResponding(msg);
        }
    }

    public abstract boolean activityResuming(String str) throws RemoteException;

    public abstract boolean activityStarting(Intent intent, String str) throws RemoteException;

    public abstract boolean appCrashed(String str, int i, String str2, String str3, long j, String str4) throws RemoteException;

    public abstract int appEarlyNotResponding(String str, int i, String str2) throws RemoteException;

    public abstract int appNotResponding(String str, int i, String str2) throws RemoteException;

    public abstract int systemNotResponding(String str) throws RemoteException;

    public IBinder asBinder() {
        return this.innerActivityController.asBinder();
    }

    /* JADX WARNING: Missing block: B:11:0x003e, code skipped:
            if (r3 != null) goto L_0x0040;
     */
    /* JADX WARNING: Missing block: B:12:0x0040, code skipped:
            r3.recycle();
     */
    /* JADX WARNING: Missing block: B:23:0x0057, code skipped:
            if (r3 != null) goto L_0x0040;
     */
    /* JADX WARNING: Missing block: B:24:0x005a, code skipped:
            r2 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("enableActivityController end. retVal = ");
            r4.append(r0);
            android.util.Log.i(r2, r4.toString());
     */
    /* JADX WARNING: Missing block: B:25:0x0070, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean setCustomActivityController(ActivityControllerEx ctrl) {
        boolean retVal = true;
        Parcel data = null;
        Parcel reply = null;
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            if (ctrl != null) {
                data.writeStrongBinder(ctrl.asBinder());
            } else {
                data.writeStrongBinder(null);
            }
            if (!ActivityManagerNative.getDefault().asBinder().transact(SET_CUSTOM_ACTIVITY_CONTROLLER_TRANSACTION, data, reply, 0)) {
                retVal = false;
                Log.e(TAG, "Transact to activity manager failed! AcitivtyController logic invalid!");
            }
            if (data != null) {
                data.recycle();
            }
        } catch (RemoteException e) {
            retVal = false;
            if (data != null) {
                data.recycle();
            }
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
        }
    }
}
