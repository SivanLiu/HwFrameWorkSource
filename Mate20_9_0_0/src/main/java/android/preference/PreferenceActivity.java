package android.preference;

import android.animation.LayoutTransition;
import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.opengl.GLES31;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.preference.PreferenceFragment.OnPreferenceStartFragmentCallback;
import android.preference.PreferenceManager.OnPreferenceTreeClickListener;
import android.provider.Downloads.Impl.RequestHeaders;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public abstract class PreferenceActivity extends ListActivity implements OnPreferenceTreeClickListener, OnPreferenceStartFragmentCallback {
    private static final String BACK_STACK_PREFS = ":android:prefs";
    private static final String CUR_HEADER_TAG = ":android:cur_header";
    public static final String EXTRA_NO_HEADERS = ":android:no_headers";
    private static final String EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text";
    private static final String EXTRA_PREFS_SET_NEXT_TEXT = "extra_prefs_set_next_text";
    private static final String EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";
    private static final String EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip";
    public static final String EXTRA_SHOW_FRAGMENT = ":android:show_fragment";
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":android:show_fragment_args";
    public static final String EXTRA_SHOW_FRAGMENT_SHORT_TITLE = ":android:show_fragment_short_title";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":android:show_fragment_title";
    private static final int FIRST_REQUEST_CODE = 100;
    private static final String HEADERS_TAG = ":android:headers";
    public static final long HEADER_ID_UNDEFINED = -1;
    private static final int MSG_BIND_PREFERENCES = 1;
    private static final int MSG_BUILD_HEADERS = 2;
    private static final String PREFERENCES_TAG = ":android:preferences";
    private static final String TAG = "PreferenceActivity";
    private CharSequence mActivityTitle;
    private Header mCurHeader;
    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PreferenceActivity.this.bindPreferences();
                    return;
                case 2:
                    ArrayList<Header> oldHeaders = new ArrayList(PreferenceActivity.this.mHeaders);
                    PreferenceActivity.this.mHeaders.clear();
                    PreferenceActivity.this.onBuildHeaders(PreferenceActivity.this.mHeaders);
                    if (PreferenceActivity.this.mAdapter instanceof BaseAdapter) {
                        ((BaseAdapter) PreferenceActivity.this.mAdapter).notifyDataSetChanged();
                    }
                    Header header = PreferenceActivity.this.onGetNewHeader();
                    Header mappedHeader;
                    if (header != null && header.fragment != null) {
                        mappedHeader = PreferenceActivity.this.findBestMatchingHeader(header, oldHeaders);
                        if (mappedHeader == null || PreferenceActivity.this.mCurHeader != mappedHeader) {
                            PreferenceActivity.this.switchToHeader(header);
                            return;
                        }
                        return;
                    } else if (PreferenceActivity.this.mCurHeader != null) {
                        mappedHeader = PreferenceActivity.this.findBestMatchingHeader(PreferenceActivity.this.mCurHeader, PreferenceActivity.this.mHeaders);
                        if (mappedHeader != null) {
                            PreferenceActivity.this.setSelectedHeader(mappedHeader);
                            return;
                        }
                        return;
                    } else {
                        return;
                    }
                default:
                    return;
            }
        }
    };
    private final ArrayList<Header> mHeaders = new ArrayList();
    private ViewGroup mHeadersContainer;
    private FrameLayout mListFooter;
    private Button mNextButton;
    private int mPreferenceHeaderItemResId = 0;
    private boolean mPreferenceHeaderRemoveEmptyIcon = false;
    private PreferenceManager mPreferenceManager;
    private ViewGroup mPrefsContainer;
    private Bundle mSavedInstanceState;
    private boolean mSinglePane;

    public static final class Header implements Parcelable {
        public static final Creator<Header> CREATOR = new Creator<Header>() {
            public Header createFromParcel(Parcel source) {
                return new Header(source);
            }

            public Header[] newArray(int size) {
                return new Header[size];
            }
        };
        public CharSequence breadCrumbShortTitle;
        public int breadCrumbShortTitleRes;
        public CharSequence breadCrumbTitle;
        public int breadCrumbTitleRes;
        public Bundle extras;
        public String fragment;
        public Bundle fragmentArguments;
        public int iconRes;
        public long id = -1;
        public Intent intent;
        public CharSequence summary;
        public int summaryRes;
        public CharSequence title;
        public int titleRes;

        public CharSequence getTitle(Resources res) {
            if (this.titleRes != 0) {
                return res.getText(this.titleRes);
            }
            return this.title;
        }

        public CharSequence getSummary(Resources res) {
            if (this.summaryRes != 0) {
                return res.getText(this.summaryRes);
            }
            return this.summary;
        }

        public CharSequence getBreadCrumbTitle(Resources res) {
            if (this.breadCrumbTitleRes != 0) {
                return res.getText(this.breadCrumbTitleRes);
            }
            return this.breadCrumbTitle;
        }

        public CharSequence getBreadCrumbShortTitle(Resources res) {
            if (this.breadCrumbShortTitleRes != 0) {
                return res.getText(this.breadCrumbShortTitleRes);
            }
            return this.breadCrumbShortTitle;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.id);
            dest.writeInt(this.titleRes);
            TextUtils.writeToParcel(this.title, dest, flags);
            dest.writeInt(this.summaryRes);
            TextUtils.writeToParcel(this.summary, dest, flags);
            dest.writeInt(this.breadCrumbTitleRes);
            TextUtils.writeToParcel(this.breadCrumbTitle, dest, flags);
            dest.writeInt(this.breadCrumbShortTitleRes);
            TextUtils.writeToParcel(this.breadCrumbShortTitle, dest, flags);
            dest.writeInt(this.iconRes);
            dest.writeString(this.fragment);
            dest.writeBundle(this.fragmentArguments);
            if (this.intent != null) {
                dest.writeInt(1);
                this.intent.writeToParcel(dest, flags);
            } else {
                dest.writeInt(0);
            }
            dest.writeBundle(this.extras);
        }

        public void readFromParcel(Parcel in) {
            this.id = in.readLong();
            this.titleRes = in.readInt();
            this.title = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.summaryRes = in.readInt();
            this.summary = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.breadCrumbTitleRes = in.readInt();
            this.breadCrumbTitle = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.breadCrumbShortTitleRes = in.readInt();
            this.breadCrumbShortTitle = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.iconRes = in.readInt();
            this.fragment = in.readString();
            this.fragmentArguments = in.readBundle();
            if (in.readInt() != 0) {
                this.intent = (Intent) Intent.CREATOR.createFromParcel(in);
            }
            this.extras = in.readBundle();
        }

        Header(Parcel in) {
            readFromParcel(in);
        }
    }

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        private LayoutInflater mInflater;
        private int mLayoutResId;
        private boolean mRemoveIconIfEmpty;

        private static class HeaderViewHolder {
            ImageView icon;
            TextView summary;
            TextView title;

            private HeaderViewHolder() {
            }

            /* synthetic */ HeaderViewHolder(AnonymousClass1 x0) {
                this();
            }
        }

        public HeaderAdapter(Context context, List<Header> objects, int layoutResId, boolean removeIconBehavior) {
            super(context, 0, (List) objects);
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mLayoutResId = layoutResId;
            this.mRemoveIconIfEmpty = removeIconBehavior;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            HeaderViewHolder holder;
            if (convertView == null) {
                view = this.mInflater.inflate(this.mLayoutResId, parent, false);
                holder = new HeaderViewHolder();
                holder.icon = (ImageView) view.findViewById(16908294);
                holder.title = (TextView) view.findViewById(16908310);
                holder.summary = (TextView) view.findViewById(16908304);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }
            Header header = (Header) getItem(position);
            if (!this.mRemoveIconIfEmpty) {
                holder.icon.setImageResource(header.iconRes);
            } else if (header.iconRes == 0) {
                holder.icon.setVisibility(8);
            } else {
                holder.icon.setVisibility(0);
                holder.icon.setImageResource(header.iconRes);
            }
            holder.title.setText(header.getTitle(getContext().getResources()));
            CharSequence summary = header.getSummary(getContext().getResources());
            if (TextUtils.isEmpty(summary)) {
                holder.summary.setVisibility(8);
            } else {
                holder.summary.setVisibility(0);
                holder.summary.setText(summary);
            }
            return view;
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        onBackPressed();
        return true;
    }

    protected void onCreate(Bundle savedInstanceState) {
        Bundle bundle = savedInstanceState;
        super.onCreate(savedInstanceState);
        TypedArray sa = obtainStyledAttributes(null, R.styleable.PreferenceActivity, 17891489, 0);
        int layoutResId = sa.getResourceId(0, 17367233);
        this.mPreferenceHeaderItemResId = sa.getResourceId(1, 17367227);
        this.mPreferenceHeaderRemoveEmptyIcon = sa.getBoolean(2, false);
        sa.recycle();
        setContentView(layoutResId);
        this.mListFooter = (FrameLayout) findViewById(16909047);
        this.mPrefsContainer = (ViewGroup) findViewById(16909214);
        this.mHeadersContainer = (ViewGroup) findViewById(16908957);
        boolean z = onIsHidingHeaders() || !onIsMultiPane();
        this.mSinglePane = z;
        String initialFragment = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        Bundle initialArguments = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        int initialTitle = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
        int initialShortTitle = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_SHORT_TITLE, 0);
        this.mActivityTitle = getTitle();
        if (bundle != null) {
            ArrayList<Header> headers = bundle.getParcelableArrayList(HEADERS_TAG);
            if (headers != null) {
                this.mHeaders.addAll(headers);
                int curHeader = bundle.getInt(CUR_HEADER_TAG, -1);
                if (curHeader >= 0 && curHeader < this.mHeaders.size()) {
                    setSelectedHeader((Header) this.mHeaders.get(curHeader));
                } else if (!this.mSinglePane && initialFragment == null) {
                    switchToHeader(onGetInitialHeader());
                }
            } else {
                showBreadCrumbs(getTitle(), null);
            }
        } else {
            if (!onIsHidingHeaders()) {
                onBuildHeaders(this.mHeaders);
            }
            if (initialFragment != null) {
                switchToHeader(initialFragment, initialArguments);
            } else if (!this.mSinglePane && this.mHeaders.size() > 0) {
                switchToHeader(onGetInitialHeader());
            }
        }
        if (this.mHeaders.size() > 0) {
            setListAdapter(new HeaderAdapter(this, this.mHeaders, this.mPreferenceHeaderItemResId, this.mPreferenceHeaderRemoveEmptyIcon));
            if (!this.mSinglePane) {
                getListView().setChoiceMode(1);
            }
        }
        if (!(!this.mSinglePane || initialFragment == null || initialTitle == 0)) {
            showBreadCrumbs(getText(initialTitle), initialShortTitle != 0 ? getText(initialShortTitle) : null);
        }
        if (this.mHeaders.size() == 0 && initialFragment == null) {
            setContentView(17367235);
            this.mListFooter = (FrameLayout) findViewById(16909047);
            this.mPrefsContainer = (ViewGroup) findViewById(16909212);
            this.mPreferenceManager = new PreferenceManager(this, 100);
            this.mPreferenceManager.setOnPreferenceTreeClickListener(this);
            this.mHeadersContainer = null;
        } else if (this.mSinglePane) {
            if (initialFragment == null && this.mCurHeader == null) {
                this.mPrefsContainer.setVisibility(8);
            } else {
                this.mHeadersContainer.setVisibility(8);
            }
            ((ViewGroup) findViewById(16909213)).setLayoutTransition(new LayoutTransition());
        } else if (this.mHeaders.size() > 0 && this.mCurHeader != null) {
            setSelectedHeader(this.mCurHeader);
        }
        Intent intent = getIntent();
        if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)) {
            String buttonText;
            findViewById(16945153).setVisibility(0);
            Button backButton = (Button) findViewById(16908757);
            backButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    PreferenceActivity.this.setResult(0);
                    PreferenceActivity.this.finish();
                }
            });
            Button skipButton = (Button) findViewById(16909335);
            skipButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    PreferenceActivity.this.setResult(-1);
                    PreferenceActivity.this.finish();
                }
            });
            this.mNextButton = (Button) findViewById(16909117);
            this.mNextButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    PreferenceActivity.this.setResult(-1);
                    PreferenceActivity.this.finish();
                }
            });
            if (intent.hasExtra(EXTRA_PREFS_SET_NEXT_TEXT)) {
                buttonText = intent.getStringExtra(EXTRA_PREFS_SET_NEXT_TEXT);
                if (TextUtils.isEmpty(buttonText)) {
                    this.mNextButton.setVisibility(8);
                } else {
                    this.mNextButton.setText((CharSequence) buttonText);
                }
            }
            if (intent.hasExtra(EXTRA_PREFS_SET_BACK_TEXT)) {
                buttonText = intent.getStringExtra(EXTRA_PREFS_SET_BACK_TEXT);
                if (TextUtils.isEmpty(buttonText)) {
                    backButton.setVisibility(8);
                } else {
                    backButton.setText((CharSequence) buttonText);
                }
            }
            if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_SKIP, false)) {
                skipButton.setVisibility(0);
            }
        }
    }

    public void onBackPressed() {
        if (this.mCurHeader != null && this.mSinglePane && getFragmentManager().getBackStackEntryCount() == 0 && getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT) == null) {
            this.mCurHeader = null;
            this.mPrefsContainer.setVisibility(8);
            this.mHeadersContainer.setVisibility(0);
            if (this.mActivityTitle != null) {
                showBreadCrumbs(this.mActivityTitle, null);
            }
            getListView().clearChoices();
            return;
        }
        super.onBackPressed();
    }

    public boolean hasHeaders() {
        return this.mHeadersContainer != null && this.mHeadersContainer.getVisibility() == 0;
    }

    public List<Header> getHeaders() {
        return this.mHeaders;
    }

    public boolean isMultiPane() {
        return this.mSinglePane ^ 1;
    }

    public boolean onIsMultiPane() {
        return getResources().getBoolean(17957107);
    }

    public boolean onIsHidingHeaders() {
        return getIntent().getBooleanExtra(EXTRA_NO_HEADERS, false);
    }

    public Header onGetInitialHeader() {
        for (int i = 0; i < this.mHeaders.size(); i++) {
            Header h = (Header) this.mHeaders.get(i);
            if (h.fragment != null) {
                return h;
            }
        }
        throw new IllegalStateException("Must have at least one header with a fragment");
    }

    public Header onGetNewHeader() {
        return null;
    }

    public void onBuildHeaders(List<Header> list) {
    }

    public void invalidateHeaders() {
        if (!this.mHandler.hasMessages(2)) {
            this.mHandler.sendEmptyMessage(2);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:128:0x01bf  */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x01bf  */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x01bf  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void loadHeadersFromResource(int resid, List<Header> target) {
        XmlPullParserException e;
        List<Header> list;
        IOException e2;
        Throwable th;
        int i;
        XmlResourceParser parser = null;
        try {
            try {
                int type;
                int i2;
                int i3;
                String nodeName;
                parser = getResources().getXml(resid);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                while (true) {
                    int next = parser.next();
                    type = next;
                    i2 = 2;
                    i3 = 1;
                    if (next == 1 || type == 2) {
                        nodeName = parser.getName();
                    }
                }
                nodeName = parser.getName();
                if ("preference-headers".equals(nodeName)) {
                    Bundle curBundle = null;
                    int outerDepth = parser.getDepth();
                    while (true) {
                        int next2 = parser.next();
                        type = next2;
                        if (next2 == i3) {
                            break;
                        }
                        next2 = 3;
                        if (type == 3 && parser.getDepth() <= outerDepth) {
                            break;
                        }
                        if (type != 3) {
                            if (type != 4) {
                                if (RequestHeaders.COLUMN_HEADER.equals(parser.getName())) {
                                    Header header = new Header();
                                    try {
                                        TypedArray sa = obtainStyledAttributes(attrs, R.styleable.PreferenceHeader);
                                        header.id = (long) sa.getResourceId(i3, -1);
                                        TypedValue tv = sa.peekValue(i2);
                                        if (tv != null && tv.type == 3) {
                                            if (tv.resourceId != 0) {
                                                header.titleRes = tv.resourceId;
                                            } else {
                                                header.title = tv.string;
                                            }
                                        }
                                        tv = sa.peekValue(3);
                                        if (tv != null && tv.type == 3) {
                                            if (tv.resourceId != 0) {
                                                header.summaryRes = tv.resourceId;
                                            } else {
                                                header.summary = tv.string;
                                            }
                                        }
                                        tv = sa.peekValue(5);
                                        if (tv != null && tv.type == 3) {
                                            if (tv.resourceId != 0) {
                                                header.breadCrumbTitleRes = tv.resourceId;
                                            } else {
                                                header.breadCrumbTitle = tv.string;
                                            }
                                        }
                                        tv = sa.peekValue(6);
                                        if (tv != null && tv.type == 3) {
                                            if (tv.resourceId != 0) {
                                                header.breadCrumbShortTitleRes = tv.resourceId;
                                            } else {
                                                header.breadCrumbShortTitle = tv.string;
                                            }
                                        }
                                        header.iconRes = sa.getResourceId(0, 0);
                                        header.fragment = sa.getString(4);
                                        sa.recycle();
                                        if (curBundle == null) {
                                            curBundle = new Bundle();
                                        }
                                        int innerDepth = parser.getDepth();
                                        while (true) {
                                            i2 = parser.next();
                                            type = i2;
                                            if (i2 != i3 && (type != next2 || parser.getDepth() > innerDepth)) {
                                                if (type != next2) {
                                                    if (type != 4) {
                                                        String innerNodeName = parser.getName();
                                                        if (innerNodeName.equals("extra")) {
                                                            getResources().parseBundleExtra("extra", attrs, curBundle);
                                                            XmlUtils.skipCurrentTag(parser);
                                                        } else if (innerNodeName.equals("intent")) {
                                                            header.intent = Intent.parseIntent(getResources(), parser, attrs);
                                                        } else {
                                                            XmlUtils.skipCurrentTag(parser);
                                                        }
                                                    }
                                                }
                                                i3 = 1;
                                                next2 = 3;
                                            }
                                        }
                                        if (curBundle.size() > 0) {
                                            header.fragmentArguments = curBundle;
                                            curBundle = null;
                                        }
                                    } catch (XmlPullParserException e3) {
                                        e = e3;
                                        list = target;
                                        throw new RuntimeException("Error parsing headers", e);
                                    } catch (IOException e4) {
                                        e2 = e4;
                                        list = target;
                                        throw new RuntimeException("Error parsing headers", e2);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        list = target;
                                        if (parser != null) {
                                        }
                                        throw th;
                                    }
                                    try {
                                        target.add(header);
                                    } catch (XmlPullParserException e5) {
                                        e = e5;
                                        throw new RuntimeException("Error parsing headers", e);
                                    } catch (IOException e6) {
                                        e2 = e6;
                                        throw new RuntimeException("Error parsing headers", e2);
                                    }
                                }
                                list = target;
                                XmlUtils.skipCurrentTag(parser);
                                i2 = 2;
                                i3 = 1;
                            }
                        }
                        list = target;
                        i2 = 2;
                        i3 = 1;
                    }
                    list = target;
                    if (parser != null) {
                        parser.close();
                        return;
                    }
                    return;
                }
                list = target;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("XML document must start with <preference-headers> tag; found");
                stringBuilder.append(nodeName);
                stringBuilder.append(" at ");
                stringBuilder.append(parser.getPositionDescription());
                throw new RuntimeException(stringBuilder.toString());
            } catch (XmlPullParserException e7) {
                e = e7;
                list = target;
                throw new RuntimeException("Error parsing headers", e);
            } catch (IOException e8) {
                e2 = e8;
                list = target;
                throw new RuntimeException("Error parsing headers", e2);
            } catch (Throwable th3) {
                th = th3;
                list = target;
                if (parser != null) {
                }
                throw th;
            }
        } catch (XmlPullParserException e9) {
            e = e9;
            i = resid;
            list = target;
            throw new RuntimeException("Error parsing headers", e);
        } catch (IOException e10) {
            e2 = e10;
            i = resid;
            list = target;
            throw new RuntimeException("Error parsing headers", e2);
        } catch (Throwable th4) {
            th = th4;
            if (parser != null) {
            }
            throw th;
        }
    }

    protected boolean isValidFragment(String fragmentName) {
        if (getApplicationInfo().targetSdkVersion < 19) {
            return true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Subclasses of PreferenceActivity must override isValidFragment(String) to verify that the Fragment class is valid! ");
        stringBuilder.append(getClass().getName());
        stringBuilder.append(" has not checked if fragment ");
        stringBuilder.append(fragmentName);
        stringBuilder.append(" is valid.");
        throw new RuntimeException(stringBuilder.toString());
    }

    public void setListFooter(View view) {
        this.mListFooter.removeAllViews();
        this.mListFooter.addView(view, (LayoutParams) new FrameLayout.LayoutParams(-1, -2));
    }

    protected void onStop() {
        super.onStop();
        if (this.mPreferenceManager != null) {
            this.mPreferenceManager.dispatchActivityStop();
        }
    }

    protected void onDestroy() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        super.onDestroy();
        if (this.mPreferenceManager != null) {
            this.mPreferenceManager.dispatchActivityDestroy();
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mHeaders.size() > 0) {
            outState.putParcelableArrayList(HEADERS_TAG, this.mHeaders);
            if (this.mCurHeader != null) {
                int index = this.mHeaders.indexOf(this.mCurHeader);
                if (index >= 0) {
                    outState.putInt(CUR_HEADER_TAG, index);
                }
            }
        }
        if (this.mPreferenceManager != null) {
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            if (preferenceScreen != null) {
                Bundle container = new Bundle();
                preferenceScreen.saveHierarchyState(container);
                outState.putBundle(PREFERENCES_TAG, container);
            }
        }
    }

    protected void onRestoreInstanceState(Bundle state) {
        if (this.mPreferenceManager != null) {
            Bundle container = state.getBundle(PREFERENCES_TAG);
            if (container != null) {
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                if (preferenceScreen != null) {
                    preferenceScreen.restoreHierarchyState(container);
                    this.mSavedInstanceState = state;
                    return;
                }
            }
        }
        super.onRestoreInstanceState(state);
        if (!(this.mSinglePane || this.mCurHeader == null)) {
            setSelectedHeader(this.mCurHeader);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (this.mPreferenceManager != null) {
            this.mPreferenceManager.dispatchActivityResult(requestCode, resultCode, data);
        }
    }

    public void onContentChanged() {
        super.onContentChanged();
        if (this.mPreferenceManager != null) {
            postBindPreferences();
        }
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (isResumed()) {
            super.onListItemClick(l, v, position, id);
            if (this.mAdapter != null) {
                Object item = this.mAdapter.getItem(position);
                if (item instanceof Header) {
                    onHeaderClick((Header) item, position);
                }
            }
        }
    }

    public void onHeaderClick(Header header, int position) {
        if (header.fragment != null) {
            switchToHeader(header);
        } else if (header.intent != null) {
            startActivity(header.intent);
        }
    }

    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args, int titleRes, int shortTitleRes) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClass(this, getClass());
        intent.putExtra(EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, titleRes);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_SHORT_TITLE, shortTitleRes);
        intent.putExtra(EXTRA_NO_HEADERS, true);
        return intent;
    }

    public void startWithFragment(String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode) {
        startWithFragment(fragmentName, args, resultTo, resultRequestCode, 0, 0);
    }

    public void startWithFragment(String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, int titleRes, int shortTitleRes) {
        Intent intent = onBuildStartFragmentIntent(fragmentName, args, titleRes, shortTitleRes);
        if (resultTo == null) {
            startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    public void showBreadCrumbs(CharSequence title, CharSequence shortTitle) {
        if (this.mFragmentBreadCrumbs == null) {
            try {
                this.mFragmentBreadCrumbs = (FragmentBreadCrumbs) findViewById(16908310);
                if (this.mFragmentBreadCrumbs == null) {
                    if (title != null) {
                        setTitle(title);
                    }
                    return;
                }
                if (this.mSinglePane) {
                    this.mFragmentBreadCrumbs.setVisibility(8);
                    View bcSection = findViewById(16908772);
                    if (bcSection != null) {
                        bcSection.setVisibility(8);
                    }
                    setTitle(title);
                }
                this.mFragmentBreadCrumbs.setMaxVisible(2);
                this.mFragmentBreadCrumbs.setActivity(this);
            } catch (ClassCastException e) {
                setTitle(title);
                return;
            }
        }
        if (this.mFragmentBreadCrumbs.getVisibility() != 0) {
            setTitle(title);
        } else {
            this.mFragmentBreadCrumbs.setTitle(title, shortTitle);
            this.mFragmentBreadCrumbs.setParentTitle(null, null, null);
        }
    }

    public void setParentTitle(CharSequence title, CharSequence shortTitle, OnClickListener listener) {
        if (this.mFragmentBreadCrumbs != null) {
            this.mFragmentBreadCrumbs.setParentTitle(title, shortTitle, listener);
        }
    }

    void setSelectedHeader(Header header) {
        this.mCurHeader = header;
        int index = this.mHeaders.indexOf(header);
        if (index >= 0) {
            getListView().setItemChecked(index, true);
        } else {
            getListView().clearChoices();
        }
        showBreadCrumbs(header);
    }

    void showBreadCrumbs(Header header) {
        if (header != null) {
            CharSequence title = header.getBreadCrumbTitle(getResources());
            if (title == null) {
                title = header.getTitle(getResources());
            }
            if (title == null) {
                title = getTitle();
            }
            showBreadCrumbs(title, header.getBreadCrumbShortTitle(getResources()));
            return;
        }
        showBreadCrumbs(getTitle(), null);
    }

    private void switchToHeaderInner(String fragmentName, Bundle args) {
        getFragmentManager().popBackStack(BACK_STACK_PREFS, 1);
        if (isValidFragment(fragmentName)) {
            int i;
            Fragment f = Fragment.instantiate(this, fragmentName, args);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            if (this.mSinglePane) {
                i = 0;
            } else {
                i = GLES31.GL_TEXTURE_INTERNAL_FORMAT;
            }
            transaction.setTransition(i);
            transaction.replace(16909212, f);
            transaction.commitAllowingStateLoss();
            if (this.mSinglePane && this.mPrefsContainer.getVisibility() == 8) {
                this.mPrefsContainer.setVisibility(0);
                this.mHeadersContainer.setVisibility(8);
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid fragment for this activity: ");
        stringBuilder.append(fragmentName);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void switchToHeader(String fragmentName, Bundle args) {
        Header selectedHeader = null;
        for (int i = 0; i < this.mHeaders.size(); i++) {
            if (fragmentName.equals(((Header) this.mHeaders.get(i)).fragment)) {
                selectedHeader = (Header) this.mHeaders.get(i);
                break;
            }
        }
        setSelectedHeader(selectedHeader);
        switchToHeaderInner(fragmentName, args);
    }

    public void switchToHeader(Header header) {
        if (this.mCurHeader == header) {
            getFragmentManager().popBackStack(BACK_STACK_PREFS, 1);
        } else if (header.fragment != null) {
            switchToHeaderInner(header.fragment, header.fragmentArguments);
            setSelectedHeader(header);
        } else {
            throw new IllegalStateException("can't switch to header that has no fragment");
        }
    }

    Header findBestMatchingHeader(Header cur, ArrayList<Header> from) {
        int j;
        Header oh;
        ArrayList<Header> matches = new ArrayList();
        int j2 = 0;
        for (j = 0; j < from.size(); j++) {
            oh = (Header) from.get(j);
            if (cur == oh || (cur.id != -1 && cur.id == oh.id)) {
                matches.clear();
                matches.add(oh);
                break;
            }
            if (cur.fragment != null) {
                if (cur.fragment.equals(oh.fragment)) {
                    matches.add(oh);
                }
            } else if (cur.intent != null) {
                if (cur.intent.equals(oh.intent)) {
                    matches.add(oh);
                }
            } else if (cur.title != null && cur.title.equals(oh.title)) {
                matches.add(oh);
            }
        }
        j = matches.size();
        if (j == 1) {
            return (Header) matches.get(0);
        }
        if (j > 1) {
            while (j2 < j) {
                oh = (Header) matches.get(j2);
                if (cur.fragmentArguments != null && cur.fragmentArguments.equals(oh.fragmentArguments)) {
                    return oh;
                }
                if (cur.extras != null && cur.extras.equals(oh.extras)) {
                    return oh;
                }
                if (cur.title != null && cur.title.equals(oh.title)) {
                    return oh;
                }
                j2++;
            }
        }
        return null;
    }

    public void startPreferenceFragment(Fragment fragment, boolean push) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(16909212, fragment);
        if (push) {
            transaction.setTransition(4097);
            transaction.addToBackStack(BACK_STACK_PREFS);
        } else {
            transaction.setTransition(GLES31.GL_TEXTURE_INTERNAL_FORMAT);
        }
        transaction.commitAllowingStateLoss();
    }

    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes, CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        Fragment f = Fragment.instantiate(this, fragmentClass, args);
        if (resultTo != null) {
            f.setTargetFragment(resultTo, resultRequestCode);
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(16909212, f);
        if (titleRes != 0) {
            transaction.setBreadCrumbTitle(titleRes);
        } else if (titleText != null) {
            transaction.setBreadCrumbTitle(titleText);
        }
        transaction.setTransition(4097);
        transaction.addToBackStack(BACK_STACK_PREFS);
        transaction.commitAllowingStateLoss();
    }

    public void finishPreferencePanel(Fragment caller, int resultCode, Intent resultData) {
        onBackPressed();
        if (caller != null && caller.getTargetFragment() != null) {
            caller.getTargetFragment().onActivityResult(caller.getTargetRequestCode(), resultCode, resultData);
        }
    }

    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        startPreferencePanel(pref.getFragment(), pref.getExtras(), pref.getTitleRes(), pref.getTitle(), null, 0);
        return true;
    }

    private void postBindPreferences() {
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.obtainMessage(1).sendToTarget();
        }
    }

    private void bindPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.bind(getListView());
            if (this.mSavedInstanceState != null) {
                super.onRestoreInstanceState(this.mSavedInstanceState);
                this.mSavedInstanceState = null;
            }
        }
    }

    @Deprecated
    public PreferenceManager getPreferenceManager() {
        return this.mPreferenceManager;
    }

    private void requirePreferenceManager() {
        if (this.mPreferenceManager != null) {
            return;
        }
        if (this.mAdapter == null) {
            throw new RuntimeException("This should be called after super.onCreate.");
        }
        throw new RuntimeException("Modern two-pane PreferenceActivity requires use of a PreferenceFragment");
    }

    @Deprecated
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        requirePreferenceManager();
        if (this.mPreferenceManager.setPreferences(preferenceScreen) && preferenceScreen != null) {
            postBindPreferences();
            CharSequence title = getPreferenceScreen().getTitle();
            if (title != null) {
                setTitle(title);
            }
        }
    }

    @Deprecated
    public PreferenceScreen getPreferenceScreen() {
        if (this.mPreferenceManager != null) {
            return this.mPreferenceManager.getPreferenceScreen();
        }
        return null;
    }

    @Deprecated
    public void addPreferencesFromIntent(Intent intent) {
        requirePreferenceManager();
        setPreferenceScreen(this.mPreferenceManager.inflateFromIntent(intent, getPreferenceScreen()));
    }

    @Deprecated
    public void addPreferencesFromResource(int preferencesResId) {
        requirePreferenceManager();
        setPreferenceScreen(this.mPreferenceManager.inflateFromResource(this, preferencesResId, getPreferenceScreen()));
    }

    @Deprecated
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    @Deprecated
    public Preference findPreference(CharSequence key) {
        if (this.mPreferenceManager == null) {
            return null;
        }
        return this.mPreferenceManager.findPreference(key);
    }

    protected void onNewIntent(Intent intent) {
        if (this.mPreferenceManager != null) {
            this.mPreferenceManager.dispatchNewIntent(intent);
        }
    }

    protected boolean hasNextButton() {
        return this.mNextButton != null;
    }

    protected Button getNextButton() {
        return this.mNextButton;
    }
}
