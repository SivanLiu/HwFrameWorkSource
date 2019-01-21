package huawei.android.widget;

import android.util.Log;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import huawei.android.widget.AlphaIndexerListView.OnItemClickListener;

public class HwQuickIndexController {
    private static final String TAG = "HwQuickIndexController";
    private AlphaIndexerListView mAlphaView;
    private HwSortedTextListAdapter mDataAdapter;
    private int mFlingStartPos = 0;
    private boolean mIsShowPopup;
    private ListView mListView;

    public HwQuickIndexController(ListView listview, AlphaIndexerListView alphaView) {
        this.mListView = listview;
        this.mDataAdapter = (HwSortedTextListAdapter) listview.getAdapter();
        this.mAlphaView = alphaView;
        this.mAlphaView.setListViewAttachTo(this.mListView);
        this.mAlphaView.setOverLayInfo(getCurrentSection(this.mDataAdapter.getSectionForPosition(this.mListView.getFirstVisiblePosition())));
    }

    private String getCurrentSection(int sectionPos) {
        String currentSection = "";
        if (this.mDataAdapter.getSections().length <= sectionPos || sectionPos < 0) {
            return currentSection;
        }
        return this.mDataAdapter.getSections()[sectionPos];
    }

    public void setOnListen() {
        this.mListView.setOnScrollListener(new OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == 0) {
                    HwQuickIndexController.this.mIsShowPopup = false;
                    HwQuickIndexController.this.mAlphaView.dismissPopup();
                    Log.e(HwQuickIndexController.TAG, "SCROLL_STATE_IDLE");
                } else if (scrollState == 2) {
                    HwQuickIndexController.this.mIsShowPopup = true;
                    HwQuickIndexController.this.mFlingStartPos = HwQuickIndexController.this.mListView.getFirstVisiblePosition();
                    String str = HwQuickIndexController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("SCROLL_STATE_FLING_IN: ");
                    stringBuilder.append(HwQuickIndexController.this.mFlingStartPos);
                    Log.e(str, stringBuilder.toString());
                }
            }

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                HwQuickIndexController.this.mAlphaView.invalidate();
                HwQuickIndexController.this.mAlphaView.setOverLayInfo(HwQuickIndexController.this.getCurrentSection(HwQuickIndexController.this.mDataAdapter.getSectionForPosition(firstVisibleItem)));
                if (HwQuickIndexController.this.mIsShowPopup && Math.abs(firstVisibleItem - HwQuickIndexController.this.mFlingStartPos) > 2) {
                    HwQuickIndexController.this.mAlphaView.showPopup();
                }
            }
        });
        this.mAlphaView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(String s, int pos) {
                if (s != null) {
                    int i;
                    String[] sections = (String[]) HwQuickIndexController.this.mDataAdapter.getSections();
                    int sectionPos = pos;
                    String sectionText = null;
                    for (i = 0; i < sections.length; i++) {
                        if (HwQuickIndexController.this.mAlphaView.equalsChar(s, sections[i], pos)) {
                            sectionText = sections[i];
                            sectionPos = i;
                            break;
                        }
                    }
                    if (sectionText != null) {
                        HwQuickIndexController.this.mAlphaView.showPopup(sectionText);
                        int position = HwQuickIndexController.this.mDataAdapter.getPositionForSection(sectionPos);
                        if (-1 != position) {
                            HwQuickIndexController.this.mListView.setSelection(position);
                        }
                        i = (HwQuickIndexController.this.mListView.getLastVisiblePosition() - HwQuickIndexController.this.mListView.getFirstVisiblePosition()) + 1;
                        if (position + i > HwQuickIndexController.this.mListView.getCount()) {
                            sectionText = (String) HwQuickIndexController.this.mDataAdapter.getSectionNameForPosition(HwQuickIndexController.this.mListView.getCount() - i);
                        }
                        HwQuickIndexController.this.mAlphaView.setOverLayInfo(pos, sectionText);
                        return;
                    }
                    if (HwQuickIndexController.this.mAlphaView.needSwitchIndexer(pos)) {
                        if (HwQuickIndexController.this.mAlphaView.isNativeIndexerShow()) {
                            HwQuickIndexController.this.mListView.setSelection(HwQuickIndexController.this.mListView.getCount() - 1);
                        } else {
                            HwQuickIndexController.this.mListView.setSelection(0);
                        }
                    }
                    HwQuickIndexController.this.mAlphaView.setOverLayInfo(pos, HwQuickIndexController.this.getCurrentSection(HwQuickIndexController.this.mDataAdapter.getSectionForPosition(HwQuickIndexController.this.mListView.getFirstVisiblePosition())));
                }
            }
        });
    }
}
