package com.android.server.zrhung.appeye;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.util.Log;
import android.zrhung.ZrHungData;
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
    private static IZRHungService mZrHungService = null;
    private ArrayList<String> list;

    /* JADX WARNING: Removed duplicated region for block: B:32:0x00c9 A:{Splitter:B:1:0x0002, PHI: r0 , ExcHandler: InterruptedException (r2_3 'e' java.lang.InterruptedException)} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:29:0x00b9, code skipped:
            java.lang.Thread.sleep(com.android.server.hidata.arbitration.HwArbitrationDEFS.DelayTimeMillisA);
            android.util.Log.i(TAG, "connecting socket: ZRHungServer");
     */
    /* JADX WARNING: Missing block: B:32:0x00c9, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:34:?, code skipped:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("errors while connecting socket: ");
            r4.append(r2);
            android.util.Log.w(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:39:0x0100, code skipped:
            libcore.io.IoUtils.closeQuietly(null);
            libcore.io.IoUtils.closeQuietly(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void connectServer() {
        LocalSocket socket = null;
        BufferedReader in = null;
        String result;
        try {
            socket = new LocalSocket();
            LocalSocketAddress address = new LocalSocketAddress(SOCKET_NAME, Namespace.RESERVED);
            while (true) {
                socket.connect(address);
                Log.i(TAG, "socket connect OK");
                break;
            }
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                result = in.readLine();
                if (result == null) {
                    break;
                } else if (result.equals("START")) {
                    this.list.clear();
                    this.list.add(result);
                } else if (!result.equals("END")) {
                    this.list.add(result);
                } else if (this.list.isEmpty()) {
                    this.list.clear();
                } else if (((String) this.list.get(0)).equals("START")) {
                    this.list.remove(0);
                    AppEyeMessage message = new AppEyeMessage();
                    if (message.parseMsg(this.list) == 0) {
                        IZRHungService zrHungService = ZRHungService.getInstance();
                        if (zrHungService != null) {
                            ZrHungData beginData = new ZrHungData();
                            beginData.putString("eventtype", "socketrecover");
                            beginData.put("appeyemessage", message);
                            zrHungService.sendEvent(beginData);
                        }
                    }
                } else {
                    Log.e(TAG, "START END not match");
                    this.list.clear();
                }
            }
            Log.w(TAG, "errors while receiving from socket: ZRHungServer");
        } catch (IOException e) {
            result = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("errors while connecting socket: ");
            stringBuilder.append(e);
            Log.w(result, stringBuilder.toString());
        } catch (InterruptedException e2) {
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
        this.list = new ArrayList();
    }
}
