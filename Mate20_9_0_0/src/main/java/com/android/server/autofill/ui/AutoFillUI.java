package com.android.server.autofill.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.net.util.NetworkConstants;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveInfo;
import android.service.autofill.ValueFinder;
import android.text.TextUtils;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.autofill.AutofillId;
import android.view.autofill.IAutofillWindowPresenter;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.server.UiThread;
import com.android.server.autofill.Helper;
import com.android.server.autofill.ui.SaveUi.OnSaveListener;
import java.io.PrintWriter;

public final class AutoFillUI {
    private static final String TAG = "AutofillUI";
    private AutoFillUiCallback mCallback;
    private final Context mContext;
    private FillUi mFillUi;
    private final Handler mHandler = UiThread.getHandler();
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final OverlayControl mOverlayControl;
    private SaveUi mSaveUi;

    public interface AutoFillUiCallback {
        void authenticate(int i, int i2, IntentSender intentSender, Bundle bundle);

        void cancelSave();

        void dispatchUnhandledKey(AutofillId autofillId, KeyEvent keyEvent);

        void fill(int i, int i2, Dataset dataset);

        void requestHideFillUi(AutofillId autofillId);

        void requestShowFillUi(AutofillId autofillId, int i, int i2, IAutofillWindowPresenter iAutofillWindowPresenter);

        void save();

        void startIntentSender(IntentSender intentSender);
    }

    public AutoFillUI(Context context) {
        this.mContext = context;
        this.mOverlayControl = new OverlayControl(context);
    }

    public void setCallback(AutoFillUiCallback callback) {
        this.mHandler.post(new -$$Lambda$AutoFillUI$Z-Di7CGd-L0nOI4i7_RO1FYbhgU(this, callback));
    }

    public static /* synthetic */ void lambda$setCallback$0(AutoFillUI autoFillUI, AutoFillUiCallback callback) {
        if (autoFillUI.mCallback != callback) {
            if (autoFillUI.mCallback != null) {
                autoFillUI.hideAllUiThread(autoFillUI.mCallback);
            }
            autoFillUI.mCallback = callback;
        }
    }

    public void clearCallback(AutoFillUiCallback callback) {
        this.mHandler.post(new -$$Lambda$AutoFillUI$i7qTc5vqiej5Psbl-bIkD7js-Ao(this, callback));
    }

    public static /* synthetic */ void lambda$clearCallback$1(AutoFillUI autoFillUI, AutoFillUiCallback callback) {
        if (autoFillUI.mCallback == callback) {
            autoFillUI.hideAllUiThread(callback);
            autoFillUI.mCallback = null;
        }
    }

    public void showError(int resId, AutoFillUiCallback callback) {
        showError(this.mContext.getString(resId), callback);
    }

