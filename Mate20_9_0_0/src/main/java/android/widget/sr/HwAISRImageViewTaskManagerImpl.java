package android.widget.sr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Vector;

public class HwAISRImageViewTaskManagerImpl extends HwSuperResolutionListener implements HwAISRImageViewTaskManager {
    private static final int DDK_STATUS_NOT_STARTED = 0;
    private static final int DDK_STATUS_PROCESSING = 4;
    private static final int DDK_STATUS_STARTED = 2;
    private static final int DDK_STATUS_STARTING = 1;
    private static final int DDK_STATUS_STOPPING = 3;
    private static final int HISI_DDK_WECHAT_MODE = 1;
    private static final long KEEP_WAITING_DURATION = 60000;
    private static final int MAX_PROCESS_ERROR_COUNT = 30;
    private static final int MAX_START_ERROR_COUNT = 5;
    private static final int SR_IMAGEVIEW_RATIO = 3;
    private static final String SR_TAG = "SuperResolution";
    private static final long TIMEOUT_DURATION = 3000;
    private static HwAISRImageViewTaskManagerImpl sInstance;
    private static boolean sIsDDKStatusAvailable = true;
    private static boolean sIsSuperResolutionSupport = Utils.isSuperResolutionSupport();
    private Runnable mClearWhenTimeoutRunnable = new Runnable() {
        public void run() {
            if (HwAISRImageViewTaskManagerImpl.this.mCurrentSRTaskInfoImpl != null) {
                HwAISRImageViewTaskManagerImpl.this.onTimeOut(HwAISRImageViewTaskManagerImpl.this.mCurrentSRTaskInfoImpl.mSrcBitmap);
            }
        }
    };
    private Context mContext;
    private SRTaskInfoImpl mCurrentSRTaskInfoImpl;
    private int mDDKStatus = 0;
    private Handler mManageHandler;
    private HandlerThread mManageThread;
    private SRMemoryRecorder mMemoryRecorder = new SRMemoryRecorder();
    private int mProcessErrorCount = 0;
    private int mStartErrorCount = 0;
    private HwSuperResolution mSuperResolution;
    private Vector<SRTaskInfoImpl> mTaskQueue = new Vector();

    public static class SRTaskInfoImpl implements SRTaskInfo {
        private WeakReference<SRTaskCallback> mCallback;
        private Bitmap mSrcBitmap;
        private WeakReference<Drawable> mSrcDrawable;

        /* synthetic */ SRTaskInfoImpl(SRTaskCallback x0, Bitmap x1, Drawable x2, AnonymousClass1 x3) {
            this(x0, x1, x2);
        }

        private SRTaskInfoImpl(SRTaskCallback callback) {
            this(callback, null, null);
        }

        private SRTaskInfoImpl(SRTaskCallback callback, Bitmap src, Drawable drawable) {
            this.mCallback = new WeakReference(callback);
            this.mSrcBitmap = src;
            this.mSrcDrawable = new WeakReference(drawable);
        }
    }

    public static boolean isSuperResolutionAvailable() {
        return sIsSuperResolutionSupport && sIsDDKStatusAvailable;
    }

    private static void setIsDDKStatusAvailable(boolean flag) {
        sIsDDKStatusAvailable = flag;
    }

    public static synchronized HwAISRImageViewTaskManagerImpl getInstance(Context context) {
        HwAISRImageViewTaskManagerImpl hwAISRImageViewTaskManagerImpl;
        synchronized (HwAISRImageViewTaskManagerImpl.class) {
            if (sInstance == null) {
                Context appContext = null;
                if (context != null) {
                    appContext = context.getApplicationContext();
                }
                sInstance = new HwAISRImageViewTaskManagerImpl(appContext);
                Log.d(SR_TAG, "HwEMUI create ManagerImpl instance");
            }
            hwAISRImageViewTaskManagerImpl = sInstance;
        }
        return hwAISRImageViewTaskManagerImpl;
    }

