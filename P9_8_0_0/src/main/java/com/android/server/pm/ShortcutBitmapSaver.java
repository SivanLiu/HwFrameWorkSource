package com.android.server.pm;

import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Icon;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import com.android.server.pm.-$Lambda$hS1mIPNPrUgj3Ey9GdylMJh-bQA.AnonymousClass1;
import java.io.ByteArrayOutputStream;
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
    private final Runnable mRunnable = new -$Lambda$hS1mIPNPrUgj3Ey9GdylMJh-bQA(this);
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
            return "PendingItem{size=" + this.bytes.length + " age=" + (SystemClock.uptimeMillis() - this.mInstantiatedUptimeMillis) + "ms" + " shortcut=" + this.shortcut.toInsecureString() + "}";
        }
    }

    public ShortcutBitmapSaver(ShortcutService service) {
        this.mService = service;
    }

    public boolean waitForAllSavesLocked() {
        CountDownLatch latch = new CountDownLatch(1);
        this.mExecutor.execute(new AnonymousClass1(latch));
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
        Throwable th;
        Icon icon = shortcut.getIcon();
        Preconditions.checkNotNull(icon);
        Bitmap original = icon.getBitmap();
        if (original == null) {
            Log.e(TAG, "Missing icon: " + shortcut);
            return;
        }
        try {
            ShortcutService shortcutService = this.mService;
            Bitmap shrunk = ShortcutService.shrinkBitmap(original, maxDimension);
            Throwable th2 = null;
            ByteArrayOutputStream byteArrayOutputStream = null;
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream(65536);
                try {
                    if (!shrunk.compress(format, quality, out)) {
                        Slog.wtf(TAG, "Unable to compress bitmap");
                    }
                    out.flush();
                    byte[] bytes = out.toByteArray();
                    out.close();
                    if (out != null) {
                        try {
                            out.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 != null) {
                        try {
                            throw th2;
                        } catch (Throwable th4) {
                            th = th4;
                            byteArrayOutputStream = out;
                        }
                    } else {
                        if (shrunk != original) {
                            shrunk.recycle();
                        }
                        shortcut.addFlags(2056);
                        if (icon.getType() == 5) {
                            shortcut.addFlags(512);
                        }
                        PendingItem item = new PendingItem(shortcut, bytes);
                        synchronized (this.mPendingItems) {
                            this.mPendingItems.add(item);
                        }
                        this.mExecutor.execute(this.mRunnable);
                    }
                } catch (Throwable th5) {
                    th = th5;
                    byteArrayOutputStream = out;
                    if (byteArrayOutputStream != null) {
                        try {
                            byteArrayOutputStream.close();
                        } catch (Throwable th6) {
                            th = th6;
                            if (shrunk != original) {
                                shrunk.recycle();
                            }
                            throw th;
                        }
                    }
                    if (th2 == null) {
                        throw th2;
                    }
                    throw th;
                }
            } catch (Throwable th7) {
                th = th7;
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                if (th2 == null) {
                    throw th;
                }
                throw th2;
            }
        } catch (Throwable e) {
            Slog.wtf(TAG, "Unable to write bitmap to file", e);
        }
    }

    /* synthetic */ void lambda$-com_android_server_pm_ShortcutBitmapSaver_7645() {
        do {
        } while (processPendingItems());
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean processPendingItems() {
        ShortcutInfo shortcutInfo = null;
        try {
            synchronized (this.mPendingItems) {
                if (this.mPendingItems.size() == 0) {
                    return false;
                }
                PendingItem item = (PendingItem) this.mPendingItems.pop();
            }
        } finally {
            if (shortcutInfo != null) {
                if (shortcutInfo.getBitmapPath() == null) {
                    removeIcon(shortcutInfo);
                }
                shortcutInfo.clearFlags(2048);
            }
        }
    }

    public void dumpLocked(PrintWriter pw, String prefix) {
        synchronized (this.mPendingItems) {
            int N = this.mPendingItems.size();
            pw.print(prefix);
            pw.println("Pending saves: Num=" + N + " Executor=" + this.mExecutor);
            for (PendingItem item : this.mPendingItems) {
                pw.print(prefix);
                pw.print("  ");
                pw.println(item);
            }
        }
    }
}