    public void showError(CharSequence message, AutoFillUiCallback callback) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("showError(): ");
        stringBuilder.append(message);
        Slog.w(str, stringBuilder.toString());
        this.mHandler.post(new -$$Lambda$AutoFillUI$S8lqjy9BKKn2SSfu43iaVPGD6rg(this, callback, message));
    }

    public static /* synthetic */ void lambda$showError$2(AutoFillUI autoFillUI, AutoFillUiCallback callback, CharSequence message) {
        if (autoFillUI.mCallback == callback) {
            autoFillUI.hideAllUiThread(callback);
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(autoFillUI.mContext, message, 1).show();
            }
        }
    }

    public void hideFillUi(AutoFillUiCallback callback) {
        this.mHandler.post(new -$$Lambda$AutoFillUI$VF2EbGE70QNyGDbklN9Uz5xHqyQ(this, callback));
    }

    public void filterFillUi(String filterText, AutoFillUiCallback callback) {
        this.mHandler.post(new -$$Lambda$AutoFillUI$LjywPhTUqjU0ZUlG1crxBg8qhRA(this, callback, filterText));
    }

    public static /* synthetic */ void lambda$filterFillUi$4(AutoFillUI autoFillUI, AutoFillUiCallback callback, String filterText) {
        if (callback == autoFillUI.mCallback && autoFillUI.mFillUi != null) {
            autoFillUI.mFillUi.setFilterText(filterText);
        }
    }

    public void showFillUi(AutofillId focusedId, FillResponse response, String filterText, String servicePackageName, ComponentName componentName, CharSequence serviceLabel, Drawable serviceIcon, AutoFillUiCallback callback, int sessionId, boolean compatMode) {
        AutofillId autofillId;
        int i = 0;
        if (Helper.sDebug) {
            int size = filterText == null ? 0 : filterText.length();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("showFillUi(): id=");
            autofillId = focusedId;
            stringBuilder.append(autofillId);
            stringBuilder.append(", filter=");
            stringBuilder.append(size);
            stringBuilder.append(" chars");
            Slog.d(str, stringBuilder.toString());
        } else {
            autofillId = focusedId;
        }
        LogMaker addTaggedData = Helper.newLogMaker(910, componentName, servicePackageName, sessionId, compatMode).addTaggedData(911, Integer.valueOf(filterText == null ? 0 : filterText.length()));
        if (response.getDatasets() != null) {
            i = response.getDatasets().size();
        }
        LogMaker log = addTaggedData.addTaggedData(909, Integer.valueOf(i));
        -$$Lambda$AutoFillUI$H0BWucCEHDp2_3FUpZ9-CLDtxYQ -__lambda_autofillui_h0bwuccehdp2_3fupz9-cldtxyq = r0;
        Handler handler = this.mHandler;
        -$$Lambda$AutoFillUI$H0BWucCEHDp2_3FUpZ9-CLDtxYQ -__lambda_autofillui_h0bwuccehdp2_3fupz9-cldtxyq2 = new -$$Lambda$AutoFillUI$H0BWucCEHDp2_3FUpZ9-CLDtxYQ(this, callback, response, autofillId, filterText, serviceLabel, serviceIcon, log);
        handler.post(-__lambda_autofillui_h0bwuccehdp2_3fupz9-cldtxyq);
    }

    public static /* synthetic */ void lambda$showFillUi$5(AutoFillUI autoFillUI, AutoFillUiCallback callback, FillResponse response, AutofillId focusedId, String filterText, CharSequence serviceLabel, Drawable serviceIcon, LogMaker log) {
        AutoFillUI autoFillUI2 = autoFillUI;
        AutoFillUiCallback autoFillUiCallback = callback;
        if (autoFillUiCallback == autoFillUI2.mCallback) {
            autoFillUI.hideAllUiThread(callback);
            Context context = autoFillUI2.mContext;
            OverlayControl overlayControl = autoFillUI2.mOverlayControl;
            final LogMaker logMaker = log;
            final AutoFillUiCallback autoFillUiCallback2 = autoFillUiCallback;
            final FillResponse fillResponse = response;
            final AutofillId autofillId = focusedId;
            Callback anonymousClass1 = new Callback() {
                public void onResponsePicked(FillResponse response) {
                    logMaker.setType(3);
                    AutoFillUI.this.hideFillUiUiThread(autoFillUiCallback2, true);
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.authenticate(response.getRequestId(), NetworkConstants.ARP_HWTYPE_RESERVED_HI, response.getAuthentication(), response.getClientState());
                    }
                }

                public void onDatasetPicked(Dataset dataset) {
                    logMaker.setType(4);
                    AutoFillUI.this.hideFillUiUiThread(autoFillUiCallback2, true);
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.fill(fillResponse.getRequestId(), fillResponse.getDatasets().indexOf(dataset), dataset);
                    }
                }

                public void onCanceled() {
                    logMaker.setType(5);
                    AutoFillUI.this.hideFillUiUiThread(autoFillUiCallback2, true);
                }

                public void onDestroy() {
                    if (logMaker.getType() == 0) {
                        logMaker.setType(2);
                    }
                    AutoFillUI.this.mMetricsLogger.write(logMaker);
                }

                public void requestShowFillUi(int width, int height, IAutofillWindowPresenter windowPresenter) {
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.requestShowFillUi(autofillId, width, height, windowPresenter);
                    }
                }

                public void requestHideFillUi() {
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.requestHideFillUi(autofillId);
                    }
                }

                public void startIntentSender(IntentSender intentSender) {
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.startIntentSender(intentSender);
                    }
                }

                public void dispatchUnhandledKey(KeyEvent keyEvent) {
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.dispatchUnhandledKey(autofillId, keyEvent);
                    }
                }
            };
            FillUi fillUi = r8;
            FillUi fillUi2 = new FillUi(context, response, focusedId, filterText, overlayControl, serviceLabel, serviceIcon, anonymousClass1);
            autoFillUI2.mFillUi = fillUi;
        }
    }

    public void showSaveUi(CharSequence serviceLabel, Drawable serviceIcon, String servicePackageName, SaveInfo info, ValueFinder valueFinder, ComponentName componentName, AutoFillUiCallback callback, PendingUi pendingSaveUi, boolean compatMode) {
        SaveInfo saveInfo;
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("showSaveUi() for ");
            stringBuilder.append(componentName.toShortString());
            stringBuilder.append(": ");
            saveInfo = info;
            stringBuilder.append(saveInfo);
            Slog.v(str, stringBuilder.toString());
        } else {
            saveInfo = info;
        }
        int i = 0;
        int numIds = 0 + (info.getRequiredIds() == null ? 0 : info.getRequiredIds().length);
        if (info.getOptionalIds() != null) {
            i = info.getOptionalIds().length;
        }
        numIds += i;
        PendingUi pendingUi = pendingSaveUi;
        String str2 = servicePackageName;
        ComponentName componentName2 = componentName;
        LogMaker log = Helper.newLogMaker(916, componentName2, str2, pendingUi.sessionId, compatMode).addTaggedData(917, Integer.valueOf(numIds));
        -$$Lambda$AutoFillUI$xTxq_LM_GKvWtCQ0xT88Q_Y8M7Q -__lambda_autofillui_xtxq_lm_gkvwtcq0xt88q_y8m7q = r3;
        Handler handler = this.mHandler;
        -$$Lambda$AutoFillUI$xTxq_LM_GKvWtCQ0xT88Q_Y8M7Q -__lambda_autofillui_xtxq_lm_gkvwtcq0xt88q_y8m7q2 = new -$$Lambda$AutoFillUI$xTxq_LM_GKvWtCQ0xT88Q_Y8M7Q(this, callback, pendingUi, serviceLabel, serviceIcon, str2, componentName2, saveInfo, valueFinder, log, compatMode);
        handler.post(-__lambda_autofillui_xtxq_lm_gkvwtcq0xt88q_y8m7q);
    }

    public static /* synthetic */ void lambda$showSaveUi$6(AutoFillUI autoFillUI, AutoFillUiCallback callback, PendingUi pendingSaveUi, CharSequence serviceLabel, Drawable serviceIcon, String servicePackageName, ComponentName componentName, SaveInfo info, ValueFinder valueFinder, LogMaker log, boolean compatMode) {
        AutoFillUI autoFillUI2 = autoFillUI;
        if (callback == autoFillUI2.mCallback) {
            autoFillUI.hideAllUiThread(callback);
            final PendingUi pendingUi = pendingSaveUi;
            final LogMaker logMaker = log;
            autoFillUI2.mSaveUi = new SaveUi(autoFillUI2.mContext, pendingUi, serviceLabel, serviceIcon, servicePackageName, componentName, info, valueFinder, autoFillUI2.mOverlayControl, new OnSaveListener() {
                public void onSave() {
                    logMaker.setType(4);
                    AutoFillUI.this.hideSaveUiUiThread(AutoFillUI.this.mCallback);
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.save();
                    }
                    AutoFillUI.this.destroySaveUiUiThread(pendingUi, true);
                }

                public void onCancel(IntentSender listener) {
                    logMaker.setType(5);
                    AutoFillUI.this.hideSaveUiUiThread(AutoFillUI.this.mCallback);
                    if (listener != null) {
                        try {
                            listener.sendIntent(AutoFillUI.this.mContext, 0, null, null, null);
                        } catch (SendIntentException e) {
                            String str = AutoFillUI.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error starting negative action listener: ");
                            stringBuilder.append(listener);
                            Slog.e(str, stringBuilder.toString(), e);
                        }
                    }
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.cancelSave();
                    }
                    AutoFillUI.this.destroySaveUiUiThread(pendingUi, true);
                }

                public void onDestroy() {
                    if (logMaker.getType() == 0) {
                        logMaker.setType(2);
                        if (AutoFillUI.this.mCallback != null) {
                            AutoFillUI.this.mCallback.cancelSave();
                        }
                    }
                    AutoFillUI.this.mMetricsLogger.write(logMaker);
                }
            }, compatMode);
        }
    }

    public void onPendingSaveUi(int operation, IBinder token) {
        this.mHandler.post(new -$$Lambda$AutoFillUI$R46Kz1SlDpiZBOYi-1HNH5FBjnU(this, operation, token));
    }

    public static /* synthetic */ void lambda$onPendingSaveUi$7(AutoFillUI autoFillUI, int operation, IBinder token) {
        if (autoFillUI.mSaveUi != null) {
            autoFillUI.mSaveUi.onPendingUi(operation, token);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onPendingSaveUi(");
        stringBuilder.append(operation);
        stringBuilder.append("): no save ui");
        Slog.w(str, stringBuilder.toString());
    }

    public void hideAll(AutoFillUiCallback callback) {
        this.mHandler.post(new -$$Lambda$AutoFillUI$56AC3ykfo4h_e2LSjdkJ3XQn370(this, callback));
    }

    public void destroyAll(PendingUi pendingSaveUi, AutoFillUiCallback callback, boolean notifyClient) {
        this.mHandler.post(new -$$Lambda$AutoFillUI$XWhvh2-Jd9NLMoEos-e8RkZdQaI(this, pendingSaveUi, callback, notifyClient));
    }

    public void dump(PrintWriter pw) {
        pw.println("Autofill UI");
        String prefix = "  ";
        String prefix2 = "    ";
        if (this.mFillUi != null) {
            pw.print("  ");
            pw.println("showsFillUi: true");
            this.mFillUi.dump(pw, "    ");
        } else {
            pw.print("  ");
            pw.println("showsFillUi: false");
        }
        if (this.mSaveUi != null) {
            pw.print("  ");
            pw.println("showsSaveUi: true");
            this.mSaveUi.dump(pw, "    ");
            return;
        }
        pw.print("  ");
        pw.println("showsSaveUi: false");
    }

    private void hideFillUiUiThread(AutoFillUiCallback callback, boolean notifyClient) {
        if (this.mFillUi == null) {
            return;
        }
        if (callback == null || callback == this.mCallback) {
            this.mFillUi.destroy(notifyClient);
            this.mFillUi = null;
        }
    }

    private PendingUi hideSaveUiUiThread(AutoFillUiCallback callback) {
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hideSaveUiUiThread(): mSaveUi=");
            stringBuilder.append(this.mSaveUi);
            stringBuilder.append(", callback=");
            stringBuilder.append(callback);
            stringBuilder.append(", mCallback=");
            stringBuilder.append(this.mCallback);
            Slog.v(str, stringBuilder.toString());
        }
        if (this.mSaveUi == null || (callback != null && callback != this.mCallback)) {
            return null;
        }
        return this.mSaveUi.hide();
    }

    private void destroySaveUiUiThread(PendingUi pendingSaveUi, boolean notifyClient) {
        if (this.mSaveUi == null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "destroySaveUiUiThread(): already destroyed");
            }
            return;
        }
        if (Helper.sDebug) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("destroySaveUiUiThread(): ");
            stringBuilder.append(pendingSaveUi);
            Slog.d(str, stringBuilder.toString());
        }
        this.mSaveUi.destroy();
        this.mSaveUi = null;
        if (pendingSaveUi != null && notifyClient) {
            try {
                if (Helper.sDebug) {
                    Slog.d(TAG, "destroySaveUiUiThread(): notifying client");
                }
                pendingSaveUi.client.setSaveUiState(pendingSaveUi.sessionId, false);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error notifying client to set save UI state to hidden: ");
                stringBuilder2.append(e);
                Slog.e(str2, stringBuilder2.toString());
            }
        }
    }

    private void destroyAllUiThread(PendingUi pendingSaveUi, AutoFillUiCallback callback, boolean notifyClient) {
        hideFillUiUiThread(callback, notifyClient);
        destroySaveUiUiThread(pendingSaveUi, notifyClient);
    }

    private void hideAllUiThread(AutoFillUiCallback callback) {
        hideFillUiUiThread(callback, true);
        PendingUi pendingSaveUi = hideSaveUiUiThread(callback);
        if (pendingSaveUi != null && pendingSaveUi.getState() == 4) {
            if (Helper.sDebug) {
                Slog.d(TAG, "hideAllUiThread(): destroying Save UI because pending restoration is finished");
            }
            destroySaveUiUiThread(pendingSaveUi, true);
        }
    }
}
