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
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.pm.DumpState;
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

    /* JADX WARNING: Missing block: B:11:0x002b, code:
            r2 = null;
     */
    /* JADX WARNING: Missing block: B:14:0x0036, code:
            r2 = r11.mPm.getActivityInfo(r12, 128, r11.mUserId);
     */
    /* JADX WARNING: Missing block: B:15:0x0038, code:
            if (r2 != null) goto L_0x0042;
     */
    /* JADX WARNING: Missing block: B:16:0x003a, code:
            android.util.Log.v(LOG_TAG, "getted activity info is null");
     */
    /* JADX WARNING: Missing block: B:17:0x0041, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:18:0x0042, code:
            r3 = null;
            r4 = r2.metaData;
     */
    /* JADX WARNING: Missing block: B:19:0x0045, code:
            if (r4 == null) goto L_0x004d;
     */
    /* JADX WARNING: Missing block: B:20:0x0047, code:
            r3 = r4.getString(MD_LABEL_DEFAULT_SEARCHABLE);
     */
    /* JADX WARNING: Missing block: B:21:0x004d, code:
            if (r3 != null) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:22:0x004f, code:
            r4 = r2.applicationInfo.metaData;
     */
    /* JADX WARNING: Missing block: B:23:0x0053, code:
            if (r4 == null) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:24:0x0055, code:
            r3 = r4.getString(MD_LABEL_DEFAULT_SEARCHABLE);
     */
    /* JADX WARNING: Missing block: B:25:0x005b, code:
            if (r3 == null) goto L_0x00c0;
     */
    /* JADX WARNING: Missing block: B:27:0x0063, code:
            if (r3.equals(MD_SEARCHABLE_SYSTEM_SEARCH) == false) goto L_0x0066;
     */
    /* JADX WARNING: Missing block: B:28:0x0065, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:29:0x0066, code:
            r5 = r12.getPackageName();
     */
    /* JADX WARNING: Missing block: B:30:0x0071, code:
            if (r3.charAt(0) != '.') goto L_0x0088;
     */
    /* JADX WARNING: Missing block: B:31:0x0073, code:
            r7 = new java.lang.StringBuilder();
            r7.append(r5);
            r7.append(r3);
            r6 = new android.content.ComponentName(r5, r7.toString());
     */
    /* JADX WARNING: Missing block: B:32:0x0088, code:
            r6 = new android.content.ComponentName(r5, r3);
     */
    /* JADX WARNING: Missing block: B:33:0x008d, code:
            monitor-enter(r11);
     */
    /* JADX WARNING: Missing block: B:35:?, code:
            r0 = (android.app.SearchableInfo) r11.mSearchablesMap.get(r6);
     */
    /* JADX WARNING: Missing block: B:36:0x0097, code:
            if (r0 == null) goto L_0x009e;
     */
    /* JADX WARNING: Missing block: B:37:0x0099, code:
            r11.mSearchablesMap.put(r12, r0);
     */
    /* JADX WARNING: Missing block: B:38:0x009e, code:
            monitor-exit(r11);
     */
    /* JADX WARNING: Missing block: B:39:0x009f, code:
            if (r0 == null) goto L_0x00c0;
     */
    /* JADX WARNING: Missing block: B:41:0x00b9, code:
            if (((android.content.pm.PackageManagerInternal) com.android.server.LocalServices.getService(android.content.pm.PackageManagerInternal.class)).canAccessComponent(android.os.Binder.getCallingUid(), r0.getSearchActivity(), android.os.UserHandle.getCallingUserId()) == false) goto L_0x00bc;
     */
    /* JADX WARNING: Missing block: B:42:0x00bb, code:
            return r0;
     */
    /* JADX WARNING: Missing block: B:43:0x00bc, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:48:0x00c0, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:49:0x00c1, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:50:0x00c2, code:
            r4 = LOG_TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("Error getting activity info ");
            r5.append(r3);
            android.util.Log.e(r4, r5.toString());
     */
    /* JADX WARNING: Missing block: B:51:0x00d8, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SearchableInfo getSearchableInfo(ComponentName activity) {
        synchronized (this) {
            SearchableInfo result = (SearchableInfo) this.mSearchablesMap.get(activity);
            if (result != null) {
                if (((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).canAccessComponent(Binder.getCallingUid(), result.getSearchActivity(), UserHandle.getCallingUserId())) {
                    return result;
                }
                return null;
            }
        }
    }

    public void updateSearchableList() {
        Throwable th;
        HashMap<ComponentName, SearchableInfo> newSearchablesMap = new HashMap();
        ArrayList<SearchableInfo> newSearchablesList = new ArrayList();
        ArrayList<SearchableInfo> newSearchablesInGlobalSearchList = new ArrayList();
        HashMap<ComponentName, SearchableInfo> newOnlineSearchablesMap = new HashMap();
        ArrayList<SearchableInfo> newOnlineSearchablesInGlobalSearchList = new ArrayList();
        Intent intent = new Intent("android.intent.action.SEARCH");
        long ident = Binder.clearCallingIdentity();
        Intent intent2;
        try {
            int count;
            int ii;
            Intent onlineSearchIntent;
            List<ResolveInfo> onlineSearchInfoList;
            List<ResolveInfo> searchList;
            List<ResolveInfo> searchList2 = queryIntentActivities(intent, 268435584);
            Intent webSearchIntent = new Intent("android.intent.action.WEB_SEARCH");
            List<ResolveInfo> webSearchInfoList = queryIntentActivities(webSearchIntent, 268435584);
            Intent intent3;
            List<ResolveInfo> list;
            if (searchList2 != null || webSearchInfoList != null) {
                int search_count = searchList2 == null ? 0 : searchList2.size();
                count = search_count + (webSearchInfoList == null ? 0 : webSearchInfoList.size());
                int ii2 = 0;
                while (true) {
                    ii = ii2;
                    if (ii >= count) {
                        intent3 = webSearchIntent;
                        list = webSearchInfoList;
                        break;
                    }
                    ResolveInfo resolveInfo;
                    if (ii < search_count) {
                        try {
                            resolveInfo = (ResolveInfo) searchList2.get(ii);
                            intent2 = intent;
                        } catch (Throwable th2) {
                            th = th2;
                            intent2 = intent;
                        }
                    } else {
                        intent2 = intent;
                        try {
                            resolveInfo = (ResolveInfo) webSearchInfoList.get(ii - search_count);
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    ResolveInfo intent4 = resolveInfo;
                    intent3 = webSearchIntent;
                    ActivityInfo ai = intent4.activityInfo;
                    ResolveInfo info = intent4;
                    list = webSearchInfoList;
                    int search_count2 = search_count;
                    if (newSearchablesMap.get(new ComponentName(ai.packageName, ai.name)) == null) {
                        intent = SearchableInfo.getActivityMetaData(this.mContext, ai, this.mUserId);
                        if (intent != null) {
                            newSearchablesList.add(intent);
                            newSearchablesMap.put(intent.getSearchActivity(), intent);
                            if (intent.shouldIncludeInGlobalSearch()) {
                                newSearchablesInGlobalSearchList.add(intent);
                            }
                        }
                    }
                    ii2 = ii + 1;
                    intent = intent2;
                    webSearchIntent = intent3;
                    webSearchInfoList = list;
                    search_count = search_count2;
                }
            } else {
                intent2 = intent;
                intent3 = webSearchIntent;
                list = webSearchInfoList;
            }
            List intent5 = findGlobalSearchActivities();
            ComponentName newGlobalSearchActivity = findGlobalSearchActivity(intent5);
            ComponentName newWebSearchActivity = findWebSearchActivity(newGlobalSearchActivity);
            Intent onlineSearchIntent2 = new Intent("huawei.intent.action.ONLINESEARCH");
            List<ResolveInfo> onlineSearchInfoList2 = queryIntentActivities(onlineSearchIntent2, 128);
            if (onlineSearchInfoList2 != null) {
                ii = onlineSearchInfoList2.size();
                int i2 = 0;
                while (true) {
                    count = i2;
                    if (count >= ii) {
                        break;
                    }
                    int online_search_count = ii;
                    onlineSearchIntent = onlineSearchIntent2;
                    ResolveInfo info2 = (ResolveInfo) onlineSearchInfoList2.get(count);
                    ActivityInfo ai2 = info2.activityInfo;
                    onlineSearchInfoList = onlineSearchInfoList2;
                    searchList = searchList2;
                    if (newOnlineSearchablesMap.get(new ComponentName(ai2.packageName, ai2.name)) == null) {
                        SearchableInfo searchable_online = SearchableInfo.getActivityMetaDataOnline(this.mContext, ai2, this.mUserId);
                        if (searchable_online != null) {
                            newOnlineSearchablesInGlobalSearchList.add(searchable_online);
                            newOnlineSearchablesMap.put(searchable_online.getSearchActivity(), searchable_online);
                        }
                    }
                    i2 = count + 1;
                    ii = online_search_count;
                    onlineSearchIntent2 = onlineSearchIntent;
                    onlineSearchInfoList2 = onlineSearchInfoList;
                    searchList2 = searchList;
                }
            }
            searchList = searchList2;
            onlineSearchIntent = onlineSearchIntent2;
            onlineSearchInfoList = onlineSearchInfoList2;
            synchronized (this) {
                this.mSearchablesMap = newSearchablesMap;
                this.mSearchablesList = newSearchablesList;
                this.mSearchablesInGlobalSearchList = newSearchablesInGlobalSearchList;
                this.mOnlineSearchablesInGlobalSearchList = newOnlineSearchablesInGlobalSearchList;
                this.mGlobalSearchActivities = intent5;
                this.mCurrentGlobalSearchActivity = newGlobalSearchActivity;
                this.mWebSearchActivity = newWebSearchActivity;
            }
            Binder.restoreCallingIdentity(ident);
        } catch (Throwable th4) {
            th = th4;
            intent2 = intent;
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    private List<ResolveInfo> findGlobalSearchActivities() {
        List<ResolveInfo> activities = queryIntentActivities(new Intent("android.search.action.GLOBAL_SEARCH"), 268500992);
        if (!(activities == null || activities.isEmpty())) {
            Collections.sort(activities, GLOBAL_SEARCH_RANKER);
        }
        return activities;
    }

    private ComponentName findGlobalSearchActivity(List<ResolveInfo> installed) {
        String searchProviderSetting = getGlobalSearchProviderSetting();
        if (!TextUtils.isEmpty(searchProviderSetting)) {
            ComponentName globalSearchComponent = ComponentName.unflattenFromString(searchProviderSetting);
            if (globalSearchComponent != null && isInstalled(globalSearchComponent)) {
                return globalSearchComponent;
            }
        }
        return getDefaultGlobalSearchProvider(installed);
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

    private static final boolean isSystemApp(ResolveInfo res) {
        return (res.activityInfo.applicationInfo.flags & 1) != 0;
    }

    private ComponentName getDefaultGlobalSearchProvider(List<ResolveInfo> providerList) {
        if (providerList == null || providerList.isEmpty()) {
            Log.w(LOG_TAG, "No global search activity found");
            return null;
        }
        ActivityInfo ai = ((ResolveInfo) providerList.get(0)).activityInfo;
        return new ComponentName(ai.packageName, ai.name);
    }

    private String getGlobalSearchProviderSetting() {
        return Secure.getString(this.mContext.getContentResolver(), "search_global_search_activity");
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
        ActivityInfo ai = ((ResolveInfo) activities.get(0)).activityInfo;
        return new ComponentName(ai.packageName, ai.name);
    }

    private List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        try {
            return this.mPm.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), DumpState.DUMP_VOLUMES | flags, this.mUserId).getList();
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
        return new ArrayList(this.mOnlineSearchablesInGlobalSearchList);
    }

    public synchronized ArrayList<ResolveInfo> getGlobalSearchActivities() {
        return createFilterdResolveInfoList(this.mGlobalSearchActivities);
    }

    private ArrayList<SearchableInfo> createFilterdSearchableInfoList(List<SearchableInfo> list) {
        if (list == null) {
            return null;
        }
        ArrayList<SearchableInfo> resultList = new ArrayList(list.size());
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
        ArrayList<ResolveInfo> resultList = new ArrayList(list.size());
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

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Searchable authorities:");
        synchronized (this) {
            if (this.mSearchablesList != null) {
                Iterator it = this.mSearchablesList.iterator();
                while (it.hasNext()) {
                    SearchableInfo info = (SearchableInfo) it.next();
                    pw.print("  ");
                    pw.println(info.getSuggestAuthority());
                }
            }
        }
    }
}
