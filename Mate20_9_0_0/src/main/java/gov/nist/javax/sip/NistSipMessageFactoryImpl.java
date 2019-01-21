package gov.nist.javax.sip;

import gov.nist.core.Separators;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPTransaction;
import gov.nist.javax.sip.stack.ServerRequestInterface;
import gov.nist.javax.sip.stack.ServerResponseInterface;
import gov.nist.javax.sip.stack.StackMessageFactory;
import javax.sip.TransactionState;

class NistSipMessageFactoryImpl implements StackMessageFactory {
    private SipStackImpl sipStack;

    public ServerRequestInterface newSIPServerRequest(SIPRequest sipRequest, MessageChannel messageChannel) {
        if (messageChannel == null || sipRequest == null) {
            throw new IllegalArgumentException("Null Arg!");
        }
        DialogFilter retval = new DialogFilter((SipStackImpl) messageChannel.getSIPStack());
        if (messageChannel instanceof SIPTransaction) {
            retval.transactionChannel = (SIPTransaction) messageChannel;
        }
        retval.listeningPoint = messageChannel.getMessageProcessor().getListeningPoint();
        if (retval.listeningPoint == null) {
            return null;
        }
        if (this.sipStack.isLoggingEnabled()) {
            StackLogger stackLogger = this.sipStack.getStackLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Returning request interface for ");
            stringBuilder.append(sipRequest.getFirstLine());
            stringBuilder.append(Separators.SP);
            stringBuilder.append(retval);
            stringBuilder.append(" messageChannel = ");
            stringBuilder.append(messageChannel);
            stackLogger.logDebug(stringBuilder.toString());
        }
        return retval;
    }

    public ServerResponseInterface newSIPServerResponse(SIPResponse sipResponse, MessageChannel messageChannel) {
        StackLogger stackLogger;
        SIPTransaction tr = messageChannel.getSIPStack().findTransaction(sipResponse, null);
        if (this.sipStack.isLoggingEnabled()) {
            stackLogger = this.sipStack.getStackLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Found Transaction ");
            stringBuilder.append(tr);
            stringBuilder.append(" for ");
            stringBuilder.append(sipResponse);
            stackLogger.logDebug(stringBuilder.toString());
        }
        if (tr != null) {
            if (tr.getState() == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dropping response - null transaction state");
                }
                return null;
            } else if (TransactionState.COMPLETED == tr.getState() && sipResponse.getStatusCode() / 100 == 1) {
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger = this.sipStack.getStackLogger();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Dropping response - late arriving ");
                    stringBuilder2.append(sipResponse.getStatusCode());
                    stackLogger.logDebug(stringBuilder2.toString());
                }
                return null;
            }
        }
        DialogFilter retval = new DialogFilter(this.sipStack);
        retval.transactionChannel = tr;
        retval.listeningPoint = messageChannel.getMessageProcessor().getListeningPoint();
        return retval;
    }

    public NistSipMessageFactoryImpl(SipStackImpl sipStackImpl) {
        this.sipStack = sipStackImpl;
    }
}
