package com.android.server.autofill.ui;

import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.service.autofill.Dataset;
import android.service.autofill.Dataset.DatasetFieldFilter;
import android.service.autofill.FillResponse;
import android.text.TextUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityManager;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutofillWindowPresenter;
import android.view.autofill.IAutofillWindowPresenter.Stub;
import android.view.autofill.IHwAutofillHelper;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filter.FilterResults;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.RemoteViews.OnClickHandler;
import android.widget.TextView;
import com.android.server.UiThread;
import com.android.server.autofill.Helper;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.WindowManagerService.H;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class FillUi {
    private static final String TAG = "FillUi";
    private static boolean sIsHwAutofillService = false;
    private static final TypedValue sTempTypedValue = new TypedValue();
    private int THEME_ID = 16974777;
    private final ItemsAdapter mAdapter;
    private AnnounceFilterResult mAnnounceFilterResult;
    private final Callback mCallback;
    private int mContentHeight;
    private int mContentWidth;
    private final Context mContext;
    private int mDecorPaddingHeight;
    private int mDecorPaddingWidth;
    private boolean mDestroyed;
    private String mFilterText;
    private final View mFooter;
    private final boolean mFullScreen;
    private final View mHeader;
    private IHwAutofillHelper mHwAutofillHelper = HwFrameworkFactory.getHwAutofillHelper();
    private final ListView mListView;
    private final Point mTempPoint = new Point();
    private final int mVisibleDatasetsMaxCount;
    private final AnchoredWindow mWindow;
    private final AutofillWindowPresenter mWindowPresenter = new AutofillWindowPresenter(this, null);

    final class AnchoredWindow {
        private final View mContentView;
        private final OverlayControl mOverlayControl;
        private LayoutParams mShowParams;
        private boolean mShowing;
        private final WindowManager mWm;

        AnchoredWindow(View contentView, OverlayControl overlayControl) {
            this.mWm = (WindowManager) contentView.getContext().getSystemService(WindowManager.class);
            this.mContentView = contentView;
            this.mOverlayControl = overlayControl;
        }

        public void show(LayoutParams params) {
            String str;
            StringBuilder stringBuilder;
            this.mShowParams = params;
            if (Helper.sVerbose) {
                String str2 = FillUi.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("show(): showing=");
                stringBuilder2.append(this.mShowing);
                stringBuilder2.append(", params=");
                stringBuilder2.append(Helper.paramsToString(params));
                Slog.v(str2, stringBuilder2.toString());
            }
            try {
                params.packageName = PackageManagerService.PLATFORM_PACKAGE_NAME;
                params.setTitle("Autofill UI");
                if (this.mShowing) {
                    this.mWm.updateViewLayout(this.mContentView, params);
                    return;
                }
                params.accessibilityTitle = this.mContentView.getContext().getString(17039659);
                this.mWm.addView(this.mContentView, params);
                this.mOverlayControl.hideOverlays();
                this.mShowing = true;
            } catch (BadTokenException e) {
                if (Helper.sDebug) {
                    str = FillUi.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Filed with with token ");
                    stringBuilder.append(params.token);
                    stringBuilder.append(" gone.");
                    Slog.d(str, stringBuilder.toString());
                }
                FillUi.this.mCallback.onDestroy();
            } catch (IllegalStateException e2) {
                str = FillUi.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception showing window ");
                stringBuilder.append(params);
                Slog.e(str, stringBuilder.toString(), e2);
                FillUi.this.mCallback.onDestroy();
            }
        }

        void hide() {
            hide(true);
        }

        void hide(boolean destroyCallbackOnError) {
            try {
                if (this.mShowing) {
                    this.mWm.removeView(this.mContentView);
                    this.mShowing = false;
                }
            } catch (IllegalStateException e) {
                Slog.e(FillUi.TAG, "Exception hiding window ", e);
                if (destroyCallbackOnError) {
                    FillUi.this.mCallback.onDestroy();
                }
            } catch (Throwable th) {
                this.mOverlayControl.showOverlays();
            }
            this.mOverlayControl.showOverlays();
        }
    }

    private final class AnnounceFilterResult implements Runnable {
        private static final int SEARCH_RESULT_ANNOUNCEMENT_DELAY = 1000;

        private AnnounceFilterResult() {
        }

        /* synthetic */ AnnounceFilterResult(FillUi x0, AnonymousClass1 x1) {
            this();
        }

        public void post() {
            remove();
            FillUi.this.mListView.postDelayed(this, 1000);
        }

        public void remove() {
            FillUi.this.mListView.removeCallbacks(this);
        }

        public void run() {
            String text;
            int count = FillUi.this.mListView.getAdapter().getCount();
            if (count <= 0) {
                text = FillUi.this.mContext.getString(17039660);
            } else {
                text = FillUi.this.mContext.getResources().getQuantityString(18153472, count, new Object[]{Integer.valueOf(count)});
            }
            FillUi.this.mListView.announceForAccessibility(text);
        }
    }

    private final class AutofillWindowPresenter extends Stub {
        private AutofillWindowPresenter() {
        }

        /* synthetic */ AutofillWindowPresenter(FillUi x0, AnonymousClass1 x1) {
            this();
        }

        public void show(LayoutParams p, Rect transitionEpicenter, boolean fitsSystemWindows, int layoutDirection) {
            if (Helper.sVerbose) {
                String str = FillUi.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AutofillWindowPresenter.show(): fit=");
                stringBuilder.append(fitsSystemWindows);
                stringBuilder.append(", params=");
                stringBuilder.append(Helper.paramsToString(p));
                Slog.v(str, stringBuilder.toString());
            }
            if (FillUi.this.mHwAutofillHelper != null && FillUi.this.mHwAutofillHelper.isHwAutofillService(FillUi.this.mContext)) {
                p.width += FillUi.this.mDecorPaddingWidth;
                p.height += FillUi.this.mDecorPaddingHeight;
            }
            UiThread.getHandler().post(new -$$Lambda$FillUi$AutofillWindowPresenter$N4xQe2B0oe5MBiqZlsy3Lb7vZTg(this, p));
        }

        public void hide(Rect transitionEpicenter) {
            Handler handler = UiThread.getHandler();
            AnchoredWindow access$700 = FillUi.this.mWindow;
            Objects.requireNonNull(access$700);
            handler.post(new -$$Lambda$E4J-3bUcyqJNd4ZlExSBhwy8Tx4(access$700));
        }
    }

    interface Callback {
        void dispatchUnhandledKey(KeyEvent keyEvent);

        void onCanceled();

        void onDatasetPicked(Dataset dataset);

        void onDestroy();

        void onResponsePicked(FillResponse fillResponse);

        void requestHideFillUi();

        void requestShowFillUi(int i, int i2, IAutofillWindowPresenter iAutofillWindowPresenter);

        void startIntentSender(IntentSender intentSender);
    }

    private final class ItemsAdapter extends BaseAdapter implements Filterable {
        private final List<ViewItem> mAllItems;
        private final List<ViewItem> mFilteredItems = new ArrayList();

        ItemsAdapter(List<ViewItem> items) {
            this.mAllItems = Collections.unmodifiableList(new ArrayList(items));
            this.mFilteredItems.addAll(items);
        }

        public Filter getFilter() {
            return new Filter() {
                protected FilterResults performFiltering(CharSequence filterText) {
                    List<ViewItem> filtered = (List) ItemsAdapter.this.mAllItems.stream().filter(new -$$Lambda$FillUi$ItemsAdapter$1$8s9zobTvKJVJjInaObtlx2flLMc(filterText)).collect(Collectors.toList());
                    FilterResults results = new FilterResults();
                    results.values = filtered;
                    results.count = filtered.size();
                    return results;
                }

                protected void publishResults(CharSequence constraint, FilterResults results) {
                    int oldItemCount = ItemsAdapter.this.mFilteredItems.size();
                    ItemsAdapter.this.mFilteredItems.clear();
                    if (results.count > 0) {
                        ItemsAdapter.this.mFilteredItems.addAll(results.values);
                    }
                    if (oldItemCount != ItemsAdapter.this.mFilteredItems.size()) {
                        FillUi.this.announceSearchResultIfNeeded();
                    }
                    ItemsAdapter.this.notifyDataSetChanged();
                }
            };
        }

        public int getCount() {
            return this.mFilteredItems.size();
        }

        public ViewItem getItem(int position) {
            return (ViewItem) this.mFilteredItems.get(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).view;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ItemsAdapter: [all=");
            stringBuilder.append(this.mAllItems);
            stringBuilder.append(", filtered=");
            stringBuilder.append(this.mFilteredItems);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    private static class ViewItem {
        public final Dataset dataset;
        public final Pattern filter;
        public final boolean filterable;
        public final String value;
        public final View view;

        ViewItem(Dataset dataset, Pattern filter, boolean filterable, String value, View view) {
            this.dataset = dataset;
            this.value = value;
            this.view = view;
            this.filter = filter;
            this.filterable = filterable;
        }

        public boolean matches(CharSequence filterText) {
            boolean z = true;
            if (TextUtils.isEmpty(filterText)) {
                return true;
            }
            if (!this.filterable) {
                return false;
            }
            String constraintLowerCase = filterText.toString().toLowerCase();
            if (this.filter != null) {
                return this.filter.matcher(constraintLowerCase).matches();
            }
            if (this.value != null) {
                z = FillUi.sIsHwAutofillService ? this.value.toLowerCase().contains(constraintLowerCase) : this.value.toLowerCase().startsWith(constraintLowerCase);
            } else if (this.dataset.getAuthentication() != null) {
                z = false;
            }
            return z;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder("ViewItem:[view=").append(this.view.getAutofillId());
            String datasetId = this.dataset == null ? null : this.dataset.getId();
            if (datasetId != null) {
                builder.append(", dataset=");
                builder.append(datasetId);
            }
            if (this.value != null) {
                builder.append(", value=");
                builder.append(this.value.length());
                builder.append("_chars");
            }
            if (this.filterable) {
                builder.append(", filterable");
            }
            if (this.filter != null) {
                builder.append(", filter=");
                builder.append(this.filter.pattern().length());
                builder.append("_chars");
            }
            builder.append(']');
            return builder.toString();
        }
    }

    public static boolean isFullScreen(Context context) {
        if (Helper.sFullScreenMode == null) {
            return context.getPackageManager().hasSystemFeature("android.software.leanback");
        }
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("forcing full-screen mode to ");
            stringBuilder.append(Helper.sFullScreenMode);
            Slog.v(str, stringBuilder.toString());
        }
        return Helper.sFullScreenMode.booleanValue();
    }

    FillUi(Context context, FillResponse response, AutofillId focusedViewId, String filterText, OverlayControl overlayControl, CharSequence serviceLabel, Drawable serviceIcon, Callback callback) {
        ViewGroup decor;
        Point outPoint;
        String str;
        OnClickHandler onClickHandler;
        RemoteViews remoteViews;
        RemoteViews remoteViews2;
        TextView textView;
        RuntimeException e;
        int datasetCount;
        RemoteViews remoteViews3;
        Callback callback2;
        FillResponse fillResponse;
        AutofillId autofillId = focusedViewId;
        OverlayControl overlayControl2 = overlayControl;
        this.mCallback = callback;
        this.mFullScreen = isFullScreen(context);
        this.mContext = new ContextThemeWrapper(context, this.THEME_ID);
        this.THEME_ID = this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null);
        this.mContext.setTheme(this.THEME_ID);
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        RemoteViews headerPresentation = response.getHeader();
        RemoteViews footerPresentation = response.getFooter();
        if (this.mFullScreen) {
            decor = (ViewGroup) inflater.inflate(17367101, null);
        } else if (headerPresentation == null && footerPresentation == null) {
            decor = (ViewGroup) inflater.inflate(17367100, null);
        } else {
            decor = (ViewGroup) inflater.inflate(17367102, null);
        }
        ViewGroup decor2 = decor;
        TextView titleView = (TextView) decor2.findViewById(16908750);
        if (titleView != null) {
            titleView.setText(this.mContext.getString(17039682, new Object[]{serviceLabel}));
        }
        ImageView iconView = (ImageView) decor2.findViewById(16908747);
        if (iconView != null) {
            iconView.setImageDrawable(serviceIcon);
        } else {
            Drawable drawable = serviceIcon;
        }
        if (this.mFullScreen) {
            outPoint = this.mTempPoint;
            this.mContext.getDisplay().getSize(outPoint);
            this.mContentWidth = -1;
            this.mContentHeight = outPoint.y / 2;
            if (Helper.sVerbose) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("initialized fillscreen LayoutParams ");
                stringBuilder.append(this.mContentWidth);
                stringBuilder.append(",");
                stringBuilder.append(this.mContentHeight);
                Slog.v(str2, stringBuilder.toString());
            }
        }
        decor2.addOnUnhandledKeyEventListener(new -$$Lambda$FillUi$FY016gv4LQ5AA6yOkKTH3EM5zaM(this));
        if (Helper.sVisibleDatasetsMaxCount > 0) {
            this.mVisibleDatasetsMaxCount = Helper.sVisibleDatasetsMaxCount;
            if (Helper.sVerbose) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("overriding maximum visible datasets to ");
                stringBuilder2.append(this.mVisibleDatasetsMaxCount);
                Slog.v(str, stringBuilder2.toString());
            }
        } else {
            this.mVisibleDatasetsMaxCount = this.mContext.getResources().getInteger(17694724);
        }
        OnClickHandler interceptionHandler = new OnClickHandler() {
            public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
                if (pendingIntent != null) {
                    FillUi.this.mCallback.startIntentSender(pendingIntent.getIntentSender());
                }
                return true;
            }
        };
        if (response.getAuthentication() != null) {
            this.mHeader = null;
            this.mListView = null;
            this.mFooter = null;
            this.mAdapter = null;
            ViewGroup container = (ViewGroup) decor2.findViewById(16908749);
            ViewGroup viewGroup;
            try {
                response.getPresentation().setApplyTheme(this.THEME_ID);
                View content = response.getPresentation().apply(this.mContext, decor2, interceptionHandler);
                container.addView(content);
                container.setFocusable(true);
                container.setOnClickListener(new -$$Lambda$FillUi$Vch3vycmBdfZEFnJyIbU-GchQzA(this, response));
                if (this.mFullScreen) {
                    viewGroup = container;
                } else {
                    outPoint = this.mTempPoint;
                    resolveMaxWindowSize(this.mContext, outPoint);
                    ViewGroup.LayoutParams layoutParams = content.getLayoutParams();
                    if (this.mFullScreen != null) {
                        titleView = outPoint.x;
                    } else {
                        titleView = -2;
                    }
                    layoutParams.width = titleView;
                    content.getLayoutParams().height = -2;
                    decor2.measure(MeasureSpec.makeMeasureSpec(outPoint.x, Integer.MIN_VALUE), MeasureSpec.makeMeasureSpec(outPoint.y, Integer.MIN_VALUE));
                    this.mContentWidth = content.getMeasuredWidth();
                    this.mContentHeight = content.getMeasuredHeight();
                }
                this.mWindow = new AnchoredWindow(decor2, overlayControl2);
                requestShowFillUi();
                onClickHandler = interceptionHandler;
                remoteViews = headerPresentation;
                remoteViews2 = footerPresentation;
            } catch (RuntimeException e2) {
                LayoutInflater layoutInflater = inflater;
                textView = titleView;
                viewGroup = container;
                inflater = response;
                callback.onCanceled();
                Slog.e(TAG, "Error inflating remote views", e2);
                this.mWindow = null;
                return;
            }
        }
        LinearLayout headerContainer;
        textView = titleView;
        inflater = response;
        int datasetCount2 = response.getDatasets().size();
        if (Helper.sVerbose) {
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Number datasets: ");
            stringBuilder3.append(datasetCount2);
            stringBuilder3.append(" max visible: ");
            stringBuilder3.append(this.mVisibleDatasetsMaxCount);
            Slog.v(str, stringBuilder3.toString());
        }
        OnClickHandler clickBlocker = null;
        if (headerPresentation != null) {
            clickBlocker = newClickBlocker();
            headerPresentation.setApplyTheme(this.THEME_ID);
            this.mHeader = headerPresentation.apply(this.mContext, null, clickBlocker);
            headerContainer = (LinearLayout) decor2.findViewById(16908746);
            if (Helper.sVerbose) {
                Slog.v(TAG, "adding header");
            }
            headerContainer.addView(this.mHeader);
            headerContainer.setVisibility(0);
        } else {
            this.mHeader = null;
        }
        if (footerPresentation != null) {
            headerContainer = (LinearLayout) decor2.findViewById(16908745);
            if (headerContainer != null) {
                if (clickBlocker == null) {
                    clickBlocker = newClickBlocker();
                }
                footerPresentation.setApplyTheme(this.THEME_ID);
                this.mFooter = footerPresentation.apply(this.mContext, null, clickBlocker);
                if (Helper.sVerbose) {
                    Slog.v(TAG, "adding footer");
                }
                headerContainer.addView(this.mFooter);
                headerContainer.setVisibility(0);
            } else {
                this.mFooter = null;
            }
        } else {
            this.mFooter = null;
        }
        boolean z = this.mHwAutofillHelper != null && this.mHwAutofillHelper.isHwAutofillService(this.mContext);
        sIsHwAutofillService = z;
        ArrayList<ViewItem> items = new ArrayList(datasetCount2);
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= datasetCount2) {
                break;
            }
            Dataset dataset = (Dataset) response.getDatasets().get(i2);
            int index = dataset.getFieldIds().indexOf(autofillId);
            if (index >= 0) {
                datasetCount = datasetCount2;
                RemoteViews presentation = dataset.getFieldPresentation(index);
                StringBuilder stringBuilder4;
                if (presentation == null) {
                    str = TAG;
                    remoteViews = headerPresentation;
                    stringBuilder4 = new StringBuilder();
                    remoteViews2 = footerPresentation;
                    stringBuilder4.append("not displaying UI on field ");
                    stringBuilder4.append(autofillId);
                    stringBuilder4.append(" because service didn't provide a presentation for it on ");
                    stringBuilder4.append(dataset);
                    Slog.w(str, stringBuilder4.toString());
                    onClickHandler = interceptionHandler;
                } else {
                    remoteViews = headerPresentation;
                    remoteViews2 = footerPresentation;
                    try {
                        if (Helper.sVerbose) {
                            try {
                                str = TAG;
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("setting remote view for ");
                                stringBuilder4.append(autofillId);
                                Slog.v(str, stringBuilder4.toString());
                            } catch (RuntimeException e3) {
                                e2 = e3;
                                onClickHandler = interceptionHandler;
                                remoteViews3 = presentation;
                            }
                        }
                        presentation.setApplyTheme(this.THEME_ID);
                        View view = presentation.apply(this.mContext, null, interceptionHandler);
                        DatasetFieldFilter filter = dataset.getFilter(index);
                        footerPresentation = null;
                        boolean filterable = true;
                        if (filter == null) {
                            onClickHandler = interceptionHandler;
                            AutofillValue interceptionHandler2 = (AutofillValue) dataset.getFieldValues().get(index);
                            if (interceptionHandler2 == null || !interceptionHandler2.isText()) {
                            } else {
                                footerPresentation = interceptionHandler2.getTextValue().toString().toLowerCase();
                            }
                            DatasetFieldFilter datasetFieldFilter = filter;
                            interceptionHandler = null;
                        } else {
                            onClickHandler = interceptionHandler;
                            remoteViews3 = presentation;
                            interceptionHandler = filter.pattern;
                            if (interceptionHandler == null) {
                                if (Helper.sVerbose) {
                                    String str3 = TAG;
                                    headerPresentation = new StringBuilder();
                                    headerPresentation.append("Explicitly disabling filter at id ");
                                    headerPresentation.append(autofillId);
                                    headerPresentation.append(" for dataset #");
                                    headerPresentation.append(index);
                                    Slog.v(str3, headerPresentation.toString());
                                }
                                filterable = null;
                            }
                        }
                        items.add(new ViewItem(dataset, interceptionHandler, filterable, footerPresentation, view));
                    } catch (RuntimeException e4) {
                        e2 = e4;
                        onClickHandler = interceptionHandler;
                        remoteViews3 = presentation;
                        Slog.e(TAG, "Error inflating remote views", e2);
                        i = i2 + 1;
                        datasetCount2 = datasetCount;
                        headerPresentation = remoteViews;
                        footerPresentation = remoteViews2;
                        interceptionHandler = onClickHandler;
                        callback2 = callback;
                        fillResponse = response;
                    }
                }
            } else {
                onClickHandler = interceptionHandler;
                datasetCount = datasetCount2;
                remoteViews = headerPresentation;
                remoteViews2 = footerPresentation;
            }
            i = i2 + 1;
            datasetCount2 = datasetCount;
            headerPresentation = remoteViews;
            footerPresentation = remoteViews2;
            interceptionHandler = onClickHandler;
            callback2 = callback;
            fillResponse = response;
        }
        datasetCount = datasetCount2;
        remoteViews = headerPresentation;
        remoteViews2 = footerPresentation;
        this.mAdapter = new ItemsAdapter(items);
        this.mListView = (ListView) decor2.findViewById(16908748);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setVisibility(0);
        this.mListView.setOnItemClickListener(new -$$Lambda$FillUi$jpLZ4TKMFTozyqA8_WsBHG3lWBg(this));
        if (this.mHwAutofillHelper != null && this.mHwAutofillHelper.isHwAutofillService(this.mContext)) {
            FrameLayout layout = (FrameLayout) decor2.findViewById(16908749);
            layout.setBackgroundColor(0);
            layout.setBackgroundResource(33751603);
            this.mListView.setDivider(null);
            this.mDecorPaddingWidth = layout.getPaddingLeft() + layout.getPaddingRight();
            this.mDecorPaddingHeight = layout.getPaddingTop() + layout.getPaddingBottom();
        }
        if (filterText == null) {
            this.mFilterText = null;
        } else {
            this.mFilterText = filterText.toLowerCase();
        }
        applyNewFilterText();
        this.mWindow = new AnchoredWindow(decor2, overlayControl2);
    }

    public static /* synthetic */ boolean lambda$new$0(FillUi fillUi, View view, KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (!(keyCode == 4 || keyCode == 66 || keyCode == 111)) {
            switch (keyCode) {
                case H.REPORT_WINDOWS_CHANGE /*19*/:
                case 20:
                case BackupHandler.MSG_OP_COMPLETE /*21*/:
                case H.REPORT_HARD_KEYBOARD_STATUS_CHANGE /*22*/:
                case H.BOOT_TIMEOUT /*23*/:
                    break;
                default:
                    fillUi.mCallback.dispatchUnhandledKey(event);
                    return true;
            }
        }
        return false;
    }

    void requestShowFillUi() {
        this.mCallback.requestShowFillUi(this.mContentWidth, this.mContentHeight, this.mWindowPresenter);
    }

    private OnClickHandler newClickBlocker() {
        return new OnClickHandler() {
            public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
                if (Helper.sVerbose) {
                    String str = FillUi.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Ignoring click on ");
                    stringBuilder.append(view);
                    Slog.v(str, stringBuilder.toString());
                }
                return true;
            }
        };
    }

    private void applyNewFilterText() {
        this.mAdapter.getFilter().filter(this.mFilterText, new -$$Lambda$FillUi$IWXsdxX6-XHI4qKxpJLqtc98gZA(this, this.mAdapter.getCount()));
    }

    public static /* synthetic */ void lambda$applyNewFilterText$3(FillUi fillUi, int oldCount, int count) {
        if (!fillUi.mDestroyed) {
            int size = 0;
            if (count <= 0) {
                if (Helper.sDebug) {
                    if (fillUi.mFilterText != null) {
                        size = fillUi.mFilterText.length();
                    }
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No dataset matches filter with ");
                    stringBuilder.append(size);
                    stringBuilder.append(" chars");
                    Slog.d(str, stringBuilder.toString());
                }
                fillUi.mCallback.requestHideFillUi();
            } else {
                if (fillUi.updateContentSize()) {
                    fillUi.requestShowFillUi();
                }
                if (fillUi.mAdapter.getCount() > fillUi.mVisibleDatasetsMaxCount) {
                    fillUi.mListView.setVerticalScrollBarEnabled(true);
                    fillUi.mListView.onVisibilityAggregated(true);
                } else {
                    fillUi.mListView.setVerticalScrollBarEnabled(false);
                }
                if (fillUi.mAdapter.getCount() != oldCount) {
                    fillUi.mListView.requestLayout();
                }
            }
        }
    }

    public void setFilterText(String filterText) {
        throwIfDestroyed();
        if (this.mAdapter == null) {
            if (TextUtils.isEmpty(filterText)) {
                requestShowFillUi();
            } else {
                this.mCallback.requestHideFillUi();
            }
            return;
        }
        if (filterText == null) {
            filterText = null;
        } else {
            filterText = filterText.toLowerCase();
        }
        if (!Objects.equals(this.mFilterText, filterText)) {
            this.mFilterText = filterText;
            applyNewFilterText();
        }
    }

    public void destroy(boolean notifyClient) {
        throwIfDestroyed();
        if (this.mWindow != null) {
            this.mWindow.hide(false);
        }
        this.mCallback.onDestroy();
        if (notifyClient) {
            this.mCallback.requestHideFillUi();
        }
        this.mDestroyed = true;
    }

    private boolean updateContentSize() {
        int i = 0;
        if (this.mAdapter == null) {
            return false;
        }
        if (this.mFullScreen) {
            return true;
        }
        boolean changed = false;
        if (this.mAdapter.getCount() <= 0) {
            if (this.mContentWidth != 0) {
                this.mContentWidth = 0;
                changed = true;
            }
            if (this.mContentHeight != 0) {
                this.mContentHeight = 0;
                changed = true;
            }
            return changed;
        }
        Point maxSize = this.mTempPoint;
        resolveMaxWindowSize(this.mContext, maxSize);
        this.mContentWidth = 0;
        this.mContentHeight = 0;
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxSize.x, Integer.MIN_VALUE);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxSize.y, Integer.MIN_VALUE);
        int itemCount = this.mAdapter.getCount();
        if (this.mHeader != null) {
            this.mHeader.measure(widthMeasureSpec, heightMeasureSpec);
            changed = (false | updateWidth(this.mHeader, maxSize)) | updateHeight(this.mHeader, maxSize);
        }
        while (i < itemCount) {
            View view = this.mAdapter.getItem(i).view;
            view.measure(widthMeasureSpec, heightMeasureSpec);
            changed |= updateWidth(view, maxSize);
            if (i < this.mVisibleDatasetsMaxCount) {
                changed |= updateHeight(view, maxSize);
            }
            i++;
        }
        if (this.mFooter != null) {
            this.mFooter.measure(widthMeasureSpec, heightMeasureSpec);
            changed = (changed | updateWidth(this.mFooter, maxSize)) | updateHeight(this.mFooter, maxSize);
        }
        return changed;
    }

    private boolean updateWidth(View view, Point maxSize) {
        int newContentWidth = Math.max(this.mContentWidth, Math.min(view.getMeasuredWidth(), maxSize.x));
        if (newContentWidth == this.mContentWidth) {
            return false;
        }
        this.mContentWidth = newContentWidth;
        return true;
    }

    private boolean updateHeight(View view, Point maxSize) {
        int newContentHeight = this.mContentHeight + Math.min(view.getMeasuredHeight(), maxSize.y);
        if (newContentHeight == this.mContentHeight) {
            return false;
        }
        this.mContentHeight = newContentHeight;
        return true;
    }

    private void throwIfDestroyed() {
        if (this.mDestroyed) {
            throw new IllegalStateException("cannot interact with a destroyed instance");
        }
    }

    private static void resolveMaxWindowSize(Context context, Point outPoint) {
        context.getDisplay().getSize(outPoint);
        TypedValue typedValue = sTempTypedValue;
        context.getTheme().resolveAttribute(17891345, typedValue, true);
        outPoint.x = (int) typedValue.getFraction((float) outPoint.x, (float) outPoint.x);
        context.getTheme().resolveAttribute(17891344, typedValue, true);
        outPoint.y = (int) typedValue.getFraction((float) outPoint.y, (float) outPoint.y);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mCallback: ");
        pw.println(this.mCallback != null);
        pw.print(prefix);
        pw.print("mFullScreen: ");
        pw.println(this.mFullScreen);
        pw.print(prefix);
        pw.print("mVisibleDatasetsMaxCount: ");
        pw.println(this.mVisibleDatasetsMaxCount);
        if (this.mHeader != null) {
            pw.print(prefix);
            pw.print("mHeader: ");
            pw.println(this.mHeader);
        }
        if (this.mListView != null) {
            pw.print(prefix);
            pw.print("mListView: ");
            pw.println(this.mListView);
        }
        if (this.mFooter != null) {
            pw.print(prefix);
            pw.print("mFooter: ");
            pw.println(this.mFooter);
        }
        if (this.mAdapter != null) {
            pw.print(prefix);
            pw.print("mAdapter: ");
            pw.println(this.mAdapter);
        }
        if (this.mFilterText != null) {
            pw.print(prefix);
            pw.print("mFilterText: ");
            Helper.printlnRedactedText(pw, this.mFilterText);
        }
        pw.print(prefix);
        pw.print("mContentWidth: ");
        pw.println(this.mContentWidth);
        pw.print(prefix);
        pw.print("mContentHeight: ");
        pw.println(this.mContentHeight);
        pw.print(prefix);
        pw.print("mDestroyed: ");
        pw.println(this.mDestroyed);
        if (this.mWindow != null) {
            pw.print(prefix);
            pw.print("mWindow: ");
            String prefix2 = new StringBuilder();
            prefix2.append(prefix);
            prefix2.append("  ");
            prefix2 = prefix2.toString();
            pw.println();
            pw.print(prefix2);
            pw.print("showing: ");
            pw.println(this.mWindow.mShowing);
            pw.print(prefix2);
            pw.print("view: ");
            pw.println(this.mWindow.mContentView);
            if (this.mWindow.mShowParams != null) {
                pw.print(prefix2);
                pw.print("params: ");
                pw.println(this.mWindow.mShowParams);
            }
            pw.print(prefix2);
            pw.print("screen coordinates: ");
            if (this.mWindow.mContentView == null) {
                pw.println("N/A");
                return;
            }
            int[] coordinates = this.mWindow.mContentView.getLocationOnScreen();
            pw.print(coordinates[0]);
            pw.print("x");
            pw.println(coordinates[1]);
        }
    }

    private void announceSearchResultIfNeeded() {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            if (this.mAnnounceFilterResult == null) {
                this.mAnnounceFilterResult = new AnnounceFilterResult(this, null);
            }
            this.mAnnounceFilterResult.post();
        }
    }
}
