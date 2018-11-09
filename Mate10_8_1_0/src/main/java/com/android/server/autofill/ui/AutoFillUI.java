package com.android.server.autofill.ui;

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
import android.view.autofill.AutofillId;
import android.view.autofill.IAutofillWindowPresenter;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.server.UiThread;
import com.android.server.autofill.Helper;
import com.android.server.autofill.ui.-$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8.AnonymousClass3;
import com.android.server.autofill.ui.-$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8.AnonymousClass4;
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
        this.mHandler.post(new -$Lambda$TTOM-vgvIOJotO3pKgpKhg7oNlE((byte) 3, this, callback));
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_AutoFillUI_3272(AutoFillUiCallback callback) {
        if (this.mCallback != callback) {
            if (this.mCallback != null) {
                hideAllUiThread(this.mCallback);
            }
            this.mCallback = callback;
        }
    }

    public void clearCallback(AutoFillUiCallback callback) {
        this.mHandler.post(new -$Lambda$TTOM-vgvIOJotO3pKgpKhg7oNlE((byte) 0, this, callback));
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_AutoFillUI_3592(AutoFillUiCallback callback) {
        if (this.mCallback == callback) {
            hideAllUiThread(callback);
            this.mCallback = null;
        }
    }

    public void showError(int resId, AutoFillUiCallback callback) {
        showError(this.mContext.getString(resId), callback);
    }

    public void showError(CharSequence message, AutoFillUiCallback callback) {
        Slog.w(TAG, "showError(): " + message);
        this.mHandler.post(new -$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8((byte) 1, this, callback, message));
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_AutoFillUI_4184(AutoFillUiCallback callback, CharSequence message) {
        if (this.mCallback == callback) {
            hideAllUiThread(callback);
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(this.mContext, message, 1).show();
            }
        }
    }

    public void hideFillUi(AutoFillUiCallback callback) {
        this.mHandler.post(new -$Lambda$TTOM-vgvIOJotO3pKgpKhg7oNlE((byte) 2, this, callback));
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_AutoFillUI_4598(AutoFillUiCallback callback) {
        hideFillUiUiThread(callback);
    }

    public void filterFillUi(String filterText, AutoFillUiCallback callback) {
        this.mHandler.post(new -$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8((byte) 0, this, callback, filterText));
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_AutoFillUI_4872(AutoFillUiCallback callback, String filterText) {
        if (callback == this.mCallback && this.mFillUi != null) {
            this.mFillUi.setFilterText(filterText);
        }
    }

    public void showFillUi(AutofillId focusedId, FillResponse response, String filterText, String servicePackageName, String packageName, AutoFillUiCallback callback) {
        int i = 0;
        if (Helper.sDebug) {
            Slog.d(TAG, "showFillUi(): id=" + focusedId + ", filter=" + (filterText == null ? 0 : filterText.length()) + " chars");
        }
        LogMaker addTaggedData = Helper.newLogMaker(910, packageName, servicePackageName).addTaggedData(911, Integer.valueOf(filterText == null ? 0 : filterText.length()));
        if (response.getDatasets() != null) {
            i = response.getDatasets().size();
        }
        this.mHandler.post(new com.android.server.autofill.ui.-$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8.AnonymousClass1(this, callback, response, focusedId, filterText, addTaggedData.addTaggedData(909, Integer.valueOf(i))));
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_AutoFillUI_6451(AutoFillUiCallback callback, FillResponse response, AutofillId focusedId, String filterText, LogMaker log) {
        if (callback == this.mCallback) {
            hideAllUiThread(callback);
            final LogMaker logMaker = log;
            final AutoFillUiCallback autoFillUiCallback = callback;
            final FillResponse fillResponse = response;
            final AutofillId autofillId = focusedId;
            this.mFillUi = new FillUi(this.mContext, response, focusedId, filterText, this.mOverlayControl, new Callback() {
                public void onResponsePicked(FillResponse response) {
                    logMaker.setType(3);
                    AutoFillUI.this.hideFillUiUiThread(autoFillUiCallback);
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.authenticate(response.getRequestId(), NetworkConstants.ARP_HWTYPE_RESERVED_HI, response.getAuthentication(), response.getClientState());
                    }
                }

                public void onDatasetPicked(Dataset dataset) {
                    logMaker.setType(4);
                    AutoFillUI.this.hideFillUiUiThread(autoFillUiCallback);
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.fill(fillResponse.getRequestId(), fillResponse.getDatasets().indexOf(dataset), dataset);
                    }
                }

                public void onCanceled() {
                    logMaker.setType(5);
                    AutoFillUI.this.hideFillUiUiThread(autoFillUiCallback);
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
            });
        }
    }

    public void showSaveUi(CharSequence serviceLabel, Drawable serviceIcon, String servicePackageName, SaveInfo info, ValueFinder valueFinder, String packageName, AutoFillUiCallback callback, PendingUi pendingSaveUi) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "showSaveUi() for " + packageName + ": " + info);
        }
        this.mHandler.post(new com.android.server.autofill.ui.-$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8.AnonymousClass2(this, callback, pendingSaveUi, serviceLabel, serviceIcon, servicePackageName, packageName, info, valueFinder, Helper.newLogMaker(916, packageName, servicePackageName).addTaggedData(917, Integer.valueOf(((info.getRequiredIds() == null ? 0 : info.getRequiredIds().length) + 0) + (info.getOptionalIds() == null ? 0 : info.getOptionalIds().length)))));
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_AutoFillUI_9969(AutoFillUiCallback callback, final PendingUi pendingSaveUi, CharSequence serviceLabel, Drawable serviceIcon, String servicePackageName, String packageName, SaveInfo info, ValueFinder valueFinder, LogMaker log) {
        if (callback == this.mCallback) {
            hideAllUiThread(callback);
            final LogMaker logMaker = log;
            this.mSaveUi = new SaveUi(this.mContext, pendingSaveUi, serviceLabel, serviceIcon, servicePackageName, packageName, info, valueFinder, this.mOverlayControl, new OnSaveListener() {
                public void onSave() {
                    logMaker.setType(4);
                    AutoFillUI.this.hideSaveUiUiThread(AutoFillUI.this.mCallback);
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.save();
                    }
                    AutoFillUI.this.destroySaveUiUiThread(pendingSaveUi, true);
                }

                public void onCancel(IntentSender listener) {
                    logMaker.setType(5);
                    AutoFillUI.this.hideSaveUiUiThread(AutoFillUI.this.mCallback);
                    if (listener != null) {
                        try {
                            listener.sendIntent(AutoFillUI.this.mContext, 0, null, null, null);
                        } catch (SendIntentException e) {
                            Slog.e(AutoFillUI.TAG, "Error starting negative action listener: " + listener, e);
                        }
                    }
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.cancelSave();
                    }
                    AutoFillUI.this.destroySaveUiUiThread(pendingSaveUi, true);
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
            });
        }
    }

    public void onPendingSaveUi(int operation, IBinder token) {
        this.mHandler.post(new AnonymousClass3(operation, this, token));
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_AutoFillUI_12114(int operation, IBinder token) {
        if (this.mSaveUi != null) {
            this.mSaveUi.onPendingUi(operation, token);
        } else {
            Slog.w(TAG, "onPendingSaveUi(" + operation + "): no save ui");
        }
    }

    public void hideAll(AutoFillUiCallback callback) {
        this.mHandler.post(new -$Lambda$TTOM-vgvIOJotO3pKgpKhg7oNlE((byte) 1, this, callback));
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_AutoFillUI_12481(AutoFillUiCallback callback) {
        hideAllUiThread(callback);
    }

    public void destroyAll(PendingUi pendingSaveUi, AutoFillUiCallback callback, boolean notifyClient) {
        this.mHandler.post(new AnonymousClass4(notifyClient, this, pendingSaveUi, callback));
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_AutoFillUI_12732(PendingUi pendingSaveUi, AutoFillUiCallback callback, boolean notifyClient) {
        destroyAllUiThread(pendingSaveUi, callback, notifyClient);
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

    private void hideFillUiUiThread(AutoFillUiCallback callback) {
        if (this.mFillUi == null) {
            return;
        }
        if (callback == null || callback == this.mCallback) {
            this.mFillUi.destroy();
            this.mFillUi = null;
        }
    }

    private PendingUi hideSaveUiUiThread(AutoFillUiCallback callback) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "hideSaveUiUiThread(): mSaveUi=" + this.mSaveUi + ", callback=" + callback + ", mCallback=" + this.mCallback);
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
            Slog.d(TAG, "destroySaveUiUiThread(): " + pendingSaveUi);
        }
        this.mSaveUi.destroy();
        this.mSaveUi = null;
        if (pendingSaveUi != null && notifyClient) {
            try {
                if (Helper.sDebug) {
                    Slog.d(TAG, "destroySaveUiUiThread(): notifying client");
                }
                pendingSaveUi.client.setSaveUiState(pendingSaveUi.id, false);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error notifying client to set save UI state to hidden: " + e);
            }
        }
    }

    private void destroyAllUiThread(PendingUi pendingSaveUi, AutoFillUiCallback callback, boolean notifyClient) {
        hideFillUiUiThread(callback);
        destroySaveUiUiThread(pendingSaveUi, notifyClient);
    }

    private void hideAllUiThread(AutoFillUiCallback callback) {
        hideFillUiUiThread(callback);
        PendingUi pendingSaveUi = hideSaveUiUiThread(callback);
        if (pendingSaveUi != null && pendingSaveUi.getState() == 4) {
            if (Helper.sDebug) {
                Slog.d(TAG, "hideAllUiThread(): destroying Save UI because pending restoration is finished");
            }
            destroySaveUiUiThread(pendingSaveUi, true);
        }
    }
}
