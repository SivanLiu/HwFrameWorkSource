package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.notification.ZenModeConfig;
import android.util.Slog;
import android.view.View;
import android.widget.Toast;
import com.android.internal.R;

public class IntentForwarderActivity extends Activity {
    public static String FORWARD_INTENT_TO_MANAGED_PROFILE = "com.android.internal.app.ForwardIntentToManagedProfile";
    public static String FORWARD_INTENT_TO_PARENT = "com.android.internal.app.ForwardIntentToParent";
    public static String TAG = "IntentForwarderActivity";
    private Injector mInjector;

    public interface Injector {
        IPackageManager getIPackageManager();

        PackageManager getPackageManager();

        UserManager getUserManager();
    }

    private class InjectorImpl implements Injector {
        private InjectorImpl() {
        }

        public IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        public UserManager getUserManager() {
            return (UserManager) IntentForwarderActivity.this.getSystemService(UserManager.class);
        }

        public PackageManager getPackageManager() {
            return IntentForwarderActivity.this.getPackageManager();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        int userMessageId;
        int targetUserId;
        super.onCreate(savedInstanceState);
        this.mInjector = createInjector();
        Intent intentReceived = getIntent();
        String className = intentReceived.getComponent().getClassName();
        if (className.equals(FORWARD_INTENT_TO_PARENT)) {
            userMessageId = R.string.forward_intent_to_owner;
            targetUserId = getProfileParent();
        } else if (className.equals(FORWARD_INTENT_TO_MANAGED_PROFILE)) {
            userMessageId = R.string.forward_intent_to_work;
            targetUserId = getManagedProfile();
        } else {
            Slog.wtf(TAG, IntentForwarderActivity.class.getName() + " cannot be called directly");
            userMessageId = -1;
            targetUserId = -10000;
        }
        if (targetUserId == -10000) {
            finish();
            return;
        }
        int callingUserId = getUserId();
        Intent newIntent = canForward(intentReceived, targetUserId);
        if (newIntent != null) {
            boolean z;
            if ("android.intent.action.CHOOSER".equals(newIntent.getAction())) {
                ((Intent) newIntent.getParcelableExtra("android.intent.extra.INTENT")).prepareToLeaveUser(callingUserId);
            } else {
                newIntent.prepareToLeaveUser(callingUserId);
            }
            ResolveInfo ri = this.mInjector.getPackageManager().resolveActivityAsUser(newIntent, 65536, targetUserId);
            if (ri == null || ri.activityInfo == null || (ZenModeConfig.SYSTEM_AUTHORITY.equals(ri.activityInfo.packageName) ^ 1) != 0) {
                z = true;
            } else {
                int i;
                if (ResolverActivity.class.getName().equals(ri.activityInfo.name)) {
                    i = 1;
                } else {
                    i = ChooserActivity.class.getName().equals(ri.activityInfo.name);
                }
                z = i ^ 1;
            }
            try {
                startActivityAsCaller(newIntent, null, false, targetUserId);
            } catch (RuntimeException e) {
                int i2 = -1;
                String launchedFromPackage = "?";
                try {
                    i2 = ActivityManager.getService().getLaunchedFromUid(getActivityToken());
                    launchedFromPackage = ActivityManager.getService().getLaunchedFromPackage(getActivityToken());
                } catch (RemoteException e2) {
                }
                Slog.wtf(TAG, "Unable to launch as UID " + i2 + " package " + launchedFromPackage + ", while running in " + ActivityThread.currentProcessName(), e);
            }
            if (z) {
                Toast.makeText((Context) this, getString(userMessageId), 1).show();
            }
        } else {
            Slog.wtf(TAG, "the intent: " + intentReceived + " cannot be forwarded from user " + callingUserId + " to user " + targetUserId);
        }
        finish();
    }

    Intent canForward(Intent incomingIntent, int targetUserId) {
        if (incomingIntent.getAction() == null) {
            Slog.wtf(TAG, "The action of fowarded intent is null");
            return null;
        }
        Intent forwardIntent = new Intent(incomingIntent);
        forwardIntent.addFlags(View.SCROLLBARS_OUTSIDE_INSET);
        sanitizeIntent(forwardIntent);
        Intent intent = forwardIntent;
        if ("android.intent.action.CHOOSER".equals(forwardIntent.getAction())) {
            if (forwardIntent.hasExtra("android.intent.extra.INITIAL_INTENTS")) {
                Slog.wtf(TAG, "An chooser intent with extra initial intents cannot be forwarded to a different user");
                return null;
            } else if (forwardIntent.hasExtra("android.intent.extra.REPLACEMENT_EXTRAS")) {
                Slog.wtf(TAG, "A chooser intent with replacement extras cannot be forwarded to a different user");
                return null;
            } else {
                intent = (Intent) forwardIntent.getParcelableExtra("android.intent.extra.INTENT");
                if (intent == null) {
                    Slog.wtf(TAG, "Cannot forward a chooser intent with no extra android.intent.extra.INTENT");
                    return null;
                }
            }
        }
        if (forwardIntent.getSelector() != null) {
            intent = forwardIntent.getSelector();
        }
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        sanitizeIntent(intent);
        try {
            if (this.mInjector.getIPackageManager().canForwardTo(intent, resolvedType, getUserId(), targetUserId)) {
                return forwardIntent;
            }
            return null;
        } catch (RemoteException e) {
            Slog.e(TAG, "PackageManagerService is dead?");
        }
    }

    private int getManagedProfile() {
        for (UserInfo userInfo : this.mInjector.getUserManager().getProfiles(UserHandle.myUserId())) {
            if (userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        Slog.wtf(TAG, FORWARD_INTENT_TO_MANAGED_PROFILE + " has been called, but there is no managed profile");
        return -10000;
    }

    private int getProfileParent() {
        UserInfo parent = this.mInjector.getUserManager().getProfileParent(UserHandle.myUserId());
        if (parent != null) {
            return parent.id;
        }
        Slog.wtf(TAG, FORWARD_INTENT_TO_PARENT + " has been called, but there is no parent");
        return -10000;
    }

    private void sanitizeIntent(Intent intent) {
        intent.setPackage(null);
        intent.setComponent(null);
    }

    protected Injector createInjector() {
        return new InjectorImpl();
    }
}
