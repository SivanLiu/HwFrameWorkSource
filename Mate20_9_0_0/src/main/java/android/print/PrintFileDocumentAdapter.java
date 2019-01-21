package android.print;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.os.FileUtils;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.print.PrintDocumentAdapter.LayoutResultCallback;
import android.print.PrintDocumentAdapter.WriteResultCallback;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PrintFileDocumentAdapter extends PrintDocumentAdapter {
    private static final String LOG_TAG = "PrintedFileDocAdapter";
    private final Context mContext;
    private final PrintDocumentInfo mDocumentInfo;
    private final File mFile;
    private WriteFileAsyncTask mWriteFileAsyncTask;

    private final class WriteFileAsyncTask extends AsyncTask<Void, Void, Void> {
        private final CancellationSignal mCancellationSignal;
        private final ParcelFileDescriptor mDestination;
        private final WriteResultCallback mResultCallback;

        public WriteFileAsyncTask(ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
            this.mDestination = destination;
            this.mResultCallback = callback;
            this.mCancellationSignal = cancellationSignal;
            this.mCancellationSignal.setOnCancelListener(new OnCancelListener(PrintFileDocumentAdapter.this) {
                public void onCancel() {
                    WriteFileAsyncTask.this.cancel(true);
                }
            });
        }

        protected Void doInBackground(Void... params) {
            Throwable th;
            Throwable th2;
            Throwable th3;
            try {
                InputStream in = new FileInputStream(PrintFileDocumentAdapter.this.mFile);
                Throwable th4;
                try {
                    OutputStream out = new FileOutputStream(this.mDestination.getFileDescriptor());
                    try {
                        FileUtils.copy(in, out, null, this.mCancellationSignal);
                        $closeResource(null, out);
                        $closeResource(null, in);
                        return null;
                    } catch (Throwable th22) {
                        th3 = th22;
                        th22 = th;
                        th = th3;
                    }
                    $closeResource(th, in);
                    throw th4;
                    $closeResource(th22, out);
                    throw th;
                } catch (Throwable th5) {
                    th3 = th5;
                    th5 = th4;
                    th4 = th3;
                }
            } catch (OperationCanceledException e) {
            } catch (IOException e2) {
                Log.e(PrintFileDocumentAdapter.LOG_TAG, "Error writing data!", e2);
                this.mResultCallback.onWriteFailed(PrintFileDocumentAdapter.this.mContext.getString(17041430));
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

        protected void onPostExecute(Void result) {
            this.mResultCallback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        }

        protected void onCancelled(Void result) {
            this.mResultCallback.onWriteFailed(PrintFileDocumentAdapter.this.mContext.getString(17041429));
        }
    }

    public PrintFileDocumentAdapter(Context context, File file, PrintDocumentInfo documentInfo) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null!");
        } else if (documentInfo != null) {
            this.mContext = context;
            this.mFile = file;
            this.mDocumentInfo = documentInfo;
        } else {
            throw new IllegalArgumentException("documentInfo cannot be null!");
        }
    }

    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle metadata) {
        callback.onLayoutFinished(this.mDocumentInfo, false);
    }

    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
        this.mWriteFileAsyncTask = new WriteFileAsyncTask(destination, cancellationSignal, callback);
        this.mWriteFileAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }
}
