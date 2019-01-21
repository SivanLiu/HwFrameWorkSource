package com.huawei.kvdb;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import java.util.Hashtable;

public class HwKVDatabase {
    public static final int IMAGE = 1;
    private static final long INITIALCRC = -1;
    private static final long POLY64REV = -7661587058870466123L;
    public static final int VIDEO = 3;
    private static HwKVDatabase database = null;
    private static long[] sCrcTable = new long[256];
    private HwKVConnectionPool kvConnectionPool;

    static {
        for (int i = 0; i < 256; i++) {
            long part = (long) i;
            for (int j = 0; j < 8; j++) {
                part = (part >> 1) ^ ((((int) part) & 1) != 0 ? POLY64REV : 0);
            }
            sCrcTable[i] = part;
        }
    }

    private static byte[] getBytes(String path) {
        byte[] result = new byte[(path.length() * 2)];
        int output = 0;
        for (char ch : path.toCharArray()) {
            int output2 = output + 1;
            result[output] = (byte) (ch & 255);
            output = output2 + 1;
            result[output2] = (byte) (ch >> 8);
        }
        return result;
    }

    private static long crc64Long(byte[] buffer) {
        long crc = INITIALCRC;
        for (byte b : buffer) {
            crc = sCrcTable[(((int) crc) ^ b) & 255] ^ (crc >> 8);
        }
        return crc;
    }

