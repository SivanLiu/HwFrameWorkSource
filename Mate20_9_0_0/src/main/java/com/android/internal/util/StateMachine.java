package com.android.internal.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class StateMachine {
    public static final boolean HANDLED = true;
    private static final boolean HWFLOW;
    public static final boolean NOT_HANDLED = false;
    private static final int SM_INIT_CMD = -2;
    private static final int SM_QUIT_CMD = -1;
    private static final String TAG = "StateMachine";
    private String mName;
    private SmHandler mSmHandler;
    private HandlerThread mSmThread;

    public static class LogRec {
        private IState mDstState;
        private String mInfo;
        private IState mOrgState;
        private StateMachine mSm;
        private IState mState;
        private long mTime;
        private int mWhat;

        LogRec(StateMachine sm, Message msg, String info, IState state, IState orgState, IState transToState) {
            update(sm, msg, info, state, orgState, transToState);
        }

        public void update(StateMachine sm, Message msg, String info, IState state, IState orgState, IState dstState) {
            this.mSm = sm;
            this.mTime = System.currentTimeMillis();
            this.mWhat = msg != null ? msg.what : 0;
            this.mInfo = info;
            this.mState = state;
            this.mOrgState = orgState;
            this.mDstState = dstState;
        }

        public long getTime() {
            return this.mTime;
        }

        public long getWhat() {
            return (long) this.mWhat;
        }

        public String getInfo() {
            return this.mInfo;
        }

        public IState getState() {
            return this.mState;
        }

        public IState getDestState() {
            return this.mDstState;
        }

        public IState getOriginalState() {
            return this.mOrgState;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("time=");
            Calendar.getInstance().setTimeInMillis(this.mTime);
            sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", new Object[]{c, c, c, c, c, c}));
            sb.append(" processed=");
            sb.append(this.mState == null ? "<null>" : this.mState.getName());
            sb.append(" org=");
            sb.append(this.mOrgState == null ? "<null>" : this.mOrgState.getName());
            sb.append(" dest=");
            sb.append(this.mDstState == null ? "<null>" : this.mDstState.getName());
            sb.append(" what=");
            String what = this.mSm != null ? this.mSm.getWhatToString(this.mWhat) : "";
            if (TextUtils.isEmpty(what)) {
                sb.append(this.mWhat);
                sb.append("(0x");
                sb.append(Integer.toHexString(this.mWhat));
                sb.append(")");
            } else {
                sb.append(what);
            }
            if (!TextUtils.isEmpty(this.mInfo)) {
                sb.append(" ");
                sb.append(this.mInfo);
            }
            return sb.toString();
        }
    }

    private static class LogRecords {
        private static final int DEFAULT_SIZE = 20;
        private int mCount;
        private boolean mLogOnlyTransitions;
        private Vector<LogRec> mLogRecVector;
        private int mMaxSize;
        private int mOldestIndex;

        private LogRecords() {
            this.mLogRecVector = new Vector();
            this.mMaxSize = 20;
            this.mOldestIndex = 0;
            this.mCount = 0;
            this.mLogOnlyTransitions = false;
        }

        synchronized void setSize(int maxSize) {
            this.mMaxSize = maxSize;
            this.mOldestIndex = 0;
            this.mCount = 0;
            this.mLogRecVector.clear();
        }

        synchronized void setLogOnlyTransitions(boolean enable) {
            this.mLogOnlyTransitions = enable;
        }

        synchronized boolean logOnlyTransitions() {
            return this.mLogOnlyTransitions;
        }

        synchronized int size() {
            return this.mLogRecVector.size();
        }

        synchronized int count() {
            return this.mCount;
        }

        synchronized void cleanup() {
            this.mLogRecVector.clear();
        }

        synchronized LogRec get(int index) {
            int nextIndex = this.mOldestIndex + index;
            if (nextIndex >= this.mMaxSize) {
                nextIndex -= this.mMaxSize;
            }
            if (nextIndex >= size()) {
                return null;
            }
            return (LogRec) this.mLogRecVector.get(nextIndex);
        }

        synchronized void add(StateMachine sm, Message msg, String messageInfo, IState state, IState orgState, IState transToState) {
            this.mCount++;
            if (this.mLogRecVector.size() < this.mMaxSize) {
                this.mLogRecVector.add(new LogRec(sm, msg, messageInfo, state, orgState, transToState));
            } else {
                LogRec pmi = (LogRec) this.mLogRecVector.get(this.mOldestIndex);
                this.mOldestIndex++;
                if (this.mOldestIndex >= this.mMaxSize) {
                    this.mOldestIndex = 0;
                }
                pmi.update(sm, msg, messageInfo, state, orgState, transToState);
            }
        }
    }

    private static class SmHandler extends Handler {
        private static final Object mSmHandlerObj = new Object();
        private boolean mDbg;
        private ArrayList<Message> mDeferredMessages;
        private State mDestState;
        private HaltingState mHaltingState;
        private boolean mHasQuit;
        private State mInitialState;
        private boolean mIsConstructionCompleted;
        private LogRecords mLogRecords;
        private Message mMsg;
        private QuittingState mQuittingState;
        private StateMachine mSm;
        private HashMap<State, StateInfo> mStateInfo;
        private StateInfo[] mStateStack;
        private int mStateStackTopIndex;
        private StateInfo[] mTempStateStack;
        private int mTempStateStackCount;
        private boolean mTransitionInProgress;

        private class StateInfo {
            boolean active;
            StateInfo parentStateInfo;
            State state;

            private StateInfo() {
            }

            public String toString() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("state=");
                stringBuilder.append(this.state.getName());
                stringBuilder.append(",active=");
                stringBuilder.append(this.active);
                stringBuilder.append(",parent=");
                stringBuilder.append(this.parentStateInfo == null ? "null" : this.parentStateInfo.state.getName());
                return stringBuilder.toString();
            }
        }

        private class HaltingState extends State {
            private HaltingState() {
            }

            public boolean processMessage(Message msg) {
                SmHandler.this.mSm.haltedProcessMessage(msg);
                return true;
            }
        }

        private class QuittingState extends State {
            private QuittingState() {
            }

            public boolean processMessage(Message msg) {
                return false;
            }
        }

        public final void handleMessage(Message msg) {
            if (!this.mHasQuit) {
                if (!(this.mSm == null || msg.what == -2 || msg.what == -1)) {
                    this.mSm.onPreHandleMessage(msg);
                }
                if (this.mDbg) {
                    StateMachine stateMachine = this.mSm;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleMessage: E msg.what=");
                    stringBuilder.append(msg.what);
                    stateMachine.log(stringBuilder.toString());
                }
                this.mMsg = msg;
                State msgProcessedState = null;
                if (this.mIsConstructionCompleted || this.mMsg.what == -1) {
                    msgProcessedState = processMsg(msg);
                } else if (!this.mIsConstructionCompleted && this.mMsg.what == -2 && this.mMsg.obj == mSmHandlerObj) {
                    this.mIsConstructionCompleted = true;
                    invokeEnterMethods(0);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("StateMachine.handleMessage: The start method not called, received msg: ");
                    stringBuilder2.append(msg);
                    throw new RuntimeException(stringBuilder2.toString());
                }
                performTransitions(msgProcessedState, msg);
                if (this.mDbg && this.mSm != null) {
                    this.mSm.log("handleMessage: X");
                }
                if (this.mSm != null && msg.what != -2 && msg.what != -1) {
                    this.mSm.onPostHandleMessage(msg);
                }
            }
        }

        private void performTransitions(State msgProcessedState, Message msg) {
            State orgState = this.mStateStack[this.mStateStackTopIndex].state;
            boolean z = this.mSm.recordLogRec(this.mMsg) && msg.obj != mSmHandlerObj;
            boolean recordLogMsg = z;
            if (this.mLogRecords.logOnlyTransitions()) {
                if (this.mDestState != null) {
                    this.mLogRecords.add(this.mSm, this.mMsg, this.mSm.getLogRecString(this.mMsg), msgProcessedState, orgState, this.mDestState);
                }
            } else if (recordLogMsg) {
                this.mLogRecords.add(this.mSm, this.mMsg, this.mSm.getLogRecString(this.mMsg), msgProcessedState, orgState, this.mDestState);
            }
            State destState = this.mDestState;
            if (destState != null) {
                while (true) {
                    if (this.mDbg) {
                        this.mSm.log("handleMessage: new destination call exit/enter");
                    }
                    StateInfo commonStateInfo = setupTempStateStackWithStatesToEnter(destState);
                    this.mTransitionInProgress = true;
                    invokeExitMethods(commonStateInfo);
                    invokeEnterMethods(moveTempStateStackToStateStack());
                    moveDeferredMessageAtFrontOfQueue();
                    if (destState == this.mDestState) {
                        break;
                    }
                    destState = this.mDestState;
                }
                this.mDestState = null;
            }
            if (destState == null) {
                return;
            }
            if (destState == this.mQuittingState) {
                this.mSm.onQuitting();
                cleanupAfterQuitting();
            } else if (destState == this.mHaltingState) {
                this.mSm.onHalting();
            }
        }

        private final void cleanupAfterQuitting() {
            if (this.mSm.mSmThread != null) {
                getLooper().quit();
                this.mSm.mSmThread = null;
            }
            this.mSm.mSmHandler = null;
            this.mSm = null;
            this.mMsg = null;
            this.mLogRecords.cleanup();
            this.mStateStack = null;
            this.mTempStateStack = null;
            this.mStateInfo.clear();
            this.mInitialState = null;
            this.mDestState = null;
            this.mDeferredMessages.clear();
            this.mHasQuit = true;
            this.mStateStackTopIndex = -1;
        }

        private final void completeConstruction() {
            if (this.mDbg) {
                this.mSm.log("completeConstruction: E");
            }
            int maxDepth = 0;
            for (StateInfo i : this.mStateInfo.values()) {
                int depth = 0;
                StateInfo i2;
                while (i2 != null) {
                    i2 = i2.parentStateInfo;
                    depth++;
                }
                if (maxDepth < depth) {
                    maxDepth = depth;
                }
            }
            if (this.mDbg) {
                StateMachine stateMachine = this.mSm;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("completeConstruction: maxDepth=");
                stringBuilder.append(maxDepth);
                stateMachine.log(stringBuilder.toString());
            }
            this.mStateStack = new StateInfo[maxDepth];
            this.mTempStateStack = new StateInfo[maxDepth];
            setupInitialStateStack();
            sendMessageAtFrontOfQueue(obtainMessage(-2, mSmHandlerObj));
            if (this.mDbg) {
                this.mSm.log("completeConstruction: X");
            }
        }

        private final State processMsg(Message msg) {
            StateMachine stateMachine;
            StringBuilder stringBuilder;
            StateInfo curStateInfo = this.mStateStack[this.mStateStackTopIndex];
            if (this.mDbg) {
                stateMachine = this.mSm;
                stringBuilder = new StringBuilder();
                stringBuilder.append("processMsg: ");
                stringBuilder.append(curStateInfo.state.getName());
                stateMachine.log(stringBuilder.toString());
            }
            if (isQuit(msg)) {
                transitionTo(this.mQuittingState);
            } else {
                while (!curStateInfo.state.processMessage(msg)) {
                    curStateInfo = curStateInfo.parentStateInfo;
                    if (curStateInfo == null) {
                        this.mSm.unhandledMessage(msg);
                        break;
                    } else if (this.mDbg) {
                        stateMachine = this.mSm;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("processMsg: ");
                        stringBuilder.append(curStateInfo.state.getName());
                        stateMachine.log(stringBuilder.toString());
                    }
                }
            }
            return curStateInfo != null ? curStateInfo.state : null;
        }

        private final void invokeExitMethods(StateInfo commonStateInfo) {
            while (this.mStateStackTopIndex >= 0 && this.mStateStack[this.mStateStackTopIndex] != commonStateInfo) {
                State curState = this.mStateStack[this.mStateStackTopIndex].state;
                if (this.mDbg) {
                    StateMachine stateMachine = this.mSm;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("invokeExitMethods: ");
                    stringBuilder.append(curState.getName());
                    stateMachine.log(stringBuilder.toString());
                }
                curState.exit();
                this.mStateStack[this.mStateStackTopIndex].active = false;
                this.mStateStackTopIndex--;
            }
        }

        private final void invokeEnterMethods(int stateStackEnteringIndex) {
            for (int i = stateStackEnteringIndex; i <= this.mStateStackTopIndex; i++) {
                if (stateStackEnteringIndex == this.mStateStackTopIndex) {
                    this.mTransitionInProgress = false;
                }
                if (this.mDbg) {
                    StateMachine stateMachine = this.mSm;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("invokeEnterMethods: ");
                    stringBuilder.append(this.mStateStack[i].state.getName());
                    stateMachine.log(stringBuilder.toString());
                }
                this.mStateStack[i].state.enter();
                this.mStateStack[i].active = true;
            }
            this.mTransitionInProgress = false;
        }

        private final void moveDeferredMessageAtFrontOfQueue() {
            for (int i = this.mDeferredMessages.size() - 1; i >= 0; i--) {
                Message curMsg = (Message) this.mDeferredMessages.get(i);
                if (this.mDbg) {
                    StateMachine stateMachine = this.mSm;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("moveDeferredMessageAtFrontOfQueue; what=");
                    stringBuilder.append(curMsg.what);
                    stateMachine.log(stringBuilder.toString());
                }
                sendMessageAtFrontOfQueue(curMsg);
            }
            this.mDeferredMessages.clear();
        }

        private final int moveTempStateStackToStateStack() {
            StateMachine stateMachine;
            StringBuilder stringBuilder;
            int startingIndex = this.mStateStackTopIndex + 1;
            int j = startingIndex;
            for (int i = this.mTempStateStackCount - 1; i >= 0; i--) {
                if (this.mDbg) {
                    stateMachine = this.mSm;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("moveTempStackToStateStack: i=");
                    stringBuilder.append(i);
                    stringBuilder.append(",j=");
                    stringBuilder.append(j);
                    stateMachine.log(stringBuilder.toString());
                }
                this.mStateStack[j] = this.mTempStateStack[i];
                j++;
            }
            this.mStateStackTopIndex = j - 1;
            if (this.mDbg) {
                stateMachine = this.mSm;
                stringBuilder = new StringBuilder();
                stringBuilder.append("moveTempStackToStateStack: X mStateStackTop=");
                stringBuilder.append(this.mStateStackTopIndex);
                stringBuilder.append(",startingIndex=");
                stringBuilder.append(startingIndex);
                stringBuilder.append(",Top=");
                stringBuilder.append(this.mStateStack[this.mStateStackTopIndex].state.getName());
                stateMachine.log(stringBuilder.toString());
            }
            return startingIndex;
        }

        private final StateInfo setupTempStateStackWithStatesToEnter(State destState) {
            this.mTempStateStackCount = 0;
            StateInfo curStateInfo = (StateInfo) this.mStateInfo.get(destState);
            do {
                StateInfo[] stateInfoArr = this.mTempStateStack;
                int i = this.mTempStateStackCount;
                this.mTempStateStackCount = i + 1;
                stateInfoArr[i] = curStateInfo;
                curStateInfo = curStateInfo.parentStateInfo;
                if (curStateInfo == null) {
                    break;
                }
            } while (!curStateInfo.active);
            if (this.mDbg) {
                StateMachine stateMachine = this.mSm;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setupTempStateStackWithStatesToEnter: X mTempStateStackCount=");
                stringBuilder.append(this.mTempStateStackCount);
                stringBuilder.append(",curStateInfo: ");
                stringBuilder.append(curStateInfo);
                stateMachine.log(stringBuilder.toString());
            }
            return curStateInfo;
        }

        private final void setupInitialStateStack() {
            if (this.mDbg) {
                StateMachine stateMachine = this.mSm;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setupInitialStateStack: E mInitialState=");
                stringBuilder.append(this.mInitialState.getName());
                stateMachine.log(stringBuilder.toString());
            }
            StateInfo curStateInfo = (StateInfo) this.mStateInfo.get(this.mInitialState);
            int i = 0;
            while (true) {
                this.mTempStateStackCount = i;
                if (curStateInfo != null) {
                    this.mTempStateStack[this.mTempStateStackCount] = curStateInfo;
                    curStateInfo = curStateInfo.parentStateInfo;
                    i = this.mTempStateStackCount + 1;
                } else {
                    this.mStateStackTopIndex = -1;
                    moveTempStateStackToStateStack();
                    return;
                }
            }
        }

        private final Message getCurrentMessage() {
            return this.mMsg;
        }

        private final IState getCurrentState() {
            if (this.mStateStack != null && this.mStateStackTopIndex >= 0) {
                return this.mStateStack[this.mStateStackTopIndex].state;
            }
            if (StateMachine.HWFLOW) {
                this.mSm.log("getCurrentState return null");
            }
            return null;
        }

        private final StateInfo addState(State state, State parent) {
            if (this.mDbg) {
                StateMachine stateMachine = this.mSm;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addStateInternal: E state=");
                stringBuilder.append(state.getName());
                stringBuilder.append(",parent=");
                stringBuilder.append(parent == null ? "" : parent.getName());
                stateMachine.log(stringBuilder.toString());
            }
            StateInfo parentStateInfo = null;
            if (parent != null) {
                parentStateInfo = (StateInfo) this.mStateInfo.get(parent);
                if (parentStateInfo == null) {
                    parentStateInfo = addState(parent, null);
                }
            }
            StateInfo stateInfo = (StateInfo) this.mStateInfo.get(state);
            if (stateInfo == null) {
                stateInfo = new StateInfo();
                this.mStateInfo.put(state, stateInfo);
            }
            if (stateInfo.parentStateInfo == null || stateInfo.parentStateInfo == parentStateInfo) {
                stateInfo.state = state;
                stateInfo.parentStateInfo = parentStateInfo;
                stateInfo.active = false;
                if (this.mDbg) {
                    StateMachine stateMachine2 = this.mSm;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("addStateInternal: X stateInfo: ");
                    stringBuilder2.append(stateInfo);
                    stateMachine2.log(stringBuilder2.toString());
                }
                return stateInfo;
            }
            throw new RuntimeException("state already added");
        }

        /* JADX WARNING: Missing block: B:9:0x0033, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void removeState(State state) {
            StateInfo stateInfo = (StateInfo) this.mStateInfo.get(state);
            if (stateInfo != null && !stateInfo.active && !this.mStateInfo.values().stream().filter(new -$$Lambda$StateMachine$SmHandler$KkPO7NIVuI9r_FPEGrY6ux6a5Ks(stateInfo)).findAny().isPresent()) {
                this.mStateInfo.remove(state);
            }
        }

        static /* synthetic */ boolean lambda$removeState$0(StateInfo stateInfo, StateInfo si) {
            return si.parentStateInfo == stateInfo;
        }

        private SmHandler(Looper looper, StateMachine sm) {
            super(looper);
            this.mHasQuit = false;
            this.mDbg = false;
            this.mLogRecords = new LogRecords();
            this.mStateStackTopIndex = -1;
            this.mHaltingState = new HaltingState();
            this.mQuittingState = new QuittingState();
            this.mStateInfo = new HashMap();
            this.mTransitionInProgress = false;
            this.mDeferredMessages = new ArrayList();
            this.mSm = sm;
            addState(this.mHaltingState, null);
            addState(this.mQuittingState, null);
        }

        private final void setInitialState(State initialState) {
            if (this.mDbg) {
                StateMachine stateMachine = this.mSm;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setInitialState: initialState=");
                stringBuilder.append(initialState.getName());
                stateMachine.log(stringBuilder.toString());
            }
            this.mInitialState = initialState;
        }

        private final void transitionTo(IState destState) {
            StringBuilder stringBuilder;
            if (this.mTransitionInProgress) {
                String access$800 = this.mSm.mName;
                stringBuilder = new StringBuilder();
                stringBuilder.append("transitionTo called while transition already in progress to ");
                stringBuilder.append(this.mDestState);
                stringBuilder.append(", new target state=");
                stringBuilder.append(destState);
                Log.wtf(access$800, stringBuilder.toString());
            }
            this.mDestState = (State) destState;
            if (this.mDbg) {
                StateMachine stateMachine = this.mSm;
                stringBuilder = new StringBuilder();
                stringBuilder.append("transitionTo: destState=");
                stringBuilder.append(this.mDestState.getName());
                stateMachine.log(stringBuilder.toString());
            }
        }

        private final void deferMessage(Message msg) {
            if (this.mDbg) {
                StateMachine stateMachine = this.mSm;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("deferMessage: msg=");
                stringBuilder.append(msg.what);
                stateMachine.log(stringBuilder.toString());
            }
            Message newMsg = obtainMessage();
            newMsg.copyFrom(msg);
            this.mDeferredMessages.add(newMsg);
        }

        private final void quit() {
            if (this.mDbg) {
                this.mSm.log("quit:");
            }
            sendMessage(obtainMessage(-1, mSmHandlerObj));
        }

        private final void quitNow() {
            if (this.mDbg) {
                this.mSm.log("quitNow:");
            }
            sendMessageAtFrontOfQueue(obtainMessage(-1, mSmHandlerObj));
        }

        private final boolean isQuit(Message msg) {
            return msg.what == -1 && msg.obj == mSmHandlerObj;
        }

        private final boolean isDbg() {
            return this.mDbg;
        }

        private final void setDbg(boolean dbg) {
            this.mDbg = dbg;
        }

        public String toString() {
            StateMachine sm = this.mSm;
            if (sm == null) {
                return super.toString();
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(super.toString());
            stringBuilder.append("#");
            stringBuilder.append(sm.getName());
            return stringBuilder.toString();
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    private void initStateMachine(String name, Looper looper) {
        this.mName = name;
        this.mSmHandler = new SmHandler(looper, this);
    }

    protected StateMachine(String name) {
        this.mSmThread = new HandlerThread(name);
        this.mSmThread.start();
        initStateMachine(name, this.mSmThread.getLooper());
    }

    protected StateMachine(String name, Looper looper) {
        initStateMachine(name, looper);
    }

    protected StateMachine(String name, Handler handler) {
        initStateMachine(name, handler.getLooper());
    }

    protected void onPreHandleMessage(Message msg) {
    }

    protected void onPostHandleMessage(Message msg) {
    }

    public final void addState(State state, State parent) {
        this.mSmHandler.addState(state, parent);
    }

    public final void addState(State state) {
        this.mSmHandler.addState(state, null);
    }

    public final void removeState(State state) {
        this.mSmHandler.removeState(state);
    }

    public final void setInitialState(State initialState) {
        this.mSmHandler.setInitialState(initialState);
    }

    public final Message getCurrentMessage() {
        SmHandler smh = this.mSmHandler;
        if (smh == null) {
            return null;
        }
        return smh.getCurrentMessage();
    }

    public final IState getCurrentState() {
        SmHandler smh = this.mSmHandler;
        if (smh == null) {
            return null;
        }
        return smh.getCurrentState();
    }

    public final void transitionTo(IState destState) {
        this.mSmHandler.transitionTo(destState);
        transitionToCallback(destState);
    }

    public final void transitionToHaltingState() {
        this.mSmHandler.transitionTo(this.mSmHandler.mHaltingState);
    }

    public final void deferMessage(Message msg) {
        this.mSmHandler.deferMessage(msg);
    }

    protected void unhandledMessage(Message msg) {
        if (this.mSmHandler.mDbg) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" - unhandledMessage: msg.what=");
            stringBuilder.append(msg.what);
            loge(stringBuilder.toString());
        }
    }

    protected void haltedProcessMessage(Message msg) {
    }

    protected void onHalting() {
    }

    protected void onQuitting() {
    }

    public final String getName() {
        return this.mName;
    }

    public final void setLogRecSize(int maxSize) {
        this.mSmHandler.mLogRecords.setSize(maxSize);
    }

    public final void setLogOnlyTransitions(boolean enable) {
        this.mSmHandler.mLogRecords.setLogOnlyTransitions(enable);
    }

    public final int getLogRecSize() {
        SmHandler smh = this.mSmHandler;
        if (smh == null) {
            return 0;
        }
        return smh.mLogRecords.size();
    }

    @VisibleForTesting
    public final int getLogRecMaxSize() {
        SmHandler smh = this.mSmHandler;
        if (smh == null) {
            return 0;
        }
        return smh.mLogRecords.mMaxSize;
    }

    public final int getLogRecCount() {
        SmHandler smh = this.mSmHandler;
        if (smh == null) {
            return 0;
        }
        return smh.mLogRecords.count();
    }

    public final LogRec getLogRec(int index) {
        SmHandler smh = this.mSmHandler;
        if (smh == null) {
            return null;
        }
        return smh.mLogRecords.get(index);
    }

    public final Collection<LogRec> copyLogRecs() {
        Vector<LogRec> vlr = new Vector();
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            Iterator it = smh.mLogRecords.mLogRecVector.iterator();
            while (it.hasNext()) {
                vlr.add((LogRec) it.next());
            }
        }
        return vlr;
    }

    public void addLogRec(String string) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.mLogRecords.add(this, smh.getCurrentMessage(), string, smh.getCurrentState(), smh.mStateStack[smh.mStateStackTopIndex].state, smh.mDestState);
        }
    }

    protected boolean recordLogRec(Message msg) {
        return true;
    }

    protected String getLogRecString(Message msg) {
        return "";
    }

    protected String getWhatToString(int what) {
        return null;
    }

    public final Handler getHandler() {
        return this.mSmHandler;
    }

    public final Message obtainMessage() {
        return Message.obtain(this.mSmHandler);
    }

    public final Message obtainMessage(int what) {
        return Message.obtain(this.mSmHandler, what);
    }

    public final Message obtainMessage(int what, Object obj) {
        return Message.obtain(this.mSmHandler, what, obj);
    }

    public final Message obtainMessage(int what, int arg1) {
        return Message.obtain(this.mSmHandler, what, arg1, 0);
    }

    public final Message obtainMessage(int what, int arg1, int arg2) {
        return Message.obtain(this.mSmHandler, what, arg1, arg2);
    }

    public final Message obtainMessage(int what, int arg1, int arg2, Object obj) {
        return Message.obtain(this.mSmHandler, what, arg1, arg2, obj);
    }

    public void sendMessage(int what) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessage(obtainMessage(what));
        }
    }

    public void sendMessage(int what, Object obj) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessage(obtainMessage(what, obj));
        }
    }

    public void sendMessage(int what, int arg1) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessage(obtainMessage(what, arg1));
        }
    }

    public void sendMessage(int what, int arg1, int arg2) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessage(obtainMessage(what, arg1, arg2));
        }
    }

    public void sendMessage(int what, int arg1, int arg2, Object obj) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessage(obtainMessage(what, arg1, arg2, obj));
        }
    }

    public void sendMessage(Message msg) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessage(msg);
        }
    }

    public void sendMessageDelayed(int what, long delayMillis) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageDelayed(obtainMessage(what), delayMillis);
        }
    }

    public void sendMessageDelayed(int what, Object obj, long delayMillis) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageDelayed(obtainMessage(what, obj), delayMillis);
        }
    }

    public void sendMessageDelayed(int what, int arg1, long delayMillis) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageDelayed(obtainMessage(what, arg1), delayMillis);
        }
    }

    public void sendMessageDelayed(int what, int arg1, int arg2, long delayMillis) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageDelayed(obtainMessage(what, arg1, arg2), delayMillis);
        }
    }

    public void sendMessageDelayed(int what, int arg1, int arg2, Object obj, long delayMillis) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageDelayed(obtainMessage(what, arg1, arg2, obj), delayMillis);
        }
    }

    public void sendMessageDelayed(Message msg, long delayMillis) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageDelayed(msg, delayMillis);
        }
    }

    protected final void sendMessageAtFrontOfQueue(int what) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageAtFrontOfQueue(obtainMessage(what));
        }
    }

    protected final void sendMessageAtFrontOfQueue(int what, Object obj) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageAtFrontOfQueue(obtainMessage(what, obj));
        }
    }

    protected final void sendMessageAtFrontOfQueue(int what, int arg1) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageAtFrontOfQueue(obtainMessage(what, arg1));
        }
    }

    protected final void sendMessageAtFrontOfQueue(int what, int arg1, int arg2) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageAtFrontOfQueue(obtainMessage(what, arg1, arg2));
        }
    }

    protected final void sendMessageAtFrontOfQueue(int what, int arg1, int arg2, Object obj) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageAtFrontOfQueue(obtainMessage(what, arg1, arg2, obj));
        }
    }

    protected final void sendMessageAtFrontOfQueue(Message msg) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.sendMessageAtFrontOfQueue(msg);
        }
    }

    protected final void removeMessages(int what) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.removeMessages(what);
        }
    }

    protected final void removeDeferredMessages(int what) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            Iterator<Message> iterator = smh.mDeferredMessages.iterator();
            while (iterator.hasNext()) {
                if (((Message) iterator.next()).what == what) {
                    iterator.remove();
                }
            }
        }
    }

    protected final boolean hasDeferredMessages(int what) {
        SmHandler smh = this.mSmHandler;
        if (smh == null) {
            return false;
        }
        Iterator<Message> iterator = smh.mDeferredMessages.iterator();
        while (iterator.hasNext()) {
            if (((Message) iterator.next()).what == what) {
                return true;
            }
        }
        return false;
    }

    protected final boolean hasDeferredMessagesForArg1(int what, int arg1) {
        SmHandler smh = this.mSmHandler;
        if (smh == null) {
            return false;
        }
        Iterator<Message> iterator = smh.mDeferredMessages.iterator();
        while (iterator.hasNext()) {
            Message msg = (Message) iterator.next();
            if (msg.what == what && msg.arg1 == arg1) {
                return true;
            }
        }
        return false;
    }

    protected final boolean hasMessages(int what) {
        SmHandler smh = this.mSmHandler;
        if (smh == null) {
            return false;
        }
        return smh.hasMessages(what);
    }

    protected final boolean isQuit(Message msg) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            return smh.isQuit(msg);
        }
        return msg.what == -1;
    }

    public final void quit() {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.quit();
        }
    }

    public final void quitNow() {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.quitNow();
        }
    }

    public boolean isDbg() {
        SmHandler smh = this.mSmHandler;
        if (smh == null) {
            return false;
        }
        return smh.isDbg();
    }

    public void setDbg(boolean dbg) {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.setDbg(dbg);
        }
    }

    public void start() {
        SmHandler smh = this.mSmHandler;
        if (smh != null) {
            smh.completeConstruction();
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getName());
        stringBuilder.append(":");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" total records=");
        stringBuilder.append(getLogRecCount());
        pw.println(stringBuilder.toString());
        for (int i = 0; i < getLogRecSize(); i++) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" rec[");
            stringBuilder2.append(i);
            stringBuilder2.append("]: ");
            stringBuilder2.append(getLogRec(i).toString());
            pw.println(stringBuilder2.toString());
            pw.flush();
        }
        if (getCurrentState() == null) {
            pw.println("curState missed.");
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("curState=");
        stringBuilder.append(getCurrentState().getName());
        pw.println(stringBuilder.toString());
    }

    public String toString() {
        String name = "(null)";
        String state = "(null)";
        try {
            name = this.mName.toString();
            state = this.mSmHandler.getCurrentState().getName().toString();
        } catch (NullPointerException e) {
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("name=");
        stringBuilder.append(name);
        stringBuilder.append(" state=");
        stringBuilder.append(state);
        return stringBuilder.toString();
    }

    protected void logAndAddLogRec(String s) {
        addLogRec(s);
        log(s);
    }

    protected void log(String s) {
        Log.d(this.mName, s);
    }

    protected void logd(String s) {
        Log.d(this.mName, s);
    }

    protected void logv(String s) {
        Log.v(this.mName, s);
    }

    protected void logi(String s) {
        Log.i(this.mName, s);
    }

    protected void logw(String s) {
        Log.w(this.mName, s);
    }

    protected void loge(String s) {
        Log.e(this.mName, s);
    }

    protected void loge(String s, Throwable e) {
        Log.e(this.mName, s, e);
    }

    public void transitionToCallback(IState destState) {
    }
}
