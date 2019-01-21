package com.leisen.wallet.sdk.newhttp;

import android.util.Log;
import com.leisen.wallet.sdk.util.LogUtil;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;

public class AsyncHttpRequestX implements Runnable {
    private static final String TAG = "AsyncHttpRequestX";
    private boolean cancelIsNotified = false;
    private int executionCount;
    private boolean isCancelled = false;
    private boolean isFinished = false;
    private final String request;
    private final ResponseHandlerInterfaceX responseHandler;
    private final HttpURLConnection urlConnection;

    public AsyncHttpRequestX(HttpURLConnection conn, String request, ResponseHandlerInterfaceX responseHandler) {
        this.urlConnection = conn;
        this.request = request;
        this.responseHandler = responseHandler;
    }

    public void run() {
        if (!isCancelled()) {
            if (this.responseHandler != null) {
                this.responseHandler.sendStartMessage();
            }
            if (!isCancelled()) {
                try {
                    makeRequestWithRetries();
                } catch (Exception e) {
                    if (isCancelled() || this.responseHandler == null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("makeRequestWithRetries returned error, but handler is null");
                        stringBuilder.append(e);
                        Log.e("AsyncHttpRequest", stringBuilder.toString());
                    } else {
                        this.responseHandler.sendFailureMessage(0, null, e);
                    }
                }
                if (!isCancelled()) {
                    if (this.responseHandler != null) {
                        this.responseHandler.sendFinishMessage();
                    }
                    this.isFinished = true;
                }
            }
        }
    }

    private void makeRequestWithRetries() throws IOException {
        StringBuilder stringBuilder;
        boolean retry = true;
        int maxRetryCnt = 3;
        IOException cause = null;
        while (retry) {
            maxRetryCnt--;
            if (maxRetryCnt <= 0) {
                retry = false;
            }
            try {
                makeRequest();
                return;
            } catch (UnknownHostException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("UnknownHostException exception:");
                stringBuilder.append(e.getMessage());
                cause = new IOException(stringBuilder.toString());
            } catch (NullPointerException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("NPE in HttpClient: ");
                stringBuilder.append(e2.getMessage());
                cause = new IOException(stringBuilder.toString());
            } catch (IOException e3) {
                try {
                    if (!isCancelled()) {
                        cause = e3;
                    } else {
                        return;
                    }
                } catch (Exception e4) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unhandled exception: ");
                    stringBuilder.append(e4.getMessage());
                    cause = new IOException(stringBuilder.toString());
                }
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unhandled exception origin fcause");
        stringBuilder2.append(cause.getMessage());
        Log.e(TAG, stringBuilder2.toString());
        throw cause;
    }

    private void makeRequest() throws IOException {
        if (!isCancelled()) {
            LogUtil.e(TAG, "==>get response before");
            this.urlConnection.connect();
            DataOutputStream wr = new DataOutputStream(this.urlConnection.getOutputStream());
            wr.writeBytes(this.request);
            wr.flush();
            wr.close();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("==>get response after = ");
            stringBuilder.append(this.urlConnection.getResponseCode());
            LogUtil.e(str, stringBuilder.toString());
            if (!(isCancelled() || this.responseHandler == null)) {
                this.responseHandler.sendResponseMessage(this.urlConnection);
            }
        }
    }

    public boolean isCancelled() {
        if (this.isCancelled) {
            sendCancelNotification();
        }
        return this.isCancelled;
    }

    private synchronized void sendCancelNotification() {
        if (!(this.isFinished || !this.isCancelled || this.cancelIsNotified)) {
            this.cancelIsNotified = true;
            if (this.responseHandler != null) {
                this.responseHandler.sendCancelMessage();
            }
        }
    }

    public boolean isDone() {
        return isCancelled() || this.isFinished;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        this.isCancelled = true;
        this.urlConnection.disconnect();
        return isCancelled();
    }
}
