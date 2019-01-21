package com.android.org.conscrypt;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSession;

public final class FileClientSessionCache {
    public static final int MAX_SIZE = 12;
    static final Map<File, Impl> caches = new HashMap();
    private static final Logger logger = Logger.getLogger(FileClientSessionCache.class.getName());

    static class CacheFile extends File {
        long lastModified = -1;
        final String name;

        CacheFile(File dir, String name) {
            super(dir, name);
            this.name = name;
        }

        public long lastModified() {
            long lastModified = this.lastModified;
            if (lastModified != -1) {
                return lastModified;
            }
            long lastModified2 = super.lastModified();
            this.lastModified = lastModified2;
            return lastModified2;
        }

        public int compareTo(File another) {
            long result = lastModified() - another.lastModified();
            if (result == 0) {
                return super.compareTo(another);
            }
            return result < 0 ? -1 : 1;
        }
    }

    static class Impl implements SSLClientSessionCache {
        Map<String, File> accessOrder = newAccessOrder();
        final File directory;
        String[] initialFiles;
        int size;

        Impl(File directory) throws IOException {
            boolean exists = directory.exists();
            StringBuilder stringBuilder;
            if (!exists || directory.isDirectory()) {
                if (exists) {
                    this.initialFiles = directory.list();
                    if (this.initialFiles != null) {
                        Arrays.sort(this.initialFiles);
                        this.size = this.initialFiles.length;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(directory);
                        stringBuilder.append(" exists but cannot list contents.");
                        throw new IOException(stringBuilder.toString());
                    }
                } else if (directory.mkdirs()) {
                    this.size = 0;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Creation of ");
                    stringBuilder.append(directory);
                    stringBuilder.append(" directory failed.");
                    throw new IOException(stringBuilder.toString());
                }
                this.directory = directory;
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(directory);
            stringBuilder.append(" exists but is not a directory.");
            throw new IOException(stringBuilder.toString());
        }

        private static Map<String, File> newAccessOrder() {
            return new LinkedHashMap(12, 0.75f, true);
        }

        private static String fileName(String host, int port) {
            if (host != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(host);
                stringBuilder.append(".");
                stringBuilder.append(port);
                return stringBuilder.toString();
            }
            throw new NullPointerException("host == null");
        }

        public synchronized byte[] getSessionData(String host, int port) {
            byte[] data;
            String name = fileName(host, port);
            File file = (File) this.accessOrder.get(name);
            byte[] bArr = null;
            if (file == null) {
                if (this.initialFiles == null) {
                    return bArr;
                }
                if (Arrays.binarySearch(this.initialFiles, name) < 0) {
                    return bArr;
                }
                file = new File(this.directory, name);
                this.accessOrder.put(name, file);
            }
            try {
                FileInputStream in = new FileInputStream(file);
                try {
                    data = new byte[((int) file.length())];
                    new DataInputStream(in).readFully(data);
                    try {
                        in.close();
                    } catch (Exception e) {
                    }
                } catch (IOException e2) {
                    try {
                        logReadError(host, file, e2);
                    } finally {
                        try {
                            in.close();
                        } catch (Exception e3) {
                        }
                    }
                    return bArr;
                }
            } catch (FileNotFoundException e4) {
                logReadError(host, file, e4);
                return bArr;
            }
            return data;
        }

        static void logReadError(String host, File file, Throwable t) {
            Logger access$000 = FileClientSessionCache.logger;
            Level level = Level.WARNING;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FileClientSessionCache: Error reading session data for ");
            stringBuilder.append(host);
            stringBuilder.append(" from ");
            stringBuilder.append(file);
            stringBuilder.append(".");
            access$000.log(level, stringBuilder.toString(), t);
        }

        /* JADX WARNING: Exception block dominator not found, dom blocks: [B:26:0x0049, B:41:0x0067] */
        /* JADX WARNING: Missing block: B:24:0x0046, code skipped:
            r7 = th;
     */
        /* JADX WARNING: Missing block: B:33:0x0055, code skipped:
            if (1 != null) goto L_0x0057;
     */
        /* JADX WARNING: Missing block: B:34:0x0057, code skipped:
            if (null == null) goto L_0x0059;
     */
        /* JADX WARNING: Missing block: B:36:0x005a, code skipped:
            r9.accessOrder.put(r1, r2);
     */
        /* JADX WARNING: Missing block: B:37:0x0060, code skipped:
            delete(r2);
     */
        /* JADX WARNING: Missing block: B:67:?, code skipped:
            r4.close();
     */
        /* JADX WARNING: Missing block: B:72:?, code skipped:
            r8 = r9.accessOrder;
     */
        /* JADX WARNING: Missing block: B:73:0x00a5, code skipped:
            r8.put(r1, r2);
     */
        /* JADX WARNING: Missing block: B:74:0x00a9, code skipped:
            delete(r2);
     */
        /* JADX WARNING: Missing block: B:75:0x00ad, code skipped:
            r7 = th;
     */
        /* JADX WARNING: Missing block: B:76:0x00af, code skipped:
            r8 = move-exception;
     */
        /* JADX WARNING: Missing block: B:78:?, code skipped:
            logWriteError(r0, r2, r8);
     */
        /* JADX WARNING: Missing block: B:82:?, code skipped:
            r8 = r9.accessOrder;
     */
        /* JADX WARNING: Missing block: B:84:0x00bc, code skipped:
            if (r6 != false) goto L_0x00be;
     */
        /* JADX WARNING: Missing block: B:85:0x00be, code skipped:
            if (null == null) goto L_0x00c0;
     */
        /* JADX WARNING: Missing block: B:87:0x00c1, code skipped:
            r9.accessOrder.put(r1, r2);
     */
        /* JADX WARNING: Missing block: B:88:0x00c7, code skipped:
            delete(r2);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public synchronized void putSessionData(SSLSession session, byte[] sessionData) {
            String host = session.getPeerHost();
            if (sessionData != null) {
                String name = fileName(host, session.getPeerPort());
                File file = new File(this.directory, name);
                boolean existedBefore = file.exists();
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    if (!existedBefore) {
                        this.size++;
                        makeRoom();
                    }
                    boolean writeSuccessful = false;
                    Map map;
                    try {
                        out.write(sessionData);
                        try {
                            out.close();
                            if (1 != null && true) {
                                map = this.accessOrder;
                                map.put(name, file);
                            }
                        } catch (IOException e) {
                            logWriteError(host, file, e);
                            if (!(1 == null || null == null)) {
                                map = this.accessOrder;
                            }
                        }
                    } catch (IOException e2) {
                        logWriteError(host, file, e2);
                        try {
                            out.close();
                            if (writeSuccessful && true) {
                                map = this.accessOrder;
                                map.put(name, file);
                            }
                        } catch (IOException e22) {
                            logWriteError(host, file, e22);
                            if (writeSuccessful && null != null) {
                                map = this.accessOrder;
                                map.put(name, file);
                            }
                        } catch (Throwable th) {
                            Throwable th2 = th;
                            if (!writeSuccessful || null == null) {
                                delete(file);
                                throw th2;
                            } else {
                                this.accessOrder.put(name, file);
                                throw th2;
                            }
                        }
                    }
                    delete(file);
                } catch (FileNotFoundException e3) {
                    logWriteError(host, file, e3);
                    return;
                }
            }
            throw new NullPointerException("sessionData == null");
        }

        private void makeRoom() {
            if (this.size > 12) {
                indexFiles();
                int removals = this.size - 12;
                Iterator<File> i = this.accessOrder.values().iterator();
                do {
                    delete((File) i.next());
                    i.remove();
                    removals--;
                } while (removals > 0);
            }
        }

        private void indexFiles() {
            String[] initialFiles = this.initialFiles;
            if (initialFiles != null) {
                this.initialFiles = null;
                Set<CacheFile> diskOnly = new TreeSet();
                for (String name : initialFiles) {
                    if (!this.accessOrder.containsKey(name)) {
                        diskOnly.add(new CacheFile(this.directory, name));
                    }
                }
                if (!diskOnly.isEmpty()) {
                    Map<String, File> newOrder = newAccessOrder();
                    for (CacheFile cacheFile : diskOnly) {
                        newOrder.put(cacheFile.name, cacheFile);
                    }
                    newOrder.putAll(this.accessOrder);
                    this.accessOrder = newOrder;
                }
            }
        }

        private void delete(File file) {
            if (!file.delete()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("FileClientSessionCache: Failed to delete ");
                stringBuilder.append(file);
                stringBuilder.append(".");
                Exception e = new IOException(stringBuilder.toString());
                FileClientSessionCache.logger.log(Level.WARNING, e.getMessage(), e);
            }
            this.size--;
        }

        static void logWriteError(String host, File file, Throwable t) {
            Logger access$000 = FileClientSessionCache.logger;
            Level level = Level.WARNING;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FileClientSessionCache: Error writing session data for ");
            stringBuilder.append(host);
            stringBuilder.append(" to ");
            stringBuilder.append(file);
            stringBuilder.append(".");
            access$000.log(level, stringBuilder.toString(), t);
        }
    }

    private FileClientSessionCache() {
    }

    public static synchronized SSLClientSessionCache usingDirectory(File directory) throws IOException {
        Impl cache;
        synchronized (FileClientSessionCache.class) {
            cache = (Impl) caches.get(directory);
            if (cache == null) {
                cache = new Impl(directory);
                caches.put(directory, cache);
            }
        }
        return cache;
    }

    static synchronized void reset() {
        synchronized (FileClientSessionCache.class) {
            caches.clear();
        }
    }
}
