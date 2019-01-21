package gov.nist.javax.sip;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.address.ParameterNames;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Contact;
import gov.nist.javax.sip.header.Event;
import gov.nist.javax.sip.header.RetryAfter;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPTransaction;
import gov.nist.javax.sip.stack.SIPTransactionStack;
import gov.nist.javax.sip.stack.ServerRequestInterface;
import gov.nist.javax.sip.stack.ServerResponseInterface;
import java.io.IOException;
import javax.sip.ClientTransaction;
import javax.sip.DialogState;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.TransactionState;
import javax.sip.header.Header;
import javax.sip.header.ReferToHeader;
import javax.sip.header.ServerHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

class DialogFilter implements ServerRequestInterface, ServerResponseInterface {
    protected ListeningPointImpl listeningPoint;
    private SipStackImpl sipStack;
    protected SIPTransaction transactionChannel;

    public DialogFilter(SipStackImpl sipStack) {
        this.sipStack = sipStack;
    }

    private void sendRequestPendingResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        Response sipResponse = sipRequest.createResponse(Response.REQUEST_PENDING);
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader((Header) serverHeader);
        }
        try {
            RetryAfter retryAfter = new RetryAfter();
            retryAfter.setRetryAfter(1);
            sipResponse.setHeader((Header) retryAfter);
            if (sipRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(transaction);
            }
            transaction.sendResponse(sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendBadRequestResponse(SIPRequest sipRequest, SIPServerTransaction transaction, String reasonPhrase) {
        Response sipResponse = sipRequest.createResponse(Response.BAD_REQUEST);
        if (reasonPhrase != null) {
            sipResponse.setReasonPhrase(reasonPhrase);
        }
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader((Header) serverHeader);
        }
        try {
            if (sipRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(transaction);
            }
            transaction.sendResponse(sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendCallOrTransactionDoesNotExistResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        Response sipResponse = sipRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader((Header) serverHeader);
        }
        try {
            if (sipRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(transaction);
            }
            transaction.sendResponse(sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendLoopDetectedResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        Response sipResponse = sipRequest.createResponse(Response.LOOP_DETECTED);
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader((Header) serverHeader);
        }
        try {
            this.sipStack.addTransactionPendingAck(transaction);
            transaction.sendResponse(sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendServerInternalErrorResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Sending 500 response for out of sequence message");
        }
        Response sipResponse = sipRequest.createResponse(500);
        sipResponse.setReasonPhrase("Request out of order");
        if (MessageFactoryImpl.getDefaultServerHeader() != null) {
            sipResponse.setHeader((Header) MessageFactoryImpl.getDefaultServerHeader());
        }
        try {
            RetryAfter retryAfter = new RetryAfter();
            retryAfter.setRetryAfter(10);
            sipResponse.setHeader((Header) retryAfter);
            this.sipStack.addTransactionPendingAck(transaction);
            transaction.sendResponse(sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    public void processRequest(SIPRequest sipRequest, MessageChannel incomingMessageChannel) {
        StackLogger stackLogger;
        SipException sipException;
        SIPRequest sIPRequest = sipRequest;
        if (this.sipStack.isLoggingEnabled()) {
            stackLogger = this.sipStack.getStackLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PROCESSING INCOMING REQUEST ");
            stringBuilder.append(sIPRequest);
            stringBuilder.append(" transactionChannel = ");
            stringBuilder.append(this.transactionChannel);
            stringBuilder.append(" listening point = ");
            stringBuilder.append(this.listeningPoint.getIPAddress());
            stringBuilder.append(Separators.COLON);
            stringBuilder.append(this.listeningPoint.getPort());
            stackLogger.logDebug(stringBuilder.toString());
        }
        if (this.listeningPoint == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping message: No listening point registered!");
            }
            return;
        }
        SipStackImpl sipStack = (SipStackImpl) this.transactionChannel.getSIPStack();
        SipProviderImpl sipProvider = this.listeningPoint.getProvider();
        if (sipProvider == null) {
            if (sipStack.isLoggingEnabled()) {
                sipStack.getStackLogger().logDebug("No provider - dropping !!");
            }
            return;
        }
        int contactPort;
        String contactTransport;
        if (sipStack == null) {
            InternalErrorHandler.handleException("Egads! no sip stack!");
        }
        SIPServerTransaction transaction = (SIPServerTransaction) this.transactionChannel;
        if (transaction != null && sipStack.isLoggingEnabled()) {
            stackLogger = sipStack.getStackLogger();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transaction state = ");
            stringBuilder2.append(transaction.getState());
            stackLogger.logDebug(stringBuilder2.toString());
        }
        String dialogId = sIPRequest.getDialogId(true);
        SIPDialog dialog = sipStack.getDialog(dialogId);
        if (!(dialog == null || sipProvider == dialog.getSipProvider())) {
            Contact contact = dialog.getMyContactHeader();
            if (contact != null) {
                SipUri contactUri = (SipUri) contact.getAddress().getURI();
                String ipAddress = contactUri.getHost();
                contactPort = contactUri.getPort();
                contactTransport = contactUri.getTransportParam();
                if (contactTransport == null) {
                    contactTransport = ParameterNames.UDP;
                }
                if (contactPort == -1) {
                    if (contactTransport.equals(ParameterNames.UDP) || contactTransport.equals(ParameterNames.TCP)) {
                        contactPort = 5060;
                    } else {
                        contactPort = 5061;
                    }
                }
                if (!(ipAddress == null || (ipAddress.equals(this.listeningPoint.getIPAddress()) && contactPort == this.listeningPoint.getPort()))) {
                    if (sipStack.isLoggingEnabled()) {
                        StackLogger stackLogger2 = sipStack.getStackLogger();
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("nulling dialog -- listening point mismatch!  ");
                        stringBuilder3.append(contactPort);
                        stringBuilder3.append("  lp port = ");
                        stringBuilder3.append(this.listeningPoint.getPort());
                        stackLogger2.logDebug(stringBuilder3.toString());
                    }
                    dialog = null;
                }
            }
        }
        if (sipProvider.isAutomaticDialogSupportEnabled() && sipProvider.isDialogErrorsAutomaticallyHandled() && sipRequest.getToTag() == null && sipStack.findMergedTransaction(sIPRequest) != null) {
            sendLoopDetectedResponse(sIPRequest, transaction);
            return;
        }
        StackLogger stackLogger3;
        StringBuilder stringBuilder4;
        StringBuilder stringBuilder5;
        RequestEvent sipEvent;
        if (sipStack.isLoggingEnabled()) {
            stackLogger3 = sipStack.getStackLogger();
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("dialogId = ");
            stringBuilder6.append(dialogId);
            stackLogger3.logDebug(stringBuilder6.toString());
            stackLogger3 = sipStack.getStackLogger();
            stringBuilder6 = new StringBuilder();
            stringBuilder6.append("dialog = ");
            stringBuilder6.append(dialog);
            stackLogger3.logDebug(stringBuilder6.toString());
        }
        if (!(sIPRequest.getHeader("Route") == null || transaction.getDialog() == null)) {
            RouteList routes = sipRequest.getRouteHeaders();
            SipUri uri = (SipUri) ((Route) routes.getFirst()).getAddress().getURI();
            if (uri.getHostPort().hasPort()) {
                contactPort = uri.getHostPort().getPort();
            } else if (this.listeningPoint.getTransport().equalsIgnoreCase(ListeningPoint.TLS)) {
                contactPort = 5061;
            } else {
                contactPort = 5060;
            }
            contactTransport = uri.getHost();
            if ((contactTransport.equals(this.listeningPoint.getIPAddress()) || contactTransport.equalsIgnoreCase(this.listeningPoint.getSentBy())) && port == this.listeningPoint.getPort()) {
                if (routes.size() == 1) {
                    sIPRequest.removeHeader("Route");
                } else {
                    routes.removeFirst();
                }
            }
        }
        SIPServerTransaction st;
        if (sipRequest.getMethod().equals(Request.REFER) && dialog != null && sipProvider.isDialogErrorsAutomaticallyHandled()) {
            if (((ReferToHeader) sIPRequest.getHeader(ReferToHeader.NAME)) == null) {
                sendBadRequestResponse(sIPRequest, transaction, "Refer-To header is missing");
                return;
            }
            SIPTransaction lastTransaction = dialog.getLastTransaction();
            if (lastTransaction != null && sipProvider.isDialogErrorsAutomaticallyHandled()) {
                SIPRequest lastRequest = (SIPRequest) lastTransaction.getRequest();
                if (lastTransaction instanceof SIPServerTransaction) {
                    if (!dialog.isAckSeen() && lastRequest.getMethod().equals("INVITE")) {
                        sendRequestPendingResponse(sIPRequest, transaction);
                        return;
                    }
                } else if (lastTransaction instanceof SIPClientTransaction) {
                    long cseqno = lastRequest.getCSeqHeader().getSeqNumber();
                    if (lastRequest.getMethod().equals("INVITE") && !dialog.isAckSent(cseqno)) {
                        sendRequestPendingResponse(sIPRequest, transaction);
                        return;
                    }
                }
            }
        } else if (sipRequest.getMethod().equals(Request.UPDATE)) {
            if (sipProvider.isAutomaticDialogSupportEnabled() && dialog == null) {
                sendCallOrTransactionDoesNotExistResponse(sIPRequest, transaction);
                return;
            }
        } else if (sipRequest.getMethod().equals("ACK")) {
            if (transaction == null || !transaction.isInviteTransaction()) {
                if (sipStack.isLoggingEnabled()) {
                    stackLogger3 = sipStack.getStackLogger();
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Processing ACK for dialog ");
                    stringBuilder4.append(dialog);
                    stackLogger3.logDebug(stringBuilder4.toString());
                }
                if (dialog == null) {
                    if (sipStack.isLoggingEnabled()) {
                        stackLogger3 = sipStack.getStackLogger();
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Dialog does not exist ");
                        stringBuilder4.append(sipRequest.getFirstLine());
                        stringBuilder4.append(" isServerTransaction = ");
                        stringBuilder4.append(true);
                        stackLogger3.logDebug(stringBuilder4.toString());
                    }
                    st = sipStack.getRetransmissionAlertTransaction(dialogId);
                    if (st != null && st.isRetransmissionAlertEnabled()) {
                        st.disableRetransmissionAlerts();
                    }
                    SIPServerTransaction ackTransaction = sipStack.findTransactionPendingAck(sIPRequest);
                    if (ackTransaction != null) {
                        if (sipStack.isLoggingEnabled()) {
                            sipStack.getStackLogger().logDebug("Found Tx pending ACK");
                        }
                        try {
                            ackTransaction.setAckSeen();
                            sipStack.removeTransaction(ackTransaction);
                            sipStack.removeTransactionPendingAck(ackTransaction);
                        } catch (Exception ex) {
                            if (sipStack.isLoggingEnabled()) {
                                sipStack.getStackLogger().logError("Problem terminating transaction", ex);
                            }
                        }
                        return;
                    }
                } else if (dialog.handleAck(transaction)) {
                    transaction.passToListener();
                    dialog.addTransaction(transaction);
                    dialog.addRoute(sIPRequest);
                    transaction.setDialog(dialog, dialogId);
                    if (sipRequest.getMethod().equals("INVITE") && sipProvider.isDialogErrorsAutomaticallyHandled()) {
                        sipStack.putInMergeTable(transaction, sIPRequest);
                    }
                    if (sipStack.deliverTerminatedEventForAck) {
                        try {
                            sipStack.addTransaction(transaction);
                            transaction.scheduleAckRemoval();
                        } catch (IOException e) {
                        }
                    } else {
                        transaction.setMapped(true);
                    }
                } else if (dialog.isSequnceNumberValidation()) {
                    if (sipStack.isLoggingEnabled()) {
                        sipStack.getStackLogger().logDebug("Dropping ACK - cannot find a transaction or dialog");
                    }
                    st = sipStack.findTransactionPendingAck(sIPRequest);
                    if (st != null) {
                        if (sipStack.isLoggingEnabled()) {
                            sipStack.getStackLogger().logDebug("Found Tx pending ACK");
                        }
                        try {
                            st.setAckSeen();
                            sipStack.removeTransaction(st);
                            sipStack.removeTransactionPendingAck(st);
                        } catch (Exception ex2) {
                            if (sipStack.isLoggingEnabled()) {
                                sipStack.getStackLogger().logError("Problem terminating transaction", ex2);
                            }
                        }
                    }
                    return;
                } else {
                    if (sipStack.isLoggingEnabled()) {
                        stackLogger3 = sipStack.getStackLogger();
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Dialog exists with loose dialog validation ");
                        stringBuilder4.append(sipRequest.getFirstLine());
                        stringBuilder4.append(" isServerTransaction = ");
                        stringBuilder4.append(true);
                        stringBuilder4.append(" dialog = ");
                        stringBuilder4.append(dialog.getDialogId());
                        stackLogger3.logDebug(stringBuilder4.toString());
                    }
                    SIPServerTransaction st2 = sipStack.getRetransmissionAlertTransaction(dialogId);
                    if (st2 != null && st2.isRetransmissionAlertEnabled()) {
                        st2.disableRetransmissionAlerts();
                    }
                }
            } else if (sipStack.isLoggingEnabled()) {
                sipStack.getStackLogger().logDebug("Processing ACK for INVITE Tx ");
            }
        } else if (sipRequest.getMethod().equals(Request.PRACK)) {
            if (sipStack.isLoggingEnabled()) {
                stackLogger3 = sipStack.getStackLogger();
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Processing PRACK for dialog ");
                stringBuilder4.append(dialog);
                stackLogger3.logDebug(stringBuilder4.toString());
            }
            if (dialog == null && sipProvider.isAutomaticDialogSupportEnabled()) {
                if (sipStack.isLoggingEnabled()) {
                    stackLogger3 = sipStack.getStackLogger();
                    StringBuilder stringBuilder7 = new StringBuilder();
                    stringBuilder7.append("Dialog does not exist ");
                    stringBuilder7.append(sipRequest.getFirstLine());
                    stringBuilder7.append(" isServerTransaction = ");
                    stringBuilder7.append(true);
                    stackLogger3.logDebug(stringBuilder7.toString());
                }
                if (sipStack.isLoggingEnabled()) {
                    sipStack.getStackLogger().logDebug("Sending 481 for PRACK - automatic dialog support is enabled -- cant find dialog!");
                }
                try {
                    sipProvider.sendResponse(sIPRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST));
                } catch (SipException e2) {
                    sipException = e2;
                    sipStack.getStackLogger().logError("error sending response", e2);
                }
                if (transaction != null) {
                    sipStack.removeTransaction(transaction);
                    transaction.releaseSem();
                }
                return;
            } else if (dialog != null) {
                if (dialog.handlePrack(sIPRequest)) {
                    try {
                        sipStack.addTransaction(transaction);
                        dialog.addTransaction(transaction);
                        dialog.addRoute(sIPRequest);
                        transaction.setDialog(dialog, dialogId);
                    } catch (Exception ex22) {
                        InternalErrorHandler.handleException(ex22);
                    }
                } else {
                    if (sipStack.isLoggingEnabled()) {
                        sipStack.getStackLogger().logDebug("Dropping out of sequence PRACK ");
                    }
                    if (transaction != null) {
                        sipStack.removeTransaction(transaction);
                        transaction.releaseSem();
                    }
                    return;
                }
            } else if (sipStack.isLoggingEnabled()) {
                sipStack.getStackLogger().logDebug("Processing PRACK without a DIALOG -- this must be a proxy element");
            }
        } else if (sipRequest.getMethod().equals("BYE")) {
            if (dialog != null && !dialog.isRequestConsumable(sIPRequest)) {
                if (sipStack.isLoggingEnabled()) {
                    stackLogger = sipStack.getStackLogger();
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Dropping out of sequence BYE ");
                    stringBuilder5.append(dialog.getRemoteSeqNumber());
                    stringBuilder5.append(Separators.SP);
                    stringBuilder5.append(sipRequest.getCSeq().getSeqNumber());
                    stackLogger.logDebug(stringBuilder5.toString());
                }
                if (dialog.getRemoteSeqNumber() >= sipRequest.getCSeq().getSeqNumber() && transaction.getState() == TransactionState.TRYING) {
                    sendServerInternalErrorResponse(sIPRequest, transaction);
                }
                if (transaction != null) {
                    sipStack.removeTransaction(transaction);
                }
                return;
            } else if (dialog == null && sipProvider.isAutomaticDialogSupportEnabled()) {
                Response response = sIPRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                response.setReasonPhrase("Dialog Not Found");
                if (sipStack.isLoggingEnabled()) {
                    sipStack.getStackLogger().logDebug("dropping request -- automatic dialog support enabled and dialog does not exist!");
                }
                try {
                    transaction.sendResponse(response);
                } catch (SipException e22) {
                    sipException = e22;
                    sipStack.getStackLogger().logError("Error in sending response", e22);
                }
                if (transaction != null) {
                    sipStack.removeTransaction(transaction);
                    transaction.releaseSem();
                }
                return;
            } else {
                if (!(transaction == null || dialog == null)) {
                    try {
                        if (sipProvider == dialog.getSipProvider()) {
                            sipStack.addTransaction(transaction);
                            dialog.addTransaction(transaction);
                            transaction.setDialog(dialog, dialogId);
                        }
                    } catch (IOException ex222) {
                        InternalErrorHandler.handleException(ex222);
                    }
                }
                if (sipStack.isLoggingEnabled()) {
                    stackLogger = sipStack.getStackLogger();
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("BYE Tx = ");
                    stringBuilder5.append(transaction);
                    stringBuilder5.append(" isMapped =");
                    stringBuilder5.append(transaction.isTransactionMapped());
                    stackLogger.logDebug(stringBuilder5.toString());
                }
            }
        } else if (sipRequest.getMethod().equals(Request.CANCEL)) {
            st = (SIPServerTransaction) sipStack.findCancelTransaction(sIPRequest, true);
            if (sipStack.isLoggingEnabled()) {
                stackLogger = sipStack.getStackLogger();
                StringBuilder stringBuilder8 = new StringBuilder();
                stringBuilder8.append("Got a CANCEL, InviteServerTx = ");
                stringBuilder8.append(st);
                stringBuilder8.append(" cancel Server Tx ID = ");
                stringBuilder8.append(transaction);
                stringBuilder8.append(" isMapped = ");
                stringBuilder8.append(transaction.isTransactionMapped());
                stackLogger.logDebug(stringBuilder8.toString());
            }
            if (sipRequest.getMethod().equals(Request.CANCEL)) {
                if (st != null && st.getState() == SIPTransaction.TERMINATED_STATE) {
                    if (sipStack.isLoggingEnabled()) {
                        sipStack.getStackLogger().logDebug("Too late to cancel Transaction");
                    }
                    try {
                        transaction.sendResponse(sIPRequest.createResponse(Response.OK));
                    } catch (Exception ex2222) {
                        if (ex2222.getCause() != null && (ex2222.getCause() instanceof IOException)) {
                            st.raiseIOExceptionEvent();
                        }
                    }
                    return;
                } else if (sipStack.isLoggingEnabled()) {
                    stackLogger = sipStack.getStackLogger();
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Cancel transaction = ");
                    stringBuilder4.append(st);
                    stackLogger.logDebug(stringBuilder4.toString());
                }
            }
            if (transaction != null && st != null && st.getDialog() != null) {
                transaction.setDialog((SIPDialog) st.getDialog(), dialogId);
                dialog = (SIPDialog) st.getDialog();
            } else if (st == null && sipProvider.isAutomaticDialogSupportEnabled() && transaction != null) {
                SIPResponse response2 = sIPRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                if (sipStack.isLoggingEnabled()) {
                    sipStack.getStackLogger().logDebug("dropping request -- automatic dialog support enabled and INVITE ST does not exist!");
                }
                try {
                    sipProvider.sendResponse(response2);
                } catch (SipException ex22222) {
                    Exception exception = ex22222;
                    InternalErrorHandler.handleException(ex22222);
                }
                if (transaction != null) {
                    sipStack.removeTransaction(transaction);
                    transaction.releaseSem();
                }
                return;
            }
            if (!(st == null || transaction == null)) {
                try {
                    sipStack.addTransaction(transaction);
                    transaction.setPassToListener();
                    transaction.setInviteTransaction(st);
                    st.acquireSem();
                } catch (Exception ex222222) {
                    InternalErrorHandler.handleException(ex222222);
                }
            }
        } else if (sipRequest.getMethod().equals("INVITE")) {
            SIPTransaction lastTransaction2 = dialog == null ? null : dialog.getInviteTransaction();
            if (dialog == null || transaction == null || lastTransaction2 == null || sipRequest.getCSeq().getSeqNumber() <= dialog.getRemoteSeqNumber() || !(lastTransaction2 instanceof SIPServerTransaction) || !sipProvider.isDialogErrorsAutomaticallyHandled() || !dialog.isSequnceNumberValidation() || !lastTransaction2.isInviteTransaction() || lastTransaction2.getState() == TransactionState.COMPLETED || lastTransaction2.getState() == TransactionState.TERMINATED || lastTransaction2.getState() == TransactionState.CONFIRMED) {
                lastTransaction2 = dialog == null ? null : dialog.getLastTransaction();
                if (dialog != null && sipProvider.isDialogErrorsAutomaticallyHandled() && lastTransaction2 != null && lastTransaction2.isInviteTransaction() && (lastTransaction2 instanceof ClientTransaction) && lastTransaction2.getLastResponse() != null && lastTransaction2.getLastResponse().getStatusCode() == Response.OK && !dialog.isAckSent(lastTransaction2.getLastResponse().getCSeq().getSeqNumber())) {
                    if (sipStack.isLoggingEnabled()) {
                        sipStack.getStackLogger().logDebug("Sending 491 response for client Dialog ACK not sent.");
                    }
                    sendRequestPendingResponse(sIPRequest, transaction);
                    return;
                } else if (dialog != null && lastTransaction2 != null && sipProvider.isDialogErrorsAutomaticallyHandled() && lastTransaction2.isInviteTransaction() && (lastTransaction2 instanceof ServerTransaction) && !dialog.isAckSeen()) {
                    if (sipStack.isLoggingEnabled()) {
                        sipStack.getStackLogger().logDebug("Sending 491 response for server Dialog ACK not seen.");
                    }
                    sendRequestPendingResponse(sIPRequest, transaction);
                    return;
                }
            }
            if (sipStack.isLoggingEnabled()) {
                sipStack.getStackLogger().logDebug("Sending 500 response for out of sequence message");
            }
            sendServerInternalErrorResponse(sIPRequest, transaction);
            return;
        }
        if (sipStack.isLoggingEnabled()) {
            stackLogger = sipStack.getStackLogger();
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append("CHECK FOR OUT OF SEQ MESSAGE ");
            stringBuilder5.append(dialog);
            stringBuilder5.append(" transaction ");
            stringBuilder5.append(transaction);
            stackLogger.logDebug(stringBuilder5.toString());
        }
        if (!(dialog == null || transaction == null || sipRequest.getMethod().equals("BYE") || sipRequest.getMethod().equals(Request.CANCEL) || sipRequest.getMethod().equals("ACK") || sipRequest.getMethod().equals(Request.PRACK))) {
            if (dialog.isRequestConsumable(sIPRequest)) {
                try {
                    if (sipProvider == dialog.getSipProvider()) {
                        sipStack.addTransaction(transaction);
                        dialog.addTransaction(transaction);
                        dialog.addRoute(sIPRequest);
                        transaction.setDialog(dialog, dialogId);
                    }
                } catch (IOException e3) {
                    transaction.raiseIOExceptionEvent();
                    sipStack.removeTransaction(transaction);
                    return;
                }
            }
            if (sipStack.isLoggingEnabled()) {
                stackLogger = sipStack.getStackLogger();
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Dropping out of sequence message ");
                stringBuilder5.append(dialog.getRemoteSeqNumber());
                stringBuilder5.append(Separators.SP);
                stringBuilder5.append(sipRequest.getCSeq());
                stackLogger.logDebug(stringBuilder5.toString());
            }
            if (dialog.getRemoteSeqNumber() >= sipRequest.getCSeq().getSeqNumber() && sipProvider.isDialogErrorsAutomaticallyHandled() && (transaction.getState() == TransactionState.TRYING || transaction.getState() == TransactionState.PROCEEDING)) {
                sendServerInternalErrorResponse(sIPRequest, transaction);
            }
            return;
        }
        if (sipStack.isLoggingEnabled()) {
            stackLogger = sipStack.getStackLogger();
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append(sipRequest.getMethod());
            stringBuilder5.append(" transaction.isMapped = ");
            stringBuilder5.append(transaction.isTransactionMapped());
            stackLogger.logDebug(stringBuilder5.toString());
        }
        if (dialog == null && sipRequest.getMethod().equals("NOTIFY")) {
            SIPClientTransaction pendingSubscribeClientTx = sipStack.findSubscribeTransaction(sIPRequest, this.listeningPoint);
            if (sipStack.isLoggingEnabled()) {
                stackLogger = sipStack.getStackLogger();
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("PROCESSING NOTIFY  DIALOG == null ");
                stringBuilder4.append(pendingSubscribeClientTx);
                stackLogger.logDebug(stringBuilder4.toString());
            }
            if (sipProvider.isAutomaticDialogSupportEnabled() && pendingSubscribeClientTx == null && !sipStack.deliverUnsolicitedNotify) {
                try {
                    if (sipStack.isLoggingEnabled()) {
                        sipStack.getStackLogger().logDebug("Could not find Subscription for Notify Tx.");
                    }
                    Response errorResponse = sIPRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                    errorResponse.setReasonPhrase("Subscription does not exist");
                    sipProvider.sendResponse(errorResponse);
                    return;
                } catch (Exception ex2222222) {
                    sipStack.getStackLogger().logError("Exception while sending error response statelessly", ex2222222);
                    return;
                }
            } else if (pendingSubscribeClientTx != null) {
                transaction.setPendingSubscribe(pendingSubscribeClientTx);
                SIPDialog subscriptionDialog = pendingSubscribeClientTx.getDefaultDialog();
                if (subscriptionDialog == null || subscriptionDialog.getDialogId() == null || !subscriptionDialog.getDialogId().equals(dialogId)) {
                    if (subscriptionDialog == null || subscriptionDialog.getDialogId() != null) {
                        subscriptionDialog = pendingSubscribeClientTx.getDialog(dialogId);
                    } else {
                        subscriptionDialog.setDialogId(dialogId);
                    }
                    if (sipStack.isLoggingEnabled()) {
                        stackLogger = sipStack.getStackLogger();
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("PROCESSING NOTIFY Subscribe DIALOG ");
                        stringBuilder4.append(subscriptionDialog);
                        stackLogger.logDebug(stringBuilder4.toString());
                    }
                    if (subscriptionDialog == null && ((sipProvider.isAutomaticDialogSupportEnabled() || pendingSubscribeClientTx.getDefaultDialog() != null) && sipStack.isEventForked(((Event) sIPRequest.getHeader("Event")).getEventType()))) {
                        subscriptionDialog = SIPDialog.createFromNOTIFY(pendingSubscribeClientTx, transaction);
                    }
                    if (subscriptionDialog != null) {
                        transaction.setDialog(subscriptionDialog, dialogId);
                        subscriptionDialog.setState(DialogState.CONFIRMED.getValue());
                        sipStack.putDialog(subscriptionDialog);
                        pendingSubscribeClientTx.setDialog(subscriptionDialog, dialogId);
                        if (!transaction.isTransactionMapped()) {
                            this.sipStack.mapTransaction(transaction);
                            transaction.setPassToListener();
                            try {
                                this.sipStack.addTransaction(transaction);
                            } catch (Exception e4) {
                            }
                        }
                    }
                } else {
                    transaction.setDialog(subscriptionDialog, dialogId);
                    dialog = subscriptionDialog;
                    if (!transaction.isTransactionMapped()) {
                        this.sipStack.mapTransaction(transaction);
                        transaction.setPassToListener();
                        try {
                            this.sipStack.addTransaction(transaction);
                        } catch (Exception e5) {
                        }
                    }
                    sipStack.putDialog(subscriptionDialog);
                    if (pendingSubscribeClientTx != null) {
                        subscriptionDialog.addTransaction(pendingSubscribeClientTx);
                        pendingSubscribeClientTx.setDialog(subscriptionDialog, dialogId);
                    }
                }
                if (transaction == null || !transaction.isTransactionMapped()) {
                    sipEvent = new RequestEvent(sipProvider, null, subscriptionDialog, sIPRequest);
                } else {
                    sipEvent = new RequestEvent(sipProvider, transaction, subscriptionDialog, sIPRequest);
                }
            } else {
                if (sipStack.isLoggingEnabled()) {
                    sipStack.getStackLogger().logDebug("could not find subscribe tx");
                }
                sipEvent = new RequestEvent(sipProvider, null, null, sIPRequest);
            }
        } else if (transaction == null || !transaction.isTransactionMapped()) {
            sipEvent = new RequestEvent(sipProvider, null, dialog, sIPRequest);
        } else {
            sipEvent = new RequestEvent(sipProvider, transaction, dialog, sIPRequest);
        }
        sipProvider.handleEvent(sipEvent, transaction);
    }

    public void processResponse(SIPResponse response, MessageChannel incomingMessageChannel, SIPDialog dialog) {
        if (this.sipStack.isLoggingEnabled()) {
            StackLogger stackLogger = this.sipStack.getStackLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PROCESSING INCOMING RESPONSE");
            stringBuilder.append(response.encodeMessage());
            stackLogger.logDebug(stringBuilder.toString());
        }
        if (this.listeningPoint == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping message: No listening point registered!");
            }
        } else if (!this.sipStack.checkBranchId() || Utils.getInstance().responseBelongsToUs(response)) {
            SipProviderImpl sipProvider = this.listeningPoint.getProvider();
            if (sipProvider == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("Dropping message:  no provider");
                }
            } else if (sipProvider.getSipListener() == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("No listener -- dropping response!");
                }
            } else {
                StackLogger stackLogger2;
                StringBuilder stringBuilder2;
                SIPClientTransaction transaction = this.transactionChannel;
                SipStackImpl sipStackImpl = sipProvider.sipStack;
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger2 = sipStackImpl.getStackLogger();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Transaction = ");
                    stringBuilder2.append(transaction);
                    stackLogger2.logDebug(stringBuilder2.toString());
                }
                if (transaction == null) {
                    if (dialog != null) {
                        if (response.getStatusCode() / 100 != 2) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Response is not a final response and dialog is found for response -- dropping response!");
                            }
                            return;
                        } else if (dialog.getState() == DialogState.TERMINATED) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Dialog is terminated -- dropping response!");
                            }
                            return;
                        } else {
                            boolean ackAlreadySent = false;
                            if (dialog.isAckSeen() && dialog.getLastAckSent() != null && dialog.getLastAckSent().getCSeq().getSeqNumber() == response.getCSeq().getSeqNumber()) {
                                ackAlreadySent = true;
                            }
                            if (ackAlreadySent && response.getCSeq().getMethod().equals(dialog.getMethod())) {
                                try {
                                    if (this.sipStack.isLoggingEnabled()) {
                                        this.sipStack.getStackLogger().logDebug("Retransmission of OK detected: Resending last ACK");
                                    }
                                    dialog.resendAck();
                                    return;
                                } catch (SipException ex) {
                                    this.sipStack.getStackLogger().logError("could not resend ack", ex);
                                }
                            }
                        }
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        stackLogger2 = this.sipStack.getStackLogger();
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("could not find tx, handling statelessly Dialog =  ");
                        stringBuilder2.append(dialog);
                        stackLogger2.logDebug(stringBuilder2.toString());
                    }
                    ResponseEventExt sipEvent = new ResponseEventExt(sipProvider, transaction, dialog, response);
                    if (response.getCSeqHeader().getMethod().equals("INVITE")) {
                        sipEvent.setOriginalTransaction(this.sipStack.getForkedTransaction(response.getTransactionId()));
                    }
                    sipProvider.handleEvent(sipEvent, transaction);
                    return;
                }
                ResponseEventExt responseEvent = new ResponseEventExt(sipProvider, transaction, dialog, response);
                if (response.getCSeqHeader().getMethod().equals("INVITE")) {
                    responseEvent.setOriginalTransaction(this.sipStack.getForkedTransaction(response.getTransactionId()));
                }
                if (!(dialog == null || response.getStatusCode() == 100)) {
                    dialog.setLastResponse(transaction, response);
                    transaction.setDialog(dialog, dialog.getDialogId());
                }
                sipProvider.handleEvent(responseEvent, transaction);
            }
        } else {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping response - topmost VIA header does not originate from this stack");
            }
        }
    }

    public String getProcessingInfo() {
        return null;
    }

    public void processResponse(SIPResponse sipResponse, MessageChannel incomingChannel) {
        String dialogID = sipResponse.getDialogId(false);
        SIPDialog sipDialog = this.sipStack.getDialog(dialogID);
        String method = sipResponse.getCSeq().getMethod();
        if (this.sipStack.isLoggingEnabled()) {
            StackLogger stackLogger = this.sipStack.getStackLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PROCESSING INCOMING RESPONSE: ");
            stringBuilder.append(sipResponse.encodeMessage());
            stackLogger.logDebug(stringBuilder.toString());
        }
        if (this.sipStack.checkBranchId() && !Utils.getInstance().responseBelongsToUs(sipResponse)) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Detected stray response -- dropping");
            }
        } else if (this.listeningPoint == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping message: No listening point registered!");
            }
        } else {
            SipProviderImpl sipProvider = this.listeningPoint.getProvider();
            if (sipProvider == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dropping message:  no provider");
                }
            } else if (sipProvider.getSipListener() == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dropping message:  no sipListener registered!");
                }
            } else {
                String originalFrom;
                SIPClientTransaction transaction = this.transactionChannel;
                if (sipDialog == null && transaction != null) {
                    sipDialog = transaction.getDialog(dialogID);
                    if (sipDialog != null && sipDialog.getState() == DialogState.TERMINATED) {
                        sipDialog = null;
                    }
                }
                if (this.sipStack.isLoggingEnabled()) {
                    StackLogger stackLogger2 = this.sipStack.getStackLogger();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Transaction = ");
                    stringBuilder2.append(transaction);
                    stringBuilder2.append(" sipDialog = ");
                    stringBuilder2.append(sipDialog);
                    stackLogger2.logDebug(stringBuilder2.toString());
                }
                if (this.transactionChannel != null) {
                    originalFrom = ((SIPRequest) this.transactionChannel.getRequest()).getFromTag();
                    int i = 1;
                    int i2 = originalFrom == null ? 1 : 0;
                    if (sipResponse.getFrom().getTag() != null) {
                        i = 0;
                    }
                    if ((i ^ i2) != 0) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("From tag mismatch -- dropping response");
                        }
                        return;
                    } else if (!(originalFrom == null || originalFrom.equalsIgnoreCase(sipResponse.getFrom().getTag()))) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("From tag mismatch -- dropping response");
                        }
                        return;
                    }
                }
                originalFrom = this.sipStack;
                if (!SIPTransactionStack.isDialogCreated(method) || sipResponse.getStatusCode() == 100 || sipResponse.getFrom().getTag() == null || sipResponse.getTo().getTag() == null || sipDialog != null) {
                    if (!(sipDialog == null || transaction != null || sipDialog.getState() == DialogState.TERMINATED)) {
                        if (sipResponse.getStatusCode() / 100 != 2) {
                            if (this.sipStack.isLoggingEnabled()) {
                                StackLogger stackLogger3 = this.sipStack.getStackLogger();
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("status code != 200 ; statusCode = ");
                                stringBuilder3.append(sipResponse.getStatusCode());
                                stackLogger3.logDebug(stringBuilder3.toString());
                            }
                        } else if (sipDialog.getState() == DialogState.TERMINATED) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Dialog is terminated -- dropping response!");
                            }
                            if (sipResponse.getStatusCode() / 100 == 2 && sipResponse.getCSeq().getMethod().equals("INVITE")) {
                                try {
                                    sipDialog.sendAck(sipDialog.createAck(sipResponse.getCSeq().getSeqNumber()));
                                } catch (Exception ex) {
                                    this.sipStack.getStackLogger().logError("Error creating ack", ex);
                                }
                            }
                            return;
                        } else {
                            boolean ackAlreadySent = false;
                            if (sipDialog.isAckSeen() && sipDialog.getLastAckSent() != null && sipDialog.getLastAckSent().getCSeq().getSeqNumber() == sipResponse.getCSeq().getSeqNumber() && sipResponse.getDialogId(false).equals(sipDialog.getLastAckSent().getDialogId(false))) {
                                ackAlreadySent = true;
                            }
                            if (ackAlreadySent && sipResponse.getCSeq().getMethod().equals(sipDialog.getMethod())) {
                                try {
                                    if (this.sipStack.isLoggingEnabled()) {
                                        this.sipStack.getStackLogger().logDebug("resending ACK");
                                    }
                                    sipDialog.resendAck();
                                    return;
                                } catch (SipException e) {
                                }
                            }
                        }
                    }
                } else if (sipProvider.isAutomaticDialogSupportEnabled()) {
                    if (this.transactionChannel == null) {
                        sipDialog = this.sipStack.createDialog(sipProvider, sipResponse);
                    } else if (sipDialog == null) {
                        sipDialog = this.sipStack.createDialog((SIPClientTransaction) this.transactionChannel, sipResponse);
                        this.transactionChannel.setDialog(sipDialog, sipResponse.getDialogId(false));
                    }
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("sending response to TU for processing ");
                }
                if (!(sipDialog == null || sipResponse.getStatusCode() == 100 || sipResponse.getTo().getTag() == null)) {
                    sipDialog.setLastResponse(transaction, sipResponse);
                }
                ResponseEventExt responseEvent = new ResponseEventExt(sipProvider, transaction, sipDialog, sipResponse);
                if (sipResponse.getCSeq().getMethod().equals("INVITE")) {
                    responseEvent.setOriginalTransaction(this.sipStack.getForkedTransaction(sipResponse.getTransactionId()));
                }
                sipProvider.handleEvent(responseEvent, transaction);
            }
        }
    }
}
