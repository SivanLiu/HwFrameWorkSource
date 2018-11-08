package com.android.server.autofill.ui;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.autofill.CustomDescription;
import android.service.autofill.SaveInfo;
import android.service.autofill.ValueFinder;
import android.text.Html;
import android.util.ArraySet;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.RemoteViews.OnClickHandler;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.server.UiThread;
import com.android.server.autofill.Helper;
import java.io.PrintWriter;

final class SaveUi {
    private static final String TAG = "AutofillSaveUi";
    private boolean mDestroyed;
    private final Dialog mDialog;
    private final Handler mHandler = UiThread.getHandler();
    private final OneTimeListener mListener;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final OverlayControl mOverlayControl;
    private final String mPackageName;
    private final PendingUi mPendingUi;
    private final String mServicePackageName;
    private final CharSequence mSubTitle;
    private final CharSequence mTitle;

    public interface OnSaveListener {
        void onCancel(IntentSender intentSender);

        void onDestroy();

        void onSave();
    }

    private class OneTimeListener implements OnSaveListener {
        private boolean mDone;
        private final OnSaveListener mRealListener;

        OneTimeListener(OnSaveListener realListener) {
            this.mRealListener = realListener;
        }

        public void onSave() {
            if (Helper.sDebug) {
                Slog.d(SaveUi.TAG, "OneTimeListener.onSave(): " + this.mDone);
            }
            if (!this.mDone) {
                this.mDone = true;
                this.mRealListener.onSave();
            }
        }

        public void onCancel(IntentSender listener) {
            if (Helper.sDebug) {
                Slog.d(SaveUi.TAG, "OneTimeListener.onCancel(): " + this.mDone);
            }
            if (!this.mDone) {
                this.mDone = true;
                this.mRealListener.onCancel(listener);
            }
        }

        public void onDestroy() {
            if (Helper.sDebug) {
                Slog.d(SaveUi.TAG, "OneTimeListener.onDestroy(): " + this.mDone);
            }
            if (!this.mDone) {
                this.mDone = true;
                this.mRealListener.onDestroy();
            }
        }
    }

