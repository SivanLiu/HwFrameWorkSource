package defpackage;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;

/* renamed from: t  reason: default package */
public final class t implements Executor {
    private final Handler handler = new Handler(Looper.getMainLooper());

    public final void execute(Runnable runnable) {
        this.handler.post(runnable);
    }
}
