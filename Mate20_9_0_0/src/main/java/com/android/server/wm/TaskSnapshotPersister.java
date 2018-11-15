package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityManager.TaskSnapshot;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.Process;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.AtomicFile;
import com.android.server.wm.nano.WindowManagerProtos.TaskSnapshotProto;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;

class TaskSnapshotPersister {
    private static final String BITMAP_EXTENSION = ".jpg";
    private static final long DELAY_MS = 100;
    static final boolean DISABLE_FULL_SIZED_BITMAPS = ActivityManager.isLowRamDeviceStatic();
    private static final int MAX_STORE_QUEUE_DEPTH = 2;
    private static final String PROTO_EXTENSION = ".proto";
    private static final int QUALITY = 95;
    private static final String REDUCED_POSTFIX = "_reduced";
    static final float REDUCED_SCALE = (ActivityManager.isLowRamDeviceStatic() ? 0.6f : 0.5f);
    private static final String SNAPSHOTS_DIRNAME = "snapshots";
    private static final String TAG = "WindowManager";
    private final DirectoryResolver mDirectoryResolver;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private boolean mPaused;
    @GuardedBy("mLock")
    private final ArraySet<Integer> mPersistedTaskIdsSinceLastRemoveObsolete = new ArraySet();
    private Thread mPersister = new Thread("TaskSnapshotPersister") {
        public void run() {
            Process.setThreadPriority(10);
            while (true) {
                WriteQueueItem next;
                synchronized (TaskSnapshotPersister.this.mLock) {
                    if (TaskSnapshotPersister.this.mPaused) {
                        next = null;
                    } else {
                        next = (WriteQueueItem) TaskSnapshotPersister.this.mWriteQueue.poll();
                        if (next != null) {
                            next.onDequeuedLocked();
                        }
                    }
                }
                if (next != null) {
                    next.write();
                    SystemClock.sleep(TaskSnapshotPersister.DELAY_MS);
                }
                synchronized (TaskSnapshotPersister.this.mLock) {
                    boolean writeQueueEmpty = TaskSnapshotPersister.this.mWriteQueue.isEmpty();
                    if (writeQueueEmpty || TaskSnapshotPersister.this.mPaused) {
                        try {
                            TaskSnapshotPersister.this.mQueueIdling = writeQueueEmpty;
                            TaskSnapshotPersister.this.mLock.wait();
                            TaskSnapshotPersister.this.mQueueIdling = false;
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            while (true) {
            }
        }
    };
    @GuardedBy("mLock")
    private boolean mQueueIdling;
    private boolean mStarted;
    @GuardedBy("mLock")
    private final ArrayDeque<StoreWriteQueueItem> mStoreQueueItems = new ArrayDeque();
    @GuardedBy("mLock")
    private final ArrayDeque<WriteQueueItem> mWriteQueue = new ArrayDeque();

    interface DirectoryResolver {
        File getSystemDirectoryForUser(int i);
    }

    private abstract class WriteQueueItem {
        abstract void write();

        private WriteQueueItem() {
        }

        /* synthetic */ WriteQueueItem(TaskSnapshotPersister x0, AnonymousClass1 x1) {
            this();
        }

        void onQueuedLocked() {
        }

        void onDequeuedLocked() {
        }
    }

    private class DeleteWriteQueueItem extends WriteQueueItem {
        private final int mTaskId;
        private final int mUserId;

        DeleteWriteQueueItem(int taskId, int userId) {
            super(TaskSnapshotPersister.this, null);
            this.mTaskId = taskId;
            this.mUserId = userId;
        }

        void write() {
            TaskSnapshotPersister.this.deleteSnapshot(this.mTaskId, this.mUserId);
        }
    }

    @VisibleForTesting
    class RemoveObsoleteFilesQueueItem extends WriteQueueItem {
        private final ArraySet<Integer> mPersistentTaskIds;
        private final int[] mRunningUserIds;

        @VisibleForTesting
        RemoveObsoleteFilesQueueItem(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
            super(TaskSnapshotPersister.this, null);
            this.mPersistentTaskIds = new ArraySet(persistentTaskIds);
            this.mRunningUserIds = runningUserIds;
        }

        void write() {
            synchronized (TaskSnapshotPersister.this.mLock) {
                ArraySet<Integer> newPersistedTaskIds = new ArraySet(TaskSnapshotPersister.this.mPersistedTaskIdsSinceLastRemoveObsolete);
            }
            for (int userId : this.mRunningUserIds) {
                File dir = TaskSnapshotPersister.this.getDirectory(userId);
                String[] files = dir.list();
                if (files != null) {
                    for (String file : files) {
                        int taskId = getTaskId(file);
                        if (!(this.mPersistentTaskIds.contains(Integer.valueOf(taskId)) || newPersistedTaskIds.contains(Integer.valueOf(taskId)))) {
                            new File(dir, file).delete();
                        }
                    }
                }
            }
            if (this.mPersistentTaskIds != null) {
                this.mPersistentTaskIds.clear();
            }
        }

        @VisibleForTesting
        int getTaskId(String fileName) {
            if (!fileName.endsWith(TaskSnapshotPersister.PROTO_EXTENSION) && !fileName.endsWith(TaskSnapshotPersister.BITMAP_EXTENSION)) {
                return -1;
            }
            int end = fileName.lastIndexOf(46);
            if (end == -1) {
                return -1;
            }
            String name = fileName.substring(0, end);
            if (name.endsWith(TaskSnapshotPersister.REDUCED_POSTFIX)) {
                name = name.substring(0, name.length() - TaskSnapshotPersister.REDUCED_POSTFIX.length());
            }
            try {
                return Integer.parseInt(name);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }

    private class StoreWriteQueueItem extends WriteQueueItem {
        private final TaskSnapshot mSnapshot;
        private final int mTaskId;
        private final int mUserId;

        StoreWriteQueueItem(int taskId, int userId, TaskSnapshot snapshot) {
            super(TaskSnapshotPersister.this, null);
            this.mTaskId = taskId;
            this.mUserId = userId;
            this.mSnapshot = snapshot;
        }

        @GuardedBy("mLock")
        void onQueuedLocked() {
            TaskSnapshotPersister.this.mStoreQueueItems.offer(this);
        }

        @GuardedBy("mLock")
        void onDequeuedLocked() {
            TaskSnapshotPersister.this.mStoreQueueItems.remove(this);
        }

        void write() {
            if (!TaskSnapshotPersister.this.createDirectory(this.mUserId)) {
                String str = TaskSnapshotPersister.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to create snapshot directory for user dir=");
                stringBuilder.append(TaskSnapshotPersister.this.getDirectory(this.mUserId));
                Slog.e(str, stringBuilder.toString());
            }
            boolean failed = false;
            if (!writeProto()) {
                failed = true;
            }
            if (!writeBuffer()) {
                failed = true;
            }
            if (failed) {
                TaskSnapshotPersister.this.deleteSnapshot(this.mTaskId, this.mUserId);
            }
        }

        boolean writeProto() {
            TaskSnapshotProto proto = new TaskSnapshotProto();
            proto.orientation = this.mSnapshot.getOrientation();
            proto.insetLeft = this.mSnapshot.getContentInsets().left;
            proto.insetTop = this.mSnapshot.getContentInsets().top;
            proto.insetRight = this.mSnapshot.getContentInsets().right;
            proto.insetBottom = this.mSnapshot.getContentInsets().bottom;
            proto.isRealSnapshot = this.mSnapshot.isRealSnapshot();
            proto.windowingMode = this.mSnapshot.getWindowingMode();
            proto.systemUiVisibility = this.mSnapshot.getSystemUiVisibility();
            proto.isTranslucent = this.mSnapshot.isTranslucent();
            byte[] bytes = TaskSnapshotProto.toByteArray(proto);
            File file = TaskSnapshotPersister.this.getProtoFile(this.mTaskId, this.mUserId);
            AtomicFile atomicFile = new AtomicFile(file);
            FileOutputStream fos = null;
            try {
                fos = atomicFile.startWrite();
                fos.write(bytes);
                atomicFile.finishWrite(fos);
                return true;
            } catch (IOException e) {
                atomicFile.failWrite(fos);
                String str = TaskSnapshotPersister.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to open ");
                stringBuilder.append(file);
                stringBuilder.append(" for persisting. ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
                return false;
            }
        }

        boolean writeBuffer() {
            Bitmap bitmap = Bitmap.createHardwareBitmap(this.mSnapshot.getSnapshot());
            if (bitmap == null) {
                Slog.e(TaskSnapshotPersister.TAG, "Invalid task snapshot hw bitmap");
                return false;
            }
            Bitmap swBitmap = bitmap.copy(Config.ARGB_8888, false);
            if (swBitmap == null) {
                Slog.e(TaskSnapshotPersister.TAG, "Invalid task snapshot sw bitmap");
                return false;
            }
            File reducedFile = TaskSnapshotPersister.this.getReducedResolutionBitmapFile(this.mTaskId, this.mUserId);
            Bitmap reduced = this.mSnapshot.isReducedResolution() ? swBitmap : Bitmap.createScaledBitmap(swBitmap, (int) (((float) bitmap.getWidth()) * TaskSnapshotPersister.REDUCED_SCALE), (int) (((float) bitmap.getHeight()) * TaskSnapshotPersister.REDUCED_SCALE), true);
            try {
                FileOutputStream reducedFos = new FileOutputStream(reducedFile);
                if (reduced == null) {
                    Slog.e(TaskSnapshotPersister.TAG, "createScaledBitmap error");
                    reducedFos.close();
                    return false;
                }
                reduced.compress(CompressFormat.JPEG, TaskSnapshotPersister.QUALITY, reducedFos);
                reducedFos.close();
                if (this.mSnapshot.isReducedResolution()) {
                    return true;
                }
                File file = TaskSnapshotPersister.this.getBitmapFile(this.mTaskId, this.mUserId);
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    swBitmap.compress(CompressFormat.JPEG, TaskSnapshotPersister.QUALITY, fos);
                    fos.close();
                    return true;
                } catch (IOException e) {
                    String str = TaskSnapshotPersister.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to open ");
                    stringBuilder.append(file);
                    stringBuilder.append(" for persisting.");
                    Slog.e(str, stringBuilder.toString(), e);
                    return false;
                }
            } catch (IOException e2) {
                String str2 = TaskSnapshotPersister.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to open ");
                stringBuilder2.append(reducedFile);
                stringBuilder2.append(" for persisting.");
                Slog.e(str2, stringBuilder2.toString(), e2);
                return false;
            }
        }
    }

    TaskSnapshotPersister(DirectoryResolver resolver) {
        this.mDirectoryResolver = resolver;
    }

    void start() {
        if (!this.mStarted) {
            this.mStarted = true;
            this.mPersister.start();
        }
    }

    void persistSnapshot(int taskId, int userId, TaskSnapshot snapshot) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.add(Integer.valueOf(taskId));
            sendToQueueLocked(new StoreWriteQueueItem(taskId, userId, snapshot));
        }
    }

    void onTaskRemovedFromRecents(int taskId, int userId) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.remove(Integer.valueOf(taskId));
            sendToQueueLocked(new DeleteWriteQueueItem(taskId, userId));
        }
    }

    void removeObsoleteFiles(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.clear();
            sendToQueueLocked(new RemoveObsoleteFilesQueueItem(persistentTaskIds, runningUserIds));
        }
    }

    void setPaused(boolean paused) {
        synchronized (this.mLock) {
            this.mPaused = paused;
            if (!paused) {
                this.mLock.notifyAll();
            }
        }
    }

    void waitForQueueEmpty() {
        while (true) {
            synchronized (this.mLock) {
                if (this.mWriteQueue.isEmpty() && this.mQueueIdling) {
                    return;
                }
            }
            SystemClock.sleep(DELAY_MS);
        }
        while (true) {
        }
    }

    @GuardedBy("mLock")
    private void sendToQueueLocked(WriteQueueItem item) {
        this.mWriteQueue.offer(item);
        item.onQueuedLocked();
        ensureStoreQueueDepthLocked();
        if (!this.mPaused) {
            this.mLock.notifyAll();
        }
    }

    @GuardedBy("mLock")
    private void ensureStoreQueueDepthLocked() {
        while (this.mStoreQueueItems.size() > 2) {
            StoreWriteQueueItem item = (StoreWriteQueueItem) this.mStoreQueueItems.poll();
            this.mWriteQueue.remove(item);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Queue is too deep! Purged item with taskid=");
            stringBuilder.append(item.mTaskId);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private File getDirectory(int userId) {
        return new File(this.mDirectoryResolver.getSystemDirectoryForUser(userId), SNAPSHOTS_DIRNAME);
    }

    File getProtoFile(int taskId, int userId) {
        File directory = getDirectory(userId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(taskId);
        stringBuilder.append(PROTO_EXTENSION);
        return new File(directory, stringBuilder.toString());
    }

    File getBitmapFile(int taskId, int userId) {
        if (DISABLE_FULL_SIZED_BITMAPS) {
            Slog.wtf(TAG, "This device does not support full sized resolution bitmaps.");
            return null;
        }
        File directory = getDirectory(userId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(taskId);
        stringBuilder.append(BITMAP_EXTENSION);
        return new File(directory, stringBuilder.toString());
    }

    File getReducedResolutionBitmapFile(int taskId, int userId) {
        File directory = getDirectory(userId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(taskId);
        stringBuilder.append(REDUCED_POSTFIX);
        stringBuilder.append(BITMAP_EXTENSION);
        return new File(directory, stringBuilder.toString());
    }

    private boolean createDirectory(int userId) {
        File dir = getDirectory(userId);
        return dir.exists() || dir.mkdirs();
    }

    private void deleteSnapshot(int taskId, int userId) {
        File protoFile = getProtoFile(taskId, userId);
        File bitmapReducedFile = getReducedResolutionBitmapFile(taskId, userId);
        protoFile.delete();
        bitmapReducedFile.delete();
        if (!DISABLE_FULL_SIZED_BITMAPS) {
            getBitmapFile(taskId, userId).delete();
        }
    }
}
