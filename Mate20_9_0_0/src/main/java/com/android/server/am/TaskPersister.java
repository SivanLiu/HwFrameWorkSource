package com.android.server.am;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Process;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class TaskPersister {
    static final boolean DEBUG = false;
    private static final long FLUSH_QUEUE = -1;
    private static final String IMAGES_DIRNAME = "recent_images";
    static final String IMAGE_EXTENSION = ".png";
    private static final long INTER_WRITE_DELAY_MS = 500;
    private static final int MAX_WRITE_QUEUE_LENGTH = 6;
    private static final String PERSISTED_TASK_IDS_FILENAME = "persisted_taskIds.txt";
    private static final long PRE_TASK_DELAY_MS = 3000;
    static final String TAG = "TaskPersister";
    private static final String TAG_TASK = "task";
    private static final String TASKS_DIRNAME = "recent_tasks";
    private static final String TASK_FILENAME_SUFFIX = "_task.xml";
    private final Object mIoLock = new Object();
    private final LazyTaskWriterThread mLazyTaskWriterThread;
    private long mNextWriteTime = 0;
    private final RecentTasks mRecentTasks;
    private final ActivityManagerService mService;
    private final ActivityStackSupervisor mStackSupervisor;
    private final File mTaskIdsDir;
    private final SparseArray<SparseBooleanArray> mTaskIdsInFile = new SparseArray();
    ArrayList<WriteQueueItem> mWriteQueue = new ArrayList();

    private class LazyTaskWriterThread extends Thread {
        LazyTaskWriterThread(String name) {
            super(name);
        }

        public void run() {
            Process.setThreadPriority(10);
            ArraySet<Integer> persistentTaskIds = new ArraySet();
            while (true) {
                boolean probablyDone;
                synchronized (TaskPersister.this) {
                    probablyDone = TaskPersister.this.mWriteQueue.isEmpty();
                }
                if (probablyDone) {
                    persistentTaskIds.clear();
                    synchronized (TaskPersister.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            TaskPersister.this.mRecentTasks.getPersistableTaskIds(persistentTaskIds);
                            TaskPersister.this.mService.mWindowManager.removeObsoleteTaskFiles(persistentTaskIds, TaskPersister.this.mRecentTasks.usersWithRecentsLoadedLocked());
                        } finally {
                            while (true) {
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    TaskPersister.this.removeObsoleteFiles(persistentTaskIds);
                }
                TaskPersister.this.writeTaskIdsFiles();
                processNextItem();
            }
        }

        private void processNextItem() {
            WriteQueueItem item;
            String str;
            StringBuilder stringBuilder;
            synchronized (TaskPersister.this) {
                if (TaskPersister.this.mNextWriteTime != -1) {
                    TaskPersister.this.mNextWriteTime = SystemClock.uptimeMillis() + 500;
                }
                while (TaskPersister.this.mWriteQueue.isEmpty()) {
                    if (TaskPersister.this.mNextWriteTime != 0) {
                        TaskPersister.this.mNextWriteTime = 0;
                        TaskPersister.this.notifyAll();
                    }
                    try {
                        TaskPersister.this.wait();
                    } catch (InterruptedException e) {
                    }
                }
                item = (WriteQueueItem) TaskPersister.this.mWriteQueue.remove(0);
                for (long now = SystemClock.uptimeMillis(); now < TaskPersister.this.mNextWriteTime; now = SystemClock.uptimeMillis()) {
                    try {
                        TaskPersister.this.wait(TaskPersister.this.mNextWriteTime - now);
                    } catch (InterruptedException e2) {
                    }
                }
            }
            WriteQueueItem item2 = item;
            AtomicFile atomicFile = null;
            if (item2 instanceof ImageWriteQueueItem) {
                ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) item2;
                String filePath = imageWriteQueueItem.mFilePath;
                if (TaskPersister.createParentDirectory(filePath)) {
                    Bitmap bitmap = imageWriteQueueItem.mImage;
                    try {
                        atomicFile = new FileOutputStream(new File(filePath));
                        if (!(bitmap == null || bitmap.isRecycled())) {
                            bitmap.compress(CompressFormat.PNG, 100, atomicFile);
                        }
                    } catch (Exception e3) {
                        str = TaskPersister.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("saveImage: unable to save ");
                        stringBuilder.append(filePath);
                        Slog.e(str, stringBuilder.toString(), e3);
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(null);
                    }
                    IoUtils.closeQuietly(atomicFile);
                } else {
                    atomicFile = TaskPersister.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error while creating images directory for file: ");
                    stringBuilder2.append(filePath);
                    Slog.e(atomicFile, stringBuilder2.toString());
                }
            } else if (item2 instanceof TaskWriteQueueItem) {
                StringWriter stringWriter = null;
                TaskRecord task = ((TaskWriteQueueItem) item2).mTask;
                synchronized (TaskPersister.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (task.inRecents) {
                            try {
                                stringWriter = TaskPersister.this.saveToXml(task);
                            } catch (IOException e4) {
                            } catch (XmlPullParserException e5) {
                            }
                        }
                    } finally {
                        while (true) {
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                }
                if (stringWriter != null) {
                    FileOutputStream file = null;
                    try {
                        File userTasksDir = TaskPersister.getUserTasksDir(task.userId);
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(String.valueOf(task.taskId));
                        stringBuilder3.append(TaskPersister.TASK_FILENAME_SUFFIX);
                        atomicFile = new AtomicFile(new File(userTasksDir, stringBuilder3.toString()));
                        file = atomicFile.startWrite();
                        file.write(stringWriter.toString().getBytes());
                        file.write(10);
                        atomicFile.finishWrite(file);
                    } catch (IOException e6) {
                        if (file != null) {
                            atomicFile.failWrite(file);
                        }
                        str = TaskPersister.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to open ");
                        stringBuilder.append(atomicFile);
                        stringBuilder.append(" for persisting. ");
                        stringBuilder.append(e6);
                        Slog.e(str, stringBuilder.toString());
                    }
                }
            }
        }
    }

    private static class WriteQueueItem {
        private WriteQueueItem() {
        }

        /* synthetic */ WriteQueueItem(AnonymousClass1 x0) {
            this();
        }
    }

    private static class ImageWriteQueueItem extends WriteQueueItem {
        final String mFilePath;
        Bitmap mImage;

        ImageWriteQueueItem(String filePath, Bitmap image) {
            super();
            this.mFilePath = filePath;
            this.mImage = image;
        }
    }

    private static class TaskWriteQueueItem extends WriteQueueItem {
        final TaskRecord mTask;

        TaskWriteQueueItem(TaskRecord task) {
            super();
            this.mTask = task;
        }
    }

    TaskPersister(File systemDir, ActivityStackSupervisor stackSupervisor, ActivityManagerService service, RecentTasks recentTasks) {
        File legacyImagesDir = new File(systemDir, IMAGES_DIRNAME);
        if (legacyImagesDir.exists() && !(FileUtils.deleteContents(legacyImagesDir) && legacyImagesDir.delete())) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failure deleting legacy images directory: ");
            stringBuilder.append(legacyImagesDir);
            Slog.i(str, stringBuilder.toString());
        }
        File legacyTasksDir = new File(systemDir, TASKS_DIRNAME);
        if (legacyTasksDir.exists() && !(FileUtils.deleteContents(legacyTasksDir) && legacyTasksDir.delete())) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failure deleting legacy tasks directory: ");
            stringBuilder2.append(legacyTasksDir);
            Slog.i(str2, stringBuilder2.toString());
        }
        this.mTaskIdsDir = new File(Environment.getDataDirectory(), "system_de");
        this.mStackSupervisor = stackSupervisor;
        this.mService = service;
        this.mRecentTasks = recentTasks;
        this.mLazyTaskWriterThread = new LazyTaskWriterThread("LazyTaskWriterThread");
    }

    @VisibleForTesting
    TaskPersister(File workingDir) {
        this.mTaskIdsDir = workingDir;
        this.mStackSupervisor = null;
        this.mService = null;
        this.mRecentTasks = null;
        this.mLazyTaskWriterThread = new LazyTaskWriterThread("LazyTaskWriterThreadTest");
    }

    void startPersisting() {
        if (!this.mLazyTaskWriterThread.isAlive()) {
            this.mLazyTaskWriterThread.start();
        }
    }

    private void removeThumbnails(TaskRecord task) {
        String taskString = Integer.toString(task.taskId);
        for (int queueNdx = this.mWriteQueue.size() - 1; queueNdx >= 0; queueNdx--) {
            WriteQueueItem item = (WriteQueueItem) this.mWriteQueue.get(queueNdx);
            if ((item instanceof ImageWriteQueueItem) && new File(((ImageWriteQueueItem) item).mFilePath).getName().startsWith(taskString)) {
                this.mWriteQueue.remove(queueNdx);
            }
        }
    }

    private void yieldIfQueueTooDeep() {
        boolean stall = false;
        synchronized (this) {
            if (this.mNextWriteTime == -1) {
                stall = true;
            }
        }
        if (stall) {
            Thread.yield();
        }
    }

    SparseBooleanArray loadPersistedTaskIdsForUser(int userId) {
        String line;
        if (this.mTaskIdsInFile.get(userId) != null) {
            return ((SparseBooleanArray) this.mTaskIdsInFile.get(userId)).clone();
        }
        SparseBooleanArray persistedTaskIds = new SparseBooleanArray();
        synchronized (this.mIoLock) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(getUserPersistedTaskIdsFile(userId)));
                while (true) {
                    String readLine = reader.readLine();
                    line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    for (String taskIdString : line.split("\\s+")) {
                        persistedTaskIds.put(Integer.parseInt(taskIdString), true);
                    }
                }
                IoUtils.closeQuietly(reader);
            } catch (FileNotFoundException e) {
                IoUtils.closeQuietly(null);
            } catch (Exception e2) {
                try {
                    line = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error while reading taskIds file for user ");
                    stringBuilder.append(userId);
                    Slog.e(line, stringBuilder.toString(), e2);
                    IoUtils.closeQuietly(null);
                } catch (Throwable th) {
                    IoUtils.closeQuietly(null);
                }
            }
        }
        this.mTaskIdsInFile.put(userId, persistedTaskIds);
        return persistedTaskIds.clone();
    }

    @VisibleForTesting
    void writePersistedTaskIdsForUser(SparseBooleanArray taskIds, int userId) {
        if (userId >= 0 && taskIds != null) {
            File persistedTaskIdsFile = getUserPersistedTaskIdsFile(userId);
            synchronized (this.mIoLock) {
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(persistedTaskIdsFile));
                    for (int i = 0; i < taskIds.size(); i++) {
                        if (taskIds.valueAt(i)) {
                            writer.write(String.valueOf(taskIds.keyAt(i)));
                            writer.newLine();
                        }
                    }
                    IoUtils.closeQuietly(writer);
                } catch (Exception e) {
                    try {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error while writing taskIds file for user ");
                        stringBuilder.append(userId);
                        Slog.e(str, stringBuilder.toString(), e);
                        IoUtils.closeQuietly(null);
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(null);
                    }
                }
            }
        }
    }

    void unloadUserDataFromMemory(int userId) {
        this.mTaskIdsInFile.delete(userId);
    }

    void wakeup(TaskRecord task, boolean flush) {
        synchronized (this) {
            if (task != null) {
                int queueNdx = this.mWriteQueue.size() - 1;
                while (queueNdx >= 0) {
                    WriteQueueItem item = (WriteQueueItem) this.mWriteQueue.get(queueNdx);
                    if ((item instanceof TaskWriteQueueItem) && ((TaskWriteQueueItem) item).mTask == task) {
                        if (!task.inRecents) {
                            removeThumbnails(task);
                        }
                        if (queueNdx < 0 && task.isPersistable) {
                            this.mWriteQueue.add(new TaskWriteQueueItem(task));
                        }
                    } else {
                        queueNdx--;
                    }
                }
                this.mWriteQueue.add(new TaskWriteQueueItem(task));
            } else {
                this.mWriteQueue.add(new WriteQueueItem());
            }
            if (flush || this.mWriteQueue.size() > 6) {
                this.mNextWriteTime = -1;
            } else if (this.mNextWriteTime == 0) {
                this.mNextWriteTime = SystemClock.uptimeMillis() + PRE_TASK_DELAY_MS;
            }
            notifyAll();
        }
        yieldIfQueueTooDeep();
    }

    void flush() {
        synchronized (this) {
            this.mNextWriteTime = -1;
            notifyAll();
            do {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            } while (this.mNextWriteTime == -1);
        }
    }

    void saveImage(Bitmap image, String filePath) {
        synchronized (this) {
            int queueNdx = this.mWriteQueue.size() - 1;
            while (queueNdx >= 0) {
                WriteQueueItem item = (WriteQueueItem) this.mWriteQueue.get(queueNdx);
                if (item instanceof ImageWriteQueueItem) {
                    ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) item;
                    if (imageWriteQueueItem.mFilePath.equals(filePath)) {
                        imageWriteQueueItem.mImage = image;
                        break;
                    }
                }
                queueNdx--;
            }
            if (queueNdx < 0) {
                this.mWriteQueue.add(new ImageWriteQueueItem(filePath, image));
            }
            if (this.mWriteQueue.size() > 6) {
                this.mNextWriteTime = -1;
            } else if (this.mNextWriteTime == 0) {
                this.mNextWriteTime = SystemClock.uptimeMillis() + PRE_TASK_DELAY_MS;
            }
            notifyAll();
        }
        yieldIfQueueTooDeep();
    }

    Bitmap getTaskDescriptionIcon(String filePath) {
        Bitmap icon = getImageFromWriteQueue(filePath);
        if (icon != null) {
            return icon;
        }
        return restoreImage(filePath);
    }

    Bitmap getImageFromWriteQueue(String filePath) {
        synchronized (this) {
            for (int queueNdx = this.mWriteQueue.size() - 1; queueNdx >= 0; queueNdx--) {
                WriteQueueItem item = (WriteQueueItem) this.mWriteQueue.get(queueNdx);
                if (item instanceof ImageWriteQueueItem) {
                    ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) item;
                    if (imageWriteQueueItem.mFilePath.equals(filePath)) {
                        Bitmap bitmap = imageWriteQueueItem.mImage;
                        return bitmap;
                    }
                }
            }
            return null;
        }
    }

    private StringWriter saveToXml(TaskRecord task) throws IOException, XmlPullParserException {
        XmlSerializer xmlSerializer = new FastXmlSerializer();
        StringWriter stringWriter = new StringWriter();
        xmlSerializer.setOutput(stringWriter);
        xmlSerializer.startDocument(null, Boolean.valueOf(true));
        xmlSerializer.startTag(null, TAG_TASK);
        task.saveToXml(xmlSerializer);
        xmlSerializer.endTag(null, TAG_TASK);
        xmlSerializer.endDocument();
        xmlSerializer.flush();
        return stringWriter;
    }

    private String fileToString(File file) {
        String newline = System.lineSeparator();
        StringBuilder stringBuilder;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuffer sb = new StringBuffer(((int) file.length()) * 2);
            while (true) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(line);
                    stringBuilder.append(newline);
                    sb.append(stringBuilder.toString());
                } else {
                    reader.close();
                    return sb.toString();
                }
            }
        } catch (IOException e) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't read file ");
            stringBuilder.append(file.getName());
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    private TaskRecord taskIdToTask(int taskId, ArrayList<TaskRecord> tasks) {
        if (taskId < 0) {
            return null;
        }
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = (TaskRecord) tasks.get(taskNdx);
            if (task.taskId == taskId) {
                return task;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Restore affiliation error looking for taskId=");
        stringBuilder.append(taskId);
        Slog.e(str, stringBuilder.toString());
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:84:0x0209 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x01f8  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0209 A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    List<TaskRecord> restoreTasksForUserLocked(int userId, SparseBooleanArray preaddedTasks) {
        NumberFormatException e;
        File[] recentFiles;
        int taskNdx;
        Exception e2;
        Throwable th;
        int i = userId;
        ArrayList<TaskRecord> tasks = new ArrayList();
        ArraySet<Integer> recoveredTaskIds = new ArraySet();
        File userTasksDir = getUserTasksDir(userId);
        File[] recentFiles2 = userTasksDir.listFiles();
        StringBuilder stringBuilder;
        if (recentFiles2 == null) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("restoreTasksForUserLocked: Unable to list files from ");
            stringBuilder.append(userTasksDir);
            Slog.e(str, stringBuilder.toString());
            return tasks;
        }
        boolean z = false;
        int taskNdx2 = 0;
        while (true) {
            int taskNdx3 = taskNdx2;
            int i2 = 1;
            if (taskNdx3 < recentFiles2.length) {
                File taskFile = recentFiles2[taskNdx3];
                if (taskFile.getName().endsWith(TASK_FILENAME_SUFFIX)) {
                    try {
                        taskNdx2 = Integer.parseInt(taskFile.getName().substring(z, taskFile.getName().length() - TASK_FILENAME_SUFFIX.length()));
                        if (preaddedTasks.get(taskNdx2, z)) {
                            try {
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Task #");
                                stringBuilder2.append(taskNdx2);
                                stringBuilder2.append(" has already been created so we don't restore again");
                                Slog.w(str2, stringBuilder2.toString());
                            } catch (NumberFormatException e3) {
                                e = e3;
                                recentFiles = recentFiles2;
                                taskNdx = taskNdx3;
                            }
                        } else {
                            BufferedReader reader = null;
                            boolean deleteFile = z;
                            String str3;
                            try {
                                reader = new BufferedReader(new FileReader(taskFile));
                                XmlPullParser in = Xml.newPullParser();
                                in.setInput(reader);
                                while (true) {
                                    int next = in.next();
                                    int event = next;
                                    if (next == i2 || event == 3) {
                                        recentFiles = recentFiles2;
                                        taskNdx = taskNdx3;
                                        IoUtils.closeQuietly(reader);
                                    } else {
                                        String name = in.getName();
                                        if (event != 2) {
                                            recentFiles = recentFiles2;
                                            taskNdx = taskNdx3;
                                        } else if (TAG_TASK.equals(name)) {
                                            TaskRecord task = TaskRecord.restoreFromXml(in, this.mStackSupervisor);
                                            StringBuilder stringBuilder3;
                                            if (task != null) {
                                                i2 = task.taskId;
                                                recentFiles = recentFiles2;
                                                try {
                                                    if (this.mStackSupervisor.anyTaskForIdLocked(i2, 1) != null) {
                                                        str3 = TAG;
                                                        StringBuilder stringBuilder4 = new StringBuilder();
                                                        taskNdx = taskNdx3;
                                                        try {
                                                            stringBuilder4.append("Existing task with taskId ");
                                                            stringBuilder4.append(i2);
                                                            stringBuilder4.append("found");
                                                            Slog.wtf(str3, stringBuilder4.toString());
                                                        } catch (Exception e4) {
                                                            e2 = e4;
                                                            try {
                                                                str3 = TAG;
                                                                stringBuilder = new StringBuilder();
                                                                stringBuilder.append("Unable to parse ");
                                                                stringBuilder.append(taskFile);
                                                                stringBuilder.append(". Error ");
                                                                Slog.wtf(str3, stringBuilder.toString(), e2);
                                                                str3 = TAG;
                                                                stringBuilder = new StringBuilder();
                                                                stringBuilder.append("Failing file: ");
                                                                stringBuilder.append(fileToString(taskFile));
                                                                Slog.e(str3, stringBuilder.toString());
                                                                IoUtils.closeQuietly(reader);
                                                                if (!true) {
                                                                }
                                                                taskFile.delete();
                                                                taskNdx2 = taskNdx + 1;
                                                                recentFiles2 = recentFiles;
                                                                z = false;
                                                            } catch (Throwable th2) {
                                                                th = th2;
                                                            }
                                                        }
                                                    } else {
                                                        taskNdx = taskNdx3;
                                                        if (i != task.userId) {
                                                            str3 = TAG;
                                                            stringBuilder3 = new StringBuilder();
                                                            stringBuilder3.append("Task with userId ");
                                                            stringBuilder3.append(task.userId);
                                                            stringBuilder3.append(" found in ");
                                                            stringBuilder3.append(userTasksDir.getAbsolutePath());
                                                            Slog.wtf(str3, stringBuilder3.toString());
                                                        } else {
                                                            this.mStackSupervisor.setNextTaskIdForUserLocked(i2, i);
                                                            task.isPersistable = true;
                                                            tasks.add(task);
                                                            recoveredTaskIds.add(Integer.valueOf(i2));
                                                        }
                                                    }
                                                } catch (Exception e5) {
                                                    e2 = e5;
                                                    taskNdx = taskNdx3;
                                                    str3 = TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Unable to parse ");
                                                    stringBuilder.append(taskFile);
                                                    stringBuilder.append(". Error ");
                                                    Slog.wtf(str3, stringBuilder.toString(), e2);
                                                    str3 = TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Failing file: ");
                                                    stringBuilder.append(fileToString(taskFile));
                                                    Slog.e(str3, stringBuilder.toString());
                                                    IoUtils.closeQuietly(reader);
                                                    if (true) {
                                                        taskNdx2 = taskNdx + 1;
                                                        recentFiles2 = recentFiles;
                                                        z = false;
                                                    }
                                                    taskFile.delete();
                                                    taskNdx2 = taskNdx + 1;
                                                    recentFiles2 = recentFiles;
                                                    z = false;
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    taskNdx = taskNdx3;
                                                    IoUtils.closeQuietly(reader);
                                                    if (deleteFile) {
                                                        taskFile.delete();
                                                    }
                                                    throw th;
                                                }
                                            }
                                            recentFiles = recentFiles2;
                                            taskNdx = taskNdx3;
                                            str3 = TAG;
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("restoreTasksForUserLocked: Unable to restore taskFile=");
                                            stringBuilder3.append(taskFile);
                                            stringBuilder3.append(": ");
                                            stringBuilder3.append(fileToString(taskFile));
                                            Slog.e(str3, stringBuilder3.toString());
                                        } else {
                                            recentFiles = recentFiles2;
                                            taskNdx = taskNdx3;
                                            str3 = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("restoreTasksForUserLocked: Unknown xml event=");
                                            stringBuilder.append(event);
                                            stringBuilder.append(" name=");
                                            stringBuilder.append(name);
                                            Slog.wtf(str3, stringBuilder.toString());
                                        }
                                        XmlUtils.skipCurrentTag(in);
                                        recentFiles2 = recentFiles;
                                        taskNdx3 = taskNdx;
                                        i2 = 1;
                                        SparseBooleanArray sparseBooleanArray = preaddedTasks;
                                    }
                                }
                                recentFiles = recentFiles2;
                                taskNdx = taskNdx3;
                                IoUtils.closeQuietly(reader);
                                if (!deleteFile) {
                                    taskNdx2 = taskNdx + 1;
                                    recentFiles2 = recentFiles;
                                    z = false;
                                }
                            } catch (Exception e6) {
                                e2 = e6;
                                recentFiles = recentFiles2;
                                taskNdx = taskNdx3;
                                str3 = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unable to parse ");
                                stringBuilder.append(taskFile);
                                stringBuilder.append(". Error ");
                                Slog.wtf(str3, stringBuilder.toString(), e2);
                                str3 = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Failing file: ");
                                stringBuilder.append(fileToString(taskFile));
                                Slog.e(str3, stringBuilder.toString());
                                IoUtils.closeQuietly(reader);
                                if (true) {
                                }
                                taskFile.delete();
                                taskNdx2 = taskNdx + 1;
                                recentFiles2 = recentFiles;
                                z = false;
                            } catch (Throwable th4) {
                                th = th4;
                                recentFiles = recentFiles2;
                                taskNdx = taskNdx3;
                                IoUtils.closeQuietly(reader);
                                if (deleteFile) {
                                }
                                throw th;
                            }
                            taskFile.delete();
                            taskNdx2 = taskNdx + 1;
                            recentFiles2 = recentFiles;
                            z = false;
                        }
                    } catch (NumberFormatException e7) {
                        e = e7;
                        recentFiles = recentFiles2;
                        taskNdx = taskNdx3;
                        Slog.w(TAG, "Unexpected task file name", e);
                        taskNdx2 = taskNdx + 1;
                        recentFiles2 = recentFiles;
                        z = false;
                    }
                }
                recentFiles = recentFiles2;
                taskNdx = taskNdx3;
                taskNdx2 = taskNdx + 1;
                recentFiles2 = recentFiles;
                z = false;
            } else {
                removeObsoleteFiles(recoveredTaskIds, userTasksDir.listFiles());
                for (taskNdx2 = tasks.size() - 1; taskNdx2 >= 0; taskNdx2--) {
                    TaskRecord task2 = (TaskRecord) tasks.get(taskNdx2);
                    task2.setPrevAffiliate(taskIdToTask(task2.mPrevAffiliateTaskId, tasks));
                    task2.setNextAffiliate(taskIdToTask(task2.mNextAffiliateTaskId, tasks));
                }
                Collections.sort(tasks, new Comparator<TaskRecord>() {
                    public int compare(TaskRecord lhs, TaskRecord rhs) {
                        long diff = rhs.mLastTimeMoved - lhs.mLastTimeMoved;
                        if (diff < 0) {
                            return -1;
                        }
                        if (diff > 0) {
                            return 1;
                        }
                        return 0;
                    }
                });
                return tasks;
            }
        }
    }

    private static void removeObsoleteFiles(ArraySet<Integer> persistentTaskIds, File[] files) {
        if (files == null) {
            Slog.e(TAG, "File error accessing recents directory (directory doesn't exist?).");
            return;
        }
        for (File file : files) {
            String filename = file.getName();
            int taskIdEnd = filename.indexOf(95);
            if (taskIdEnd > 0) {
                try {
                    if (!persistentTaskIds.contains(Integer.valueOf(Integer.parseInt(filename.substring(0, taskIdEnd))))) {
                        file.delete();
                    }
                } catch (Exception e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("removeObsoleteFiles: Can't parse file=");
                    stringBuilder.append(file.getName());
                    Slog.wtf(str, stringBuilder.toString());
                    file.delete();
                }
            }
        }
    }

    private void writeTaskIdsFiles() {
        int i;
        SparseArray<SparseBooleanArray> changedTaskIdsPerUser = new SparseArray();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                i = 0;
                for (int userId : this.mRecentTasks.usersWithRecentsLoadedLocked()) {
                    SparseBooleanArray taskIdsToSave = this.mRecentTasks.getTaskIdsForUser(userId);
                    SparseBooleanArray persistedIdsInFile = (SparseBooleanArray) this.mTaskIdsInFile.get(userId);
                    if (persistedIdsInFile == null || !persistedIdsInFile.equals(taskIdsToSave)) {
                        SparseBooleanArray taskIdsToSaveCopy = taskIdsToSave.clone();
                        this.mTaskIdsInFile.put(userId, taskIdsToSaveCopy);
                        changedTaskIdsPerUser.put(userId, taskIdsToSaveCopy);
                    }
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        while (true) {
            int i2 = i;
            if (i2 < changedTaskIdsPerUser.size()) {
                writePersistedTaskIdsForUser((SparseBooleanArray) changedTaskIdsPerUser.valueAt(i2), changedTaskIdsPerUser.keyAt(i2));
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    private void removeObsoleteFiles(ArraySet<Integer> persistentTaskIds) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                int[] candidateUserIds = this.mRecentTasks.usersWithRecentsLoadedLocked();
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        for (int userId : candidateUserIds) {
            removeObsoleteFiles(persistentTaskIds, getUserImagesDir(userId).listFiles());
            removeObsoleteFiles(persistentTaskIds, getUserTasksDir(userId).listFiles());
        }
    }

    static Bitmap restoreImage(String filename) {
        return BitmapFactory.decodeFile(filename);
    }

    private File getUserPersistedTaskIdsFile(int userId) {
        File userTaskIdsDir = new File(this.mTaskIdsDir, String.valueOf(userId));
        if (!(userTaskIdsDir.exists() || userTaskIdsDir.mkdirs())) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error while creating user directory: ");
            stringBuilder.append(userTaskIdsDir);
            Slog.e(str, stringBuilder.toString());
        }
        return new File(userTaskIdsDir, PERSISTED_TASK_IDS_FILENAME);
    }

    static File getUserTasksDir(int userId) {
        File userTasksDir = new File(Environment.getDataSystemCeDirectory(userId), TASKS_DIRNAME);
        if (!(userTasksDir.exists() || userTasksDir.mkdir())) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failure creating tasks directory for user ");
            stringBuilder.append(userId);
            stringBuilder.append(": ");
            stringBuilder.append(userTasksDir);
            Slog.e(str, stringBuilder.toString());
        }
        return userTasksDir;
    }

    static File getUserImagesDir(int userId) {
        return new File(Environment.getDataSystemCeDirectory(userId), IMAGES_DIRNAME);
    }

    private static boolean createParentDirectory(String filePath) {
        File parentDir = new File(filePath).getParentFile();
        return parentDir.exists() || parentDir.mkdirs();
    }
}
