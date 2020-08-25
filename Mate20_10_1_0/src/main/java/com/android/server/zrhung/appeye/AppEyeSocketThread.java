package com.android.server.zrhung.appeye;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import android.zrhung.ZrHungData;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.zrhung.IZRHungService;
import com.android.server.zrhung.ZRHungService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import libcore.io.IoUtils;

public class AppEyeSocketThread extends Thread {
    private static final String SOCKET_NAME = "ZRHungServer";
    static final String TAG = "AppEyeSocketThread";
    private ArrayList<String> list;

    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00c5, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:?, code lost:
        android.util.Log.w(com.android.server.zrhung.appeye.AppEyeSocketThread.TAG, "errors while connecting socket: " + r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x00da, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x00db, code lost:
        android.util.Log.w(com.android.server.zrhung.appeye.AppEyeSocketThread.TAG, "errors while connecting socket: " + r0);
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00c5 A[ExcHandler: InterruptedException (r0v2 'e' java.lang.InterruptedException A[CUSTOM_DECLARE]), PHI: r3 
      PHI: (r3v3 'socket' android.net.LocalSocket) = (r3v0 'socket' android.net.LocalSocket), (r3v5 'socket' android.net.LocalSocket), (r3v5 'socket' android.net.LocalSocket), (r3v5 'socket' android.net.LocalSocket) binds: [B:1:0x0008, B:2:?, B:3:0x0017, B:4:?] A[DONT_GENERATE, DONT_INLINE], Splitter:B:1:0x0008] */
    private void connectServer() {
        IZRHungService zrHungService;
        LocalSocket socket = null;
        BufferedReader in = null;
        try {
            socket = new LocalSocket();
            LocalSocketAddress address = new LocalSocketAddress(SOCKET_NAME, LocalSocketAddress.Namespace.RESERVED);
            while (true) {
                socket.connect(address);
                Log.i(TAG, "socket connect OK");
                break;
            }
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                String result = in.readLine();
                if (result == null) {
                    break;
                } else if (result.equals("START")) {
                    this.list.clear();
                    this.list.add(result);
                } else if (!result.equals("END")) {
                    this.list.add(result);
                } else if (this.list.isEmpty()) {
                    this.list.clear();
                } else if (!this.list.get(0).equals("START")) {
                    Log.e(TAG, "START END not match");
                    this.list.clear();
                } else {
                    this.list.remove(0);
                    AppEyeMessage message = new AppEyeMessage();
                    if (message.parseMsg(this.list) == 0 && (zrHungService = ZRHungService.getInstance()) != null) {
                        ZrHungData beginData = new ZrHungData();
                        beginData.putString("eventtype", "socketrecover");
                        beginData.put("appeyemessage", message);
                        zrHungService.sendEvent(beginData);
                    }
                }
            }
            Log.w(TAG, "errors while receiving from socket: ZRHungServer");
        } catch (IOException e) {
            Thread.sleep(HwArbitrationDEFS.DelayTimeMillisA);
            Log.i(TAG, "connecting socket: ZRHungServer");
        } catch (InterruptedException e2) {
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            IoUtils.closeQuietly(socket);
            throw th;
        }
        IoUtils.closeQuietly(in);
        IoUtils.closeQuietly(socket);
    }

    public void run() {
        while (true) {
            connectServer();
        }
    }

    public AppEyeSocketThread() {
        this.list = null;
        this.list = new ArrayList<>();
    }
}
