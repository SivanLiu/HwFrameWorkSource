package gov.nist.javax.sip.stack;

import gov.nist.core.Host;
import gov.nist.core.HostPort;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.core.ServerLogger;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.ContentType;
import gov.nist.javax.sip.header.ParameterNames;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import javax.sip.address.Hop;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ServerHeader;
import org.ccil.cowan.tagsoup.HTMLModels;

public abstract class MessageChannel {
    protected transient MessageProcessor messageProcessor;
    protected int useCount;

    public abstract void close();

    public abstract String getKey();

    public abstract String getPeerAddress();

    protected abstract InetAddress getPeerInetAddress();

    public abstract InetAddress getPeerPacketSourceAddress();

    public abstract int getPeerPacketSourcePort();

    public abstract int getPeerPort();

    protected abstract String getPeerProtocol();

    public abstract SIPTransactionStack getSIPStack();

    public abstract String getTransport();

    public abstract String getViaHost();

    public abstract int getViaPort();

    public abstract boolean isReliable();

    public abstract boolean isSecure();

    public abstract void sendMessage(SIPMessage sIPMessage) throws IOException;

    protected abstract void sendMessage(byte[] bArr, InetAddress inetAddress, int i, boolean z) throws IOException;

    protected void uncache() {
    }

    public String getHost() {
        return getMessageProcessor().getIpAddress().getHostAddress();
    }

    public int getPort() {
        if (this.messageProcessor != null) {
            return this.messageProcessor.getPort();
        }
        return -1;
    }

    public void sendMessage(SIPMessage sipMessage, Hop hop) throws IOException {
        long time = System.currentTimeMillis();
        InetAddress hopAddr = InetAddress.getByName(hop.getHost());
        try {
            for (MessageProcessor messageProcessor : getSIPStack().getMessageProcessors()) {
                if (messageProcessor.getIpAddress().equals(hopAddr) && messageProcessor.getPort() == hop.getPort() && messageProcessor.getTransport().equals(hop.getTransport())) {
                    MessageChannel messageChannel = messageProcessor.createMessageChannel(hopAddr, hop.getPort());
                    if (messageChannel instanceof RawMessageChannel) {
                        ((RawMessageChannel) messageChannel).processMessage(sipMessage);
                        if (getSIPStack().isLoggingEnabled()) {
                            getSIPStack().getStackLogger().logDebug("Self routing message");
                        }
                        if (getSIPStack().getStackLogger().isLoggingEnabled(16)) {
                            logMessage(sipMessage, hopAddr, hop.getPort(), time);
                        }
                        return;
                    }
                }
            }
            sendMessage(sipMessage.encodeAsBytes(getTransport()), hopAddr, hop.getPort(), sipMessage instanceof SIPRequest);
            if (getSIPStack().getStackLogger().isLoggingEnabled(16)) {
                logMessage(sipMessage, hopAddr, hop.getPort(), time);
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception ex) {
            if (getSIPStack().getStackLogger().isLoggingEnabled(4)) {
                getSIPStack().getStackLogger().logError("Error self routing message cause by: ", ex);
            }
            throw new IOException("Error self routing message");
        } catch (Throwable th) {
            Throwable th2 = th;
            if (getSIPStack().getStackLogger().isLoggingEnabled(16)) {
                logMessage(sipMessage, hopAddr, hop.getPort(), time);
            }
        }
    }

    public void sendMessage(SIPMessage sipMessage, InetAddress receiverAddress, int receiverPort) throws IOException {
        long time = System.currentTimeMillis();
        sendMessage(sipMessage.encodeAsBytes(getTransport()), receiverAddress, receiverPort, sipMessage instanceof SIPRequest);
        logMessage(sipMessage, receiverAddress, receiverPort, time);
    }

    public String getRawIpSourceAddress() {
        try {
            return InetAddress.getByName(getPeerAddress()).getHostAddress();
        } catch (Exception ex) {
            InternalErrorHandler.handleException(ex);
            return null;
        }
    }

    public static String getKey(InetAddress inetAddr, int port, String transport) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(transport);
        stringBuilder.append(Separators.COLON);
        stringBuilder.append(inetAddr.getHostAddress());
        stringBuilder.append(Separators.COLON);
        stringBuilder.append(port);
        return stringBuilder.toString().toLowerCase();
    }

