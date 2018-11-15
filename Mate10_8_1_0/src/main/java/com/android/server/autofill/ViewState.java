package com.android.server.autofill;

import android.graphics.Rect;
import android.service.autofill.FillResponse;
import android.util.DebugUtils;
import android.util.Slog;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import java.io.PrintWriter;

final class ViewState {
    public static final int STATE_AUTOFILLED = 4;
    public static final int STATE_CHANGED = 8;
    public static final int STATE_FILLABLE = 2;
    public static final int STATE_IGNORED = 128;
    public static final int STATE_INITIAL = 1;
    public static final int STATE_RESTARTED_SESSION = 256;
    public static final int STATE_STARTED_PARTITION = 32;
    public static final int STATE_STARTED_SESSION = 16;
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_WAITING_DATASET_AUTH = 64;
    private static final String TAG = "ViewState";
    public final AutofillId id;
    private AutofillValue mAutofilledValue;
    private AutofillValue mCurrentValue;
    private final Listener mListener;
    private FillResponse mResponse;
    private final Session mSession;
    private int mState;
    private Rect mVirtualBounds;

    interface Listener {
        void onFillReady(FillResponse fillResponse, AutofillId autofillId, AutofillValue autofillValue);
    }

    void resetState(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.server.autofill.ViewState.resetState(int):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.autofill.ViewState.resetState(int):void");
    }

    ViewState(Session session, AutofillId id, Listener listener, int state) {
        this.mSession = session;
        this.id = id;
        this.mListener = listener;
        this.mState = state;
    }

    Rect getVirtualBounds() {
        return this.mVirtualBounds;
    }

    AutofillValue getCurrentValue() {
        return this.mCurrentValue;
    }

    void setCurrentValue(AutofillValue value) {
        this.mCurrentValue = value;
    }

    AutofillValue getAutofilledValue() {
        return this.mAutofilledValue;
    }

    void setAutofilledValue(AutofillValue value) {
        this.mAutofilledValue = value;
    }

    FillResponse getResponse() {
        return this.mResponse;
    }

    void setResponse(FillResponse response) {
        this.mResponse = response;
    }

    CharSequence getServiceName() {
        return this.mSession.getServiceName();
    }

    int getState() {
        return this.mState;
    }

    String getStateAsString() {
        return DebugUtils.flagsToString(ViewState.class, "STATE_", this.mState);
    }

    void setState(int state) {
        if (this.mState == 1) {
            this.mState = state;
        } else {
            this.mState |= state;
        }
    }

    void update(AutofillValue autofillValue, Rect virtualBounds, int flags) {
        if (autofillValue != null) {
            this.mCurrentValue = autofillValue;
        }
        if (virtualBounds != null) {
            this.mVirtualBounds = virtualBounds;
        }
        maybeCallOnFillReady(flags);
    }

    void maybeCallOnFillReady(int flags) {
        if ((this.mState & 4) == 0 || (flags & 1) != 0) {
            if (!(this.mResponse == null || (this.mResponse.getDatasets() == null && this.mResponse.getAuthentication() == null))) {
                this.mListener.onFillReady(this.mResponse, this.id, this.mCurrentValue);
            }
            return;
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "Ignoring UI for " + this.id + " on " + getStateAsString());
        }
    }

    public String toString() {
        return "ViewState: [id=" + this.id + ", currentValue=" + this.mCurrentValue + ", autofilledValue=" + this.mAutofilledValue + ", bounds=" + this.mVirtualBounds + ", state=" + getStateAsString() + "]";
    }

    void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("id:");
        pw.println(this.id);
        pw.print(prefix);
        pw.print("state:");
        pw.println(getStateAsString());
        pw.print(prefix);
        pw.print("response:");
        if (this.mResponse == null) {
            pw.println("N/A");
        } else if (Helper.sVerbose) {
            pw.println(this.mResponse);
        } else {
            pw.println(this.mResponse.getRequestId());
        }
        pw.print(prefix);
        pw.print("currentValue:");
        pw.println(this.mCurrentValue);
        pw.print(prefix);
        pw.print("autofilledValue:");
        pw.println(this.mAutofilledValue);
        pw.print(prefix);
        pw.print("virtualBounds:");
        pw.println(this.mVirtualBounds);
    }
}
