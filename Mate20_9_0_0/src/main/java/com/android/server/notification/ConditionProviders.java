package com.android.server.notification;

import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.IConditionProvider.Stub;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.notification.ManagedServices.Config;
import com.android.server.notification.ManagedServices.ManagedServiceInfo;
import com.android.server.notification.ManagedServices.UserProfiles;
import com.android.server.notification.NotificationManagerService.DumpFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class ConditionProviders extends ManagedServices {
    @VisibleForTesting
    static final String TAG_ENABLED_DND_APPS = "dnd_apps";
    private Callback mCallback;
    private final ArrayList<ConditionRecord> mRecords;
    private final ArraySet<String> mSystemConditionProviderNames;
    private final ArraySet<SystemConditionProviderService> mSystemConditionProviders;

    public interface Callback {
        void onBootComplete();

        void onConditionChanged(Uri uri, Condition condition);

        void onServiceAdded(ComponentName componentName);

        void onUserSwitched();
    }

    private static class ConditionRecord {
        public final ComponentName component;
        public Condition condition;
        public final Uri id;
        public ManagedServiceInfo info;
        public boolean subscribed;

        private ConditionRecord(Uri id, ComponentName component) {
            this.id = id;
            this.component = component;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("ConditionRecord[id=");
            sb.append(this.id);
            sb.append(",component=");
            sb.append(this.component);
            sb.append(",subscribed=");
            sb = sb.append(this.subscribed);
            sb.append(']');
            return sb.toString();
        }
    }

    public ConditionProviders(Context context, UserProfiles userProfiles, IPackageManager pm) {
        super(context, new Object(), userProfiles, pm);
        this.mRecords = new ArrayList();
        this.mSystemConditionProviders = new ArraySet();
        this.mSystemConditionProviderNames = safeSet(PropConfig.getStringArray(this.mContext, "system.condition.providers", 17236038));
        this.mApprovalLevel = 0;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public boolean isSystemProviderEnabled(String path) {
        return this.mSystemConditionProviderNames.contains(path);
    }

    public void addSystemProvider(SystemConditionProviderService service) {
        this.mSystemConditionProviders.add(service);
        service.attachBase(this.mContext);
        registerService(service.asInterface(), service.getComponent(), 0);
    }

    public Iterable<SystemConditionProviderService> getSystemProviders() {
        return this.mSystemConditionProviders;
    }

    protected Config getConfig() {
        Config c = new Config();
        c.caption = "condition provider";
        c.serviceInterface = "android.service.notification.ConditionProviderService";
        c.secureSettingName = "enabled_notification_policy_access_packages";
        c.xmlTag = TAG_ENABLED_DND_APPS;
        c.secondarySettingName = "enabled_notification_listeners";
        c.bindPermission = "android.permission.BIND_CONDITION_PROVIDER_SERVICE";
        c.settingsAction = "android.settings.ACTION_CONDITION_PROVIDER_SETTINGS";
        c.clientLabel = 17039762;
        return c;
    }

    public void dump(PrintWriter pw, DumpFilter filter) {
        int i;
        super.dump(pw, filter);
        synchronized (this.mMutex) {
            pw.print("    mRecords(");
            pw.print(this.mRecords.size());
            pw.println("):");
            i = 0;
            for (int i2 = 0; i2 < this.mRecords.size(); i2++) {
                ConditionRecord r = (ConditionRecord) this.mRecords.get(i2);
                if (filter == null || filter.matches(r.component)) {
                    pw.print("      ");
                    pw.println(r);
                    String countdownDesc = CountdownConditionProvider.tryParseDescription(r.id);
                    if (countdownDesc != null) {
                        pw.print("        (");
                        pw.print(countdownDesc);
                        pw.println(")");
                    }
                }
            }
        }
        pw.print("    mSystemConditionProviders: ");
        pw.println(this.mSystemConditionProviderNames);
        while (true) {
            int i3 = i;
            if (i3 < this.mSystemConditionProviders.size()) {
                ((SystemConditionProviderService) this.mSystemConditionProviders.valueAt(i3)).dump(pw, filter);
                i = i3 + 1;
            } else {
                return;
            }
        }
    }

    protected IInterface asInterface(IBinder binder) {
        return Stub.asInterface(binder);
    }

    protected boolean checkType(IInterface service) {
        return service instanceof IConditionProvider;
    }

    public void onBootPhaseAppsCanStart() {
        super.onBootPhaseAppsCanStart();
        for (int i = 0; i < this.mSystemConditionProviders.size(); i++) {
            ((SystemConditionProviderService) this.mSystemConditionProviders.valueAt(i)).onBootComplete();
        }
        if (this.mCallback != null) {
            this.mCallback.onBootComplete();
        }
    }

    public void onUserSwitched(int user) {
        super.onUserSwitched(user);
        if (this.mCallback != null) {
            this.mCallback.onUserSwitched();
        }
    }

    protected void onServiceAdded(ManagedServiceInfo info) {
        try {
            provider(info).onConnected();
        } catch (RemoteException e) {
        }
        if (this.mCallback != null) {
            this.mCallback.onServiceAdded(info.component);
        }
    }

    protected void onServiceRemovedLocked(ManagedServiceInfo removed) {
        if (removed != null) {
            for (int i = this.mRecords.size() - 1; i >= 0; i--) {
                if (((ConditionRecord) this.mRecords.get(i)).component.equals(removed.component)) {
                    this.mRecords.remove(i);
                }
            }
        }
    }

    public void onPackagesChanged(boolean removingPackage, String[] pkgList, int[] uid) {
        if (removingPackage) {
            INotificationManager inm = NotificationManager.getService();
            if (pkgList != null && pkgList.length > 0) {
                for (String pkgName : pkgList) {
                    try {
                        inm.removeAutomaticZenRules(pkgName);
                        inm.setNotificationPolicyAccessGranted(pkgName, false);
                    } catch (Exception e) {
                        String str = this.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to clean up rules for ");
                        stringBuilder.append(pkgName);
                        Slog.e(str, stringBuilder.toString(), e);
                    }
                }
            }
        }
        super.onPackagesChanged(removingPackage, pkgList, uid);
    }

    protected boolean isValidEntry(String packageOrComponent, int userId) {
        return true;
    }

    public ManagedServiceInfo checkServiceToken(IConditionProvider provider) {
        ManagedServiceInfo checkServiceTokenLocked;
        synchronized (this.mMutex) {
            checkServiceTokenLocked = checkServiceTokenLocked(provider);
        }
        return checkServiceTokenLocked;
    }

    private Condition[] removeDuplicateConditions(String pkg, Condition[] conditions) {
        if (conditions == null || conditions.length == 0) {
            return null;
        }
        int N = conditions.length;
        ArrayMap<Uri, Condition> valid = new ArrayMap(N);
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            Uri id = conditions[i2].id;
            if (valid.containsKey(id)) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ignoring condition from ");
                stringBuilder.append(pkg);
                stringBuilder.append(" for duplicate id: ");
                stringBuilder.append(id);
                Slog.w(str, stringBuilder.toString());
            } else {
                valid.put(id, conditions[i2]);
            }
        }
        if (valid.size() == 0) {
            return null;
        }
        if (valid.size() == N) {
            return conditions;
        }
        Condition[] rt = new Condition[valid.size()];
        while (i < rt.length) {
            rt[i] = (Condition) valid.valueAt(i);
            i++;
        }
        return rt;
    }

    private ConditionRecord getRecordLocked(Uri id, ComponentName component, boolean create) {
        if (id == null || component == null) {
            return null;
        }
        int N = this.mRecords.size();
        for (int i = 0; i < N; i++) {
            ConditionRecord r = (ConditionRecord) this.mRecords.get(i);
            if (r.id.equals(id) && r.component.equals(component)) {
                return r;
            }
        }
        if (!create) {
            return null;
        }
        ConditionRecord r2 = new ConditionRecord(id, component);
        this.mRecords.add(r2);
        return r2;
    }

    /* JADX WARNING: Missing block: B:18:0x0059, code:
            r0 = r11.length;
     */
    /* JADX WARNING: Missing block: B:19:0x005c, code:
            if (r8.mCallback == null) goto L_0x006d;
     */
    /* JADX WARNING: Missing block: B:21:0x0062, code:
            if ((r8.mCallback instanceof com.android.server.notification.ZenModeConditions) == false) goto L_0x006d;
     */
    /* JADX WARNING: Missing block: B:22:0x0064, code:
            ((com.android.server.notification.ZenModeConditions) r8.mCallback).onConditionChanged(r11);
     */
    /* JADX WARNING: Missing block: B:23:0x006d, code:
            r1 = r2;
     */
    /* JADX WARNING: Missing block: B:24:0x006e, code:
            if (r1 >= r0) goto L_0x0080;
     */
    /* JADX WARNING: Missing block: B:25:0x0070, code:
            r2 = r11[r1];
     */
    /* JADX WARNING: Missing block: B:26:0x0074, code:
            if (r8.mCallback == null) goto L_0x007d;
     */
    /* JADX WARNING: Missing block: B:27:0x0076, code:
            r8.mCallback.onConditionChanged(r2.id, r2);
     */
    /* JADX WARNING: Missing block: B:28:0x007d, code:
            r2 = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:29:0x0080, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyConditions(String pkg, ManagedServiceInfo info, Condition[] conditions) {
        synchronized (this.mMutex) {
            if (this.DEBUG) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("notifyConditions pkg=");
                stringBuilder.append(pkg);
                stringBuilder.append(" info=");
                stringBuilder.append(info);
                stringBuilder.append(" conditions=");
                stringBuilder.append(conditions == null ? null : Arrays.asList(conditions));
                Slog.d(str, stringBuilder.toString());
            }
            conditions = removeDuplicateConditions(pkg, conditions);
            if (conditions == null || conditions.length == 0) {
                return;
            }
            int i = 0;
            for (Condition c : conditions) {
                ConditionRecord r = getRecordLocked(c.id, info.component, true);
                r.info = info;
                r.condition = c;
            }
        }
    }

    public IConditionProvider findConditionProvider(ComponentName component) {
        if (component == null) {
            return null;
        }
        for (ManagedServiceInfo service : getServices()) {
            if (component.equals(service.component)) {
                return provider(service);
            }
        }
        return null;
    }

    public Condition findCondition(ComponentName component, Uri conditionId) {
        Condition condition = null;
        if (component == null || conditionId == null) {
            return null;
        }
        synchronized (this.mMutex) {
            ConditionRecord r = getRecordLocked(conditionId, component, null);
            if (r != null) {
                condition = r.condition;
            }
        }
        return condition;
    }

    public void ensureRecordExists(ComponentName component, Uri conditionId, IConditionProvider provider) {
        ConditionRecord r = getRecordLocked(conditionId, component, true);
        if (r.info == null) {
            r.info = checkServiceTokenLocked(provider);
        }
    }

    public boolean subscribeIfNecessary(ComponentName component, Uri conditionId) {
        synchronized (this.mMutex) {
            ConditionRecord r = getRecordLocked(conditionId, component, false);
            if (r == null) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to subscribe to ");
                stringBuilder.append(component);
                stringBuilder.append(" ");
                stringBuilder.append(conditionId);
                Slog.w(str, stringBuilder.toString());
                return false;
            } else if (r.subscribed) {
                return true;
            } else {
                subscribeLocked(r);
                boolean z = r.subscribed;
                return z;
            }
        }
    }

    public void unsubscribeIfNecessary(ComponentName component, Uri conditionId) {
        synchronized (this.mMutex) {
            ConditionRecord r = getRecordLocked(conditionId, component, null);
            if (r == null) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to unsubscribe to ");
                stringBuilder.append(component);
                stringBuilder.append(" ");
                stringBuilder.append(conditionId);
                Slog.w(str, stringBuilder.toString());
            } else if (r.subscribed) {
                unsubscribeLocked(r);
            }
        }
    }

    private void subscribeLocked(ConditionRecord r) {
        if (this.DEBUG) {
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("subscribeLocked ");
            stringBuilder.append(r);
            Slog.d(str, stringBuilder.toString());
        }
        IConditionProvider provider = provider(r);
        RemoteException re = null;
        if (provider != null) {
            try {
                String str2 = this.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Subscribing to ");
                stringBuilder2.append(r.id);
                stringBuilder2.append(" with ");
                stringBuilder2.append(r.component);
                Slog.d(str2, stringBuilder2.toString());
                provider.onSubscribe(r.id);
                r.subscribed = true;
            } catch (RemoteException e) {
                String str3 = this.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Error subscribing to ");
                stringBuilder3.append(r);
                Slog.w(str3, stringBuilder3.toString(), e);
                re = e;
            }
        }
        ZenLog.traceSubscribe(r != null ? r.id : null, provider, re);
    }

    @SafeVarargs
    private static <T> ArraySet<T> safeSet(T... items) {
        ArraySet<T> rt = new ArraySet();
        if (items == null || items.length == 0) {
            return rt;
        }
        for (T item : items) {
            if (item != null) {
                rt.add(item);
            }
        }
        return rt;
    }

    private void unsubscribeLocked(ConditionRecord r) {
        if (this.DEBUG) {
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unsubscribeLocked ");
            stringBuilder.append(r);
            Slog.d(str, stringBuilder.toString());
        }
        IConditionProvider provider = provider(r);
        RemoteException re = null;
        if (provider != null) {
            try {
                provider.onUnsubscribe(r.id);
            } catch (RemoteException e) {
                String str2 = this.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error unsubscribing to ");
                stringBuilder2.append(r);
                Slog.w(str2, stringBuilder2.toString(), e);
                re = e;
            }
            r.subscribed = false;
        }
        ZenLog.traceUnsubscribe(r != null ? r.id : null, provider, re);
    }

    private static IConditionProvider provider(ConditionRecord r) {
        return r == null ? null : provider(r.info);
    }

    private static IConditionProvider provider(ManagedServiceInfo info) {
        return info == null ? null : (IConditionProvider) info.service;
    }
}
