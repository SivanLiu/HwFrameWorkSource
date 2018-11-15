package com.android.server.pm;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPinItemRequest;
import android.content.pm.IPinItemRequest.Stub;
import android.content.pm.LauncherApps.PinItemRequest;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;

class ShortcutRequestPinProcessor {
    private static final boolean DEBUG = false;
    private static final String TAG = "ShortcutService";
    private final Object mLock;
    private final ShortcutService mService;

    private static abstract class PinItemRequestInner extends Stub {
        @GuardedBy("this")
        private boolean mAccepted;
        private final int mLauncherUid;
        protected final ShortcutRequestPinProcessor mProcessor;
        private final IntentSender mResultIntent;

        private PinItemRequestInner(ShortcutRequestPinProcessor processor, IntentSender resultIntent, int launcherUid) {
            this.mProcessor = processor;
            this.mResultIntent = resultIntent;
            this.mLauncherUid = launcherUid;
        }

        public ShortcutInfo getShortcutInfo() {
            return null;
        }

        public AppWidgetProviderInfo getAppWidgetProviderInfo() {
            return null;
        }

        public Bundle getExtras() {
            return null;
        }

        private boolean isCallerValid() {
            return this.mProcessor.isCallerUid(this.mLauncherUid);
        }

        public boolean isValid() {
            if (!isCallerValid()) {
                return false;
            }
            boolean z;
            synchronized (this) {
                z = this.mAccepted ^ 1;
            }
            return z;
        }

        public boolean accept(Bundle options) {
            if (isCallerValid()) {
                Intent extras = null;
                if (options != null) {
                    try {
                        options.size();
                        extras = new Intent().putExtras(options);
                    } catch (RuntimeException e) {
                        throw new IllegalArgumentException("options cannot be unparceled", e);
                    }
                }
                synchronized (this) {
                    if (this.mAccepted) {
                        throw new IllegalStateException("accept() called already");
                    }
                    this.mAccepted = true;
                }
                if (!tryAccept()) {
                    return false;
                }
                this.mProcessor.sendResultIntent(this.mResultIntent, extras);
                return true;
            }
            throw new SecurityException("Calling uid mismatch");
        }

        protected boolean tryAccept() {
            return true;
        }
    }

    private static class PinAppWidgetRequestInner extends PinItemRequestInner {
        final AppWidgetProviderInfo mAppWidgetProviderInfo;
        final Bundle mExtras;

        private PinAppWidgetRequestInner(ShortcutRequestPinProcessor processor, IntentSender resultIntent, int launcherUid, AppWidgetProviderInfo appWidgetProviderInfo, Bundle extras) {
            super(resultIntent, launcherUid);
            this.mAppWidgetProviderInfo = appWidgetProviderInfo;
            this.mExtras = extras;
        }

        public AppWidgetProviderInfo getAppWidgetProviderInfo() {
            return this.mAppWidgetProviderInfo;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }
    }

    private static class PinShortcutRequestInner extends PinItemRequestInner {
        public final String launcherPackage;
        public final int launcherUserId;
        public final boolean preExisting;
        public final ShortcutInfo shortcutForLauncher;
        public final ShortcutInfo shortcutOriginal;

        private PinShortcutRequestInner(ShortcutRequestPinProcessor processor, ShortcutInfo shortcutOriginal, ShortcutInfo shortcutForLauncher, IntentSender resultIntent, String launcherPackage, int launcherUserId, int launcherUid, boolean preExisting) {
            super(resultIntent, launcherUid);
            this.shortcutOriginal = shortcutOriginal;
            this.shortcutForLauncher = shortcutForLauncher;
            this.launcherPackage = launcherPackage;
            this.launcherUserId = launcherUserId;
            this.preExisting = preExisting;
        }

        public ShortcutInfo getShortcutInfo() {
            return this.shortcutForLauncher;
        }

        protected boolean tryAccept() {
            return this.mProcessor.directPinShortcut(this);
        }
    }

    public ShortcutRequestPinProcessor(ShortcutService service, Object lock) {
        this.mService = service;
        this.mLock = lock;
    }

    public boolean isRequestPinItemSupported(int callingUserId, int requestType) {
        return getRequestPinConfirmationActivity(callingUserId, requestType) != null;
    }

