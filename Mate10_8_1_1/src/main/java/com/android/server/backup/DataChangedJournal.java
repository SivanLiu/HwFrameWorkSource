package com.android.server.backup;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public final class DataChangedJournal {
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

    public void addPackage(String packageName) throws IOException {
        Throwable th;
        Throwable th2 = null;
        RandomAccessFile randomAccessFile = null;
        try {
            RandomAccessFile out = new RandomAccessFile(this.mFile, "rws");
            try {
                out.seek(out.length());
                out.writeUTF(packageName);
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 != null) {
                    throw th2;
                }
            } catch (Throwable th4) {
                th = th4;
                randomAccessFile = out;
                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    throw th2;
                }
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
            if (th2 == null) {
                throw th;
            }
            throw th2;
        }
    }

    public void forEach(Consumer consumer) throws IOException {
        Throwable th;
        Throwable th2;
        Throwable th3 = null;
        BufferedInputStream bufferedInputStream = null;
        DataInputStream dataInputStream = null;
        try {
            BufferedInputStream bufferedInputStream2 = new BufferedInputStream(new FileInputStream(this.mFile), 8192);
            try {
                DataInputStream dataInputStream2 = new DataInputStream(bufferedInputStream2);
                while (dataInputStream2.available() > 0) {
                    try {
                        consumer.accept(dataInputStream2.readUTF());
                    } catch (Throwable th4) {
                        th = th4;
                        dataInputStream = dataInputStream2;
                        bufferedInputStream = bufferedInputStream2;
                    }
                }
                if (dataInputStream2 != null) {
                    try {
                        dataInputStream2.close();
                    } catch (Throwable th5) {
                        th3 = th5;
                    }
                }
                if (bufferedInputStream2 != null) {
                    try {
                        bufferedInputStream2.close();
                    } catch (Throwable th6) {
                        th = th6;
                        if (th3 != null) {
                            if (th3 != th) {
                                th3.addSuppressed(th);
                                th = th3;
                            }
                        }
                    }
                }
                th = th3;
                if (th != null) {
                    throw th;
                }
            } catch (Throwable th7) {
                th = th7;
                bufferedInputStream = bufferedInputStream2;
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (Throwable th8) {
                        th2 = th8;
                        if (th3 != null) {
                            if (th3 != th2) {
                                th3.addSuppressed(th2);
                                th2 = th3;
                            }
                        }
                    }
                }
                th2 = th3;
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (Throwable th9) {
                        th3 = th9;
                        if (th2 != null) {
                            if (th2 != th3) {
                                th2.addSuppressed(th3);
                                th3 = th2;
                            }
                        }
                    }
                }
                th3 = th2;
                if (th3 == null) {
                    throw th;
                }
                throw th3;
            }
        } catch (Throwable th10) {
            th = th10;
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            th2 = th3;
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            th3 = th2;
            if (th3 == null) {
                throw th3;
            }
            throw th;
        }
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
