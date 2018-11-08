package com.android.server.am;

import android.app.ActivityManager.TaskThumbnail;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.HwPCUtils;
import com.android.internal.os.BackgroundThread;
import com.android.server.wm.HwWindowManagerService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import libcore.io.IoUtils;

public class HwPCMultiWindowThumbnailPersister {
    private static final int DELETE_THUMBNAIL_DELAY = 2000;
    private static final int MSG_DELETE_THUMBNAIL = 1;
    private static final int MSG_SAVE_THUMBNAIL = 2;
    private static final int MSG_SCREENSHOT = 3;
    private static final int SAVE_THUMBNAIL_DELAY = 2000;
    private static final int SCREENSHOT_DELAY = 3000;
    private static final String TASK_THUMBNAILS_PATH = "/data/system/hw_recent/task_thumbnails";
    private static final String THUMBNAIL_EXTENSION = ".png";
    private static final String THUMBNAIL_SUFFIX = "_task_thumbnail";
    private static final File ThumbnailDir = new File(getTaskThumbnailsPath());
    private String TAG = "HwPCMultiWindowThumbnailPersister";
    private HwActivityManagerService mService;
    private TaskStackListener mTaskStackListener = new TaskStackListener() {
        public void onTaskRemoved(final int taskId) throws RemoteException {
            HwPCMultiWindowThumbnailPersister.this.mWorkerHandler.post(new Runnable() {
                public void run() {
                    TaskThumbnailItem item;
                    synchronized (HwPCMultiWindowThumbnailPersister.this) {
                        item = (TaskThumbnailItem) HwPCMultiWindowThumbnailPersister.this.mTasks.remove(Integer.valueOf(taskId));
                    }
                    HwPCMultiWindowThumbnailPersister.this.mWorkerHandler.sendMessageDelayed(HwPCMultiWindowThumbnailPersister.this.mWorkerHandler.obtainMessage(1, item), 2000);
                }
            });
        }

        public void onTaskCreated(int taskId, ComponentName componentName) throws RemoteException {
            synchronized (HwPCMultiWindowThumbnailPersister.this) {
                HwPCMultiWindowThumbnailPersister.this.mTasks.put(Integer.valueOf(taskId), new TaskThumbnailItem(taskId));
            }
            HwPCMultiWindowThumbnailPersister.this.mWorkerHandler.sendMessageDelayed(HwPCMultiWindowThumbnailPersister.this.mWorkerHandler.obtainMessage(3, Integer.valueOf(taskId)), 3000);
        }
    };
    HashMap<Integer, TaskThumbnailItem> mTasks = new HashMap();
    Handler mWorkerHandler = new WorkerHandler(BackgroundThread.getHandler().getLooper());

    private static class TaskThumbnailItem {
        String mReason;
        final int mTaskId;
        Bitmap mThumbnail;
        final File mThumbnailFile;

        TaskThumbnailItem(int taskId) {
            this.mTaskId = taskId;
            this.mThumbnailFile = new File(HwPCMultiWindowThumbnailPersister.ThumbnailDir, String.valueOf(taskId) + HwPCMultiWindowThumbnailPersister.THUMBNAIL_SUFFIX + HwPCMultiWindowThumbnailPersister.THUMBNAIL_EXTENSION);
        }

