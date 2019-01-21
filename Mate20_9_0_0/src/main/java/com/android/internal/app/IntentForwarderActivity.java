package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Protocol;

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

    /* JADX WARNING: Removed duplicated region for block: B:11:0x005f  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x005b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void onCreate(Bundle savedInstanceState) {
        int userMessageId;
        int targetUserId;
        int userMessageId2;
        super.onCreate(savedInstanceState);
        this.mInjector = createInjector();
        Intent intentReceived = getIntent();
        String className = intentReceived.getComponent().getClassName();
        if (className.equals(FORWARD_INTENT_TO_PARENT)) {
            userMessageId = 17040105;
            targetUserId = getProfileParent();
        } else if (className.equals(FORWARD_INTENT_TO_MANAGED_PROFILE)) {
            userMessageId = 17040106;
            targetUserId = getManagedProfile();
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(IntentForwarderActivity.class.getName());
            stringBuilder.append(" cannot be called directly");
            Slog.wtf(str, stringBuilder.toString());
            userMessageId2 = -1;
            targetUserId = -10000;
            if (targetUserId != -10000) {
                finish();
                return;
            }
            int callingUserId = getUserId();
            Intent newIntent = canForward(intentReceived, targetUserId);
            if (newIntent != null) {
                if ("android.intent.action.CHOOSER".equals(newIntent.getAction())) {
                    ((Intent) newIntent.getParcelableExtra("android.intent.extra.INTENT")).prepareToLeaveUser(callingUserId);
                } else {
                    newIntent.prepareToLeaveUser(callingUserId);
                }
                ResolveInfo ri = this.mInjector.getPackageManager().resolveActivityAsUser(newIntent, Protocol.BASE_SYSTEM_RESERVED, targetUserId);
                boolean shouldShowDisclosure = ri == null || ri.activityInfo == null || !"android".equals(ri.activityInfo.packageName) || !(ResolverActivity.class.getName().equals(ri.activityInfo.name) || ChooserActivity.class.getName().equals(ri.activityInfo.name));
                try {
                    startActivityAsCaller(newIntent, null, false, targetUserId);
                } catch (RuntimeException e) {
                    RuntimeException e2 = e;
                    int launchedFromUid = -1;
                    String launchedFromPackage = "?";
                    try {
                        launchedFromUid = ActivityManager.getService().getLaunchedFromUid(getActivityToken());
                        launchedFromPackage = ActivityManager.getService().getLaunchedFromPackage(getActivityToken());
                    } catch (RemoteException e3) {
                    }
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to launch as UID ");
                    stringBuilder2.append(launchedFromUid);
                    stringBuilder2.append(" package ");
                    stringBuilder2.append(launchedFromPackage);
                    stringBuilder2.append(", while running in ");
                    stringBuilder2.append(ActivityThread.currentProcessName());
                    Slog.wtf(str, stringBuilder2.toString(), e2);
                }
                if (shouldShowDisclosure) {
                    Toast.makeText(this, getString(userMessageId2), 1).show();
                }
            } else {
                str = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("the intent: ");
                stringBuilder3.append(intentReceived);
                stringBuilder3.append(" cannot be forwarded from user ");
                stringBuilder3.append(callingUserId);
                stringBuilder3.append(" to user ");
                stringBuilder3.append(targetUserId);
                Slog.wtf(str, stringBuilder3.toString());
            }
            finish();
            return;
        }
        userMessageId2 = userMessageId;
        if (targetUserId != -10000) {
        }
    }

    Intent canForward(Intent incomingIntent, int targetUserId) {
        if (incomingIntent.getAction() == null) {
            Slog.wtf(TAG, "The action of fowarded intent is null");
            return null;
        }
        Intent forwardIntent = new Intent(incomingIntent);
        forwardIntent.addFlags(50331648);
        sanitizeIntent(forwardIntent);
        Intent intentToCheck = forwardIntent;
        if ("android.intent.action.CHOOSER".equals(forwardIntent.getAction())) {
            if (forwardIntent.hasExtra("android.intent.extra.INITIAL_INTENTS")) {
                Slog.wtf(TAG, "An chooser intent with extra initial intents cannot be forwarded to a different user");
                return null;
            } else if (forwardIntent.hasExtra("android.intent.extra.REPLACEMENT_EXTRAS")) {
                Slog.wtf(TAG, "A chooser intent with replacement extras cannot be forwarded to a different user");
                return null;
            } else {
                intentToCheck = (Intent) forwardIntent.getParcelableExtra("android.intent.extra.INTENT");
                if (intentToCheck == null) {
                    Slog.wtf(TAG, "Cannot forward a chooser intent with no extra android.intent.extra.INTENT");
                    return null;
                }
            }
        }
        if (forwardIntent.getSelector() != null) {
            intentToCheck = forwardIntent.getSelector();
        }
        String resolvedType = intentToCheck.resolveTypeIfNeeded(getContentResolver());
        sanitizeIntent(intentToCheck);
        try {
            if (this.mInjector.getIPackageManager().canForwardTo(intentToCheck, resolvedType, getUserId(), targetUserId)) {
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(FORWARD_INTENT_TO_MANAGED_PROFILE);
        stringBuilder.append(" has been called, but there is no managed profile");
        Slog.wtf(str, stringBuilder.toString());
        return -10000;
    }

    private int getProfileParent() {
        UserInfo parent = this.mInjector.getUserManager().getProfileParent(UserHandle.myUserId());
        if (parent != null) {
            return parent.id;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(FORWARD_INTENT_TO_PARENT);
        stringBuilder.append(" has been called, but there is no parent");
        Slog.wtf(str, stringBuilder.toString());
        return -10000;
    }

    private void sanitizeIntent(Intent intent) {
        intent.setPackage(null);
        intent.setComponent(null);
    }

    @VisibleForTesting
    protected Injector createInjector() {
        return new InjectorImpl();
    }
}
