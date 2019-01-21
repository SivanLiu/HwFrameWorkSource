package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.core.ServerLogger;
import gov.nist.core.StackLogger;
import gov.nist.core.ThreadAuditor.ThreadHandle;
import gov.nist.javax.sip.address.ParameterNames;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.parser.ParseExceptionListener;
import gov.nist.javax.sip.parser.StringMsgParser;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.TimerTask;
import javax.sip.ListeningPoint;
import javax.sip.address.Hop;

public class UDPMessageChannel extends MessageChannel implements ParseExceptionListener, Runnable, RawMessageChannel {
    private DatagramPacket incomingPacket;
    private String myAddress;
    protected StringMsgParser myParser;
    protected int myPort;
    private InetAddress peerAddress;
    private InetAddress peerPacketSourceAddress;
    private int peerPacketSourcePort;
    private int peerPort;
    private String peerProtocol;
    private Hashtable<String, PingBackTimerTask> pingBackRecord = new Hashtable();
    private long receptionTime;
    protected SIPTransactionStack sipStack;

    class PingBackTimerTask extends TimerTask {
        String ipAddress;
        int port;

        public PingBackTimerTask(String ipAddress, int port) {
            this.ipAddress = ipAddress;
            this.port = port;
            Hashtable access$000 = UDPMessageChannel.this.pingBackRecord;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ipAddress);
            stringBuilder.append(Separators.COLON);
            stringBuilder.append(port);
            access$000.put(stringBuilder.toString(), this);
        }

