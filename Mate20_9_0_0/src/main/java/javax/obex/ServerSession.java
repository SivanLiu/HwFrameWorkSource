package javax.obex;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ServerSession extends ObexSession implements Runnable {
    private static final String TAG = "Obex ServerSession";
    private static final boolean V = false;
    private boolean mClosed;
    private InputStream mInput = this.mTransport.openInputStream();
    private ServerRequestHandler mListener;
    private int mMaxPacketLength;
    private OutputStream mOutput = this.mTransport.openOutputStream();
    private Thread mProcessThread;
    private ObexTransport mTransport;

    public ServerSession(ObexTransport trans, ServerRequestHandler handler, Authenticator auth) throws IOException {
        this.mAuthenticator = auth;
        this.mTransport = trans;
        this.mListener = handler;
        this.mMaxPacketLength = 256;
        this.mClosed = false;
        this.mProcessThread = new Thread(this);
        this.mProcessThread.start();
    }

    /* JADX WARNING: Missing block: B:19:0x0047, code skipped:
            handleGetRequest(r1);
     */
    /* JADX WARNING: Missing block: B:20:0x004b, code skipped:
            handlePutRequest(r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        boolean done = false;
        while (!done) {
            try {
                if (this.mClosed) {
                    close();
                }
                int requestType = this.mInput.read();
                if (requestType == -1) {
                    done = true;
                } else if (requestType == ObexHelper.OBEX_OPCODE_SETPATH) {
                    handleSetPathRequest();
                } else if (requestType != 255) {
                    switch (requestType) {
                        case 2:
                            break;
                        case 3:
                            break;
                        default:
                            switch (requestType) {
                                case 128:
                                    handleConnectRequest();
                                    break;
                                case ObexHelper.OBEX_OPCODE_DISCONNECT /*129*/:
                                    handleDisconnectRequest();
                                    break;
                                case ObexHelper.OBEX_OPCODE_PUT_FINAL /*130*/:
                                    break;
                                case ObexHelper.OBEX_OPCODE_GET_FINAL /*131*/:
                                    break;
                                default:
                                    int length = (this.mInput.read() << 8) + this.mInput.read();
                                    for (int i = 3; i < length; i++) {
                                        this.mInput.read();
                                    }
                                    sendResponse(ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED, null);
                                    break;
                            }
                    }
                } else {
                    handleAbortRequest();
                }
            } catch (NullPointerException e) {
                Log.d(TAG, "Exception occured - ignoring", e);
            } catch (Exception e2) {
                Log.d(TAG, "Exception occured - ignoring", e2);
            }
        }
        close();
    }

    private void handleAbortRequest() throws IOException {
        int code;
        HeaderSet request = new HeaderSet();
        HeaderSet reply = new HeaderSet();
        int length = (this.mInput.read() << 8) + this.mInput.read();
        if (length > ObexHelper.getMaxRxPacketSize(this.mTransport)) {
            code = ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE;
        } else {
            for (int i = 3; i < length; i++) {
                this.mInput.read();
            }
            code = this.mListener.onAbort(request, reply);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onAbort request handler return value- ");
            stringBuilder.append(code);
            Log.v(str, stringBuilder.toString());
            code = validateResponseCode(code);
        }
        sendResponse(code, null);
    }

    private void handlePutRequest(int type) throws IOException {
        ServerOperation op = new ServerOperation(this, this.mInput, type, this.mMaxPacketLength, this.mListener);
        try {
            int response;
            if (!op.finalBitSet || op.isValidBody()) {
                response = validateResponseCode(this.mListener.onPut(op));
            } else {
                response = validateResponseCode(this.mListener.onDelete(op.requestHeader, op.replyHeader));
            }
            if (response != ResponseCodes.OBEX_HTTP_OK && !op.isAborted) {
                op.sendReply(response);
            } else if (!op.isAborted) {
                while (!op.finalBitSet) {
                    op.sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
                }
                op.sendReply(response);
            }
        } catch (Exception e) {
            if (!op.isAborted) {
                sendResponse(ResponseCodes.OBEX_HTTP_INTERNAL_ERROR, null);
            }
        }
    }

    private void handleGetRequest(int type) throws IOException {
        ServerOperation op = new ServerOperation(this, this.mInput, type, this.mMaxPacketLength, this.mListener);
        try {
            int response = validateResponseCode(this.mListener.onGet(op));
            if (!op.isAborted) {
                op.sendReply(response);
            }
        } catch (Exception e) {
            sendResponse(ResponseCodes.OBEX_HTTP_INTERNAL_ERROR, null);
        }
    }

    public void sendResponse(int code, byte[] header) throws IOException {
        OutputStream op = this.mOutput;
        if (op != null) {
            byte[] data;
            if (header != null) {
                int totalLength = 3 + header.length;
                data = new byte[totalLength];
                data[0] = (byte) code;
                data[1] = (byte) (totalLength >> 8);
                data[2] = (byte) totalLength;
                System.arraycopy(header, 0, data, 3, header.length);
            } else {
                data = new byte[]{(byte) code, (byte) 0, (byte) 3};
            }
            op.write(data);
            op.flush();
        }
    }

    private void handleSetPathRequest() throws IOException {
        byte[] headers;
        int totalLength = 3;
        byte[] head = null;
        int code = -1;
        HeaderSet request = new HeaderSet();
        HeaderSet reply = new HeaderSet();
        int length = (this.mInput.read() << 8) + this.mInput.read();
        int flags = this.mInput.read();
        int constants = this.mInput.read();
        if (length > ObexHelper.getMaxRxPacketSize(this.mTransport)) {
            code = ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE;
            totalLength = 3;
        } else {
            if (length > 5) {
                headers = new byte[(length - 5)];
                for (int bytesReceived = this.mInput.read(headers); bytesReceived != headers.length; bytesReceived += this.mInput.read(headers, bytesReceived, headers.length - bytesReceived)) {
                }
                ObexHelper.updateHeaderSet(request, headers);
                if (this.mListener.getConnectionId() == -1 || request.mConnectionID == null) {
                    this.mListener.setConnectionId(1);
                } else {
                    this.mListener.setConnectionId(ObexHelper.convertToLong(request.mConnectionID));
                }
                if (request.mAuthResp != null) {
                    if (!handleAuthResp(request.mAuthResp)) {
                        code = ResponseCodes.OBEX_HTTP_UNAUTHORIZED;
                        this.mListener.onAuthenticationFailure(ObexHelper.getTagValue((byte) 1, request.mAuthResp));
                    }
                    request.mAuthResp = null;
                }
            }
            int code2 = code;
            if (code2 != ResponseCodes.OBEX_HTTP_UNAUTHORIZED) {
                if (request.mAuthChall != null) {
                    handleAuthChall(request);
                    reply.mAuthResp = new byte[request.mAuthResp.length];
                    System.arraycopy(request.mAuthResp, 0, reply.mAuthResp, 0, reply.mAuthResp.length);
                    request.mAuthChall = null;
                    request.mAuthResp = null;
                }
                boolean backup = false;
                boolean create = true;
                if ((flags & 1) != 0) {
                    backup = true;
                }
                boolean backup2 = backup;
                if ((flags & 2) != 0) {
                    create = false;
                }
                try {
                    code = validateResponseCode(this.mListener.onSetPath(request, reply, backup2, create));
                    if (reply.nonce != null) {
                        this.mChallengeDigest = new byte[16];
                        System.arraycopy(reply.nonce, 0, this.mChallengeDigest, 0, 16);
                        headers = null;
                    } else {
                        headers = null;
                        this.mChallengeDigest = null;
                    }
                    long id = this.mListener.getConnectionId();
                    if (id == -1) {
                        reply.mConnectionID = headers;
                    } else {
                        reply.mConnectionID = ObexHelper.convertToByteArray(id);
                    }
                    head = ObexHelper.createHeader(reply, false);
                    totalLength = 3 + head.length;
                    if (totalLength > this.mMaxPacketLength) {
                        totalLength = 3;
                        head = null;
                        code = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
                    }
                } catch (Exception e) {
                    sendResponse(ResponseCodes.OBEX_HTTP_INTERNAL_ERROR, null);
                    return;
                }
            }
            code = code2;
        }
        headers = new byte[totalLength];
        headers[0] = (byte) code;
        headers[1] = (byte) (totalLength >> 8);
        headers[2] = (byte) totalLength;
        if (head != null) {
            System.arraycopy(head, 0, headers, 3, head.length);
        }
        this.mOutput.write(headers);
        this.mOutput.flush();
    }

    private void handleDisconnectRequest() throws IOException {
        byte[] headers;
        int code = ResponseCodes.OBEX_HTTP_OK;
        int totalLength = 3;
        byte[] head = null;
        HeaderSet request = new HeaderSet();
        HeaderSet reply = new HeaderSet();
        int length = (this.mInput.read() << 8) + this.mInput.read();
        if (length > ObexHelper.getMaxRxPacketSize(this.mTransport)) {
            code = ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE;
            totalLength = 3;
        } else {
            if (length > 3) {
                headers = new byte[(length - 3)];
                for (int bytesReceived = this.mInput.read(headers); bytesReceived != headers.length; bytesReceived += this.mInput.read(headers, bytesReceived, headers.length - bytesReceived)) {
                }
                ObexHelper.updateHeaderSet(request, headers);
            }
            if (this.mListener.getConnectionId() == -1 || request.mConnectionID == null) {
                this.mListener.setConnectionId(1);
            } else {
                this.mListener.setConnectionId(ObexHelper.convertToLong(request.mConnectionID));
            }
            if (request.mAuthResp != null) {
                if (!handleAuthResp(request.mAuthResp)) {
                    code = ResponseCodes.OBEX_HTTP_UNAUTHORIZED;
                    this.mListener.onAuthenticationFailure(ObexHelper.getTagValue((byte) 1, request.mAuthResp));
                }
                request.mAuthResp = null;
            }
            int code2 = code;
            if (code2 != ResponseCodes.OBEX_HTTP_UNAUTHORIZED) {
                if (request.mAuthChall != null) {
                    handleAuthChall(request);
                    request.mAuthChall = null;
                }
                try {
                    this.mListener.onDisconnect(request, reply);
                    long id = this.mListener.getConnectionId();
                    if (id == -1) {
                        reply.mConnectionID = null;
                    } else {
                        reply.mConnectionID = ObexHelper.convertToByteArray(id);
                    }
                    head = ObexHelper.createHeader(reply, false);
                    totalLength = 3 + head.length;
                    if (totalLength > this.mMaxPacketLength) {
                        totalLength = 3;
                        head = null;
                        code = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
                    }
                } catch (Exception e) {
                    sendResponse(ResponseCodes.OBEX_HTTP_INTERNAL_ERROR, null);
                    return;
                }
            }
            code = code2;
        }
        if (head != null) {
            headers = new byte[(head.length + 3)];
        } else {
            headers = new byte[3];
        }
        headers[0] = (byte) code;
        headers[1] = (byte) (totalLength >> 8);
        headers[2] = (byte) totalLength;
        if (head != null) {
            System.arraycopy(head, 0, headers, 3, head.length);
        }
        this.mOutput.write(headers);
        this.mOutput.flush();
    }

    private void handleConnectRequest() throws IOException {
        byte[] headers;
        int bytesReceived;
        int totalLength = 7;
        byte[] head = null;
        int code = -1;
        HeaderSet request = new HeaderSet();
        HeaderSet reply = new HeaderSet();
        int packetLength = (this.mInput.read() << 8) + this.mInput.read();
        int version = this.mInput.read();
        int flags = this.mInput.read();
        this.mMaxPacketLength = this.mInput.read();
        this.mMaxPacketLength = (this.mMaxPacketLength << 8) + this.mInput.read();
        if (this.mMaxPacketLength > ObexHelper.MAX_PACKET_SIZE_INT) {
            this.mMaxPacketLength = ObexHelper.MAX_PACKET_SIZE_INT;
        }
        if (this.mMaxPacketLength > ObexHelper.getMaxTxPacketSize(this.mTransport)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Requested MaxObexPacketSize ");
            stringBuilder.append(this.mMaxPacketLength);
            stringBuilder.append(" is larger than the max size supported by the transport: ");
            stringBuilder.append(ObexHelper.getMaxTxPacketSize(this.mTransport));
            stringBuilder.append(" Reducing to this size.");
            Log.w(str, stringBuilder.toString());
            this.mMaxPacketLength = ObexHelper.getMaxTxPacketSize(this.mTransport);
        }
        if (packetLength > ObexHelper.getMaxRxPacketSize(this.mTransport)) {
            code = ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE;
            totalLength = 7;
        } else {
            if (packetLength > 7) {
                headers = new byte[(packetLength - 7)];
                for (bytesReceived = this.mInput.read(headers); bytesReceived != headers.length; bytesReceived += this.mInput.read(headers, bytesReceived, headers.length - bytesReceived)) {
                }
                ObexHelper.updateHeaderSet(request, headers);
            }
            if (this.mListener.getConnectionId() == -1 || request.mConnectionID == null) {
                this.mListener.setConnectionId(1);
            } else {
                this.mListener.setConnectionId(ObexHelper.convertToLong(request.mConnectionID));
            }
            if (request.mAuthResp != null) {
                if (!handleAuthResp(request.mAuthResp)) {
                    code = ResponseCodes.OBEX_HTTP_UNAUTHORIZED;
                    this.mListener.onAuthenticationFailure(ObexHelper.getTagValue((byte) 1, request.mAuthResp));
                }
                request.mAuthResp = null;
            }
            int code2 = code;
            if (code2 != ResponseCodes.OBEX_HTTP_UNAUTHORIZED) {
                if (request.mAuthChall != null) {
                    handleAuthChall(request);
                    reply.mAuthResp = new byte[request.mAuthResp.length];
                    System.arraycopy(request.mAuthResp, 0, reply.mAuthResp, 0, reply.mAuthResp.length);
                    request.mAuthChall = null;
                    request.mAuthResp = null;
                }
                try {
                    code2 = validateResponseCode(this.mListener.onConnect(request, reply));
                    if (reply.nonce != null) {
                        this.mChallengeDigest = new byte[16];
                        System.arraycopy(reply.nonce, 0, this.mChallengeDigest, 0, 16);
                    } else {
                        this.mChallengeDigest = null;
                    }
                    long id = this.mListener.getConnectionId();
                    if (id == -1) {
                        reply.mConnectionID = null;
                    } else {
                        reply.mConnectionID = ObexHelper.convertToByteArray(id);
                    }
                    head = ObexHelper.createHeader(reply, false);
                    totalLength = 7 + head.length;
                    if (totalLength > this.mMaxPacketLength) {
                        totalLength = 7;
                        code = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
                        head = null;
                    } else {
                        code = code2;
                    }
                } catch (Exception e) {
                    totalLength = 7;
                    head = null;
                    code = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
                }
            } else {
                code = code2;
            }
        }
        headers = ObexHelper.convertToByteArray((long) totalLength);
        byte[] sendData = new byte[totalLength];
        bytesReceived = ObexHelper.getMaxRxPacketSize(this.mTransport);
        if (bytesReceived > this.mMaxPacketLength) {
            bytesReceived = this.mMaxPacketLength;
        }
        sendData[0] = (byte) code;
        sendData[1] = headers[2];
        sendData[2] = headers[3];
        sendData[3] = (byte) 16;
        sendData[4] = (byte) 0;
        sendData[5] = (byte) (bytesReceived >> 8);
        sendData[6] = (byte) (bytesReceived & 255);
        if (head != null) {
            System.arraycopy(head, 0, sendData, 7, head.length);
        }
        this.mOutput.write(sendData);
        this.mOutput.flush();
    }

    public synchronized void close() {
        if (this.mListener != null) {
            this.mListener.onClose();
        }
        try {
            this.mClosed = true;
            if (this.mInput != null) {
                this.mInput.close();
            }
            if (this.mOutput != null) {
                this.mOutput.close();
            }
            if (this.mTransport != null) {
                this.mTransport.close();
            }
        } catch (Exception e) {
        }
        this.mTransport = null;
        this.mInput = null;
        this.mOutput = null;
        this.mListener = null;
    }

    private int validateResponseCode(int code) {
        if (code >= ResponseCodes.OBEX_HTTP_OK && code <= ResponseCodes.OBEX_HTTP_PARTIAL) {
            return code;
        }
        if (code >= ResponseCodes.OBEX_HTTP_MULT_CHOICE && code <= ResponseCodes.OBEX_HTTP_USE_PROXY) {
            return code;
        }
        if (code >= 192 && code <= ResponseCodes.OBEX_HTTP_UNSUPPORTED_TYPE) {
            return code;
        }
        if (code >= ResponseCodes.OBEX_HTTP_INTERNAL_ERROR && code <= ResponseCodes.OBEX_HTTP_VERSION) {
            return code;
        }
        if (code < ResponseCodes.OBEX_DATABASE_FULL || code > ResponseCodes.OBEX_DATABASE_LOCKED) {
            return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
        }
        return code;
    }

    public ObexTransport getTransport() {
        return this.mTransport;
    }
}
