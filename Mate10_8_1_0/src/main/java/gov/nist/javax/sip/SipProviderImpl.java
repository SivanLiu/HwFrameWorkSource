package gov.nist.javax.sip;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.DialogTimeoutEvent.Reason;
import gov.nist.javax.sip.address.ParameterNames;
import gov.nist.javax.sip.address.RouterExt;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.HopImpl;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPDialogErrorEvent;
import gov.nist.javax.sip.stack.SIPDialogEventListener;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPTransaction;
import gov.nist.javax.sip.stack.SIPTransactionErrorEvent;
import gov.nist.javax.sip.stack.SIPTransactionEventListener;
import gov.nist.javax.sip.stack.SIPTransactionStack;
import java.io.IOException;
import java.text.ParseException;
import java.util.EventObject;
import java.util.Iterator;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.Timeout;
import javax.sip.TimeoutEvent;
import javax.sip.Transaction;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionState;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Hop;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class SipProviderImpl implements SipProvider, SipProviderExt, SIPTransactionEventListener, SIPDialogEventListener {
    private String IN6_ADDR_ANY = "::0";
    private String IN_ADDR_ANY = "0.0.0.0";
    private String address;
    private boolean automaticDialogSupportEnabled;
    private boolean dialogErrorsAutomaticallyHandled = true;
    private EventScanner eventScanner;
    private ConcurrentHashMap listeningPoints;
    private int port;
    private SipListener sipListener;
    protected SipStackImpl sipStack;

    public void sendRequest(javax.sip.message.Request r13) throws javax.sip.SipException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x011b in list [B:61:0x0197]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r12 = this;
        r10 = 0;
        r8 = r12.sipStack;
        r8 = r8.isAlive();
        if (r8 != 0) goto L_0x0012;
    L_0x0009:
        r8 = new javax.sip.SipException;
        r9 = "Stack is stopped.";
        r8.<init>(r9);
        throw r8;
    L_0x0012:
        r8 = r13;
        r8 = (gov.nist.javax.sip.message.SIPRequest) r8;
        r8 = r8.getRequestLine();
        if (r8 == 0) goto L_0x0066;
    L_0x001b:
        r8 = r13.getMethod();
        r9 = "ACK";
        r8 = r8.equals(r9);
        if (r8 == 0) goto L_0x0066;
    L_0x0028:
        r9 = r12.sipStack;
        r8 = r13;
        r8 = (gov.nist.javax.sip.message.SIPRequest) r8;
        r8 = r8.getDialogId(r10);
        r1 = r9.getDialog(r8);
        if (r1 == 0) goto L_0x0066;
    L_0x0037:
        r8 = r1.getState();
        if (r8 == 0) goto L_0x0066;
    L_0x003d:
        r8 = r12.sipStack;
        r8 = r8.isLoggingEnabled();
        if (r8 == 0) goto L_0x0066;
    L_0x0045:
        r8 = r12.sipStack;
        r8 = r8.getStackLogger();
        r9 = new java.lang.StringBuilder;
        r9.<init>();
        r10 = "Dialog exists -- you may want to use Dialog.sendAck() ";
        r9 = r9.append(r10);
        r10 = r1.getState();
        r9 = r9.append(r10);
        r9 = r9.toString();
        r8.logWarning(r9);
    L_0x0066:
        r9 = r12.sipStack;
        r8 = r13;
        r8 = (gov.nist.javax.sip.message.SIPRequest) r8;
        r8 = r9.getRouter(r8);
        r4 = r8.getNextHop(r13);
        if (r4 != 0) goto L_0x007e;
    L_0x0075:
        r8 = new javax.sip.SipException;
        r9 = "could not determine next hop!";
        r8.<init>(r9);
        throw r8;
    L_0x007e:
        r6 = r13;
        r6 = (gov.nist.javax.sip.message.SIPRequest) r6;
        r8 = r6.isNullRequest();
        if (r8 != 0) goto L_0x0096;
    L_0x0087:
        r8 = r6.getTopmostVia();
        if (r8 != 0) goto L_0x0096;
    L_0x008d:
        r8 = new javax.sip.SipException;
        r9 = "Invalid SipRequest -- no via header!";
        r8.<init>(r9);
        throw r8;
    L_0x0096:
        r8 = r6.isNullRequest();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        if (r8 != 0) goto L_0x00b3;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x009c:
        r7 = r6.getTopmostVia();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r0 = r7.getBranch();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        if (r0 == 0) goto L_0x00ac;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x00a6:
        r8 = r0.length();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        if (r8 != 0) goto L_0x00b3;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x00ac:
        r8 = r6.getTransactionId();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r7.setBranch(r8);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x00b3:
        r5 = 0;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r8 = r12.listeningPoints;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9 = r4.getTransport();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9 = r9.toUpperCase();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r8 = r8.containsKey(r9);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        if (r8 == 0) goto L_0x00e2;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x00c4:
        r8 = r12.sipStack;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9 = r4.getTransport();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9 = r12.getListeningPoint(r9);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9 = r9.getIPAddress();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r10 = r4.getTransport();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r10 = r12.getListeningPoint(r10);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r10 = r10.getPort();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r5 = r8.createRawMessageChannel(r9, r10, r4);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x00e2:
        if (r5 == 0) goto L_0x011c;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x00e4:
        r5.sendMessage(r6, r4);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r8 = r12.sipStack;
        r8 = r8.isLoggingEnabled();
        if (r8 == 0) goto L_0x011b;
    L_0x00ef:
        r8 = r12.sipStack;
        r8 = r8.getStackLogger();
        r9 = new java.lang.StringBuilder;
        r9.<init>();
        r10 = "done sending ";
        r9 = r9.append(r10);
        r10 = r13.getMethod();
        r9 = r9.append(r10);
        r10 = " to hop ";
        r9 = r9.append(r10);
        r9 = r9.append(r4);
        r9 = r9.toString();
        r8.logDebug(r9);
    L_0x011b:
        return;
    L_0x011c:
        r8 = new javax.sip.SipException;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9.<init>();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r10 = "Could not create a message channel for ";	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9 = r9.append(r10);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r10 = r4.toString();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9 = r9.append(r10);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9 = r9.toString();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r8.<init>(r9);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        throw r8;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x013a:
        r2 = move-exception;
        r8 = r12.sipStack;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r8 = r8.isLoggingEnabled();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        if (r8 == 0) goto L_0x014c;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x0143:
        r8 = r12.sipStack;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r8 = r8.getStackLogger();	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r8.logException(r2);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x014c:
        r8 = new javax.sip.SipException;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r9 = "IO Exception occured while Sending Request";	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r8.<init>(r9, r2);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        throw r8;	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
    L_0x0155:
        r8 = move-exception;
        r9 = r12.sipStack;
        r9 = r9.isLoggingEnabled();
        if (r9 == 0) goto L_0x018a;
    L_0x015e:
        r9 = r12.sipStack;
        r9 = r9.getStackLogger();
        r10 = new java.lang.StringBuilder;
        r10.<init>();
        r11 = "done sending ";
        r10 = r10.append(r11);
        r11 = r13.getMethod();
        r10 = r10.append(r11);
        r11 = " to hop ";
        r10 = r10.append(r11);
        r10 = r10.append(r4);
        r10 = r10.toString();
        r9.logDebug(r10);
    L_0x018a:
        throw r8;
    L_0x018b:
        r3 = move-exception;
        gov.nist.core.InternalErrorHandler.handleException(r3);	 Catch:{ IOException -> 0x013a, ParseException -> 0x018b, all -> 0x0155 }
        r8 = r12.sipStack;
        r8 = r8.isLoggingEnabled();
        if (r8 == 0) goto L_0x011b;
    L_0x0197:
        r8 = r12.sipStack;
        r8 = r8.getStackLogger();
        r9 = new java.lang.StringBuilder;
        r9.<init>();
        r10 = "done sending ";
        r9 = r9.append(r10);
        r10 = r13.getMethod();
        r9 = r9.append(r10);
        r10 = " to hop ";
        r9 = r9.append(r10);
        r9 = r9.append(r4);
        r9 = r9.toString();
        r8.logDebug(r9);
        goto L_0x011b;
        */
        throw new UnsupportedOperationException("Method not decompiled: gov.nist.javax.sip.SipProviderImpl.sendRequest(javax.sip.message.Request):void");
    }

    private SipProviderImpl() {
    }

    protected void stop() {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Exiting provider");
        }
        for (ListeningPointImpl listeningPoint : this.listeningPoints.values()) {
            listeningPoint.removeSipProvider();
        }
        this.eventScanner.stop();
    }

    public ListeningPoint getListeningPoint(String transport) {
        if (transport != null) {
            return (ListeningPoint) this.listeningPoints.get(transport.toUpperCase());
        }
        throw new NullPointerException("Null transport param");
    }

    public void handleEvent(EventObject sipEvent, SIPTransaction transaction) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("handleEvent " + sipEvent + "currentTransaction = " + transaction + "this.sipListener = " + getSipListener() + "sipEvent.source = " + sipEvent.getSource());
            Dialog dialog;
            if (sipEvent instanceof RequestEvent) {
                dialog = ((RequestEvent) sipEvent).getDialog();
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dialog = " + dialog);
                }
            } else if (sipEvent instanceof ResponseEvent) {
                dialog = ((ResponseEvent) sipEvent).getDialog();
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dialog = " + dialog);
                }
            }
            this.sipStack.getStackLogger().logStackTrace();
        }
        EventWrapper eventWrapper = new EventWrapper(sipEvent, transaction);
        if (this.sipStack.reEntrantListener) {
            this.eventScanner.deliverEvent(eventWrapper);
        } else {
            this.eventScanner.addEvent(eventWrapper);
        }
    }

    protected SipProviderImpl(SipStackImpl sipStack) {
        this.eventScanner = sipStack.getEventScanner();
        this.sipStack = sipStack;
        this.eventScanner.incrementRefcount();
        this.listeningPoints = new ConcurrentHashMap();
        this.automaticDialogSupportEnabled = this.sipStack.isAutomaticDialogSupportEnabled();
        this.dialogErrorsAutomaticallyHandled = this.sipStack.isAutomaticDialogErrorHandlingEnabled();
    }

    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public void addSipListener(SipListener sipListener) throws TooManyListenersException {
        if (this.sipStack.sipListener == null) {
            this.sipStack.sipListener = sipListener;
        } else if (this.sipStack.sipListener != sipListener) {
            throw new TooManyListenersException("Stack already has a listener. Only one listener per stack allowed");
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("add SipListener " + sipListener);
        }
        this.sipListener = sipListener;
    }

    public ListeningPoint getListeningPoint() {
        if (this.listeningPoints.size() > 0) {
            return (ListeningPoint) this.listeningPoints.values().iterator().next();
        }
        return null;
    }

    public CallIdHeader getNewCallId() {
        String callId = Utils.getInstance().generateCallIdentifier(getListeningPoint().getIPAddress());
        CallID callid = new CallID();
        try {
            callid.setCallId(callId);
        } catch (ParseException e) {
        }
        return callid;
    }

    public ClientTransaction getNewClientTransaction(Request request) throws TransactionUnavailableException {
        if (request == null) {
            throw new NullPointerException("null request");
        } else if (this.sipStack.isAlive()) {
            SIPRequest sipRequest = (SIPRequest) request;
            if (sipRequest.getTransaction() != null) {
                throw new TransactionUnavailableException("Transaction already assigned to request");
            } else if (sipRequest.getMethod().equals("ACK")) {
                throw new TransactionUnavailableException("Cannot create client transaction for  ACK");
            } else {
                if (sipRequest.getTopmostVia() == null) {
                    request.setHeader(((ListeningPointImpl) getListeningPoint(ParameterNames.UDP)).getViaHeader());
                }
                try {
                    sipRequest.checkHeaders();
                    if (sipRequest.getTopmostVia().getBranch() == null || !sipRequest.getTopmostVia().getBranch().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE) || this.sipStack.findTransaction((SIPRequest) request, false) == null) {
                        SIPClientTransaction ct;
                        if (request.getMethod().equalsIgnoreCase(Request.CANCEL)) {
                            ct = (SIPClientTransaction) this.sipStack.findCancelTransaction((SIPRequest) request, false);
                            if (ct != null) {
                                ClientTransaction retval = this.sipStack.createClientTransaction((SIPRequest) request, ct.getMessageChannel());
                                ((SIPTransaction) retval).addEventListener(this);
                                this.sipStack.addTransaction((SIPClientTransaction) retval);
                                if (ct.getDialog() != null) {
                                    ((SIPClientTransaction) retval).setDialog((SIPDialog) ct.getDialog(), sipRequest.getDialogId(false));
                                }
                                return retval;
                            }
                        }
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("could not find existing transaction for " + ((SIPRequest) request).getFirstLine() + " creating a new one ");
                        }
                        try {
                            Hop hop = this.sipStack.getNextHop((SIPRequest) request);
                            if (hop == null) {
                                throw new TransactionUnavailableException("Cannot resolve next hop -- transaction unavailable");
                            }
                            String transport = hop.getTransport();
                            ListeningPointImpl listeningPoint = (ListeningPointImpl) getListeningPoint(transport);
                            SIPDialog dialog = this.sipStack.getDialog(sipRequest.getDialogId(false));
                            if (dialog != null && dialog.getState() == DialogState.TERMINATED) {
                                this.sipStack.removeDialog(dialog);
                            }
                            try {
                                if (sipRequest.getTopmostVia().getBranch() == null || (sipRequest.getTopmostVia().getBranch().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE) ^ 1) != 0 || this.sipStack.checkBranchId()) {
                                    sipRequest.getTopmostVia().setBranch(Utils.getInstance().generateBranchId());
                                }
                                Via topmostVia = sipRequest.getTopmostVia();
                                if (topmostVia.getTransport() == null) {
                                    topmostVia.setTransport(transport);
                                }
                                if (topmostVia.getPort() == -1) {
                                    topmostVia.setPort(listeningPoint.getPort());
                                }
                                String branchId = sipRequest.getTopmostVia().getBranch();
                                ct = (SIPClientTransaction) this.sipStack.createMessageChannel(sipRequest, listeningPoint.getMessageProcessor(), hop);
                                if (ct == null) {
                                    throw new TransactionUnavailableException("Cound not create tx");
                                }
                                ct.setNextHop(hop);
                                ct.setOriginalRequest(sipRequest);
                                ct.setBranch(branchId);
                                SipStackImpl sipStackImpl = this.sipStack;
                                if (SIPTransactionStack.isDialogCreated(request.getMethod())) {
                                    if (dialog != null) {
                                        ct.setDialog(dialog, sipRequest.getDialogId(false));
                                    } else if (isAutomaticDialogSupportEnabled()) {
                                        ct.setDialog(this.sipStack.createDialog(ct), sipRequest.getDialogId(false));
                                    }
                                } else if (dialog != null) {
                                    ct.setDialog(dialog, sipRequest.getDialogId(false));
                                }
                                ct.addEventListener(this);
                                return ct;
                            } catch (IOException ex) {
                                throw new TransactionUnavailableException("Could not resolve next hop or listening point unavailable! ", ex);
                            } catch (Exception ex2) {
                                InternalErrorHandler.handleException(ex2);
                                throw new TransactionUnavailableException("Unexpected Exception FIXME! ", ex2);
                            } catch (Exception ex3) {
                                InternalErrorHandler.handleException(ex3);
                                throw new TransactionUnavailableException("Unexpected Exception FIXME! ", ex3);
                            }
                        } catch (SipException ex4) {
                            throw new TransactionUnavailableException("Cannot resolve next hop -- transaction unavailable", ex4);
                        }
                    }
                    throw new TransactionUnavailableException("Transaction already exists!");
                } catch (ParseException ex5) {
                    throw new TransactionUnavailableException(ex5.getMessage(), ex5);
                }
            }
        } else {
            throw new TransactionUnavailableException("Stack is stopped");
        }
    }

    public ServerTransaction getNewServerTransaction(Request request) throws TransactionAlreadyExistsException, TransactionUnavailableException {
        if (this.sipStack.isAlive()) {
            SIPRequest sipRequest = (SIPRequest) request;
            try {
                sipRequest.checkHeaders();
                if (request.getMethod().equals("ACK")) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logError("Creating server transaction for ACK -- makes no sense!");
                    }
                    throw new TransactionUnavailableException("Cannot create Server transaction for ACK ");
                } else if (sipRequest.getMethod().equals("NOTIFY") && sipRequest.getFromTag() != null && sipRequest.getToTag() == null && this.sipStack.findSubscribeTransaction(sipRequest, (ListeningPointImpl) getListeningPoint()) == null && (this.sipStack.deliverUnsolicitedNotify ^ 1) != 0) {
                    throw new TransactionUnavailableException("Cannot find matching Subscription (and gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY not set)");
                } else if (this.sipStack.acquireSem()) {
                    try {
                        SIPServerTransaction transaction;
                        SipStackImpl sipStackImpl = this.sipStack;
                        SIPDialog dialog;
                        if (SIPTransactionStack.isDialogCreated(sipRequest.getMethod())) {
                            if (this.sipStack.findTransaction((SIPRequest) request, true) != null) {
                                throw new TransactionAlreadyExistsException("server transaction already exists!");
                            }
                            transaction = (SIPServerTransaction) ((SIPRequest) request).getTransaction();
                            if (transaction == null) {
                                throw new TransactionUnavailableException("Transaction not available");
                            }
                            if (transaction.getOriginalRequest() == null) {
                                transaction.setOriginalRequest(sipRequest);
                            }
                            this.sipStack.addTransaction(transaction);
                            transaction.addEventListener(this);
                            if (isAutomaticDialogSupportEnabled()) {
                                dialog = this.sipStack.getDialog(sipRequest.getDialogId(true));
                                if (dialog == null) {
                                    dialog = this.sipStack.createDialog(transaction);
                                }
                                transaction.setDialog(dialog, sipRequest.getDialogId(true));
                                if (sipRequest.getMethod().equals("INVITE") && isDialogErrorsAutomaticallyHandled()) {
                                    this.sipStack.putInMergeTable(transaction, sipRequest);
                                }
                                dialog.addRoute(sipRequest);
                                if (!(dialog.getRemoteTag() == null || dialog.getLocalTag() == null)) {
                                    this.sipStack.putDialog(dialog);
                                }
                            }
                        } else if (isAutomaticDialogSupportEnabled()) {
                            if (((SIPServerTransaction) this.sipStack.findTransaction((SIPRequest) request, true)) != null) {
                                throw new TransactionAlreadyExistsException("Transaction exists! ");
                            }
                            transaction = (SIPServerTransaction) ((SIPRequest) request).getTransaction();
                            if (transaction == null) {
                                throw new TransactionUnavailableException("Transaction not available!");
                            }
                            if (transaction.getOriginalRequest() == null) {
                                transaction.setOriginalRequest(sipRequest);
                            }
                            this.sipStack.addTransaction(transaction);
                            dialog = this.sipStack.getDialog(sipRequest.getDialogId(true));
                            if (dialog != null) {
                                dialog.addTransaction(transaction);
                                dialog.addRoute(sipRequest);
                                transaction.setDialog(dialog, sipRequest.getDialogId(true));
                            }
                        } else if (((SIPServerTransaction) this.sipStack.findTransaction((SIPRequest) request, true)) != null) {
                            throw new TransactionAlreadyExistsException("Transaction exists! ");
                        } else {
                            transaction = (SIPServerTransaction) ((SIPRequest) request).getTransaction();
                            if (transaction != null) {
                                if (transaction.getOriginalRequest() == null) {
                                    transaction.setOriginalRequest(sipRequest);
                                }
                                this.sipStack.mapTransaction(transaction);
                                dialog = this.sipStack.getDialog(sipRequest.getDialogId(true));
                                if (dialog != null) {
                                    dialog.addTransaction(transaction);
                                    dialog.addRoute(sipRequest);
                                    transaction.setDialog(dialog, sipRequest.getDialogId(true));
                                }
                                this.sipStack.releaseSem();
                                return transaction;
                            }
                            transaction = this.sipStack.createServerTransaction((MessageChannel) sipRequest.getMessageChannel());
                            if (transaction == null) {
                                throw new TransactionUnavailableException("Transaction unavailable -- too many servrer transactions");
                            }
                            transaction.setOriginalRequest(sipRequest);
                            this.sipStack.mapTransaction(transaction);
                            dialog = this.sipStack.getDialog(sipRequest.getDialogId(true));
                            if (dialog != null) {
                                dialog.addTransaction(transaction);
                                dialog.addRoute(sipRequest);
                                transaction.setDialog(dialog, sipRequest.getDialogId(true));
                            }
                            this.sipStack.releaseSem();
                            return transaction;
                        }
                        this.sipStack.releaseSem();
                        return transaction;
                    } catch (IOException e) {
                        throw new TransactionUnavailableException("Could not send back provisional response!");
                    } catch (IOException e2) {
                        throw new TransactionUnavailableException("Error sending provisional response");
                    } catch (Throwable th) {
                        this.sipStack.releaseSem();
                    }
                } else {
                    throw new TransactionUnavailableException("Transaction not available -- could not acquire stack lock");
                }
            } catch (ParseException ex) {
                throw new TransactionUnavailableException(ex.getMessage(), ex);
            }
        }
        throw new TransactionUnavailableException("Stack is stopped");
    }

    public SipStack getSipStack() {
        return this.sipStack;
    }

    public void removeSipListener(SipListener sipListener) {
        if (sipListener == getSipListener()) {
            this.sipListener = null;
        }
        boolean found = false;
        Iterator<SipProviderImpl> it = this.sipStack.getSipProviders();
        while (it.hasNext()) {
            if (((SipProviderImpl) it.next()).getSipListener() != null) {
                found = true;
            }
        }
        if (!found) {
            this.sipStack.sipListener = null;
        }
    }

    public void sendResponse(Response response) throws SipException {
        if (this.sipStack.isAlive()) {
            SIPResponse sipResponse = (SIPResponse) response;
            Via via = sipResponse.getTopmostVia();
            if (via == null) {
                throw new SipException("No via header in response!");
            }
            SIPServerTransaction st = (SIPServerTransaction) this.sipStack.findTransaction((SIPMessage) response, true);
            if (st == null || st.getState() == TransactionState.TERMINATED || !isAutomaticDialogSupportEnabled()) {
                String transport = via.getTransport();
                String host = via.getReceived();
                if (host == null) {
                    host = via.getHost();
                }
                int port = via.getRPort();
                if (port == -1) {
                    port = via.getPort();
                    if (port == -1) {
                        if (transport.equalsIgnoreCase(ListeningPoint.TLS)) {
                            port = 5061;
                        } else {
                            port = 5060;
                        }
                    }
                }
                if (host.indexOf(Separators.COLON) > 0 && host.indexOf("[") < 0) {
                    host = "[" + host + "]";
                }
                Hop hop = this.sipStack.getAddressResolver().resolveAddress(new HopImpl(host, port, transport));
                try {
                    ListeningPointImpl listeningPoint = (ListeningPointImpl) getListeningPoint(transport);
                    if (listeningPoint == null) {
                        throw new SipException("whoopsa daisy! no listening point found for transport " + transport);
                    }
                    this.sipStack.createRawMessageChannel(getListeningPoint(hop.getTransport()).getIPAddress(), listeningPoint.port, hop).sendMessage(sipResponse);
                    return;
                } catch (IOException ex) {
                    throw new SipException(ex.getMessage());
                }
            }
            throw new SipException("Transaction exists -- cannot send response statelessly");
        }
        throw new SipException("Stack is stopped");
    }

    public synchronized void setListeningPoint(ListeningPoint listeningPoint) {
        if (listeningPoint == null) {
            throw new NullPointerException("Null listening point");
        }
        ListeningPointImpl lp = (ListeningPointImpl) listeningPoint;
        lp.sipProvider = this;
        String transport = lp.getTransport().toUpperCase();
        this.address = listeningPoint.getIPAddress();
        this.port = listeningPoint.getPort();
        this.listeningPoints.clear();
        this.listeningPoints.put(transport, listeningPoint);
    }

    public Dialog getNewDialog(Transaction transaction) throws SipException {
        if (transaction == null) {
            throw new NullPointerException("Null transaction!");
        } else if (!this.sipStack.isAlive()) {
            throw new SipException("Stack is stopped.");
        } else if (isAutomaticDialogSupportEnabled()) {
            throw new SipException(" Error - AUTOMATIC_DIALOG_SUPPORT is on");
        } else {
            SipStackImpl sipStackImpl = this.sipStack;
            if (SIPTransactionStack.isDialogCreated(transaction.getRequest().getMethod())) {
                SIPDialog dialog;
                SIPTransaction sipTransaction = (SIPTransaction) transaction;
                if (transaction instanceof ServerTransaction) {
                    SIPServerTransaction st = (SIPServerTransaction) transaction;
                    Response response = st.getLastResponse();
                    if (response == null || response.getStatusCode() == 100) {
                        SIPRequest sipRequest = (SIPRequest) transaction.getRequest();
                        dialog = this.sipStack.getDialog(sipRequest.getDialogId(true));
                        if (dialog == null) {
                            dialog = this.sipStack.createDialog((SIPTransaction) transaction);
                            dialog.addTransaction(sipTransaction);
                            dialog.addRoute(sipRequest);
                            sipTransaction.setDialog(dialog, null);
                        } else {
                            sipTransaction.setDialog(dialog, sipRequest.getDialogId(true));
                        }
                        if (sipRequest.getMethod().equals("INVITE") && isDialogErrorsAutomaticallyHandled()) {
                            this.sipStack.putInMergeTable(st, sipRequest);
                        }
                    } else {
                        throw new SipException("Cannot set dialog after response has been sent");
                    }
                }
                SIPClientTransaction sipClientTx = (SIPClientTransaction) transaction;
                if (sipClientTx.getLastResponse() == null) {
                    if (this.sipStack.getDialog(((SIPRequest) sipClientTx.getRequest()).getDialogId(false)) != null) {
                        throw new SipException("Dialog already exists!");
                    }
                    dialog = this.sipStack.createDialog(sipTransaction);
                    sipClientTx.setDialog(dialog, null);
                } else {
                    throw new SipException("Cannot call this method after response is received!");
                }
                dialog.addEventListener(this);
                return dialog;
            }
            throw new SipException("Dialog cannot be created for this method " + transaction.getRequest().getMethod());
        }
    }

    public void transactionErrorEvent(SIPTransactionErrorEvent transactionErrorEvent) {
        SIPTransaction transaction = (SIPTransaction) transactionErrorEvent.getSource();
        Timeout timeout;
        TimeoutEvent ev;
        Hop hop;
        if (transactionErrorEvent.getErrorID() == 2) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("TransportError occured on " + transaction);
            }
            SIPClientTransaction errorObject = transactionErrorEvent.getSource();
            timeout = Timeout.TRANSACTION;
            if (errorObject instanceof SIPServerTransaction) {
                ev = new TimeoutEvent((Object) this, (ServerTransaction) errorObject, timeout);
            } else {
                hop = errorObject.getNextHop();
                if (this.sipStack.getRouter() instanceof RouterExt) {
                    ((RouterExt) this.sipStack.getRouter()).transactionTimeout(hop);
                }
                ev = new TimeoutEvent((Object) this, (ClientTransaction) errorObject, timeout);
            }
            handleEvent(ev, errorObject);
        } else if (transactionErrorEvent.getErrorID() == 1) {
            Object errorObject2 = transactionErrorEvent.getSource();
            timeout = Timeout.TRANSACTION;
            if (errorObject2 instanceof SIPServerTransaction) {
                ev = new TimeoutEvent((Object) this, (ServerTransaction) errorObject2, timeout);
            } else {
                hop = ((SIPClientTransaction) errorObject2).getNextHop();
                if (this.sipStack.getRouter() instanceof RouterExt) {
                    ((RouterExt) this.sipStack.getRouter()).transactionTimeout(hop);
                }
                ev = new TimeoutEvent((Object) this, (ClientTransaction) errorObject2, timeout);
            }
            handleEvent(ev, (SIPTransaction) errorObject2);
        } else if (transactionErrorEvent.getErrorID() == 3) {
            Transaction errorObject3 = transactionErrorEvent.getSource();
            if (errorObject3.getDialog() != null) {
                InternalErrorHandler.handleException("Unexpected event !", this.sipStack.getStackLogger());
            }
            timeout = Timeout.RETRANSMIT;
            if (errorObject3 instanceof SIPServerTransaction) {
                ev = new TimeoutEvent((Object) this, (ServerTransaction) errorObject3, timeout);
            } else {
                ev = new TimeoutEvent((Object) this, (ClientTransaction) errorObject3, timeout);
            }
            handleEvent(ev, (SIPTransaction) errorObject3);
        }
    }

    public synchronized void dialogErrorEvent(SIPDialogErrorEvent dialogErrorEvent) {
        SIPDialog sipDialog = (SIPDialog) dialogErrorEvent.getSource();
        Reason reason = Reason.AckNotReceived;
        if (dialogErrorEvent.getErrorID() == 2) {
            reason = Reason.AckNotSent;
        } else if (dialogErrorEvent.getErrorID() == 3) {
            reason = Reason.ReInviteTimeout;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Dialog TimeoutError occured on " + sipDialog);
        }
        handleEvent(new DialogTimeoutEvent(this, sipDialog, reason), null);
    }

    public synchronized ListeningPoint[] getListeningPoints() {
        ListeningPoint[] retval;
        retval = new ListeningPointImpl[this.listeningPoints.size()];
        this.listeningPoints.values().toArray(retval);
        return retval;
    }

    public synchronized void addListeningPoint(ListeningPoint listeningPoint) throws ObjectInUseException {
        ListeningPointImpl lp = (ListeningPointImpl) listeningPoint;
        if (lp.sipProvider == null || lp.sipProvider == this) {
            String transport = lp.getTransport().toUpperCase();
            if (this.listeningPoints.isEmpty()) {
                this.address = listeningPoint.getIPAddress();
                this.port = listeningPoint.getPort();
            } else if (!(this.address.equals(listeningPoint.getIPAddress()) && this.port == listeningPoint.getPort())) {
                throw new ObjectInUseException("Provider already has different IP Address associated");
            }
            if (!this.listeningPoints.containsKey(transport) || this.listeningPoints.get(transport) == listeningPoint) {
                lp.sipProvider = this;
                this.listeningPoints.put(transport, lp);
            } else {
                throw new ObjectInUseException("Listening point already assigned for transport!");
            }
        }
        throw new ObjectInUseException("Listening point assigned to another provider");
    }

    public synchronized void removeListeningPoint(ListeningPoint listeningPoint) throws ObjectInUseException {
        ListeningPointImpl lp = (ListeningPointImpl) listeningPoint;
        if (lp.messageProcessor.inUse()) {
            throw new ObjectInUseException("Object is in use");
        }
        this.listeningPoints.remove(lp.getTransport().toUpperCase());
    }

    public synchronized void removeListeningPoints() {
        Iterator it = this.listeningPoints.values().iterator();
        while (it.hasNext()) {
            ((ListeningPointImpl) it.next()).messageProcessor.stop();
            it.remove();
        }
    }

    public void setAutomaticDialogSupportEnabled(boolean automaticDialogSupportEnabled) {
        this.automaticDialogSupportEnabled = automaticDialogSupportEnabled;
        if (this.automaticDialogSupportEnabled) {
            this.dialogErrorsAutomaticallyHandled = true;
        }
    }

    public boolean isAutomaticDialogSupportEnabled() {
        return this.automaticDialogSupportEnabled;
    }

    public void setDialogErrorsAutomaticallyHandled() {
        this.dialogErrorsAutomaticallyHandled = true;
    }

    public boolean isDialogErrorsAutomaticallyHandled() {
        return this.dialogErrorsAutomaticallyHandled;
    }

    public SipListener getSipListener() {
        return this.sipListener;
    }
}