    public boolean requestPinItemLocked(ShortcutInfo inShortcut, AppWidgetProviderInfo inAppWidget, Bundle extras, int userId, IntentSender resultIntent) {
        ShortcutInfo shortcutInfo = inShortcut;
        int requestType = shortcutInfo != null ? 1 : 2;
        Pair<ComponentName, Integer> confirmActivity = getRequestPinConfirmationActivity(userId, requestType);
        if (confirmActivity == null) {
            Log.w(TAG, "Launcher doesn't support requestPinnedShortcut(). Shortcut not created.");
            return false;
        }
        PinItemRequest request;
        int launcherUserId = ((Integer) confirmActivity.second).intValue();
        this.mService.throwIfUserLockedL(launcherUserId);
        if (shortcutInfo != null) {
            request = requestPinShortcutLocked(shortcutInfo, resultIntent, confirmActivity);
        } else {
            IPinItemRequest iPinItemRequest = r0;
            PinAppWidgetRequestInner pinAppWidgetRequestInner = new PinAppWidgetRequestInner(resultIntent, this.mService.injectGetPackageUid(((ComponentName) confirmActivity.first).getPackageName(), launcherUserId), inAppWidget, extras);
            request = new PinItemRequest(iPinItemRequest, 2);
        }
        return startRequestConfirmActivity((ComponentName) confirmActivity.first, launcherUserId, request, requestType);
    }

    public Intent createShortcutResultIntent(ShortcutInfo inShortcut, int userId) {
        int launcherUserId = this.mService.getParentOrSelfUserId(userId);
        ComponentName defaultLauncher = this.mService.getDefaultLauncher(launcherUserId);
        if (defaultLauncher == null) {
            Log.e(TAG, "Default launcher not found.");
            return null;
        }
        this.mService.throwIfUserLockedL(launcherUserId);
        return new Intent().putExtra("android.content.pm.extra.PIN_ITEM_REQUEST", requestPinShortcutLocked(inShortcut, null, Pair.create(defaultLauncher, Integer.valueOf(launcherUserId))));
    }

    private PinItemRequest requestPinShortcutLocked(ShortcutInfo inShortcut, IntentSender resultIntentOriginal, Pair<ComponentName, Integer> confirmActivity) {
        IntentSender resultIntentToSend;
        ShortcutInfo shortcutForLauncher;
        ShortcutInfo shortcutInfo = inShortcut;
        Pair<ComponentName, Integer> pair = confirmActivity;
        ShortcutInfo existing = this.mService.getPackageShortcutsForPublisherLocked(inShortcut.getPackage(), inShortcut.getUserId()).findShortcutById(inShortcut.getId());
        boolean z = false;
        boolean existsAlready = existing != null;
        if (existsAlready && existing.isVisibleToPublisher()) {
            z = true;
        }
        boolean existingIsVisible = z;
        String launcherPackage = ((ComponentName) pair.first).getPackageName();
        int launcherUserId = ((Integer) pair.second).intValue();
        IntentSender resultIntentToSend2 = resultIntentOriginal;
        IntentSender intentSender;
        if (existsAlready) {
            validateExistingShortcut(existing);
            boolean isAlreadyPinned = this.mService.getLauncherShortcutsLocked(launcherPackage, existing.getUserId(), launcherUserId).hasPinned(existing);
            if (isAlreadyPinned) {
                sendResultIntent(resultIntentOriginal, null);
                resultIntentToSend2 = null;
            } else {
                intentSender = resultIntentOriginal;
            }
            ShortcutInfo shortcutForLauncher2 = existing.clone(11);
            if (!isAlreadyPinned) {
                shortcutForLauncher2.clearFlags(2);
            }
            resultIntentToSend = resultIntentToSend2;
            shortcutForLauncher = shortcutForLauncher2;
        } else {
            intentSender = resultIntentOriginal;
            if (inShortcut.getActivity() == null) {
                shortcutInfo.setActivity(this.mService.injectGetDefaultMainActivity(inShortcut.getPackage(), inShortcut.getUserId()));
            }
            this.mService.validateShortcutForPinRequest(shortcutInfo);
            shortcutInfo.resolveResourceStrings(this.mService.injectGetResourcesForApplicationAsUser(inShortcut.getPackage(), inShortcut.getUserId()));
            resultIntentToSend = resultIntentToSend2;
            shortcutForLauncher = shortcutInfo.clone(10);
        }
        return new PinItemRequest(new PinShortcutRequestInner(shortcutInfo, shortcutForLauncher, resultIntentToSend, launcherPackage, launcherUserId, this.mService.injectGetPackageUid(launcherPackage, launcherUserId), existsAlready), 1);
    }

