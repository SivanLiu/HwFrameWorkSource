package com.android.server.autofill.ui;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.autofill.BatchUpdates;
import android.service.autofill.CustomDescription;
import android.service.autofill.InternalTransformation;
import android.service.autofill.InternalValidator;
import android.service.autofill.SaveInfo;
import android.service.autofill.ValueFinder;
import android.text.Html;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.autofill.IHwAutofillHelper;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.RemoteViews.OnClickHandler;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.server.UiThread;
import com.android.server.autofill.Helper;
import java.io.PrintWriter;
import java.util.ArrayList;

final class SaveUi {
    private static final String TAG = "AutofillSaveUi";
    private static final int THEME_ID = 16974778;
    private final boolean mCompatMode;
    private final ComponentName mComponentName;
    private boolean mDestroyed;
    private final Dialog mDialog;
    private final Handler mHandler = UiThread.getHandler();
    private IHwAutofillHelper mHwAutofillHelper = HwFrameworkFactory.getHwAutofillHelper();
    private final OneTimeListener mListener;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final OverlayControl mOverlayControl;
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
                String str = SaveUi.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("OneTimeListener.onSave(): ");
                stringBuilder.append(this.mDone);
                Slog.d(str, stringBuilder.toString());
            }
            if (!this.mDone) {
                this.mDone = true;
                this.mRealListener.onSave();
            }
        }

        public void onCancel(IntentSender listener) {
            if (Helper.sDebug) {
                String str = SaveUi.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("OneTimeListener.onCancel(): ");
                stringBuilder.append(this.mDone);
                Slog.d(str, stringBuilder.toString());
            }
            if (!this.mDone) {
                this.mDone = true;
                this.mRealListener.onCancel(listener);
            }
        }

        public void onDestroy() {
            if (Helper.sDebug) {
                String str = SaveUi.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("OneTimeListener.onDestroy(): ");
                stringBuilder.append(this.mDone);
                Slog.d(str, stringBuilder.toString());
            }
            if (!this.mDone) {
                this.mDone = true;
                this.mRealListener.onDestroy();
            }
        }
    }

    SaveUi(Context context, PendingUi pendingUi, CharSequence serviceLabel, Drawable serviceIcon, String servicePackageName, ComponentName componentName, SaveInfo info, ValueFinder valueFinder, OverlayControl overlayControl, OnSaveListener listener, boolean compatMode) {
        Context context2 = context;
        SaveInfo saveInfo = info;
        this.mPendingUi = pendingUi;
        this.mListener = new OneTimeListener(listener);
        this.mOverlayControl = overlayControl;
        this.mServicePackageName = servicePackageName;
        this.mComponentName = componentName;
        this.mCompatMode = compatMode;
        context2.setTheme(context.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null));
        View view = LayoutInflater.from(context).inflate(17367103, null);
        TextView titleView = (TextView) view.findViewById(16908755);
        ArraySet<String> types = new ArraySet(3);
        int type = info.getType();
        if ((type & 1) != 0) {
            types.add(context2.getString(17039674));
        }
        if ((type & 2) != 0) {
            types.add(context2.getString(17039671));
        }
        if ((type & 4) != 0) {
            types.add(context2.getString(17039672));
        }
        if ((type & 8) != 0) {
            types.add(context2.getString(17039675));
        }
        if ((type & 16) != 0) {
            types.add(context2.getString(17039673));
        }
        switch (types.size()) {
            case 1:
                this.mTitle = Html.fromHtml(context2.getString(17039670, new Object[]{types.valueAt(0), serviceLabel}), 0);
                break;
            case 2:
                int i = 0;
                this.mTitle = Html.fromHtml(context2.getString(17039668, new Object[]{types.valueAt(0), types.valueAt(1), serviceLabel}), 0);
                break;
            case 3:
                this.mTitle = Html.fromHtml(context2.getString(17039669, new Object[]{types.valueAt(0), types.valueAt(1), types.valueAt(2), serviceLabel}), 0);
                break;
            default:
                this.mTitle = Html.fromHtml(context2.getString(17039667, new Object[]{serviceLabel}), 0);
                break;
        }
        titleView.setText(this.mTitle);
        setServiceIcon(context2, view, serviceIcon);
        boolean hasCustomDescription = applyCustomDescription(context2, view, valueFinder, saveInfo);
        if (hasCustomDescription) {
            this.mSubTitle = null;
            if (Helper.sDebug) {
                Slog.d(TAG, "on constructor: applied custom description");
            }
        } else {
            this.mSubTitle = info.getDescription();
            if (this.mSubTitle != null) {
                writeLog(1131, type);
                ViewGroup subtitleContainer = (ViewGroup) view.findViewById(16908752);
                TextView subtitleView = new TextView(context2);
                subtitleView.setText(this.mSubTitle);
                boolean z = hasCustomDescription;
                subtitleContainer.addView(subtitleView, new LayoutParams(-1, -2));
                subtitleContainer.setVisibility(0);
            }
            if (Helper.sDebug) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("on constructor: title=");
                stringBuilder.append(this.mTitle);
                stringBuilder.append(", subTitle=");
                stringBuilder.append(this.mSubTitle);
                Slog.d(str, stringBuilder.toString());
            }
        }
        TextView noButton = (TextView) view.findViewById(16908754);
        if (info.getNegativeActionStyle() == 1) {
            noButton.setText(17041041);
        } else {
            noButton.setText(17039666);
        }
        noButton.setOnClickListener(new -$$Lambda$SaveUi$E9O26NP1L_DDYBfaO7fQ0hhPAx8(this, saveInfo));
        view.findViewById(16908756).setOnClickListener(new -$$Lambda$SaveUi$b3z89RdKv6skukyM-l67uIcvlf0(this));
        Builder builder = new Builder(context2);
        builder.setView(view);
        this.mDialog = builder.create();
        this.mDialog.setOnDismissListener(new -$$Lambda$SaveUi$ckPlzqJfB_ohleAkb5RXKU7mFY8(this));
        Window window = this.mDialog.getWindow();
        window.setType(2038);
        window.addFlags(393248);
        window.addPrivateFlags(16);
        window.setSoftInputMode(32);
        window.setGravity(81);
        window.setCloseOnTouchOutside(true);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = -1;
        params.accessibilityTitle = context2.getString(17039665);
        params.windowAnimations = 16974604;
        params.privateFlags |= 16;
        if (this.mHwAutofillHelper == null || !this.mHwAutofillHelper.isHwAutofillService(context2)) {
            show();
        } else {
            this.mListener.onSave();
        }
    }

    private boolean applyCustomDescription(Context context, View saveUiView, ValueFinder valueFinder, SaveInfo info) {
        Exception e;
        Context context2 = context;
        ValueFinder valueFinder2 = valueFinder;
        CustomDescription customDescription = info.getCustomDescription();
        if (customDescription == null) {
            return false;
        }
        final int type = info.getType();
        writeLog(1129, type);
        RemoteViews template = customDescription.getPresentation();
        if (template == null) {
            Slog.w(TAG, "No remote view on custom description");
            return false;
        }
        ArrayList<Pair<Integer, InternalTransformation>> transformations = customDescription.getTransformations();
        if (transformations == null || InternalTransformation.batchApply(valueFinder2, template, transformations)) {
            OnClickHandler handler = new OnClickHandler() {
                public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent intent) {
                    LogMaker log = SaveUi.this.newLogMaker(1132, type);
                    if (SaveUi.isValidLink(pendingIntent, intent)) {
                        if (Helper.sVerbose) {
                            Slog.v(SaveUi.TAG, "Intercepting custom description intent");
                        }
                        IBinder token = SaveUi.this.mPendingUi.getToken();
                        intent.putExtra("android.view.autofill.extra.RESTORE_SESSION_TOKEN", token);
                        try {
                            SaveUi.this.mPendingUi.client.startIntentSender(pendingIntent.getIntentSender(), intent);
                            SaveUi.this.mPendingUi.setState(2);
                            if (Helper.sDebug) {
                                String str = SaveUi.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("hiding UI until restored with token ");
                                stringBuilder.append(token);
                                Slog.d(str, stringBuilder.toString());
                            }
                            SaveUi.this.hide();
                            log.setType(1);
                            SaveUi.this.mMetricsLogger.write(log);
                            return true;
                        } catch (RemoteException e) {
                            String str2 = SaveUi.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("error triggering pending intent: ");
                            stringBuilder2.append(intent);
                            Slog.w(str2, stringBuilder2.toString());
                            log.setType(11);
                            SaveUi.this.mMetricsLogger.write(log);
                            return false;
                        }
                    }
                    log.setType(0);
                    SaveUi.this.mMetricsLogger.write(log);
                    return false;
                }
            };
            CustomDescription customDescription2;
            int type2;
            try {
                template.setApplyTheme(THEME_ID);
                View customSubtitleView = template.apply(context2, null, handler);
                ArrayList<Pair<InternalValidator, BatchUpdates>> updates = customDescription.getUpdates();
                if (updates != null) {
                    int size = updates.size();
                    if (Helper.sDebug) {
                        try {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("custom description has ");
                            stringBuilder.append(size);
                            stringBuilder.append(" batch updates");
                            Slog.d(str, stringBuilder.toString());
                        } catch (Exception e2) {
                            e = e2;
                        }
                    }
                    int i = 0;
                    while (i < size) {
                        String str2;
                        StringBuilder stringBuilder2;
                        Pair<InternalValidator, BatchUpdates> pair = (Pair) updates.get(i);
                        InternalValidator condition = pair.first;
                        if (condition == null) {
                            customDescription2 = customDescription;
                            type2 = type;
                        } else if (condition.isValid(valueFinder2)) {
                            BatchUpdates batchUpdates = pair.second;
                            RemoteViews templateUpdates = batchUpdates.getUpdates();
                            if (templateUpdates != null) {
                                if (Helper.sDebug) {
                                    str2 = TAG;
                                    customDescription2 = customDescription;
                                    try {
                                        stringBuilder2 = new StringBuilder();
                                        type2 = type;
                                        try {
                                            stringBuilder2.append("Applying template updates for batch update #");
                                            stringBuilder2.append(i);
                                            Slog.d(str2, stringBuilder2.toString());
                                        } catch (Exception e3) {
                                            e = e3;
                                            customDescription = saveUiView;
                                            Slog.e(TAG, "Error applying custom description. ", e);
                                            return false;
                                        }
                                    } catch (Exception e4) {
                                        e = e4;
                                        type2 = type;
                                        View view = saveUiView;
                                        Slog.e(TAG, "Error applying custom description. ", e);
                                        return false;
                                    }
                                }
                                customDescription2 = customDescription;
                                type2 = type;
                                templateUpdates.reapply(context2, customSubtitleView);
                            } else {
                                customDescription2 = customDescription;
                                type2 = type;
                            }
                            ArrayList<Pair<Integer, InternalTransformation>> batchTransformations = batchUpdates.getTransformations();
                            if (batchTransformations == null) {
                                continue;
                            } else {
                                String str3;
                                if (Helper.sDebug) {
                                    str3 = TAG;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Applying child transformation for batch update #");
                                    stringBuilder3.append(i);
                                    stringBuilder3.append(": ");
                                    stringBuilder3.append(batchTransformations);
                                    Slog.d(str3, stringBuilder3.toString());
                                }
                                if (InternalTransformation.batchApply(valueFinder2, template, batchTransformations)) {
                                    template.reapply(context2, customSubtitleView);
                                } else {
                                    str3 = TAG;
                                    StringBuilder stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Could not apply child transformation for batch update #");
                                    stringBuilder4.append(i);
                                    stringBuilder4.append(": ");
                                    stringBuilder4.append(batchTransformations);
                                    Slog.w(str3, stringBuilder4.toString());
                                    return false;
                                }
                            }
                            i++;
                            customDescription = customDescription2;
                            type = type2;
                        } else {
                            customDescription2 = customDescription;
                            type2 = type;
                        }
                        if (Helper.sDebug) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Skipping batch update #");
                            stringBuilder2.append(i);
                            Slog.d(str2, stringBuilder2.toString());
                        }
                        i++;
                        customDescription = customDescription2;
                        type = type2;
                    }
                }
                type2 = type;
                try {
                    ViewGroup subtitleContainer = (ViewGroup) saveUiView.findViewById(16908752);
                    subtitleContainer.addView(customSubtitleView);
                    subtitleContainer.setVisibility(0);
                    return true;
                } catch (Exception e5) {
                    e = e5;
                    Slog.e(TAG, "Error applying custom description. ", e);
                    return false;
                }
            } catch (Exception e6) {
                e = e6;
                customDescription2 = customDescription;
                type2 = type;
                customDescription = saveUiView;
                Slog.e(TAG, "Error applying custom description. ", e);
                return false;
            }
        }
        Slog.w(TAG, "could not apply main transformations on custom description");
        return false;
    }

    private void setServiceIcon(Context context, View view, Drawable serviceIcon) {
        ImageView iconView = (ImageView) view.findViewById(16908753);
        int maxWidth = context.getResources().getDimensionPixelSize(17104939);
        int maxHeight = maxWidth;
        int actualWidth = serviceIcon.getMinimumWidth();
        int actualHeight = serviceIcon.getMinimumHeight();
        String str;
        StringBuilder stringBuilder;
        if (actualWidth > maxWidth || actualHeight > maxHeight) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not adding service icon of size (");
            stringBuilder.append(actualWidth);
            stringBuilder.append("x");
            stringBuilder.append(actualHeight);
            stringBuilder.append(") because maximum is (");
            stringBuilder.append(maxWidth);
            stringBuilder.append("x");
            stringBuilder.append(maxHeight);
            stringBuilder.append(").");
            Slog.w(str, stringBuilder.toString());
            ((ViewGroup) iconView.getParent()).removeView(iconView);
            return;
        }
        if (Helper.sDebug) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Adding service icon (");
            stringBuilder.append(actualWidth);
            stringBuilder.append("x");
            stringBuilder.append(actualHeight);
            stringBuilder.append(") as it's less than maximum (");
            stringBuilder.append(maxWidth);
            stringBuilder.append("x");
            stringBuilder.append(maxHeight);
            stringBuilder.append(").");
            Slog.d(str, stringBuilder.toString());
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
        return Helper.newLogMaker(category, this.mComponentName, this.mServicePackageName, this.mPendingUi.sessionId, this.mCompatMode);
    }

    private void writeLog(int category, int saveType) {
        this.mMetricsLogger.write(newLogMaker(category, saveType));
    }

    void onPendingUi(int operation, IBinder token) {
        if (this.mPendingUi.matches(token)) {
            LogMaker log = newLogMaker(1134);
            String str;
            StringBuilder stringBuilder;
            switch (operation) {
                case 1:
                    log.setType(5);
                    if (Helper.sDebug) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Cancelling pending save dialog for ");
                        stringBuilder.append(token);
                        Slog.d(str, stringBuilder.toString());
                    }
                    hide();
                    break;
                case 2:
                    if (Helper.sDebug) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Restoring save dialog for ");
                        stringBuilder.append(token);
                        Slog.d(str, stringBuilder.toString());
                    }
                    log.setType(1);
                    show();
                    break;
                default:
                    try {
                        log.setType(11);
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("restore(): invalid operation ");
                        stringBuilder.append(operation);
                        Slog.w(str, stringBuilder.toString());
                        break;
                    } catch (Throwable th) {
                        this.mMetricsLogger.write(log);
                    }
            }
            this.mMetricsLogger.write(log);
            this.mPendingUi.setState(4);
            return;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("restore(");
        stringBuilder2.append(operation);
        stringBuilder2.append("): got token ");
        stringBuilder2.append(token);
        stringBuilder2.append(" instead of ");
        stringBuilder2.append(this.mPendingUi.getToken());
        Slog.w(str2, stringBuilder2.toString());
    }

    private void show() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Showing save dialog: ");
        stringBuilder.append(this.mTitle);
        Slog.i(str, stringBuilder.toString());
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
        pw.println(this.mComponentName.toShortString());
        pw.print(prefix);
        pw.print("compat mode: ");
        pw.println(this.mCompatMode);
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
