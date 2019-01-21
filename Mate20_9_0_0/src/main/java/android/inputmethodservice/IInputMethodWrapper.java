package android.inputmethodservice;

import android.Manifest.permission;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.InputChannel;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethod.SessionCallback;
import android.view.inputmethod.InputMethodSession;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.HandlerCaller.Callback;
import com.android.internal.os.SomeArgs;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethod.Stub;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.IInputSessionCallback;
import com.android.internal.view.InputConnectionWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class IInputMethodWrapper extends Stub implements Callback {
    private static final int DO_ATTACH_TOKEN = 10;
    private static final int DO_CHANGE_INPUTMETHOD_SUBTYPE = 80;
    private static final int DO_CREATE_SESSION = 40;
    private static final int DO_DUMP = 1;
    private static final int DO_HIDE_SOFT_INPUT = 70;
    private static final int DO_REVOKE_SESSION = 50;
    private static final int DO_SET_INPUT_CONTEXT = 20;
    private static final int DO_SET_SESSION_ENABLED = 45;
    private static final int DO_SHOW_SOFT_INPUT = 60;
    private static final int DO_START_INPUT = 32;
    private static final int DO_UNSET_INPUT_CONTEXT = 30;
    private static final String TAG = "InputMethodWrapper";
    final HandlerCaller mCaller;
    final Context mContext;
    final WeakReference<InputMethod> mInputMethod;
    AtomicBoolean mIsUnbindIssued = null;
    final WeakReference<AbstractInputMethodService> mTarget;
    final int mTargetSdkVersion;

    static final class InputMethodSessionCallbackWrapper implements SessionCallback {
        final IInputSessionCallback mCb;
        final InputChannel mChannel;
        final Context mContext;

        InputMethodSessionCallbackWrapper(Context context, InputChannel channel, IInputSessionCallback cb) {
            this.mContext = context;
            this.mChannel = channel;
            this.mCb = cb;
        }

        public void sessionCreated(InputMethodSession session) {
            if (session != null) {
                try {
                    this.mCb.sessionCreated(new IInputMethodSessionWrapper(this.mContext, session, this.mChannel));
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            if (this.mChannel != null) {
                this.mChannel.dispose();
            }
            this.mCb.sessionCreated(null);
        }
    }

    public IInputMethodWrapper(AbstractInputMethodService context, InputMethod inputMethod) {
        this.mTarget = new WeakReference(context);
        this.mContext = context.getApplicationContext();
        this.mCaller = new HandlerCaller(this.mContext, null, this, true);
        this.mInputMethod = new WeakReference(inputMethod);
        this.mTargetSdkVersion = context.getApplicationInfo().targetSdkVersion;
    }

    public void executeMessage(Message msg) {
        InputMethod inputMethod = (InputMethod) this.mInputMethod.get();
        boolean restarting = true;
        String str;
        StringBuilder stringBuilder;
        if (inputMethod != null || msg.what == 1) {
            SomeArgs args;
            switch (msg.what) {
                case 1:
                    AbstractInputMethodService target = (AbstractInputMethodService) this.mTarget.get();
                    if (target != null) {
                        args = (SomeArgs) msg.obj;
                        try {
                            target.dump((FileDescriptor) args.arg1, (PrintWriter) args.arg2, (String[]) args.arg3);
                        } catch (RuntimeException e) {
                            PrintWriter printWriter = (PrintWriter) args.arg2;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Exception: ");
                            stringBuilder2.append(e);
                            printWriter.println(stringBuilder2.toString());
                        }
                        synchronized (args.arg4) {
                            ((CountDownLatch) args.arg4).countDown();
                        }
                        args.recycle();
                        return;
                    }
                    return;
                case 10:
                    inputMethod.attachToken((IBinder) msg.obj);
                    return;
                case 20:
                    inputMethod.bindInput((InputBinding) msg.obj);
                    return;
                case 30:
                    inputMethod.unbindInput();
                    return;
                case 32:
                    InputConnection ic;
                    args = msg.obj;
                    int missingMethods = msg.arg1;
                    if (msg.arg2 == 0) {
                        restarting = false;
                    }
                    IBinder startInputToken = args.arg1;
                    IInputContext inputContext = args.arg2;
                    EditorInfo info = args.arg3;
                    AtomicBoolean isUnbindIssued = args.arg4;
                    if (inputContext != null) {
                        ic = new InputConnectionWrapper(this.mTarget, inputContext, missingMethods, isUnbindIssued);
                    } else {
                        ic = null;
                    }
                    info.makeCompatible(this.mTargetSdkVersion);
                    inputMethod.dispatchStartInputWithToken(ic, info, restarting, startInputToken);
                    args.recycle();
                    return;
                case 40:
                    SomeArgs args2 = msg.obj;
                    inputMethod.createSession(new InputMethodSessionCallbackWrapper(this.mContext, (InputChannel) args2.arg1, (IInputSessionCallback) args2.arg2));
                    args2.recycle();
                    return;
                case 45:
                    InputMethodSession inputMethodSession = (InputMethodSession) msg.obj;
                    if (msg.arg1 == 0) {
                        restarting = false;
                    }
                    inputMethod.setSessionEnabled(inputMethodSession, restarting);
                    return;
                case 50:
                    inputMethod.revokeSession((InputMethodSession) msg.obj);
                    return;
                case 60:
                    inputMethod.showSoftInput(msg.arg1, (ResultReceiver) msg.obj);
                    return;
                case 70:
                    inputMethod.hideSoftInput(msg.arg1, (ResultReceiver) msg.obj);
                    return;
                case 80:
                    inputMethod.changeInputMethodSubtype((InputMethodSubtype) msg.obj);
                    return;
                default:
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unhandled message code: ");
                    stringBuilder.append(msg.what);
                    Log.w(str, stringBuilder.toString());
                    return;
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Input method reference was null, ignoring message: ");
        stringBuilder.append(msg.what);
        Log.w(str, stringBuilder.toString());
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        AbstractInputMethodService target = (AbstractInputMethodService) this.mTarget.get();
        if (target != null) {
            if (target.checkCallingOrSelfPermission(permission.DUMP) != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Permission Denial: can't dump InputMethodManager from from pid=");
                stringBuilder.append(Binder.getCallingPid());
                stringBuilder.append(", uid=");
                stringBuilder.append(Binder.getCallingUid());
                fout.println(stringBuilder.toString());
                return;
            }
            CountDownLatch latch = new CountDownLatch(1);
            this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOOOO(1, fd, fout, args, latch));
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    fout.println("Timeout waiting for dump");
                }
            } catch (InterruptedException e) {
                fout.println("Interrupted waiting for dump");
            }
        }
    }

    public void attachToken(IBinder token) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(10, token));
    }

    public void bindInput(InputBinding binding) {
        if (this.mIsUnbindIssued != null) {
            Log.e(TAG, "bindInput must be paired with unbindInput.");
        }
        this.mIsUnbindIssued = new AtomicBoolean();
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(20, new InputBinding(new InputConnectionWrapper(this.mTarget, IInputContext.Stub.asInterface(binding.getConnectionToken()), 0, this.mIsUnbindIssued), binding)));
    }

    public void unbindInput() {
        if (this.mIsUnbindIssued != null) {
            this.mIsUnbindIssued.set(true);
            this.mIsUnbindIssued = null;
        } else {
            Log.e(TAG, "unbindInput must be paired with bindInput.");
        }
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessage(30));
    }

    public void startInput(IBinder startInputToken, IInputContext inputContext, int missingMethods, EditorInfo attribute, boolean restarting) {
        if (this.mIsUnbindIssued == null) {
            Log.e(TAG, "startInput must be called after bindInput.");
            this.mIsUnbindIssued = new AtomicBoolean();
        }
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageIIOOOO(32, missingMethods, restarting, startInputToken, inputContext, attribute, this.mIsUnbindIssued));
    }

    public void createSession(InputChannel channel, IInputSessionCallback callback) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOO(40, channel, callback));
    }

    public void setSessionEnabled(IInputMethodSession session, boolean enabled) {
        String str;
        StringBuilder stringBuilder;
        try {
            InputMethodSession ls = ((IInputMethodSessionWrapper) session).getInternalInputMethodSession();
            if (ls == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Session is already finished: ");
                stringBuilder.append(session);
                Log.w(str, stringBuilder.toString());
                return;
            }
            this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageIO(45, enabled, ls));
        } catch (ClassCastException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Incoming session not of correct type: ");
            stringBuilder.append(session);
            Log.w(str, stringBuilder.toString(), e);
        }
    }

    public void revokeSession(IInputMethodSession session) {
        String str;
        StringBuilder stringBuilder;
        try {
            InputMethodSession ls = ((IInputMethodSessionWrapper) session).getInternalInputMethodSession();
            if (ls == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Session is already finished: ");
                stringBuilder.append(session);
                Log.w(str, stringBuilder.toString());
                return;
            }
            this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(50, ls));
        } catch (ClassCastException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Incoming session not of correct type: ");
            stringBuilder.append(session);
            Log.w(str, stringBuilder.toString(), e);
        }
    }

    public void showSoftInput(int flags, ResultReceiver resultReceiver) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageIO(60, flags, resultReceiver));
    }

    public void hideSoftInput(int flags, ResultReceiver resultReceiver) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageIO(70, flags, resultReceiver));
    }

    public void changeInputMethodSubtype(InputMethodSubtype subtype) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(80, subtype));
    }
}
