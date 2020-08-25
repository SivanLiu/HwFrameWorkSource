package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Process;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.AtomicFile;
import com.android.server.wm.nano.WindowManagerProtos;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;

class TaskSnapshotPersister {
    private static final String BITMAP_EXTENSION = ".jpg";
    private static final long DELAY_MS = 100;
    static final boolean DISABLE_FULL_SIZED_BITMAPS = ActivityManager.isLowRamDeviceStatic();
    private static final float LOW_RAM_RECENTS_REDUCED_SCALE = 0.1f;
    private static final float LOW_RAM_REDUCED_SCALE = 0.6f;
    private static final int MAX_STORE_QUEUE_DEPTH = 2;
    private static final String PROTO_EXTENSION = ".proto";
    private static final int QUALITY = 95;
    private static final String REDUCED_POSTFIX = "_reduced";
    private static final float REDUCED_SCALE = 0.5f;
    private static final String SNAPSHOTS_DIRNAME = "snapshots";
    private static final String TAG = "WindowManager";
    private final DirectoryResolver mDirectoryResolver;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public boolean mPaused;
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public final ArraySet<Integer> mPersistedTaskIdsSinceLastRemoveObsolete = new ArraySet<>();
    private Thread mPersister = new Thread("TaskSnapshotPersister") {
        /* class com.android.server.wm.TaskSnapshotPersister.AnonymousClass1 */

        public void run() {
            WriteQueueItem next;
            Process.setThreadPriority(10);
            while (true) {
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
                            boolean unused = TaskSnapshotPersister.this.mQueueIdling = writeQueueEmpty;
                            TaskSnapshotPersister.this.mLock.wait();
                            boolean unused2 = TaskSnapshotPersister.this.mQueueIdling = false;
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public boolean mQueueIdling;
    /* access modifiers changed from: private */
    public final float mReducedScale;
    private boolean mStarted;
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public final ArrayDeque<StoreWriteQueueItem> mStoreQueueItems = new ArrayDeque<>();
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public final ArrayDeque<WriteQueueItem> mWriteQueue = new ArrayDeque<>();

    interface DirectoryResolver {
        File getSystemDirectoryForUser(int i);
    }

    TaskSnapshotPersister(WindowManagerService service, DirectoryResolver resolver) {
        this.mDirectoryResolver = resolver;
        if (service.mLowRamTaskSnapshotsAndRecents) {
            this.mReducedScale = LOW_RAM_RECENTS_REDUCED_SCALE;
        } else {
            this.mReducedScale = ActivityManager.isLowRamDeviceStatic() ? LOW_RAM_REDUCED_SCALE : 0.5f;
        }
    }

    /* access modifiers changed from: package-private */
    public void start() {
        if (!this.mStarted) {
            this.mStarted = true;
            this.mPersister.start();
        }
    }

    /* access modifiers changed from: package-private */
    public void persistSnapshot(int taskId, int userId, ActivityManager.TaskSnapshot snapshot) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.add(Integer.valueOf(taskId));
            sendToQueueLocked(new StoreWriteQueueItem(taskId, userId, snapshot));
        }
    }

    /* access modifiers changed from: package-private */
    public void persistSnapshot(int taskId, int userId, ActivityManager.TaskSnapshot snapshot, int color) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.add(Integer.valueOf(taskId));
            sendToQueueLocked(new StoreWriteQueueItem(this, taskId, userId, snapshot, color));
        }
    }

    /* access modifiers changed from: package-private */
    public void onTaskRemovedFromRecents(int taskId, int userId) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.remove(Integer.valueOf(taskId));
            sendToQueueLocked(new DeleteWriteQueueItem(taskId, userId));
        }
    }

    /* access modifiers changed from: package-private */
    public void removeObsoleteFiles(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.clear();
            sendToQueueLocked(new RemoveObsoleteFilesQueueItem(persistentTaskIds, runningUserIds));
        }
    }

    /* access modifiers changed from: package-private */
    public void setPaused(boolean paused) {
        synchronized (this.mLock) {
            this.mPaused = paused;
            if (!paused) {
                this.mLock.notifyAll();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public float getReducedScale() {
        return this.mReducedScale;
    }

    /* access modifiers changed from: package-private */
    public void waitForQueueEmpty() {
        while (true) {
            synchronized (this.mLock) {
                if (this.mWriteQueue.isEmpty() && this.mQueueIdling) {
                    return;
                }
            }
            SystemClock.sleep(DELAY_MS);
        }
    }

    @GuardedBy({"mLock"})
    private void sendToQueueLocked(WriteQueueItem item) {
        this.mWriteQueue.offer(item);
        item.onQueuedLocked();
        ensureStoreQueueDepthLocked();
        if (!this.mPaused) {
            this.mLock.notifyAll();
        }
    }

    @GuardedBy({"mLock"})
    private void ensureStoreQueueDepthLocked() {
        while (this.mStoreQueueItems.size() > 2) {
            StoreWriteQueueItem item = this.mStoreQueueItems.poll();
            this.mWriteQueue.remove(item);
            Slog.i(TAG, "Queue is too deep! Purged item with taskid=" + item.mTaskId);
        }
    }

    /* access modifiers changed from: private */
    public File getDirectory(int userId) {
        return new File(this.mDirectoryResolver.getSystemDirectoryForUser(userId), SNAPSHOTS_DIRNAME);
    }

    /* access modifiers changed from: package-private */
    public File getProtoFile(int taskId, int userId) {
        File directory = getDirectory(userId);
        return new File(directory, taskId + PROTO_EXTENSION);
    }

    /* access modifiers changed from: package-private */
    public File getBitmapFile(int taskId, int userId) {
        if (DISABLE_FULL_SIZED_BITMAPS) {
            Slog.wtf(TAG, "This device does not support full sized resolution bitmaps.");
            return null;
        }
        File directory = getDirectory(userId);
        return new File(directory, taskId + BITMAP_EXTENSION);
    }

    /* access modifiers changed from: package-private */
    public File getReducedResolutionBitmapFile(int taskId, int userId) {
        File directory = getDirectory(userId);
        return new File(directory, taskId + REDUCED_POSTFIX + BITMAP_EXTENSION);
    }

    /* access modifiers changed from: private */
    public boolean createDirectory(int userId) {
        File dir = getDirectory(userId);
        return dir.exists() || dir.mkdirs();
    }

    /* access modifiers changed from: private */
    public void deleteSnapshot(int taskId, int userId) {
        File protoFile = getProtoFile(taskId, userId);
        File bitmapReducedFile = getReducedResolutionBitmapFile(taskId, userId);
        protoFile.delete();
        bitmapReducedFile.delete();
        if (!DISABLE_FULL_SIZED_BITMAPS) {
            getBitmapFile(taskId, userId).delete();
        }
    }

    /* access modifiers changed from: private */
    public abstract class WriteQueueItem {
        /* access modifiers changed from: package-private */
        public abstract void write();

        private WriteQueueItem() {
        }

        /* access modifiers changed from: package-private */
        public void onQueuedLocked() {
        }

        /* access modifiers changed from: package-private */
        public void onDequeuedLocked() {
        }
    }

    /* access modifiers changed from: private */
    public class StoreWriteQueueItem extends WriteQueueItem {
        private int mColor;
        private final ActivityManager.TaskSnapshot mSnapshot;
        /* access modifiers changed from: private */
        public final int mTaskId;
        private final int mUserId;

        StoreWriteQueueItem(int taskId, int userId, ActivityManager.TaskSnapshot snapshot) {
            super();
            this.mColor = 255;
            this.mTaskId = taskId;
            this.mUserId = userId;
            this.mSnapshot = snapshot;
        }

        StoreWriteQueueItem(TaskSnapshotPersister taskSnapshotPersister, int taskId, int userId, ActivityManager.TaskSnapshot snapshot, int color) {
            this(taskId, userId, snapshot);
            this.mColor = color;
        }

        /* access modifiers changed from: package-private */
        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        @GuardedBy({"mLock"})
        public void onQueuedLocked() {
            TaskSnapshotPersister.this.mStoreQueueItems.offer(this);
        }

        /* access modifiers changed from: package-private */
        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        @GuardedBy({"mLock"})
        public void onDequeuedLocked() {
            TaskSnapshotPersister.this.mStoreQueueItems.remove(this);
        }

        /* access modifiers changed from: package-private */
        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        public void write() {
            if (!TaskSnapshotPersister.this.createDirectory(this.mUserId)) {
                Slog.e(TaskSnapshotPersister.TAG, "Unable to create snapshot directory for user dir=" + TaskSnapshotPersister.this.getDirectory(this.mUserId));
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

        /* access modifiers changed from: package-private */
        public boolean writeProto() {
            WindowManagerProtos.TaskSnapshotProto proto = new WindowManagerProtos.TaskSnapshotProto();
            proto.orientation = this.mSnapshot.getOrientation();
            proto.insetLeft = this.mSnapshot.getContentInsets().left;
            proto.insetTop = this.mSnapshot.getContentInsets().top;
            proto.insetRight = this.mSnapshot.getContentInsets().right;
            proto.insetBottom = this.mSnapshot.getContentInsets().bottom;
            proto.isRealSnapshot = this.mSnapshot.isRealSnapshot();
            proto.windowingMode = this.mSnapshot.getWindowingMode();
            proto.systemUiVisibility = this.mSnapshot.getSystemUiVisibility();
            proto.isTranslucent = this.mSnapshot.isTranslucent();
            proto.topActivityComponent = this.mSnapshot.getTopActivityComponent().flattenToString();
            proto.scale = this.mSnapshot.getScale();
            byte[] bytes = WindowManagerProtos.TaskSnapshotProto.toByteArray(proto);
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
                Slog.e(TaskSnapshotPersister.TAG, "Unable to open " + file + " for persisting. " + e);
                return false;
            }
        }

        /* access modifiers changed from: package-private */
        public boolean writeBuffer() {
            Bitmap reduced;
            Bitmap bitmap = Bitmap.wrapHardwareBuffer(this.mSnapshot.getSnapshot(), this.mSnapshot.getColorSpace());
            if (bitmap == null) {
                Slog.e(TaskSnapshotPersister.TAG, "Invalid task snapshot hw bitmap");
                return false;
            }
            Bitmap swBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            if (swBitmap == null) {
                Slog.e(TaskSnapshotPersister.TAG, "Invalid task snapshot sw bitmap");
                return false;
            }
            if (this.mSnapshot.isReducedResolution()) {
                reduced = swBitmap;
            } else {
                reduced = Bitmap.createScaledBitmap(swBitmap, (int) (((float) bitmap.getWidth()) * TaskSnapshotPersister.this.mReducedScale), (int) (((float) bitmap.getHeight()) * TaskSnapshotPersister.this.mReducedScale), true);
            }
            Bitmap outBitmap = null;
            if (HwDisplaySizeUtil.hasSideInScreen()) {
                outBitmap = reduced.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(outBitmap);
                canvas.drawColor(this.mColor);
                canvas.drawBitmap(reduced, 0.0f, 0.0f, (Paint) null);
            }
            File reducedFile = TaskSnapshotPersister.this.getReducedResolutionBitmapFile(this.mTaskId, this.mUserId);
            try {
                FileOutputStream reducedFos = new FileOutputStream(reducedFile);
                if (reduced == null) {
                    Slog.e(TaskSnapshotPersister.TAG, "createScaledBitmap error");
                    reducedFos.close();
                    if (outBitmap != null) {
                        outBitmap.recycle();
                    }
                    return false;
                }
                if (outBitmap != null) {
                    outBitmap.compress(Bitmap.CompressFormat.JPEG, TaskSnapshotPersister.QUALITY, reducedFos);
                } else {
                    reduced.compress(Bitmap.CompressFormat.JPEG, TaskSnapshotPersister.QUALITY, reducedFos);
                }
                reducedFos.close();
                if (outBitmap != null) {
                    outBitmap.recycle();
                }
                reduced.recycle();
                if (this.mSnapshot.isReducedResolution()) {
                    swBitmap.recycle();
                    return true;
                }
                Bitmap outBitmap2 = null;
                if (HwDisplaySizeUtil.hasSideInScreen()) {
                    outBitmap2 = swBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas2 = new Canvas(outBitmap2);
                    canvas2.drawColor(this.mColor);
                    canvas2.drawBitmap(swBitmap, 0.0f, 0.0f, (Paint) null);
                }
                File file = TaskSnapshotPersister.this.getBitmapFile(this.mTaskId, this.mUserId);
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    if (outBitmap2 != null) {
                        outBitmap2.compress(Bitmap.CompressFormat.JPEG, TaskSnapshotPersister.QUALITY, fos);
                    } else {
                        swBitmap.compress(Bitmap.CompressFormat.JPEG, TaskSnapshotPersister.QUALITY, fos);
                    }
                    fos.close();
                    if (outBitmap2 != null) {
                        outBitmap2.recycle();
                    }
                    swBitmap.recycle();
                    return true;
                } catch (IOException e) {
                    Slog.e(TaskSnapshotPersister.TAG, "Unable to open " + file + " for persisting.", e);
                    if (outBitmap2 != null) {
                        outBitmap2.recycle();
                    }
                    return false;
                } catch (Throwable th) {
                    if (outBitmap2 != null) {
                        outBitmap2.recycle();
                    }
                    throw th;
                }
            } catch (IOException e2) {
                Slog.e(TaskSnapshotPersister.TAG, "Unable to open " + reducedFile + " for persisting.", e2);
                if (outBitmap != null) {
                    outBitmap.recycle();
                }
                return false;
            } catch (Throwable th2) {
                if (outBitmap != null) {
                    outBitmap.recycle();
                }
                throw th2;
            }
        }
    }

    private class DeleteWriteQueueItem extends WriteQueueItem {
        private final int mTaskId;
        private final int mUserId;

        DeleteWriteQueueItem(int taskId, int userId) {
            super();
            this.mTaskId = taskId;
            this.mUserId = userId;
        }

        /* access modifiers changed from: package-private */
        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        public void write() {
            TaskSnapshotPersister.this.deleteSnapshot(this.mTaskId, this.mUserId);
        }
    }

    @VisibleForTesting
    class RemoveObsoleteFilesQueueItem extends WriteQueueItem {
        private final ArraySet<Integer> mPersistentTaskIds;
        private final int[] mRunningUserIds;

        @VisibleForTesting
        RemoveObsoleteFilesQueueItem(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
            super();
            this.mPersistentTaskIds = new ArraySet<>(persistentTaskIds);
            this.mRunningUserIds = Arrays.copyOf(runningUserIds, runningUserIds.length);
        }

        /* access modifiers changed from: package-private */
        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        public void write() {
            ArraySet<Integer> newPersistedTaskIds;
            synchronized (TaskSnapshotPersister.this.mLock) {
                newPersistedTaskIds = new ArraySet<>((ArraySet<Integer>) TaskSnapshotPersister.this.mPersistedTaskIdsSinceLastRemoveObsolete);
            }
            for (int userId : this.mRunningUserIds) {
                File dir = TaskSnapshotPersister.this.getDirectory(userId);
                String[] files = dir.list();
                if (files != null) {
                    for (String file : files) {
                        int taskId = getTaskId(file);
                        if (!this.mPersistentTaskIds.contains(Integer.valueOf(taskId)) && !newPersistedTaskIds.contains(Integer.valueOf(taskId))) {
                            new File(dir, file).delete();
                        }
                    }
                }
            }
        }

        /* access modifiers changed from: package-private */
        @VisibleForTesting
        public int getTaskId(String fileName) {
            int end;
            if ((!fileName.endsWith(TaskSnapshotPersister.PROTO_EXTENSION) && !fileName.endsWith(TaskSnapshotPersister.BITMAP_EXTENSION)) || (end = fileName.lastIndexOf(46)) == -1) {
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
}
