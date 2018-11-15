package com.android.server.storage;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.Notification.TvExtender;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.ArrayMap;
import android.util.DataUnit;
import android.util.Slog;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.EventLogTags;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.InstructionSets;
import com.android.server.pm.PackageManagerService;
import com.android.server.utils.PriorityDump;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class DeviceStorageMonitorService extends AbsDeviceStorageMonitorService {
    private static final long BOOT_IMAGE_STORAGE_REQUIREMENT = DataUnit.MEBIBYTES.toBytes(250);
    private static final long DEFAULT_CHECK_INTERVAL = 60000;
    private static final long DEFAULT_LOG_DELTA_BYTES = DataUnit.MEBIBYTES.toBytes(64);
    public static final String EXTRA_SEQUENCE = "seq";
    private static final int MSG_CHECK = 1;
    static final int OPTION_FORCE_UPDATE = 1;
    private static final int OWNER_USERFLAG = 0;
    static final String SERVICE = "devicestoragemonitor";
    private static final String TAG = "DeviceStorageMonitorService";
    private static final String TV_NOTIFICATION_CHANNEL_ID = "devicestoragemonitor.tv";
    private CacheFileDeletedObserver mCacheFileDeletedObserver;
    private volatile int mForceLevel = -1;
    protected final Handler mHandler;
    private final HandlerThread mHandlerThread = new HandlerThread(TAG, 10);
    private final DeviceStorageMonitorInternal mLocalService = new DeviceStorageMonitorInternal() {
        public void checkMemory() {
            DeviceStorageMonitorService.this.mHandler.removeMessages(1);
            DeviceStorageMonitorService.this.mHandler.obtainMessage(1).sendToTarget();
        }

        public boolean isMemoryLow() {
            return Environment.getDataDirectory().getUsableSpace() < getMemoryLowThreshold();
        }

        public long getMemoryLowThreshold() {
            return ((StorageManager) DeviceStorageMonitorService.this.getContext().getSystemService(StorageManager.class)).getStorageLowBytes(Environment.getDataDirectory());
        }
    };
    private NotificationManager mNotifManager;
    private final Binder mRemoteService = new Binder() {
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(DeviceStorageMonitorService.this.getContext(), DeviceStorageMonitorService.TAG, pw)) {
                DeviceStorageMonitorService.this.dumpImpl(fd, pw, args);
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new Shell().exec(this, in, out, err, args, callback, resultReceiver);
        }
    };
    private final AtomicInteger mSeq = new AtomicInteger(1);
    private final ArrayMap<UUID, State> mStates = new ArrayMap();

    private static class CacheFileDeletedObserver extends FileObserver {
        public CacheFileDeletedObserver() {
            super(Environment.getDownloadCacheDirectory().getAbsolutePath(), 512);
        }

        public void onEvent(int event, String path) {
            EventLogTags.writeCacheFileDeleted(path);
        }
    }

    class Shell extends ShellCommand {
        Shell() {
        }

        public int onCommand(String cmd) {
            return DeviceStorageMonitorService.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
            DeviceStorageMonitorService.dumpHelp(getOutPrintWriter());
        }
    }

    private static class State {
        private static final int LEVEL_FULL = 2;
        private static final int LEVEL_LOW = 1;
        private static final int LEVEL_NORMAL = 0;
        private static final int LEVEL_UNKNOWN = -1;
        public long lastUsableBytes;
        public int level;

        private State() {
            this.level = 0;
            this.lastUsableBytes = JobStatus.NO_LATEST_RUNTIME;
        }

        /* synthetic */ State(AnonymousClass1 x0) {
            this();
        }

        private static boolean isEntering(int level, int oldLevel, int newLevel) {
            return newLevel >= level && (oldLevel < level || oldLevel == -1);
        }

        private static boolean isLeaving(int level, int oldLevel, int newLevel) {
            return newLevel < level && (oldLevel >= level || oldLevel == -1);
        }

        private static String levelToString(int level) {
            switch (level) {
                case -1:
                    return "UNKNOWN";
                case 0:
                    return PriorityDump.PRIORITY_ARG_NORMAL;
                case 1:
                    return "LOW";
                case 2:
                    return "FULL";
                default:
                    return Integer.toString(level);
            }
        }
    }

    private State findOrCreateState(UUID uuid) {
        State state = (State) this.mStates.get(uuid);
        if (state != null) {
            return state;
        }
        state = new State();
        this.mStates.put(uuid, state);
        return state;
    }

    private void check() {
        StorageManager storage = (StorageManager) getContext().getSystemService(StorageManager.class);
        if (storage == null) {
            Slog.w(TAG, "StorageManager is null, return.");
            return;
        }
        int seq = this.mSeq.get();
        for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
            int newLevel;
            File file = vol.getPath();
            long fullBytes = storage.getStorageFullBytes(file);
            long lowBytes = storage.getStorageLowBytes(file);
            if (file.getUsableSpace() < (3 * lowBytes) / 2) {
                PackageManagerService pms = (PackageManagerService) ServiceManager.getService("package");
                try {
                    Slog.i(TAG, "Start to freeStorage try to trimming usage back to 200% of the threshold");
                    pms.freeStorage(vol.getFsUuid(), lowBytes * 2, 0);
                    Slog.i(TAG, "Finish to freeStorage try to trimming usage back to 200% of the threshold");
                } catch (IOException e) {
                    Slog.w(TAG, e);
                }
            }
            UUID uuid = StorageManager.convert(vol.getFsUuid());
            State state = findOrCreateState(uuid);
            long totalBytes = file.getTotalSpace();
            long usableBytes = file.getUsableSpace();
            int oldLevel = state.level;
            StorageManager storage2 = storage;
            if (this.mForceLevel != -1) {
                storage = -1;
                newLevel = this.mForceLevel;
            } else {
                if (usableBytes <= fullBytes) {
                    newLevel = 2;
                } else if (usableBytes <= lowBytes) {
                    newLevel = 1;
                } else if (StorageManager.UUID_DEFAULT.equals(uuid) == null || isBootImageOnDisk() != null || usableBytes >= BOOT_IMAGE_STORAGE_REQUIREMENT) {
                    storage = oldLevel;
                    newLevel = 0;
                } else {
                    newLevel = 1;
                }
                storage = oldLevel;
            }
            long usableBytes2;
            if (Math.abs(state.lastUsableBytes - usableBytes) > DEFAULT_LOG_DELTA_BYTES || storage != newLevel) {
                usableBytes2 = usableBytes;
                EventLogTags.writeStorageState(uuid.toString(), storage, newLevel, usableBytes2, totalBytes);
                state.lastUsableBytes = usableBytes2;
            } else {
                usableBytes2 = usableBytes;
            }
            updateNotifications(vol, storage, newLevel);
            updateBroadcasts(vol, storage, newLevel, seq);
            state.level = newLevel;
            if (getCust() != null && usableBytes2 < getCust().getCritiLowMemThreshold()) {
                getCust().clearMemoryForCritiLow();
            }
            storage = storage2;
        }
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 60000);
        }
    }

    public DeviceStorageMonitorService(Context context) {
        super(context);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    DeviceStorageMonitorService.this.check();
                }
            }
        };
    }

    private static boolean isBootImageOnDisk() {
        for (String instructionSet : InstructionSets.getAllDexCodeInstructionSets()) {
            if (!VMRuntime.isBootClassPathOnDisk(instructionSet)) {
                return false;
            }
        }
        return true;
    }

    public void onStart() {
        Context context = getContext();
        this.mNotifManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        this.mCacheFileDeletedObserver = new CacheFileDeletedObserver();
        this.mCacheFileDeletedObserver.startWatching();
        if (context.getPackageManager().hasSystemFeature("android.software.leanback")) {
            this.mNotifManager.createNotificationChannel(new NotificationChannel(TV_NOTIFICATION_CHANNEL_ID, context.getString(17039936), 4));
        }
        publishBinderService(SERVICE, this.mRemoteService);
        publishLocalService(DeviceStorageMonitorInternal.class, this.mLocalService);
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    int parseOptions(Shell shell) {
        int opts = 0;
        while (true) {
            String nextOption = shell.getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                return opts;
            }
            if ("-f".equals(opt)) {
                opts |= 1;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x0046  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00a1  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0076  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004b  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0046  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00a1  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0076  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004b  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0046  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00a1  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0076  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int onShellCommand(Shell shell, String cmd) {
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        int hashCode = cmd.hashCode();
        if (hashCode != 108404047) {
            if (hashCode != 1526871410) {
                if (hashCode == 1692300408 && cmd.equals("force-not-low")) {
                    hashCode = 1;
                    int seq;
                    switch (hashCode) {
                        case 0:
                            hashCode = parseOptions(shell);
                            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                            this.mForceLevel = 1;
                            seq = this.mSeq.incrementAndGet();
                            if ((hashCode & 1) != 0) {
                                this.mHandler.removeMessages(1);
                                this.mHandler.obtainMessage(1).sendToTarget();
                                pw.println(seq);
                                break;
                            }
                            break;
                        case 1:
                            hashCode = parseOptions(shell);
                            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                            this.mForceLevel = 0;
                            seq = this.mSeq.incrementAndGet();
                            if ((hashCode & 1) != 0) {
                                this.mHandler.removeMessages(1);
                                this.mHandler.obtainMessage(1).sendToTarget();
                                pw.println(seq);
                                break;
                            }
                            break;
                        case 2:
                            hashCode = parseOptions(shell);
                            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                            this.mForceLevel = -1;
                            seq = this.mSeq.incrementAndGet();
                            if ((hashCode & 1) != 0) {
                                this.mHandler.removeMessages(1);
                                this.mHandler.obtainMessage(1).sendToTarget();
                                pw.println(seq);
                                break;
                            }
                            break;
                        default:
                            return shell.handleDefaultCommands(cmd);
                    }
                    return 0;
                }
            } else if (cmd.equals("force-low")) {
                hashCode = 0;
                switch (hashCode) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
                return 0;
            }
        } else if (cmd.equals("reset")) {
            hashCode = 2;
            switch (hashCode) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                default:
                    break;
            }
            return 0;
        }
        hashCode = -1;
        switch (hashCode) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            default:
                break;
        }
        return 0;
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Device storage monitor service (devicestoragemonitor) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  force-low [-f]");
        pw.println("    Force storage to be low, freezing storage state.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
        pw.println("  force-not-low [-f]");
        pw.println("    Force storage to not be low, freezing storage state.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
        pw.println("  reset [-f]");
        pw.println("    Unfreeze storage state, returning to current real values.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
    }

    void dumpImpl(FileDescriptor fd, PrintWriter _pw, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(_pw, "  ");
        int i = 0;
        if (args == null || args.length == 0 || "-a".equals(args[0])) {
            pw.println("Known volumes:");
            pw.increaseIndent();
            while (i < this.mStates.size()) {
                UUID uuid = (UUID) this.mStates.keyAt(i);
                State state = (State) this.mStates.valueAt(i);
                if (StorageManager.UUID_DEFAULT.equals(uuid)) {
                    pw.println("Default:");
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(uuid);
                    stringBuilder.append(":");
                    pw.println(stringBuilder.toString());
                }
                pw.increaseIndent();
                pw.printPair("level", State.levelToString(state.level));
                pw.printPair("lastUsableBytes", Long.valueOf(state.lastUsableBytes));
                pw.println();
                pw.decreaseIndent();
                i++;
            }
            pw.decreaseIndent();
            pw.println();
            pw.printPair("mSeq", Integer.valueOf(this.mSeq.get()));
            pw.printPair("mForceState", State.levelToString(this.mForceLevel));
            pw.println();
            pw.println();
            return;
        }
        new Shell().exec(this.mRemoteService, null, fd, null, args, null, new ResultReceiver(0));
    }

    private void updateNotifications(VolumeInfo vol, int oldLevel, int newLevel) {
        int i = oldLevel;
        int i2 = newLevel;
        Context context = getContext();
        UUID uuid = StorageManager.convert(vol.getFsUuid());
        if (State.isEntering(1, i, i2)) {
            CharSequence details;
            if (checkSystemManagerApkExist()) {
                if (!isBootImageOnDisk()) {
                    sendNotificationHwSM(vol.getPath().getUsableSpace(), isBootImageOnDisk(), 23, uuid.toString());
                    return;
                } else if (ActivityManager.getCurrentUser() == 0) {
                    return;
                }
            }
            Intent lowMemIntent = new Intent("android.os.storage.action.MANAGE_STORAGE");
            lowMemIntent.putExtra("android.os.storage.extra.UUID", uuid);
            lowMemIntent.addFlags(268435456);
            CharSequence title = context.getText(17040400);
            int i3 = 17040398;
            if (StorageManager.UUID_DEFAULT.equals(uuid)) {
                if (!isBootImageOnDisk()) {
                    i3 = 17040399;
                }
                details = context.getText(i3);
            } else {
                details = context.getText(17040398);
            }
            CharSequence details2 = details;
            Notification notification = new Builder(context, SystemNotificationChannels.ALERTS).setSmallIcon(17303473).setTicker(title).setColor(context.getColor(17170784)).setContentTitle(title).setContentText(details2).setContentIntent(PendingIntent.getActivityAsUser(context, 0, lowMemIntent, 0, null, UserHandle.CURRENT)).setStyle(new BigTextStyle().bigText(details2)).setVisibility(1).setCategory("sys").extend(new TvExtender().setChannelId(TV_NOTIFICATION_CHANNEL_ID)).build();
            Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), 33751680);
            if (bmp != null) {
                notification.largeIcon = bmp;
            }
            notification.flags |= 32;
            this.mNotifManager.notifyAsUser(uuid.toString(), 23, notification, UserHandle.ALL);
        } else if (State.isLeaving(1, i, i2)) {
            this.mNotifManager.cancelAsUser(uuid.toString(), 23, UserHandle.ALL);
        }
    }

    private void updateBroadcasts(VolumeInfo vol, int oldLevel, int newLevel, int seq) {
        if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, vol.getFsUuid())) {
            Intent lowIntent = new Intent("android.intent.action.DEVICE_STORAGE_LOW").addFlags(85983232).putExtra(EXTRA_SEQUENCE, seq);
            Intent notLowIntent = new Intent("android.intent.action.DEVICE_STORAGE_OK").addFlags(85983232).putExtra(EXTRA_SEQUENCE, seq);
            if (State.isEntering(1, oldLevel, newLevel)) {
                getContext().sendStickyBroadcastAsUser(lowIntent, UserHandle.ALL);
            } else if (State.isLeaving(1, oldLevel, newLevel)) {
                getContext().removeStickyBroadcastAsUser(lowIntent, UserHandle.ALL);
                getContext().sendBroadcastAsUser(notLowIntent, UserHandle.ALL);
            }
            Intent fullIntent = new Intent("android.intent.action.DEVICE_STORAGE_FULL").addFlags(67108864).putExtra(EXTRA_SEQUENCE, seq);
            Intent notFullIntent = new Intent("android.intent.action.DEVICE_STORAGE_NOT_FULL").addFlags(67108864).putExtra(EXTRA_SEQUENCE, seq);
            if (State.isEntering(2, oldLevel, newLevel)) {
                getContext().sendStickyBroadcastAsUser(fullIntent, UserHandle.ALL);
            } else if (State.isLeaving(2, oldLevel, newLevel)) {
                getContext().removeStickyBroadcastAsUser(fullIntent, UserHandle.ALL);
                getContext().sendBroadcastAsUser(notFullIntent, UserHandle.ALL);
            }
        }
    }

    public boolean checkSystemManagerApkExist() {
        return false;
    }

    public void sendNotificationHwSM(long freeMem, boolean isBootImageOnDisk, int notficationId, String tag) {
    }
}
