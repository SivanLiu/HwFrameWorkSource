package com.android.internal.os;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;
import android.util.Slog;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;

class ZygoteServer {
    private static final String ANDROID_SOCKET_PREFIX = "ANDROID_SOCKET_";
    public static final String TAG = "ZygoteServer";
    private boolean mCloseSocketFd;
    private boolean mIsForkChild;
    private LocalServerSocket mServerSocket;

    ZygoteServer() {
    }

    void setForkChild() {
        this.mIsForkChild = true;
    }

    void registerServerSocketFromEnv(String socketName) {
        if (this.mServerSocket == null) {
            String fullSocketName = new StringBuilder();
            fullSocketName.append(ANDROID_SOCKET_PREFIX);
            fullSocketName.append(socketName);
            fullSocketName = fullSocketName.toString();
            try {
                int fileDesc = Integer.parseInt(System.getenv(fullSocketName));
                try {
                    FileDescriptor fd = new FileDescriptor();
                    fd.setInt$(fileDesc);
                    this.mServerSocket = new LocalServerSocket(fd);
                    this.mCloseSocketFd = true;
                } catch (IOException ex) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error binding to local socket '");
                    stringBuilder.append(fileDesc);
                    stringBuilder.append("'");
                    throw new RuntimeException(stringBuilder.toString(), ex);
                }
            } catch (RuntimeException ex2) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(fullSocketName);
                stringBuilder2.append(" unset or invalid");
                throw new RuntimeException(stringBuilder2.toString(), ex2);
            }
        }
    }

    void registerServerSocketAtAbstractName(String socketName) {
        if (this.mServerSocket == null) {
            try {
                this.mServerSocket = new LocalServerSocket(socketName);
                this.mCloseSocketFd = false;
            } catch (IOException ex) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error binding to abstract socket '");
                stringBuilder.append(socketName);
                stringBuilder.append("'");
                throw new RuntimeException(stringBuilder.toString(), ex);
            }
        }
    }

    private ZygoteConnection acceptCommandPeer(String abiList) {
        try {
            return createNewConnection(this.mServerSocket.accept(), abiList);
        } catch (IOException ex) {
            throw new RuntimeException("IOException during accept()", ex);
        }
    }

    protected ZygoteConnection createNewConnection(LocalSocket socket, String abiList) throws IOException {
        return new ZygoteConnection(socket, abiList);
    }

    void closeServerSocket() {
        try {
            if (this.mServerSocket != null) {
                FileDescriptor fd = this.mServerSocket.getFileDescriptor();
                this.mServerSocket.close();
                if (fd != null && this.mCloseSocketFd) {
                    Os.close(fd);
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Zygote:  error closing sockets", ex);
        } catch (ErrnoException ex2) {
            Log.e(TAG, "Zygote:  error closing descriptor", ex2);
        }
        this.mServerSocket = null;
    }

    FileDescriptor getServerSocketFileDescriptor() {
        return this.mServerSocket.getFileDescriptor();
    }

    Runnable runSelectLoop(String abiList) {
        ArrayList<FileDescriptor> fds = new ArrayList();
        ArrayList<ZygoteConnection> peers = new ArrayList();
        fds.add(this.mServerSocket.getFileDescriptor());
        peers.add(null);
        while (true) {
            int i;
            StructPollfd[] pollFds = new StructPollfd[fds.size()];
            for (i = 0; i < pollFds.length; i++) {
                pollFds[i] = new StructPollfd();
                pollFds[i].fd = (FileDescriptor) fds.get(i);
                pollFds[i].events = (short) OsConstants.POLLIN;
            }
            try {
                Os.poll(pollFds, -1);
                for (i = pollFds.length - 1; i >= 0; i--) {
                    if ((pollFds[i].revents & OsConstants.POLLIN) != 0) {
                        ZygoteConnection newPeer;
                        if (i == 0) {
                            newPeer = acceptCommandPeer(abiList);
                            peers.add(newPeer);
                            fds.add(newPeer.getFileDesciptor());
                        } else {
                            try {
                                newPeer = (ZygoteConnection) peers.get(i);
                                Runnable command = newPeer.processOneCommand(this);
                                if (this.mIsForkChild) {
                                    if (command != null) {
                                        this.mIsForkChild = false;
                                        return command;
                                    }
                                    throw new IllegalStateException("command == null");
                                } else if (command == null) {
                                    if (newPeer.isClosedByPeer()) {
                                        newPeer.closeSocket();
                                        peers.remove(i);
                                        fds.remove(i);
                                    }
                                    this.mIsForkChild = false;
                                } else {
                                    throw new IllegalStateException("command != null");
                                }
                            } catch (Exception e) {
                                if (this.mIsForkChild) {
                                    Log.e(TAG, "Caught post-fork exception in child process.", e);
                                    throw e;
                                }
                                Slog.e(TAG, "Exception executing zygote command: ", e);
                                ((ZygoteConnection) peers.remove(i)).closeSocket();
                                fds.remove(i);
                            } catch (Throwable th) {
                                this.mIsForkChild = false;
                            }
                        }
                    }
                }
            } catch (ErrnoException ex) {
                throw new RuntimeException("poll failed", ex);
            }
        }
    }
}
