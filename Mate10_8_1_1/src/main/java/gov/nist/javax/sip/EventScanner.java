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
            this.sipStack.getStackLogger().logDebug("addEvent " + eventWrapper);
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
            this.sipStack.getStackLogger().logDebug("sipEvent = " + sipEvent + "source = " + sipEvent.getSource());
        }
        if (sipEvent instanceof IOExceptionEvent) {
            sipListener = this.sipStack.getSipListener();
        } else {
            sipListener = ((SipProviderImpl) sipEvent.getSource()).getSipListener();
        }
        StackLogger stackLogger;
        StackLogger stackLogger2;
        if (sipEvent instanceof RequestEvent) {
            try {
                SIPRequest sipRequest = (SIPRequest) ((RequestEvent) sipEvent).getRequest();
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger = this.sipStack.getStackLogger();
                    stackLogger2 = stackLogger;
                    stackLogger2.logDebug("deliverEvent : " + sipRequest.getFirstLine() + " transaction " + eventWrapper.transaction + " sipEvent.serverTx = " + ((RequestEvent) sipEvent).getServerTransaction());
                }
                SIPServerTransaction tx = (SIPServerTransaction) this.sipStack.findTransaction(sipRequest, true);
                if (tx == null || (tx.passToListener() ^ 1) == 0) {
                    if (this.sipStack.findPendingTransaction(sipRequest) != null) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("transaction already exists!!");
                        }
                        if (this.sipStack.isLoggingEnabled()) {
                            stackLogger = this.sipStack.getStackLogger();
                            stackLogger2 = stackLogger;
                            stackLogger2.logDebug("Done processing Message " + ((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
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
                        this.sipStack.getStackLogger().logDebug("transaction already exists! " + tx);
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        stackLogger = this.sipStack.getStackLogger();
                        stackLogger2 = stackLogger;
                        stackLogger2.logDebug("Done processing Message " + ((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
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
                    this.sipStack.getStackLogger().logDebug("Calling listener " + sipRequest.getFirstLine());
                    this.sipStack.getStackLogger().logDebug("Calling listener " + eventWrapper.transaction);
                }
                if (sipListener != null) {
                    sipListener.processRequest((RequestEvent) sipEvent);
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Done processing Message " + sipRequest.getFirstLine());
                }
                if (eventWrapper.transaction != null) {
                    SIPDialog dialog = (SIPDialog) eventWrapper.transaction.getDialog();
                    if (dialog != null) {
                        dialog.requestConsumed();
                    }
                }
            } catch (Exception ex) {
                this.sipStack.getStackLogger().logException(ex);
            } catch (Throwable th) {
                Throwable th2 = th;
                if (this.sipStack.isLoggingEnabled()) {
                    StackLogger stackLogger3 = this.sipStack.getStackLogger();
                    stackLogger2 = stackLogger3;
                    stackLogger2.logDebug("Done processing Message " + ((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
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
                stackLogger = this.sipStack.getStackLogger();
                stackLogger2 = stackLogger;
                stackLogger2.logDebug("Done processing Message " + ((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
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
                ResponseEvent responseEvent = (ResponseEvent) sipEvent;
                SIPResponse sipResponse = (SIPResponse) responseEvent.getResponse();
                SIPDialog sipDialog = (SIPDialog) responseEvent.getDialog();
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Calling listener for " + sipResponse.getFirstLine());
                }
                if (sipListener != null) {
                    SIPTransaction tx2 = eventWrapper.transaction;
                    if (tx2 != null) {
                        tx2.setPassToListener();
                    }
                    sipListener.processResponse((ResponseEvent) sipEvent);
                }
                if (sipDialog != null && ((sipDialog.getState() == null || (sipDialog.getState().equals(DialogState.TERMINATED) ^ 1) != 0) && (sipResponse.getStatusCode() == 481 || sipResponse.getStatusCode() == 408))) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Removing dialog on 408 or 481 response");
                    }
                    sipDialog.doDeferredDelete();
                }
                if (sipResponse.getCSeq().getMethod().equals("INVITE") && sipDialog != null && sipResponse.getStatusCode() == 200) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Warning! unacknowledged dialog. " + sipDialog.getState());
                    }
                    sipDialog.doDeferredDeleteIfNoAckSent(sipResponse.getCSeq().getSeqNumber());
                }
            } catch (Exception ex2) {
                this.sipStack.getStackLogger().logException(ex2);
            } catch (Throwable th3) {
                if (eventWrapper.transaction != null && eventWrapper.transaction.passToListener()) {
                    eventWrapper.transaction.releaseSem();
                }
            }
            SIPClientTransaction ct = eventWrapper.transaction;
            if (!(ct == null || TransactionState.COMPLETED != ct.getState() || ct.getOriginalRequest() == null || (ct.getOriginalRequest().getMethod().equals("INVITE") ^ 1) == 0)) {
                ct.clearState();
            }
            if (eventWrapper.transaction != null && eventWrapper.transaction.passToListener()) {
                eventWrapper.transaction.releaseSem();
            }
        } else if (sipEvent instanceof TimeoutEvent) {
            if (sipListener != null) {
                try {
                    sipListener.processTimeout((TimeoutEvent) sipEvent);
                } catch (Exception ex22) {
                    this.sipStack.getStackLogger().logException(ex22);
                }
            }
        } else if (sipEvent instanceof DialogTimeoutEvent) {
            if (sipListener != null) {
                try {
                    if (sipListener instanceof SipListenerExt) {
                        ((SipListenerExt) sipListener).processDialogTimeout((DialogTimeoutEvent) sipEvent);
                    }
                } catch (Exception ex222) {
                    this.sipStack.getStackLogger().logException(ex222);
                }
            }
        } else if (sipEvent instanceof IOExceptionEvent) {
            if (sipListener != null) {
                try {
                    sipListener.processIOException((IOExceptionEvent) sipEvent);
                } catch (Exception ex2222) {
                    this.sipStack.getStackLogger().logException(ex2222);
                }
            }
        } else if (sipEvent instanceof TransactionTerminatedEvent) {
            try {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("About to deliver transactionTerminatedEvent");
                    stackLogger = this.sipStack.getStackLogger();
                    stackLogger2 = stackLogger;
                    stackLogger2.logDebug("tx = " + ((TransactionTerminatedEvent) sipEvent).getClientTransaction());
                    stackLogger = this.sipStack.getStackLogger();
                    stackLogger2 = stackLogger;
                    stackLogger2.logDebug("tx = " + ((TransactionTerminatedEvent) sipEvent).getServerTransaction());
                }
                if (sipListener != null) {
                    sipListener.processTransactionTerminated((TransactionTerminatedEvent) sipEvent);
                }
            } catch (AbstractMethodError e) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logWarning("Unable to call sipListener.processTransactionTerminated");
                }
            } catch (Exception ex22222) {
                this.sipStack.getStackLogger().logException(ex22222);
            }
        } else if (!(sipEvent instanceof DialogTerminatedEvent)) {
            this.sipStack.getStackLogger().logFatalError("bad event" + sipEvent);
        } else if (sipListener != null) {
            try {
                sipListener.processDialogTerminated((DialogTerminatedEvent) sipEvent);
            } catch (AbstractMethodError e2) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logWarning("Unable to call sipListener.processDialogTerminated");
                }
            } catch (Exception ex222222) {
                this.sipStack.getStackLogger().logException(ex222222);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        try {
            ThreadHandle threadHandle = this.sipStack.getThreadAuditor().addCurrentThread();
            loop0:
            while (true) {
                synchronized (this.eventMutex) {
                    while (this.pendingEvents.isEmpty()) {
                        if (this.isStopped) {
                            break loop0;
                        }
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
                    }
                    LinkedList eventsToDeliver = this.pendingEvents;
                    this.pendingEvents = new LinkedList();
                }
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Stopped event scanner!!");
            }
            if (this.sipStack.isLoggingEnabled() && !this.isStopped) {
                this.sipStack.getStackLogger().logFatalError("Event scanner exited abnormally");
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
