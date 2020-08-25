package com.android.server.search;

import android.app.AppGlobals;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.LocalServices;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Searchables {
    public static String ENHANCED_GOOGLE_SEARCH_COMPONENT_NAME = "com.google.android.providers.enhancedgooglesearch/.Launcher";
    private static final Comparator<ResolveInfo> GLOBAL_SEARCH_RANKER = new Comparator<ResolveInfo>() {
        /* class com.android.server.search.Searchables.AnonymousClass1 */

        public int compare(ResolveInfo lhs, ResolveInfo rhs) {
            if (lhs == rhs) {
                return 0;
            }
            boolean lhsSystem = Searchables.isSystemApp(lhs);
            boolean rhsSystem = Searchables.isSystemApp(rhs);
            if (lhsSystem && !rhsSystem) {
                return -1;
            }
            if (!rhsSystem || lhsSystem) {
                return rhs.priority - lhs.priority;
            }
            return 1;
        }
    };
    public static String GOOGLE_SEARCH_COMPONENT_NAME = "com.android.googlesearch/.GoogleSearch";
    private static final String LOG_TAG = "Searchables";
    private static final String MD_LABEL_DEFAULT_SEARCHABLE = "android.app.default_searchable";
    private static final String MD_SEARCHABLE_SYSTEM_SEARCH = "*";
    private Context mContext;
    private ComponentName mCurrentGlobalSearchActivity = null;
    private List<ResolveInfo> mGlobalSearchActivities;
    private ArrayList<SearchableInfo> mOnlineSearchablesInGlobalSearchList = null;
    private final IPackageManager mPm;
    private ArrayList<SearchableInfo> mSearchablesInGlobalSearchList = null;
    private ArrayList<SearchableInfo> mSearchablesList = null;
    private HashMap<ComponentName, SearchableInfo> mSearchablesMap = null;
    private int mUserId;
    private ComponentName mWebSearchActivity = null;

    public Searchables(Context context, int userId) {
        this.mContext = context;
        this.mUserId = userId;
        this.mPm = AppGlobals.getPackageManager();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:13:?, code lost:
        r3 = r11.mPm.getActivityInfo(r12, 128, r11.mUserId);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0036, code lost:
        r3 = null;
        r4 = r3.metaData;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x003b, code lost:
        if (r4 == null) goto L_0x0043;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x003d, code lost:
        r3 = r4.getString(com.android.server.search.Searchables.MD_LABEL_DEFAULT_SEARCHABLE);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0043, code lost:
        if (r3 != null) goto L_0x0051;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0045, code lost:
        r4 = r3.applicationInfo.metaData;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0049, code lost:
        if (r4 == null) goto L_0x0051;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x004b, code lost:
        r3 = r4.getString(com.android.server.search.Searchables.MD_LABEL_DEFAULT_SEARCHABLE);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0051, code lost:
        if (r3 == null) goto L_0x00b6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0059, code lost:
        if (r3.equals(com.android.server.search.Searchables.MD_SEARCHABLE_SYSTEM_SEARCH) == false) goto L_0x005c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x005b, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x005c, code lost:
        r5 = r12.getPackageName();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0067, code lost:
        if (r3.charAt(0) != '.') goto L_0x007e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0069, code lost:
        r6 = new android.content.ComponentName(r5, r5 + r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x007e, code lost:
        r6 = new android.content.ComponentName(r5, r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0083, code lost:
        monitor-enter(r11);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:?, code lost:
        r7 = r11.mSearchablesMap.get(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x008d, code lost:
        if (r7 == null) goto L_0x0094;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x008f, code lost:
        r11.mSearchablesMap.put(r12, r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0094, code lost:
        monitor-exit(r11);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x0095, code lost:
        if (r7 == null) goto L_0x00b6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x00af, code lost:
        if (((android.content.pm.PackageManagerInternal) com.android.server.LocalServices.getService(android.content.pm.PackageManagerInternal.class)).canAccessComponent(android.os.Binder.getCallingUid(), r7.getSearchActivity(), android.os.UserHandle.getCallingUserId()) == false) goto L_0x00b2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00b1, code lost:
        return r7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00b2, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00b6, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x00b7, code lost:
        r3 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00b8, code lost:
        android.util.Log.e(com.android.server.search.Searchables.LOG_TAG, "Error getting activity info " + r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00ce, code lost:
        return null;
     */
    public SearchableInfo getSearchableInfo(ComponentName activity) {
        synchronized (this) {
            SearchableInfo result = this.mSearchablesMap.get(activity);
            if (result != null) {
                if (((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).canAccessComponent(Binder.getCallingUid(), result.getSearchActivity(), UserHandle.getCallingUserId())) {
                    return result;
                }
                return null;
            }
        }
    }

    /* JADX INFO: Multiple debug info for r12v3 android.content.pm.ActivityInfo: [D('webSearchInfoList' java.util.List<android.content.pm.ResolveInfo>), D('ai2' android.content.pm.ActivityInfo)] */
    /* JADX INFO: Multiple debug info for r7v4 android.content.pm.ActivityInfo: [D('ai' android.content.pm.ActivityInfo), D('intent' android.content.Intent)] */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x00ea A[Catch:{ all -> 0x015c }] */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x013e A[Catch:{ all -> 0x015c }] */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x0145 A[SYNTHETIC] */
    public void updateSearchableList() {
        List<ResolveInfo> onlineSearchInfoList;
        SearchableInfo searchable_online;
        int web_search_count;
        ResolveInfo info;
        SearchableInfo searchable;
        HashMap<ComponentName, SearchableInfo> newSearchablesMap = new HashMap<>();
        ArrayList<SearchableInfo> newSearchablesList = new ArrayList<>();
        ArrayList<SearchableInfo> newSearchablesInGlobalSearchList = new ArrayList<>();
        HashMap<ComponentName, SearchableInfo> newOnlineSearchablesMap = new HashMap<>();
        ArrayList<SearchableInfo> newOnlineSearchablesInGlobalSearchList = new ArrayList<>();
        Intent intent = new Intent("android.intent.action.SEARCH");
        long ident = Binder.clearCallingIdentity();
        try {
            List<ResolveInfo> searchList = queryIntentActivities(intent, 268435584);
            Intent webSearchIntent = new Intent("android.intent.action.WEB_SEARCH");
            List<ResolveInfo> webSearchInfoList = queryIntentActivities(webSearchIntent, 268435584);
            if (searchList == null) {
                if (webSearchInfoList == null) {
                    List<ResolveInfo> newGlobalSearchActivities = findGlobalSearchActivities();
                    ComponentName newGlobalSearchActivity = findGlobalSearchActivity(newGlobalSearchActivities);
                    ComponentName newWebSearchActivity = findWebSearchActivity(newGlobalSearchActivity);
                    Intent onlineSearchIntent = new Intent("huawei.intent.action.ONLINESEARCH");
                    onlineSearchInfoList = queryIntentActivities(onlineSearchIntent, 128);
                    if (onlineSearchInfoList == null) {
                        int online_search_count = onlineSearchInfoList.size();
                        int i2 = 0;
                        while (i2 < online_search_count) {
                            ActivityInfo ai2 = onlineSearchInfoList.get(i2).activityInfo;
                            if (newOnlineSearchablesMap.get(new ComponentName(ai2.packageName, ai2.name)) == null && (searchable_online = SearchableInfo.getActivityMetaDataOnline(this.mContext, ai2, this.mUserId)) != null) {
                                newOnlineSearchablesInGlobalSearchList.add(searchable_online);
                                newOnlineSearchablesMap.put(searchable_online.getSearchActivity(), searchable_online);
                            }
                            i2++;
                            online_search_count = online_search_count;
                            webSearchInfoList = webSearchInfoList;
                            onlineSearchIntent = onlineSearchIntent;
                            onlineSearchInfoList = onlineSearchInfoList;
                        }
                    }
                    synchronized (this) {
                        this.mSearchablesMap = newSearchablesMap;
                        this.mSearchablesList = newSearchablesList;
                        this.mSearchablesInGlobalSearchList = newSearchablesInGlobalSearchList;
                        this.mOnlineSearchablesInGlobalSearchList = newOnlineSearchablesInGlobalSearchList;
                        this.mGlobalSearchActivities = newGlobalSearchActivities;
                        this.mCurrentGlobalSearchActivity = newGlobalSearchActivity;
                        this.mWebSearchActivity = newWebSearchActivity;
                    }
                    Binder.restoreCallingIdentity(ident);
                    return;
                }
            }
            int web_search_count2 = 0;
            int search_count = searchList == null ? 0 : searchList.size();
            if (webSearchInfoList != null) {
                web_search_count2 = webSearchInfoList.size();
            }
            int count = search_count + web_search_count2;
            int ii = 0;
            while (ii < count) {
                if (ii < search_count) {
                    try {
                        web_search_count = web_search_count2;
                        info = searchList.get(ii);
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(ident);
                        throw th;
                    }
                } else {
                    web_search_count = web_search_count2;
                    info = webSearchInfoList.get(ii - search_count);
                }
                try {
                    ActivityInfo ai = info.activityInfo;
                    if (newSearchablesMap.get(new ComponentName(ai.packageName, ai.name)) == null && (searchable = SearchableInfo.getActivityMetaData(this.mContext, ai, this.mUserId)) != null) {
                        newSearchablesList.add(searchable);
                        newSearchablesMap.put(searchable.getSearchActivity(), searchable);
                        if (searchable.shouldIncludeInGlobalSearch()) {
                            newSearchablesInGlobalSearchList.add(searchable);
                        }
                    }
                    ii++;
                    web_search_count2 = web_search_count;
                    intent = intent;
                    searchList = searchList;
                    webSearchIntent = webSearchIntent;
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            }
            List<ResolveInfo> newGlobalSearchActivities2 = findGlobalSearchActivities();
            ComponentName newGlobalSearchActivity2 = findGlobalSearchActivity(newGlobalSearchActivities2);
            ComponentName newWebSearchActivity2 = findWebSearchActivity(newGlobalSearchActivity2);
            Intent onlineSearchIntent2 = new Intent("huawei.intent.action.ONLINESEARCH");
            onlineSearchInfoList = queryIntentActivities(onlineSearchIntent2, 128);
            if (onlineSearchInfoList == null) {
            }
            synchronized (this) {
            }
        } catch (Throwable th3) {
            th = th3;
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    private List<ResolveInfo> findGlobalSearchActivities() {
        List<ResolveInfo> activities = queryIntentActivities(new Intent("android.search.action.GLOBAL_SEARCH"), 268500992);
        if (activities != null && !activities.isEmpty()) {
            Collections.sort(activities, GLOBAL_SEARCH_RANKER);
        }
        return activities;
    }

    private ComponentName findGlobalSearchActivity(List<ResolveInfo> installed) {
        ComponentName globalSearchComponent;
        String searchProviderSetting = getGlobalSearchProviderSetting();
        if (TextUtils.isEmpty(searchProviderSetting) || (globalSearchComponent = ComponentName.unflattenFromString(searchProviderSetting)) == null || !isInstalled(globalSearchComponent)) {
            return getDefaultGlobalSearchProvider(installed);
        }
        return globalSearchComponent;
    }

    private boolean isInstalled(ComponentName globalSearch) {
        Intent intent = new Intent("android.search.action.GLOBAL_SEARCH");
        intent.setComponent(globalSearch);
        List<ResolveInfo> activities = queryIntentActivities(intent, 65536);
        if (activities == null || activities.isEmpty()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public static final boolean isSystemApp(ResolveInfo res) {
        return (res.activityInfo.applicationInfo.flags & 1) != 0;
    }

    private ComponentName getDefaultGlobalSearchProvider(List<ResolveInfo> providerList) {
        if (providerList == null || providerList.isEmpty()) {
            Log.w(LOG_TAG, "No global search activity found");
            return null;
        }
        ActivityInfo ai = providerList.get(0).activityInfo;
        return new ComponentName(ai.packageName, ai.name);
    }

    private String getGlobalSearchProviderSetting() {
        return Settings.Secure.getString(this.mContext.getContentResolver(), "search_global_search_activity");
    }

    private ComponentName findWebSearchActivity(ComponentName globalSearchActivity) {
        if (globalSearchActivity == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.WEB_SEARCH");
        intent.setPackage(globalSearchActivity.getPackageName());
        List<ResolveInfo> activities = queryIntentActivities(intent, 65536);
        if (activities == null || activities.isEmpty()) {
            Log.w(LOG_TAG, "No web search activity found");
            return null;
        }
        ActivityInfo ai = activities.get(0).activityInfo;
        return new ComponentName(ai.packageName, ai.name);
    }

    private List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        try {
            return this.mPm.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 8388608 | flags, this.mUserId).getList();
        } catch (RemoteException e) {
            return null;
        }
    }

    public synchronized ArrayList<SearchableInfo> getSearchablesList() {
        return createFilterdSearchableInfoList(this.mSearchablesList);
    }

    public synchronized ArrayList<SearchableInfo> getSearchablesInGlobalSearchList() {
        return createFilterdSearchableInfoList(this.mSearchablesInGlobalSearchList);
    }

    public synchronized ArrayList<SearchableInfo> getOnlineSearchablesInGlobalSearchList() {
        return new ArrayList<>(this.mOnlineSearchablesInGlobalSearchList);
    }

    public synchronized ArrayList<ResolveInfo> getGlobalSearchActivities() {
        return createFilterdResolveInfoList(this.mGlobalSearchActivities);
    }

    private ArrayList<SearchableInfo> createFilterdSearchableInfoList(List<SearchableInfo> list) {
        if (list == null) {
            return null;
        }
        ArrayList<SearchableInfo> resultList = new ArrayList<>(list.size());
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        for (SearchableInfo info : list) {
            if (pm.canAccessComponent(callingUid, info.getSearchActivity(), callingUserId)) {
                resultList.add(info);
            }
        }
        return resultList;
    }

    private ArrayList<ResolveInfo> createFilterdResolveInfoList(List<ResolveInfo> list) {
        if (list == null) {
            return null;
        }
        ArrayList<ResolveInfo> resultList = new ArrayList<>(list.size());
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        for (ResolveInfo info : list) {
            if (pm.canAccessComponent(callingUid, info.activityInfo.getComponentName(), callingUserId)) {
                resultList.add(info);
            }
        }
        return resultList;
    }

    public synchronized ComponentName getGlobalSearchActivity() {
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        if (this.mCurrentGlobalSearchActivity == null || !pm.canAccessComponent(callingUid, this.mCurrentGlobalSearchActivity, callingUserId)) {
            return null;
        }
        return this.mCurrentGlobalSearchActivity;
    }

    public synchronized ComponentName getWebSearchActivity() {
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        if (this.mWebSearchActivity == null || !pm.canAccessComponent(callingUid, this.mWebSearchActivity, callingUserId)) {
            return null;
        }
        return this.mWebSearchActivity;
    }

    /* access modifiers changed from: package-private */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Searchable authorities:");
        synchronized (this) {
            if (this.mSearchablesList != null) {
                Iterator<SearchableInfo> it = this.mSearchablesList.iterator();
                while (it.hasNext()) {
                    pw.print("  ");
                    pw.println(it.next().getSuggestAuthority());
                }
            }
        }
    }
}
