package com.android.server.location;

import android.hardware.contexthub.V1_0.IContexthub;
import android.hardware.location.IContextHubTransactionCallback;
import android.hardware.location.NanoAppBinary;
import android.hardware.location.NanoAppState;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class ContextHubTransactionManager {
    private static final int MAX_PENDING_REQUESTS = 10000;
    private static final String TAG = "ContextHubTransactionManager";
    private final ContextHubClientManager mClientManager;
    private final IContexthub mContextHubProxy;
    private final NanoAppStateManager mNanoAppStateManager;
    private final AtomicInteger mNextAvailableId = new AtomicInteger();
    private final ScheduledThreadPoolExecutor mTimeoutExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> mTimeoutFuture = null;
    private final ArrayDeque<ContextHubServiceTransaction> mTransactionQueue = new ArrayDeque();

    ContextHubTransactionManager(IContexthub contextHubProxy, ContextHubClientManager clientManager, NanoAppStateManager nanoAppStateManager) {
        this.mContextHubProxy = contextHubProxy;
        this.mClientManager = clientManager;
        this.mNanoAppStateManager = nanoAppStateManager;
    }

    ContextHubServiceTransaction createLoadTransaction(int contextHubId, NanoAppBinary nanoAppBinary, IContextHubTransactionCallback onCompleteCallback) {
        final NanoAppBinary nanoAppBinary2 = nanoAppBinary;
        final int i = contextHubId;
        final IContextHubTransactionCallback iContextHubTransactionCallback = onCompleteCallback;
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 0) {
            int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.loadNanoApp(i, ContextHubServiceUtil.createHidlNanoAppBinary(nanoAppBinary2), getTransactionId());
                } catch (RemoteException e) {
                    String str = ContextHubTransactionManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("RemoteException while trying to load nanoapp with ID 0x");
                    stringBuilder.append(Long.toHexString(nanoAppBinary2.getNanoAppId()));
                    Log.e(str, stringBuilder.toString(), e);
                    return 1;
                }
            }

            void onTransactionComplete(int result) {
                if (result == 0) {
                    ContextHubTransactionManager.this.mNanoAppStateManager.addNanoAppInstance(i, nanoAppBinary2.getNanoAppId(), nanoAppBinary2.getNanoAppVersion());
                }
                try {
                    iContextHubTransactionCallback.onTransactionComplete(result);
                    if (result == 0) {
                        ContextHubTransactionManager.this.mClientManager.onNanoAppLoaded(i, nanoAppBinary2.getNanoAppId());
                    }
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    ContextHubServiceTransaction createUnloadTransaction(int contextHubId, long nanoAppId, IContextHubTransactionCallback onCompleteCallback) {
        final int i = contextHubId;
        final long j = nanoAppId;
        final IContextHubTransactionCallback iContextHubTransactionCallback = onCompleteCallback;
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 1) {
            int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.unloadNanoApp(i, j, getTransactionId());
                } catch (RemoteException e) {
                    String str = ContextHubTransactionManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("RemoteException while trying to unload nanoapp with ID 0x");
                    stringBuilder.append(Long.toHexString(j));
                    Log.e(str, stringBuilder.toString(), e);
                    return 1;
                }
            }

            void onTransactionComplete(int result) {
                if (result == 0) {
                    ContextHubTransactionManager.this.mNanoAppStateManager.removeNanoAppInstance(i, j);
                }
                try {
                    iContextHubTransactionCallback.onTransactionComplete(result);
                    if (result == 0) {
                        ContextHubTransactionManager.this.mClientManager.onNanoAppUnloaded(i, j);
                    }
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    ContextHubServiceTransaction createEnableTransaction(int contextHubId, long nanoAppId, IContextHubTransactionCallback onCompleteCallback) {
        final int i = contextHubId;
        final long j = nanoAppId;
        final IContextHubTransactionCallback iContextHubTransactionCallback = onCompleteCallback;
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 2) {
            int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.enableNanoApp(i, j, getTransactionId());
                } catch (RemoteException e) {
                    String str = ContextHubTransactionManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("RemoteException while trying to enable nanoapp with ID 0x");
                    stringBuilder.append(Long.toHexString(j));
                    Log.e(str, stringBuilder.toString(), e);
                    return 1;
                }
            }

            void onTransactionComplete(int result) {
                try {
                    iContextHubTransactionCallback.onTransactionComplete(result);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    ContextHubServiceTransaction createDisableTransaction(int contextHubId, long nanoAppId, IContextHubTransactionCallback onCompleteCallback) {
        final int i = contextHubId;
        final long j = nanoAppId;
        final IContextHubTransactionCallback iContextHubTransactionCallback = onCompleteCallback;
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 3) {
            int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.disableNanoApp(i, j, getTransactionId());
                } catch (RemoteException e) {
                    String str = ContextHubTransactionManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("RemoteException while trying to disable nanoapp with ID 0x");
                    stringBuilder.append(Long.toHexString(j));
                    Log.e(str, stringBuilder.toString(), e);
                    return 1;
                }
            }

            void onTransactionComplete(int result) {
                try {
                    iContextHubTransactionCallback.onTransactionComplete(result);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    ContextHubServiceTransaction createQueryTransaction(int contextHubId, IContextHubTransactionCallback onCompleteCallback) {
        final int i = contextHubId;
        final IContextHubTransactionCallback iContextHubTransactionCallback = onCompleteCallback;
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 4) {
            int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.queryApps(i);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to query for nanoapps", e);
                    return 1;
                }
            }

            void onTransactionComplete(int result) {
                onQueryResponse(result, Collections.emptyList());
            }

            void onQueryResponse(int result, List<NanoAppState> nanoAppStateList) {
                try {
                    iContextHubTransactionCallback.onQueryResponse(result, nanoAppStateList);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onQueryComplete", e);
                }
            }
        };
    }

    synchronized void addTransaction(ContextHubServiceTransaction transaction) throws IllegalStateException {
        if (this.mTransactionQueue.size() != 10000) {
            this.mTransactionQueue.add(transaction);
            if (this.mTransactionQueue.size() == 1) {
                startNextTransaction();
            }
        } else {
            throw new IllegalStateException("Transaction queue is full (capacity = 10000)");
        }
    }

    synchronized void onTransactionResponse(int transactionId, int result) {
        ContextHubServiceTransaction transaction = (ContextHubServiceTransaction) this.mTransactionQueue.peek();
        if (transaction == null) {
            Log.w(TAG, "Received unexpected transaction response (no transaction pending)");
        } else if (transaction.getTransactionId() != transactionId) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Received unexpected transaction response (expected ID = ");
            stringBuilder.append(transaction.getTransactionId());
            stringBuilder.append(", received ID = ");
            stringBuilder.append(transactionId);
            stringBuilder.append(")");
            Log.w(str, stringBuilder.toString());
        } else {
            int i;
            if (result == 0) {
                i = 0;
            } else {
                i = 5;
            }
            transaction.onTransactionComplete(i);
            removeTransactionAndStartNext();
        }
    }

    synchronized void onQueryResponse(List<NanoAppState> nanoAppStateList) {
        ContextHubServiceTransaction transaction = (ContextHubServiceTransaction) this.mTransactionQueue.peek();
        if (transaction == null) {
            Log.w(TAG, "Received unexpected query response (no transaction pending)");
        } else if (transaction.getTransactionType() != 4) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Received unexpected query response (expected ");
            stringBuilder.append(transaction);
            stringBuilder.append(")");
            Log.w(str, stringBuilder.toString());
        } else {
            transaction.onQueryResponse(0, nanoAppStateList);
            removeTransactionAndStartNext();
        }
    }

    synchronized void onHubReset() {
        if (((ContextHubServiceTransaction) this.mTransactionQueue.peek()) != null) {
            removeTransactionAndStartNext();
        }
    }

    private void removeTransactionAndStartNext() {
        this.mTimeoutFuture.cancel(false);
        ((ContextHubServiceTransaction) this.mTransactionQueue.remove()).setComplete();
        if (!this.mTransactionQueue.isEmpty()) {
            startNextTransaction();
        }
    }

    private void startNextTransaction() {
        int result = 1;
        while (result != 0 && !this.mTransactionQueue.isEmpty()) {
            ContextHubServiceTransaction transaction = (ContextHubServiceTransaction) this.mTransactionQueue.peek();
            result = transaction.onTransact();
            if (result == 0) {
                this.mTimeoutFuture = this.mTimeoutExecutor.schedule(new -$$Lambda$ContextHubTransactionManager$sHbjr4TaLEATkCX_yhD2L7ebuxE(this, transaction), transaction.getTimeout(TimeUnit.SECONDS), TimeUnit.SECONDS);
            } else {
                transaction.onTransactionComplete(ContextHubServiceUtil.toTransactionResult(result));
                this.mTransactionQueue.remove();
            }
        }
    }

    public static /* synthetic */ void lambda$startNextTransaction$0(ContextHubTransactionManager contextHubTransactionManager, ContextHubServiceTransaction transaction) {
        synchronized (contextHubTransactionManager) {
            if (!transaction.isComplete()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(transaction);
                stringBuilder.append(" timed out");
                Log.d(str, stringBuilder.toString());
                transaction.onTransactionComplete(6);
                contextHubTransactionManager.removeTransactionAndStartNext();
            }
        }
    }
}
