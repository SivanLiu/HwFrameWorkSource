package android.bluetooth;

import android.net.LocalSocket;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import com.huawei.pgmng.log.LogPower;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public final class BluetoothSocket implements Closeable {
    static final int BTSOCK_FLAG_NO_SDP = 4;
    private static final boolean DBG = Log.isLoggable(TAG, 3);
    static final int EADDRINUSE = 98;
    static final int EBADFD = 77;
    static final int MAX_L2CAP_PACKAGE_SIZE = 65535;
    public static final int MAX_RFCOMM_CHANNEL = 30;
    private static final int PROXY_CONNECTION_TIMEOUT = 5000;
    static final int SEC_FLAG_AUTH = 2;
    static final int SEC_FLAG_AUTH_16_DIGIT = 16;
    static final int SEC_FLAG_AUTH_MITM = 8;
    static final int SEC_FLAG_ENCRYPT = 1;
    private static final int SOCK_SIGNAL_SIZE = 20;
    private static final String TAG = "BluetoothSocket";
    public static final int TYPE_L2CAP = 3;
    public static final int TYPE_L2CAP_BREDR = 3;
    public static final int TYPE_L2CAP_LE = 4;
    public static final int TYPE_RFCOMM = 1;
    public static final int TYPE_SCO = 2;
    private static final boolean VDBG = Log.isLoggable(TAG, 2);
    private String mAddress;
    private final boolean mAuth;
    private boolean mAuthMitm;
    private BluetoothDevice mDevice;
    private final boolean mEncrypt;
    private boolean mExcludeSdp;
    private int mFd;
    private final BluetoothInputStream mInputStream;
    private ByteBuffer mL2capBuffer;
    private int mMaxRxPacketSize;
    private int mMaxTxPacketSize;
    private boolean mMin16DigitPin;
    private final BluetoothOutputStream mOutputStream;
    private ParcelFileDescriptor mPfd;
    private int mPort;
    private String mServiceName;
    private LocalSocket mSocket;
    private InputStream mSocketIS;
    private OutputStream mSocketOS;
    private volatile SocketState mSocketState;
    private final int mType;
    private final ParcelUuid mUuid;

    private enum SocketState {
        INIT,
        CONNECTED,
        LISTENING,
        CLOSED
    }

    BluetoothSocket(int type, int fd, boolean auth, boolean encrypt, BluetoothDevice device, int port, ParcelUuid uuid) throws IOException {
        this(type, fd, auth, encrypt, device, port, uuid, false, false);
    }

    BluetoothSocket(int type, int fd, boolean auth, boolean encrypt, BluetoothDevice device, int port, ParcelUuid uuid, boolean mitm, boolean min16DigitPin) throws IOException {
        StringBuilder stringBuilder;
        this.mExcludeSdp = false;
        this.mAuthMitm = false;
        this.mMin16DigitPin = false;
        this.mL2capBuffer = null;
        this.mMaxTxPacketSize = 0;
        this.mMaxRxPacketSize = 0;
        if (VDBG) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Creating new BluetoothSocket of type: ");
            stringBuilder.append(type);
            Log.d(str, stringBuilder.toString());
        }
        if (type == 1 && uuid == null && fd == -1 && port != -2 && (port < 1 || port > 30)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid RFCOMM channel: ");
            stringBuilder.append(port);
            throw new IOException(stringBuilder.toString());
        }
        if (uuid != null) {
            this.mUuid = uuid;
        } else {
            this.mUuid = new ParcelUuid(new UUID(0, 0));
        }
        this.mType = type;
        this.mAuth = auth;
        this.mAuthMitm = mitm;
        this.mMin16DigitPin = min16DigitPin;
        this.mEncrypt = encrypt;
        this.mDevice = device;
        this.mPort = port;
        this.mFd = fd;
        this.mSocketState = SocketState.INIT;
        if (device == null) {
            this.mAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
        } else {
            this.mAddress = device.getAddress();
        }
        this.mInputStream = new BluetoothInputStream(this);
        this.mOutputStream = new BluetoothOutputStream(this);
    }

    private BluetoothSocket(BluetoothSocket s) {
        this.mExcludeSdp = false;
        this.mAuthMitm = false;
        this.mMin16DigitPin = false;
        this.mL2capBuffer = null;
        this.mMaxTxPacketSize = 0;
        this.mMaxRxPacketSize = 0;
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Creating new Private BluetoothSocket of type: ");
            stringBuilder.append(s.mType);
            Log.d(str, stringBuilder.toString());
        }
        this.mUuid = s.mUuid;
        this.mType = s.mType;
        this.mAuth = s.mAuth;
        this.mEncrypt = s.mEncrypt;
        this.mPort = s.mPort;
        this.mInputStream = new BluetoothInputStream(this);
        this.mOutputStream = new BluetoothOutputStream(this);
        this.mMaxRxPacketSize = s.mMaxRxPacketSize;
        this.mMaxTxPacketSize = s.mMaxTxPacketSize;
        this.mServiceName = s.mServiceName;
        this.mExcludeSdp = s.mExcludeSdp;
        this.mAuthMitm = s.mAuthMitm;
        this.mMin16DigitPin = s.mMin16DigitPin;
    }

    private BluetoothSocket acceptSocket(String remoteAddr) throws IOException {
        BluetoothSocket as = new BluetoothSocket(this);
        as.mSocketState = SocketState.CONNECTED;
        FileDescriptor[] fds = this.mSocket.getAncillaryFileDescriptors();
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("socket fd passed by stack fds: ");
            stringBuilder.append(Arrays.toString(fds));
            Log.d(str, stringBuilder.toString());
        }
        if (fds == null || fds.length != 1) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("socket fd passed from stack failed, fds: ");
            stringBuilder2.append(Arrays.toString(fds));
            Log.e(TAG, stringBuilder2.toString());
            as.close();
            throw new IOException("bt socket acept failed");
        }
        as.mPfd = new ParcelFileDescriptor(fds[0]);
        as.mSocket = LocalSocket.createConnectedLocalSocket(fds[0]);
        as.mSocketIS = as.mSocket.getInputStream();
        as.mSocketOS = as.mSocket.getOutputStream();
        as.mAddress = remoteAddr;
        as.mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(remoteAddr);
        LogPower.push(172, Integer.toString(Binder.getCallingUid()), Integer.toString(this.mType), Integer.toString(this.mPort));
        return as;
    }

    private BluetoothSocket(int type, int fd, boolean auth, boolean encrypt, String address, int port) throws IOException {
        this(type, fd, auth, encrypt, new BluetoothDevice(address), port, null, false, false);
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private int getSecurityFlags() {
        int flags = 0;
        if (this.mAuth) {
            flags = 0 | 2;
        }
        if (this.mEncrypt) {
            flags |= 1;
        }
        if (this.mExcludeSdp) {
            flags |= 4;
        }
        if (this.mAuthMitm) {
            flags |= 8;
        }
        if (this.mMin16DigitPin) {
            return flags | 16;
        }
        return flags;
    }

    public BluetoothDevice getRemoteDevice() {
        return this.mDevice;
    }

    public InputStream getInputStream() throws IOException {
        return this.mInputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        return this.mOutputStream;
    }

    public boolean isConnected() {
        return this.mSocketState == SocketState.CONNECTED;
    }

    void setServiceName(String name) {
        this.mServiceName = name;
    }

    public void connect() throws IOException {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BT connect calling pid/uid = ");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append("/");
        stringBuilder.append(Binder.getCallingUid());
        Log.d(str, stringBuilder.toString());
        if (this.mDevice != null) {
            StringBuilder stringBuilder2;
            try {
                if (this.mSocketState != SocketState.CLOSED) {
                    IBluetooth bluetoothProxy = BluetoothAdapter.getDefaultAdapter().getBluetoothService(null);
                    if (bluetoothProxy == null) {
                        throw new IOException("Bluetooth is off");
                    } else if (bluetoothProxy.shouldRefuseConn(Binder.getCallingUid(), Binder.getCallingPid(), System.currentTimeMillis())) {
                        throw new IOException("Connect refused");
                    } else {
                        this.mPfd = bluetoothProxy.getSocketManager().connectSocket(this.mDevice, this.mType, this.mUuid, this.mPort, getSecurityFlags());
                        synchronized (this) {
                            if (DBG) {
                                String str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("connect(), SocketState: ");
                                stringBuilder2.append(this.mSocketState);
                                stringBuilder2.append(", mPfd: ");
                                stringBuilder2.append(this.mPfd);
                                Log.d(str2, stringBuilder2.toString());
                            }
                            if (this.mSocketState == SocketState.CLOSED) {
                                throw new IOException("socket closed");
                            } else if (this.mPfd != null) {
                                this.mSocket = LocalSocket.createConnectedLocalSocket(this.mPfd.getFileDescriptor());
                                this.mSocketIS = this.mSocket.getInputStream();
                                this.mSocketOS = this.mSocket.getOutputStream();
                            } else {
                                throw new IOException("bt socket connect failed");
                            }
                        }
                        int channel = readInt(this.mSocketIS);
                        if (channel > 0) {
                            this.mPort = channel;
                            waitSocketSignal(this.mSocketIS);
                            synchronized (this) {
                                if (this.mSocketState != SocketState.CLOSED) {
                                    this.mSocketState = SocketState.CONNECTED;
                                    LogPower.push(172, Integer.toString(Binder.getCallingUid()), Integer.toString(this.mType), Integer.toString(this.mPort));
                                } else {
                                    throw new IOException("bt socket closed");
                                }
                            }
                            return;
                        }
                        throw new IOException("bt socket connect failed");
                    }
                }
                throw new IOException("socket closed");
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("unable to send RPC: ");
                stringBuilder2.append(e.getMessage());
                throw new IOException(stringBuilder2.toString());
            }
        }
        throw new IOException("Connect is called on null device");
    }

    /* JADX WARNING: Missing block: B:45:0x00dd, code skipped:
            if (DBG == false) goto L_0x00f7;
     */
    /* JADX WARNING: Missing block: B:46:0x00df, code skipped:
            r2 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("bindListen(), readInt mSocketIS: ");
            r4.append(r10.mSocketIS);
            android.util.Log.d(r2, r4.toString());
     */
    /* JADX WARNING: Missing block: B:47:0x00f7, code skipped:
            r2 = readInt(r10.mSocketIS);
     */
    /* JADX WARNING: Missing block: B:48:0x00fd, code skipped:
            monitor-enter(r10);
     */
    /* JADX WARNING: Missing block: B:51:0x0102, code skipped:
            if (r10.mSocketState != android.bluetooth.BluetoothSocket.SocketState.INIT) goto L_0x0108;
     */
    /* JADX WARNING: Missing block: B:52:0x0104, code skipped:
            r10.mSocketState = android.bluetooth.BluetoothSocket.SocketState.LISTENING;
     */
    /* JADX WARNING: Missing block: B:53:0x0108, code skipped:
            monitor-exit(r10);
     */
    /* JADX WARNING: Missing block: B:56:0x010b, code skipped:
            if (DBG == false) goto L_0x012d;
     */
    /* JADX WARNING: Missing block: B:57:0x010d, code skipped:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("bindListen(): channel=");
            r5.append(r2);
            r5.append(", mPort=");
            r5.append(r10.mPort);
            android.util.Log.d(r4, r5.toString());
     */
    /* JADX WARNING: Missing block: B:59:0x012f, code skipped:
            if (r10.mPort > -1) goto L_0x0133;
     */
    /* JADX WARNING: Missing block: B:60:0x0131, code skipped:
            r10.mPort = r2;
     */
    /* JADX WARNING: Missing block: B:62:0x0136, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int bindListen() {
        if (this.mSocketState == SocketState.CLOSED) {
            return 77;
        }
        IBluetooth bluetoothProxy = BluetoothAdapter.getDefaultAdapter().getBluetoothService(null);
        if (bluetoothProxy == null) {
            Log.e(TAG, "bindListen fail, reason: bluetooth is off");
            return -1;
        }
        try {
            if (bluetoothProxy.getSocketManager() == null) {
                Log.e(TAG, "bindListen fail, reason: bluetoothProxy.getSocketManager() is off");
                return -1;
            }
            String str;
            StringBuilder stringBuilder;
            if (DBG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("bindListen(): mPort=");
                stringBuilder.append(this.mPort);
                stringBuilder.append(", mType=");
                stringBuilder.append(this.mType);
                Log.d(str, stringBuilder.toString());
            }
            this.mPfd = bluetoothProxy.getSocketManager().createSocketChannel(this.mType, this.mServiceName, this.mUuid, this.mPort, getSecurityFlags());
            try {
                synchronized (this) {
                    if (DBG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("bindListen(), SocketState: ");
                        stringBuilder.append(this.mSocketState);
                        stringBuilder.append(", mPfd: ");
                        stringBuilder.append(this.mPfd);
                        Log.d(str, stringBuilder.toString());
                    }
                    if (this.mSocketState != SocketState.INIT) {
                        return 77;
                    } else if (this.mPfd == null) {
                        return -1;
                    } else {
                        FileDescriptor fd = this.mPfd.getFileDescriptor();
                        if (fd == null) {
                            Log.e(TAG, "bindListen(), null file descriptor");
                            return -1;
                        }
                        if (DBG) {
                            Log.d(TAG, "bindListen(), Create LocalSocket");
                        }
                        this.mSocket = LocalSocket.createConnectedLocalSocket(fd);
                        if (DBG) {
                            Log.d(TAG, "bindListen(), new LocalSocket.getInputStream()");
                        }
                        this.mSocketIS = this.mSocket.getInputStream();
                        this.mSocketOS = this.mSocket.getOutputStream();
                    }
                }
            } catch (IOException e) {
                if (this.mPfd != null) {
                    try {
                        this.mPfd.close();
                    } catch (IOException e1) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("bindListen, close mPfd: ");
                        stringBuilder2.append(e1);
                        Log.e(str2, stringBuilder2.toString());
                    }
                    this.mPfd = null;
                }
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("bindListen, fail to get port number, exception: ");
                stringBuilder3.append(e);
                Log.e(str3, stringBuilder3.toString());
                return -1;
            }
        } catch (RemoteException e2) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    BluetoothSocket accept(int timeout) throws IOException {
        if (this.mSocketState == SocketState.LISTENING) {
            String str;
            BluetoothSocket acceptedSocket;
            if (timeout > 0) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("accept() set timeout (ms):");
                stringBuilder.append(timeout);
                Log.d(str, stringBuilder.toString());
                this.mSocket.setSoTimeout(timeout);
            }
            str = waitSocketSignal(this.mSocketIS);
            if (timeout > 0) {
                this.mSocket.setSoTimeout(0);
            }
            synchronized (this) {
                if (this.mSocketState == SocketState.LISTENING) {
                    acceptedSocket = acceptSocket(str);
                } else {
                    throw new IOException("bt socket is not in listen state");
                }
            }
            return acceptedSocket;
        }
        throw new IOException("bt socket is not in listen state");
    }

    int available() throws IOException {
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("available: ");
            stringBuilder.append(this.mSocketIS);
            Log.d(str, stringBuilder.toString());
        }
        return this.mSocketIS.available();
    }

    void flush() throws IOException {
        if (this.mSocketOS != null) {
            if (VDBG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("flush: ");
                stringBuilder.append(this.mSocketOS);
                Log.d(str, stringBuilder.toString());
            }
            this.mSocketOS.flush();
            return;
        }
        throw new IOException("flush is called on null OutputStream");
    }

    int read(byte[] b, int offset, int length) throws IOException {
        String str;
        StringBuilder stringBuilder;
        int ret;
        if (VDBG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("read in:  ");
            stringBuilder.append(this.mSocketIS);
            stringBuilder.append(" len: ");
            stringBuilder.append(length);
            Log.d(str, stringBuilder.toString());
        }
        if (this.mType == 3 || this.mType == 4) {
            String str2;
            StringBuilder stringBuilder2;
            int bytesToRead = length;
            if (VDBG) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("l2cap: read(): offset: ");
                stringBuilder2.append(offset);
                stringBuilder2.append(" length:");
                stringBuilder2.append(length);
                stringBuilder2.append("mL2capBuffer= ");
                stringBuilder2.append(this.mL2capBuffer);
                Log.v(str2, stringBuilder2.toString());
            }
            if (this.mL2capBuffer == null) {
                createL2capRxBuffer();
            }
            if (this.mL2capBuffer.remaining() == 0) {
                if (VDBG) {
                    Log.v(TAG, "l2cap buffer empty, refilling...");
                }
                if (fillL2capRxBuffer() == -1) {
                    return -1;
                }
            }
            if (bytesToRead > this.mL2capBuffer.remaining()) {
                bytesToRead = this.mL2capBuffer.remaining();
            }
            if (VDBG) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("get(): offset: ");
                stringBuilder2.append(offset);
                stringBuilder2.append(" bytesToRead: ");
                stringBuilder2.append(bytesToRead);
                Log.v(str2, stringBuilder2.toString());
            }
            this.mL2capBuffer.get(b, offset, bytesToRead);
            ret = bytesToRead;
        } else {
            if (VDBG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("default: read(): offset: ");
                stringBuilder.append(offset);
                stringBuilder.append(" length:");
                stringBuilder.append(length);
                Log.v(str, stringBuilder.toString());
            }
            ret = this.mSocketIS.read(b, offset, length);
        }
        if (ret >= 0) {
            if (VDBG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("read out:  ");
                stringBuilder.append(this.mSocketIS);
                stringBuilder.append(" ret: ");
                stringBuilder.append(ret);
                Log.d(str, stringBuilder.toString());
            }
            return ret;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("bt socket closed, read return: ");
        stringBuilder.append(ret);
        throw new IOException(stringBuilder.toString());
    }

    int write(byte[] b, int offset, int length) throws IOException {
        String str;
        StringBuilder stringBuilder;
        if (VDBG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("write: ");
            stringBuilder.append(this.mSocketOS);
            stringBuilder.append(" length: ");
            stringBuilder.append(length);
            Log.d(str, stringBuilder.toString());
        }
        if (this.mType != 3 && this.mType != 4) {
            this.mSocketOS.write(b, offset, length);
        } else if (length <= this.mMaxTxPacketSize) {
            this.mSocketOS.write(b, offset, length);
        } else {
            if (DBG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("WARNING: Write buffer larger than L2CAP packet size!\nPacket will be divided into SDU packets of size ");
                stringBuilder.append(this.mMaxTxPacketSize);
                Log.w(str, stringBuilder.toString());
            }
            int tmpOffset = offset;
            int bytesToWrite = length;
            while (bytesToWrite > 0) {
                int tmpLength;
                if (bytesToWrite > this.mMaxTxPacketSize) {
                    tmpLength = this.mMaxTxPacketSize;
                } else {
                    tmpLength = bytesToWrite;
                }
                this.mSocketOS.write(b, tmpOffset, tmpLength);
                tmpOffset += tmpLength;
                bytesToWrite -= tmpLength;
            }
        }
        if (VDBG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("write out: ");
            stringBuilder.append(this.mSocketOS);
            stringBuilder.append(" length: ");
            stringBuilder.append(length);
            Log.d(str, stringBuilder.toString());
        }
        return length;
    }

    /* JADX WARNING: Missing block: B:22:0x00ba, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void close() throws IOException {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("close() this: ");
        stringBuilder.append(this);
        stringBuilder.append(", channel: ");
        stringBuilder.append(this.mPort);
        stringBuilder.append(", mSocketIS: ");
        stringBuilder.append(this.mSocketIS);
        stringBuilder.append(", mSocketOS: ");
        stringBuilder.append(this.mSocketOS);
        stringBuilder.append("mSocket: ");
        stringBuilder.append(this.mSocket);
        stringBuilder.append(", mSocketState: ");
        stringBuilder.append(this.mSocketState);
        Log.d(str, stringBuilder.toString());
        if (this.mSocketState != SocketState.CLOSED) {
            synchronized (this) {
                if (this.mSocketState == SocketState.CLOSED) {
                    return;
                }
                if (this.mSocketState == SocketState.CONNECTED) {
                    LogPower.push(173, Integer.toString(Binder.getCallingUid()), Integer.toString(this.mType), Integer.toString(this.mPort));
                }
                this.mSocketState = SocketState.CLOSED;
                if (this.mSocket != null) {
                    if (DBG) {
                        str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Closing mSocket: ");
                        stringBuilder2.append(this.mSocket);
                        Log.d(str, stringBuilder2.toString());
                    }
                    this.mSocket.shutdownInput();
                    this.mSocket.shutdownOutput();
                    this.mSocket.close();
                    this.mSocket = null;
                }
                if (this.mPfd != null) {
                    this.mPfd.close();
                    this.mPfd = null;
                }
            }
        }
    }

    void removeChannel() {
    }

    int getPort() {
        return this.mPort;
    }

    public int getMaxTransmitPacketSize() {
        return this.mMaxTxPacketSize;
    }

    public int getMaxReceivePacketSize() {
        return this.mMaxRxPacketSize;
    }

    public int getConnectionType() {
        return this.mType;
    }

    public void setExcludeSdp(boolean excludeSdp) {
        this.mExcludeSdp = excludeSdp;
    }

    public void requestMaximumTxDataLength() throws IOException {
        if (this.mDevice != null) {
            try {
                if (this.mSocketState != SocketState.CLOSED) {
                    IBluetooth bluetoothProxy = BluetoothAdapter.getDefaultAdapter().getBluetoothService(null);
                    if (bluetoothProxy != null) {
                        if (DBG) {
                            Log.d(TAG, "requestMaximumTxDataLength");
                        }
                        bluetoothProxy.getSocketManager().requestMaximumTxDataLength(this.mDevice);
                        return;
                    }
                    throw new IOException("Bluetooth is off");
                }
                throw new IOException("socket closed");
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to send RPC: ");
                stringBuilder.append(e.getMessage());
                throw new IOException(stringBuilder.toString());
            }
        }
        throw new IOException("requestMaximumTxDataLength is called on null device");
    }

    private String convertAddr(byte[] addr) {
        return String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", new Object[]{Byte.valueOf(addr[0]), Byte.valueOf(addr[1]), Byte.valueOf(addr[2]), Byte.valueOf(addr[3]), Byte.valueOf(addr[4]), Byte.valueOf(addr[5])});
    }

    private String waitSocketSignal(InputStream is) throws IOException {
        byte[] sig = new byte[20];
        int ret = readAll(is, sig);
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("waitSocketSignal read 20 bytes signal ret: ");
            stringBuilder.append(ret);
            Log.d(str, stringBuilder.toString());
        }
        ByteBuffer bb = ByteBuffer.wrap(sig);
        bb.order(ByteOrder.nativeOrder());
        int size = bb.getShort();
        if (size == 20) {
            StringBuilder stringBuilder2;
            byte[] addr = new byte[6];
            bb.get(addr);
            int channel = bb.getInt();
            int status = bb.getInt();
            this.mMaxTxPacketSize = bb.getShort() & 65535;
            this.mMaxRxPacketSize = bb.getShort() & 65535;
            String RemoteAddr = convertAddr(addr);
            if (VDBG) {
                String str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("waitSocketSignal: sig size: ");
                stringBuilder2.append(size);
                stringBuilder2.append(", remote addr: ");
                stringBuilder2.append(RemoteAddr);
                stringBuilder2.append(", channel: ");
                stringBuilder2.append(channel);
                stringBuilder2.append(", status: ");
                stringBuilder2.append(status);
                stringBuilder2.append(" MaxRxPktSize: ");
                stringBuilder2.append(this.mMaxRxPacketSize);
                stringBuilder2.append(" MaxTxPktSize: ");
                stringBuilder2.append(this.mMaxTxPacketSize);
                Log.d(str2, stringBuilder2.toString());
            }
            if (status == 0) {
                return RemoteAddr;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Connection failure, status: ");
            stringBuilder2.append(status);
            throw new IOException(stringBuilder2.toString());
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Connection failure, wrong signal size: ");
        stringBuilder3.append(size);
        throw new IOException(stringBuilder3.toString());
    }

    private void createL2capRxBuffer() {
        if (this.mType == 3 || this.mType == 4) {
            String str;
            StringBuilder stringBuilder;
            if (VDBG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("  Creating mL2capBuffer: mMaxPacketSize: ");
                stringBuilder.append(this.mMaxRxPacketSize);
                Log.v(str, stringBuilder.toString());
            }
            this.mL2capBuffer = ByteBuffer.wrap(new byte[this.mMaxRxPacketSize]);
            if (VDBG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mL2capBuffer.remaining()");
                stringBuilder.append(this.mL2capBuffer.remaining());
                Log.v(str, stringBuilder.toString());
            }
            this.mL2capBuffer.limit(0);
            if (VDBG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mL2capBuffer.remaining() after limit(0):");
                stringBuilder.append(this.mL2capBuffer.remaining());
                Log.v(str, stringBuilder.toString());
            }
        }
    }

    private int readAll(InputStream is, byte[] b) throws IOException {
        int left = b.length;
        while (left > 0) {
            int ret = is.read(b, b.length - left, left);
            StringBuilder stringBuilder;
            if (ret > 0) {
                left -= ret;
                if (left != 0) {
                    String str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("readAll() looping, read partial size: ");
                    stringBuilder.append(b.length - left);
                    stringBuilder.append(", expect size: ");
                    stringBuilder.append(b.length);
                    Log.w(str, stringBuilder.toString());
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("read failed, socket might closed or timeout, read ret: ");
                stringBuilder.append(ret);
                throw new IOException(stringBuilder.toString());
            }
        }
        return b.length;
    }

    private int readInt(InputStream is) throws IOException {
        byte[] ibytes = new byte[4];
        int ret = readAll(is, ibytes);
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("inputStream.read ret: ");
            stringBuilder.append(ret);
            Log.d(str, stringBuilder.toString());
        }
        ByteBuffer bb = ByteBuffer.wrap(ibytes);
        bb.order(ByteOrder.nativeOrder());
        return bb.getInt();
    }

    private int fillL2capRxBuffer() throws IOException {
        this.mL2capBuffer.rewind();
        int ret = this.mSocketIS.read(this.mL2capBuffer.array());
        if (ret == -1) {
            this.mL2capBuffer.limit(0);
            return -1;
        }
        this.mL2capBuffer.limit(ret);
        return ret;
    }
}
