package com.android.server;

import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.MemoryFile;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.util.Log;
import android.view.IGraphicsStats.Stub;
import android.view.IGraphicsStatsCallback;
import com.android.internal.util.DumpUtils;
import com.android.server.job.controllers.JobStatus;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.TimeZone;

public class GraphicsStatsService extends Stub {
    private static final int DELETE_OLD = 2;
    public static final String GRAPHICS_STATS_SERVICE = "graphicsstats";
    private static final int SAVE_BUFFER = 1;
    private static final String TAG = "GraphicsStatsService";
    private final int ASHMEM_SIZE = nGetAshmemSize();
    private final byte[] ZERO_DATA = new byte[this.ASHMEM_SIZE];
    private ArrayList<ActiveBuffer> mActive = new ArrayList();
    private final AlarmManager mAlarmManager;
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final Object mFileAccessLock = new Object();
    private File mGraphicsStatsDir;
    private final Object mLock = new Object();
    private boolean mRotateIsScheduled = false;
    private Handler mWriteOutHandler;

    private final class ActiveBuffer implements DeathRecipient {
        final IGraphicsStatsCallback mCallback;
        final BufferInfo mInfo;
        final int mPid;
        MemoryFile mProcessBuffer;
        final IBinder mToken = this.mCallback.asBinder();
        final int mUid;

        ActiveBuffer(IGraphicsStatsCallback token, int uid, int pid, String packageName, long versionCode) throws RemoteException, IOException {
            this.mInfo = new BufferInfo(packageName, versionCode, System.currentTimeMillis());
            this.mUid = uid;
            this.mPid = pid;
            this.mCallback = token;
            this.mToken.linkToDeath(this, 0);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GFXStats-");
            stringBuilder.append(pid);
            this.mProcessBuffer = new MemoryFile(stringBuilder.toString(), GraphicsStatsService.this.ASHMEM_SIZE);
            this.mProcessBuffer.writeBytes(GraphicsStatsService.this.ZERO_DATA, 0, 0, GraphicsStatsService.this.ASHMEM_SIZE);
        }

        public void binderDied() {
            this.mToken.unlinkToDeath(this, 0);
            GraphicsStatsService.this.processDied(this);
        }

        void closeAllBuffers() {
            if (this.mProcessBuffer != null) {
                this.mProcessBuffer.close();
                this.mProcessBuffer = null;
            }
        }
    }

    private final class BufferInfo {
        long endTime;
        final String packageName;
        long startTime;
        final long versionCode;

        BufferInfo(String packageName, long versionCode, long startTime) {
            this.packageName = packageName;
            this.versionCode = versionCode;
            this.startTime = startTime;
        }
    }

    private final class HistoricalBuffer {
        final byte[] mData = new byte[GraphicsStatsService.this.ASHMEM_SIZE];
        final BufferInfo mInfo;

        HistoricalBuffer(ActiveBuffer active) throws IOException {
            this.mInfo = active.mInfo;
            this.mInfo.endTime = System.currentTimeMillis();
            active.mProcessBuffer.readBytes(this.mData, 0, 0, GraphicsStatsService.this.ASHMEM_SIZE);
        }
    }

    private static native void nAddToDump(long j, String str);

    private static native void nAddToDump(long j, String str, String str2, long j2, long j3, long j4, byte[] bArr);

    private static native long nCreateDump(int i, boolean z);

    private static native void nFinishDump(long j);

    private static native int nGetAshmemSize();

    private static native void nSaveBuffer(String str, String str2, long j, long j2, long j3, byte[] bArr);