        public void run() {
            Hashtable access$000 = UDPMessageChannel.this.pingBackRecord;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.ipAddress);
            stringBuilder.append(Separators.COLON);
            stringBuilder.append(this.port);
            access$000.remove(stringBuilder.toString());
        }

        public int hashCode() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.ipAddress);
            stringBuilder.append(Separators.COLON);
            stringBuilder.append(this.port);
            return stringBuilder.toString().hashCode();
        }
    }

    protected UDPMessageChannel(SIPTransactionStack stack, UDPMessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
        this.sipStack = stack;
        Thread mythread = new Thread(this);
        this.myAddress = messageProcessor.getIpAddress().getHostAddress();
        this.myPort = messageProcessor.getPort();
        mythread.setName("UDPMessageChannelThread");
        mythread.setDaemon(true);
        mythread.start();
    }

    protected UDPMessageChannel(SIPTransactionStack stack, UDPMessageProcessor messageProcessor, DatagramPacket packet) {
        this.incomingPacket = packet;
        this.messageProcessor = messageProcessor;
        this.sipStack = stack;
        this.myAddress = messageProcessor.getIpAddress().getHostAddress();
        this.myPort = messageProcessor.getPort();
        Thread mythread = new Thread(this);
        mythread.setDaemon(true);
        mythread.setName("UDPMessageChannelThread");
        mythread.start();
    }

    protected UDPMessageChannel(InetAddress targetAddr, int port, SIPTransactionStack sipStack, UDPMessageProcessor messageProcessor) {
        this.peerAddress = targetAddr;
        this.peerPort = port;
        this.peerProtocol = ListeningPoint.UDP;
        this.messageProcessor = messageProcessor;
        this.myAddress = messageProcessor.getIpAddress().getHostAddress();
        this.myPort = messageProcessor.getPort();
        this.sipStack = sipStack;
        if (sipStack.isLoggingEnabled()) {
            StackLogger stackLogger = this.sipStack.getStackLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Creating message channel ");
            stringBuilder.append(targetAddr.getHostAddress());
            stringBuilder.append(Separators.SLASH);
            stringBuilder.append(port);
            stackLogger.logDebug(stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:29:0x006f, code skipped:
            r6.incomingPacket = r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        ThreadHandle threadHandle = null;
        while (true) {
            DatagramPacket packet;
            if (this.myParser == null) {
                this.myParser = new StringMsgParser();
                this.myParser.setParseExceptionListener(this);
            }
            if (this.sipStack.threadPoolSize != -1) {
                synchronized (((UDPMessageProcessor) this.messageProcessor).messageQueue) {
                    while (((UDPMessageProcessor) this.messageProcessor).messageQueue.isEmpty()) {
                        if (((UDPMessageProcessor) this.messageProcessor).isRunning) {
                            if (threadHandle == null) {
                                try {
                                    threadHandle = this.sipStack.getThreadAuditor().addCurrentThread();
                                } catch (InterruptedException e) {
                                    if (!((UDPMessageProcessor) this.messageProcessor).isRunning) {
                                        return;
                                    }
                                }
                            }
                            threadHandle.ping();
                            ((UDPMessageProcessor) this.messageProcessor).messageQueue.wait(threadHandle.getPingIntervalInMillisecs());
                        } else {
                            return;
                        }
                    }
                    packet = (DatagramPacket) ((UDPMessageProcessor) this.messageProcessor).messageQueue.removeFirst();
                }
            } else {
                packet = this.incomingPacket;
            }
            try {
                processIncomingDataPacket(packet);
            } catch (Exception e2) {
                this.sipStack.getStackLogger().logError("Error while processing incoming UDP packet", e2);
            }
            if (this.sipStack.threadPoolSize == -1) {
                return;
            }
        }
        while (true) {
        }
    }

    private void processIncomingDataPacket(DatagramPacket packet) throws Exception {
        StackLogger stackLogger;
        this.peerAddress = packet.getAddress();
        int packetLength = packet.getLength();
        byte[] msgBytes = new byte[packetLength];
        System.arraycopy(packet.getData(), 0, msgBytes, 0, packetLength);
        if (this.sipStack.isLoggingEnabled()) {
            stackLogger = this.sipStack.getStackLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UDPMessageChannel: processIncomingDataPacket : peerAddress = ");
            stringBuilder.append(this.peerAddress.getHostAddress());
            stringBuilder.append(Separators.SLASH);
            stringBuilder.append(packet.getPort());
            stringBuilder.append(" Length = ");
            stringBuilder.append(packetLength);
            stackLogger.logDebug(stringBuilder.toString());
        }
        SIPMessage sipMessage = null;
        String badmsg;
        StringBuilder stringBuilder2;
        try {
            this.receptionTime = System.currentTimeMillis();
            sipMessage = this.myParser.parseSIPMessage(msgBytes);
            this.myParser = null;
            if (sipMessage == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Rejecting message !  + Null message parsed.");
                }
                Hashtable hashtable = this.pingBackRecord;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(packet.getAddress().getHostAddress());
                stringBuilder3.append(Separators.COLON);
                stringBuilder3.append(packet.getPort());
                if (hashtable.get(stringBuilder3.toString()) == null) {
                    byte[] retval = "\r\n\r\n".getBytes();
                    ((UDPMessageProcessor) this.messageProcessor).sock.send(new DatagramPacket(retval, 0, retval.length, packet.getAddress(), packet.getPort()));
                    this.sipStack.getTimer().schedule(new PingBackTimerTask(packet.getAddress().getHostAddress(), packet.getPort()), 1000);
                }
                return;
            }
            ViaList viaList = sipMessage.getViaHeaders();
            if (sipMessage.getFrom() == null || sipMessage.getTo() == null || sipMessage.getCallId() == null || sipMessage.getCSeq() == null || sipMessage.getViaHeaders() == null) {
                badmsg = new String(msgBytes);
                if (this.sipStack.isLoggingEnabled()) {
                    StackLogger stackLogger2 = this.sipStack.getStackLogger();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("bad message ");
                    stringBuilder2.append(badmsg);
                    stackLogger2.logError(stringBuilder2.toString());
                    stackLogger2 = this.sipStack.getStackLogger();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(">>> Dropped Bad Msg From = ");
                    stringBuilder2.append(sipMessage.getFrom());
                    stringBuilder2.append("To = ");
                    stringBuilder2.append(sipMessage.getTo());
                    stringBuilder2.append("CallId = ");
                    stringBuilder2.append(sipMessage.getCallId());
                    stringBuilder2.append("CSeq = ");
                    stringBuilder2.append(sipMessage.getCSeq());
                    stringBuilder2.append("Via = ");
                    stringBuilder2.append(sipMessage.getViaHeaders());
                    stackLogger2.logError(stringBuilder2.toString());
                }
                return;
            }
            if (sipMessage instanceof SIPRequest) {
                Via v = (Via) viaList.getFirst();
                Hop hop = this.sipStack.addressResolver.resolveAddress(v.getHop());
                this.peerPort = hop.getPort();
                this.peerProtocol = v.getTransport();
                this.peerPacketSourceAddress = packet.getAddress();
                this.peerPacketSourcePort = packet.getPort();
                try {
                    this.peerAddress = packet.getAddress();
                    boolean hasRPort = v.hasParameter("rport");
                    if (hasRPort || !hop.getHost().equals(this.peerAddress.getHostAddress())) {
                        v.setParameter("received", this.peerAddress.getHostAddress());
                    }
                    if (hasRPort) {
                        v.setParameter("rport", Integer.toString(this.peerPacketSourcePort));
                    }
                } catch (ParseException ex1) {
                    InternalErrorHandler.handleException(ex1);
                }
            } else {
                this.peerPacketSourceAddress = packet.getAddress();
                this.peerPacketSourcePort = packet.getPort();
                this.peerAddress = packet.getAddress();
                this.peerPort = packet.getPort();
                this.peerProtocol = ((Via) viaList.getFirst()).getTransport();
            }
            processMessage(sipMessage);
        } catch (ParseException ex) {
            this.myParser = null;
            if (this.sipStack.isLoggingEnabled()) {
                stackLogger = this.sipStack.getStackLogger();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Rejecting message !  ");
                stringBuilder2.append(new String(msgBytes));
                stackLogger.logDebug(stringBuilder2.toString());
                stackLogger = this.sipStack.getStackLogger();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("error message ");
                stringBuilder2.append(ex.getMessage());
                stackLogger.logDebug(stringBuilder2.toString());
                this.sipStack.getStackLogger().logException(ex);
            }
            String msgString = new String(msgBytes, 0, packetLength);
            if (!(msgString.startsWith("SIP/") || msgString.startsWith("ACK "))) {
                badmsg = createBadReqRes(msgString, ex);
                if (badmsg != null) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Sending automatic 400 Bad Request:");
                        this.sipStack.getStackLogger().logDebug(badmsg);
                    }
                    try {
                        sendMessage(badmsg.getBytes(), this.peerAddress, packet.getPort(), ListeningPoint.UDP, false);
                    } catch (IOException e) {
                        this.sipStack.getStackLogger().logException(e);
                    }
                } else if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Could not formulate automatic 400 Bad Request");
                }
            }
        }
    }

    public void processMessage(SIPMessage sipMessage) {
        StackLogger stackLogger;
        StringBuilder stringBuilder;
        if (sipMessage instanceof SIPRequest) {
            SIPRequest sipRequest = (SIPRequest) sipMessage;
            if (this.sipStack.getStackLogger().isLoggingEnabled(16)) {
                ServerLogger serverLogger = this.sipStack.serverLogger;
                String hostPort = getPeerHostPort().toString();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(getHost());
                stringBuilder2.append(Separators.COLON);
                stringBuilder2.append(this.myPort);
                serverLogger.logMessage(sipMessage, hostPort, stringBuilder2.toString(), false, this.receptionTime);
            }
            ServerRequestInterface sipServerRequest = this.sipStack.newSIPServerRequest(sipRequest, this);
            if (sipServerRequest == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logWarning("Null request interface returned -- dropping request");
                }
                return;
            }
            if (this.sipStack.isLoggingEnabled()) {
                stackLogger = this.sipStack.getStackLogger();
                stringBuilder = new StringBuilder();
                stringBuilder.append("About to process ");
                stringBuilder.append(sipRequest.getFirstLine());
                stringBuilder.append(Separators.SLASH);
                stringBuilder.append(sipServerRequest);
                stackLogger.logDebug(stringBuilder.toString());
            }
            try {
                sipServerRequest.processRequest(sipRequest, this);
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger = this.sipStack.getStackLogger();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Done processing ");
                    stringBuilder.append(sipRequest.getFirstLine());
                    stringBuilder.append(Separators.SLASH);
                    stringBuilder.append(sipServerRequest);
                    stackLogger.logDebug(stringBuilder.toString());
                }
            } finally {
                if ((sipServerRequest instanceof SIPTransaction) && !((SIPServerTransaction) sipServerRequest).passToListener()) {
                    ((SIPTransaction) sipServerRequest).releaseSem();
                }
            }
        } else {
            SIPResponse sipResponse = (SIPResponse) sipMessage;
            try {
                sipResponse.checkHeaders();
                ServerResponseInterface sipServerResponse = this.sipStack.newSIPServerResponse(sipResponse, this);
                if (sipServerResponse != null) {
                    try {
                        if (!(sipServerResponse instanceof SIPClientTransaction) || ((SIPClientTransaction) sipServerResponse).checkFromTag(sipResponse)) {
                            sipServerResponse.processResponse(sipResponse, this);
                            if ((sipServerResponse instanceof SIPTransaction) && !((SIPTransaction) sipServerResponse).passToListener()) {
                                ((SIPTransaction) sipServerResponse).releaseSem();
                            }
                        } else {
                            if (this.sipStack.isLoggingEnabled()) {
                                stackLogger = this.sipStack.getStackLogger();
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Dropping response message with invalid tag >>> ");
                                stringBuilder.append(sipResponse);
                                stackLogger.logError(stringBuilder.toString());
                            }
                            if ((sipServerResponse instanceof SIPTransaction) && !((SIPTransaction) sipServerResponse).passToListener()) {
                                ((SIPTransaction) sipServerResponse).releaseSem();
                            }
                        }
                    } catch (Throwable th) {
                        if ((sipServerResponse instanceof SIPTransaction) && !((SIPTransaction) sipServerResponse).passToListener()) {
                            ((SIPTransaction) sipServerResponse).releaseSem();
                        }
                    }
                } else if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("null sipServerResponse!");
                }
            } catch (ParseException e) {
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger = this.sipStack.getStackLogger();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Dropping Badly formatted response message >>> ");
                    stringBuilder.append(sipResponse);
                    stackLogger.logError(stringBuilder.toString());
                }
            }
        }
    }

    public void handleException(ParseException ex, SIPMessage sipMessage, Class hdrClass, String header, String message) throws ParseException {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logException(ex);
        }
        if (hdrClass == null || !(hdrClass.equals(From.class) || hdrClass.equals(To.class) || hdrClass.equals(CSeq.class) || hdrClass.equals(Via.class) || hdrClass.equals(CallID.class) || hdrClass.equals(RequestLine.class) || hdrClass.equals(StatusLine.class))) {
            sipMessage.addUnparsed(header);
            return;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logError("BAD MESSAGE!");
            this.sipStack.getStackLogger().logError(message);
        }
        throw ex;
    }

    public void sendMessage(SIPMessage sipMessage) throws IOException {
        if (this.sipStack.isLoggingEnabled() && this.sipStack.isLogStackTraceOnMessageSend()) {
            if (!(sipMessage instanceof SIPRequest) || ((SIPRequest) sipMessage).getRequestLine() == null) {
                this.sipStack.getStackLogger().logStackTrace(16);
            } else {
                this.sipStack.getStackLogger().logStackTrace(16);
            }
        }
        long time = System.currentTimeMillis();
        try {
            for (MessageProcessor messageProcessor : this.sipStack.getMessageProcessors()) {
                if (messageProcessor.getIpAddress().equals(this.peerAddress) && messageProcessor.getPort() == this.peerPort && messageProcessor.getTransport().equals(this.peerProtocol)) {
                    MessageChannel messageChannel = messageProcessor.createMessageChannel(this.peerAddress, this.peerPort);
                    if (messageChannel instanceof RawMessageChannel) {
                        ((RawMessageChannel) messageChannel).processMessage(sipMessage);
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("Self routing message");
                        }
                        if (this.sipStack.getStackLogger().isLoggingEnabled(16) && !sipMessage.isNullRequest()) {
                            logMessage(sipMessage, this.peerAddress, this.peerPort, time);
                        } else if (this.sipStack.getStackLogger().isLoggingEnabled(32)) {
                            this.sipStack.getStackLogger().logDebug("Sent EMPTY Message");
                        }
                        return;
                    }
                }
            }
            sendMessage(sipMessage.encodeAsBytes(getTransport()), this.peerAddress, this.peerPort, this.peerProtocol, sipMessage instanceof SIPRequest);
            if (this.sipStack.getStackLogger().isLoggingEnabled(16) && !sipMessage.isNullRequest()) {
                logMessage(sipMessage, this.peerAddress, this.peerPort, time);
            } else if (this.sipStack.getStackLogger().isLoggingEnabled(32)) {
                this.sipStack.getStackLogger().logDebug("Sent EMPTY Message");
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex2) {
            this.sipStack.getStackLogger().logError("An exception occured while sending message", ex2);
            throw new IOException("An exception occured while sending message");
        } catch (Throwable th) {
            if (this.sipStack.getStackLogger().isLoggingEnabled(16) && !sipMessage.isNullRequest()) {
                logMessage(sipMessage, this.peerAddress, this.peerPort, time);
            } else if (this.sipStack.getStackLogger().isLoggingEnabled(32)) {
                this.sipStack.getStackLogger().logDebug("Sent EMPTY Message");
            }
        }
    }

    protected void sendMessage(byte[] msg, InetAddress peerAddress, int peerPort, boolean reConnect) throws IOException {
        if (this.sipStack.isLoggingEnabled() && this.sipStack.isLogStackTraceOnMessageSend()) {
            this.sipStack.getStackLogger().logStackTrace(16);
        }
        StackLogger stackLogger;
        StringBuilder stringBuilder;
        if (peerPort == -1) {
            if (this.sipStack.isLoggingEnabled()) {
                stackLogger = this.sipStack.getStackLogger();
                stringBuilder = new StringBuilder();
                stringBuilder.append(getClass().getName());
                stringBuilder.append(":sendMessage: Dropping reply!");
                stackLogger.logDebug(stringBuilder.toString());
            }
            throw new IOException("Receiver port not set ");
        }
        if (this.sipStack.isLoggingEnabled()) {
            stackLogger = this.sipStack.getStackLogger();
            stringBuilder = new StringBuilder();
            stringBuilder.append("sendMessage ");
            stringBuilder.append(peerAddress.getHostAddress());
            stringBuilder.append(Separators.SLASH);
            stringBuilder.append(peerPort);
            stringBuilder.append("\nmessageSize =  ");
            stringBuilder.append(msg.length);
            stringBuilder.append(" message = ");
            stringBuilder.append(new String(msg));
            stackLogger.logDebug(stringBuilder.toString());
            this.sipStack.getStackLogger().logDebug("*******************\n");
        }
        DatagramPacket reply = new DatagramPacket(msg, msg.length, peerAddress, peerPort);
        boolean created = false;
        try {
            DatagramSocket sock;
            if (this.sipStack.udpFlag) {
                sock = ((UDPMessageProcessor) this.messageProcessor).sock;
            } else {
                sock = new DatagramSocket();
                created = true;
            }
            sock.send(reply);
            if (created) {
                sock.close();
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex2) {
            InternalErrorHandler.handleException(ex2);
        }
    }

    protected void sendMessage(byte[] msg, InetAddress peerAddress, int peerPort, String peerProtocol, boolean retry) throws IOException {
        StackLogger stackLogger;
        StringBuilder stringBuilder;
        if (peerPort == -1) {
            if (this.sipStack.isLoggingEnabled()) {
                stackLogger = this.sipStack.getStackLogger();
                stringBuilder = new StringBuilder();
                stringBuilder.append(getClass().getName());
                stringBuilder.append(":sendMessage: Dropping reply!");
                stackLogger.logDebug(stringBuilder.toString());
            }
            throw new IOException("Receiver port not set ");
        }
        if (this.sipStack.isLoggingEnabled()) {
            stackLogger = this.sipStack.getStackLogger();
            stringBuilder = new StringBuilder();
            stringBuilder.append(":sendMessage ");
            stringBuilder.append(peerAddress.getHostAddress());
            stringBuilder.append(Separators.SLASH);
            stringBuilder.append(peerPort);
            stringBuilder.append("\n messageSize = ");
            stringBuilder.append(msg.length);
            stackLogger.logDebug(stringBuilder.toString());
        }
        if (peerProtocol.compareToIgnoreCase(ListeningPoint.UDP) == 0) {
            DatagramPacket reply = new DatagramPacket(msg, msg.length, peerAddress, peerPort);
            try {
                DatagramSocket sock;
                if (this.sipStack.udpFlag) {
                    sock = ((UDPMessageProcessor) this.messageProcessor).sock;
                } else {
                    sock = this.sipStack.getNetworkLayer().createDatagramSocket();
                }
                if (this.sipStack.isLoggingEnabled()) {
                    StackLogger stackLogger2 = this.sipStack.getStackLogger();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("sendMessage ");
                    stringBuilder2.append(peerAddress.getHostAddress());
                    stringBuilder2.append(Separators.SLASH);
                    stringBuilder2.append(peerPort);
                    stringBuilder2.append(Separators.RETURN);
                    stringBuilder2.append(new String(msg));
                    stackLogger2.logDebug(stringBuilder2.toString());
                }
                sock.send(reply);
                if (!this.sipStack.udpFlag) {
                    sock.close();
                    return;
                }
                return;
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex2) {
                InternalErrorHandler.handleException(ex2);
                return;
            }
        }
        OutputStream myOutputStream = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), peerAddress, peerPort, ParameterNames.TCP, msg, retry, this).getOutputStream();
        myOutputStream.write(msg, 0, msg.length);
        myOutputStream.flush();
    }

    public SIPTransactionStack getSIPStack() {
        return this.sipStack;
    }

    public String getTransport() {
        return ParameterNames.UDP;
    }

    public String getHost() {
        return this.messageProcessor.getIpAddress().getHostAddress();
    }

    public int getPort() {
        return ((UDPMessageProcessor) this.messageProcessor).getPort();
    }

    public String getPeerName() {
        return this.peerAddress.getHostName();
    }

    public String getPeerAddress() {
        return this.peerAddress.getHostAddress();
    }

    protected InetAddress getPeerInetAddress() {
        return this.peerAddress;
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        boolean retval;
        if (getClass().equals(other.getClass())) {
            retval = getKey().equals(((UDPMessageChannel) other).getKey());
        } else {
            retval = false;
        }
        return retval;
    }

    public String getKey() {
        return MessageChannel.getKey(this.peerAddress, this.peerPort, ListeningPoint.UDP);
    }

    public int getPeerPacketSourcePort() {
        return this.peerPacketSourcePort;
    }

    public InetAddress getPeerPacketSourceAddress() {
        return this.peerPacketSourceAddress;
    }

    public String getViaHost() {
        return this.myAddress;
    }

    public int getViaPort() {
        return this.myPort;
    }

    public boolean isReliable() {
        return false;
    }

    public boolean isSecure() {
        return false;
    }

    public int getPeerPort() {
        return this.peerPort;
    }

    public String getPeerProtocol() {
        return this.peerProtocol;
    }

    public void close() {
    }
}
