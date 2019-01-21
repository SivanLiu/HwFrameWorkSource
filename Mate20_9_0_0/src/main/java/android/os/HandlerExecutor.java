package android.os;

import com.android.internal.util.Preconditions;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class HandlerExecutor implements Executor {
    private final Handler mHandler;

    public HandlerExecutor(Handler handler) {
        this.mHandler = (Handler) Preconditions.checkNotNull(handler);
    }

    public void execute(Runnable command) {
        if (!this.mHandler.post(command)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mHandler);
            stringBuilder.append(" is shutting down");
            throw new RejectedExecutionException(stringBuilder.toString());
        }
    }
}
