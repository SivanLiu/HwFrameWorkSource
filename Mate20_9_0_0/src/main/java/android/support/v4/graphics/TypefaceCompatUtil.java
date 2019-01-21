package android.support.v4.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.support.annotation.Nullable;
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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

@RestrictTo({Scope.LIBRARY_GROUP})
public class TypefaceCompatUtil {
    private static final String CACHE_FILE_PREFIX = ".font";
    private static final String TAG = "TypefaceCompatUtil";

    private TypefaceCompatUtil() {
    }

    @Nullable
    public static File getTempFile(Context context) {
        String prefix = new StringBuilder();
        prefix.append(CACHE_FILE_PREFIX);
        prefix.append(Process.myPid());
        prefix.append("-");
        prefix.append(Process.myTid());
        prefix.append("-");
        prefix = prefix.toString();
        int i = 0;
        while (i < 100) {
            File cacheDir = context.getCacheDir();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append(i);
            File file = new File(cacheDir, stringBuilder.toString());
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

    @Nullable
    @RequiresApi(19)
    private static ByteBuffer mmap(File file) {
        Throwable th;
        Throwable th2;
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                FileChannel channel = fis.getChannel();
                FileChannel fileChannel = channel;
                MappedByteBuffer map = fileChannel.map(MapMode.READ_ONLY, 0, channel.size());
                fis.close();
                return map;
            } catch (Throwable th22) {
                Throwable th3 = th22;
                th22 = th;
                th = th3;
            }
            throw th;
            if (th22 != null) {
                try {
                    fis.close();
                } catch (Throwable th4) {
                    th22.addSuppressed(th4);
                }
            } else {
                fis.close();
            }
            throw th;
        } catch (IOException e) {
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:33:0x004f A:{Splitter:B:8:0x0014, ExcHandler: all (th java.lang.Throwable)} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:33:0x004f, code skipped:
            r3 = th;
     */
    /* JADX WARNING: Missing block: B:34:0x0050, code skipped:
            r4 = null;
     */
    /* JADX WARNING: Missing block: B:38:0x0054, code skipped:
            r4 = move-exception;
     */
    /* JADX WARNING: Missing block: B:39:0x0055, code skipped:
            r11 = r4;
            r4 = r3;
            r3 = r11;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Nullable
    @RequiresApi(19)
    public static ByteBuffer mmap(Context context, CancellationSignal cancellationSignal, Uri uri) {
        Throwable th;
        Throwable th2;
        try {
            FileInputStream fis;
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r", cancellationSignal);
            if (pfd == null) {
                if (pfd != null) {
                    pfd.close();
                }
                return null;
            }
            try {
                fis = new FileInputStream(pfd.getFileDescriptor());
                try {
                    FileChannel channel = fis.getChannel();
                    FileChannel fileChannel = channel;
                    MappedByteBuffer map = fileChannel.map(MapMode.READ_ONLY, 0, channel.size());
                    fis.close();
                    if (pfd != null) {
                        pfd.close();
                    }
                    return map;
                } catch (Throwable th22) {
                    Throwable th3 = th22;
                    th22 = th;
                    th = th3;
                }
            } catch (Throwable th4) {
            }
            if (th22 != null) {
                fis.close();
            } else {
                fis.close();
            }
            throw th;
            throw th;
            if (pfd != null) {
                if (th != null) {
                    try {
                        pfd.close();
                    } catch (Throwable th222) {
                        th.addSuppressed(th222);
                    }
                } else {
                    pfd.close();
                }
            }
            throw th;
            throw th;
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    @RequiresApi(19)
    public static ByteBuffer copyToDirectBuffer(Context context, Resources res, int id) {
        File tmpFile = getTempFile(context);
        ByteBuffer byteBuffer = null;
        if (tmpFile == null) {
            return null;
        }
        try {
            if (copyToFile(tmpFile, res, id)) {
                byteBuffer = mmap(tmpFile);
            }
            tmpFile.delete();
            return byteBuffer;
        } catch (Throwable th) {
            tmpFile.delete();
        }
    }

    public static boolean copyToFile(File file, InputStream is) {
        FileOutputStream os = null;
        boolean z = false;
        try {
            os = new FileOutputStream(file, false);
            byte[] buffer = new byte[1024];
            while (true) {
                int read = is.read(buffer);
                int readLen = read;
                if (read == -1) {
                    break;
                }
                os.write(buffer, 0, readLen);
            }
            z = true;
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error copying resource contents to temp file: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            closeQuietly(null);
        }
        closeQuietly(os);
        return z;
    }

    public static boolean copyToFile(File file, Resources res, int id) {
        InputStream is = null;
        try {
            is = res.openRawResource(id);
            boolean copyToFile = copyToFile(file, is);
            return copyToFile;
        } finally {
            closeQuietly(is);
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