    private HwAISRImageViewTaskManagerImpl(Context context) {
        createManageThread();
        this.mContext = context;
    }

    private HwSuperResolution getSuperResolution() {
        if (this.mSuperResolution == null) {
            this.mSuperResolution = new HwSuperResolution(this.mContext, this);
        }
        return this.mSuperResolution;
    }

    private void increaseStartErrorCount() {
        Log.d(SR_TAG, "increaseStartErrorCount");
        this.mStartErrorCount++;
        if (this.mStartErrorCount >= 5) {
            Log.w(SR_TAG, "increaseStartErrorCount: start error too many times");
            setIsDDKStatusAvailable(false);
        }
    }

    private void increaseProcessErrorCount() {
        Log.d(SR_TAG, "increaseProcessErrorCount");
        this.mProcessErrorCount++;
        if (this.mProcessErrorCount >= 30) {
            Log.w(SR_TAG, "increaseProcessErrorCount: process error too many times");
            setIsDDKStatusAvailable(false);
        }
    }

    private void resetStatus() {
        this.mDDKStatus = 0;
        this.mCurrentSRTaskInfoImpl = null;
        this.mTaskQueue.clear();
        this.mManageHandler.removeCallbacksAndMessages(null);
    }

    private void createManageThread() {
        this.mManageThread = new HandlerThread("SRManagerThread");
        this.mManageThread.start();
        this.mManageHandler = new Handler(this.mManageThread.getLooper());
    }

    private boolean isCurrentTaskProcessing() {
        return this.mCurrentSRTaskInfoImpl != null;
    }

    public SRTaskInfo postNewTask(SRTaskCallback callback, Drawable drawable) {
        if (!isSuperResolutionAvailable()) {
            Log.w(SR_TAG, "postNewTask: SuperResolution is not available now.");
            return null;
        } else if (callback == null || drawable == null) {
            return null;
        } else {
            SRTaskInfoImpl taskInfo = new SRTaskInfoImpl(callback, null, drawable, null);
            postNewTask(taskInfo);
            return taskInfo;
        }
    }

    private void postNewTask(final SRTaskInfoImpl taskInfo) {
        Log.d(SR_TAG, "postNewTask: ");
        if (taskInfo != null) {
            this.mManageHandler.post(new Runnable() {
                public void run() {
                    HwAISRImageViewTaskManagerImpl.this.scheduleTask(taskInfo);
                }
            });
        }
    }

    private synchronized void scheduleTask(SRTaskInfoImpl taskInfo) {
        this.mTaskQueue.add(taskInfo);
        if (!isCurrentTaskProcessing() && this.mTaskQueue.size() == 1) {
            getAndDoNextTask();
        }
    }

