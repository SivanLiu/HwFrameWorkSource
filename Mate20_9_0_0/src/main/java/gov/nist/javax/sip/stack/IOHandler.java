package gov.nist.javax.sip.stack;

import gov.nist.core.Separators;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.ParameterNames;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.ccil.cowan.tagsoup.HTMLModels;

class IOHandler {
    private static String TCP = ParameterNames.TCP;
    private static String TLS = ParameterNames.TLS;
    private Semaphore ioSemaphore = new Semaphore(1);
    private SipStackImpl sipStack;
    private ConcurrentHashMap<String, Socket> socketTable;

    protected static String makeKey(InetAddress addr, int port) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(addr.getHostAddress());
        stringBuilder.append(Separators.COLON);
        stringBuilder.append(port);
        return stringBuilder.toString();
    }

    protected IOHandler(SIPTransactionStack sipStack) {
        this.sipStack = (SipStackImpl) sipStack;
        this.socketTable = new ConcurrentHashMap();
    }

    protected void putSocket(String key, Socket sock) {
        this.socketTable.put(key, sock);
    }

    protected Socket getSocket(String key) {
        return (Socket) this.socketTable.get(key);
    }

    protected void removeSocket(String key) {
        this.socketTable.remove(key);
    }

    private void writeChunks(OutputStream outputStream, byte[] bytes, int length) throws IOException {
        synchronized (outputStream) {
            int p = 0;
            while (p < length) {
                outputStream.write(bytes, p, p + HTMLModels.M_LEGEND < length ? HTMLModels.M_LEGEND : length - p);
                p += HTMLModels.M_LEGEND;
            }
        }
        outputStream.flush();
    }

    public SocketAddress obtainLocalAddress(InetAddress dst, int dstPort, InetAddress localAddress, int localPort) throws IOException {
        String key = makeKey(dst, dstPort);
        Socket clientSock = getSocket(key);
        if (clientSock == null) {
            clientSock = this.sipStack.getNetworkLayer().createSocket(dst, dstPort, localAddress, localPort);
            putSocket(key, clientSock);
        }
        return clientSock.getLocalSocketAddress();
    }

    /* JADX WARNING: Missing block: B:24:0x0096, code skipped:
            if (r1.sipStack.isLoggingEnabled() == false) goto L_0x00cc;
     */
    /* JADX WARNING: Missing block: B:25:0x0098, code skipped:
            r0 = r1.sipStack.getStackLogger();
            r4 = new java.lang.StringBuilder();
            r4.append("inaddr = ");
            r4.append(r9);
            r0.logDebug(r4.toString());
            r0 = r1.sipStack.getStackLogger();
            r4 = new java.lang.StringBuilder();
            r4.append("port = ");
            r4.append(r10);
            r0.logDebug(r4.toString());
     */
    /* JADX WARNING: Missing block: B:26:0x00cc, code skipped:
            r3 = r1.sipStack.getNetworkLayer().createSocket(r9, r10, r2);
            writeChunks(r3.getOutputStream(), r12, r7);
            putSocket(r5, r3);
     */
    /* JADX WARNING: Missing block: B:70:0x01bf, code skipped:
            if (r1.sipStack.isLoggingEnabled() == false) goto L_0x01f5;
     */
    /* JADX WARNING: Missing block: B:71:0x01c1, code skipped:
            r0 = r1.sipStack.getStackLogger();
            r4 = new java.lang.StringBuilder();
            r4.append("inaddr = ");
            r4.append(r9);
            r0.logDebug(r4.toString());
            r0 = r1.sipStack.getStackLogger();
            r4 = new java.lang.StringBuilder();
            r4.append("port = ");
            r4.append(r10);
            r0.logDebug(r4.toString());
     */
    /* JADX WARNING: Missing block: B:72:0x01f5, code skipped:
            r3 = r1.sipStack.getNetworkLayer().createSSLSocket(r9, r10, r2);
            r0 = (javax.net.ssl.SSLSocket) r3;
            r4 = new gov.nist.javax.sip.stack.HandshakeCompletedListenerImpl((gov.nist.javax.sip.stack.TLSMessageChannel) r24);
            ((gov.nist.javax.sip.stack.TLSMessageChannel) r24).setHandshakeCompletedListener(r4);
            r0.addHandshakeCompletedListener(r4);
            r0.setEnabledProtocols(r1.sipStack.getEnabledProtocols());
            r0.startHandshake();
            writeChunks(r3.getOutputStream(), r12, r7);
            putSocket(r5, r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Socket sendBytes(InetAddress senderAddress, InetAddress receiverAddress, int contactPort, String transport, byte[] bytes, boolean retry, MessageChannel messageChannel) throws IOException {
        StackLogger stackLogger;
        IOException ex;
        InetAddress inetAddress = senderAddress;
        InetAddress inetAddress2 = receiverAddress;
        int i = contactPort;
        String str = transport;
        byte[] bArr = bytes;
        int retry_count = 0;
        int max_retry = retry ? 2 : 1;
        int length = bArr.length;
        if (this.sipStack.isLoggingEnabled()) {
            stackLogger = this.sipStack.getStackLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendBytes ");
            stringBuilder.append(str);
            stringBuilder.append(" inAddr ");
            stringBuilder.append(receiverAddress.getHostAddress());
            stringBuilder.append(" port = ");
            stringBuilder.append(i);
            stringBuilder.append(" length = ");
            stringBuilder.append(length);
            stackLogger.logDebug(stringBuilder.toString());
        }
        if (this.sipStack.isLoggingEnabled() && this.sipStack.isLogStackTraceOnMessageSend()) {
            this.sipStack.getStackLogger().logStackTrace(16);
        }
        String key;
        Socket clientSock;
        Socket clientSock2;
        StringBuilder stringBuilder2;
        if (str.compareToIgnoreCase(TCP) == 0) {
            key = makeKey(receiverAddress, contactPort);
            try {
                if (this.ioSemaphore.tryAcquire(10000, TimeUnit.MILLISECONDS)) {
                    clientSock = getSocket(key);
                    while (true) {
                        clientSock2 = clientSock;
                        if (retry_count >= max_retry) {
                            break;
                        } else if (clientSock2 == null) {
                            break;
                        } else {
                            try {
                                writeChunks(clientSock2.getOutputStream(), bArr, length);
                                break;
                            } catch (IOException e) {
                                ex = e;
                                if (this.sipStack.isLoggingEnabled()) {
                                    stackLogger = this.sipStack.getStackLogger();
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("IOException occured retryCount ");
                                    stringBuilder3.append(retry_count);
                                    stackLogger.logDebug(stringBuilder3.toString());
                                }
                                removeSocket(key);
                                try {
                                    clientSock2.close();
                                } catch (Exception e2) {
                                }
                                clientSock = null;
                                retry_count++;
                            } catch (Throwable th) {
                                this.ioSemaphore.release();
                            }
                        }
                    }
                    this.ioSemaphore.release();
                    if (clientSock2 != null) {
                        return clientSock2;
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug(this.socketTable.toString());
                        stackLogger = this.sipStack.getStackLogger();
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Could not connect to ");
                        stringBuilder2.append(inetAddress2);
                        stringBuilder2.append(Separators.COLON);
                        stringBuilder2.append(i);
                        stackLogger.logError(stringBuilder2.toString());
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Could not connect to ");
                    stringBuilder2.append(inetAddress2);
                    stringBuilder2.append(Separators.COLON);
                    stringBuilder2.append(i);
                    throw new IOException(stringBuilder2.toString());
                }
                throw new IOException("Could not acquire IO Semaphore after 10 seconds -- giving up ");
            } catch (InterruptedException e3) {
                throw new IOException("exception in acquiring sem");
            }
        } else if (str.compareToIgnoreCase(TLS) == 0) {
            key = makeKey(receiverAddress, contactPort);
            try {
                if (this.ioSemaphore.tryAcquire(10000, TimeUnit.MILLISECONDS)) {
                    clientSock = getSocket(key);
                    while (true) {
                        clientSock2 = clientSock;
                        if (retry_count >= max_retry) {
                            break;
                        } else if (clientSock2 == null) {
                            break;
                        } else {
                            try {
                                writeChunks(clientSock2.getOutputStream(), bArr, length);
                                break;
                            } catch (IOException e4) {
                                ex = e4;
                                if (this.sipStack.isLoggingEnabled()) {
                                    this.sipStack.getStackLogger().logException(ex);
                                }
                                removeSocket(key);
                                try {
                                    clientSock2.close();
                                } catch (Exception e5) {
                                }
                                clientSock = null;
                                retry_count++;
                            } catch (Throwable th2) {
                                this.ioSemaphore.release();
                            }
                        }
                    }
                    this.ioSemaphore.release();
                    if (clientSock2 != null) {
                        return clientSock2;
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Could not connect to ");
                    stringBuilder2.append(inetAddress2);
                    stringBuilder2.append(Separators.COLON);
                    stringBuilder2.append(i);
                    throw new IOException(stringBuilder2.toString());
                }
                throw new IOException("Timeout acquiring IO SEM");
            } catch (InterruptedException e6) {
                throw new IOException("exception in acquiring sem");
            }
        } else {
            DatagramSocket datagramSock = this.sipStack.getNetworkLayer().createDatagramSocket();
            datagramSock.connect(inetAddress2, i);
            datagramSock.send(new DatagramPacket(bArr, 0, length, inetAddress2, i));
            datagramSock.close();
            return null;
        }
    }

    public void closeAll() {
        Enumeration<Socket> values = this.socketTable.elements();
        while (values.hasMoreElements()) {
            try {
                ((Socket) values.nextElement()).close();
            } catch (IOException e) {
            }
        }
    }
}
