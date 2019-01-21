package com.android.internal.app;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.service.resolver.IResolverRankerResult;
import android.service.resolver.IResolverRankerResult.Stub;
import android.service.resolver.IResolverRankerService;
import android.service.resolver.ResolverTarget;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.app.ResolverActivity.ResolvedComponentInfo;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class ResolverComparator implements Comparator<ResolvedComponentInfo> {
    private static final int CONNECTION_COST_TIMEOUT_MILLIS = 200;
    private static final boolean DEBUG = false;
    private static final int NUM_OF_TOP_ANNOTATIONS_TO_USE = 3;
    private static final float RECENCY_MULTIPLIER = 2.0f;
    private static final long RECENCY_TIME_PERIOD = 43200000;
    private static final int RESOLVER_RANKER_RESULT_TIMEOUT = 1;
    private static final int RESOLVER_RANKER_SERVICE_RESULT = 0;
    private static final String TAG = "ResolverComparator";
    private static final long USAGE_STATS_PERIOD = 604800000;
    private static final int WATCHDOG_TIMEOUT_MILLIS = 500;
    private String mAction;
    private AfterCompute mAfterCompute;
    private String[] mAnnotations;
    private final Collator mCollator;
    private CountDownLatch mConnectSignal;
    private ResolverRankerServiceConnection mConnection;
    private String mContentType;
    private Context mContext;
    private final long mCurrentTime;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            int i = 0;
            switch (msg.what) {
                case 0:
                    if (ResolverComparator.this.mHandler.hasMessages(1)) {
                        if (msg.obj != null) {
                            List<ResolverTarget> receivedTargets = msg.obj;
                            if (receivedTargets == null || ResolverComparator.this.mTargets == null || receivedTargets.size() != ResolverComparator.this.mTargets.size()) {
                                Log.e(ResolverComparator.TAG, "Sizes of sent and received ResolverTargets diff.");
                            } else {
                                int size = ResolverComparator.this.mTargets.size();
                                boolean isUpdated = false;
                                while (i < size) {
                                    float predictedProb = ((ResolverTarget) receivedTargets.get(i)).getSelectProbability();
                                    if (predictedProb != ((ResolverTarget) ResolverComparator.this.mTargets.get(i)).getSelectProbability()) {
                                        ((ResolverTarget) ResolverComparator.this.mTargets.get(i)).setSelectProbability(predictedProb);
                                        isUpdated = true;
                                    }
                                    i++;
                                }
                                if (isUpdated) {
                                    ResolverComparator.this.mRankerServiceName = ResolverComparator.this.mResolvedRankerName;
                                }
                            }
                        } else {
                            Log.e(ResolverComparator.TAG, "Receiving null prediction results.");
                        }
                        ResolverComparator.this.mHandler.removeMessages(1);
                        ResolverComparator.this.mAfterCompute.afterCompute();
                        return;
                    }
                    return;
                case 1:
                    ResolverComparator.this.mHandler.removeMessages(0);
                    ResolverComparator.this.mAfterCompute.afterCompute();
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    };
    private final boolean mHttp;
    private final Object mLock = new Object();
    private final PackageManager mPm;
    private IResolverRankerService mRanker;
    private ComponentName mRankerServiceName;
    private final String mReferrerPackage;
    private ComponentName mResolvedRankerName;
    private final long mSinceTime;
    private final Map<String, UsageStats> mStats;
    private ArrayList<ResolverTarget> mTargets;
    private final LinkedHashMap<ComponentName, ResolverTarget> mTargetsDict = new LinkedHashMap();
    private final UsageStatsManager mUsm;

    public interface AfterCompute {
        void afterCompute();
    }

    private class ResolverRankerServiceConnection implements ServiceConnection {
        private final CountDownLatch mConnectSignal;
        public final IResolverRankerResult resolverRankerResult = new Stub() {
            public void sendResult(List<ResolverTarget> targets) throws RemoteException {
                synchronized (ResolverComparator.this.mLock) {
                    Message msg = Message.obtain();
                    msg.what = 0;
                    msg.obj = targets;
                    ResolverComparator.this.mHandler.sendMessage(msg);
                }
            }
        };

        public ResolverRankerServiceConnection(CountDownLatch connectSignal) {
            this.mConnectSignal = connectSignal;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (ResolverComparator.this.mLock) {
                ResolverComparator.this.mRanker = IResolverRankerService.Stub.asInterface(service);
                this.mConnectSignal.countDown();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (ResolverComparator.this.mLock) {
                destroy();
            }
        }

        public void destroy() {
            synchronized (ResolverComparator.this.mLock) {
                ResolverComparator.this.mRanker = null;
            }
        }
    }

    public ResolverComparator(Context context, Intent intent, String referrerPackage, AfterCompute afterCompute) {
        this.mCollator = Collator.getInstance(context.getResources().getConfiguration().locale);
        String scheme = intent.getScheme();
        boolean z = "http".equals(scheme) || "https".equals(scheme);
        this.mHttp = z;
        this.mReferrerPackage = referrerPackage;
        this.mAfterCompute = afterCompute;
        this.mContext = context;
        this.mPm = context.getPackageManager();
        this.mUsm = (UsageStatsManager) context.getSystemService("usagestats");
        this.mCurrentTime = System.currentTimeMillis();
        this.mSinceTime = this.mCurrentTime - USAGE_STATS_PERIOD;
        this.mStats = this.mUsm.queryAndAggregateUsageStats(this.mSinceTime, this.mCurrentTime);
        this.mContentType = intent.getType();
        getContentAnnotations(intent);
        this.mAction = intent.getAction();
        this.mRankerServiceName = new ComponentName(this.mContext, getClass());
    }

    public void getContentAnnotations(Intent intent) {
        ArrayList<String> annotations = intent.getStringArrayListExtra("android.intent.extra.CONTENT_ANNOTATIONS");
        if (annotations != null) {
            int size = annotations.size();
            if (size > 3) {
                size = 3;
            }
            this.mAnnotations = new String[size];
            for (int i = 0; i < size; i++) {
                this.mAnnotations[i] = (String) annotations.get(i);
            }
        }
    }

    public void setCallBack(AfterCompute afterCompute) {
        this.mAfterCompute = afterCompute;
    }

    public void compute(List<ResolvedComponentInfo> targets) {
        reset();
        long recentSinceTime = this.mCurrentTime - RECENCY_TIME_PERIOD;
        Iterator it = targets.iterator();
        float mostRecencyScore = 1.0f;
        float mostTimeSpentScore = 1.0f;
        float mostLaunchScore = 1.0f;
        float mostChooserScore = 1.0f;
        while (it.hasNext()) {
            Iterator it2;
            ResolvedComponentInfo target = (ResolvedComponentInfo) it.next();
            ResolverTarget resolverTarget = new ResolverTarget();
            this.mTargetsDict.put(target.name, resolverTarget);
            UsageStats pkStats = (UsageStats) this.mStats.get(target.name.getPackageName());
            if (pkStats != null) {
                float recencyScore;
                if (target.name.getPackageName().equals(this.mReferrerPackage) || isPersistentProcess(target)) {
                    it2 = it;
                } else {
                    it2 = it;
                    recencyScore = (float) Math.max(pkStats.getLastTimeUsed() - recentSinceTime, 0);
                    resolverTarget.setRecencyScore(recencyScore);
                    if (recencyScore > mostRecencyScore) {
                        mostRecencyScore = recencyScore;
                    }
                }
                recencyScore = (float) pkStats.getTotalTimeInForeground();
                resolverTarget.setTimeSpentScore(recencyScore);
                if (recencyScore > mostTimeSpentScore) {
                    mostTimeSpentScore = recencyScore;
                }
                float launchScore = (float) pkStats.mLaunchCount;
                resolverTarget.setLaunchScore(launchScore);
                if (launchScore > mostLaunchScore) {
                    mostLaunchScore = launchScore;
                }
                float chooserScore = 0.0f;
                UsageStats usageStats;
                if (pkStats.mChooserCounts == null || this.mAction == null || pkStats.mChooserCounts.get(this.mAction) == null) {
                    usageStats = pkStats;
                    float f = recencyScore;
                } else {
                    chooserScore = (float) ((Integer) ((ArrayMap) pkStats.mChooserCounts.get(this.mAction)).getOrDefault(this.mContentType, Integer.valueOf(0))).intValue();
                    if (this.mAnnotations != null) {
                        float chooserScore2 = chooserScore;
                        int i = 0;
                        for (int size = this.mAnnotations.length; i < size; size = size) {
                            chooserScore2 += (float) ((Integer) ((ArrayMap) pkStats.mChooserCounts.get(this.mAction)).getOrDefault(this.mAnnotations[i], Integer.valueOf(0))).intValue();
                            i++;
                            int i2 = 0;
                            pkStats = pkStats;
                        }
                        chooserScore = chooserScore2;
                    } else {
                        usageStats = pkStats;
                    }
                }
                resolverTarget.setChooserScore(chooserScore);
                if (chooserScore > mostChooserScore) {
                    mostChooserScore = chooserScore;
                }
            } else {
                it2 = it;
            }
            it = it2;
        }
        this.mTargets = new ArrayList(this.mTargetsDict.values());
        Iterator it3 = this.mTargets.iterator();
        while (it3.hasNext()) {
            ResolverTarget target2 = (ResolverTarget) it3.next();
            float recency = target2.getRecencyScore() / mostRecencyScore;
            setFeatures(target2, (recency * recency) * RECENCY_MULTIPLIER, target2.getLaunchScore() / mostLaunchScore, target2.getTimeSpentScore() / mostTimeSpentScore, target2.getChooserScore() / mostChooserScore);
            addDefaultSelectProbability(target2);
        }
        predictSelectProbabilities(this.mTargets);
    }

    public int compare(ResolvedComponentInfo lhsp, ResolvedComponentInfo rhsp) {
        int i = 0;
        ResolveInfo lhs = lhsp.getResolveInfoAt(0);
        ResolveInfo rhs = rhsp.getResolveInfoAt(0);
        if (lhs.targetUserId != -2) {
            if (rhs.targetUserId == -2) {
                i = 1;
            }
            return i;
        }
        int i2 = -1;
        if (rhs.targetUserId != -2) {
            return -1;
        }
        boolean lhsSpecific;
        if (this.mHttp) {
            lhsSpecific = ResolverActivity.isSpecificUriMatch(lhs.match);
            if (lhsSpecific != ResolverActivity.isSpecificUriMatch(rhs.match)) {
                if (!lhsSpecific) {
                    i2 = 1;
                }
                return i2;
            }
        }
        lhsSpecific = lhsp.isPinned();
        boolean rPinned = rhsp.isPinned();
        if (lhsSpecific && !rPinned) {
            return -1;
        }
        if (!lhsSpecific && rPinned) {
            return 1;
        }
        if (!(lhsSpecific || rPinned || this.mStats == null)) {
            ResolverTarget lhsTarget = (ResolverTarget) this.mTargetsDict.get(new ComponentName(lhs.activityInfo.packageName, lhs.activityInfo.name));
            ResolverTarget rhsTarget = (ResolverTarget) this.mTargetsDict.get(new ComponentName(rhs.activityInfo.packageName, rhs.activityInfo.name));
            if (!(lhsTarget == null || rhsTarget == null)) {
                int selectProbabilityDiff = Float.compare(rhsTarget.getSelectProbability(), lhsTarget.getSelectProbability());
                if (selectProbabilityDiff != 0) {
                    if (selectProbabilityDiff > 0) {
                        i2 = 1;
                    }
                    return i2;
                }
            }
        }
        CharSequence sa = lhs.loadLabel(this.mPm);
        if (sa == null) {
            sa = lhs.activityInfo.name;
        }
        CharSequence sb = rhs.loadLabel(this.mPm);
        if (sb == null) {
            sb = rhs.activityInfo.name;
        }
        return this.mCollator.compare(sa.toString().trim(), sb.toString().trim());
    }

    public float getScore(ComponentName name) {
        ResolverTarget target = (ResolverTarget) this.mTargetsDict.get(name);
        if (target != null) {
            return target.getSelectProbability();
        }
        return 0.0f;
    }

    public void updateChooserCounts(String packageName, int userId, String action) {
        if (this.mUsm != null) {
            this.mUsm.reportChooserSelection(packageName, userId, this.mContentType, this.mAnnotations, action);
        }
    }

    public void updateModel(ComponentName componentName) {
        synchronized (this.mLock) {
            if (this.mRanker != null) {
                try {
                    int selectedPos = new ArrayList(this.mTargetsDict.keySet()).indexOf(componentName);
                    if (selectedPos >= 0 && this.mTargets != null) {
                        float selectedProbability = getScore(componentName);
                        int order = 0;
                        Iterator it = this.mTargets.iterator();
                        while (it.hasNext()) {
                            if (((ResolverTarget) it.next()).getSelectProbability() > selectedProbability) {
                                order++;
                            }
                        }
                        logMetrics(order);
                        this.mRanker.train(this.mTargets, selectedPos);
                    }
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error in Train: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
            }
        }
    }

    public void destroy() {
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
        if (this.mConnection != null) {
            this.mContext.unbindService(this.mConnection);
            this.mConnection.destroy();
        }
        if (this.mAfterCompute != null) {
            this.mAfterCompute.afterCompute();
        }
    }

    private void logMetrics(int selectedPos) {
        if (this.mRankerServiceName != null) {
            MetricsLogger metricsLogger = new MetricsLogger();
            LogMaker log = new LogMaker(MetricsEvent.ACTION_TARGET_SELECTED);
            log.setComponentName(this.mRankerServiceName);
            log.addTaggedData(MetricsEvent.FIELD_IS_CATEGORY_USED, Integer.valueOf(this.mAnnotations == null ? 0 : 1));
            log.addTaggedData(MetricsEvent.FIELD_RANKED_POSITION, Integer.valueOf(selectedPos));
            metricsLogger.write(log);
        }
    }

    /* JADX WARNING: Missing block: B:10:0x000e, code skipped:
            r0 = resolveRankerService();
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            if (r0 != null) goto L_0x0015;
     */
    /* JADX WARNING: Missing block: B:12:0x0014, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:13:0x0015, code skipped:
            r4.mConnectSignal = new java.util.concurrent.CountDownLatch(1);
            r4.mConnection = new com.android.internal.app.ResolverComparator.ResolverRankerServiceConnection(r4, r4.mConnectSignal);
            r5.bindServiceAsUser(r0, r4.mConnection, 1, android.os.UserHandle.SYSTEM);
     */
    /* JADX WARNING: Missing block: B:14:0x002d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void initRanker(Context context) {
        synchronized (this.mLock) {
            if (this.mConnection == null || this.mRanker == null) {
            }
        }
    }

    private Intent resolveRankerService() {
        Intent intent = new Intent("android.service.resolver.ResolverRankerService");
        for (ResolveInfo resolveInfo : this.mPm.queryIntentServices(intent, 0)) {
            if (!(resolveInfo == null || resolveInfo.serviceInfo == null)) {
                if (resolveInfo.serviceInfo.applicationInfo != null) {
                    ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.applicationInfo.packageName, resolveInfo.serviceInfo.name);
                    String str;
                    StringBuilder stringBuilder;
                    try {
                        if (!"android.permission.BIND_RESOLVER_RANKER_SERVICE".equals(this.mPm.getServiceInfo(componentName, 0).permission)) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("ResolverRankerService ");
                            stringBuilder.append(componentName);
                            stringBuilder.append(" does not require permission ");
                            stringBuilder.append("android.permission.BIND_RESOLVER_RANKER_SERVICE");
                            stringBuilder.append(" - this service will not be queried for ResolverComparator. add android:permission=\"");
                            stringBuilder.append("android.permission.BIND_RESOLVER_RANKER_SERVICE");
                            stringBuilder.append("\" to the <service> tag for ");
                            stringBuilder.append(componentName);
                            stringBuilder.append(" in the manifest.");
                            Log.w(str, stringBuilder.toString());
                        } else if (this.mPm.checkPermission("android.permission.PROVIDE_RESOLVER_RANKER_SERVICE", resolveInfo.serviceInfo.packageName) != 0) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("ResolverRankerService ");
                            stringBuilder.append(componentName);
                            stringBuilder.append(" does not hold permission ");
                            stringBuilder.append("android.permission.PROVIDE_RESOLVER_RANKER_SERVICE");
                            stringBuilder.append(" - this service will not be queried for ResolverComparator.");
                            Log.w(str, stringBuilder.toString());
                        } else {
                            this.mResolvedRankerName = componentName;
                            intent.setComponent(componentName);
                            return intent;
                        }
                    } catch (NameNotFoundException e) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not look up service ");
                        stringBuilder.append(componentName);
                        stringBuilder.append("; component name not found");
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }
        }
        return null;
    }

    private void startWatchDog(int timeOutLimit) {
        if (this.mHandler == null) {
            Log.d(TAG, "Error: Handler is Null; Needs to be initialized.");
        }
        this.mHandler.sendEmptyMessageDelayed(1, (long) timeOutLimit);
    }

    private void reset() {
        this.mTargetsDict.clear();
        this.mTargets = null;
        this.mRankerServiceName = new ComponentName(this.mContext, getClass());
        this.mResolvedRankerName = null;
        startWatchDog(500);
        initRanker(this.mContext);
    }

    private void predictSelectProbabilities(List<ResolverTarget> targets) {
        if (this.mConnection != null) {
            try {
                this.mConnectSignal.await(200, TimeUnit.MILLISECONDS);
                synchronized (this.mLock) {
                    if (this.mRanker != null) {
                        this.mRanker.predict(targets, this.mConnection.resolverRankerResult);
                        return;
                    }
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Error in Wait for Service Connection.");
            } catch (RemoteException e2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error in Predict: ");
                stringBuilder.append(e2);
                Log.e(str, stringBuilder.toString());
            }
        }
        if (this.mAfterCompute != null) {
            this.mAfterCompute.afterCompute();
        }
    }

    private void addDefaultSelectProbability(ResolverTarget target) {
        target.setSelectProbability((float) (1.0d / (Math.exp((double) (1.6568f - ((((2.5543f * target.getLaunchScore()) + (2.8412f * target.getTimeSpentScore())) + (0.269f * target.getRecencyScore())) + (4.2222f * target.getChooserScore())))) + 1.0d)));
    }

    private void setFeatures(ResolverTarget target, float recencyScore, float launchScore, float timeSpentScore, float chooserScore) {
        target.setRecencyScore(recencyScore);
        target.setLaunchScore(launchScore);
        target.setTimeSpentScore(timeSpentScore);
        target.setChooserScore(chooserScore);
    }

    static boolean isPersistentProcess(ResolvedComponentInfo rci) {
        boolean z = false;
        if (rci == null || rci.getCount() <= 0) {
            return false;
        }
        if ((rci.getResolveInfoAt(0).activityInfo.applicationInfo.flags & 8) != 0) {
            z = true;
        }
        return z;
    }
}