    SaveUi(Context context, PendingUi pendingUi, CharSequence serviceLabel, Drawable serviceIcon, String servicePackageName, String packageName, SaveInfo info, ValueFinder valueFinder, OverlayControl overlayControl, OnSaveListener listener) {
        this.mPendingUi = pendingUi;
        this.mListener = new OneTimeListener(listener);
        this.mOverlayControl = overlayControl;
        this.mServicePackageName = servicePackageName;
        this.mPackageName = packageName;
        context.setTheme(context.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null));
        View view = LayoutInflater.from(context).inflate(17367101, null);
        TextView titleView = (TextView) view.findViewById(16908743);
        ArraySet<String> arraySet = new ArraySet(3);
        int type = info.getType();
        if ((type & 1) != 0) {
            arraySet.add(context.getString(17039667));
        }
        if ((type & 2) != 0) {
            arraySet.add(context.getString(17039664));
        }
        if ((type & 4) != 0) {
            arraySet.add(context.getString(17039665));
        }
        if ((type & 8) != 0) {
            arraySet.add(context.getString(17039668));
        }
        if ((type & 16) != 0) {
            arraySet.add(context.getString(17039666));
        }
        switch (arraySet.size()) {
            case 1:
                this.mTitle = Html.fromHtml(context.getString(17039663, new Object[]{arraySet.valueAt(0), serviceLabel}), 0);
                break;
            case 2:
                this.mTitle = Html.fromHtml(context.getString(17039661, new Object[]{arraySet.valueAt(0), arraySet.valueAt(1), serviceLabel}), 0);
                break;
            case 3:
                this.mTitle = Html.fromHtml(context.getString(17039662, new Object[]{arraySet.valueAt(0), arraySet.valueAt(1), arraySet.valueAt(2), serviceLabel}), 0);
                break;
            default:
                this.mTitle = Html.fromHtml(context.getString(17039660, new Object[]{serviceLabel}), 0);
                break;
        }
        titleView.setText(this.mTitle);
        setServiceIcon(context, view, serviceIcon);
        CustomDescription customDescription = info.getCustomDescription();
        ScrollView subtitleContainer;
        if (customDescription != null) {
            writeLog(1129, type);
            this.mSubTitle = null;
            if (Helper.sDebug) {
                Slog.d(TAG, "Using custom description");
            }
            RemoteViews presentation = customDescription.getPresentation(valueFinder);
            if (presentation != null) {
                final int i = type;
                final PendingUi pendingUi2 = pendingUi;
                try {
                    subtitleContainer = (ScrollView) view.findViewById(16908740);
                    subtitleContainer.addView(presentation.apply(context, null, new OnClickHandler() {
                        public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent intent) {
                            LogMaker log = SaveUi.this.newLogMaker(1132, i);
                            if (SaveUi.isValidLink(pendingIntent, intent)) {
                                if (Helper.sVerbose) {
                                    Slog.v(SaveUi.TAG, "Intercepting custom description intent");
                                }
                                IBinder token = SaveUi.this.mPendingUi.getToken();
                                intent.putExtra("android.view.autofill.extra.RESTORE_SESSION_TOKEN", token);
                                try {
                                    pendingUi2.client.startIntentSender(pendingIntent.getIntentSender(), intent);
                                    SaveUi.this.mPendingUi.setState(2);
                                    if (Helper.sDebug) {
                                        Slog.d(SaveUi.TAG, "hiding UI until restored with token " + token);
                                    }
                                    SaveUi.this.hide();
                                    log.setType(1);
                                    SaveUi.this.mMetricsLogger.write(log);
                                    return true;
                                } catch (RemoteException e) {
                                    Slog.w(SaveUi.TAG, "error triggering pending intent: " + intent);
                                    log.setType(11);
                                    SaveUi.this.mMetricsLogger.write(log);
                                    return false;
                                }
                            }
                            log.setType(0);
                            SaveUi.this.mMetricsLogger.write(log);
                            return false;
                        }
                    }));
                    subtitleContainer.setVisibility(0);
                } catch (Exception e) {
                    Slog.e(TAG, "Could not inflate custom description. ", e);
                }
            } else {
                Slog.w(TAG, "could not create remote presentation for custom title");
            }
        } else {
            this.mSubTitle = info.getDescription();
            if (this.mSubTitle != null) {
                writeLog(1131, type);
                subtitleContainer = (ScrollView) view.findViewById(16908740);
                TextView subtitleView = new TextView(context);
                subtitleView.setText(this.mSubTitle);
                subtitleContainer.addView(subtitleView, new LayoutParams(-1, -2));
                subtitleContainer.setVisibility(0);
            }
            if (Helper.sDebug) {
                Slog.d(TAG, "on constructor: title=" + this.mTitle + ", subTitle=" + this.mSubTitle);
            }
        }
        TextView noButton = (TextView) view.findViewById(16908742);
        if (info.getNegativeActionStyle() == 1) {
            noButton.setText(17040937);
        } else {
            noButton.setText(17039659);
        }
        noButton.setOnClickListener(new -$Lambda$At7ghLP7ePb8IjX4T-3J791grHE((byte) 1, this, info));
        view.findViewById(16908744).setOnClickListener(new com.android.server.autofill.ui.-$Lambda$lWFJV62mVsorLi23UkgJkVRbfB8.AnonymousClass1(this));
        Builder builder = new Builder(context);
        builder.setView(view);
        this.mDialog = builder.create();
        this.mDialog.setOnDismissListener(new -$Lambda$lWFJV62mVsorLi23UkgJkVRbfB8(this));
        Window window = this.mDialog.getWindow();
        window.setType(2038);
        window.addFlags(393256);
        window.addPrivateFlags(16);
        window.setSoftInputMode(32);
        window.setGravity(81);
        window.setCloseOnTouchOutside(true);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = -1;
        params.accessibilityTitle = context.getString(17039658);
        params.windowAnimations = 16974601;
        params.privateFlags |= 16;
        show();
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_SaveUi_11594(SaveInfo info, View v) {
        this.mListener.onCancel(info.getNegativeActionListener());
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_SaveUi_11768(View v) {
        this.mListener.onSave();
    }

    /* synthetic */ void lambda$-com_android_server_autofill_ui_SaveUi_12236(DialogInterface d) {
        this.mListener.onCancel(null);
    }

    private void setServiceIcon(Context context, View view, Drawable serviceIcon) {
        ImageView iconView = (ImageView) view.findViewById(16908741);
        int maxWidth = context.getResources().getDimensionPixelSize(17104937);
        int maxHeight = maxWidth;
        int actualWidth = serviceIcon.getMinimumWidth();
        int actualHeight = serviceIcon.getMinimumHeight();
        if (actualWidth > maxWidth || actualHeight > maxWidth) {
            Slog.w(TAG, "Not adding service icon of size (" + actualWidth + "x" + actualHeight + ") because maximum is " + "(" + maxWidth + "x" + maxWidth + ").");
            ((ViewGroup) iconView.getParent()).removeView(iconView);
            return;
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "Adding service icon (" + actualWidth + "x" + actualHeight + ") as it's less than maximum " + "(" + maxWidth + "x" + maxWidth + ").");
        }
        iconView.setImageDrawable(serviceIcon);
    }

    private static boolean isValidLink(PendingIntent pendingIntent, Intent intent) {
        if (pendingIntent == null) {
            Slog.w(TAG, "isValidLink(): custom description without pending intent");
            return false;
        } else if (!pendingIntent.isActivity()) {
            Slog.w(TAG, "isValidLink(): pending intent not for activity");
            return false;
        } else if (intent != null) {
            return true;
        } else {
            Slog.w(TAG, "isValidLink(): no intent");
            return false;
        }
    }

    private LogMaker newLogMaker(int category, int saveType) {
        return newLogMaker(category).addTaggedData(1130, Integer.valueOf(saveType));
    }

    private LogMaker newLogMaker(int category) {
        return new LogMaker(category).setPackageName(this.mPackageName).addTaggedData(908, this.mServicePackageName);
    }

    private void writeLog(int category, int saveType) {
        this.mMetricsLogger.write(newLogMaker(category, saveType));
    }

    void onPendingUi(int operation, IBinder token) {
        if (this.mPendingUi.matches(token)) {
            LogMaker log = newLogMaker(1134);
            switch (operation) {
                case 1:
                    log.setType(5);
                    if (Helper.sDebug) {
                        Slog.d(TAG, "Cancelling pending save dialog for " + token);
                    }
                    hide();
                    break;
                case 2:
                    if (Helper.sDebug) {
                        Slog.d(TAG, "Restoring save dialog for " + token);
                    }
                    log.setType(1);
                    show();
                    break;
                default:
                    try {
                        log.setType(11);
                        Slog.w(TAG, "restore(): invalid operation " + operation);
                        break;
                    } catch (Throwable th) {
                        this.mMetricsLogger.write(log);
                    }
            }
            this.mMetricsLogger.write(log);
            this.mPendingUi.setState(4);
            return;
        }
        Slog.w(TAG, "restore(" + operation + "): got token " + token + " instead of " + this.mPendingUi.getToken());
    }

    private void show() {
        Slog.i(TAG, "Showing save dialog: " + this.mTitle);
        this.mDialog.show();
        this.mOverlayControl.hideOverlays();
    }

    PendingUi hide() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "Hiding save dialog.");
        }
        try {
            this.mDialog.hide();
            return this.mPendingUi;
        } finally {
            this.mOverlayControl.showOverlays();
        }
    }

    void destroy() {
        try {
            if (Helper.sDebug) {
                Slog.d(TAG, "destroy()");
            }
            throwIfDestroyed();
            this.mListener.onDestroy();
            this.mHandler.removeCallbacksAndMessages(this.mListener);
            this.mDialog.dismiss();
            this.mDestroyed = true;
        } finally {
            this.mOverlayControl.showOverlays();
        }
    }

    private void throwIfDestroyed() {
        if (this.mDestroyed) {
            throw new IllegalStateException("cannot interact with a destroyed instance");
        }
    }

    public String toString() {
        return this.mTitle == null ? "NO TITLE" : this.mTitle.toString();
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("title: ");
        pw.println(this.mTitle);
        pw.print(prefix);
        pw.print("subtitle: ");
        pw.println(this.mSubTitle);
        pw.print(prefix);
        pw.print("pendingUi: ");
        pw.println(this.mPendingUi);
        pw.print(prefix);
        pw.print("service: ");
        pw.println(this.mServicePackageName);
        pw.print(prefix);
        pw.print("app: ");
        pw.println(this.mPackageName);
        View view = this.mDialog.getWindow().getDecorView();
        int[] loc = view.getLocationOnScreen();
        pw.print(prefix);
        pw.print("coordinates: ");
        pw.print('(');
        pw.print(loc[0]);
        pw.print(',');
        pw.print(loc[1]);
        pw.print(')');
        pw.print('(');
        pw.print(loc[0] + view.getWidth());
        pw.print(',');
        pw.print(loc[1] + view.getHeight());
        pw.println(')');
        pw.print(prefix);
        pw.print("destroyed: ");
        pw.println(this.mDestroyed);
    }
}
