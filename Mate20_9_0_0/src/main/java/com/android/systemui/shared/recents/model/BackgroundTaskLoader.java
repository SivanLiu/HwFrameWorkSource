package com.android.systemui.shared.recents.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.android.systemui.shared.system.ActivityManagerWrapper;

class BackgroundTaskLoader implements Runnable {
    static boolean DEBUG = false;
    static String TAG = "BackgroundTaskLoader";
    private boolean mCancelled;
    private Context mContext;
    private final IconLoader mIconLoader;
    private final TaskResourceLoadQueue mLoadQueue;
    private final HandlerThread mLoadThread;
    private final Handler mLoadThreadHandler;
    private final Handler mMainThreadHandler = new Handler();
    private final OnIdleChangedListener mOnIdleChangedListener;
    private boolean mStarted;
    private boolean mWaitingOnLoadQueue;

    interface OnIdleChangedListener {
        void onIdleChanged(boolean z);
    }

    public BackgroundTaskLoader(TaskResourceLoadQueue loadQueue, IconLoader iconLoader, OnIdleChangedListener onIdleChangedListener) {
        this.mLoadQueue = loadQueue;
        this.mIconLoader = iconLoader;
        this.mOnIdleChangedListener = onIdleChangedListener;
        this.mLoadThread = new HandlerThread("Recents-TaskResourceLoader", 10);
        this.mLoadThread.start();
        this.mLoadThreadHandler = new Handler(this.mLoadThread.getLooper());
    }

    void start(Context context) {
        this.mContext = context;
        this.mCancelled = false;
        if (this.mStarted) {
            synchronized (this.mLoadThread) {
                this.mLoadThread.notifyAll();
            }
            return;
        }
        this.mStarted = true;
        this.mLoadThreadHandler.post(this);
    }

    void stop() {
        this.mCancelled = true;
        if (this.mWaitingOnLoadQueue) {
            this.mContext = null;
        }
    }

    public void run() {
        while (true) {
            if (this.mCancelled) {
                this.mContext = null;
                synchronized (this.mLoadThread) {
                    try {
                        this.mLoadThread.wait();
                    } catch (InterruptedException e) {
                        Log.e("TaskResourceLoadQueue", "InterruptedException", e);
                    }
                }
            } else {
                processLoadQueueItem();
                if (!this.mCancelled && this.mLoadQueue.isEmpty()) {
                    synchronized (this.mLoadQueue) {
                        try {
                            this.mWaitingOnLoadQueue = true;
                            this.mMainThreadHandler.post(new -$$Lambda$BackgroundTaskLoader$gaMb8n3irXHj3SpODGi50cngupE(this));
                            this.mLoadQueue.wait();
                            this.mMainThreadHandler.post(new -$$Lambda$BackgroundTaskLoader$XRsMGIp0x8MAJ36UKSTd3DJ9dTg(this));
                            this.mWaitingOnLoadQueue = false;
                        } catch (InterruptedException e2) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("run,InterruptedException: ");
                            stringBuilder.append(e2.getMessage());
                            Log.e(str, stringBuilder.toString());
                        }
                    }
                }
            }
        }
    }

    private void processLoadQueueItem() {
        Task t = this.mLoadQueue.nextTask();
        if (t != null) {
            Drawable icon = this.mIconLoader.getIcon(t);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Loading thumbnail: ");
                stringBuilder.append(t.key);
                Log.d(str, stringBuilder.toString());
            }
            ThumbnailData thumbnailData = ActivityManagerWrapper.getInstance().getTaskThumbnail(t.key.id, true);
            if (!this.mCancelled) {
                this.mMainThreadHandler.post(new -$$Lambda$BackgroundTaskLoader$mJeiv3P4w5EJwXqKPoDi48s7tFI(t, thumbnailData, icon));
            }
        }
    }
}
