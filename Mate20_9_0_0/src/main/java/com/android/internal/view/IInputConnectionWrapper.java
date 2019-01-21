package com.android.internal.view;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionInspector;
import android.view.inputmethod.InputContentInfo;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.SomeArgs;
import com.android.internal.view.IInputContext.Stub;

public abstract class IInputConnectionWrapper extends Stub {
    private static final boolean DEBUG = false;
    private static final int DO_BEGIN_BATCH_EDIT = 90;
    private static final int DO_CLEAR_META_KEY_STATES = 130;
    private static final int DO_CLOSE_CONNECTION = 150;
    private static final int DO_COMMIT_COMPLETION = 55;
    private static final int DO_COMMIT_CONTENT = 160;
    private static final int DO_COMMIT_CORRECTION = 56;
    private static final int DO_COMMIT_TEXT = 50;
    private static final int DO_DELETE_SURROUNDING_TEXT = 80;
    private static final int DO_DELETE_SURROUNDING_TEXT_IN_CODE_POINTS = 81;
    private static final int DO_END_BATCH_EDIT = 95;
    private static final int DO_FINISH_COMPOSING_TEXT = 65;
    private static final int DO_GET_CURSOR_CAPS_MODE = 30;
    private static final int DO_GET_EXTRACTED_TEXT = 40;
    private static final int DO_GET_SELECTED_TEXT = 25;
    private static final int DO_GET_TEXT_AFTER_CURSOR = 10;
    private static final int DO_GET_TEXT_BEFORE_CURSOR = 20;
    private static final int DO_PERFORM_CONTEXT_MENU_ACTION = 59;
    private static final int DO_PERFORM_EDITOR_ACTION = 58;
    private static final int DO_PERFORM_PRIVATE_COMMAND = 120;
    private static final int DO_REQUEST_UPDATE_CURSOR_ANCHOR_INFO = 140;
    private static final int DO_SEND_KEY_EVENT = 70;
    private static final int DO_SET_COMPOSING_REGION = 63;
    private static final int DO_SET_COMPOSING_TEXT = 60;
    private static final int DO_SET_SELECTION = 57;
    private static final String TAG = "IInputConnectionWrapper";
    @GuardedBy("mLock")
    private boolean mFinished = false;
    private Handler mH;
    @GuardedBy("mLock")
    private InputConnection mInputConnection;
    private Object mLock = new Object();
    private Looper mMainLooper;

