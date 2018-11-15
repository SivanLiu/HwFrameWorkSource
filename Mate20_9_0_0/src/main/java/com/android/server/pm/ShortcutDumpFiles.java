package com.android.server.pm;

import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

public class ShortcutDumpFiles {
    private static final boolean DEBUG = false;
    private static final String TAG = "ShortcutService";
    private final ShortcutService mService;

    public ShortcutDumpFiles(ShortcutService service) {
        this.mService = service;
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x004c A:{Splitter: B:1:0x0001, ExcHandler: java.lang.RuntimeException (r1_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:21:0x004c, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:22:0x004d, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Failed to create dump file: ");
            r3.append(r7);
            android.util.Slog.w(r2, r3.toString(), r1);
     */
    /* JADX WARNING: Missing block: B:23:0x0063, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean save(String filename, Consumer<PrintWriter> dumper) {
        PrintWriter pw;
        try {
            File directory = this.mService.getDumpPath();
            directory.mkdirs();
            if (directory.exists()) {
                pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(directory, filename))));
                dumper.accept(pw);
                $closeResource(null, pw);
                return true;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to create directory: ");
            stringBuilder.append(directory);
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (Exception e) {
        } catch (Throwable th) {
            $closeResource(r4, pw);
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    public boolean save(String filename, byte[] utf8bytes) {
        return save(filename, new -$$Lambda$ShortcutDumpFiles$rwmVVp6PnQCcurF7D6VzrdNqEdk(utf8bytes));
    }

    /* JADX WARNING: Removed duplicated region for block: B:27:0x0077 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.RuntimeException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:27:0x0077, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:28:0x0078, code:
            android.util.Slog.w(TAG, "Failed to print dump files", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dumpAll(PrintWriter pw) {
        BufferedReader reader;
        try {
            File directory = this.mService.getDumpPath();
            File[] files = directory.listFiles(-$$Lambda$ShortcutDumpFiles$v6wMz6MRa9pgSnEDM_9bjvrLaKY.INSTANCE);
            if (!directory.exists() || ArrayUtils.isEmpty(files)) {
                pw.print("  No dump files found.");
                return;
            }
            Arrays.sort(files, Comparator.comparing(-$$Lambda$ShortcutDumpFiles$stGgHzhh-NVWPgDSwmH2ybAWRE8.INSTANCE));
            for (File path : files) {
                pw.print("*** Dumping: ");
                pw.println(path.getName());
                pw.print("mtime: ");
                pw.println(ShortcutService.formatTime(path.lastModified()));
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
                String line = null;
                while (true) {
                    String readLine = reader.readLine();
                    line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    pw.println(line);
                }
                $closeResource(null, reader);
            }
        } catch (Exception e) {
        } catch (Throwable th) {
            $closeResource(th, reader);
        }
    }
}
