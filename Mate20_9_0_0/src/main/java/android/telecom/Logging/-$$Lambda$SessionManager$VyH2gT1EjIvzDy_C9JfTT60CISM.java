package android.telecom.Logging;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SessionManager$VyH2gT1EjIvzDy_C9JfTT60CISM implements Runnable {
    private final /* synthetic */ SessionManager f$0;

    public /* synthetic */ -$$Lambda$SessionManager$VyH2gT1EjIvzDy_C9JfTT60CISM(SessionManager sessionManager) {
        this.f$0 = sessionManager;
    }

    public final void run() {
        this.f$0.cleanupStaleSessions(this.f$0.getSessionCleanupTimeoutMs());
    }
}