    class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            IInputConnectionWrapper.this.executeMessage(msg);
        }
    }

    protected abstract boolean isActive();

    protected abstract void onUserAction();

    public IInputConnectionWrapper(Looper mainLooper, InputConnection inputConnection) {
        this.mInputConnection = inputConnection;
        this.mMainLooper = mainLooper;
        this.mH = new MyHandler(this.mMainLooper);
    }

    public InputConnection getInputConnection() {
        InputConnection inputConnection;
        synchronized (this.mLock) {
            inputConnection = this.mInputConnection;
        }
        return inputConnection;
    }

    protected boolean isFinished() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mFinished;
        }
        return z;
    }

    public void getTextAfterCursor(int length, int flags, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageIISC(10, length, flags, seq, callback));
    }

    public void getTextBeforeCursor(int length, int flags, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageIISC(20, length, flags, seq, callback));
    }

    public void getSelectedText(int flags, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageISC(25, flags, seq, callback));
    }

    public void getCursorCapsMode(int reqModes, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageISC(30, reqModes, seq, callback));
    }

    public void getExtractedText(ExtractedTextRequest request, int flags, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageIOSC(40, flags, request, seq, callback));
    }

    public void commitText(CharSequence text, int newCursorPosition) {
        dispatchMessage(obtainMessageIO(50, newCursorPosition, text));
    }

    public void commitCompletion(CompletionInfo text) {
        dispatchMessage(obtainMessageO(55, text));
    }

    public void commitCorrection(CorrectionInfo info) {
        dispatchMessage(obtainMessageO(56, info));
    }

    public void setSelection(int start, int end) {
        dispatchMessage(obtainMessageII(57, start, end));
    }

    public void performEditorAction(int id) {
        dispatchMessage(obtainMessageII(58, id, 0));
    }

    public void performContextMenuAction(int id) {
        dispatchMessage(obtainMessageII(59, id, 0));
    }

    public void setComposingRegion(int start, int end) {
        dispatchMessage(obtainMessageII(63, start, end));
    }

    public void setComposingText(CharSequence text, int newCursorPosition) {
        dispatchMessage(obtainMessageIO(60, newCursorPosition, text));
    }

    public void finishComposingText() {
        dispatchMessage(obtainMessage(65));
    }

    public void sendKeyEvent(KeyEvent event) {
        dispatchMessage(obtainMessageO(70, event));
    }

    public void clearMetaKeyStates(int states) {
        dispatchMessage(obtainMessageII(130, states, 0));
    }

    public void deleteSurroundingText(int beforeLength, int afterLength) {
        dispatchMessage(obtainMessageII(80, beforeLength, afterLength));
    }

    public void deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        dispatchMessage(obtainMessageII(81, beforeLength, afterLength));
    }

    public void beginBatchEdit() {
        dispatchMessage(obtainMessage(90));
    }

    public void endBatchEdit() {
        dispatchMessage(obtainMessage(95));
    }

    public void performPrivateCommand(String action, Bundle data) {
        dispatchMessage(obtainMessageOO(120, action, data));
    }

    public void requestUpdateCursorAnchorInfo(int cursorUpdateMode, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageISC(140, cursorUpdateMode, seq, callback));
    }

    public void closeConnection() {
        dispatchMessage(obtainMessage(150));
    }

    public void commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts, int seq, IInputContextCallback callback) {
        dispatchMessage(obtainMessageIOOSC(160, flags, inputContentInfo, opts, seq, callback));
    }

    void dispatchMessage(Message msg) {
        if (Looper.myLooper() == this.mMainLooper) {
            executeMessage(msg);
            msg.recycle();
            return;
        }
        this.mH.sendMessage(msg);
    }

    void executeMessage(Message msg) {
        int i = msg.what;
        InputConnection ic;
        switch (i) {
            case 55:
                ic = getInputConnection();
                if (ic == null || !isActive()) {
                    Log.w(TAG, "commitCompletion on inactive InputConnection");
                    return;
                } else {
                    ic.commitCompletion((CompletionInfo) msg.obj);
                    return;
                }
            case 56:
                ic = getInputConnection();
                if (ic == null || !isActive()) {
                    Log.w(TAG, "commitCorrection on inactive InputConnection");
                    return;
                } else {
                    ic.commitCorrection((CorrectionInfo) msg.obj);
                    return;
                }
            case 57:
                ic = getInputConnection();
                if (ic == null || !isActive()) {
                    Log.w(TAG, "setSelection on inactive InputConnection");
                    return;
                } else {
                    ic.setSelection(msg.arg1, msg.arg2);
                    return;
                }
            case 58:
                ic = getInputConnection();
                if (ic == null || !isActive()) {
                    Log.w(TAG, "performEditorAction on inactive InputConnection");
                    return;
                } else {
                    ic.performEditorAction(msg.arg1);
                    return;
                }
            case 59:
                ic = getInputConnection();
                if (ic == null || !isActive()) {
                    Log.w(TAG, "performContextMenuAction on inactive InputConnection");
                    return;
                } else {
                    ic.performContextMenuAction(msg.arg1);
                    return;
                }
            case 60:
                ic = getInputConnection();
                if (ic == null || !isActive()) {
                    Log.w(TAG, "setComposingText on inactive InputConnection");
                    return;
                }
                ic.setComposingText((CharSequence) msg.obj, msg.arg1);
                onUserAction();
                return;
            default:
                switch (i) {
                    case 80:
                        ic = getInputConnection();
                        if (ic == null || !isActive()) {
                            Log.w(TAG, "deleteSurroundingText on inactive InputConnection");
                            return;
                        } else {
                            ic.deleteSurroundingText(msg.arg1, msg.arg2);
                            return;
                        }
                    case 81:
                        ic = getInputConnection();
                        if (ic == null || !isActive()) {
                            Log.w(TAG, "deleteSurroundingTextInCodePoints on inactive InputConnection");
                            return;
                        } else {
                            ic.deleteSurroundingTextInCodePoints(msg.arg1, msg.arg2);
                            return;
                        }
                    default:
                        SomeArgs args;
                        IInputContextCallback callback;
                        int callbackSeq;
                        InputConnection ic2;
                        IInputContextCallback callback2;
                        String str;
                        switch (i) {
                            case 10:
                                args = (SomeArgs) msg.obj;
                                try {
                                    callback = args.arg6;
                                    callbackSeq = args.argi6;
                                    ic2 = getInputConnection();
                                    if (ic2 != null) {
                                        if (isActive()) {
                                            callback.setTextAfterCursor(ic2.getTextAfterCursor(msg.arg1, msg.arg2), callbackSeq);
                                            args.recycle();
                                            return;
                                        }
                                    }
                                    Log.w(TAG, "getTextAfterCursor on inactive InputConnection");
                                    callback.setTextAfterCursor(null, callbackSeq);
                                    args.recycle();
                                    return;
                                } catch (RemoteException e) {
                                    Log.w(TAG, "Got RemoteException calling setTextAfterCursor", e);
                                } catch (Throwable th) {
                                    args.recycle();
                                }
                            case 20:
                                args = (SomeArgs) msg.obj;
                                try {
                                    callback = args.arg6;
                                    callbackSeq = args.argi6;
                                    ic2 = getInputConnection();
                                    if (ic2 != null) {
                                        if (isActive()) {
                                            callback.setTextBeforeCursor(ic2.getTextBeforeCursor(msg.arg1, msg.arg2), callbackSeq);
                                            args.recycle();
                                            return;
                                        }
                                    }
                                    Log.w(TAG, "getTextBeforeCursor on inactive InputConnection");
                                    callback.setTextBeforeCursor(null, callbackSeq);
                                    args.recycle();
                                    return;
                                } catch (RemoteException e2) {
                                    Log.w(TAG, "Got RemoteException calling setTextBeforeCursor", e2);
                                } catch (Throwable th2) {
                                    args.recycle();
                                }
                            case 25:
                                args = (SomeArgs) msg.obj;
                                try {
                                    callback = args.arg6;
                                    callbackSeq = args.argi6;
                                    ic2 = getInputConnection();
                                    if (ic2 != null) {
                                        if (isActive()) {
                                            callback.setSelectedText(ic2.getSelectedText(msg.arg1), callbackSeq);
                                            args.recycle();
                                            return;
                                        }
                                    }
                                    Log.w(TAG, "getSelectedText on inactive InputConnection");
                                    callback.setSelectedText(null, callbackSeq);
                                    args.recycle();
                                    return;
                                } catch (RemoteException e22) {
                                    Log.w(TAG, "Got RemoteException calling setSelectedText", e22);
                                } catch (Throwable th3) {
                                    args.recycle();
                                }
                            case 30:
                                args = (SomeArgs) msg.obj;
                                try {
                                    callback2 = args.arg6;
                                    callbackSeq = args.argi6;
                                    ic2 = getInputConnection();
                                    if (ic2 != null) {
                                        if (isActive()) {
                                            callback2.setCursorCapsMode(ic2.getCursorCapsMode(msg.arg1), callbackSeq);
                                            args.recycle();
                                            return;
                                        }
                                    }
                                    Log.w(TAG, "getCursorCapsMode on inactive InputConnection");
                                    callback2.setCursorCapsMode(0, callbackSeq);
                                    args.recycle();
                                    return;
                                } catch (RemoteException e222) {
                                    Log.w(TAG, "Got RemoteException calling setCursorCapsMode", e222);
                                } catch (Throwable th4) {
                                    args.recycle();
                                }
                            case 40:
                                args = msg.obj;
                                try {
                                    callback = args.arg6;
                                    callbackSeq = args.argi6;
                                    ic2 = getInputConnection();
                                    if (ic2 != null) {
                                        if (isActive()) {
                                            callback.setExtractedText(ic2.getExtractedText((ExtractedTextRequest) args.arg1, msg.arg1), callbackSeq);
                                            args.recycle();
                                            return;
                                        }
                                    }
                                    Log.w(TAG, "getExtractedText on inactive InputConnection");
                                    callback.setExtractedText(null, callbackSeq);
                                    args.recycle();
                                    return;
                                } catch (RemoteException e2222) {
                                    Log.w(TAG, "Got RemoteException calling setExtractedText", e2222);
                                } catch (Throwable th5) {
                                    args.recycle();
                                }
                            case 50:
                                ic = getInputConnection();
                                if (ic == null || !isActive()) {
                                    str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("ic");
                                    stringBuilder.append(ic);
                                    stringBuilder.append(", isActive()");
                                    stringBuilder.append(isActive());
                                    Log.w(str, stringBuilder.toString());
                                    Log.w(TAG, "commitText on inactive InputConnection");
                                    return;
                                }
                                ic.commitText((CharSequence) msg.obj, msg.arg1);
                                onUserAction();
                                return;
                            case 63:
                                ic = getInputConnection();
                                if (ic == null || !isActive()) {
                                    Log.w(TAG, "setComposingRegion on inactive InputConnection");
                                    return;
                                } else {
                                    ic.setComposingRegion(msg.arg1, msg.arg2);
                                    return;
                                }
                            case 65:
                                if (!isFinished()) {
                                    ic = getInputConnection();
                                    if (ic == null) {
                                        Log.w(TAG, "finishComposingText on inactive InputConnection");
                                        return;
                                    } else {
                                        ic.finishComposingText();
                                        return;
                                    }
                                }
                                return;
                            case 70:
                                ic = getInputConnection();
                                if (ic == null || !isActive()) {
                                    Log.w(TAG, "sendKeyEvent on inactive InputConnection");
                                    return;
                                }
                                ic.sendKeyEvent((KeyEvent) msg.obj);
                                onUserAction();
                                return;
                            case 90:
                                ic = getInputConnection();
                                if (ic == null || !isActive()) {
                                    Log.w(TAG, "beginBatchEdit on inactive InputConnection");
                                    return;
                                } else {
                                    ic.beginBatchEdit();
                                    return;
                                }
                            case 95:
                                ic = getInputConnection();
                                if (ic == null || !isActive()) {
                                    Log.w(TAG, "endBatchEdit on inactive InputConnection");
                                    return;
                                } else {
                                    ic.endBatchEdit();
                                    return;
                                }
                            case 120:
                                args = msg.obj;
                                try {
                                    str = args.arg1;
                                    Bundle data = args.arg2;
                                    InputConnection ic3 = getInputConnection();
                                    if (ic3 != null) {
                                        if (isActive()) {
                                            ic3.performPrivateCommand(str, data);
                                            args.recycle();
                                            return;
                                        }
                                    }
                                    Log.w(TAG, "performPrivateCommand on inactive InputConnection");
                                    return;
                                } finally {
                                    args.recycle();
                                }
                            case 130:
                                ic = getInputConnection();
                                if (ic == null || !isActive()) {
                                    Log.w(TAG, "clearMetaKeyStates on inactive InputConnection");
                                    return;
                                } else {
                                    ic.clearMetaKeyStates(msg.arg1);
                                    return;
                                }
                            case 140:
                                args = msg.obj;
                                try {
                                    callback2 = args.arg6;
                                    callbackSeq = args.argi6;
                                    ic2 = getInputConnection();
                                    if (ic2 != null) {
                                        if (isActive()) {
                                            callback2.setRequestUpdateCursorAnchorInfoResult(ic2.requestCursorUpdates(msg.arg1), callbackSeq);
                                            args.recycle();
                                            return;
                                        }
                                    }
                                    Log.w(TAG, "requestCursorAnchorInfo on inactive InputConnection");
                                    callback2.setRequestUpdateCursorAnchorInfoResult(false, callbackSeq);
                                    args.recycle();
                                    return;
                                } catch (RemoteException e22222) {
                                    Log.w(TAG, "Got RemoteException calling requestCursorAnchorInfo", e22222);
                                } catch (Throwable th6) {
                                    args.recycle();
                                }
                            case 150:
                                if (!isFinished()) {
                                    try {
                                        InputConnection ic4 = getInputConnection();
                                        if (ic4 == null) {
                                            synchronized (this.mLock) {
                                                this.mInputConnection = null;
                                                this.mFinished = true;
                                            }
                                            return;
                                        }
                                        if ((InputConnectionInspector.getMissingMethodFlags(ic4) & 64) == 0) {
                                            ic4.closeConnection();
                                        }
                                        synchronized (this.mLock) {
                                            this.mInputConnection = null;
                                            this.mFinished = true;
                                        }
                                        return;
                                    } catch (Throwable th7) {
                                        synchronized (this.mLock) {
                                            this.mInputConnection = null;
                                            this.mFinished = true;
                                        }
                                    }
                                }
                                return;
                            case 160:
                                i = msg.arg1;
                                SomeArgs args2 = msg.obj;
                                try {
                                    IInputContextCallback callback3 = args2.arg6;
                                    int callbackSeq2 = args2.argi6;
                                    InputConnection ic5 = getInputConnection();
                                    if (ic5 != null) {
                                        if (isActive()) {
                                            InputContentInfo inputContentInfo = args2.arg1;
                                            if (inputContentInfo != null) {
                                                if (inputContentInfo.validate()) {
                                                    callback3.setCommitContentResult(ic5.commitContent(inputContentInfo, i, (Bundle) args2.arg2), callbackSeq2);
                                                    args2.recycle();
                                                    return;
                                                }
                                            }
                                            String str2 = TAG;
                                            StringBuilder stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("commitContent with invalid inputContentInfo=");
                                            stringBuilder2.append(inputContentInfo);
                                            Log.w(str2, stringBuilder2.toString());
                                            callback3.setCommitContentResult(false, callbackSeq2);
                                            args2.recycle();
                                            return;
                                        }
                                    }
                                    Log.w(TAG, "commitContent on inactive InputConnection");
                                    callback3.setCommitContentResult(false, callbackSeq2);
                                    args2.recycle();
                                    return;
                                } catch (RemoteException e222222) {
                                    Log.w(TAG, "Got RemoteException calling commitContent", e222222);
                                } catch (Throwable th8) {
                                    args2.recycle();
                                }
                            default:
                                String str3 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Unhandled message code: ");
                                stringBuilder3.append(msg.what);
                                Log.w(str3, stringBuilder3.toString());
                                return;
                        }
                        break;
                }
        }
    }

    Message obtainMessage(int what) {
        return this.mH.obtainMessage(what);
    }

    Message obtainMessageII(int what, int arg1, int arg2) {
        return this.mH.obtainMessage(what, arg1, arg2);
    }

    Message obtainMessageO(int what, Object arg1) {
        return this.mH.obtainMessage(what, 0, 0, arg1);
    }

    Message obtainMessageISC(int what, int arg1, int callbackSeq, IInputContextCallback callback) {
        SomeArgs args = SomeArgs.obtain();
        args.arg6 = callback;
        args.argi6 = callbackSeq;
        return this.mH.obtainMessage(what, arg1, 0, args);
    }

    Message obtainMessageIISC(int what, int arg1, int arg2, int callbackSeq, IInputContextCallback callback) {
        SomeArgs args = SomeArgs.obtain();
        args.arg6 = callback;
        args.argi6 = callbackSeq;
        return this.mH.obtainMessage(what, arg1, arg2, args);
    }

    Message obtainMessageIOOSC(int what, int arg1, Object objArg1, Object objArg2, int callbackSeq, IInputContextCallback callback) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = objArg1;
        args.arg2 = objArg2;
        args.arg6 = callback;
        args.argi6 = callbackSeq;
        return this.mH.obtainMessage(what, arg1, 0, args);
    }

    Message obtainMessageIOSC(int what, int arg1, Object arg2, int callbackSeq, IInputContextCallback callback) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = arg2;
        args.arg6 = callback;
        args.argi6 = callbackSeq;
        return this.mH.obtainMessage(what, arg1, 0, args);
    }

    Message obtainMessageIO(int what, int arg1, Object arg2) {
        return this.mH.obtainMessage(what, arg1, 0, arg2);
    }

    Message obtainMessageOO(int what, Object arg1, Object arg2) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = arg1;
        args.arg2 = arg2;
        return this.mH.obtainMessage(what, 0, 0, args);
    }
}
