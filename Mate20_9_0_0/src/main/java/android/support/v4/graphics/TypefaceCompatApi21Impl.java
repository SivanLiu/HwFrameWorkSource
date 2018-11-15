package android.support.v4.graphics;

import android.content.Context;
import android.graphics.Typeface;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.provider.FontsContractCompat.FontInfo;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RequiresApi(21)
@RestrictTo({Scope.LIBRARY_GROUP})
class TypefaceCompatApi21Impl extends TypefaceCompatBaseImpl {
    private static final String TAG = "TypefaceCompatApi21Impl";

    TypefaceCompatApi21Impl() {
    }

    private File getFile(ParcelFileDescriptor fd) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/proc/self/fd/");
            stringBuilder.append(fd.getFd());
            String path = Os.readlink(stringBuilder.toString());
            if (OsConstants.S_ISREG(Os.stat(path).st_mode)) {
                return new File(path);
            }
            return null;
        } catch (ErrnoException e) {
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:41:0x005f A:{Splitter: B:6:0x001a, ExcHandler: all (th java.lang.Throwable)} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:41:0x005f, code:
            r4 = th;
     */
    /* JADX WARNING: Missing block: B:42:0x0060, code:
            r5 = null;
     */
    /* JADX WARNING: Missing block: B:46:0x0064, code:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:47:0x0065, code:
            r9 = r5;
            r5 = r4;
            r4 = r9;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Typeface createFromFontInfo(Context context, CancellationSignal cancellationSignal, @NonNull FontInfo[] fonts, int style) {
        ParcelFileDescriptor pfd;
        FileInputStream fis;
        Throwable th;
        Throwable th2;
        if (fonts.length < 1) {
            return null;
        }
        FontInfo bestFont = findBestInfo(fonts, style);
        try {
            pfd = context.getContentResolver().openFileDescriptor(bestFont.getUri(), "r", cancellationSignal);
            try {
                File file = getFile(pfd);
                if (file == null || !file.canRead()) {
                    fis = new FileInputStream(pfd.getFileDescriptor());
                    try {
                        Typeface createFromInputStream = super.createFromInputStream(context, fis);
                        fis.close();
                        if (pfd != null) {
                            pfd.close();
                        }
                        return createFromInputStream;
                    } catch (Throwable th22) {
                        Throwable th3 = th22;
                        th22 = th;
                        th = th3;
                    }
                } else {
                    Typeface createFromFile = Typeface.createFromFile(file);
                    if (pfd != null) {
                        pfd.close();
                    }
                    return createFromFile;
                }
            } catch (Throwable th4) {
            }
        } catch (IOException e) {
            return null;
        }
        if (th22 != null) {
            fis.close();
        } else {
            fis.close();
        }
        throw th;
        throw th;
        if (pfd != null) {
            if (r5 != null) {
                try {
                    pfd.close();
                } catch (Throwable th5) {
                    r5.addSuppressed(th5);
                }
            } else {
                pfd.close();
            }
        }
        throw th;
        throw th5;
    }
}