    private void validateExistingShortcut(ShortcutInfo shortcutInfo) {
        boolean isEnabled = shortcutInfo.isEnabled();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Shortcut ID=");
        stringBuilder.append(shortcutInfo);
        stringBuilder.append(" already exists but disabled.");
        Preconditions.checkArgument(isEnabled, stringBuilder.toString());
    }

    private boolean startRequestConfirmActivity(ComponentName activity, int launcherUserId, PinItemRequest request, int requestType) {
        String action;
        if (requestType == 1) {
            action = "android.content.pm.action.CONFIRM_PIN_SHORTCUT";
        } else {
            action = "android.content.pm.action.CONFIRM_PIN_APPWIDGET";
        }
        Intent confirmIntent = new Intent(action);
        confirmIntent.setComponent(activity);
        confirmIntent.putExtra("android.content.pm.extra.PIN_ITEM_REQUEST", request);
        confirmIntent.addFlags(268468224);
        long token = this.mService.injectClearCallingIdentity();
        try {
            this.mService.mContext.startActivityAsUser(confirmIntent, UserHandle.of(launcherUserId));
            this.mService.injectRestoreCallingIdentity(token);
            return true;
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to start activity ");
            stringBuilder.append(activity);
            Log.e(str, stringBuilder.toString(), e);
            this.mService.injectRestoreCallingIdentity(token);
            return false;
        } catch (Throwable th) {
            this.mService.injectRestoreCallingIdentity(token);
            throw th;
        }
    }

    @VisibleForTesting
    Pair<ComponentName, Integer> getRequestPinConfirmationActivity(int callingUserId, int requestType) {
        int launcherUserId = this.mService.getParentOrSelfUserId(callingUserId);
        ComponentName defaultLauncher = this.mService.getDefaultLauncher(launcherUserId);
        Pair<ComponentName, Integer> pair = null;
        if (defaultLauncher == null) {
            Log.e(TAG, "Default launcher not found.");
            return null;
        }
        ComponentName activity = this.mService.injectGetPinConfirmationActivity(defaultLauncher.getPackageName(), launcherUserId, requestType);
        if (activity != null) {
            pair = Pair.create(activity, Integer.valueOf(launcherUserId));
        }
        return pair;
    }

    public void sendResultIntent(IntentSender intent, Intent extras) {
        this.mService.injectSendIntentSender(intent, extras);
    }

    public boolean isCallerUid(int uid) {
        return uid == this.mService.injectBinderCallingUid();
    }

    public boolean directPinShortcut(PinShortcutRequestInner request) {
        PinShortcutRequestInner pinShortcutRequestInner = request;
        ShortcutInfo original = pinShortcutRequestInner.shortcutOriginal;
        int appUserId = original.getUserId();
        String appPackageName = original.getPackage();
        int launcherUserId = pinShortcutRequestInner.launcherUserId;
        String launcherPackage = pinShortcutRequestInner.launcherPackage;
        String shortcutId = original.getId();
        synchronized (this.mLock) {
            if (this.mService.isUserUnlockedL(appUserId) && this.mService.isUserUnlockedL(pinShortcutRequestInner.launcherUserId)) {
                ShortcutLauncher launcher = this.mService.getLauncherShortcutsLocked(launcherPackage, appUserId, launcherUserId);
                launcher.attemptToRestoreIfNeededAndSave();
                if (launcher.hasPinned(original)) {
                    return true;
                }
                ShortcutPackage ps = this.mService.getPackageShortcutsForPublisherLocked(appPackageName, appUserId);
                ShortcutInfo current = ps.findShortcutById(shortcutId);
                if (current == null) {
                    try {
                        this.mService.validateShortcutForPinRequest(original);
                    } catch (RuntimeException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to pin shortcut: ");
                        stringBuilder.append(e.getMessage());
                        Log.w(str, stringBuilder.toString());
                        return false;
                    }
                }
                validateExistingShortcut(current);
                if (current == null) {
                    if (original.getActivity() == null) {
                        original.setActivity(this.mService.getDummyMainActivity(appPackageName));
                    }
                    ps.addOrReplaceDynamicShortcut(original);
                }
                launcher.addPinnedShortcut(appPackageName, appUserId, shortcutId, true);
                if (current == null) {
                    ps.deleteDynamicWithId(shortcutId, false);
                }
                ps.adjustRanks();
                this.mService.verifyStates();
                this.mService.packageShortcutsChanged(appPackageName, appUserId);
                return true;
            }
            Log.w(TAG, "User is locked now.");
            return false;
        }
    }
}