        public String getReason() {
            return this.mReason;
        }
    }

    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            Object obj;
            Throwable th;
            TaskThumbnailItem item;
            switch (msg.what) {
                case 1:
                    try {
                        item = msg.obj;
                        if (!(item == null || !item.mThumbnailFile.exists() || item.mThumbnailFile.delete())) {
                            HwPCUtils.log(HwPCMultiWindowThumbnailPersister.this.TAG, "MSG_DELETE_THUMBNAIL delete thumbnail file failed!!!");
                            break;
                        }
                    } catch (Exception e) {
                        HwPCUtils.log(HwPCMultiWindowThumbnailPersister.this.TAG, "MSG_DELETE_THUMBNAIL failed!!!");
                        break;
                    }
                case 2:
                    synchronized (HwPCMultiWindowThumbnailPersister.this) {
                        AutoCloseable autoCloseable = null;
                        try {
                            item = (TaskThumbnailItem) msg.obj;
                            if (!(item == null || item.mThumbnail == null)) {
                                FileOutputStream imageFile = new FileOutputStream(item.mThumbnailFile);
                                try {
                                    item.mThumbnail.compress(CompressFormat.PNG, 100, imageFile);
                                    item.mThumbnail = null;
                                    autoCloseable = imageFile;
                                } catch (Exception e2) {
                                    obj = imageFile;
                                    try {
                                        HwPCUtils.log(HwPCMultiWindowThumbnailPersister.this.TAG, "MSG_SAVE_THUMBNAIL failed!!!");
                                        IoUtils.closeQuietly(autoCloseable);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        IoUtils.closeQuietly(autoCloseable);
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    obj = imageFile;
                                    IoUtils.closeQuietly(autoCloseable);
                                    throw th;
                                }
                            }
                            IoUtils.closeQuietly(autoCloseable);
                        } catch (Exception e3) {
                            HwPCUtils.log(HwPCMultiWindowThumbnailPersister.this.TAG, "MSG_SAVE_THUMBNAIL failed!!!");
                            IoUtils.closeQuietly(autoCloseable);
                        }
                    }
                case 3:
                    int taskId = ((Integer) msg.obj).intValue();
                    synchronized (HwPCMultiWindowThumbnailPersister.this) {
                        item = (TaskThumbnailItem) HwPCMultiWindowThumbnailPersister.this.mTasks.get(Integer.valueOf(taskId));
                        if (item != null && item.mThumbnail == null) {
                            if (!item.mThumbnailFile.exists()) {
                            }
                        }
                    }
                    break;
            }
        }
    }

    private static String getTaskThumbnailsPath() {
        return TASK_THUMBNAILS_PATH;
    }

    public HwPCMultiWindowThumbnailPersister(ActivityManagerService service) {
        this.mService = (HwActivityManagerService) service;
        if (ThumbnailDir.isDirectory() && ThumbnailDir.exists()) {
            String[] children = ThumbnailDir.list();
            if (children != null) {
                for (String file : children) {
                    if (!new File(ThumbnailDir, file).delete()) {
                        HwPCUtils.log(this.TAG, "HwPCMultiWindowThumbnailPersister delete fail!!!");
                    }
                }
            }
        }
        if (!ThumbnailDir.mkdirs()) {
            HwPCUtils.log(this.TAG, "HwPCMultiWindowThumbnailPersister mkdirs fail!!!");
        }
        this.mService.registerHwTaskStackListener(this.mTaskStackListener);
    }

    public TaskThumbnail getTaskThumbnail(int taskId) {
        TaskThumbnail taskThumbnail = new TaskThumbnail();
        Bitmap bitmap = null;
        synchronized (this.mService) {
            TaskRecord tr = this.mService.mStackSupervisor.anyTaskForIdLocked(taskId, 1, -1);
            if (!(tr == null || tr.mStack == null || !(this.mService.mWindowManager instanceof HwWindowManagerService))) {
                ActivityRecord r = tr.topRunningActivityLocked();
                if (r != null) {
                    bitmap = ((HwWindowManagerService) this.mService.mWindowManager).getTaskSnapshotForPc(r.getDisplayId(), r.appToken);
                }
            }
        }
        if (bitmap != null) {
            updateThumbnail(taskId, bitmap, "getTaskThumbnail");
        }
        synchronized (this) {
            TaskThumbnailItem item = (TaskThumbnailItem) this.mTasks.get(Integer.valueOf(taskId));
            if (item != null) {
                taskThumbnail.mainThumbnail = item.mThumbnail;
                if (taskThumbnail.mainThumbnail == null && item.mThumbnailFile.exists()) {
                    try {
                        taskThumbnail.thumbnailFileDescriptor = ParcelFileDescriptor.open(item.mThumbnailFile, 268435456);
                    } catch (FileNotFoundException e) {
                        HwPCUtils.log(this.TAG, "getTaskThumbnail FileNotFoundException");
                    }
                }
            }
        }
        return taskThumbnail;
    }

    private void updateThumbnail(int taskId, Bitmap thumbnail, String reason) {
        HwPCUtils.log(this.TAG, "updateThumbnail: taskId " + taskId + " reason " + reason + " thumbnail " + thumbnail);
        synchronized (this) {
            TaskThumbnailItem item = (TaskThumbnailItem) this.mTasks.get(Integer.valueOf(taskId));
            if (!(item == null || thumbnail == null)) {
                item.mThumbnail = thumbnail;
                item.mReason = reason;
                this.mWorkerHandler.removeMessages(2, item);
                this.mWorkerHandler.sendMessageDelayed(this.mWorkerHandler.obtainMessage(2, item), 2000);
            }
        }
    }
}
