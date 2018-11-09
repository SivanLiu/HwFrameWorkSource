package com.android.server.pm;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPinItemRequest.Stub;
import android.content.pm.LauncherApps.PinItemRequest;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.GuardedBy;
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

    private boolean startRequestConfirmActivity(android.content.ComponentName r9, int r10, android.content.pm.LauncherApps.PinItemRequest r11, int r12) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:16:? in {2, 6, 7, 12, 13, 15, 17, 18} preds:[]
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:129)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.rerun(BlockProcessor.java:44)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:58)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r8 = this;
        r7 = 1;
        if (r12 != r7) goto L_0x0031;
    L_0x0003:
        r0 = "android.content.pm.action.CONFIRM_PIN_SHORTCUT";
    L_0x0006:
        r1 = new android.content.Intent;
        r1.<init>(r0);
        r1.setComponent(r9);
        r3 = "android.content.pm.extra.PIN_ITEM_REQUEST";
        r1.putExtra(r3, r11);
        r3 = 268468224; // 0x10008000 float:2.5342157E-29 double:1.326409265E-315;
        r1.addFlags(r3);
        r3 = r8.mService;
        r4 = r3.injectClearCallingIdentity();
        r3 = r8.mService;	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r3 = r3.mContext;	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r6 = android.os.UserHandle.of(r10);	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r3.startActivityAsUser(r1, r6);	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r3 = r8.mService;
        r3.injectRestoreCallingIdentity(r4);
        return r7;
    L_0x0031:
        r0 = "android.content.pm.action.CONFIRM_PIN_APPWIDGET";
        goto L_0x0006;
    L_0x0035:
        r2 = move-exception;
        r3 = "ShortcutService";	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r6 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r6.<init>();	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r7 = "Unable to start activity ";	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r6 = r6.append(r7);	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r6 = r6.append(r9);	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r6 = r6.toString();	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        android.util.Log.e(r3, r6, r2);	 Catch:{ RuntimeException -> 0x0035, all -> 0x0057 }
        r3 = 0;
        r6 = r8.mService;
        r6.injectRestoreCallingIdentity(r4);
        return r3;
    L_0x0057:
        r3 = move-exception;
        r6 = r8.mService;
        r6.injectRestoreCallingIdentity(r4);
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.ShortcutRequestPinProcessor.startRequestConfirmActivity(android.content.ComponentName, int, android.content.pm.LauncherApps$PinItemRequest, int):boolean");
    }

    public ShortcutRequestPinProcessor(ShortcutService service, Object lock) {
        this.mService = service;
        this.mLock = lock;
    }

    public boolean isRequestPinItemSupported(int callingUserId, int requestType) {
        return getRequestPinConfirmationActivity(callingUserId, requestType) != null;
    }

    public boolean requestPinItemLocked(ShortcutInfo inShortcut, AppWidgetProviderInfo inAppWidget, Bundle extras, int userId, IntentSender resultIntent) {
        int requestType = inShortcut != null ? 1 : 2;
        Pair<ComponentName, Integer> confirmActivity = getRequestPinConfirmationActivity(userId, requestType);
        if (confirmActivity == null) {
            Log.w(TAG, "Launcher doesn't support requestPinnedShortcut(). Shortcut not created.");
            return false;
        }
        PinItemRequest request;
        int launcherUserId = ((Integer) confirmActivity.second).intValue();
        this.mService.throwIfUserLockedL(launcherUserId);
        if (inShortcut != null) {
            request = requestPinShortcutLocked(inShortcut, resultIntent, confirmActivity);
        } else {
            request = new PinItemRequest(new PinAppWidgetRequestInner(resultIntent, this.mService.injectGetPackageUid(((ComponentName) confirmActivity.first).getPackageName(), launcherUserId), inAppWidget, extras), 2);
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
        ShortcutInfo shortcutForLauncher;
        ShortcutInfo existing = this.mService.getPackageShortcutsForPublisherLocked(inShortcut.getPackage(), inShortcut.getUserId()).findShortcutById(inShortcut.getId());
        boolean existsAlready = existing != null;
        String launcherPackage = ((ComponentName) confirmActivity.first).getPackageName();
        int launcherUserId = ((Integer) confirmActivity.second).intValue();
        IntentSender intentSender = resultIntentOriginal;
        if (existsAlready) {
            validateExistingShortcut(existing);
            boolean isAlreadyPinned = this.mService.getLauncherShortcutsLocked(launcherPackage, existing.getUserId(), launcherUserId).hasPinned(existing);
            if (isAlreadyPinned) {
                sendResultIntent(resultIntentOriginal, null);
                intentSender = null;
            }
            shortcutForLauncher = existing.clone(11);
            if (!isAlreadyPinned) {
                shortcutForLauncher.clearFlags(2);
            }
        } else {
            if (inShortcut.getActivity() == null) {
                inShortcut.setActivity(this.mService.injectGetDefaultMainActivity(inShortcut.getPackage(), inShortcut.getUserId()));
            }
            this.mService.validateShortcutForPinRequest(inShortcut);
            inShortcut.resolveResourceStrings(this.mService.injectGetResourcesForApplicationAsUser(inShortcut.getPackage(), inShortcut.getUserId()));
            shortcutForLauncher = inShortcut.clone(10);
        }
        return new PinItemRequest(new PinShortcutRequestInner(inShortcut, shortcutForLauncher, intentSender, launcherPackage, launcherUserId, this.mService.injectGetPackageUid(launcherPackage, launcherUserId), existsAlready), 1);
    }

    private void validateExistingShortcut(ShortcutInfo shortcutInfo) {
        Preconditions.checkArgument(shortcutInfo.isEnabled(), "Shortcut ID=" + shortcutInfo + " already exists but disabled.");
    }

    Pair<ComponentName, Integer> getRequestPinConfirmationActivity(int callingUserId, int requestType) {
        Pair<ComponentName, Integer> pair = null;
        int launcherUserId = this.mService.getParentOrSelfUserId(callingUserId);
        ComponentName defaultLauncher = this.mService.getDefaultLauncher(launcherUserId);
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
        ShortcutInfo original = request.shortcutOriginal;
        int appUserId = original.getUserId();
        String appPackageName = original.getPackage();
        int launcherUserId = request.launcherUserId;
        String launcherPackage = request.launcherPackage;
        String shortcutId = original.getId();
        synchronized (this.mLock) {
            boolean isUserUnlockedL;
            if (this.mService.isUserUnlockedL(appUserId)) {
                isUserUnlockedL = this.mService.isUserUnlockedL(request.launcherUserId);
            } else {
                isUserUnlockedL = false;
            }
            if (isUserUnlockedL) {
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
                        Log.w(TAG, "Unable to pin shortcut: " + e.getMessage());
                        return false;
                    }
                }
                validateExistingShortcut(current);
                if (current == null) {
                    if (original.getActivity() == null) {
                        original.setActivity(this.mService.getDummyMainActivity(appPackageName));
                    }
                    ps.addOrUpdateDynamicShortcut(original);
                }
                launcher.addPinnedShortcut(appPackageName, appUserId, shortcutId);
                if (current == null) {
                    ps.deleteDynamicWithId(shortcutId);
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
