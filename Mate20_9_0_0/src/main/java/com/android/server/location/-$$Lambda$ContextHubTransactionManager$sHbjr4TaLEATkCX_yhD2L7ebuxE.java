package com.android.server.location;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubTransactionManager$sHbjr4TaLEATkCX_yhD2L7ebuxE implements Runnable {
    private final /* synthetic */ ContextHubTransactionManager f$0;
    private final /* synthetic */ ContextHubServiceTransaction f$1;

    public /* synthetic */ -$$Lambda$ContextHubTransactionManager$sHbjr4TaLEATkCX_yhD2L7ebuxE(ContextHubTransactionManager contextHubTransactionManager, ContextHubServiceTransaction contextHubServiceTransaction) {
        this.f$0 = contextHubTransactionManager;
        this.f$1 = contextHubServiceTransaction;
    }

    public final void run() {
        ContextHubTransactionManager.lambda$startNextTransaction$0(this.f$0, this.f$1);
    }
}