    public GraphicsStatsService(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mGraphicsStatsDir = new File(new File(Environment.getDataDirectory(), "system"), GRAPHICS_STATS_SERVICE);
        this.mGraphicsStatsDir.mkdirs();
        if (this.mGraphicsStatsDir.exists()) {
            HandlerThread bgthread = new HandlerThread("GraphicsStats-disk", 10);
            bgthread.start();
            this.mWriteOutHandler = new Handler(bgthread.getLooper(), new Callback() {
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case 1:
                            GraphicsStatsService.this.saveBuffer((HistoricalBuffer) msg.obj);
                            break;
                        case 2:
                            GraphicsStatsService.this.deleteOldBuffers();
                            break;
                    }
                    return true;
                }
            });
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Graphics stats directory does not exist: ");
        stringBuilder.append(this.mGraphicsStatsDir.getAbsolutePath());
        throw new IllegalStateException(stringBuilder.toString());
    }

    private void scheduleRotateLocked() {
        if (!this.mRotateIsScheduled) {
            this.mRotateIsScheduled = true;
            Calendar calendar = normalizeDate(System.currentTimeMillis());
            calendar.add(5, 1);
            this.mAlarmManager.setExact(1, calendar.getTimeInMillis(), TAG, new -$$Lambda$GraphicsStatsService$2EDVu98hsJvSwNgKvijVLSR3IrQ(this), this.mWriteOutHandler);
        }
    }

    private void onAlarm() {
        synchronized (this.mLock) {
            this.mRotateIsScheduled = false;
            scheduleRotateLocked();
        }
        for (ActiveBuffer active : (ActiveBuffer[]) this.mActive.toArray(new ActiveBuffer[0])) {
            try {
                active.mCallback.onRotateGraphicsStatsBuffer();
            } catch (RemoteException e) {
                Log.w(TAG, String.format("Failed to notify '%s' (pid=%d) to rotate buffers", new Object[]{active.mInfo.packageName, Integer.valueOf(active.mPid)}), e);
            }
        }
        this.mWriteOutHandler.sendEmptyMessageDelayed(2, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    public ParcelFileDescriptor requestBufferForProcess(String packageName, IGraphicsStatsCallback token) throws RemoteException {
        Throwable th;
        String str = packageName;
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAppOps.checkPackage(uid, str);
            PackageInfo info = this.mContext.getPackageManager().getPackageInfoAsUser(str, 0, UserHandle.getUserId(uid));
            Object obj = this.mLock;
            synchronized (obj) {
                Object obj2;
                try {
                    obj2 = obj;
                    try {
                        ParcelFileDescriptor pfd = requestBufferForProcessLocked(token, uid, pid, str, info.getLongVersionCode());
                        try {
                            Binder.restoreCallingIdentity(callingIdentity);
                            return pfd;
                        } catch (Throwable th2) {
                            th = th2;
                            ParcelFileDescriptor parcelFileDescriptor = pfd;
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    obj2 = obj;
                    throw th;
                }
            }
        } catch (NameNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to find package: '");
            stringBuilder.append(str);
            stringBuilder.append("'");
            throw new RemoteException(stringBuilder.toString());
        } catch (Throwable th5) {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private ParcelFileDescriptor getPfd(MemoryFile file) {
        try {
            if (file.getFileDescriptor().valid()) {
                return new ParcelFileDescriptor(file.getFileDescriptor());
            }
            throw new IllegalStateException("Invalid file descriptor");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to get PFD from memory file", ex);
        }
    }

    private ParcelFileDescriptor requestBufferForProcessLocked(IGraphicsStatsCallback token, int uid, int pid, String packageName, long versionCode) throws RemoteException {
        ActiveBuffer buffer = fetchActiveBuffersLocked(token, uid, pid, packageName, versionCode);
        scheduleRotateLocked();
        return getPfd(buffer.mProcessBuffer);
    }

    private Calendar normalizeDate(long timestamp) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(timestamp);
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar;
    }

    private File pathForApp(BufferInfo info) {
        return new File(this.mGraphicsStatsDir, String.format("%d/%s/%d/total", new Object[]{Long.valueOf(normalizeDate(info.startTime).getTimeInMillis()), info.packageName, Long.valueOf(info.versionCode)}));
    }

    private void saveBuffer(HistoricalBuffer buffer) {
        if (Trace.isTagEnabled(524288)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("saving graphicsstats for ");
            stringBuilder.append(buffer.mInfo.packageName);
            Trace.traceBegin(524288, stringBuilder.toString());
        }
        synchronized (this.mFileAccessLock) {
            File path = pathForApp(buffer.mInfo);
            File parent = path.getParentFile();
            parent.mkdirs();
            if (parent.exists()) {
                nSaveBuffer(path.getAbsolutePath(), buffer.mInfo.packageName, buffer.mInfo.versionCode, buffer.mInfo.startTime, buffer.mInfo.endTime, buffer.mData);
                Trace.traceEnd(524288);
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to create path: '");
            stringBuilder2.append(parent.getAbsolutePath());
            stringBuilder2.append("'");
            Log.w(str, stringBuilder2.toString());
        }
    }

    private void deleteRecursiveLocked(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursiveLocked(child);
            }
        }
        if (!file.delete()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to delete '");
            stringBuilder.append(file.getAbsolutePath());
            stringBuilder.append("'!");
            Log.w(str, stringBuilder.toString());
        }
    }

    private void deleteOldBuffers() {
        Trace.traceBegin(524288, "deleting old graphicsstats buffers");
        synchronized (this.mFileAccessLock) {
            File[] files = this.mGraphicsStatsDir.listFiles();
            if (files == null || files.length <= 3) {
                return;
            }
            long[] sortedDates = new long[files.length];
            int i = 0;
            for (int i2 = 0; i2 < files.length; i2++) {
                try {
                    sortedDates[i2] = Long.parseLong(files[i2].getName());
                } catch (NumberFormatException e) {
                }
            }
            if (sortedDates.length <= 3) {
                return;
            }
            Arrays.sort(sortedDates);
            while (i < sortedDates.length - 3) {
                deleteRecursiveLocked(new File(this.mGraphicsStatsDir, Long.toString(sortedDates[i])));
                i++;
            }
            Trace.traceEnd(524288);
        }
    }

    private void addToSaveQueue(ActiveBuffer buffer) {
        try {
            Message.obtain(this.mWriteOutHandler, 1, new HistoricalBuffer(buffer)).sendToTarget();
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to copy graphicsstats from ");
            stringBuilder.append(buffer.mInfo.packageName);
            Log.w(str, stringBuilder.toString(), e);
        }
        buffer.closeAllBuffers();
    }

    private void processDied(ActiveBuffer buffer) {
        synchronized (this.mLock) {
            this.mActive.remove(buffer);
        }
        addToSaveQueue(buffer);
    }

    private ActiveBuffer fetchActiveBuffersLocked(IGraphicsStatsCallback token, int uid, int pid, String packageName, long versionCode) throws RemoteException {
        ActiveBuffer buffer;
        int i;
        int i2;
        int size = this.mActive.size();
        long today = normalizeDate(System.currentTimeMillis()).getTimeInMillis();
        for (int i3 = 0; i3 < size; i3++) {
            buffer = (ActiveBuffer) this.mActive.get(i3);
            i = pid;
            if (buffer.mPid == i) {
                i2 = uid;
                if (buffer.mUid == i2) {
                    if (buffer.mInfo.startTime >= today) {
                        return buffer;
                    }
                    buffer.binderDied();
                    buffer = new ActiveBuffer(token, i2, i, packageName, versionCode);
                    this.mActive.add(buffer);
                    return buffer;
                }
            } else {
                i2 = uid;
            }
        }
        i2 = uid;
        i = pid;
        try {
            buffer = new ActiveBuffer(token, i2, i, packageName, versionCode);
            this.mActive.add(buffer);
            return buffer;
        } catch (IOException e) {
            throw new RemoteException("Failed to allocate space");
        }
    }

    private HashSet<File> dumpActiveLocked(long dump, ArrayList<HistoricalBuffer> buffers) {
        HashSet<File> skipFiles = new HashSet(buffers.size());
        for (int i = 0; i < buffers.size(); i++) {
            HistoricalBuffer buffer = (HistoricalBuffer) buffers.get(i);
            File path = pathForApp(buffer.mInfo);
            skipFiles.add(path);
            nAddToDump(dump, path.getAbsolutePath(), buffer.mInfo.packageName, buffer.mInfo.versionCode, buffer.mInfo.startTime, buffer.mInfo.endTime, buffer.mData);
        }
        ArrayList<HistoricalBuffer> arrayList = buffers;
        return skipFiles;
    }

    private void dumpHistoricalLocked(long dump, HashSet<File> skipFiles) {
        long j;
        HashSet<File> hashSet;
        File[] listFiles = this.mGraphicsStatsDir.listFiles();
        int length = listFiles.length;
        int i = 0;
        while (i < length) {
            File[] listFiles2 = listFiles[i].listFiles();
            int length2 = listFiles2.length;
            int i2 = 0;
            while (i2 < length2) {
                File[] listFiles3 = listFiles2[i2].listFiles();
                int length3 = listFiles3.length;
                int i3 = 0;
                while (i3 < length3) {
                    File[] fileArr;
                    File data = new File(listFiles3[i3], "total");
                    if (skipFiles.contains(data)) {
                        fileArr = listFiles;
                        j = dump;
                    } else {
                        fileArr = listFiles;
                        nAddToDump(dump, data.getAbsolutePath());
                    }
                    i3++;
                    listFiles = fileArr;
                }
                hashSet = skipFiles;
                j = dump;
                i2++;
                listFiles = listFiles;
            }
            hashSet = skipFiles;
            j = dump;
            i++;
            listFiles = listFiles;
        }
        j = dump;
        hashSet = skipFiles;
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, fout)) {
            ArrayList<HistoricalBuffer> buffers;
            boolean dumpProto = false;
            int i = 0;
            for (String str : args) {
                if (PriorityDump.PROTO_ARG.equals(str)) {
                    dumpProto = true;
                    break;
                }
            }
            synchronized (this.mLock) {
                buffers = new ArrayList(this.mActive.size());
                while (i < this.mActive.size()) {
                    try {
                        buffers.add(new HistoricalBuffer((ActiveBuffer) this.mActive.get(i)));
                    } catch (IOException e) {
                    }
                    i++;
                }
            }
            ArrayList<HistoricalBuffer> buffers2 = buffers;
            long dump = nCreateDump(fd.getInt$(), dumpProto);
            try {
                synchronized (this.mFileAccessLock) {
                    HashSet<File> skipList = dumpActiveLocked(dump, buffers2);
                    buffers2.clear();
                    dumpHistoricalLocked(dump, skipList);
                }
                nFinishDump(dump);
            } catch (Throwable th) {
                nFinishDump(dump);
            }
        }
    }
}
