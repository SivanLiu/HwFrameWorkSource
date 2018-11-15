package com.android.server.zrhung.appeye;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.util.Log;
import com.android.server.zrhung.IZRHungService;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import libcore.io.IoUtils;

public class AppEyeSocketThread extends Thread {
    private static final String SOCKET_NAME = "ZRHungServer";
    static final String TAG = "AppEyeSocketThread";
    private static IZRHungService mZrHungService = null;
    private ArrayList<String> list;

    /* JADX WARNING: Removed duplicated region for block: B:32:0x00c9 A:{PHI: r0 , ExcHandler: java.lang.InterruptedException (r2_3 'e' java.lang.InterruptedException), Splitter: B:1:0x0002} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:6:?, code:
            r1 = new java.io.BufferedReader(new java.io.InputStreamReader(r0.getInputStream()));
     */
    /* JADX WARNING: Missing block: B:7:0x002c, code:
            r3 = r1.readLine();
     */
    /* JADX WARNING: Missing block: B:8:0x0030, code:
            if (r3 != null) goto L_0x003b;
     */
    /* JADX WARNING: Missing block: B:9:0x0032, code:
            android.util.Log.w(TAG, "errors while receiving from socket: ZRHungServer");
     */
    /* JADX WARNING: Missing block: B:11:0x0041, code:
            if (r3.equals("START") == false) goto L_0x004e;
     */
    /* JADX WARNING: Missing block: B:12:0x0043, code:
            r11.list.clear();
            r11.list.add(r3);
     */
    /* JADX WARNING: Missing block: B:14:0x0054, code:
            if (r3.equals("END") == false) goto L_0x00b1;
     */
    /* JADX WARNING: Missing block: B:16:0x005c, code:
            if (r11.list.isEmpty() == false) goto L_0x0064;
     */
    /* JADX WARNING: Missing block: B:17:0x005e, code:
            r11.list.clear();
     */
    /* JADX WARNING: Missing block: B:19:0x0073, code:
            if (((java.lang.String) r11.list.get(0)).equals("START") != false) goto L_0x0082;
     */
    /* JADX WARNING: Missing block: B:20:0x0075, code:
            android.util.Log.e(TAG, "START END not match");
            r11.list.clear();
     */
    /* JADX WARNING: Missing block: B:21:0x0082, code:
            r11.list.remove(0);
            r5 = new com.android.server.zrhung.appeye.AppEyeMessage();
     */
    /* JADX WARNING: Missing block: B:22:0x0092, code:
            if (r5.parseMsg(r11.list) != 0) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:23:0x0094, code:
            r7 = com.android.server.zrhung.ZRHungService.getInstance();
     */
    /* JADX WARNING: Missing block: B:24:0x0098, code:
            if (r7 == null) goto L_0x00af;
     */
    /* JADX WARNING: Missing block: B:25:0x009a, code:
            r8 = new android.zrhung.ZrHungData();
            r8.putString("eventtype", "socketrecover");
            r8.put("appeyemessage", r5);
            r7.sendEvent(r8);
     */
    /* JADX WARNING: Missing block: B:27:0x00b1, code:
            r11.list.add(r3);
     */
    /* JADX WARNING: Missing block: B:29:0x00b9, code:
            java.lang.Thread.sleep(com.android.server.hidata.arbitration.HwArbitrationDEFS.DelayTimeMillisA);
            android.util.Log.i(TAG, "connecting socket: ZRHungServer");
     */
    /* JADX WARNING: Missing block: B:32:0x00c9, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:34:?, code:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("errors while connecting socket: ");
            r4.append(r2);
            android.util.Log.w(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:39:0x0100, code:
            libcore.io.IoUtils.closeQuietly(null);
            libcore.io.IoUtils.closeQuietly(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void connectServer() {
        LocalSocket socket = null;
        BufferedReader in = null;
        try {
            socket = new LocalSocket();
            LocalSocketAddress address = new LocalSocketAddress(SOCKET_NAME, Namespace.RESERVED);
            while (true) {
                socket.connect(address);
                Log.i(TAG, "socket connect OK");
                break;
            }
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("errors while connecting socket: ");
            stringBuilder.append(e);
            Log.w(str, stringBuilder.toString());
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
