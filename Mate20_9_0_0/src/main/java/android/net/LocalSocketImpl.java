package android.net;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.bluetooth.BluetoothHidHost;
import android.system.ErrnoException;
import android.system.Int32Ref;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructLinger;
import android.system.StructTimeval;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class LocalSocketImpl {
    private FileDescriptor fd;
    private SocketInputStream fis;
    private SocketOutputStream fos;
    FileDescriptor[] inboundFileDescriptors;
    private boolean mFdCreatedInternally;
    FileDescriptor[] outboundFileDescriptors;
    private Object readMonitor = new Object();
    private Object writeMonitor = new Object();

    class SocketInputStream extends InputStream {
        SocketInputStream() {
        }

        public int available() throws IOException {
            FileDescriptor myFd = LocalSocketImpl.this.fd;
            if (myFd != null) {
                Int32Ref avail = new Int32Ref(0);
                try {
                    Os.ioctlInt(myFd, OsConstants.FIONREAD, avail);
                    return avail.value;
                } catch (ErrnoException e) {
                    throw e.rethrowAsIOException();
                }
            }
            throw new IOException("socket closed");
        }

        public void close() throws IOException {
            LocalSocketImpl.this.close();
        }

        public int read() throws IOException {
            int ret;
            synchronized (LocalSocketImpl.this.readMonitor) {
                FileDescriptor myFd = LocalSocketImpl.this.fd;
                if (myFd != null) {
                    ret = LocalSocketImpl.this.read_native(myFd);
                } else {
                    throw new IOException("socket closed");
                }
            }
            return ret;
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            int ret;
            synchronized (LocalSocketImpl.this.readMonitor) {
                FileDescriptor myFd = LocalSocketImpl.this.fd;
                if (myFd == null) {
                    throw new IOException("socket closed");
                } else if (off < 0 || len < 0 || off + len > b.length) {
                    throw new ArrayIndexOutOfBoundsException();
                } else {
                    ret = LocalSocketImpl.this.readba_native(b, off, len, myFd);
                }
            }
            return ret;
        }
    }

    class SocketOutputStream extends OutputStream {
        SocketOutputStream() {
        }

        public void close() throws IOException {
            LocalSocketImpl.this.close();
        }

        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            synchronized (LocalSocketImpl.this.writeMonitor) {
                FileDescriptor myFd = LocalSocketImpl.this.fd;
                if (myFd == null) {
                    throw new IOException("socket closed");
                } else if (off < 0 || len < 0 || off + len > b.length) {
                    throw new ArrayIndexOutOfBoundsException();
                } else {
                    LocalSocketImpl.this.writeba_native(b, off, len, myFd);
                }
            }
        }

        public void write(int b) throws IOException {
            synchronized (LocalSocketImpl.this.writeMonitor) {
                FileDescriptor myFd = LocalSocketImpl.this.fd;
                if (myFd != null) {
                    LocalSocketImpl.this.write_native(b, myFd);
                } else {
                    throw new IOException("socket closed");
                }
            }
        }

        public void flush() throws IOException {
            FileDescriptor myFd = LocalSocketImpl.this.fd;
            if (myFd != null) {
                Int32Ref pending = new Int32Ref(0);
                while (true) {
                    try {
                        Os.ioctlInt(myFd, OsConstants.TIOCOUTQ, pending);
                        if (pending.value > 0) {
                            int left = pending.value;
                            if (left <= 1000) {
                                try {
                                    Thread.sleep(0, 10);
                                } catch (InterruptedException e) {
                                    return;
                                }
                            } else if (left <= BluetoothHidHost.INPUT_DISCONNECT_FAILED_NOT_CONNECTED) {
                                Thread.sleep(0, RunningAppProcessInfo.IMPORTANCE_EMPTY);
                            } else {
                                Thread.sleep(1);
                            }
                        } else {
                            return;
                        }
                    } catch (ErrnoException e2) {
                        throw e2.rethrowAsIOException();
                    }
                }
            }
            throw new IOException("socket closed");
        }
    }

    private native void bindLocal(FileDescriptor fileDescriptor, String str, int i) throws IOException;

    private native void connectLocal(FileDescriptor fileDescriptor, String str, int i) throws IOException;

    private native Credentials getPeerCredentials_native(FileDescriptor fileDescriptor) throws IOException;

    private native int read_native(FileDescriptor fileDescriptor) throws IOException;

    private native int readba_native(byte[] bArr, int i, int i2, FileDescriptor fileDescriptor) throws IOException;

    private native void write_native(int i, FileDescriptor fileDescriptor) throws IOException;

    private native void writeba_native(byte[] bArr, int i, int i2, FileDescriptor fileDescriptor) throws IOException;

    LocalSocketImpl() {
    }

    LocalSocketImpl(FileDescriptor fd) {
        this.fd = fd;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append(" fd:");
        stringBuilder.append(this.fd);
        return stringBuilder.toString();
    }

    public void create(int sockType) throws IOException {
        if (this.fd == null) {
            int osType;
            switch (sockType) {
                case 1:
                    osType = OsConstants.SOCK_DGRAM;
                    break;
                case 2:
                    osType = OsConstants.SOCK_STREAM;
                    break;
                case 3:
                    osType = OsConstants.SOCK_SEQPACKET;
                    break;
                default:
                    throw new IllegalStateException("unknown sockType");
            }
            try {
                this.fd = Os.socket(OsConstants.AF_UNIX, osType, 0);
                this.mFdCreatedInternally = true;
                return;
            } catch (ErrnoException e) {
                e.rethrowAsIOException();
                return;
            }
        }
        throw new IOException("LocalSocketImpl already has an fd");
    }

    public void close() throws IOException {
        synchronized (this) {
            if (this.fd == null || !this.mFdCreatedInternally) {
                this.fd = null;
                return;
            }
            try {
                Os.close(this.fd);
            } catch (ErrnoException e) {
                e.rethrowAsIOException();
            }
            this.fd = null;
        }
    }

    protected void connect(LocalSocketAddress address, int timeout) throws IOException {
        if (this.fd != null) {
            connectLocal(this.fd, address.getName(), address.getNamespace().getId());
            return;
        }
        throw new IOException("socket not created");
    }

    public void bind(LocalSocketAddress endpoint) throws IOException {
        if (this.fd != null) {
            bindLocal(this.fd, endpoint.getName(), endpoint.getNamespace().getId());
            return;
        }
        throw new IOException("socket not created");
    }

    protected void listen(int backlog) throws IOException {
        if (this.fd != null) {
            try {
                Os.listen(this.fd, backlog);
                return;
            } catch (ErrnoException e) {
                throw e.rethrowAsIOException();
            }
        }
        throw new IOException("socket not created");
    }

    protected void accept(LocalSocketImpl s) throws IOException {
        if (this.fd != null) {
            try {
                s.fd = Os.accept(this.fd, null);
                s.mFdCreatedInternally = true;
                return;
            } catch (ErrnoException e) {
                throw e.rethrowAsIOException();
            }
        }
        throw new IOException("socket not created");
    }

    protected InputStream getInputStream() throws IOException {
        if (this.fd != null) {
            SocketInputStream socketInputStream;
            synchronized (this) {
                if (this.fis == null) {
                    this.fis = new SocketInputStream();
                }
                socketInputStream = this.fis;
            }
            return socketInputStream;
        }
        throw new IOException("socket not created");
    }

    protected OutputStream getOutputStream() throws IOException {
        if (this.fd != null) {
            SocketOutputStream socketOutputStream;
            synchronized (this) {
                if (this.fos == null) {
                    this.fos = new SocketOutputStream();
                }
                socketOutputStream = this.fos;
            }
            return socketOutputStream;
        }
        throw new IOException("socket not created");
    }

    protected int available() throws IOException {
        return getInputStream().available();
    }

    protected void shutdownInput() throws IOException {
        if (this.fd != null) {
            try {
                Os.shutdown(this.fd, OsConstants.SHUT_RD);
                return;
            } catch (ErrnoException e) {
                throw e.rethrowAsIOException();
            }
        }
        throw new IOException("socket not created");
    }

    protected void shutdownOutput() throws IOException {
        if (this.fd != null) {
            try {
                Os.shutdown(this.fd, OsConstants.SHUT_WR);
                return;
            } catch (ErrnoException e) {
                throw e.rethrowAsIOException();
            }
        }
        throw new IOException("socket not created");
    }

    protected FileDescriptor getFileDescriptor() {
        return this.fd;
    }

    protected boolean supportsUrgentData() {
        return false;
    }

    protected void sendUrgentData(int data) throws IOException {
        throw new RuntimeException("not impled");
    }

    public Object getOption(int optID) throws IOException {
        if (this.fd != null) {
            ErrnoException toReturn;
            if (optID != 1) {
                if (optID != 4) {
                    ErrnoException e;
                    if (optID == 128) {
                        e = Os.getsockoptLinger(this.fd, OsConstants.SOL_SOCKET, OsConstants.SO_LINGER);
                        if (e.isOn()) {
                            toReturn = Integer.valueOf(e.l_linger);
                        } else {
                            toReturn = Integer.valueOf(-1);
                        }
                    } else if (optID != 4102) {
                        switch (optID) {
                            case 4097:
                            case 4098:
                                break;
                            default:
                                try {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Unknown option: ");
                                    stringBuilder.append(optID);
                                    throw new IOException(stringBuilder.toString());
                                } catch (ErrnoException e2) {
                                    throw e2.rethrowAsIOException();
                                }
                        }
                    } else {
                        toReturn = Integer.valueOf((int) Os.getsockoptTimeval(this.fd, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO).toMillis());
                    }
                }
                toReturn = Integer.valueOf(Os.getsockoptInt(this.fd, OsConstants.SOL_SOCKET, javaSoToOsOpt(optID)));
            } else {
                toReturn = Integer.valueOf(Os.getsockoptInt(this.fd, OsConstants.IPPROTO_TCP, OsConstants.TCP_NODELAY));
            }
            return toReturn;
        }
        throw new IOException("socket not created");
    }

    public void setOption(int optID, Object value) throws IOException {
        ErrnoException e;
        if (this.fd != null) {
            StringBuilder stringBuilder;
            int boolValue = -1;
            int intValue = 0;
            if (value instanceof Integer) {
                intValue = ((Integer) value).intValue();
            } else if (value instanceof Boolean) {
                boolValue = ((Boolean) value).booleanValue();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("bad value: ");
                stringBuilder.append(value);
                throw new IOException(stringBuilder.toString());
            }
            if (optID != 1) {
                if (optID != 4) {
                    if (optID == 128) {
                        Os.setsockoptLinger(this.fd, OsConstants.SOL_SOCKET, OsConstants.SO_LINGER, new StructLinger(boolValue, intValue));
                        return;
                    } else if (optID != 4102) {
                        switch (optID) {
                            case 4097:
                            case 4098:
                                break;
                            default:
                                try {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Unknown option: ");
                                    stringBuilder.append(optID);
                                    throw new IOException(stringBuilder.toString());
                                } catch (ErrnoException e2) {
                                    throw e2.rethrowAsIOException();
                                }
                        }
                    } else {
                        e2 = StructTimeval.fromMillis((long) intValue);
                        Os.setsockoptTimeval(this.fd, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, e2);
                        Os.setsockoptTimeval(this.fd, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, e2);
                        return;
                    }
                }
                Os.setsockoptInt(this.fd, OsConstants.SOL_SOCKET, javaSoToOsOpt(optID), intValue);
                return;
            }
            Os.setsockoptInt(this.fd, OsConstants.IPPROTO_TCP, OsConstants.TCP_NODELAY, intValue);
            return;
        }
        throw new IOException("socket not created");
    }

    public void setFileDescriptorsForSend(FileDescriptor[] fds) {
        synchronized (this.writeMonitor) {
            this.outboundFileDescriptors = fds;
        }
    }

    public FileDescriptor[] getAncillaryFileDescriptors() throws IOException {
        FileDescriptor[] result;
        synchronized (this.readMonitor) {
            result = this.inboundFileDescriptors;
            this.inboundFileDescriptors = null;
        }
        return result;
    }

    public Credentials getPeerCredentials() throws IOException {
        return getPeerCredentials_native(this.fd);
    }

    public LocalSocketAddress getSockAddress() throws IOException {
        return null;
    }

    protected void finalize() throws IOException {
        close();
    }

    private static int javaSoToOsOpt(int optID) {
        if (optID == 4) {
            return OsConstants.SO_REUSEADDR;
        }
        switch (optID) {
            case 4097:
                return OsConstants.SO_SNDBUF;
            case 4098:
                return OsConstants.SO_RCVBUF;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown option: ");
                stringBuilder.append(optID);
                throw new UnsupportedOperationException(stringBuilder.toString());
        }
    }
}
