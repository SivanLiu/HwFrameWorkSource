package gov.nist.javax.sip;

import gov.nist.core.StackLogger;
import gov.nist.core.ThreadAuditor.ThreadHandle;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPTransaction;
import java.util.EventObject;
import java.util.LinkedList;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.message.Response;

class EventScanner implements Runnable {
    private int[] eventMutex;
    private boolean isStopped;
    private LinkedList pendingEvents;
    private int refCount;
    private SipStackImpl sipStack;

    public void incrementRefcount() {
        synchronized (this.eventMutex) {
            this.refCount++;
        }
    }

    public EventScanner(SipStackImpl sipStackImpl) {
        this.pendingEvents = new LinkedList();
        this.eventMutex = new int[]{0};
        this.pendingEvents = new LinkedList();
        Thread myThread = new Thread(this);
        myThread.setDaemon(false);
        this.sipStack = sipStackImpl;
        myThread.setName("EventScannerThread");
        myThread.start();
    }

    public void addEvent(EventWrapper eventWrapper) {
        if (this.sipStack.isLoggingEnabled()) {
            StackLogger stackLogger = this.sipStack.getStackLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addEvent ");
            stringBuilder.append(eventWrapper);
            stackLogger.logDebug(stringBuilder.toString());
        }
        synchronized (this.eventMutex) {
            this.pendingEvents.add(eventWrapper);
            this.eventMutex.notify();
        }
    }

    public void stop() {
        synchronized (this.eventMutex) {
            if (this.refCount > 0) {
                this.refCount--;
            }
            if (this.refCount == 0) {
                this.isStopped = true;
                this.eventMutex.notify();
            }
        }
    }

    public void forceStop() {
        synchronized (this.eventMutex) {
            this.isStopped = true;
            this.refCount = 0;
            this.eventMutex.notify();
        }
    }

