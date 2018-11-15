package com.android.server.pc.whiltestrategy;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class WhiteListAppStrategyManager {
    private static boolean DEBUG = true;
    private static final String TAG = "DefaultXmlFileAppStrategy";
    private static volatile WhiteListAppStrategyManager mInstance;
    private static final Object mInstanceSync = new Object();
    private Context mContext;
    private AppStrategy mDefaultXmlFileAppStrategy;
    private AppStrategy mMetaDataAppStategy = new MetaDataAppStrategy();

    private WhiteListAppStrategyManager(Context context) {
        this.mContext = context;
        this.mDefaultXmlFileAppStrategy = new DefaultXmlFileAppStrategy(context);
    }

    public static WhiteListAppStrategyManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (mInstanceSync) {
                if (mInstance == null) {
                    mInstance = new WhiteListAppStrategyManager(context);
                }
            }
        }
        return mInstance;
    }

    public List<String> getAllSupportPcAppList() {
        if (this.mContext == null) {
            return null;
        }
        List<String> supportPCAppList = new ArrayList();
        for (Entry<String, Integer> entry : this.mDefaultXmlFileAppStrategy.getAppList(this.mContext).entrySet()) {
            String key = (String) entry.getKey();
            if (this.mMetaDataAppStategy.getAppState(key, this.mContext) == 0) {
                supportPCAppList.add(key);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("support pc from white config:");
                    stringBuilder.append(key);
                    Log.i(str, stringBuilder.toString());
                }
            }
        }
        for (Entry<String, Integer> entry2 : this.mMetaDataAppStategy.getAppList(this.mContext).entrySet()) {
            String key2 = (String) entry2.getKey();
            if (((Integer) entry2.getValue()).intValue() == 1) {
                supportPCAppList.add(key2);
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("support pc from metadata:");
                    stringBuilder2.append(key2);
                    Log.i(str2, stringBuilder2.toString());
                }
            }
        }
        return supportPCAppList;
    }

    public int getAppSupportPCState(String packageName) {
        if (this.mContext == null || packageName == null) {
            return -1;
        }
        int appStateFromWhite = this.mDefaultXmlFileAppStrategy.getAppState(packageName, null);
        int appStateFromMeta = this.mMetaDataAppStategy.getAppState(packageName, this.mContext);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAppSupportPCState packageName:");
            stringBuilder.append(packageName);
            stringBuilder.append(" appStateFromWhite:");
            stringBuilder.append(appStateFromWhite);
            stringBuilder.append(" appStateFromMeta:");
            stringBuilder.append(appStateFromMeta);
            Log.i(str, stringBuilder.toString());
        }
        if (appStateFromMeta == 1) {
            return 1;
        }
        if (appStateFromMeta == -1) {
            return -1;
        }
        if (appStateFromMeta == 0 && appStateFromWhite == 1) {
            return 1;
        }
        return (appStateFromMeta != 0 || appStateFromWhite == -1) ? -1 : -1;
    }

    public List<Pair<String, Integer>> getSpecailWindowPolicyAppList() {
        return ((DefaultXmlFileAppStrategy) this.mDefaultXmlFileAppStrategy).getSpecailWindowPolicyAppList();
    }

    public List<String> getMutiResumeAppList() {
        return ((DefaultXmlFileAppStrategy) this.mDefaultXmlFileAppStrategy).getMutiResumeAppList();
    }
}
