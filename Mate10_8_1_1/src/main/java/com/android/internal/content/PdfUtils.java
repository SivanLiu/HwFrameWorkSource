package com.android.internal.content;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.pdf.PdfRenderer;
import android.graphics.pdf.PdfRenderer.Page;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import libcore.io.IoUtils;
import libcore.io.Streams;

public final class PdfUtils {
    private PdfUtils() {
    }

    public static AssetFileDescriptor openPdfThumbnail(File file, Point size) throws IOException {
        final ParcelFileDescriptor pdfDescriptor = ParcelFileDescriptor.open(file, 268435456);
        Page frontPage = new PdfRenderer(pdfDescriptor).openPage(0);
        Bitmap thumbnail = Bitmap.createBitmap(size.x, size.y, Config.ARGB_8888);
        frontPage.render(thumbnail, null, null, 1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        thumbnail.compress(CompressFormat.PNG, 100, out);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final ParcelFileDescriptor[] fds = ParcelFileDescriptor.createReliablePipe();
        new AsyncTask<Object, Object, Object>() {
            protected Object doInBackground(Object... params) {
                try {
                    Streams.copy(in, new FileOutputStream(fds[1].getFileDescriptor()));
                    IoUtils.closeQuietly(fds[1]);
                    try {
                        pdfDescriptor.close();
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (IOException e2) {
                    throw new RuntimeException(e2);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[0]);
        pdfDescriptor.close();
        return new AssetFileDescriptor(fds[0], 0, -1);
    }
}
