package android.support.v4.content;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.OperationCanceledException;
import android.support.v4.os.CancellationSignal;

public final class ContentResolverCompat {
    private ContentResolverCompat() {
    }

    public static Cursor query(ContentResolver resolver, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        Exception e;
        if (VERSION.SDK_INT >= 16) {
            if (cancellationSignal != null) {
                try {
                    e = cancellationSignal.getCancellationSignalObject();
                } catch (Exception e2) {
                    if (e2 instanceof OperationCanceledException) {
                        throw new android.support.v4.os.OperationCanceledException();
                    }
                    throw e2;
                }
            }
            e2 = null;
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder, (android.os.CancellationSignal) e2);
        }
        if (cancellationSignal != null) {
            cancellationSignal.throwIfCanceled();
        }
        return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
    }
}
