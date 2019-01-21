package com.android.server.pm;

import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public boolean save(String filename, Consumer<PrintWriter> dumper) {
        PrintWriter pw;
        String str;
        StringBuilder stringBuilder;
        try {
            File directory = this.mService.getDumpPath();
            directory.mkdirs();
            if (directory.exists()) {
                pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(directory, filename))));
                dumper.accept(pw);
                $closeResource(null, pw);
                return true;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to create directory: ");
            stringBuilder.append(directory);
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (IOException | RuntimeException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to create dump file: ");
            stringBuilder.append(filename);
            Slog.w(str, stringBuilder.toString(), e);
            return false;
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

    public void dumpAll(PrintWriter pw) {
        BufferedReader reader;
        try {
            File directory = this.mService.getDumpPath();
            File[] files = directory.listFiles(-$$Lambda$ShortcutDumpFiles$v6wMz6MRa9pgSnEDM_9bjvrLaKY.INSTANCE);
            if (directory.exists()) {
                if (!ArrayUtils.isEmpty(files)) {
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
                    return;
                }
            }
            pw.print("  No dump files found.");
        } catch (IOException | RuntimeException e) {
            Slog.w(TAG, "Failed to print dump files", e);
        } catch (Throwable th) {
            $closeResource(th, reader);
        }
    }
}