    private synchronized boolean doNewTask(SRTaskInfoImpl taskInfo) {
        String str = SR_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("doNewTask: taskInfo = ");
        stringBuilder.append(taskInfo);
        Log.d(str, stringBuilder.toString());
        this.mCurrentSRTaskInfoImpl = taskInfo;
        try {
            if (isSuperResolutionAvailable()) {
                taskInfo.mSrcBitmap = SRUtils.drawableToBitmap(taskInfo.mSrcDrawable == null ? null : (Drawable) taskInfo.mSrcDrawable.get());
                if (taskInfo.mSrcBitmap == null) {
                    Log.w(SR_TAG, "doNewTask: mSrcBitmap is empty ! ");
                    this.mDDKStatus = 4;
                    return false;
                } else if (!enoughRoomForSize((taskInfo.mSrcBitmap.getByteCount() * 3) * 3)) {
                    Log.w(SR_TAG, "doNewTask: there is not enough room ! ");
                    this.mDDKStatus = 4;
                    return false;
                } else if (process(taskInfo.mSrcBitmap)) {
                    this.mManageHandler.postDelayed(this.mClearWhenTimeoutRunnable, TIMEOUT_DURATION);
                    return true;
                } else {
                    Log.w(SR_TAG, "doNewTask: process fail");
                    return false;
                }
            }
            Log.w(SR_TAG, "postNewTask: SuperResolution is not available now.");
            this.mDDKStatus = 4;
            return false;
        } catch (Exception e) {
            Log.w(SR_TAG, "doNewTask: some exception happened!", e);
            this.mDDKStatus = 4;
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:26:0x006f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void getAndDoNextTask() {
        Log.d(SR_TAG, "getAndDoNextTask: ");
        if (isCurrentTaskProcessing()) {
            Log.w(SR_TAG, "getAndDoNextTask: there is a task still doing. We shall never call this method at this point. ");
        } else if (this.mTaskQueue.size() == 0) {
            Log.w(SR_TAG, "getAndDoNextTask: the taskQueue is empty. ");
        } else {
            int i = this.mDDKStatus;
            if (i == 0) {
                start();
            } else if (i != 2) {
                String str = SR_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getAndDoNextTask: some exception mStatus = ");
                stringBuilder.append(this.mDDKStatus);
                Log.w(str, stringBuilder.toString());
            } else {
                SRTaskInfoImpl taskInfo = (SRTaskInfoImpl) this.mTaskQueue.get(0);
                this.mTaskQueue.remove(0);
                if (!(doNewTask(taskInfo) || this.mCurrentSRTaskInfoImpl == null)) {
                    onProcessFail(this.mCurrentSRTaskInfoImpl.mSrcBitmap);
                }
            }
        }
    }

    public void postCancelTask(final SRTaskInfo taskInfo) {
        if (!isSuperResolutionAvailable()) {
            Log.w(SR_TAG, "postCancelTask: SuperResolution is not available now.");
        } else if (taskInfo instanceof SRTaskInfoImpl) {
            this.mManageHandler.post(new Runnable() {
                public void run() {
                    HwAISRImageViewTaskManagerImpl.this.doCancelTask((SRTaskInfoImpl) taskInfo);
                }
            });
        } else {
            Log.w(SR_TAG, "postCancelTask: taskInfo is not SRTaskInfoImpl");
        }
    }

    /* JADX WARNING: Missing block: B:28:0x0070, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void doCancelTask(SRTaskInfoImpl taskInfo) {
        String str = SR_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("doCancelTask: taskInfo = ");
        stringBuilder.append(taskInfo);
        Log.d(str, stringBuilder.toString());
        if (!isSuperResolutionAvailable()) {
            Log.w(SR_TAG, "doCancelTask: SuperResolution is not available now.");
        } else if (taskInfo == null) {
            Log.w(SR_TAG, "doCancelTask: can't cancel an null taskInfo");
        } else if (taskInfo != this.mCurrentSRTaskInfoImpl) {
            Iterator it = this.mTaskQueue.iterator();
            while (it.hasNext()) {
                if (taskInfo == ((SRTaskInfoImpl) it.next())) {
                    this.mTaskQueue.remove(taskInfo);
                    str = SR_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("doCancelTask: remove taskInfo = ");
                    stringBuilder2.append(taskInfo);
                    Log.d(str, stringBuilder2.toString());
                    return;
                }
            }
            Log.w(SR_TAG, "doCancelTask: can't find taskInfo, it has been processed or removed");
        }
    }

    private synchronized void clearCurrentTaskAndGetNext() {
        this.mManageHandler.removeCallbacks(this.mClearWhenTimeoutRunnable);
        this.mCurrentSRTaskInfoImpl = null;
        this.mDDKStatus = 2;
        if (this.mTaskQueue.size() == 0) {
            stop();
        } else {
            getAndDoNextTask();
        }
    }

    public void addMemory(int size) {
        this.mMemoryRecorder.add(size);
    }

    public void removeMemory(int size) {
        this.mMemoryRecorder.remove(size);
    }

    public boolean enoughRoomForSize(int size) {
        return this.mMemoryRecorder.enoughRoomForSize(size);
    }

    private synchronized boolean start() {
        Log.d(SR_TAG, "start: ");
        String str;
        if (this.mDDKStatus == 0) {
            this.mDDKStatus = 1;
            int result = getSuperResolution().start(1);
            if (result == 0) {
                Log.d(SR_TAG, "start: command success");
                return true;
            }
            str = SR_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start: command fail, result = ");
            stringBuilder.append(result);
            Log.w(str, stringBuilder.toString());
            if (result == 16) {
                Log.w(SR_TAG, "start: already called");
            } else {
                this.mSuperResolution = null;
            }
            increaseStartErrorCount();
            resetStatus();
        } else {
            str = SR_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("start: DDK status is ");
            stringBuilder2.append(this.mDDKStatus);
            stringBuilder2.append(". Can not call start now");
            Log.w(str, stringBuilder2.toString());
        }
        return false;
    }

    private synchronized boolean process(Bitmap bitmap) {
        Log.d(SR_TAG, "process: ");
        if (this.mDDKStatus == 2) {
            this.mDDKStatus = 4;
            int result = getSuperResolution().process(bitmap, 3);
            if (result == 0) {
                Log.d(SR_TAG, "process: command success");
                return true;
            }
            String str = SR_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("process: command fail, result = ");
            stringBuilder.append(result);
            Log.w(str, stringBuilder.toString());
            if (result == 2 || result == 18) {
                increaseProcessErrorCount();
            }
        } else {
            String str2 = SR_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("process: DDK status is ");
            stringBuilder2.append(this.mDDKStatus);
            stringBuilder2.append(". Can not call process now");
            Log.w(str2, stringBuilder2.toString());
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:14:0x0064, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized boolean stop() {
        Log.d(SR_TAG, "stop: ");
        if (this.mDDKStatus == 2) {
            this.mDDKStatus = 3;
            int result = getSuperResolution().stop();
            if (result == 0) {
                Log.d(SR_TAG, "stop: command success");
                return true;
            }
            String str = SR_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stop: command fail, result = ");
            stringBuilder.append(result);
            Log.w(str, stringBuilder.toString());
            this.mSuperResolution = null;
            this.mStartErrorCount = 0;
            this.mProcessErrorCount = 0;
            resetStatus();
        } else {
            String str2 = SR_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("stop: DDK status is ");
            stringBuilder2.append(this.mDDKStatus);
            stringBuilder2.append(". Can not call process now");
            Log.w(str2, stringBuilder2.toString());
        }
    }

    public synchronized void onError(int errCode) {
        super.onError(errCode);
        String str = SR_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onError: error code = ");
        stringBuilder.append(errCode);
        Log.w(str, stringBuilder.toString());
        int i = this.mDDKStatus;
        if (i != 1) {
            switch (i) {
                case 3:
                    this.mStartErrorCount = 0;
                    this.mProcessErrorCount = 0;
                    resetStatus();
                    break;
                case 4:
                    if (this.mCurrentSRTaskInfoImpl != null) {
                        onProcessFail(this.mCurrentSRTaskInfoImpl.mSrcBitmap);
                        break;
                    }
                    break;
                default:
                    str = SR_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onError: DDK status is ");
                    stringBuilder.append(this.mDDKStatus);
                    stringBuilder.append(". Abnormal callback.");
                    Log.w(str, stringBuilder.toString());
                    break;
            }
        }
        this.mSuperResolution = null;
        increaseStartErrorCount();
        resetStatus();
    }

    /* JADX WARNING: Missing block: B:22:0x005a, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void onProcessDone(Bitmap src, Bitmap des) {
        if (this.mDDKStatus != 4) {
            String str = SR_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onProcessDone: DDK status is ");
            stringBuilder.append(this.mDDKStatus);
            stringBuilder.append(". Abnormal callback.");
            Log.w(str, stringBuilder.toString());
        } else if (this.mCurrentSRTaskInfoImpl == null || this.mCurrentSRTaskInfoImpl.mSrcBitmap != src) {
            Log.w(SR_TAG, "onProcessDone: this task doesn't match");
        } else {
            SRTaskCallback callback = this.mCurrentSRTaskInfoImpl.mCallback == null ? null : (SRTaskCallback) this.mCurrentSRTaskInfoImpl.mCallback.get();
            SRTaskInfoImpl taskInfo = this.mCurrentSRTaskInfoImpl;
            if (callback != null) {
                callback.onSRTaskSuccess(taskInfo, des);
            }
            clearCurrentTaskAndGetNext();
        }
    }

    public synchronized void onTimeOut(Bitmap bitmap) {
        Log.d(SR_TAG, "onTimeOut: ");
        if (this.mDDKStatus != 4) {
            String str = SR_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onTimeOut: DDK status is ");
            stringBuilder.append(this.mDDKStatus);
            stringBuilder.append(". Abnormal callback.");
            Log.w(str, stringBuilder.toString());
            return;
        }
        onProcessFail(bitmap);
    }

    public synchronized void onServiceDied() {
        Log.w(SR_TAG, "onServiceDied: ");
        if (this.mDDKStatus == 4 && this.mCurrentSRTaskInfoImpl != null) {
            onProcessFail(this.mCurrentSRTaskInfoImpl.mSrcBitmap);
        }
        this.mSuperResolution = null;
        this.mStartErrorCount = 0;
        this.mProcessErrorCount = 0;
        resetStatus();
    }

    public synchronized void onStartDone() {
        Log.d(SR_TAG, "onStartDone: ");
        if (this.mDDKStatus == 1) {
            Log.d(SR_TAG, "onStartDone: DDK has been started");
            this.mDDKStatus = 2;
            this.mStartErrorCount = 0;
            if (this.mTaskQueue.size() > 0) {
                getAndDoNextTask();
            } else if (this.mTaskQueue.size() == 0) {
                Log.d(SR_TAG, "onStartDone: no task to do");
                stop();
            }
        } else {
            String str = SR_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onStartDone: DDK status is ");
            stringBuilder.append(this.mDDKStatus);
            stringBuilder.append(". Abnormal callback.");
            Log.w(str, stringBuilder.toString());
        }
    }

    public synchronized void onStopDone() {
        Log.d(SR_TAG, "onStopDone: ");
        if (this.mDDKStatus == 3) {
            Log.d(SR_TAG, "onStopDone: DDK has been stopped");
            this.mDDKStatus = 0;
            this.mStartErrorCount = 0;
            this.mProcessErrorCount = 0;
            if (this.mTaskQueue.size() > 0) {
                getAndDoNextTask();
            }
        } else {
            String str = SR_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onStopDone:  DDK status is ");
            stringBuilder.append(this.mDDKStatus);
            stringBuilder.append(". Abnormal callback.");
            Log.w(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:22:0x0061, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void onProcessFail(Bitmap srcBitmap) {
        Log.d(SR_TAG, "onProcessFail: ");
        if (this.mDDKStatus != 4) {
            String str = SR_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onProcessFail: DDK status is ");
            stringBuilder.append(this.mDDKStatus);
            stringBuilder.append(". Abnormal callback.");
            Log.w(str, stringBuilder.toString());
        } else if (this.mCurrentSRTaskInfoImpl == null || this.mCurrentSRTaskInfoImpl.mSrcBitmap != srcBitmap) {
            Log.w(SR_TAG, "onProcessFail: this task doesn't match");
        } else {
            SRTaskCallback callback = this.mCurrentSRTaskInfoImpl.mCallback == null ? null : (SRTaskCallback) this.mCurrentSRTaskInfoImpl.mCallback.get();
            SRTaskInfoImpl taskInfo = this.mCurrentSRTaskInfoImpl;
            if (callback != null) {
                callback.onSRTaskFail(taskInfo);
            }
            clearCurrentTaskAndGetNext();
        }
    }
}
