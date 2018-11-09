package android.support.v4.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.os.Process;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.util.Log;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

@RestrictTo({Scope.LIBRARY_GROUP})
class TypefaceCompatUtil {
    private static final String CACHE_FILE_PREFIX = ".font";
    private static final String TAG = "TypefaceCompatUtil";

    private static class ByteBufferInputStream extends InputStream {
        private ByteBuffer mBuf;

        ByteBufferInputStream(ByteBuffer buf) {
            this.mBuf = buf;
        }

        public int read() {
            if (this.mBuf.hasRemaining()) {
                return this.mBuf.get() & 255;
            }
            return -1;
        }

        public int read(byte[] bytes, int off, int len) {
            if (!this.mBuf.hasRemaining()) {
                return -1;
            }
            len = Math.min(len, this.mBuf.remaining());
            this.mBuf.get(bytes, off, len);
            return len;
        }
    }

    private TypefaceCompatUtil() {
    }

    public static File getTempFile(Context context) {
        String prefix = CACHE_FILE_PREFIX + Process.myPid() + "-" + Process.myTid() + "-";
        int i = 0;
        while (i < 100) {
            File file = new File(context.getCacheDir(), prefix + i);
            try {
                if (file.createNewFile()) {
                    return file;
                }
                i++;
            } catch (IOException e) {
            }
        }
        return null;
    }

    @RequiresApi(19)
    private static ByteBuffer mmap(File file) {
        Throwable th;
        FileInputStream fileInputStream = null;
        Throwable th2;
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                FileChannel channel = fis.getChannel();
                ByteBuffer map = channel.map(MapMode.READ_ONLY, 0, channel.size());
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                th2 = null;
                if (th2 == null) {
                    return map;
                }
                try {
                    throw th2;
                } catch (IOException e) {
                    fileInputStream = fis;
                }
            } catch (Throwable th4) {
                th2 = th4;
                fileInputStream = fis;
                th = null;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (Throwable th5) {
                        if (th == null) {
                            th = th5;
                        } else if (th != th5) {
                            th.addSuppressed(th5);
                        }
                    }
                }
                if (th == null) {
                    throw th2;
                }
                try {
                    throw th;
                } catch (IOException e2) {
                    return null;
                }
            }
        } catch (Throwable th6) {
            th2 = th6;
            th = null;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (th == null) {
                throw th;
            }
            throw th2;
        }
    }

    @RequiresApi(19)
    public static ByteBuffer copyToDirectBuffer(Context context, Resources res, int id) {
        File tmpFile = getTempFile(context);
        if (tmpFile == null) {
            return null;
        }
        try {
            if (!copyToFile(tmpFile, res, id)) {
                return null;
            }
            ByteBuffer mmap = mmap(tmpFile);
            tmpFile.delete();
            return mmap;
        } finally {
            tmpFile.delete();
        }
    }

    public static boolean copyToFile(File file, ByteBuffer buffer) {
        return copyToFile(file, new ByteBufferInputStream(buffer));
    }

    public static boolean copyToFile(File file, InputStream is) {
        IOException e;
        Throwable th;
        Closeable closeable = null;
        try {
            FileOutputStream os = new FileOutputStream(file, false);
            try {
                byte[] buffer = new byte[1024];
                while (true) {
                    int readLen = is.read(buffer);
                    if (readLen != -1) {
                        os.write(buffer, 0, readLen);
                    } else {
                        closeQuietly(os);
                        return true;
                    }
                }
            } catch (IOException e2) {
                e = e2;
                closeable = os;
                try {
                    Log.e(TAG, "Error copying resource contents to temp file: " + e.getMessage());
                    closeQuietly(closeable);
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    closeQuietly(closeable);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                Object os2 = os;
                closeQuietly(closeable);
                throw th;
            }
        } catch (IOException e3) {
            e = e3;
            Log.e(TAG, "Error copying resource contents to temp file: " + e.getMessage());
            closeQuietly(closeable);
            return false;
        }
    }

    public static boolean copyToFile(File file, Resources res, int id) {
        Closeable closeable = null;
        try {
            closeable = res.openRawResource(id);
            boolean copyToFile = copyToFile(file, (InputStream) closeable);
            return copyToFile;
        } finally {
            closeQuietly(closeable);
        }
    }

    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }
}
