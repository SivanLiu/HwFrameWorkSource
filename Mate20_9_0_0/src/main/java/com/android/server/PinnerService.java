package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.app.ResolverActivity;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.server.os.HwBootFail;
import dalvik.system.DexFile;
import dalvik.system.VMRuntime;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PinnerService extends SystemService {
    private static final int ARY_SIZ = 2;
    private static final long B_TO_G = 1073741824;
    private static final boolean DEBUG = false;
    private static final int IDX_FST = 0;
    private static final int IDX_SEC = 1;
    private static final int MAX_CAMERA_PIN_SIZE = 83886080;
    private static final int PAGE_SIZE = ((int) Os.sysconf(OsConstants._SC_PAGESIZE));
    private static final String PIN_META_FILENAME = "pinlist.meta";
    private static final String TAG = "PinnerService";
    private BinderService mBinderService;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == "android.intent.action.PACKAGE_REPLACED") {
                String packageName = intent.getData().getSchemeSpecificPart();
                ArraySet<String> updatedPackages = new ArraySet();
                updatedPackages.add(packageName);
                PinnerService.this.update(updatedPackages);
            }
        }
    };
    private final Context mContext;
    private final ArrayList<PinnedFile> mPinnedCameraFiles = new ArrayList();
    private final ArrayList<PinnedFile> mPinnedFiles = new ArrayList();
    private PinnerHandler mPinnerHandler = null;
    private final boolean mShouldPinCamera;

    private final class BinderService extends Binder {
        private BinderService() {
        }

        /* synthetic */ BinderService(PinnerService x0, AnonymousClass1 x1) {
            this();
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(PinnerService.this.mContext, PinnerService.TAG, pw)) {
                long totalSize = 0;
                Iterator it = PinnerService.this.snapshotPinnedFiles().iterator();
                while (it.hasNext()) {
                    pw.format("%s %s\n", new Object[]{pinnedFile.fileName, Integer.valueOf(((PinnedFile) it.next()).bytesPinned)});
                    totalSize += (long) pinnedFile.bytesPinned;
                }
                pw.format("Total size: %s\n", new Object[]{Long.valueOf(totalSize)});
            }
        }
    }

    static final class PinRange {
        int length;
        int start;

        PinRange() {
        }
    }

    private static abstract class PinRangeSource {
        abstract boolean read(PinRange pinRange);

        private PinRangeSource() {
        }

        /* synthetic */ PinRangeSource(AnonymousClass1 x0) {
            this();
        }
    }

    private static final class PinnedFile implements AutoCloseable {
        final int bytesPinned;
        final String fileName;
        private long mAddress;
        final int mapSize;

        PinnedFile(long address, int mapSize, String fileName, int bytesPinned) {
            this.mAddress = address;
            this.mapSize = mapSize;
            this.fileName = fileName;
            this.bytesPinned = bytesPinned;
        }

        public void close() {
            if (this.mAddress >= 0) {
                PinnerService.safeMunmap(this.mAddress, (long) this.mapSize);
                this.mAddress = -1;
            }
        }

        public void finalize() {
            close();
        }
    }

    final class PinnerHandler extends Handler {
        static final int PIN_CAMERA_MSG = 4000;
        static final int PIN_ONSTART_MSG = 4001;

        public PinnerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PIN_CAMERA_MSG /*4000*/:
                    PinnerService.this.handlePinCamera(msg.arg1);
                    return;
                case PIN_ONSTART_MSG /*4001*/:
                    PinnerService.this.handlePinOnStart();
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    }

    private static final class PinRangeSourceStatic extends PinRangeSource {
        private boolean mDone = false;
        private final int mPinLength;
        private final int mPinStart;

        PinRangeSourceStatic(int pinStart, int pinLength) {
            super();
            this.mPinStart = pinStart;
            this.mPinLength = pinLength;
        }

        boolean read(PinRange outPinRange) {
            outPinRange.start = this.mPinStart;
            outPinRange.length = this.mPinLength;
            boolean done = this.mDone;
            this.mDone = true;
            return done ^ 1;
        }
    }

    private static final class PinRangeSourceStream extends PinRangeSource {
        private boolean mDone = false;
        private final DataInputStream mStream;

        PinRangeSourceStream(InputStream stream) {
            super();
            this.mStream = new DataInputStream(stream);
        }

        boolean read(PinRange outPinRange) {
            if (!this.mDone) {
                try {
                    outPinRange.start = this.mStream.readInt();
                    outPinRange.length = this.mStream.readInt();
                } catch (IOException e) {
                    this.mDone = true;
                }
            }
            return this.mDone ^ true;
        }
    }

    public PinnerService(Context context) {
        super(context);
        this.mContext = context;
        this.mShouldPinCamera = context.getResources().getBoolean(17957001);
        this.mPinnerHandler = new PinnerHandler(BackgroundThread.get().getLooper());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addDataScheme("package");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    public void onStart() {
        this.mBinderService = new BinderService(this, null);
        publishBinderService("pinner", this.mBinderService);
        publishLocalService(PinnerService.class, this);
        this.mPinnerHandler.obtainMessage(4001).sendToTarget();
        this.mPinnerHandler.obtainMessage(4000, 0, 0).sendToTarget();
    }

    public void onSwitchUser(int userHandle) {
        this.mPinnerHandler.obtainMessage(4000, userHandle, 0).sendToTarget();
    }

    public void update(ArraySet<String> updatedPackages) {
        ApplicationInfo cameraInfo = getCameraInfo(0);
        if (cameraInfo != null && updatedPackages.contains(cameraInfo.packageName)) {
            Slog.i(TAG, "Updating pinned files.");
            this.mPinnerHandler.obtainMessage(4000, 0, 0).sendToTarget();
        }
    }

    private void handlePinOnStart() {
        int deviceMemory = HwBootFail.STAGE_BOOT_SUCCESS;
        for (String fileToPin : this.mContext.getResources().getStringArray(17236000)) {
            String fileToPin2;
            StringBuilder stringBuilder;
            if (fileToPin2 != null && fileToPin2.contains(":")) {
                try {
                    String[] tmpArray = fileToPin2.split(":");
                    if (tmpArray.length != 2) {
                        String str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(fileToPin2);
                        stringBuilder.append(" is not valid");
                        Slog.w(str, stringBuilder.toString());
                    } else {
                        if (deviceMemory == HwBootFail.STAGE_BOOT_SUCCESS) {
                            deviceMemory = (int) Math.ceil(((double) Process.getTotalMemory()) / 1.073741824E9d);
                        }
                        if (deviceMemory >= Integer.parseInt(tmpArray[0])) {
                            fileToPin2 = tmpArray[1];
                        } else {
                            continue;
                        }
                    }
                } catch (Exception e) {
                    String str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Errors happened when pin file ");
                    stringBuilder.append(fileToPin2);
                    Slog.e(str2, stringBuilder.toString(), e);
                }
            }
            int deviceMemory2 = deviceMemory;
            PinnedFile pf = pinFile(fileToPin2, HwBootFail.STAGE_BOOT_SUCCESS, false);
            if (pf == null) {
                String str3 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to pin file = ");
                stringBuilder.append(fileToPin2);
                Slog.e(str3, stringBuilder.toString());
            } else {
                synchronized (this) {
                    this.mPinnedFiles.add(pf);
                }
            }
            deviceMemory = deviceMemory2;
        }
    }

    private void handlePinCamera(int userHandle) {
        if (this.mShouldPinCamera) {
            pinCamera(userHandle);
        }
    }

    private void unpinCameraApp() {
        ArrayList<PinnedFile> pinnedCameraFiles;
        synchronized (this) {
            pinnedCameraFiles = new ArrayList(this.mPinnedCameraFiles);
            this.mPinnedCameraFiles.clear();
        }
        Iterator it = pinnedCameraFiles.iterator();
        while (it.hasNext()) {
            ((PinnedFile) it.next()).close();
        }
    }

    private boolean isResolverActivity(ActivityInfo info) {
        return ResolverActivity.class.getName().equals(info.name);
    }

    private ApplicationInfo getCameraInfo(int userHandle) {
        ResolveInfo cameraResolveInfo = this.mContext.getPackageManager().resolveActivityAsUser(new Intent("android.media.action.STILL_IMAGE_CAMERA"), 851968, userHandle);
        if (cameraResolveInfo == null || isResolverActivity(cameraResolveInfo.activityInfo)) {
            return null;
        }
        return cameraResolveInfo.activityInfo.applicationInfo;
    }

    private boolean pinCamera(int userHandle) {
        ApplicationInfo cameraInfo = getCameraInfo(userHandle);
        if (cameraInfo == null) {
            return false;
        }
        unpinCameraApp();
        String camAPK = cameraInfo.sourceDir;
        PinnedFile pf = pinFile(camAPK, 83886080, true);
        if (pf == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to pin ");
            stringBuilder.append(camAPK);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
        synchronized (this) {
            this.mPinnedCameraFiles.add(pf);
        }
        String arch = "arm";
        StringBuilder stringBuilder2;
        if (cameraInfo.primaryCpuAbi != null) {
            if (VMRuntime.is64BitAbi(cameraInfo.primaryCpuAbi)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(arch);
                stringBuilder2.append("64");
                arch = stringBuilder2.toString();
            }
        } else if (VMRuntime.is64BitAbi(Build.SUPPORTED_ABIS[0])) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(arch);
            stringBuilder2.append("64");
            arch = stringBuilder2.toString();
        }
        String[] files = null;
        try {
            files = DexFile.getDexFileOutputPaths(cameraInfo.getBaseCodePath(), arch);
        } catch (IOException e) {
        }
        if (files == null) {
            return true;
        }
        for (String file : files) {
            PinnedFile pf2 = pinFile(file, 83886080, false);
            if (pf2 != null) {
                synchronized (this) {
                    this.mPinnedCameraFiles.add(pf2);
                }
            }
        }
        return true;
    }

    private static PinnedFile pinFile(String fileToPin, int maxBytesToPin, boolean attemptPinIntrospection) {
        PinRangeSource pinRangeSource;
        Closeable fileAsZip = null;
        Closeable pinRangeStream = null;
        if (attemptPinIntrospection) {
            try {
                fileAsZip = maybeOpenZip(fileToPin);
            } catch (Throwable th) {
                safeClose(null);
                safeClose(null);
            }
        }
        if (fileAsZip != null) {
            pinRangeStream = maybeOpenPinMetaInZip(fileAsZip, fileToPin);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pinRangeStream: ");
        stringBuilder.append(pinRangeStream);
        Slog.d(str, stringBuilder.toString());
        if (pinRangeStream != null) {
            pinRangeSource = new PinRangeSourceStream(pinRangeStream);
        } else {
            pinRangeSource = new PinRangeSourceStatic(0, HwBootFail.STAGE_BOOT_SUCCESS);
        }
        PinnedFile pinFileRanges = pinFileRanges(fileToPin, maxBytesToPin, pinRangeSource);
        safeClose(pinRangeStream);
        safeClose(fileAsZip);
        return pinFileRanges;
    }

    private static ZipFile maybeOpenZip(String fileName) {
        try {
            return new ZipFile(fileName);
        } catch (IOException ex) {
            Slog.w(TAG, String.format("could not open \"%s\" as zip: pinning as blob", new Object[]{fileName}), ex);
            return null;
        }
    }

    private static InputStream maybeOpenPinMetaInZip(ZipFile zipFile, String fileName) {
        ZipEntry pinMetaEntry = zipFile.getEntry(PIN_META_FILENAME);
        if (pinMetaEntry == null) {
            return null;
        }
        try {
            return zipFile.getInputStream(pinMetaEntry);
        } catch (IOException ex) {
            Slog.w(TAG, String.format("error reading pin metadata \"%s\": pinning as blob", new Object[]{fileName}), ex);
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:77:0x0132  */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0142  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0132  */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0142  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0132  */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0142  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0132  */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0142  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0132  */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0142  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0132  */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0142  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0132  */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0142  */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0142  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static PinnedFile pinFileRanges(String fileToPin, int maxBytesToPin, PinRangeSource pinRangeSource) {
        long address;
        ErrnoException ex;
        String str;
        StringBuilder stringBuilder;
        Throwable ex2;
        FileDescriptor fileDescriptor;
        String str2 = fileToPin;
        FileDescriptor fd = new FileDescriptor();
        long address2 = -1;
        int i = 0;
        int mapSize = 0;
        FileDescriptor fd2;
        int i2;
        try {
            int i3;
            int bytesPinned;
            fd2 = Os.open(str2, OsConstants.O_NOFOLLOW | (OsConstants.O_RDONLY | OsConstants.O_CLOEXEC), 0);
            try {
                mapSize = (int) Math.min(Os.fstat(fd2).st_size, 2147483647L);
                try {
                    address = Os.mmap(0, (long) mapSize, OsConstants.PROT_READ, OsConstants.MAP_SHARED, fd2, 0);
                } catch (ErrnoException e) {
                    ex = e;
                    i3 = mapSize;
                    i2 = maxBytesToPin;
                    fd = fd2;
                    try {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not pin file ");
                        stringBuilder.append(str2);
                        Slog.e(str, stringBuilder.toString(), ex);
                        safeClose(fd);
                        if (address2 >= 0) {
                        }
                        return null;
                    } catch (Throwable th) {
                        ex2 = th;
                        fd2 = fd;
                        address = address2;
                        safeClose(fd2);
                        if (address >= 0) {
                        }
                        throw ex2;
                    }
                } catch (Throwable th2) {
                    ex2 = th2;
                    i3 = mapSize;
                    fileDescriptor = fd2;
                    i2 = maxBytesToPin;
                    address = -1;
                    safeClose(fd2);
                    if (address >= 0) {
                    }
                    throw ex2;
                }
            } catch (ErrnoException e2) {
                ex = e2;
                i2 = maxBytesToPin;
                fd = fd2;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not pin file ");
                stringBuilder.append(str2);
                Slog.e(str, stringBuilder.toString(), ex);
                safeClose(fd);
                if (address2 >= 0) {
                }
                return null;
            } catch (Throwable th3) {
                ex2 = th3;
                fileDescriptor = fd2;
                i2 = maxBytesToPin;
                address = -1;
                safeClose(fd2);
                if (address >= 0) {
                }
                throw ex2;
            }
            try {
                PinRange pinRange = new PinRange();
                if (maxBytesToPin % PAGE_SIZE != 0) {
                    try {
                        i2 = maxBytesToPin - (maxBytesToPin % PAGE_SIZE);
                    } catch (ErrnoException e3) {
                        ex = e3;
                        fd = fd2;
                        address2 = address;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not pin file ");
                        stringBuilder.append(str2);
                        Slog.e(str, stringBuilder.toString(), ex);
                        safeClose(fd);
                        if (address2 >= 0) {
                        }
                        return null;
                    } catch (Throwable th4) {
                        ex2 = th4;
                        i2 = maxBytesToPin;
                        safeClose(fd2);
                        if (address >= 0) {
                        }
                        throw ex2;
                    }
                }
                i2 = maxBytesToPin;
                bytesPinned = 0;
                while (bytesPinned < i2) {
                    try {
                        if (pinRangeSource.read(pinRange)) {
                            int pinStart = pinRange.start;
                            int pinLength = pinRange.length;
                            pinStart = clamp(i, pinStart, mapSize);
                            pinLength = Math.min(i2 - bytesPinned, clamp(i, pinLength, mapSize - pinStart)) + (pinStart % PAGE_SIZE);
                            pinStart -= pinStart % PAGE_SIZE;
                            if (pinLength % PAGE_SIZE != 0) {
                                pinLength += PAGE_SIZE - (pinLength % PAGE_SIZE);
                            }
                            pinLength = clamp(i, pinLength, i2 - bytesPinned);
                            if (pinLength > 0) {
                                Os.mlock(((long) pinStart) + address, (long) pinLength);
                            }
                            bytesPinned += pinLength;
                            i = 0;
                        }
                        break;
                    } catch (ErrnoException e4) {
                        ex = e4;
                        fd = fd2;
                        address2 = address;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not pin file ");
                        stringBuilder.append(str2);
                        Slog.e(str, stringBuilder.toString(), ex);
                        safeClose(fd);
                        if (address2 >= 0) {
                            safeMunmap(address2, (long) mapSize);
                        }
                        return null;
                    } catch (Throwable th5) {
                        ex2 = th5;
                        safeClose(fd2);
                        if (address >= 0) {
                            safeMunmap(address, (long) mapSize);
                        }
                        throw ex2;
                    }
                }
                PinRangeSource pinRangeSource2 = pinRangeSource;
            } catch (ErrnoException e5) {
                ex = e5;
                i3 = mapSize;
                fileDescriptor = fd2;
                i2 = maxBytesToPin;
                fd = fileDescriptor;
                address2 = address;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not pin file ");
                stringBuilder.append(str2);
                Slog.e(str, stringBuilder.toString(), ex);
                safeClose(fd);
                if (address2 >= 0) {
                }
                return null;
            } catch (Throwable th6) {
                ex2 = th6;
                i3 = mapSize;
                fileDescriptor = fd2;
                i2 = maxBytesToPin;
                safeClose(fd2);
                if (address >= 0) {
                }
                throw ex2;
            }
            try {
                PinnedFile pinnedFile = pinnedFile;
                i3 = mapSize;
                fileDescriptor = fd2;
                try {
                    pinnedFile = new PinnedFile(address, mapSize, str2, bytesPinned);
                    safeClose(fileDescriptor);
                    if (-1 >= 0) {
                        safeMunmap(-1, (long) i3);
                    }
                    return pinnedFile;
                } catch (ErrnoException e6) {
                    ex = e6;
                    mapSize = i3;
                    fd = fileDescriptor;
                    address2 = address;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Could not pin file ");
                    stringBuilder.append(str2);
                    Slog.e(str, stringBuilder.toString(), ex);
                    safeClose(fd);
                    if (address2 >= 0) {
                    }
                    return null;
                } catch (Throwable th7) {
                    ex2 = th7;
                    mapSize = i3;
                    fd2 = fileDescriptor;
                    safeClose(fd2);
                    if (address >= 0) {
                    }
                    throw ex2;
                }
            } catch (ErrnoException e7) {
                ex = e7;
                i3 = mapSize;
                fileDescriptor = fd2;
                fd = fileDescriptor;
                address2 = address;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not pin file ");
                stringBuilder.append(str2);
                Slog.e(str, stringBuilder.toString(), ex);
                safeClose(fd);
                if (address2 >= 0) {
                }
                return null;
            } catch (Throwable th8) {
                ex2 = th8;
                i3 = mapSize;
                fileDescriptor = fd2;
                safeClose(fd2);
                if (address >= 0) {
                }
                throw ex2;
            }
        } catch (ErrnoException e8) {
            ex = e8;
            i2 = maxBytesToPin;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not pin file ");
            stringBuilder.append(str2);
            Slog.e(str, stringBuilder.toString(), ex);
            safeClose(fd);
            if (address2 >= 0) {
            }
            return null;
        } catch (Throwable th9) {
            ex2 = th9;
            i2 = maxBytesToPin;
            fd2 = fd;
            address = address2;
            safeClose(fd2);
            if (address >= 0) {
            }
            throw ex2;
        }
    }

    private static int clamp(int min, int value, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static void safeMunmap(long address, long mapSize) {
        try {
            Os.munmap(address, mapSize);
        } catch (ErrnoException ex) {
            Slog.w(TAG, "ignoring error in unmap", ex);
        }
    }

    private static void safeClose(FileDescriptor fd) {
        if (fd != null && fd.valid()) {
            try {
                Os.close(fd);
            } catch (ErrnoException ex) {
                if (ex.errno == OsConstants.EBADF) {
                    throw new AssertionError(ex);
                }
            }
        }
    }

    private static void safeClose(Closeable thing) {
        if (thing != null) {
            try {
                thing.close();
            } catch (IOException ex) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ignoring error closing resource: ");
                stringBuilder.append(thing);
                Slog.w(str, stringBuilder.toString(), ex);
            }
        }
    }

    private synchronized ArrayList<PinnedFile> snapshotPinnedFiles() {
        ArrayList<PinnedFile> pinnedFiles;
        pinnedFiles = new ArrayList(this.mPinnedFiles.size() + this.mPinnedCameraFiles.size());
        pinnedFiles.addAll(this.mPinnedFiles);
        pinnedFiles.addAll(this.mPinnedCameraFiles);
        return pinnedFiles;
    }
}
