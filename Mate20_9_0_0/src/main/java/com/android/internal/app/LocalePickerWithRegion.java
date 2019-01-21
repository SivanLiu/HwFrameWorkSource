package com.android.internal.app;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import com.android.internal.app.LocaleHelper.LocaleInfoComparator;
import com.android.internal.app.LocaleStore.LocaleInfo;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class LocalePickerWithRegion extends ListFragment implements OnQueryTextListener {
    private static final String PARENT_FRAGMENT_NAME = "localeListEditor";
    private SuggestedLocaleAdapter mAdapter;
    private int mFirstVisiblePosition = 0;
    private LocaleSelectedListener mListener;
    private Set<LocaleInfo> mLocaleList;
    private LocaleInfo mParentLocale;
    private CharSequence mPreviousSearch = null;
    private boolean mPreviousSearchHadFocus = false;
    private SearchView mSearchView = null;
    private int mTopDistance = 0;
    private boolean mTranslatedOnly = false;

    public interface LocaleSelectedListener {
        void onLocaleSelected(LocaleInfo localeInfo);
    }

    private static LocalePickerWithRegion createCountryPicker(Context context, LocaleSelectedListener listener, LocaleInfo parent, boolean translatedOnly) {
        LocalePickerWithRegion localePicker = new LocalePickerWithRegion();
        return localePicker.setListener(context, listener, parent, translatedOnly) ? localePicker : null;
    }

    public static LocalePickerWithRegion createLanguagePicker(Context context, LocaleSelectedListener listener, boolean translatedOnly) {
        LocalePickerWithRegion localePicker = new LocalePickerWithRegion();
        localePicker.setListener(context, listener, null, translatedOnly);
        return localePicker;
    }

    private boolean setListener(Context context, LocaleSelectedListener listener, LocaleInfo parent, boolean translatedOnly) {
        this.mParentLocale = parent;
        this.mListener = listener;
        this.mTranslatedOnly = translatedOnly;
        setRetainInstance(true);
        HashSet<String> langTagsToIgnore = new HashSet();
        if (!translatedOnly) {
            Collections.addAll(langTagsToIgnore, LocalePicker.getLocales().toLanguageTags().split(","));
        }
        if (parent != null) {
            this.mLocaleList = LocaleStore.getLevelLocales(context, langTagsToIgnore, parent, translatedOnly);
            if (this.mLocaleList.size() <= 1) {
                if (listener != null && this.mLocaleList.size() == 1) {
                    listener.onLocaleSelected((LocaleInfo) this.mLocaleList.iterator().next());
                }
                return false;
            }
        }
        this.mLocaleList = LocaleStore.getLevelLocales(context, langTagsToIgnore, null, translatedOnly);
        return true;
    }

    private void returnToParentFrame() {
        getFragmentManager().popBackStack(PARENT_FRAGMENT_NAME, 1);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean countryMode = true;
        setHasOptionsMenu(true);
        if (this.mLocaleList == null) {
            returnToParentFrame();
            return;
        }
        if (this.mParentLocale == null) {
            countryMode = false;
        }
        Locale sortingLocale = countryMode ? this.mParentLocale.getLocale() : Locale.getDefault();
        this.mAdapter = new SuggestedLocaleAdapter(this.mLocaleList, countryMode);
        this.mAdapter.sort(new LocaleInfoComparator(sortingLocale, countryMode));
        setListAdapter(this.mAdapter);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        View searchArea = getLayoutInflater().inflate(34013324, null);
        this.mSearchView = (SearchView) searchArea.findViewById(34603016);
        getListView().addHeaderView(searchArea);
        this.mSearchView.setQueryHint(getText(33686095));
        this.mSearchView.setOnQueryTextListener(this);
        if (this.mPreviousSearch != null) {
            this.mSearchView.setIconified(false);
            this.mSearchView.setActivated(true);
            if (this.mPreviousSearchHadFocus) {
                this.mSearchView.requestFocus();
            }
            this.mSearchView.setQuery(this.mPreviousSearch, true);
        } else {
            this.mSearchView.setQuery(null, false);
        }
        getListView().setSelectionFromTop(this.mFirstVisiblePosition, this.mTopDistance);
        super.onViewCreated(view, savedInstanceState);
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() != 16908332) {
            return super.onOptionsItemSelected(menuItem);
        }
        getFragmentManager().popBackStack();
        return true;
    }

    public void onResume() {
        super.onResume();
        if (getActivity().getActionBar() != null) {
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
            if (this.mParentLocale != null) {
                getActivity().getActionBar().setTitle(this.mParentLocale.getFullNameNative());
            } else {
                getActivity().getActionBar().setTitle(17040317);
            }
        }
        getListView().requestFocus();
    }

    public void onPause() {
        super.onPause();
        int i = 0;
        if (this.mSearchView != null) {
            this.mPreviousSearchHadFocus = this.mSearchView.hasFocus();
            this.mPreviousSearch = this.mSearchView.getQuery();
        } else {
            this.mPreviousSearchHadFocus = false;
            this.mPreviousSearch = null;
        }
        ListView list = getListView();
        View firstChild = list.getChildAt(0);
        this.mFirstVisiblePosition = list.getFirstVisiblePosition();
        if (firstChild != null) {
            i = firstChild.getTop() - list.getPaddingTop();
        }
        this.mTopDistance = i;
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        LocaleInfo locale = (LocaleInfo) getListAdapter().getItem(position - getListView().getHeaderViewsCount());
        if (locale.getParent() != null) {
            if (this.mListener != null) {
                this.mListener.onLocaleSelected(locale);
            }
            returnToParentFrame();
            return;
        }
        LocalePickerWithRegion selector = createCountryPicker(getContext(), this.mListener, locale, this.mTranslatedOnly);
        if (selector != null) {
            getFragmentManager().beginTransaction().setTransition(4097).replace(getId(), selector).addToBackStack(null).commit();
        } else {
            returnToParentFrame();
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (this.mParentLocale == null) {
            inflater.inflate(18087936, menu);
            MenuItem searchMenuItem = menu.findItem(16909052);
            this.mSearchView = (SearchView) searchMenuItem.getActionView();
            this.mSearchView.setQueryHint(getText(17041049));
            this.mSearchView.setOnQueryTextListener(this);
            if (TextUtils.isEmpty(this.mPreviousSearch)) {
                this.mSearchView.setQuery(null, false);
            } else {
                searchMenuItem.expandActionView();
                this.mSearchView.setIconified(false);
                this.mSearchView.setActivated(true);
                if (this.mPreviousSearchHadFocus) {
                    this.mSearchView.requestFocus();
                }
                this.mSearchView.setQuery(this.mPreviousSearch, true);
            }
            getListView().setSelectionFromTop(this.mFirstVisiblePosition, this.mTopDistance);
        }
    }

    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    public boolean onQueryTextChange(String newText) {
        if (this.mAdapter != null) {
            this.mAdapter.getFilter().filter(newText);
        }
        return false;
    }
}
