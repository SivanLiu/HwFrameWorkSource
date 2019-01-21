package com.android.server.pm;

import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Icon;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.ThreadPolicy.Builder;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ShortcutBitmapSaver {
    private static final boolean ADD_DELAY_BEFORE_SAVE_FOR_TEST = false;
    private static final boolean DEBUG = false;
    private static final long SAVE_DELAY_MS_FOR_TEST = 1000;
    private static final String TAG = "ShortcutService";
    private final long SAVE_WAIT_TIMEOUT_MS = 30000;
    private final Executor mExecutor = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue());
    @GuardedBy("mPendingItems")
    private final Deque<PendingItem> mPendingItems = new LinkedBlockingDeque();
    private final Runnable mRunnable = new -$$Lambda$ShortcutBitmapSaver$AUDgG57FGyGDUVDAjL-7cuiE0pM(this);
    private final ShortcutService mService;

    private static class PendingItem {
        public final byte[] bytes;
        private final long mInstantiatedUptimeMillis;
        public final ShortcutInfo shortcut;

        private PendingItem(ShortcutInfo shortcut, byte[] bytes) {
            this.shortcut = shortcut;
            this.bytes = bytes;
            this.mInstantiatedUptimeMillis = SystemClock.uptimeMillis();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PendingItem{size=");
            stringBuilder.append(this.bytes.length);
            stringBuilder.append(" age=");
            stringBuilder.append(SystemClock.uptimeMillis() - this.mInstantiatedUptimeMillis);
            stringBuilder.append("ms shortcut=");
            stringBuilder.append(this.shortcut.toInsecureString());
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    public ShortcutBitmapSaver(ShortcutService service) {
        this.mService = service;
    }

    public boolean waitForAllSavesLocked() {
        CountDownLatch latch = new CountDownLatch(1);
        this.mExecutor.execute(new -$$Lambda$ShortcutBitmapSaver$xgjvZfaiKXavxgGCSta_eIdVBnk(latch));
        try {
            if (latch.await(30000, TimeUnit.MILLISECONDS)) {
                return true;
            }
            this.mService.wtf("Timed out waiting on saving bitmaps.");
            return false;
        } catch (InterruptedException e) {
            Slog.w(TAG, "interrupted");
        }
    }

    public String getBitmapPathMayWaitLocked(ShortcutInfo shortcut) {
        if (waitForAllSavesLocked() && shortcut.hasIconFile()) {
            return shortcut.getBitmapPath();
        }
        return null;
    }

    public void removeIcon(ShortcutInfo shortcut) {
        shortcut.setIconResourceId(0);
        shortcut.setIconResName(null);
        shortcut.setBitmapPath(null);
        shortcut.clearFlags(2572);
    }

    public void saveBitmapLocked(ShortcutInfo shortcut, int maxDimension, CompressFormat format, int quality) {
        Icon icon = shortcut.getIcon();
        Preconditions.checkNotNull(icon);
        Bitmap original = icon.getBitmap();
        if (original == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Missing icon: ");
            stringBuilder.append(shortcut);
            Log.e(str, stringBuilder.toString());
            return;
        }
        ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            StrictMode.setThreadPolicy(new Builder(oldPolicy).permitCustomSlowCalls().build());
            ShortcutService shortcutService = this.mService;
            Bitmap shrunk = ShortcutService.shrinkBitmap(original, maxDimension);
            ByteArrayOutputStream out;
            try {
                out = new ByteArrayOutputStream(65536);
                if (!shrunk.compress(format, quality, out)) {
                    Slog.wtf(TAG, "Unable to compress bitmap");
                }
                out.flush();
                byte[] bytes = out.toByteArray();
                out.close();
                out.close();
                out = bytes;
                if (shrunk != original) {
                    shrunk.recycle();
                }
                StrictMode.setThreadPolicy(oldPolicy);
                shortcut.addFlags(2056);
                if (icon.getType() == 5) {
                    shortcut.addFlags(512);
                }
                PendingItem item = new PendingItem(shortcut, out);
                synchronized (this.mPendingItems) {
                    this.mPendingItems.add(item);
                }
                this.mExecutor.execute(this.mRunnable);
            } catch (Throwable th) {
                if (shrunk != original) {
                    shrunk.recycle();
                }
            }
        } catch (IOException | OutOfMemoryError | RuntimeException e) {
            try {
                Slog.wtf(TAG, "Unable to write bitmap to file", e);
            } finally {
                StrictMode.setThreadPolicy(oldPolicy);
            }
        }
    }

    public static /* synthetic */ void lambda$new$1(ShortcutBitmapSaver shortcutBitmapSaver) {
        while (shortcutBitmapSaver.processPendingItems()) {
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0011, code skipped:
            if (r1 == null) goto L_0x001f;
     */
    /* JADX WARNING: Missing block: B:11:0x0017, code skipped:
            if (r1.getBitmapPath() != null) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:12:0x0019, code skipped:
            removeIcon(r1);
     */
    /* JADX WARNING: Missing block: B:13:0x001c, code skipped:
            r1.clearFlags(2048);
     */
    /* JADX WARNING: Missing block: B:14:0x001f, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            r1 = r4.shortcut;
     */
    /* JADX WARNING: Missing block: B:21:0x0031, code skipped:
            if (r1.isIconPendingSave() != false) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:22:0x0034, code skipped:
            if (r1 == null) goto L_0x0042;
     */
    /* JADX WARNING: Missing block: B:24:0x003a, code skipped:
            if (r1.getBitmapPath() != null) goto L_0x003f;
     */
    /* JADX WARNING: Missing block: B:25:0x003c, code skipped:
            removeIcon(r1);
     */
    /* JADX WARNING: Missing block: B:26:0x003f, code skipped:
            r1.clearFlags(2048);
     */
    /* JADX WARNING: Missing block: B:27:0x0042, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:29:?, code skipped:
            r3 = r8.mService.openIconFileForWrite(r1.getUserId(), r1);
     */
    /* JADX WARNING: Missing block: B:30:0x0052, code skipped:
            r0 = r3.getFile();
     */
    /* JADX WARNING: Missing block: B:32:?, code skipped:
            r3.write(r4.bytes);
     */
    /* JADX WARNING: Missing block: B:34:?, code skipped:
            libcore.io.IoUtils.closeQuietly(r3);
            r1.setBitmapPath(r0.getAbsolutePath());
     */
    /* JADX WARNING: Missing block: B:35:0x0064, code skipped:
            if (r1 == null) goto L_0x0072;
     */
    /* JADX WARNING: Missing block: B:37:0x006a, code skipped:
            if (r1.getBitmapPath() != null) goto L_0x006f;
     */
    /* JADX WARNING: Missing block: B:38:0x006c, code skipped:
            removeIcon(r1);
     */
    /* JADX WARNING: Missing block: B:39:0x006f, code skipped:
            r1.clearFlags(2048);
     */
    /* JADX WARNING: Missing block: B:40:0x0072, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:43:?, code skipped:
            libcore.io.IoUtils.closeQuietly(r3);
     */
    /* JADX WARNING: Missing block: B:45:0x0078, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            android.util.Slog.e(TAG, "Unable to write bitmap to file", r3);
     */
    /* JADX WARNING: Missing block: B:51:0x0088, code skipped:
            r0.delete();
     */
    /* JADX WARNING: Missing block: B:52:0x008c, code skipped:
            if (r1 != null) goto L_0x008e;
     */
    /* JADX WARNING: Missing block: B:54:0x0092, code skipped:
            if (r1.getBitmapPath() == null) goto L_0x0094;
     */
    /* JADX WARNING: Missing block: B:55:0x0094, code skipped:
            removeIcon(r1);
     */
    /* JADX WARNING: Missing block: B:56:0x0097, code skipped:
            r1.clearFlags(2048);
     */
    /* JADX WARNING: Missing block: B:57:0x009a, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean processPendingItems() {
        File file = null;
        ShortcutInfo shortcut = null;
        try {
            synchronized (this.mPendingItems) {
                if (this.mPendingItems.size() != 0) {
                    PendingItem item = (PendingItem) this.mPendingItems.pop();
                }
            }
        } catch (Throwable th) {
            if (shortcut != null) {
                if (shortcut.getBitmapPath() == null) {
                    removeIcon(shortcut);
                }
                shortcut.clearFlags(2048);
            }
        }
    }

    public void dumpLocked(PrintWriter pw, String prefix) {
        synchronized (this.mPendingItems) {
            int N = this.mPendingItems.size();
            pw.print(prefix);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Pending saves: Num=");
            stringBuilder.append(N);
            stringBuilder.append(" Executor=");
            stringBuilder.append(this.mExecutor);
            pw.println(stringBuilder.toString());
            for (PendingItem item : this.mPendingItems) {
                pw.print(prefix);
                pw.print("  ");
                pw.println(item);
            }
        }
    }
}
