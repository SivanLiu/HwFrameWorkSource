package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.service.chooser.ChooserTarget;
import android.service.chooser.IChooserTargetResult;
import android.service.chooser.IChooserTargetResult.Stub;
import android.service.chooser.IChooserTargetService;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Space;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.ResolverActivity.DisplayResolveInfo;
import com.android.internal.app.ResolverActivity.ResolveListAdapter;
import com.android.internal.app.ResolverActivity.TargetInfo;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.Protocol;
import com.google.android.collect.Lists;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChooserActivity extends ResolverActivity {
    private static final float CALLER_TARGET_SCORE_BOOST = 900.0f;
    private static final int CHOOSER_TARGET_SERVICE_RESULT = 1;
    private static final int CHOOSER_TARGET_SERVICE_WATCHDOG_TIMEOUT = 2;
    private static final boolean DEBUG = false;
    public static final String EXTRA_PRIVATE_RETAIN_IN_ON_STOP = "com.android.internal.app.ChooserActivity.EXTRA_PRIVATE_RETAIN_IN_ON_STOP";
    private static final String PINNED_SHARED_PREFS_NAME = "chooser_pin_settings";
    private static final float PINNED_TARGET_SCORE_BOOST = 1000.0f;
    private static final int QUERY_TARGET_SERVICE_LIMIT = 5;
    private static final String TAG = "ChooserActivity";
    private static final String TARGET_DETAILS_FRAGMENT_TAG = "targetDetailsFragment";
    private static final int WATCHDOG_TIMEOUT_MILLIS = 2000;
    private ChooserTarget[] mCallerChooserTargets;
    private final Handler mChooserHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!ChooserActivity.this.isDestroyed()) {
                        ServiceResultInfo sri = msg.obj;
                        if (ChooserActivity.this.mServiceConnections.contains(sri.connection)) {
                            if (sri.resultTargets != null) {
                                ChooserActivity.this.mChooserListAdapter.addServiceResults(sri.originalTarget, sri.resultTargets);
                            }
                            ChooserActivity.this.unbindService(sri.connection);
                            sri.connection.destroy();
                            ChooserActivity.this.mServiceConnections.remove(sri.connection);
                            if (ChooserActivity.this.mServiceConnections.isEmpty()) {
                                ChooserActivity.this.mChooserHandler.removeMessages(2);
                                ChooserActivity.this.sendVoiceChoicesIfNeeded();
                                ChooserActivity.this.mChooserListAdapter.setShowServiceTargets(true);
                                return;
                            }
                            return;
                        }
                        String str = ChooserActivity.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ChooserTargetServiceConnection ");
                        stringBuilder.append(sri.connection);
                        stringBuilder.append(" returned after being removed from active connections. Have you considered returning results faster?");
                        Log.w(str, stringBuilder.toString());
                        return;
                    }
                    return;
                case 2:
                    ChooserActivity.this.unbindRemainingServices();
                    ChooserActivity.this.sendVoiceChoicesIfNeeded();
                    ChooserActivity.this.mChooserListAdapter.setShowServiceTargets(true);
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    };
    private ChooserListAdapter mChooserListAdapter;
    private ChooserRowAdapter mChooserRowAdapter;
    private long mChooserShownTime;
    private IntentSender mChosenComponentSender;
    private ComponentName[] mFilteredComponentNames;
    protected boolean mIsSuccessfullySelected;
    private SharedPreferences mPinnedSharedPrefs;
    private Intent mReferrerFillInIntent;
    private IntentSender mRefinementIntentSender;
    private RefinementResultReceiver mRefinementResultReceiver;
    private Bundle mReplacementExtras;
    private final List<ChooserTargetServiceConnection> mServiceConnections = new ArrayList();

    static class BaseChooserTargetComparator implements Comparator<ChooserTarget> {
        BaseChooserTargetComparator() {
        }

        public int compare(ChooserTarget lhs, ChooserTarget rhs) {
            return (int) Math.signum(rhs.getScore() - lhs.getScore());
        }
    }

    static class RowViewHolder {
        final View[] cells;
        int[] itemIndices;
        int measuredRowHeight;
        final ViewGroup row;

        public RowViewHolder(ViewGroup row, int cellCount) {
            this.row = row;
            this.cells = new View[cellCount];
            this.itemIndices = new int[cellCount];
        }

        public void measure() {
            int spec = MeasureSpec.makeMeasureSpec(0, 0);
            this.row.measure(spec, spec);
            this.measuredRowHeight = this.row.getMeasuredHeight();
        }
    }

    static class ServiceResultInfo {
        public final ChooserTargetServiceConnection connection;
        public final DisplayResolveInfo originalTarget;
        public final List<ChooserTarget> resultTargets;

        public ServiceResultInfo(DisplayResolveInfo ot, List<ChooserTarget> rt, ChooserTargetServiceConnection c) {
            this.originalTarget = ot;
            this.resultTargets = rt;
            this.connection = c;
        }
    }

    public class ChooserListController extends ResolverListController {
        public ChooserListController(Context context, PackageManager pm, Intent targetIntent, String referrerPackageName, int launchedFromUid) {
            super(context, pm, targetIntent, referrerPackageName, launchedFromUid);
        }

        boolean isComponentPinned(ComponentName name) {
            return ChooserActivity.this.mPinnedSharedPrefs.getBoolean(name.flattenToString(), false);
        }

        boolean isComponentFiltered(ComponentName name) {
            if (ChooserActivity.this.mFilteredComponentNames == null) {
                return false;
            }
            for (ComponentName filteredComponentName : ChooserActivity.this.mFilteredComponentNames) {
                if (name.equals(filteredComponentName)) {
                    return true;
                }
            }
            return false;
        }

        public float getScore(DisplayResolveInfo target) {
            if (target == null) {
                return ChooserActivity.CALLER_TARGET_SCORE_BOOST;
            }
            float score = super.getScore(target);
            if (target.isPinned()) {
                score += ChooserActivity.PINNED_TARGET_SCORE_BOOST;
            }
            return score;
        }
    }

    final class ChooserTargetInfo implements TargetInfo {
        private final ResolveInfo mBackupResolveInfo;
        private CharSequence mBadgeContentDescription;
        private Drawable mBadgeIcon = null;
        private final ChooserTarget mChooserTarget;
        private Drawable mDisplayIcon;
        private final int mFillInFlags;
        private final Intent mFillInIntent;
        private final float mModifiedScore;
        private final DisplayResolveInfo mSourceInfo;

        public ChooserTargetInfo(DisplayResolveInfo sourceInfo, ChooserTarget chooserTarget, float modifiedScore) {
            this.mSourceInfo = sourceInfo;
            this.mChooserTarget = chooserTarget;
            this.mModifiedScore = modifiedScore;
            if (sourceInfo != null) {
                ResolveInfo ri = sourceInfo.getResolveInfo();
                if (ri != null) {
                    ActivityInfo ai = ri.activityInfo;
                    if (!(ai == null || ai.applicationInfo == null)) {
                        PackageManager pm = ChooserActivity.this.getPackageManager();
                        this.mBadgeIcon = (ChooserActivity.this.mIsClonedProfile ? ChooserActivity.this.mPmForParent : pm).getApplicationIcon(ai.applicationInfo);
                        this.mBadgeContentDescription = pm.getApplicationLabel(ai.applicationInfo);
                    }
                }
            }
            Icon icon = chooserTarget.getIcon();
            this.mDisplayIcon = icon != null ? icon.loadDrawable(ChooserActivity.this) : null;
            if (sourceInfo != null) {
                this.mBackupResolveInfo = null;
            } else {
                this.mBackupResolveInfo = ChooserActivity.this.getPackageManager().resolveActivity(getResolvedIntent(), 0);
            }
            this.mFillInIntent = null;
            this.mFillInFlags = 0;
        }

        private ChooserTargetInfo(ChooserTargetInfo other, Intent fillInIntent, int flags) {
            this.mSourceInfo = other.mSourceInfo;
            this.mBackupResolveInfo = other.mBackupResolveInfo;
            this.mChooserTarget = other.mChooserTarget;
            this.mBadgeIcon = other.mBadgeIcon;
            this.mBadgeContentDescription = other.mBadgeContentDescription;
            this.mDisplayIcon = other.mDisplayIcon;
            this.mFillInIntent = fillInIntent;
            this.mFillInFlags = flags;
            this.mModifiedScore = other.mModifiedScore;
        }

        public float getModifiedScore() {
            return this.mModifiedScore;
        }

        public Intent getResolvedIntent() {
            if (this.mSourceInfo != null) {
                return this.mSourceInfo.getResolvedIntent();
            }
            Intent targetIntent = new Intent(ChooserActivity.this.getTargetIntent());
            targetIntent.setComponent(this.mChooserTarget.getComponentName());
            targetIntent.putExtras(this.mChooserTarget.getIntentExtras());
            return targetIntent;
        }

        public ComponentName getResolvedComponentName() {
            if (this.mSourceInfo != null) {
                return this.mSourceInfo.getResolvedComponentName();
            }
            if (this.mBackupResolveInfo != null) {
                return new ComponentName(this.mBackupResolveInfo.activityInfo.packageName, this.mBackupResolveInfo.activityInfo.name);
            }
            return null;
        }

        private Intent getBaseIntentToSend() {
            Intent result = getResolvedIntent();
            if (result == null) {
                Log.e(ChooserActivity.TAG, "ChooserTargetInfo: no base intent available to send");
            } else {
                result = new Intent(result);
                if (this.mFillInIntent != null) {
                    result.fillIn(this.mFillInIntent, this.mFillInFlags);
                }
                result.fillIn(ChooserActivity.this.mReferrerFillInIntent, 0);
            }
            return result;
        }

        public boolean start(Activity activity, Bundle options) {
            throw new RuntimeException("ChooserTargets should be started as caller.");
        }

        public boolean startAsCaller(Activity activity, Bundle options, int userId) {
            Intent intent = getBaseIntentToSend();
            boolean ignoreTargetSecurity = false;
            if (intent == null) {
                return false;
            }
            intent.setComponent(this.mChooserTarget.getComponentName());
            intent.putExtras(this.mChooserTarget.getIntentExtras());
            if (this.mSourceInfo != null && this.mSourceInfo.getResolvedComponentName().getPackageName().equals(this.mChooserTarget.getComponentName().getPackageName())) {
                ignoreTargetSecurity = true;
            }
            activity.startActivityAsCaller(intent, options, ignoreTargetSecurity, userId);
            return true;
        }

        public boolean startAsUser(Activity activity, Bundle options, UserHandle user) {
            throw new RuntimeException("ChooserTargets should be started as caller.");
        }

        public ResolveInfo getResolveInfo() {
            return this.mSourceInfo != null ? this.mSourceInfo.getResolveInfo() : this.mBackupResolveInfo;
        }

        public CharSequence getDisplayLabel() {
            return this.mChooserTarget.getTitle();
        }

        public CharSequence getExtendedInfo() {
            return null;
        }

        public Drawable getDisplayIcon() {
            return this.mDisplayIcon;
        }

        public Drawable getBadgeIcon() {
            return this.mBadgeIcon;
        }

        public CharSequence getBadgeContentDescription() {
            return this.mBadgeContentDescription;
        }

        public TargetInfo cloneFilledIn(Intent fillInIntent, int flags) {
            return new ChooserTargetInfo(this, fillInIntent, flags);
        }

        public List<Intent> getAllSourceIntents() {
            List<Intent> results = new ArrayList();
            if (this.mSourceInfo != null) {
                results.add((Intent) this.mSourceInfo.getAllSourceIntents().get(0));
            }
            return results;
        }

        public boolean isPinned() {
            return this.mSourceInfo != null ? this.mSourceInfo.isPinned() : false;
        }
    }

    static class ChooserTargetServiceConnection implements ServiceConnection {
        private ChooserActivity mChooserActivity;
        private final IChooserTargetResult mChooserTargetResult = new Stub() {
            public void sendResult(List<ChooserTarget> targets) throws RemoteException {
                synchronized (ChooserTargetServiceConnection.this.mLock) {
                    if (ChooserTargetServiceConnection.this.mChooserActivity == null) {
                        String str = ChooserActivity.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("destroyed ChooserTargetServiceConnection received result from ");
                        stringBuilder.append(ChooserTargetServiceConnection.this.mConnectedComponent);
                        stringBuilder.append("; ignoring...");
                        Log.e(str, stringBuilder.toString());
                        return;
                    }
                    ChooserTargetServiceConnection.this.mChooserActivity.filterServiceTargets(ChooserTargetServiceConnection.this.mOriginalTarget.getResolveInfo().activityInfo.packageName, targets);
                    Message msg = Message.obtain();
                    msg.what = 1;
                    msg.obj = new ServiceResultInfo(ChooserTargetServiceConnection.this.mOriginalTarget, targets, ChooserTargetServiceConnection.this);
                    ChooserTargetServiceConnection.this.mChooserActivity.mChooserHandler.sendMessage(msg);
                }
            }
        };
        private ComponentName mConnectedComponent;
        private final Object mLock = new Object();
        private DisplayResolveInfo mOriginalTarget;

        public ChooserTargetServiceConnection(ChooserActivity chooserActivity, DisplayResolveInfo dri) {
            this.mChooserActivity = chooserActivity;
            this.mOriginalTarget = dri;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (this.mLock) {
                if (this.mChooserActivity == null) {
                    Log.e(ChooserActivity.TAG, "destroyed ChooserTargetServiceConnection got onServiceConnected");
                    return;
                }
                try {
                    IChooserTargetService.Stub.asInterface(service).getChooserTargets(this.mOriginalTarget.getResolvedComponentName(), this.mOriginalTarget.getResolveInfo().filter, this.mChooserTargetResult);
                } catch (RemoteException e) {
                    String str = ChooserActivity.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Querying ChooserTargetService ");
                    stringBuilder.append(name);
                    stringBuilder.append(" failed.");
                    Log.e(str, stringBuilder.toString(), e);
                    this.mChooserActivity.unbindService(this);
                    this.mChooserActivity.mServiceConnections.remove(this);
                    destroy();
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (this.mLock) {
                if (this.mChooserActivity == null) {
                    Log.e(ChooserActivity.TAG, "destroyed ChooserTargetServiceConnection got onServiceDisconnected");
                    return;
                }
                this.mChooserActivity.unbindService(this);
                this.mChooserActivity.mServiceConnections.remove(this);
                if (this.mChooserActivity.mServiceConnections.isEmpty()) {
                    this.mChooserActivity.mChooserHandler.removeMessages(2);
                    this.mChooserActivity.sendVoiceChoicesIfNeeded();
                }
                this.mConnectedComponent = null;
                destroy();
            }
        }

        public void destroy() {
            synchronized (this.mLock) {
                this.mChooserActivity = null;
                this.mOriginalTarget = null;
            }
        }

        public String toString() {
            String activityInfo;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ChooserTargetServiceConnection{service=");
            stringBuilder.append(this.mConnectedComponent);
            stringBuilder.append(", activity=");
            if (this.mOriginalTarget != null) {
                activityInfo = this.mOriginalTarget.getResolveInfo().activityInfo.toString();
            } else {
                activityInfo = "<connection destroyed>";
            }
            stringBuilder.append(activityInfo);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    class OffsetDataSetObserver extends DataSetObserver {
        private View mCachedView;
        private int mCachedViewType = -1;
        private final AbsListView mListView;

        public OffsetDataSetObserver(AbsListView listView) {
            this.mListView = listView;
        }

        public void onChanged() {
            if (ChooserActivity.this.mResolverDrawerLayout != null) {
                int chooserTargetRows = ChooserActivity.this.mChooserRowAdapter.getServiceTargetRowCount();
                int offset = 0;
                for (int i = 0; i < chooserTargetRows; i++) {
                    int pos = ChooserActivity.this.mChooserRowAdapter.getCallerTargetRowCount() + i;
                    int vt = ChooserActivity.this.mChooserRowAdapter.getItemViewType(pos);
                    if (vt != this.mCachedViewType) {
                        this.mCachedView = null;
                    }
                    View v = ChooserActivity.this.mChooserRowAdapter.getView(pos, this.mCachedView, this.mListView);
                    offset += ((RowViewHolder) v.getTag()).measuredRowHeight;
                    if (vt >= 0) {
                        this.mCachedViewType = vt;
                        this.mCachedView = v;
                    } else {
                        this.mCachedViewType = -1;
                    }
                }
                ChooserActivity.this.mResolverDrawerLayout.setCollapsibleHeightReserved(offset);
            }
        }
    }

    static class RefinementResultReceiver extends ResultReceiver {
        private ChooserActivity mChooserActivity;
        private TargetInfo mSelectedTarget;

        public RefinementResultReceiver(ChooserActivity host, TargetInfo target, Handler handler) {
            super(handler);
            this.mChooserActivity = host;
            this.mSelectedTarget = target;
        }

        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (this.mChooserActivity == null) {
                Log.e(ChooserActivity.TAG, "Destroyed RefinementResultReceiver received a result");
            } else if (resultData == null) {
                Log.e(ChooserActivity.TAG, "RefinementResultReceiver received null resultData");
            } else {
                switch (resultCode) {
                    case -1:
                        Parcelable intentParcelable = resultData.getParcelable("android.intent.extra.INTENT");
                        if (!(intentParcelable instanceof Intent)) {
                            Log.e(ChooserActivity.TAG, "RefinementResultReceiver received RESULT_OK but no Intent in resultData with key Intent.EXTRA_INTENT");
                            break;
                        } else {
                            this.mChooserActivity.onRefinementResult(this.mSelectedTarget, (Intent) intentParcelable);
                            break;
                        }
                    case 0:
                        this.mChooserActivity.onRefinementCanceled();
                        break;
                    default:
                        String str = ChooserActivity.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown result code ");
                        stringBuilder.append(resultCode);
                        stringBuilder.append(" sent to RefinementResultReceiver");
                        Log.w(str, stringBuilder.toString());
                        break;
                }
            }
        }

        public void destroy() {
            this.mChooserActivity = null;
            this.mSelectedTarget = null;
        }
    }

    class ChooserRowAdapter extends BaseAdapter {
        private int mAnimationCount = 0;
        private ChooserListAdapter mChooserListAdapter;
        private final int mColumnCount = 4;
        private final LayoutInflater mLayoutInflater;

        public ChooserRowAdapter(ChooserListAdapter wrappedAdapter) {
            this.mChooserListAdapter = wrappedAdapter;
            this.mLayoutInflater = LayoutInflater.from(ChooserActivity.this);
            wrappedAdapter.registerDataSetObserver(new DataSetObserver(ChooserActivity.this) {
                public void onChanged() {
                    super.onChanged();
                    ChooserRowAdapter.this.notifyDataSetChanged();
                }

                public void onInvalidated() {
                    super.onInvalidated();
                    ChooserRowAdapter.this.notifyDataSetInvalidated();
                }
            });
        }

        public int getCount() {
            return (int) (((double) (getCallerTargetRowCount() + getServiceTargetRowCount())) + Math.ceil((double) (((float) this.mChooserListAdapter.getStandardTargetCount()) / 4.0f)));
        }

        public int getCallerTargetRowCount() {
            return (int) Math.ceil((double) (((float) this.mChooserListAdapter.getCallerTargetCount()) / 4.0f));
        }

        public int getServiceTargetRowCount() {
            return this.mChooserListAdapter.getServiceTargetCount() == 0 ? 0 : 1;
        }

        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            RowViewHolder holder;
            if (convertView == null) {
                holder = createViewHolder(parent);
            } else {
                holder = (RowViewHolder) convertView.getTag();
            }
            bindViewHolder(position, holder);
            return holder.row;
        }

        RowViewHolder createViewHolder(ViewGroup parent) {
            ViewGroup row = (ViewGroup) this.mLayoutInflater.inflate(17367116, parent, false);
            final RowViewHolder holder = new RowViewHolder(row, 4);
            int spec = MeasureSpec.makeMeasureSpec(0, 0);
            for (int i = 0; i < 4; i++) {
                View v = this.mChooserListAdapter.createView(row);
                final int column = i;
                v.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        ChooserActivity.this.startSelected(holder.itemIndices[column], false, true);
                    }
                });
                v.setOnLongClickListener(new OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        ChooserActivity.this.showTargetDetails(ChooserRowAdapter.this.mChooserListAdapter.resolveInfoForPosition(holder.itemIndices[column], true));
                        return true;
                    }
                });
                row.addView(v);
                holder.cells[i] = v;
                LayoutParams lp = v.getLayoutParams();
                v.measure(spec, spec);
                if (lp == null) {
                    row.setLayoutParams(new LayoutParams(-1, v.getMeasuredHeight()));
                } else {
                    lp.height = v.getMeasuredHeight();
                }
                if (i != 3) {
                    row.addView(new Space(ChooserActivity.this), new LinearLayout.LayoutParams(0, 0, 1.0f));
                }
            }
            holder.measure();
            LayoutParams lp2 = row.getLayoutParams();
            if (lp2 == null) {
                row.setLayoutParams(new LayoutParams(-1, holder.measuredRowHeight));
            } else {
                lp2.height = holder.measuredRowHeight;
            }
            row.setTag(holder);
            return holder;
        }

        void bindViewHolder(int rowPosition, RowViewHolder holder) {
            int serviceSpacing;
            int start = getFirstRowPosition(rowPosition);
            int startType = this.mChooserListAdapter.getPositionTargetType(start);
            int end = (start + 4) - 1;
            while (this.mChooserListAdapter.getPositionTargetType(end) != startType && end >= start) {
                end--;
            }
            if (startType == 1) {
                holder.row.setBackgroundColor(ChooserActivity.this.getColor(17170532));
                int nextStartType = this.mChooserListAdapter.getPositionTargetType(getFirstRowPosition(rowPosition + 1));
                serviceSpacing = holder.row.getContext().getResources().getDimensionPixelSize(17104955);
                if (rowPosition != 0 || nextStartType == 1) {
                    int top = rowPosition == 0 ? serviceSpacing : 0;
                    if (nextStartType != 1) {
                        setVertPadding(holder, top, serviceSpacing);
                    } else {
                        setVertPadding(holder, top, 0);
                    }
                } else {
                    setVertPadding(holder, 0, 0);
                }
            } else {
                holder.row.setBackgroundColor(0);
                if (this.mChooserListAdapter.getPositionTargetType(getFirstRowPosition(rowPosition - 1)) == 1 || rowPosition == 0) {
                    setVertPadding(holder, holder.row.getContext().getResources().getDimensionPixelSize(17104955), 0);
                } else {
                    setVertPadding(holder, 0, 0);
                }
            }
            serviceSpacing = holder.row.getLayoutParams().height;
            holder.row.getLayoutParams().height = Math.max(1, holder.measuredRowHeight);
            if (holder.row.getLayoutParams().height != serviceSpacing) {
                holder.row.requestLayout();
            }
            for (int i = 0; i < 4; i++) {
                View v = holder.cells[i];
                if (start + i <= end) {
                    v.setVisibility(0);
                    holder.itemIndices[i] = start + i;
                    this.mChooserListAdapter.bindView(holder.itemIndices[i], v);
                } else {
                    v.setVisibility(4);
                }
            }
        }

        private void setVertPadding(RowViewHolder holder, int top, int bottom) {
            holder.row.setPadding(holder.row.getPaddingLeft(), top, holder.row.getPaddingRight(), bottom);
        }

        int getFirstRowPosition(int row) {
            int callerCount = this.mChooserListAdapter.getCallerTargetCount();
            int callerRows = (int) Math.ceil((double) (((float) callerCount) / 4.0f));
            if (row < callerRows) {
                return row * 4;
            }
            int serviceCount = this.mChooserListAdapter.getServiceTargetCount();
            int serviceRows = (int) Math.ceil((double) (((float) serviceCount) / 4.0f));
            if (row < callerRows + serviceRows) {
                return ((row - callerRows) * 4) + callerCount;
            }
            return (callerCount + serviceCount) + (((row - callerRows) - serviceRows) * 4);
        }
    }

    public class ChooserListAdapter extends ResolveListAdapter {
        private static final int MAX_SERVICE_TARGETS = 4;
        private static final int MAX_TARGETS_PER_SERVICE = 2;
        public static final int TARGET_BAD = -1;
        public static final int TARGET_CALLER = 0;
        public static final int TARGET_SERVICE = 1;
        public static final int TARGET_STANDARD = 2;
        private final BaseChooserTargetComparator mBaseTargetComparator;
        private final List<TargetInfo> mCallerTargets = new ArrayList();
        private float mLateFee = 1.0f;
        private final List<ChooserTargetInfo> mServiceTargets = new ArrayList();
        private boolean mShowServiceTargets;
        private boolean mTargetsNeedPruning;
        final /* synthetic */ ChooserActivity this$0;

        public ChooserListAdapter(ChooserActivity this$0, Context context, List<Intent> payloadIntents, Intent[] initialIntents, List<ResolveInfo> rList, int launchedFromUid, boolean filterLastUsed, ResolverListController resolverListController) {
            ResolverActivity resolverActivity = this$0;
            Intent[] intentArr = initialIntents;
            this.this$0 = resolverActivity;
            super(context, payloadIntents, null, rList, launchedFromUid, filterLastUsed, resolverListController);
            int i = 0;
            this.mTargetsNeedPruning = false;
            this.mBaseTargetComparator = new BaseChooserTargetComparator();
            if (intentArr != null) {
                PackageManager pm = this$0.getPackageManager();
                int i2 = 0;
                while (true) {
                    int i3 = i2;
                    if (i3 < intentArr.length) {
                        Intent ii = intentArr[i3];
                        if (ii != null) {
                            ResolveInfo ri;
                            ResolveInfo ri2 = null;
                            ActivityInfo ai = null;
                            if (ii.getComponent() != null) {
                                try {
                                    ai = pm.getActivityInfo(ii.getComponent(), i);
                                    ri2 = new ResolveInfo();
                                    ri2.activityInfo = ai;
                                } catch (NameNotFoundException e) {
                                }
                            }
                            if (ai == null) {
                                ResolveInfo ri3 = pm.resolveActivity(ii, Protocol.BASE_SYSTEM_RESERVED);
                                ai = ri3 != null ? ri3.activityInfo : null;
                                ri = ri3;
                            } else {
                                ri = ri2;
                            }
                            ActivityInfo ai2 = ai;
                            if (ai2 == null) {
                                String str = ChooserActivity.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("No activity found for ");
                                stringBuilder.append(ii);
                                Log.w(str, stringBuilder.toString());
                            } else {
                                UserManager userManager = (UserManager) resolverActivity.getSystemService("user");
                                if (ii instanceof LabeledIntent) {
                                    LabeledIntent li = (LabeledIntent) ii;
                                    ri.resolvePackageName = li.getSourcePackage();
                                    ri.labelRes = li.getLabelResource();
                                    ri.nonLocalizedLabel = li.getNonLocalizedLabel();
                                    ri.icon = li.getIconResource();
                                    ri.iconResourceId = ri.icon;
                                }
                                if (userManager.isManagedProfile()) {
                                    ri.noResourceId = true;
                                    ri.icon = i;
                                }
                                DisplayResolveInfo displayResolveInfo = r1;
                                List list = this.mCallerTargets;
                                DisplayResolveInfo displayResolveInfo2 = new DisplayResolveInfo(ii, ri, ri.loadLabel(pm), null, ii);
                                list.add(displayResolveInfo);
                            }
                        }
                        i2 = i3 + 1;
                        i = 0;
                    } else {
                        return;
                    }
                }
            }
        }

        public boolean showsExtendedInfo(TargetInfo info) {
            return false;
        }

        public boolean isComponentPinned(ComponentName name) {
            return this.this$0.mPinnedSharedPrefs.getBoolean(name.flattenToString(), false);
        }

        public View onCreateView(ViewGroup parent) {
            return this.mInflater.inflate(17367250, parent, false);
        }

        public void onListRebuilt() {
            if (!ActivityManager.isLowRamDeviceStatic()) {
                if (this.mServiceTargets != null && getDisplayInfoCount() == 0) {
                    this.mTargetsNeedPruning = true;
                }
                this.this$0.queryTargetServices(this);
            }
        }

        public boolean shouldGetResolvedFilter() {
            return true;
        }

        public int getCount() {
            return (super.getCount() + getServiceTargetCount()) + getCallerTargetCount();
        }

        public int getUnfilteredCount() {
            return (super.getUnfilteredCount() + getServiceTargetCount()) + getCallerTargetCount();
        }

        public int getCallerTargetCount() {
            return this.mCallerTargets.size();
        }

        public int getServiceTargetCount() {
            if (this.mShowServiceTargets) {
                return Math.min(this.mServiceTargets.size(), 4);
            }
            return 0;
        }

        public int getStandardTargetCount() {
            return super.getCount();
        }

        public int getPositionTargetType(int position) {
            int callerTargetCount = getCallerTargetCount();
            if (position < callerTargetCount) {
                return 0;
            }
            int offset = 0 + callerTargetCount;
            int serviceTargetCount = getServiceTargetCount();
            if (position - offset < serviceTargetCount) {
                return 1;
            }
            if (position - (offset + serviceTargetCount) < super.getCount()) {
                return 2;
            }
            return -1;
        }

        public TargetInfo getItem(int position) {
            return targetInfoForPosition(position, true);
        }

        public TargetInfo targetInfoForPosition(int position, boolean filtered) {
            int callerTargetCount = getCallerTargetCount();
            if (position < callerTargetCount) {
                return (TargetInfo) this.mCallerTargets.get(position);
            }
            int offset = 0 + callerTargetCount;
            int serviceTargetCount = getServiceTargetCount();
            if (position - offset < serviceTargetCount) {
                return (TargetInfo) this.mServiceTargets.get(position - offset);
            }
            TargetInfo item;
            offset += serviceTargetCount;
            if (filtered) {
                item = super.getItem(position - offset);
            } else {
                item = getDisplayInfoAt(position - offset);
            }
            return item;
        }

        public void addServiceResults(DisplayResolveInfo origTarget, List<ChooserTarget> targets) {
            if (this.mTargetsNeedPruning && targets.size() > 0) {
                this.mServiceTargets.clear();
                this.mTargetsNeedPruning = false;
            }
            float parentScore = getScore(origTarget);
            Collections.sort(targets, this.mBaseTargetComparator);
            float lastScore = 0.0f;
            int N = Math.min(targets.size(), 2);
            for (int i = 0; i < N; i++) {
                ChooserTarget target = (ChooserTarget) targets.get(i);
                float targetScore = (target.getScore() * parentScore) * this.mLateFee;
                if (i > 0 && targetScore >= lastScore) {
                    targetScore = lastScore * 0.95f;
                }
                insertServiceTarget(new ChooserTargetInfo(origTarget, target, targetScore));
                lastScore = targetScore;
            }
            this.mLateFee *= 0.95f;
            notifyDataSetChanged();
        }

        public void setShowServiceTargets(boolean show) {
            if (show != this.mShowServiceTargets) {
                this.mShowServiceTargets = show;
                notifyDataSetChanged();
            }
        }

        private void insertServiceTarget(ChooserTargetInfo chooserTargetInfo) {
            float newScore = chooserTargetInfo.getModifiedScore();
            int N = this.mServiceTargets.size();
            for (int i = 0; i < N; i++) {
                if (newScore > ((ChooserTargetInfo) this.mServiceTargets.get(i)).getModifiedScore()) {
                    this.mServiceTargets.add(i, chooserTargetInfo);
                    return;
                }
            }
            this.mServiceTargets.add(chooserTargetInfo);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        long intentReceivedTime = System.currentTimeMillis();
        this.mIsSuccessfullySelected = false;
        Intent intent = getIntent();
        Parcelable targetParcelable = intent.getParcelableExtra("android.intent.extra.INTENT");
        String str;
        if (targetParcelable instanceof Intent) {
            Intent target;
            StringBuilder stringBuilder;
            Intent target2;
            Intent target3 = (Intent) targetParcelable;
            if (target3 != null) {
                modifyTargetIntent(target3);
            }
            Parcelable[] targetsParcelable = intent.getParcelableArrayExtra("android.intent.extra.ALTERNATE_INTENTS");
            if (targetsParcelable != null) {
                boolean offset = target3 == null;
                Intent[] additionalTargets = new Intent[(offset ? targetsParcelable.length - 1 : targetsParcelable.length)];
                target = target3;
                target3 = null;
                while (target3 < targetsParcelable.length) {
                    if (targetsParcelable[target3] instanceof Intent) {
                        Intent additionalTarget = targetsParcelable[target3];
                        if (target3 == null && target == null) {
                            target = additionalTarget;
                            modifyTargetIntent(target);
                        } else {
                            additionalTargets[offset ? target3 - 1 : target3] = additionalTarget;
                            modifyTargetIntent(additionalTarget);
                        }
                        target3++;
                    } else {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("EXTRA_ALTERNATE_INTENTS array entry #");
                        stringBuilder.append(target3);
                        stringBuilder.append(" is not an Intent: ");
                        stringBuilder.append(targetsParcelable[target3]);
                        Log.w(str, stringBuilder.toString());
                        finish();
                        super.onCreate(null);
                        return;
                    }
                }
                setAdditionalTargets(additionalTargets);
                target2 = target;
            } else {
                target2 = target3;
            }
            this.mReplacementExtras = intent.getBundleExtra("android.intent.extra.REPLACEMENT_EXTRAS");
            CharSequence title = intent.getCharSequenceExtra("android.intent.extra.TITLE");
            int defaultTitleRes = 0;
            if (title == null) {
                defaultTitleRes = 17039747;
            }
            int defaultTitleRes2 = defaultTitleRes;
            Parcelable[] pa = intent.getParcelableArrayExtra("android.intent.extra.INITIAL_INTENTS");
            Intent[] initialIntents = null;
            if (pa != null) {
                initialIntents = new Intent[pa.length];
                int i = 0;
                while (i < pa.length) {
                    if (pa[i] instanceof Intent) {
                        target = pa[i];
                        modifyTargetIntent(target);
                        initialIntents[i] = target;
                        i++;
                    } else {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Initial intent #");
                        stringBuilder.append(i);
                        stringBuilder.append(" not an Intent: ");
                        stringBuilder.append(pa[i]);
                        Log.w(str, stringBuilder.toString());
                        finish();
                        super.onCreate(null);
                        return;
                    }
                }
            }
            Intent[] initialIntents2 = initialIntents;
            this.mReferrerFillInIntent = new Intent().putExtra("android.intent.extra.REFERRER", getReferrer());
            this.mChosenComponentSender = (IntentSender) intent.getParcelableExtra("android.intent.extra.CHOSEN_COMPONENT_INTENT_SENDER");
            this.mRefinementIntentSender = (IntentSender) intent.getParcelableExtra("android.intent.extra.CHOOSER_REFINEMENT_INTENT_SENDER");
            setSafeForwardingMode(true);
            pa = intent.getParcelableArrayExtra("android.intent.extra.EXCLUDE_COMPONENTS");
            if (pa != null) {
                ComponentName[] names = new ComponentName[pa.length];
                for (int i2 = 0; i2 < pa.length; i2++) {
                    if (!(pa[i2] instanceof ComponentName)) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Filtered component #");
                        stringBuilder2.append(i2);
                        stringBuilder2.append(" not a ComponentName: ");
                        stringBuilder2.append(pa[i2]);
                        Log.w(str2, stringBuilder2.toString());
                        names = null;
                        break;
                    }
                    names[i2] = (ComponentName) pa[i2];
                }
                this.mFilteredComponentNames = names;
            }
            Parcelable[] pa2 = intent.getParcelableArrayExtra("android.intent.extra.CHOOSER_TARGETS");
            if (pa2 != null) {
                ChooserTarget[] targets = new ChooserTarget[pa2.length];
                for (int i3 = 0; i3 < pa2.length; i3++) {
                    if (!(pa2[i3] instanceof ChooserTarget)) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Chooser target #");
                        stringBuilder3.append(i3);
                        stringBuilder3.append(" not a ChooserTarget: ");
                        stringBuilder3.append(pa2[i3]);
                        Log.w(str3, stringBuilder3.toString());
                        targets = null;
                        break;
                    }
                    targets[i3] = (ChooserTarget) pa2[i3];
                }
                this.mCallerChooserTargets = targets;
            }
            this.mPinnedSharedPrefs = getPinnedSharedPrefs(this);
            setRetainInOnStop(intent.getBooleanExtra(EXTRA_PRIVATE_RETAIN_IN_ON_STOP, false));
            super.onCreate(savedInstanceState, target2, title, defaultTitleRes2, initialIntents2, null, null);
            MetricsLogger.action(this, 214);
            this.mChooserShownTime = System.currentTimeMillis();
            MetricsLogger.histogram(null, "system_cost_for_smart_sharing", (int) (this.mChooserShownTime - intentReceivedTime));
            return;
        }
        str = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Target is not an intent: ");
        stringBuilder4.append(targetParcelable);
        Log.w(str, stringBuilder4.toString());
        finish();
        super.onCreate(null);
    }

    static SharedPreferences getPinnedSharedPrefs(Context context) {
        return context.getSharedPreferences(new File(new File(Environment.getDataUserCePackageDirectory(StorageManager.UUID_PRIVATE_INTERNAL, context.getUserId(), context.getPackageName()), "shared_prefs"), "chooser_pin_settings.xml"), 0);
    }

    protected void onDestroy() {
        super.onDestroy();
        if (this.mRefinementResultReceiver != null) {
            this.mRefinementResultReceiver.destroy();
            this.mRefinementResultReceiver = null;
        }
        unbindRemainingServices();
        this.mChooserHandler.removeMessages(1);
    }

    public Intent getReplacementIntent(ActivityInfo aInfo, Intent defIntent) {
        Intent result = defIntent;
        if (this.mReplacementExtras != null) {
            Bundle replExtras = this.mReplacementExtras.getBundle(aInfo.packageName);
            if (replExtras != null) {
                result = new Intent(defIntent);
                result.putExtras(replExtras);
            }
        }
        if (!aInfo.name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_PARENT) && !aInfo.name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE)) {
            return result;
        }
        result = Intent.createChooser(result, getIntent().getCharSequenceExtra("android.intent.extra.TITLE"));
        result.putExtra("android.intent.extra.AUTO_LAUNCH_SINGLE_CHOICE", false);
        return result;
    }

    public void onActivityStarted(TargetInfo cti) {
        if (this.mChosenComponentSender != null) {
            ComponentName target = cti.getResolvedComponentName();
            if (target != null) {
                try {
                    this.mChosenComponentSender.sendIntent(this, -1, new Intent().putExtra("android.intent.extra.CHOSEN_COMPONENT", target), null, null);
                } catch (SendIntentException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to launch supplied IntentSender to report the chosen component: ");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                }
            }
        }
    }

    public void onPrepareAdapterView(AbsListView adapterView, ResolveListAdapter adapter) {
        ListView listView = adapterView instanceof ListView ? (ListView) adapterView : null;
        this.mChooserListAdapter = (ChooserListAdapter) adapter;
        if (this.mCallerChooserTargets != null && this.mCallerChooserTargets.length > 0) {
            this.mChooserListAdapter.addServiceResults(null, Lists.newArrayList(this.mCallerChooserTargets));
        }
        this.mChooserRowAdapter = new ChooserRowAdapter(this.mChooserListAdapter);
        this.mChooserRowAdapter.registerDataSetObserver(new OffsetDataSetObserver(adapterView));
        adapterView.setAdapter(this.mChooserRowAdapter);
        if (listView != null) {
            listView.setItemsCanFocus(true);
        }
    }

    public int getLayoutResource() {
        return 17367115;
    }

    public boolean shouldGetActivityMetadata() {
        return true;
    }

    public boolean shouldAutoLaunchSingleChoice(TargetInfo target) {
        return getIntent().getBooleanExtra("android.intent.extra.AUTO_LAUNCH_SINGLE_CHOICE", super.shouldAutoLaunchSingleChoice(target));
    }

    public void showTargetDetails(ResolveInfo ri) {
        if (ri != null) {
            ComponentName name = ri.activityInfo.getComponentName();
            new ResolverTargetActionsDialogFragment(ri.loadLabel(getPackageManager()), name, this.mPinnedSharedPrefs.getBoolean(name.flattenToString(), false)).show(getFragmentManager(), TARGET_DETAILS_FRAGMENT_TAG);
        }
    }

    private void modifyTargetIntent(Intent in) {
        String action = in.getAction();
        if ("android.intent.action.SEND".equals(action) || "android.intent.action.SEND_MULTIPLE".equals(action)) {
            in.addFlags(134742016);
        }
    }

    protected boolean onTargetSelected(TargetInfo target, boolean alwaysCheck) {
        if (this.mRefinementIntentSender != null) {
            Intent fillIn = new Intent();
            List<Intent> sourceIntents = target.getAllSourceIntents();
            if (!sourceIntents.isEmpty()) {
                fillIn.putExtra("android.intent.extra.INTENT", (Parcelable) sourceIntents.get(0));
                if (sourceIntents.size() > 1) {
                    Intent[] alts = new Intent[(sourceIntents.size() - 1)];
                    int N = sourceIntents.size();
                    for (int i = 1; i < N; i++) {
                        alts[i - 1] = (Intent) sourceIntents.get(i);
                    }
                    fillIn.putExtra("android.intent.extra.ALTERNATE_INTENTS", alts);
                }
                if (this.mRefinementResultReceiver != null) {
                    this.mRefinementResultReceiver.destroy();
                }
                this.mRefinementResultReceiver = new RefinementResultReceiver(this, target, null);
                fillIn.putExtra("android.intent.extra.RESULT_RECEIVER", this.mRefinementResultReceiver);
                try {
                    this.mRefinementIntentSender.sendIntent(this, 0, fillIn, null, null);
                    return false;
                } catch (SendIntentException e) {
                    Log.e(TAG, "Refinement IntentSender failed to send", e);
                }
            }
        }
        updateModelAndChooserCounts(target);
        return super.onTargetSelected(target, alwaysCheck);
    }

    public void startSelected(int which, boolean always, boolean filtered) {
        long selectionCost = System.currentTimeMillis() - this.mChooserShownTime;
        super.startSelected(which, always, filtered);
        if (this.mChooserListAdapter != null) {
            int cat = 0;
            int value = which;
            switch (this.mChooserListAdapter.getPositionTargetType(which)) {
                case 0:
                    cat = MetricsEvent.ACTION_ACTIVITY_CHOOSER_PICKED_APP_TARGET;
                    break;
                case 1:
                    cat = MetricsEvent.ACTION_ACTIVITY_CHOOSER_PICKED_SERVICE_TARGET;
                    value -= this.mChooserListAdapter.getCallerTargetCount();
                    break;
                case 2:
                    cat = 217;
                    value -= this.mChooserListAdapter.getCallerTargetCount() + this.mChooserListAdapter.getServiceTargetCount();
                    break;
            }
            if (cat != 0) {
                MetricsLogger.action((Context) this, cat, value);
            }
            if (this.mIsSuccessfullySelected) {
                MetricsLogger.histogram(null, "user_selection_cost_for_smart_sharing", (int) selectionCost);
                MetricsLogger.histogram(null, "app_position_for_smart_sharing", value);
            }
        }
    }

    void queryTargetServices(ChooserListAdapter adapter) {
        PackageManager pm = getPackageManager();
        int targetsToQuery = 0;
        int N = adapter.getDisplayResolveInfoCount();
        for (int i = 0; i < N; i++) {
            DisplayResolveInfo dri = adapter.getDisplayResolveInfo(i);
            if (adapter.getScore(dri) != 0.0f) {
                String serviceName;
                ActivityInfo ai = dri.getResolveInfo().activityInfo;
                Bundle md = ai.metaData;
                if (md != null) {
                    serviceName = convertServiceName(ai.packageName, md.getString("android.service.chooser.chooser_target_service"));
                } else {
                    serviceName = null;
                }
                if (serviceName != null) {
                    ComponentName serviceComponent = new ComponentName(ai.packageName, serviceName);
                    Intent serviceIntent = new Intent("android.service.chooser.ChooserTargetService").setComponent(serviceComponent);
                    StringBuilder stringBuilder;
                    try {
                        if ("android.permission.BIND_CHOOSER_TARGET_SERVICE".equals(pm.getServiceInfo(serviceComponent, 0).permission)) {
                            ChooserTargetServiceConnection conn = new ChooserTargetServiceConnection(this, dri);
                            if (bindServiceAsUser(serviceIntent, conn, 5, Process.myUserHandle())) {
                                this.mServiceConnections.add(conn);
                                targetsToQuery++;
                            }
                        } else {
                            String str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("ChooserTargetService ");
                            stringBuilder.append(serviceComponent);
                            stringBuilder.append(" does not require permission ");
                            stringBuilder.append("android.permission.BIND_CHOOSER_TARGET_SERVICE");
                            stringBuilder.append(" - this service will not be queried for ChooserTargets. add android:permission=\"");
                            stringBuilder.append("android.permission.BIND_CHOOSER_TARGET_SERVICE");
                            stringBuilder.append("\" to the <service> tag for ");
                            stringBuilder.append(serviceComponent);
                            stringBuilder.append(" in the manifest.");
                            Log.w(str, stringBuilder.toString());
                        }
                    } catch (NameNotFoundException e) {
                        String str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not look up service ");
                        stringBuilder.append(serviceComponent);
                        stringBuilder.append("; component name not found");
                        Log.e(str2, stringBuilder.toString());
                    }
                }
                if (targetsToQuery >= 5) {
                    break;
                }
            }
        }
        if (this.mServiceConnections.isEmpty()) {
            sendVoiceChoicesIfNeeded();
        } else {
            this.mChooserHandler.sendEmptyMessageDelayed(2, 2000);
        }
    }

    private String convertServiceName(String packageName, String serviceName) {
        String fullName = null;
        if (TextUtils.isEmpty(serviceName)) {
            return null;
        }
        if (serviceName.startsWith(".")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append(serviceName);
            fullName = stringBuilder.toString();
        } else if (serviceName.indexOf(46) >= 0) {
            fullName = serviceName;
        }
        return fullName;
    }

    void unbindRemainingServices() {
        int N = this.mServiceConnections.size();
        for (int i = 0; i < N; i++) {
            ChooserTargetServiceConnection conn = (ChooserTargetServiceConnection) this.mServiceConnections.get(i);
            unbindService(conn);
            conn.destroy();
        }
        this.mServiceConnections.clear();
        this.mChooserHandler.removeMessages(2);
    }

    public void onSetupVoiceInteraction() {
    }

    void updateModelAndChooserCounts(TargetInfo info) {
        if (info != null) {
            ResolveInfo ri = info.getResolveInfo();
            Intent targetIntent = getTargetIntent();
            if (!(ri == null || ri.activityInfo == null || targetIntent == null || this.mAdapter == null)) {
                this.mAdapter.updateModel(info.getResolvedComponentName());
                this.mAdapter.updateChooserCounts(ri.activityInfo.packageName, getUserId(), targetIntent.getAction());
            }
        }
        this.mIsSuccessfullySelected = true;
    }

    void onRefinementResult(TargetInfo selectedTarget, Intent matchingIntent) {
        if (this.mRefinementResultReceiver != null) {
            this.mRefinementResultReceiver.destroy();
            this.mRefinementResultReceiver = null;
        }
        if (selectedTarget == null) {
            Log.e(TAG, "Refinement result intent did not match any known targets; canceling");
        } else if (checkTargetSourceIntent(selectedTarget, matchingIntent)) {
            TargetInfo clonedTarget = selectedTarget.cloneFilledIn(matchingIntent, 0);
            if (super.onTargetSelected(clonedTarget, false)) {
                updateModelAndChooserCounts(clonedTarget);
                finish();
                return;
            }
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRefinementResult: Selected target ");
            stringBuilder.append(selectedTarget);
            stringBuilder.append(" cannot match refined source intent ");
            stringBuilder.append(matchingIntent);
            Log.e(str, stringBuilder.toString());
        }
        onRefinementCanceled();
    }

    void onRefinementCanceled() {
        if (this.mRefinementResultReceiver != null) {
            this.mRefinementResultReceiver.destroy();
            this.mRefinementResultReceiver = null;
        }
        finish();
    }

    boolean checkTargetSourceIntent(TargetInfo target, Intent matchingIntent) {
        List<Intent> targetIntents = target.getAllSourceIntents();
        int N = targetIntents.size();
        for (int i = 0; i < N; i++) {
            if (((Intent) targetIntents.get(i)).filterEquals(matchingIntent)) {
                return true;
            }
        }
        return false;
    }

    void filterServiceTargets(String packageName, List<ChooserTarget> targets) {
        if (targets != null) {
            PackageManager pm = getPackageManager();
            for (int i = targets.size() - 1; i >= 0; i--) {
                ChooserTarget target = (ChooserTarget) targets.get(i);
                ComponentName targetName = target.getComponentName();
                if (packageName == null || !packageName.equals(targetName.getPackageName())) {
                    boolean remove = false;
                    try {
                        ActivityInfo ai = pm.getActivityInfo(targetName, 0);
                        if (!(ai.exported && ai.permission == null)) {
                            remove = true;
                        }
                    } catch (NameNotFoundException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Target ");
                        stringBuilder.append(target);
                        stringBuilder.append(" returned by ");
                        stringBuilder.append(packageName);
                        stringBuilder.append(" component not found");
                        Log.e(str, stringBuilder.toString());
                        remove = true;
                    }
                    if (remove) {
                        targets.remove(i);
                    }
                }
            }
        }
    }

    public ResolveListAdapter createAdapter(Context context, List<Intent> payloadIntents, Intent[] initialIntents, List<ResolveInfo> rList, int launchedFromUid, boolean filterLastUsed) {
        return new ChooserListAdapter(this, context, payloadIntents, initialIntents, rList, launchedFromUid, filterLastUsed, createListController());
    }

    @VisibleForTesting
    protected ResolverListController createListController() {
        return new ChooserListController(this, this.mPm, getTargetIntent(), getReferrerPackageName(), this.mLaunchedFromUid);
    }
}
