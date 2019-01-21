package com.android.server;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.os.IRecoverySystem.Stub;
import android.os.IRecoverySystemProgressListener;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import libcore.io.IoUtils;

public final class RecoverySystemService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String INIT_SERVICE_CLEAR_BCB = "init.svc.clear-bcb";
    private static final String INIT_SERVICE_SETUP_BCB = "init.svc.setup-bcb";
    private static final String INIT_SERVICE_UNCRYPT = "init.svc.uncrypt";
    private static final int SOCKET_CONNECTION_MAX_RETRY = 30;
    private static final String TAG = "RecoverySystemService";
    private static final String UNCRYPT_SOCKET = "uncrypt";
    private static final Object sRequestLock = new Object();
    private Context mContext;

    private final class BinderService extends Stub {
        private BinderService() {
        }

        /* JADX WARNING: Missing block: B:47:?, code skipped:
            r6 = com.android.server.RecoverySystemService.TAG;
            r9 = new java.lang.StringBuilder();
            r9.append("uncrypt failed with status: ");
            r9.append(r8);
            android.util.Slog.e(r6, r9.toString());
            r3.writeInt(0);
     */
        /* JADX WARNING: Missing block: B:49:?, code skipped:
            libcore.io.IoUtils.closeQuietly(r5);
            libcore.io.IoUtils.closeQuietly(r3);
            libcore.io.IoUtils.closeQuietly(r4);
     */
        /* JADX WARNING: Missing block: B:51:0x00ec, code skipped:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean uncrypt(String filename, IRecoverySystemProgressListener listener) {
            synchronized (RecoverySystemService.sRequestLock) {
                DataOutputStream dos;
                FileWriter uncryptFile;
                DataInputStream dis;
                try {
                    dos = null;
                    RecoverySystemService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
                    if (checkAndWaitForUncryptService()) {
                        RecoverySystem.UNCRYPT_PACKAGE_FILE.delete();
                        StringBuilder stringBuilder;
                        try {
                            uncryptFile = new FileWriter(RecoverySystem.UNCRYPT_PACKAGE_FILE);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(filename);
                            stringBuilder.append("\n");
                            uncryptFile.write(stringBuilder.toString());
                            uncryptFile.close();
                            SystemProperties.set("ctl.start", RecoverySystemService.UNCRYPT_SOCKET);
                            uncryptFile = connectService();
                            if (uncryptFile == null) {
                                Slog.e(RecoverySystemService.TAG, "Failed to connect to uncrypt socket");
                                return false;
                            }
                            dis = null;
                            try {
                                dis = new DataInputStream(uncryptFile.getInputStream());
                                dos = new DataOutputStream(uncryptFile.getOutputStream());
                                int lastStatus = Integer.MIN_VALUE;
                                while (true) {
                                    int status = dis.readInt();
                                    if (status != lastStatus || lastStatus == Integer.MIN_VALUE) {
                                        lastStatus = status;
                                        if (status < 0 || status > 100) {
                                            break;
                                        }
                                        String str = RecoverySystemService.TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("uncrypt read status: ");
                                        stringBuilder2.append(status);
                                        Slog.i(str, stringBuilder2.toString());
                                        if (listener != null) {
                                            try {
                                                listener.onProgress(status);
                                            } catch (RemoteException e) {
                                                Slog.w(RecoverySystemService.TAG, "RemoteException when posting progress");
                                            }
                                        }
                                        if (status == 100) {
                                            Slog.i(RecoverySystemService.TAG, "uncrypt successfully finished.");
                                            dos.writeInt(0);
                                            IoUtils.closeQuietly(dis);
                                            IoUtils.closeQuietly(dos);
                                            IoUtils.closeQuietly(uncryptFile);
                                            return true;
                                        }
                                    }
                                }
                            } catch (IOException e2) {
                                Slog.e(RecoverySystemService.TAG, "IOException when reading status: ", e2);
                                IoUtils.closeQuietly(dis);
                                IoUtils.closeQuietly(dos);
                                IoUtils.closeQuietly(uncryptFile);
                                return false;
                            }
                        } catch (IOException e3) {
                            String str2 = RecoverySystemService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("IOException when writing \"");
                            stringBuilder.append(RecoverySystem.UNCRYPT_PACKAGE_FILE);
                            stringBuilder.append("\":");
                            Slog.e(str2, stringBuilder.toString(), e3);
                            return false;
                        } catch (Throwable th) {
                            th.addSuppressed(th);
                        }
                    }
                    Slog.e(RecoverySystemService.TAG, "uncrypt service is unavailable.");
                    return false;
                } catch (Throwable th2) {
                    throw th2;
                }
            }
            throw th;
        }

        public boolean clearBcb() {
            boolean z;
            synchronized (RecoverySystemService.sRequestLock) {
                z = setupOrClearBcb(false, null);
            }
            return z;
        }

        public boolean setupBcb(String command) {
            boolean z;
            synchronized (RecoverySystemService.sRequestLock) {
                z = setupOrClearBcb(true, command);
            }
            return z;
        }

        public void rebootRecoveryWithCommand(String command) {
            synchronized (RecoverySystemService.sRequestLock) {
                if (setupOrClearBcb(true, command)) {
                    ((PowerManager) RecoverySystemService.this.mContext.getSystemService("power")).reboot("recovery");
                    return;
                }
            }
        }

        private boolean checkAndWaitForUncryptService() {
            for (int retry = 0; retry < 30; retry++) {
                boolean busy = "running".equals(SystemProperties.get(RecoverySystemService.INIT_SERVICE_UNCRYPT)) || "running".equals(SystemProperties.get(RecoverySystemService.INIT_SERVICE_SETUP_BCB)) || "running".equals(SystemProperties.get(RecoverySystemService.INIT_SERVICE_CLEAR_BCB));
                if (!busy) {
                    return true;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Slog.w(RecoverySystemService.TAG, "Interrupted:", e);
                }
            }
            return false;
        }

        private LocalSocket connectService() {
            LocalSocket socket = new LocalSocket();
            boolean done = false;
            int retry = 0;
            while (retry < 30) {
                try {
                    socket.connect(new LocalSocketAddress(RecoverySystemService.UNCRYPT_SOCKET, Namespace.RESERVED));
                    done = true;
                    break;
                } catch (IOException e) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e2) {
                        Slog.w(RecoverySystemService.TAG, "Interrupted:", e2);
                    }
                    retry++;
                }
            }
            if (done) {
                return socket;
            }
            Slog.e(RecoverySystemService.TAG, "Timed out connecting to uncrypt socket");
            return null;
        }

        private boolean setupOrClearBcb(boolean isSetup, String command) {
            DataOutputStream dos = null;
            RecoverySystemService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
            if (checkAndWaitForUncryptService()) {
                if (isSetup) {
                    SystemProperties.set("ctl.start", "setup-bcb");
                } else {
                    SystemProperties.set("ctl.start", "clear-bcb");
                }
                LocalSocket socket = connectService();
                if (socket == null) {
                    Slog.e(RecoverySystemService.TAG, "Failed to connect to uncrypt socket");
                    return false;
                }
                DataInputStream dis = null;
                try {
                    dis = new DataInputStream(socket.getInputStream());
                    dos = new DataOutputStream(socket.getOutputStream());
                    if (isSetup) {
                        byte[] cmdUtf8 = command.getBytes("UTF-8");
                        dos.writeInt(cmdUtf8.length);
                        dos.write(cmdUtf8, 0, cmdUtf8.length);
                        dos.flush();
                    }
                    int status = dis.readInt();
                    dos.writeInt(0);
                    String str;
                    StringBuilder stringBuilder;
                    if (status == 100) {
                        str = RecoverySystemService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("uncrypt ");
                        stringBuilder.append(isSetup ? "setup" : "clear");
                        stringBuilder.append(" bcb successfully finished.");
                        Slog.i(str, stringBuilder.toString());
                        return true;
                    }
                    str = RecoverySystemService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("uncrypt failed with status: ");
                    stringBuilder.append(status);
                    Slog.e(str, stringBuilder.toString());
                    IoUtils.closeQuietly(dis);
                    IoUtils.closeQuietly(dos);
                    IoUtils.closeQuietly(socket);
                    return false;
                } catch (IOException e) {
                    Slog.e(RecoverySystemService.TAG, "IOException when communicating with uncrypt:", e);
                    return false;
                } finally {
                    IoUtils.closeQuietly(dis);
                    IoUtils.closeQuietly(dos);
                    IoUtils.closeQuietly(socket);
                }
            } else {
                Slog.e(RecoverySystemService.TAG, "uncrypt service is unavailable.");
                return false;
            }
        }
    }

    public RecoverySystemService(Context context) {
        super(context);
        this.mContext = context;
    }

    public void onStart() {
        publishBinderService("recovery", new BinderService());
    }
}