    public static String getKey(HostPort hostPort, String transport) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(transport);
        stringBuilder.append(Separators.COLON);
        stringBuilder.append(hostPort.getHost().getHostname());
        stringBuilder.append(Separators.COLON);
        stringBuilder.append(hostPort.getPort());
        return stringBuilder.toString().toLowerCase();
    }

    public HostPort getHostPort() {
        HostPort retval = new HostPort();
        retval.setHost(new Host(getHost()));
        retval.setPort(getPort());
        return retval;
    }

    public HostPort getPeerHostPort() {
        HostPort retval = new HostPort();
        retval.setHost(new Host(getPeerAddress()));
        retval.setPort(getPeerPort());
        return retval;
    }

    public Via getViaHeader() {
        Via channelViaHeader = new Via();
        try {
            channelViaHeader.setTransport(getTransport());
        } catch (ParseException e) {
        }
        channelViaHeader.setSentBy(getHostPort());
        return channelViaHeader;
    }

    public HostPort getViaHostPort() {
        HostPort retval = new HostPort();
        retval.setHost(new Host(getViaHost()));
        retval.setPort(getViaPort());
        return retval;
    }

    protected void logMessage(SIPMessage sipMessage, InetAddress address, int port, long time) {
        if (getSIPStack().getStackLogger().isLoggingEnabled(16)) {
            if (port == -1) {
                port = 5060;
            }
            ServerLogger serverLogger = getSIPStack().serverLogger;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getHost());
            stringBuilder.append(Separators.COLON);
            stringBuilder.append(getPort());
            String stringBuilder2 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(address.getHostAddress().toString());
            stringBuilder.append(Separators.COLON);
            stringBuilder.append(port);
            serverLogger.logMessage(sipMessage, stringBuilder2, stringBuilder.toString(), true, time);
        }
    }

    public void logResponse(SIPResponse sipResponse, long receptionTime, String status) {
        int peerport = getPeerPort();
        if (peerport == 0 && sipResponse.getContactHeaders() != null) {
            peerport = ((AddressImpl) ((ContactHeader) sipResponse.getContactHeaders().getFirst()).getAddress()).getPort();
        }
        String from = new StringBuilder();
        from.append(getPeerAddress().toString());
        from.append(Separators.COLON);
        from.append(peerport);
        from = from.toString();
        String to = new StringBuilder();
        to.append(getHost());
        to.append(Separators.COLON);
        to.append(getPort());
        getSIPStack().serverLogger.logMessage(sipResponse, from, to.toString(), status, false, receptionTime);
    }

    protected final String createBadReqRes(String badReq, ParseException pe) {
        StringBuffer buf = new StringBuffer(HTMLModels.M_FRAME);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SIP/2.0 400 Bad Request (");
        stringBuilder.append(pe.getLocalizedMessage());
        stringBuilder.append(')');
        buf.append(stringBuilder.toString());
        if (!copyViaHeaders(badReq, buf) || !copyHeader("CSeq", badReq, buf) || !copyHeader("Call-ID", badReq, buf) || !copyHeader("From", badReq, buf) || !copyHeader("To", badReq, buf)) {
            return null;
        }
        int toStart = buf.indexOf("To");
        if (toStart != -1 && buf.indexOf(ParameterNames.TAG, toStart) == -1) {
            buf.append(";tag=badreq");
        }
        ServerHeader s = MessageFactoryImpl.getDefaultServerHeader();
        if (s != null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Separators.NEWLINE);
            stringBuilder2.append(s.toString());
            buf.append(stringBuilder2.toString());
        }
        int clength = badReq.length();
        StringBuilder stringBuilder3;
        if (!(this instanceof UDPMessageChannel) || (((buf.length() + clength) + "Content-Type".length()) + ": message/sipfrag\r\n".length()) + "Content-Length".length() < 1300) {
            ContentTypeHeader cth = new ContentType("message", "sipfrag");
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(Separators.NEWLINE);
            stringBuilder3.append(cth.toString());
            buf.append(stringBuilder3.toString());
            ContentLength clengthHeader = new ContentLength(clength);
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(Separators.NEWLINE);
            stringBuilder4.append(clengthHeader.toString());
            buf.append(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("\r\n\r\n");
            stringBuilder4.append(badReq);
            buf.append(stringBuilder4.toString());
        } else {
            ContentLength clengthHeader2 = new ContentLength(0);
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(Separators.NEWLINE);
            stringBuilder3.append(clengthHeader2.toString());
            buf.append(stringBuilder3.toString());
        }
        return buf.toString();
    }

    private static final boolean copyHeader(String name, String fromReq, StringBuffer buf) {
        int start = fromReq.indexOf(name);
        if (start != -1) {
            int end = fromReq.indexOf(Separators.NEWLINE, start);
            if (end != -1) {
                buf.append(fromReq.subSequence(start - 2, end));
                return true;
            }
        }
        return false;
    }

    private static final boolean copyViaHeaders(String fromReq, StringBuffer buf) {
        int start = fromReq.indexOf("Via");
        boolean found = false;
        while (start != -1) {
            int end = fromReq.indexOf(Separators.NEWLINE, start);
            if (end == -1) {
                return false;
            }
            buf.append(fromReq.subSequence(start - 2, end));
            found = true;
            start = fromReq.indexOf("Via", end);
        }
        return found;
    }

    public MessageProcessor getMessageProcessor() {
        return this.messageProcessor;
    }
}