    public static long generateKey(int id, long timeModified, int mediaType, int type) {
        String path;
        if (mediaType == 1) {
            path = new StringBuilder();
            path.append("/local/image/item/");
            path.append(id);
            path = path.toString();
        } else if (mediaType != 3) {
            return 0;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/local/video/item/");
            stringBuilder.append(id);
            path = stringBuilder.toString();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(path);
        stringBuilder2.append("+");
        stringBuilder2.append(timeModified);
        stringBuilder2.append("+");
        stringBuilder2.append(type);
        return crc64Long(getBytes(stringBuilder2.toString()));
    }

    private HwKVDatabase(Context context) {
        this.kvConnectionPool = new HwKVConnectionPool(context);
    }

    public static synchronized HwKVDatabase getInstance(Context context) {
        HwKVDatabase hwKVDatabase;
        synchronized (HwKVDatabase.class) {
            if (database == null) {
                database = new HwKVDatabase(context);
            }
            hwKVDatabase = database;
        }
        return hwKVDatabase;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        this.kvConnectionPool.closeConnection();
    }

    /* JADX WARNING: Missing block: B:12:0x001a, code skipped:
            if (null == null) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:13:0x001c, code skipped:
            r6.kvConnectionPool.releaseWriteConnection(r2);
     */
    /* JADX WARNING: Missing block: B:22:0x0034, code skipped:
            if (true != false) goto L_0x0037;
     */
    /* JADX WARNING: Missing block: B:23:0x0037, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean put(long key, byte[] value, int size) throws HwKVFullException {
        if (value == null || value.length == 0 || value.length != size) {
            return false;
        }
        HwKVConnection kvConnection = this.kvConnectionPool.getWriteConnection();
        if (kvConnection == null) {
            return false;
        }
        boolean res;
        try {
            res = kvConnection.put(key, value, size);
        } catch (HwKVDatabaseDeleteException e) {
            this.kvConnectionPool.closeExceptionConnection(kvConnection, true);
            res = false;
        } catch (HwKVFullException e2) {
            throw new HwKVFullException();
        } catch (Throwable th) {
            if (null == null) {
                this.kvConnectionPool.releaseWriteConnection(kvConnection);
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0011, code skipped:
            if (null == null) goto L_0x0013;
     */
    /* JADX WARNING: Missing block: B:7:0x0013, code skipped:
            r6.kvConnectionPool.releaseWriteConnection(r1);
     */
    /* JADX WARNING: Missing block: B:16:0x002b, code skipped:
            if (true != false) goto L_0x002e;
     */
    /* JADX WARNING: Missing block: B:17:0x002e, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean remove(long key) throws HwKVFullException {
        HwKVConnection kvConnection = this.kvConnectionPool.getWriteConnection();
        if (kvConnection == null) {
            return false;
        }
        boolean res;
        try {
            res = kvConnection.remove(key);
        } catch (HwKVDatabaseDeleteException e) {
            this.kvConnectionPool.closeExceptionConnection(kvConnection, true);
            res = false;
        } catch (HwKVFullException e2) {
            throw new HwKVFullException();
        } catch (Throwable th) {
            if (null == null) {
                this.kvConnectionPool.releaseWriteConnection(kvConnection);
            }
        }
    }

    public Bitmap getBitmap(long key, Options options) {
        HwKVConnection kvConnection = this.kvConnectionPool.getReadOnlyConnection();
        Bitmap bitmap = null;
        if (kvConnection == null) {
            return null;
        }
        HwKVData res;
        boolean openConnection = true;
        try {
            res = kvConnection.get(key);
        } catch (HwKVDatabaseDeleteException e) {
            res = null;
            this.kvConnectionPool.closeExceptionConnection(kvConnection, false);
            openConnection = false;
        }
        if (res != null) {
            try {
                bitmap = BitmapFactory.decodeByteArray(res.value, 0, res.size, options);
            } catch (Throwable th) {
                if (openConnection) {
                    this.kvConnectionPool.releaseReadOnlyConnection(kvConnection);
                }
            }
        }
        if (openConnection) {
            this.kvConnectionPool.releaseReadOnlyConnection(kvConnection);
        }
        return bitmap;
    }

    /* JADX WARNING: Missing block: B:7:0x0011, code skipped:
            if (r3 == false) goto L_0x0013;
     */
    /* JADX WARNING: Missing block: B:8:0x0013, code skipped:
            r6.kvConnectionPool.releaseReadOnlyConnection(r0);
     */
    /* JADX WARNING: Missing block: B:14:0x0023, code skipped:
            if (true != false) goto L_0x0026;
     */
    /* JADX WARNING: Missing block: B:15:0x0026, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean hasKey(long key) {
        HwKVConnection kvConnection = this.kvConnectionPool.getReadOnlyConnection();
        if (kvConnection == null) {
            return false;
        }
        boolean connectionException = false;
        boolean res;
        try {
            res = kvConnection.hasKey(key);
        } catch (HwKVDatabaseDeleteException e) {
            this.kvConnectionPool.closeExceptionConnection(kvConnection, false);
            res = false;
        } catch (Throwable th) {
            if (!connectionException) {
                this.kvConnectionPool.releaseReadOnlyConnection(kvConnection);
            }
        }
    }

    public HwKVConnection getKVConnection() {
        return this.kvConnectionPool.getReadOnlyConnection();
    }

    public void releaseKVConnection(HwKVConnection kvConnection, boolean isDatabaseExist) {
        if (isDatabaseExist) {
            this.kvConnectionPool.releaseReadOnlyConnection(kvConnection);
        } else {
            this.kvConnectionPool.closeExceptionConnection(kvConnection, false);
        }
    }

    public int getKeyNum() {
        HwKVConnection kvConnection = this.kvConnectionPool.getReadOnlyConnection();
        if (kvConnection == null) {
            return 0;
        }
        int res;
        boolean connectionException = false;
        try {
            res = kvConnection.getKeyNum();
            if (!connectionException) {
                this.kvConnectionPool.releaseReadOnlyConnection(kvConnection);
            }
        } catch (HwKVDatabaseDeleteException e) {
            this.kvConnectionPool.closeExceptionConnection(kvConnection, false);
            if (!true) {
                this.kvConnectionPool.releaseReadOnlyConnection(kvConnection);
            }
            res = 0;
        } catch (Throwable th) {
            if (!connectionException) {
                this.kvConnectionPool.releaseReadOnlyConnection(kvConnection);
            }
            throw th;
        }
        return res;
    }

    /* JADX WARNING: Missing block: B:8:0x0012, code skipped:
            if (r3 == false) goto L_0x0014;
     */
    /* JADX WARNING: Missing block: B:9:0x0014, code skipped:
            r6.kvConnectionPool.releaseReadOnlyConnection(r0);
     */
    /* JADX WARNING: Missing block: B:16:0x0024, code skipped:
            if (true != false) goto L_0x0027;
     */
    /* JADX WARNING: Missing block: B:17:0x0027, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Hashtable<Long, Long> getAllKeys() {
        HwKVConnection kvConnection = this.kvConnectionPool.getReadOnlyConnection();
        if (kvConnection == null) {
            return null;
        }
        boolean connectionException = false;
        Hashtable<Long, Long> hashTable;
        try {
            hashTable = kvConnection.getAllKeys();
        } catch (HwKVDatabaseDeleteException e) {
            hashTable = null;
            this.kvConnectionPool.closeExceptionConnection(kvConnection, false);
        } catch (Throwable th) {
            if (!connectionException) {
                this.kvConnectionPool.releaseReadOnlyConnection(kvConnection);
            }
        }
    }

    public HwKVConnectionPool getKVConnectionPool() {
        return this.kvConnectionPool;
    }
}
