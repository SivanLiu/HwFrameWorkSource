package com.android.server.timezone;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Binder;
import java.io.PrintWriter;
import java.util.concurrent.Executor;

final class RulesManagerServiceHelperImpl implements PermissionHelper, Executor {
    private final Context mContext;

    RulesManagerServiceHelperImpl(Context context) {
        this.mContext = context;
    }

    public void enforceCallerHasPermission(String requiredPermission) {
        this.mContext.enforceCallingPermission(requiredPermission, null);
    }

    public boolean checkDumpPermission(String tag, PrintWriter pw) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") == 0) {
            return true;
        }
        pw.println("Permission Denial: can't dump LocationManagerService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        return false;
    }

    public void execute(Runnable runnable) {
        AsyncTask.execute(runnable);
    }
}
