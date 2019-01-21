package com.android.server.am;

import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IAssistDataReceiver.Stub;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.IWindowManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class AssistDataRequester extends Stub {
    public static final String KEY_RECEIVER_EXTRA_COUNT = "count";
    public static final String KEY_RECEIVER_EXTRA_INDEX = "index";
    private AppOpsManager mAppOpsManager;
    private final ArrayList<Bundle> mAssistData = new ArrayList();
    private final ArrayList<Bitmap> mAssistScreenshot = new ArrayList();
    private AssistDataRequesterCallbacks mCallbacks;
    private Object mCallbacksLock;
    private boolean mCanceled;
    private Context mContext;
    private int mPendingDataCount;
    private int mPendingScreenshotCount;
    private int mRequestScreenshotAppOps;
    private int mRequestStructureAppOps;
    private IActivityManager mService;
    private IWindowManager mWindowManager;

    public interface AssistDataRequesterCallbacks {
        @GuardedBy("mCallbacksLock")
        boolean canHandleReceivedAssistDataLocked();

        @GuardedBy("mCallbacksLock")
        void onAssistDataReceivedLocked(Bundle bundle, int i, int i2);

        @GuardedBy("mCallbacksLock")
        void onAssistScreenshotReceivedLocked(Bitmap bitmap);

        @GuardedBy("mCallbacksLock")
        void onAssistRequestCompleted() {
        }
    }

    public AssistDataRequester(Context context, IActivityManager service, IWindowManager windowManager, AppOpsManager appOpsManager, AssistDataRequesterCallbacks callbacks, Object callbacksLock, int requestStructureAppOps, int requestScreenshotAppOps) {
        this.mCallbacks = callbacks;
        this.mCallbacksLock = callbacksLock;
        this.mWindowManager = windowManager;
        this.mService = service;
        this.mContext = context;
        this.mAppOpsManager = appOpsManager;
        this.mRequestStructureAppOps = requestStructureAppOps;
        this.mRequestScreenshotAppOps = requestScreenshotAppOps;
    }

    public void requestAssistData(List<IBinder> activityTokens, boolean fetchData, boolean fetchScreenshot, boolean allowFetchData, boolean allowFetchScreenshot, int callingUid, String callingPackage) {
        int i = callingUid;
        String str = callingPackage;
        if (activityTokens.isEmpty()) {
            tryDispatchRequestComplete();
            return;
        }
        Bitmap bitmap;
        boolean z = false;
        boolean isAssistDataAllowed = false;
        try {
            isAssistDataAllowed = this.mService.isAssistDataAllowedOnCurrentActivity();
        } catch (RemoteException e) {
        }
        boolean isAssistDataAllowed2 = isAssistDataAllowed;
        boolean allowFetchData2 = allowFetchData & isAssistDataAllowed2;
        int i2 = (fetchData && isAssistDataAllowed2 && this.mRequestScreenshotAppOps != -1) ? 1 : 0;
        boolean allowFetchScreenshot2 = allowFetchScreenshot & i2;
        this.mCanceled = false;
        this.mPendingDataCount = 0;
        this.mPendingScreenshotCount = 0;
        this.mAssistData.clear();
        this.mAssistScreenshot.clear();
        Bitmap bitmap2 = null;
        if (fetchData) {
            if (this.mAppOpsManager.checkOpNoThrow(this.mRequestStructureAppOps, i, str) == 0 && allowFetchData2) {
                int numActivities = activityTokens.size();
                i2 = 0;
                while (true) {
                    int i3 = i2;
                    if (i3 >= numActivities) {
                        bitmap = bitmap2;
                        break;
                    }
                    int i4;
                    int numActivities2;
                    Bundle bundle;
                    IBinder topActivity = (IBinder) activityTokens.get(i3);
                    try {
                        MetricsLogger.count(this.mContext, "assist_with_context", 1);
                        Bundle receiverExtras = new Bundle();
                        receiverExtras.putInt(KEY_RECEIVER_EXTRA_INDEX, i3);
                        receiverExtras.putInt(KEY_RECEIVER_EXTRA_COUNT, numActivities);
                        IActivityManager iActivityManager = this.mService;
                        boolean z2 = i3 == 0 ? true : z;
                        boolean z3 = i3 == 0 ? true : z;
                        i4 = i3;
                        numActivities2 = numActivities;
                        bitmap = bitmap2;
                        try {
                            if (iActivityManager.requestAssistContextExtras(1, this, receiverExtras, topActivity, z2, z3)) {
                                this.mPendingDataCount++;
                            } else if (i4 == 0) {
                                if (this.mCallbacks.canHandleReceivedAssistDataLocked()) {
                                    dispatchAssistDataReceived(bitmap);
                                } else {
                                    this.mAssistData.add(bitmap);
                                }
                                allowFetchScreenshot2 = false;
                            }
                        } catch (RemoteException e2) {
                        }
                    } catch (RemoteException e3) {
                        i4 = i3;
                        numActivities2 = numActivities;
                        bundle = bitmap2;
                    }
                    i2 = i4 + 1;
                    Object bitmap22 = bundle;
                    numActivities = numActivities2;
                    z = false;
                }
            } else {
                bitmap = null;
                if (this.mCallbacks.canHandleReceivedAssistDataLocked()) {
                    dispatchAssistDataReceived(bitmap);
                } else {
                    this.mAssistData.add(bitmap);
                }
                allowFetchScreenshot2 = false;
            }
        } else {
            bitmap = null;
        }
        if (fetchScreenshot) {
            if (this.mAppOpsManager.checkOpNoThrow(this.mRequestScreenshotAppOps, i, str) == 0 && allowFetchScreenshot2) {
                try {
                    MetricsLogger.count(this.mContext, "assist_with_screen", 1);
                    this.mPendingScreenshotCount++;
                    this.mWindowManager.requestAssistScreenshot(this);
                } catch (RemoteException e4) {
                }
            } else if (this.mCallbacks.canHandleReceivedAssistDataLocked()) {
                dispatchAssistScreenshotReceived(bitmap);
            } else {
                this.mAssistScreenshot.add(bitmap);
            }
        }
        tryDispatchRequestComplete();
    }

    public void processPendingAssistData() {
        flushPendingAssistData();
        tryDispatchRequestComplete();
    }

    private void flushPendingAssistData() {
        int i;
        int dataCount = this.mAssistData.size();
        int i2 = 0;
        for (i = 0; i < dataCount; i++) {
            dispatchAssistDataReceived((Bundle) this.mAssistData.get(i));
        }
        this.mAssistData.clear();
        i = this.mAssistScreenshot.size();
        while (i2 < i) {
            dispatchAssistScreenshotReceived((Bitmap) this.mAssistScreenshot.get(i2));
            i2++;
        }
        this.mAssistScreenshot.clear();
    }

    public int getPendingDataCount() {
        return this.mPendingDataCount;
    }

    public int getPendingScreenshotCount() {
        return this.mPendingScreenshotCount;
    }

    public void cancel() {
        this.mCanceled = true;
        this.mPendingDataCount = 0;
        this.mPendingScreenshotCount = 0;
        this.mAssistData.clear();
        this.mAssistScreenshot.clear();
    }

    /* JADX WARNING: Missing block: B:12:0x0027, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onHandleAssistData(Bundle data) {
        synchronized (this.mCallbacksLock) {
            if (this.mCanceled) {
                return;
            }
            this.mPendingDataCount--;
            if (this.mCallbacks.canHandleReceivedAssistDataLocked()) {
                flushPendingAssistData();
                dispatchAssistDataReceived(data);
                tryDispatchRequestComplete();
            } else {
                this.mAssistData.add(data);
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0027, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onHandleAssistScreenshot(Bitmap screenshot) {
        synchronized (this.mCallbacksLock) {
            if (this.mCanceled) {
                return;
            }
            this.mPendingScreenshotCount--;
            if (this.mCallbacks.canHandleReceivedAssistDataLocked()) {
                flushPendingAssistData();
                dispatchAssistScreenshotReceived(screenshot);
                tryDispatchRequestComplete();
            } else {
                this.mAssistScreenshot.add(screenshot);
            }
        }
    }

    private void dispatchAssistDataReceived(Bundle data) {
        int activityIndex = 0;
        int activityCount = 0;
        Bundle receiverExtras = data != null ? data.getBundle("receiverExtras") : null;
        if (receiverExtras != null) {
            activityIndex = receiverExtras.getInt(KEY_RECEIVER_EXTRA_INDEX);
            activityCount = receiverExtras.getInt(KEY_RECEIVER_EXTRA_COUNT);
        }
        this.mCallbacks.onAssistDataReceivedLocked(data, activityIndex, activityCount);
    }

    private void dispatchAssistScreenshotReceived(Bitmap screenshot) {
        this.mCallbacks.onAssistScreenshotReceivedLocked(screenshot);
    }

    private void tryDispatchRequestComplete() {
        if (this.mPendingDataCount == 0 && this.mPendingScreenshotCount == 0 && this.mAssistData.isEmpty() && this.mAssistScreenshot.isEmpty()) {
            this.mCallbacks.onAssistRequestCompleted();
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mPendingDataCount=");
        pw.println(this.mPendingDataCount);
        pw.print(prefix);
        pw.print("mAssistData=");
        pw.println(this.mAssistData);
        pw.print(prefix);
        pw.print("mPendingScreenshotCount=");
        pw.println(this.mPendingScreenshotCount);
        pw.print(prefix);
        pw.print("mAssistScreenshot=");
        pw.println(this.mAssistScreenshot);
    }
}
