package android.app;

import android.app.SharedPreferencesImpl.EditorImpl;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SharedPreferencesImpl$EditorImpl$3CAjkhzA131V3V-sLfP2uy0FWZ0 implements Runnable {
    private final /* synthetic */ EditorImpl f$0;
    private final /* synthetic */ MemoryCommitResult f$1;

    public /* synthetic */ -$$Lambda$SharedPreferencesImpl$EditorImpl$3CAjkhzA131V3V-sLfP2uy0FWZ0(EditorImpl editorImpl, MemoryCommitResult memoryCommitResult) {
        this.f$0 = editorImpl;
        this.f$1 = memoryCommitResult;
    }

    public final void run() {
        this.f$0.notifyListeners(this.f$1);
    }
}