    public void deliverEvent(EventWrapper eventWrapper) {
        SipListener sipListener;
        EventObject sipEvent = eventWrapper.sipEvent;
        if (this.sipStack.isLoggingEnabled()) {
            StackLogger stackLogger = this.sipStack.getStackLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sipEvent = ");
            stringBuilder.append(sipEvent);
            stringBuilder.append("source = ");
            stringBuilder.append(sipEvent.getSource());
            stackLogger.logDebug(stringBuilder.toString());
        }
        if (sipEvent instanceof IOExceptionEvent) {
            sipListener = this.sipStack.getSipListener();
        } else {
            sipListener = ((SipProviderImpl) sipEvent.getSource()).getSipListener();
        }
        SIPDialog dialog;
        StackLogger stackLogger2;
        StringBuilder stringBuilder2;
        if (sipEvent instanceof RequestEvent) {
            StackLogger stackLogger3;
            StringBuilder stringBuilder3;
            try {
                StackLogger stackLogger4;
                StringBuilder stringBuilder4;
                SIPRequest sipRequest = (SIPRequest) ((RequestEvent) sipEvent).getRequest();
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger3 = this.sipStack.getStackLogger();
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("deliverEvent : ");
                    stringBuilder3.append(sipRequest.getFirstLine());
                    stringBuilder3.append(" transaction ");
                    stringBuilder3.append(eventWrapper.transaction);
                    stringBuilder3.append(" sipEvent.serverTx = ");
                    stringBuilder3.append(((RequestEvent) sipEvent).getServerTransaction());
                    stackLogger3.logDebug(stringBuilder3.toString());
                }
                SIPServerTransaction tx = (SIPServerTransaction) this.sipStack.findTransaction(sipRequest, true);
                if (tx == null || tx.passToListener()) {
                    if (this.sipStack.findPendingTransaction(sipRequest) != null) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("transaction already exists!!");
                        }
                        if (this.sipStack.isLoggingEnabled()) {
                            stackLogger4 = this.sipStack.getStackLogger();
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Done processing Message ");
                            stringBuilder4.append(((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
                            stackLogger4.logDebug(stringBuilder4.toString());
                        }
                        if (eventWrapper.transaction != null && ((SIPServerTransaction) eventWrapper.transaction).passToListener()) {
                            ((SIPServerTransaction) eventWrapper.transaction).releaseSem();
                        }
                        if (eventWrapper.transaction != null) {
                            this.sipStack.removePendingTransaction((SIPServerTransaction) eventWrapper.transaction);
                        }
                        if (eventWrapper.transaction.getOriginalRequest().getMethod().equals("ACK")) {
                            eventWrapper.transaction.setState(TransactionState.TERMINATED);
                        }
                        return;
                    }
                    this.sipStack.putPendingTransaction(eventWrapper.transaction);
                } else if (!sipRequest.getMethod().equals("ACK") || !tx.isInviteTransaction() || (tx.getLastResponse().getStatusCode() / 100 != 2 && !this.sipStack.isNon2XXAckPassedToListener())) {
                    if (this.sipStack.isLoggingEnabled()) {
                        stackLogger4 = this.sipStack.getStackLogger();
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("transaction already exists! ");
                        stringBuilder4.append(tx);
                        stackLogger4.logDebug(stringBuilder4.toString());
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        stackLogger4 = this.sipStack.getStackLogger();
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Done processing Message ");
                        stringBuilder4.append(((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
                        stackLogger4.logDebug(stringBuilder4.toString());
                    }
                    if (eventWrapper.transaction != null && ((SIPServerTransaction) eventWrapper.transaction).passToListener()) {
                        ((SIPServerTransaction) eventWrapper.transaction).releaseSem();
                    }
                    if (eventWrapper.transaction != null) {
                        this.sipStack.removePendingTransaction((SIPServerTransaction) eventWrapper.transaction);
                    }
                    if (eventWrapper.transaction.getOriginalRequest().getMethod().equals("ACK")) {
                        eventWrapper.transaction.setState(TransactionState.TERMINATED);
                    }
                    return;
                } else if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Detected broken client sending ACK with same branch! Passing...");
                }
                sipRequest.setTransaction(eventWrapper.transaction);
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger4 = this.sipStack.getStackLogger();
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Calling listener ");
                    stringBuilder4.append(sipRequest.getFirstLine());
                    stackLogger4.logDebug(stringBuilder4.toString());
                    stackLogger4 = this.sipStack.getStackLogger();
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Calling listener ");
                    stringBuilder4.append(eventWrapper.transaction);
                    stackLogger4.logDebug(stringBuilder4.toString());
                }
                if (sipListener != null) {
                    sipListener.processRequest((RequestEvent) sipEvent);
                }
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger4 = this.sipStack.getStackLogger();
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Done processing Message ");
                    stringBuilder4.append(sipRequest.getFirstLine());
                    stackLogger4.logDebug(stringBuilder4.toString());
                }
                if (eventWrapper.transaction != null) {
                    dialog = (SIPDialog) eventWrapper.transaction.getDialog();
                    if (dialog != null) {
                        dialog.requestConsumed();
                    }
                }
            } catch (Exception ex) {
                this.sipStack.getStackLogger().logException(ex);
            } catch (Throwable th) {
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger3 = this.sipStack.getStackLogger();
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Done processing Message ");
                    stringBuilder3.append(((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
                    stackLogger3.logDebug(stringBuilder3.toString());
                }
                if (eventWrapper.transaction != null && ((SIPServerTransaction) eventWrapper.transaction).passToListener()) {
                    ((SIPServerTransaction) eventWrapper.transaction).releaseSem();
                }
                if (eventWrapper.transaction != null) {
                    this.sipStack.removePendingTransaction((SIPServerTransaction) eventWrapper.transaction);
                }
                if (eventWrapper.transaction.getOriginalRequest().getMethod().equals("ACK")) {
                    eventWrapper.transaction.setState(TransactionState.TERMINATED);
                }
            }
            if (this.sipStack.isLoggingEnabled()) {
                stackLogger2 = this.sipStack.getStackLogger();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Done processing Message ");
                stringBuilder2.append(((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
                stackLogger2.logDebug(stringBuilder2.toString());
            }
            if (eventWrapper.transaction != null && ((SIPServerTransaction) eventWrapper.transaction).passToListener()) {
                ((SIPServerTransaction) eventWrapper.transaction).releaseSem();
            }
            if (eventWrapper.transaction != null) {
                this.sipStack.removePendingTransaction((SIPServerTransaction) eventWrapper.transaction);
            }
            if (eventWrapper.transaction.getOriginalRequest().getMethod().equals("ACK")) {
                eventWrapper.transaction.setState(TransactionState.TERMINATED);
            }
        } else if (sipEvent instanceof ResponseEvent) {
            try {
                StackLogger stackLogger5;
                StringBuilder stringBuilder5;
                ResponseEvent responseEvent = (ResponseEvent) sipEvent;
                SIPResponse sipResponse = (SIPResponse) responseEvent.getResponse();
                dialog = (SIPDialog) responseEvent.getDialog();
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger5 = this.sipStack.getStackLogger();
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Calling listener for ");
                    stringBuilder5.append(sipResponse.getFirstLine());
                    stackLogger5.logDebug(stringBuilder5.toString());
                }
                if (sipListener != null) {
                    SIPTransaction tx2 = eventWrapper.transaction;
                    if (tx2 != null) {
                        tx2.setPassToListener();
                    }
                    sipListener.processResponse((ResponseEvent) sipEvent);
                }
                if (dialog != null && ((dialog.getState() == null || !dialog.getState().equals(DialogState.TERMINATED)) && (sipResponse.getStatusCode() == Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST || sipResponse.getStatusCode() == Response.REQUEST_TIMEOUT))) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Removing dialog on 408 or 481 response");
                    }
                    dialog.doDeferredDelete();
                }
                if (sipResponse.getCSeq().getMethod().equals("INVITE") && dialog != null && sipResponse.getStatusCode() == Response.OK) {
                    if (this.sipStack.isLoggingEnabled()) {
                        stackLogger5 = this.sipStack.getStackLogger();
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("Warning! unacknowledged dialog. ");
                        stringBuilder5.append(dialog.getState());
                        stackLogger5.logDebug(stringBuilder5.toString());
                    }
                    dialog.doDeferredDeleteIfNoAckSent(sipResponse.getCSeq().getSeqNumber());
                }
            } catch (Exception ex2) {
                this.sipStack.getStackLogger().logException(ex2);
            } catch (Throwable th2) {
                if (eventWrapper.transaction != null && eventWrapper.transaction.passToListener()) {
                    eventWrapper.transaction.releaseSem();
                }
            }
            SIPClientTransaction ct = eventWrapper.transaction;
            if (!(ct == null || TransactionState.COMPLETED != ct.getState() || ct.getOriginalRequest() == null || ct.getOriginalRequest().getMethod().equals("INVITE"))) {
                ct.clearState();
            }
            if (eventWrapper.transaction != null && eventWrapper.transaction.passToListener()) {
                eventWrapper.transaction.releaseSem();
            }
        } else if (sipEvent instanceof TimeoutEvent) {
            if (sipListener != null) {
                try {
                    sipListener.processTimeout((TimeoutEvent) sipEvent);
                } catch (Exception ex3) {
                    this.sipStack.getStackLogger().logException(ex3);
                }
            }
        } else if (sipEvent instanceof DialogTimeoutEvent) {
            if (sipListener != null) {
                try {
                    if (sipListener instanceof SipListenerExt) {
                        ((SipListenerExt) sipListener).processDialogTimeout((DialogTimeoutEvent) sipEvent);
                    }
                } catch (Exception ex32) {
                    this.sipStack.getStackLogger().logException(ex32);
                }
            }
        } else if (sipEvent instanceof IOExceptionEvent) {
            if (sipListener != null) {
                try {
                    sipListener.processIOException((IOExceptionEvent) sipEvent);
                } catch (Exception ex322) {
                    this.sipStack.getStackLogger().logException(ex322);
                }
            }
        } else if (sipEvent instanceof TransactionTerminatedEvent) {
            try {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("About to deliver transactionTerminatedEvent");
                    stackLogger2 = this.sipStack.getStackLogger();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("tx = ");
                    stringBuilder2.append(((TransactionTerminatedEvent) sipEvent).getClientTransaction());
                    stackLogger2.logDebug(stringBuilder2.toString());
                    stackLogger2 = this.sipStack.getStackLogger();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("tx = ");
                    stringBuilder2.append(((TransactionTerminatedEvent) sipEvent).getServerTransaction());
                    stackLogger2.logDebug(stringBuilder2.toString());
                }
                if (sipListener != null) {
                    sipListener.processTransactionTerminated((TransactionTerminatedEvent) sipEvent);
                }
            } catch (AbstractMethodError e) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logWarning("Unable to call sipListener.processTransactionTerminated");
                }
            } catch (Exception ex3222) {
                this.sipStack.getStackLogger().logException(ex3222);
            }
        } else if (!(sipEvent instanceof DialogTerminatedEvent)) {
            stackLogger2 = this.sipStack.getStackLogger();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("bad event");
            stringBuilder2.append(sipEvent);
            stackLogger2.logFatalError(stringBuilder2.toString());
        } else if (sipListener != null) {
            try {
                sipListener.processDialogTerminated((DialogTerminatedEvent) sipEvent);
            } catch (AbstractMethodError e2) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logWarning("Unable to call sipListener.processDialogTerminated");
                }
            } catch (Exception ex32222) {
                this.sipStack.getStackLogger().logException(ex32222);
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x0034, code skipped:
            if (r7.sipStack.isLoggingEnabled() == false) goto L_0x0045;
     */
    /* JADX WARNING: Missing block: B:16:0x0038, code skipped:
            if (r7.isStopped != false) goto L_0x0045;
     */
    /* JADX WARNING: Missing block: B:17:0x003a, code skipped:
            r7.sipStack.getStackLogger().logFatalError("Event scanner exited abnormally");
     */
    /* JADX WARNING: Missing block: B:18:0x0045, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:38:?, code skipped:
            r2 = r3.listIterator();
     */
    /* JADX WARNING: Missing block: B:40:0x0092, code skipped:
            if (r2.hasNext() == false) goto L_0x00e2;
     */
    /* JADX WARNING: Missing block: B:41:0x0094, code skipped:
            r1 = (gov.nist.javax.sip.EventWrapper) r2.next();
     */
    /* JADX WARNING: Missing block: B:42:0x00a1, code skipped:
            if (r7.sipStack.isLoggingEnabled() == false) goto L_0x00c9;
     */
    /* JADX WARNING: Missing block: B:43:0x00a3, code skipped:
            r4 = r7.sipStack.getStackLogger();
            r5 = new java.lang.StringBuilder();
            r5.append("Processing ");
            r5.append(r1);
            r5.append("nevents ");
            r5.append(r3.size());
            r4.logDebug(r5.toString());
     */
    /* JADX WARNING: Missing block: B:45:?, code skipped:
            deliverEvent(r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        try {
            ThreadHandle threadHandle = this.sipStack.getThreadAuditor().addCurrentThread();
            while (true) {
                synchronized (this.eventMutex) {
                    while (this.pendingEvents.isEmpty()) {
                        if (!this.isStopped) {
                            try {
                                threadHandle.ping();
                                this.eventMutex.wait(threadHandle.getPingIntervalInMillisecs());
                            } catch (InterruptedException e) {
                                if (this.sipStack.isLoggingEnabled()) {
                                    this.sipStack.getStackLogger().logDebug("Interrupted!");
                                }
                                if (this.sipStack.isLoggingEnabled() && !this.isStopped) {
                                    this.sipStack.getStackLogger().logFatalError("Event scanner exited abnormally");
                                }
                                return;
                            }
                        } else if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("Stopped event scanner!!");
                        }
                    }
                    LinkedList eventsToDeliver = this.pendingEvents;
                    this.pendingEvents = new LinkedList();
                }
            }
            while (true) {
            }
        } catch (Exception e2) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Unexpected exception caught while delivering event -- carrying on bravely", e2);
            }
        } catch (Throwable th) {
            if (this.sipStack.isLoggingEnabled() && !this.isStopped) {
                this.sipStack.getStackLogger().logFatalError("Event scanner exited abnormally");
            }
        }
    }
}
