package android.view.textservice;

import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.textservice.ISpellCheckerSession;
import com.android.internal.textservice.ISpellCheckerSessionListener;
import com.android.internal.textservice.ITextServicesManager;
import com.android.internal.textservice.ITextServicesSessionListener;
import com.android.internal.textservice.ITextServicesSessionListener.Stub;
import dalvik.system.CloseGuard;
import java.util.LinkedList;
import java.util.Queue;

public class SpellCheckerSession {
    private static final boolean DBG = false;
    private static final int MSG_ON_GET_SUGGESTION_MULTIPLE = 1;
    private static final int MSG_ON_GET_SUGGESTION_MULTIPLE_FOR_SENTENCE = 2;
    public static final String SERVICE_META_DATA = "android.view.textservice.scs";
    private static final String TAG = SpellCheckerSession.class.getSimpleName();
    private final CloseGuard mGuard = CloseGuard.get();
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SpellCheckerSession.this.handleOnGetSuggestionsMultiple((SuggestionsInfo[]) msg.obj);
                    return;
                case 2:
                    SpellCheckerSession.this.handleOnGetSentenceSuggestionsMultiple((SentenceSuggestionsInfo[]) msg.obj);
                    return;
                default:
                    return;
            }
        }
    };
    private final InternalListener mInternalListener;
    private final SpellCheckerInfo mSpellCheckerInfo;
    private final SpellCheckerSessionListener mSpellCheckerSessionListener;
    private final SpellCheckerSessionListenerImpl mSpellCheckerSessionListenerImpl;
    private final ITextServicesManager mTextServicesManager;

    public interface SpellCheckerSessionListener {
        void onGetSentenceSuggestions(SentenceSuggestionsInfo[] sentenceSuggestionsInfoArr);

        void onGetSuggestions(SuggestionsInfo[] suggestionsInfoArr);
    }

    private static final class InternalListener extends Stub {
        private final SpellCheckerSessionListenerImpl mParentSpellCheckerSessionListenerImpl;

        public InternalListener(SpellCheckerSessionListenerImpl spellCheckerSessionListenerImpl) {
            this.mParentSpellCheckerSessionListenerImpl = spellCheckerSessionListenerImpl;
        }

        public void onServiceConnected(ISpellCheckerSession session) {
            this.mParentSpellCheckerSessionListenerImpl.onServiceConnected(session);
        }
    }

    private static final class SpellCheckerSessionListenerImpl extends ISpellCheckerSessionListener.Stub {
        private static final int STATE_CLOSED_AFTER_CONNECTION = 2;
        private static final int STATE_CLOSED_BEFORE_CONNECTION = 3;
        private static final int STATE_CONNECTED = 1;
        private static final int STATE_WAIT_CONNECTION = 0;
        private static final int TASK_CANCEL = 1;
        private static final int TASK_CLOSE = 3;
        private static final int TASK_GET_SUGGESTIONS_MULTIPLE = 2;
        private static final int TASK_GET_SUGGESTIONS_MULTIPLE_FOR_SENTENCE = 4;
        private Handler mAsyncHandler;
        private Handler mHandler;
        private ISpellCheckerSession mISpellCheckerSession;
        private final Queue<SpellCheckerParams> mPendingTasks = new LinkedList();
        private int mState = 0;
        private HandlerThread mThread;

        private static class SpellCheckerParams {
            public final boolean mSequentialWords;
            public ISpellCheckerSession mSession;
            public final int mSuggestionsLimit;
            public final TextInfo[] mTextInfos;
            public final int mWhat;

            public SpellCheckerParams(int what, TextInfo[] textInfos, int suggestionsLimit, boolean sequentialWords) {
                this.mWhat = what;
                this.mTextInfos = textInfos;
                this.mSuggestionsLimit = suggestionsLimit;
                this.mSequentialWords = sequentialWords;
            }
        }

        private static String taskToString(int task) {
            switch (task) {
                case 1:
                    return "TASK_CANCEL";
                case 2:
                    return "TASK_GET_SUGGESTIONS_MULTIPLE";
                case 3:
                    return "TASK_CLOSE";
                case 4:
                    return "TASK_GET_SUGGESTIONS_MULTIPLE_FOR_SENTENCE";
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected task=");
                    stringBuilder.append(task);
                    return stringBuilder.toString();
            }
        }

        private static String stateToString(int state) {
            switch (state) {
                case 0:
                    return "STATE_WAIT_CONNECTION";
                case 1:
                    return "STATE_CONNECTED";
                case 2:
                    return "STATE_CLOSED_AFTER_CONNECTION";
                case 3:
                    return "STATE_CLOSED_BEFORE_CONNECTION";
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected state=");
                    stringBuilder.append(state);
                    return stringBuilder.toString();
            }
        }

        public SpellCheckerSessionListenerImpl(Handler handler) {
            this.mHandler = handler;
        }

        private void processTask(ISpellCheckerSession session, SpellCheckerParams scp, boolean async) {
            String access$200;
            StringBuilder stringBuilder;
            if (async || this.mAsyncHandler == null) {
                switch (scp.mWhat) {
                    case 1:
                        try {
                            session.onCancel();
                            break;
                        } catch (RemoteException e) {
                            access$200 = SpellCheckerSession.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to cancel ");
                            stringBuilder.append(e);
                            Log.e(access$200, stringBuilder.toString());
                            break;
                        }
                    case 2:
                        try {
                            session.onGetSuggestionsMultiple(scp.mTextInfos, scp.mSuggestionsLimit, scp.mSequentialWords);
                            break;
                        } catch (RemoteException e2) {
                            access$200 = SpellCheckerSession.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to get suggestions ");
                            stringBuilder.append(e2);
                            Log.e(access$200, stringBuilder.toString());
                            break;
                        }
                    case 3:
                        try {
                            session.onClose();
                            break;
                        } catch (RemoteException e22) {
                            access$200 = SpellCheckerSession.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to close ");
                            stringBuilder.append(e22);
                            Log.e(access$200, stringBuilder.toString());
                            break;
                        }
                    case 4:
                        try {
                            session.onGetSentenceSuggestionsMultiple(scp.mTextInfos, scp.mSuggestionsLimit);
                            break;
                        } catch (RemoteException e222) {
                            access$200 = SpellCheckerSession.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to get suggestions ");
                            stringBuilder.append(e222);
                            Log.e(access$200, stringBuilder.toString());
                            break;
                        }
                }
            }
            scp.mSession = session;
            this.mAsyncHandler.sendMessage(Message.obtain(this.mAsyncHandler, 1, scp));
            if (scp.mWhat == 3) {
                synchronized (this) {
                    processCloseLocked();
                }
            }
        }

        private void processCloseLocked() {
            this.mISpellCheckerSession = null;
            if (this.mThread != null) {
                this.mThread.quit();
            }
            this.mHandler = null;
            this.mPendingTasks.clear();
            this.mThread = null;
            this.mAsyncHandler = null;
            switch (this.mState) {
                case 0:
                    this.mState = 3;
                    return;
                case 1:
                    this.mState = 2;
                    return;
                default:
                    String access$200 = SpellCheckerSession.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("processCloseLocked is called unexpectedly. mState=");
                    stringBuilder.append(stateToString(this.mState));
                    Log.e(access$200, stringBuilder.toString());
                    return;
            }
        }

        public void onServiceConnected(ISpellCheckerSession session) {
            synchronized (this) {
                int i = this.mState;
                if (i != 0) {
                    if (i != 3) {
                        String access$200 = SpellCheckerSession.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ignoring onServiceConnected due to unexpected mState=");
                        stringBuilder.append(stateToString(this.mState));
                        Log.e(access$200, stringBuilder.toString());
                        return;
                    }
                } else if (session == null) {
                    Log.e(SpellCheckerSession.TAG, "ignoring onServiceConnected due to session=null");
                } else {
                    this.mISpellCheckerSession = session;
                    if ((session.asBinder() instanceof Binder) && this.mThread == null) {
                        this.mThread = new HandlerThread("SpellCheckerSession", 10);
                        this.mThread.start();
                        this.mAsyncHandler = new Handler(this.mThread.getLooper()) {
                            public void handleMessage(Message msg) {
                                SpellCheckerParams scp = msg.obj;
                                SpellCheckerSessionListenerImpl.this.processTask(scp.mSession, scp, true);
                            }
                        };
                    }
                    this.mState = 1;
                    while (!this.mPendingTasks.isEmpty()) {
                        processTask(session, (SpellCheckerParams) this.mPendingTasks.poll(), false);
                    }
                }
            }
        }

        public void cancel() {
            processOrEnqueueTask(new SpellCheckerParams(1, null, 0, false));
        }

        public void getSuggestionsMultiple(TextInfo[] textInfos, int suggestionsLimit, boolean sequentialWords) {
            processOrEnqueueTask(new SpellCheckerParams(2, textInfos, suggestionsLimit, sequentialWords));
        }

        public void getSentenceSuggestionsMultiple(TextInfo[] textInfos, int suggestionsLimit) {
            processOrEnqueueTask(new SpellCheckerParams(4, textInfos, suggestionsLimit, false));
        }

        public void close() {
            processOrEnqueueTask(new SpellCheckerParams(3, null, 0, false));
        }

        public boolean isDisconnected() {
            boolean z;
            synchronized (this) {
                z = true;
                if (this.mState == 1) {
                    z = false;
                }
            }
            return z;
        }

        /* JADX WARNING: Missing block: B:9:0x0010, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:36:0x007d, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void processOrEnqueueTask(SpellCheckerParams scp) {
            synchronized (this) {
                if (!(scp.mWhat == 3 && (this.mState == 2 || this.mState == 3))) {
                    if (this.mState != 0 && this.mState != 1) {
                        String access$200 = SpellCheckerSession.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ignoring processOrEnqueueTask due to unexpected mState=");
                        stringBuilder.append(stateToString(this.mState));
                        stringBuilder.append(" scp.mWhat=");
                        stringBuilder.append(taskToString(scp.mWhat));
                        Log.e(access$200, stringBuilder.toString());
                    } else if (this.mState != 0) {
                        ISpellCheckerSession session = this.mISpellCheckerSession;
                        processTask(session, scp, false);
                    } else if (scp.mWhat == 3) {
                        processCloseLocked();
                    } else {
                        SpellCheckerParams closeTask = null;
                        if (scp.mWhat == 1) {
                            while (!this.mPendingTasks.isEmpty()) {
                                SpellCheckerParams tmp = (SpellCheckerParams) this.mPendingTasks.poll();
                                if (tmp.mWhat == 3) {
                                    closeTask = tmp;
                                }
                            }
                        }
                        this.mPendingTasks.offer(scp);
                        if (closeTask != null) {
                            this.mPendingTasks.offer(closeTask);
                        }
                    }
                }
            }
        }

        public void onGetSuggestions(SuggestionsInfo[] results) {
            synchronized (this) {
                if (this.mHandler != null) {
                    this.mHandler.sendMessage(Message.obtain(this.mHandler, 1, results));
                }
            }
        }

        public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
            synchronized (this) {
                if (this.mHandler != null) {
                    this.mHandler.sendMessage(Message.obtain(this.mHandler, 2, results));
                }
            }
        }
    }

    public SpellCheckerSession(SpellCheckerInfo info, ITextServicesManager tsm, SpellCheckerSessionListener listener) {
        if (info == null || listener == null || tsm == null) {
            throw new NullPointerException();
        }
        this.mSpellCheckerInfo = info;
        this.mSpellCheckerSessionListenerImpl = new SpellCheckerSessionListenerImpl(this.mHandler);
        this.mInternalListener = new InternalListener(this.mSpellCheckerSessionListenerImpl);
        this.mTextServicesManager = tsm;
        this.mSpellCheckerSessionListener = listener;
        this.mGuard.open("finishSession");
    }

    public boolean isSessionDisconnected() {
        return this.mSpellCheckerSessionListenerImpl.isDisconnected();
    }

    public SpellCheckerInfo getSpellChecker() {
        return this.mSpellCheckerInfo;
    }

    public void cancel() {
        this.mSpellCheckerSessionListenerImpl.cancel();
    }

    public void close() {
        this.mGuard.close();
        try {
            this.mSpellCheckerSessionListenerImpl.close();
            this.mTextServicesManager.finishSpellCheckerService(this.mSpellCheckerSessionListenerImpl);
        } catch (RemoteException e) {
        }
    }

    public void getSentenceSuggestions(TextInfo[] textInfos, int suggestionsLimit) {
        this.mSpellCheckerSessionListenerImpl.getSentenceSuggestionsMultiple(textInfos, suggestionsLimit);
    }

    @Deprecated
    public void getSuggestions(TextInfo textInfo, int suggestionsLimit) {
        getSuggestions(new TextInfo[]{textInfo}, suggestionsLimit, false);
    }

    @Deprecated
    public void getSuggestions(TextInfo[] textInfos, int suggestionsLimit, boolean sequentialWords) {
        this.mSpellCheckerSessionListenerImpl.getSuggestionsMultiple(textInfos, suggestionsLimit, sequentialWords);
    }

    private void handleOnGetSuggestionsMultiple(SuggestionsInfo[] suggestionInfos) {
        this.mSpellCheckerSessionListener.onGetSuggestions(suggestionInfos);
    }

    private void handleOnGetSentenceSuggestionsMultiple(SentenceSuggestionsInfo[] suggestionInfos) {
        this.mSpellCheckerSessionListener.onGetSentenceSuggestions(suggestionInfos);
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mGuard != null) {
                this.mGuard.warnIfOpen();
                close();
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    public ITextServicesSessionListener getTextServicesSessionListener() {
        return this.mInternalListener;
    }

    public ISpellCheckerSessionListener getSpellCheckerSessionListener() {
        return this.mSpellCheckerSessionListenerImpl;
    }
}
