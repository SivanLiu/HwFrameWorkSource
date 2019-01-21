package com.huawei.android.pgmng.plug;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PGPlug implements Runnable, Callback {
    private static final int EVENT_DISPATCH_MESSAGE = 1000;
    private static final int MAX_TRY_COUNT = 10;
    private String TAG = "PGPlug";
    private Handler mCallbackHandler;
    private IPGPlugCallbacks mCallbacks;
    private String mSocket = "pg-socket";
    private int retries = 0;

    public PGPlug(IPGPlugCallbacks callbacks, String logTag) {
        this.mCallbacks = callbacks;
        if (logTag != null) {
            this.TAG = logTag;
        }
    }

    public void run() {
        if (this.mCallbacks == null) {
            Log.e(this.TAG, "client callback is null");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.TAG);
        stringBuilder.append(".CallbackHandler");
        HandlerThread thread = new HandlerThread(stringBuilder.toString());
        thread.start();
        this.mCallbackHandler = new Handler(thread.getLooper(), this);
        this.retries = 10;
        while (this.retries > 0) {
            try {
                listenToSocket();
            } catch (Exception e) {
                String str = this.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error in connect to PG, sleep 5s try again, retries =");
                stringBuilder2.append((10 - this.retries) + 1);
                Log.e(str, stringBuilder2.toString(), e);
                SystemClock.sleep(5000);
            }
            this.retries--;
        }
        this.mCallbacks.onConnectedTimeout();
    }

    public boolean handleMessage(Message msg) {
        if (msg.what == 1000) {
            String event = msg.obj;
            String str;
            StringBuilder stringBuilder;
            try {
                String[] token = event.split("\\|");
                if (!this.mCallbacks.onEvent(Integer.parseInt(token[0]), token[1])) {
                    str = this.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unhandled event ");
                    stringBuilder.append(event);
                    Log.w(str, stringBuilder.toString());
                }
            } catch (Exception e) {
                str = this.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error handling '");
                stringBuilder.append(event);
                stringBuilder.append("': ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
        return true;
    }

    private void listenToSocket() throws IOException {
        LocalSocket socket = null;
        BufferedReader in = null;
        try {
            socket = new LocalSocket();
            socket.connect(new LocalSocketAddress(this.mSocket, Namespace.ABSTRACT));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.mCallbacks.onDaemonConnected();
            this.retries = 10;
            while (true) {
                String readLine = in.readLine();
                String content = readLine;
                if (readLine == null) {
                    try {
                        in.close();
                        socket.close();
                        return;
                    } catch (IOException ex) {
                        Log.w(this.TAG, "Failed closing socket", ex);
                        throw ex;
                    }
                } else if (this.mCallbackHandler != null) {
                    this.mCallbackHandler.sendMessage(this.mCallbackHandler.obtainMessage(1000, content));
                }
            }
        } catch (IOException ex2) {
            Log.e(this.TAG, "Communications to PG-server error", ex2);
            throw ex2;
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex22) {
                    Log.w(this.TAG, "Failed closing socket", ex22);
                    throw ex22;
                }
            }
            if (socket != null) {
                socket.close();
            }
        }
    }
}
