package com.android.server.backup;

import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

final class ProcessedPackagesJournal {
    private static final boolean DEBUG = true;
    private static final String JOURNAL_FILE_NAME = "processed";
    private static final String TAG = "ProcessedPackagesJournal";
    @GuardedBy("mProcessedPackages")
    private final Set<String> mProcessedPackages = new HashSet();
    private final File mStateDirectory;

    ProcessedPackagesJournal(File stateDirectory) {
        this.mStateDirectory = stateDirectory;
    }

    void init() {
        synchronized (this.mProcessedPackages) {
            loadFromDisk();
        }
    }

    boolean hasBeenProcessed(String packageName) {
        boolean contains;
        synchronized (this.mProcessedPackages) {
            contains = this.mProcessedPackages.contains(packageName);
        }
        return contains;
    }

    void addPackage(String packageName) {
        synchronized (this.mProcessedPackages) {
            if (this.mProcessedPackages.add(packageName)) {
                File journalFile = new File(this.mStateDirectory, JOURNAL_FILE_NAME);
                RandomAccessFile out;
                try {
                    out = new RandomAccessFile(journalFile, "rws");
                    out.seek(out.length());
                    out.writeUTF(packageName);
                    $closeResource(null, out);
                } catch (IOException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't log backup of ");
                    stringBuilder.append(packageName);
                    stringBuilder.append(" to ");
                    stringBuilder.append(journalFile);
                    Slog.e(str, stringBuilder.toString());
                } catch (Throwable th) {
                    $closeResource(r3, out);
                }
            } else {
                return;
            }
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

    Set<String> getPackagesCopy() {
        HashSet hashSet;
        synchronized (this.mProcessedPackages) {
            hashSet = new HashSet(this.mProcessedPackages);
        }
        return hashSet;
    }

    void reset() {
        synchronized (this.mProcessedPackages) {
            this.mProcessedPackages.clear();
            new File(this.mStateDirectory, JOURNAL_FILE_NAME).delete();
        }
    }

    private void loadFromDisk() {
        File journalFile = new File(this.mStateDirectory, JOURNAL_FILE_NAME);
        if (journalFile.exists()) {
            DataInputStream oldJournal;
            try {
                oldJournal = new DataInputStream(new BufferedInputStream(new FileInputStream(journalFile)));
                while (true) {
                    String packageName = oldJournal.readUTF();
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("   + ");
                    stringBuilder.append(packageName);
                    Slog.v(str, stringBuilder.toString());
                    this.mProcessedPackages.add(packageName);
                }
            } catch (EOFException e) {
            } catch (IOException e2) {
                Slog.e(TAG, "Error reading processed packages journal", e2);
            } catch (Throwable th) {
                $closeResource(r2, oldJournal);
            }
        }
    }
}
