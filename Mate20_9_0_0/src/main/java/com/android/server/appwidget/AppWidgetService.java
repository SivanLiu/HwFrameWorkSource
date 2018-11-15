package com.android.server.appwidget;

import android.content.Context;
import android.util.Log;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.FgThread;
import com.android.server.SystemService;
import com.android.server.appwidget.HwAppWidgetServiceFactory.IHwAppWidgetService;

public class AppWidgetService extends SystemService {
    private static final String TAG = "AppWidgetService";
    private final AppWidgetServiceImpl mImpl;

    public AppWidgetService(Context context) {
        super(context);
        IHwAppWidgetService iAppWidgetMS = HwAppWidgetServiceFactory.getHwAppWidgetService();
        if (iAppWidgetMS != null) {
            this.mImpl = iAppWidgetMS.getAppWidgetImpl(context);
        } else {
            this.mImpl = new AppWidgetServiceImpl(context);
        }
    }

    public void onStart() {
        this.mImpl.onStart();
        publishBinderService("appwidget", this.mImpl);
        AppWidgetBackupBridge.register(this.mImpl);
    }

    public void onBootPhase(int phase) {
        if (phase == 550) {
            Log.d(TAG, "onBootPhase=600");
            this.mImpl.setSafeMode(isSafeMode());
        }
    }

    public void onUnlockUser(int userHandle) {
        FgThread.getHandler().post(new -$$Lambda$AppWidgetService$HIwvoPMyKqEhLVIiysgUKH8QJg8(this, userHandle));
    }

    public void onStopUser(int userHandle) {
        this.mImpl.onUserStopped(userHandle);
    }

    public void onSwitchUser(int userHandle) {
        this.mImpl.reloadWidgetsMaskedStateForGroup(userHandle);
    }
}
