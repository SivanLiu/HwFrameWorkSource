package com.android.server.backup;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class DataChangedJournal {
    private static final int BUFFER_SIZE_BYTES = 8192;
    private static final String FILE_NAME_PREFIX = "journal";
    private final File mFile;

    @FunctionalInterface
    public interface Consumer {
        void accept(String str);
    }

    DataChangedJournal(File file) {
        this.mFile = file;
    }

    /* JADX WARNING: Missing block: B:9:0x001d, code:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addPackage(String packageName) throws IOException {
        RandomAccessFile out = new RandomAccessFile(this.mFile, "rws");
        out.seek(out.length());
        out.writeUTF(packageName);
        $closeResource(null, out);
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

    /* JADX WARNING: Missing block: B:26:0x003c, code:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void forEach(Consumer consumer) throws IOException {
        Throwable th;
        Throwable th2;
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(this.mFile), 8192);
        DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
        while (dataInputStream.available() > 0) {
            try {
                consumer.accept(dataInputStream.readUTF());
            } catch (Throwable th22) {
                Throwable th3 = th22;
                th22 = th;
                th = th3;
            }
        }
        $closeResource(null, dataInputStream);
        $closeResource(null, bufferedInputStream);
        return;
        $closeResource(th22, dataInputStream);
        throw th;
    }

    public boolean delete() {
        return this.mFile.delete();
    }

    public boolean equals(Object object) {
        if (!(object instanceof DataChangedJournal)) {
            return false;
        }
        try {
            return this.mFile.getCanonicalPath().equals(((DataChangedJournal) object).mFile.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    public String toString() {
        return this.mFile.toString();
    }

    static DataChangedJournal newJournal(File journalDirectory) throws IOException {
        return new DataChangedJournal(File.createTempFile(FILE_NAME_PREFIX, null, journalDirectory));
    }

    static ArrayList<DataChangedJournal> listJournals(File journalDirectory) {
        ArrayList<DataChangedJournal> journals = new ArrayList();
        for (File file : journalDirectory.listFiles()) {
            journals.add(new DataChangedJournal(file));
        }
        return journals;
    }
}
