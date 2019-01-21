package android.app.servertransaction;

import android.app.ActivityThread.ActivityClientRecord;
import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.util.IntArray;
import com.android.internal.annotations.VisibleForTesting;
import java.util.List;

public class TransactionExecutor {
    private static final boolean DEBUG_RESOLVER = false;
    private static final String TAG = "TransactionExecutor";
    private TransactionExecutorHelper mHelper = new TransactionExecutorHelper();
    private PendingTransactionActions mPendingActions = new PendingTransactionActions();
    private ClientTransactionHandler mTransactionHandler;

    public TransactionExecutor(ClientTransactionHandler clientTransactionHandler) {
        this.mTransactionHandler = clientTransactionHandler;
    }

    public void execute(ClientTransaction transaction) {
        IBinder token = transaction.getActivityToken();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Start resolving transaction for client: ");
        stringBuilder.append(this.mTransactionHandler);
        stringBuilder.append(", token: ");
        stringBuilder.append(token);
        log(stringBuilder.toString());
        executeCallbacks(transaction);
        executeLifecycleState(transaction);
        this.mPendingActions.clear();
        log("End resolving transaction");
    }

    @VisibleForTesting
    public void executeCallbacks(ClientTransaction transaction) {
        List<ClientTransactionItem> callbacks = transaction.getCallbacks();
        if (callbacks != null) {
            int finalState;
            log("Resolving callbacks");
            IBinder token = transaction.getActivityToken();
            ActivityClientRecord r = this.mTransactionHandler.getActivityClient(token);
            ActivityLifecycleItem finalStateRequest = transaction.getLifecycleStateRequest();
            if (finalStateRequest != null) {
                finalState = finalStateRequest.getTargetState();
            } else {
                finalState = -1;
            }
            int lastCallbackRequestingState = TransactionExecutorHelper.lastCallbackRequestingState(transaction);
            int size = callbacks.size();
            ActivityClientRecord r2 = r;
            int i = 0;
            while (i < size) {
                ClientTransactionItem item = (ClientTransactionItem) callbacks.get(i);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Resolving callback: ");
                stringBuilder.append(item);
                log(stringBuilder.toString());
                int postExecutionState = item.getPostExecutionState();
                int closestPreExecutionState = this.mHelper.getClosestPreExecutionState(r2, item.getPostExecutionState());
                if (closestPreExecutionState != -1) {
                    cycleToPath(r2, closestPreExecutionState);
                }
                item.execute(this.mTransactionHandler, token, this.mPendingActions);
                item.postExecute(this.mTransactionHandler, token, this.mPendingActions);
                if (r2 == null) {
                    r2 = this.mTransactionHandler.getActivityClient(token);
                }
                if (!(postExecutionState == -1 || r2 == null)) {
                    boolean shouldExcludeLastTransition = i == lastCallbackRequestingState && finalState == postExecutionState;
                    cycleToPath(r2, postExecutionState, shouldExcludeLastTransition);
                }
                i++;
            }
        }
    }

    private void executeLifecycleState(ClientTransaction transaction) {
        ActivityLifecycleItem lifecycleItem = transaction.getLifecycleStateRequest();
        if (lifecycleItem != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Resolving lifecycle state: ");
            stringBuilder.append(lifecycleItem);
            log(stringBuilder.toString());
            IBinder token = transaction.getActivityToken();
            ActivityClientRecord r = this.mTransactionHandler.getActivityClient(token);
            if (r != null) {
                cycleToPath(r, lifecycleItem.getTargetState(), true);
                lifecycleItem.execute(this.mTransactionHandler, token, this.mPendingActions);
                lifecycleItem.postExecute(this.mTransactionHandler, token, this.mPendingActions);
            }
        }
    }

    @VisibleForTesting
    public void cycleToPath(ActivityClientRecord r, int finish) {
        cycleToPath(r, finish, false);
    }

    private void cycleToPath(ActivityClientRecord r, int finish, boolean excludeLastState) {
        int start = r.getLifecycleState();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cycle from: ");
        stringBuilder.append(start);
        stringBuilder.append(" to: ");
        stringBuilder.append(finish);
        stringBuilder.append(" excludeLastState:");
        stringBuilder.append(excludeLastState);
        log(stringBuilder.toString());
        performLifecycleSequence(r, this.mHelper.getLifecyclePath(start, finish, excludeLastState));
    }

    private void performLifecycleSequence(ActivityClientRecord r, IntArray path) {
        ActivityClientRecord activityClientRecord = r;
        IntArray intArray = path;
        int size = path.size();
        for (int i = 0; i < size; i++) {
            int state = intArray.get(i);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Transitioning to state: ");
            stringBuilder.append(state);
            log(stringBuilder.toString());
            switch (state) {
                case 1:
                    this.mTransactionHandler.handleLaunchActivity(activityClientRecord, this.mPendingActions, null);
                    break;
                case 2:
                    this.mTransactionHandler.handleStartActivity(activityClientRecord, this.mPendingActions);
                    break;
                case 3:
                    this.mTransactionHandler.handleResumeActivity(activityClientRecord.token, false, activityClientRecord.isForward, "LIFECYCLER_RESUME_ACTIVITY");
                    break;
                case 4:
                    ClientTransactionHandler clientTransactionHandler = this.mTransactionHandler;
                    clientTransactionHandler.handlePauseActivity(activityClientRecord.token, false, false, 0, this.mPendingActions, "LIFECYCLER_PAUSE_ACTIVITY");
                    break;
                case 5:
                    this.mTransactionHandler.handleStopActivity(activityClientRecord.token, false, 0, this.mPendingActions, false, "LIFECYCLER_STOP_ACTIVITY");
                    break;
                case 6:
                    ClientTransactionHandler clientTransactionHandler2 = this.mTransactionHandler;
                    IBinder iBinder = activityClientRecord.token;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("performLifecycleSequence. cycling to:");
                    stringBuilder.append(intArray.get(size - 1));
                    clientTransactionHandler2.handleDestroyActivity(iBinder, false, 0, false, stringBuilder.toString());
                    break;
                case 7:
                    this.mTransactionHandler.performRestartActivity(activityClientRecord.token, false);
                    break;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected lifecycle state: ");
                    stringBuilder.append(state);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    private static void log(String message) {
    }
}
