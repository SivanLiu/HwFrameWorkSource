package android.view.accessibility;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.provider.SettingsStringUtil;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AccessibilityClickableSpan;
import android.text.style.AccessibilityURLSpan;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.ArraySet;
import android.util.LongArray;
import android.util.Pools.SynchronizedPool;
import android.view.View;
import com.android.internal.util.BitUtils;
import com.android.internal.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class AccessibilityNodeInfo implements Parcelable {
    public static final int ACTION_ACCESSIBILITY_FOCUS = 64;
    public static final String ACTION_ARGUMENT_ACCESSIBLE_CLICKABLE_SPAN = "android.view.accessibility.action.ACTION_ARGUMENT_ACCESSIBLE_CLICKABLE_SPAN";
    public static final String ACTION_ARGUMENT_COLUMN_INT = "android.view.accessibility.action.ARGUMENT_COLUMN_INT";
    public static final String ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN = "ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN";
    public static final String ACTION_ARGUMENT_HTML_ELEMENT_STRING = "ACTION_ARGUMENT_HTML_ELEMENT_STRING";
    public static final String ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT = "ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT";
    public static final String ACTION_ARGUMENT_MOVE_WINDOW_X = "ACTION_ARGUMENT_MOVE_WINDOW_X";
    public static final String ACTION_ARGUMENT_MOVE_WINDOW_Y = "ACTION_ARGUMENT_MOVE_WINDOW_Y";
    public static final String ACTION_ARGUMENT_PROGRESS_VALUE = "android.view.accessibility.action.ARGUMENT_PROGRESS_VALUE";
    public static final String ACTION_ARGUMENT_ROW_INT = "android.view.accessibility.action.ARGUMENT_ROW_INT";
    public static final String ACTION_ARGUMENT_SELECTION_END_INT = "ACTION_ARGUMENT_SELECTION_END_INT";
    public static final String ACTION_ARGUMENT_SELECTION_START_INT = "ACTION_ARGUMENT_SELECTION_START_INT";
    public static final String ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE = "ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE";
    public static final int ACTION_CLEAR_ACCESSIBILITY_FOCUS = 128;
    public static final int ACTION_CLEAR_FOCUS = 2;
    public static final int ACTION_CLEAR_SELECTION = 8;
    public static final int ACTION_CLICK = 16;
    public static final int ACTION_COLLAPSE = 524288;
    public static final int ACTION_COPY = 16384;
    public static final int ACTION_CUT = 65536;
    public static final int ACTION_DISMISS = 1048576;
    public static final int ACTION_EXPAND = 262144;
    public static final int ACTION_FOCUS = 1;
    public static final int ACTION_LONG_CLICK = 32;
    public static final int ACTION_NEXT_AT_MOVEMENT_GRANULARITY = 256;
    public static final int ACTION_NEXT_HTML_ELEMENT = 1024;
    public static final int ACTION_PASTE = 32768;
    public static final int ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY = 512;
    public static final int ACTION_PREVIOUS_HTML_ELEMENT = 2048;
    public static final int ACTION_SCROLL_BACKWARD = 8192;
    public static final int ACTION_SCROLL_FORWARD = 4096;
    public static final int ACTION_SELECT = 4;
    public static final int ACTION_SET_SELECTION = 131072;
    public static final int ACTION_SET_TEXT = 2097152;
    private static final int ACTION_TYPE_MASK = -16777216;
    private static final int BOOLEAN_PROPERTY_ACCESSIBILITY_FOCUSED = 1024;
    private static final int BOOLEAN_PROPERTY_CHECKABLE = 1;
    private static final int BOOLEAN_PROPERTY_CHECKED = 2;
    private static final int BOOLEAN_PROPERTY_CLICKABLE = 32;
    private static final int BOOLEAN_PROPERTY_CONTENT_INVALID = 65536;
    private static final int BOOLEAN_PROPERTY_CONTEXT_CLICKABLE = 131072;
    private static final int BOOLEAN_PROPERTY_DISMISSABLE = 16384;
    private static final int BOOLEAN_PROPERTY_EDITABLE = 4096;
    private static final int BOOLEAN_PROPERTY_ENABLED = 128;
    private static final int BOOLEAN_PROPERTY_FOCUSABLE = 4;
    private static final int BOOLEAN_PROPERTY_FOCUSED = 8;
    private static final int BOOLEAN_PROPERTY_IMPORTANCE = 262144;
    private static final int BOOLEAN_PROPERTY_IS_HEADING = 2097152;
    private static final int BOOLEAN_PROPERTY_IS_SHOWING_HINT = 1048576;
    private static final int BOOLEAN_PROPERTY_LONG_CLICKABLE = 64;
    private static final int BOOLEAN_PROPERTY_MULTI_LINE = 32768;
    private static final int BOOLEAN_PROPERTY_OPENS_POPUP = 8192;
    private static final int BOOLEAN_PROPERTY_PASSWORD = 256;
    private static final int BOOLEAN_PROPERTY_SCREEN_READER_FOCUSABLE = 524288;
    private static final int BOOLEAN_PROPERTY_SCROLLABLE = 512;
    private static final int BOOLEAN_PROPERTY_SELECTED = 16;
    private static final int BOOLEAN_PROPERTY_VISIBLE_TO_USER = 2048;
    public static final Creator<AccessibilityNodeInfo> CREATOR = new Creator<AccessibilityNodeInfo>() {
        public AccessibilityNodeInfo createFromParcel(Parcel parcel) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.initFromParcel(parcel);
            return info;
        }

        public AccessibilityNodeInfo[] newArray(int size) {
            return new AccessibilityNodeInfo[size];
        }
    };
    private static final boolean DEBUG = false;
    private static final AccessibilityNodeInfo DEFAULT = new AccessibilityNodeInfo();
    public static final String EXTRA_DATA_REQUESTED_KEY = "android.view.accessibility.AccessibilityNodeInfo.extra_data_requested";
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH = "android.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH";
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX = "android.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX";
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY = "android.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_KEY";
    public static final int FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 8;
    public static final int FLAG_PREFETCH_DESCENDANTS = 4;
    public static final int FLAG_PREFETCH_PREDECESSORS = 1;
    public static final int FLAG_PREFETCH_SIBLINGS = 2;
    public static final int FLAG_REPORT_VIEW_IDS = 16;
    public static final int FOCUS_ACCESSIBILITY = 2;
    public static final int FOCUS_INPUT = 1;
    public static final int LAST_LEGACY_STANDARD_ACTION = 2097152;
    private static final int MAX_POOL_SIZE = 50;
    public static final int MOVEMENT_GRANULARITY_CHARACTER = 1;
    public static final int MOVEMENT_GRANULARITY_LINE = 4;
    public static final int MOVEMENT_GRANULARITY_PAGE = 16;
    public static final int MOVEMENT_GRANULARITY_PARAGRAPH = 8;
    public static final int MOVEMENT_GRANULARITY_WORD = 2;
    public static final int ROOT_ITEM_ID = 2147483646;
    public static final long ROOT_NODE_ID = makeNodeId(2147483646, -1);
    public static final int UNDEFINED_CONNECTION_ID = -1;
    public static final int UNDEFINED_ITEM_ID = Integer.MAX_VALUE;
    public static final long UNDEFINED_NODE_ID = makeNodeId(Integer.MAX_VALUE, Integer.MAX_VALUE);
    public static final int UNDEFINED_SELECTION_INDEX = -1;
    private static final long VIRTUAL_DESCENDANT_ID_MASK = -4294967296L;
    private static final int VIRTUAL_DESCENDANT_ID_SHIFT = 32;
    private static AtomicInteger sNumInstancesInUse;
    private static final SynchronizedPool<AccessibilityNodeInfo> sPool = new SynchronizedPool(50);
    private ArrayList<AccessibilityAction> mActions;
    private int mBooleanProperties;
    private final Rect mBoundsInParent = new Rect();
    private final Rect mBoundsInScreen = new Rect();
    private LongArray mChildNodeIds;
    private CharSequence mClassName;
    private CollectionInfo mCollectionInfo;
    private CollectionItemInfo mCollectionItemInfo;
    private int mConnectionId = -1;
    private CharSequence mContentDescription;
    private int mDrawingOrderInParent;
    private CharSequence mError;
    private ArrayList<String> mExtraDataKeys;
    private Bundle mExtras;
    private CharSequence mHintText;
    private int mInputType = 0;
    private long mLabelForId = UNDEFINED_NODE_ID;
    private long mLabeledById = UNDEFINED_NODE_ID;
    private int mLiveRegion = 0;
    private int mMaxTextLength = -1;
    private int mMovementGranularities;
    private CharSequence mOriginalText;
    private CharSequence mPackageName;
    private CharSequence mPaneTitle;
    private long mParentNodeId = UNDEFINED_NODE_ID;
    private RangeInfo mRangeInfo;
    private boolean mSealed;
    private long mSourceNodeId = UNDEFINED_NODE_ID;
    private CharSequence mText;
    private int mTextSelectionEnd = -1;
    private int mTextSelectionStart = -1;
    private CharSequence mTooltipText;
    private long mTraversalAfter = UNDEFINED_NODE_ID;
    private long mTraversalBefore = UNDEFINED_NODE_ID;
    private String mViewIdResourceName;
    private int mWindowId = -1;

    public static final class AccessibilityAction {
        public static final AccessibilityAction ACTION_ACCESSIBILITY_FOCUS = new AccessibilityAction(64);
        public static final AccessibilityAction ACTION_CLEAR_ACCESSIBILITY_FOCUS = new AccessibilityAction(128);
        public static final AccessibilityAction ACTION_CLEAR_FOCUS = new AccessibilityAction(2);
        public static final AccessibilityAction ACTION_CLEAR_SELECTION = new AccessibilityAction(8);
        public static final AccessibilityAction ACTION_CLICK = new AccessibilityAction(16);
        public static final AccessibilityAction ACTION_COLLAPSE = new AccessibilityAction(524288);
        public static final AccessibilityAction ACTION_CONTEXT_CLICK = new AccessibilityAction(16908348);
        public static final AccessibilityAction ACTION_COPY = new AccessibilityAction(16384);
        public static final AccessibilityAction ACTION_CUT = new AccessibilityAction(65536);
        public static final AccessibilityAction ACTION_DISMISS = new AccessibilityAction(1048576);
        public static final AccessibilityAction ACTION_EXPAND = new AccessibilityAction(262144);
        public static final AccessibilityAction ACTION_FOCUS = new AccessibilityAction(1);
        public static final AccessibilityAction ACTION_HIDE_TOOLTIP = new AccessibilityAction(16908357);
        public static final AccessibilityAction ACTION_LONG_CLICK = new AccessibilityAction(32);
        public static final AccessibilityAction ACTION_MOVE_WINDOW = new AccessibilityAction(16908354);
        public static final AccessibilityAction ACTION_NEXT_AT_MOVEMENT_GRANULARITY = new AccessibilityAction(256);
        public static final AccessibilityAction ACTION_NEXT_HTML_ELEMENT = new AccessibilityAction(1024);
        public static final AccessibilityAction ACTION_PASTE = new AccessibilityAction(32768);
        public static final AccessibilityAction ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY = new AccessibilityAction(512);
        public static final AccessibilityAction ACTION_PREVIOUS_HTML_ELEMENT = new AccessibilityAction(2048);
        public static final AccessibilityAction ACTION_SCROLL_BACKWARD = new AccessibilityAction(8192);
        public static final AccessibilityAction ACTION_SCROLL_DOWN = new AccessibilityAction(16908346);
        public static final AccessibilityAction ACTION_SCROLL_FORWARD = new AccessibilityAction(4096);
        public static final AccessibilityAction ACTION_SCROLL_LEFT = new AccessibilityAction(16908345);
        public static final AccessibilityAction ACTION_SCROLL_RIGHT = new AccessibilityAction(16908347);
        public static final AccessibilityAction ACTION_SCROLL_TO_POSITION = new AccessibilityAction(16908343);
        public static final AccessibilityAction ACTION_SCROLL_UP = new AccessibilityAction(16908344);
        public static final AccessibilityAction ACTION_SELECT = new AccessibilityAction(4);
        public static final AccessibilityAction ACTION_SET_PROGRESS = new AccessibilityAction(16908349);
        public static final AccessibilityAction ACTION_SET_SELECTION = new AccessibilityAction(131072);
        public static final AccessibilityAction ACTION_SET_TEXT = new AccessibilityAction(2097152);
        public static final AccessibilityAction ACTION_SHOW_ON_SCREEN = new AccessibilityAction(16908342);
        public static final AccessibilityAction ACTION_SHOW_TOOLTIP = new AccessibilityAction(16908356);
        public static final ArraySet<AccessibilityAction> sStandardActions = new ArraySet();
        private final int mActionId;
        private final CharSequence mLabel;
        public long mSerializationFlag;

        public AccessibilityAction(int actionId, CharSequence label) {
            this.mSerializationFlag = -1;
            if ((-16777216 & actionId) != 0 || Integer.bitCount(actionId) == 1) {
                this.mActionId = actionId;
                this.mLabel = label;
                return;
            }
            throw new IllegalArgumentException("Invalid standard action id");
        }

        private AccessibilityAction(int standardActionId) {
            this(standardActionId, null);
            this.mSerializationFlag = BitUtils.bitAt(sStandardActions.size());
            sStandardActions.add(this);
        }

        public int getId() {
            return this.mActionId;
        }

        public CharSequence getLabel() {
            return this.mLabel;
        }

        public int hashCode() {
            return this.mActionId;
        }

        public boolean equals(Object other) {
            boolean z = false;
            if (other == null) {
                return false;
            }
            if (other == this) {
                return true;
            }
            if (getClass() != other.getClass()) {
                return false;
            }
            if (this.mActionId == ((AccessibilityAction) other).mActionId) {
                z = true;
            }
            return z;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AccessibilityAction: ");
            stringBuilder.append(AccessibilityNodeInfo.getActionSymbolicName(this.mActionId));
            stringBuilder.append(" - ");
            stringBuilder.append(this.mLabel);
            return stringBuilder.toString();
        }
    }

    public static final class CollectionInfo {
        private static final int MAX_POOL_SIZE = 20;
        public static final int SELECTION_MODE_MULTIPLE = 2;
        public static final int SELECTION_MODE_NONE = 0;
        public static final int SELECTION_MODE_SINGLE = 1;
        private static final SynchronizedPool<CollectionInfo> sPool = new SynchronizedPool(20);
        private int mColumnCount;
        private boolean mHierarchical;
        private int mRowCount;
        private int mSelectionMode;

        public static CollectionInfo obtain(CollectionInfo other) {
            return obtain(other.mRowCount, other.mColumnCount, other.mHierarchical, other.mSelectionMode);
        }

        public static CollectionInfo obtain(int rowCount, int columnCount, boolean hierarchical) {
            return obtain(rowCount, columnCount, hierarchical, 0);
        }

        public static CollectionInfo obtain(int rowCount, int columnCount, boolean hierarchical, int selectionMode) {
            CollectionInfo info = (CollectionInfo) sPool.acquire();
            if (info == null) {
                return new CollectionInfo(rowCount, columnCount, hierarchical, selectionMode);
            }
            info.mRowCount = rowCount;
            info.mColumnCount = columnCount;
            info.mHierarchical = hierarchical;
            info.mSelectionMode = selectionMode;
            return info;
        }

        private CollectionInfo(int rowCount, int columnCount, boolean hierarchical, int selectionMode) {
            this.mRowCount = rowCount;
            this.mColumnCount = columnCount;
            this.mHierarchical = hierarchical;
            this.mSelectionMode = selectionMode;
        }

        public int getRowCount() {
            return this.mRowCount;
        }

        public int getColumnCount() {
            return this.mColumnCount;
        }

        public boolean isHierarchical() {
            return this.mHierarchical;
        }

        public int getSelectionMode() {
            return this.mSelectionMode;
        }

        void recycle() {
            clear();
            sPool.release(this);
        }

        private void clear() {
            this.mRowCount = 0;
            this.mColumnCount = 0;
            this.mHierarchical = false;
            this.mSelectionMode = 0;
        }
    }

    public static final class CollectionItemInfo {
        private static final int MAX_POOL_SIZE = 20;
        private static final SynchronizedPool<CollectionItemInfo> sPool = new SynchronizedPool(20);
        private int mColumnIndex;
        private int mColumnSpan;
        private boolean mHeading;
        private int mRowIndex;
        private int mRowSpan;
        private boolean mSelected;

        public static CollectionItemInfo obtain(CollectionItemInfo other) {
            return obtain(other.mRowIndex, other.mRowSpan, other.mColumnIndex, other.mColumnSpan, other.mHeading, other.mSelected);
        }

        public static CollectionItemInfo obtain(int rowIndex, int rowSpan, int columnIndex, int columnSpan, boolean heading) {
            return obtain(rowIndex, rowSpan, columnIndex, columnSpan, heading, false);
        }

        public static CollectionItemInfo obtain(int rowIndex, int rowSpan, int columnIndex, int columnSpan, boolean heading, boolean selected) {
            CollectionItemInfo info = (CollectionItemInfo) sPool.acquire();
            if (info == null) {
                return new CollectionItemInfo(rowIndex, rowSpan, columnIndex, columnSpan, heading, selected);
            }
            info.mRowIndex = rowIndex;
            info.mRowSpan = rowSpan;
            info.mColumnIndex = columnIndex;
            info.mColumnSpan = columnSpan;
            info.mHeading = heading;
            info.mSelected = selected;
            return info;
        }

        private CollectionItemInfo(int rowIndex, int rowSpan, int columnIndex, int columnSpan, boolean heading, boolean selected) {
            this.mRowIndex = rowIndex;
            this.mRowSpan = rowSpan;
            this.mColumnIndex = columnIndex;
            this.mColumnSpan = columnSpan;
            this.mHeading = heading;
            this.mSelected = selected;
        }

        public int getColumnIndex() {
            return this.mColumnIndex;
        }

        public int getRowIndex() {
            return this.mRowIndex;
        }

        public int getColumnSpan() {
            return this.mColumnSpan;
        }

        public int getRowSpan() {
            return this.mRowSpan;
        }

        public boolean isHeading() {
            return this.mHeading;
        }

        public boolean isSelected() {
            return this.mSelected;
        }

        void recycle() {
            clear();
            sPool.release(this);
        }

        private void clear() {
            this.mColumnIndex = 0;
            this.mColumnSpan = 0;
            this.mRowIndex = 0;
            this.mRowSpan = 0;
            this.mHeading = false;
            this.mSelected = false;
        }
    }

    public static final class RangeInfo {
        private static final int MAX_POOL_SIZE = 10;
        public static final int RANGE_TYPE_FLOAT = 1;
        public static final int RANGE_TYPE_INT = 0;
        public static final int RANGE_TYPE_PERCENT = 2;
        private static final SynchronizedPool<RangeInfo> sPool = new SynchronizedPool(10);
        private float mCurrent;
        private float mMax;
        private float mMin;
        private int mType;

        public static RangeInfo obtain(RangeInfo other) {
            return obtain(other.mType, other.mMin, other.mMax, other.mCurrent);
        }

        public static RangeInfo obtain(int type, float min, float max, float current) {
            RangeInfo info = (RangeInfo) sPool.acquire();
            if (info == null) {
                return new RangeInfo(type, min, max, current);
            }
            info.mType = type;
            info.mMin = min;
            info.mMax = max;
            info.mCurrent = current;
            return info;
        }

        private RangeInfo(int type, float min, float max, float current) {
            this.mType = type;
            this.mMin = min;
            this.mMax = max;
            this.mCurrent = current;
        }

        public int getType() {
            return this.mType;
        }

        public float getMin() {
            return this.mMin;
        }

        public float getMax() {
            return this.mMax;
        }

        public float getCurrent() {
            return this.mCurrent;
        }

        void recycle() {
            clear();
            sPool.release(this);
        }

        private void clear() {
            this.mType = 0;
            this.mMin = 0.0f;
            this.mMax = 0.0f;
            this.mCurrent = 0.0f;
        }
    }

    public static int getAccessibilityViewId(long accessibilityNodeId) {
        return (int) accessibilityNodeId;
    }

    public static int getVirtualDescendantId(long accessibilityNodeId) {
        return (int) ((VIRTUAL_DESCENDANT_ID_MASK & accessibilityNodeId) >> 32);
    }

    public static long makeNodeId(int accessibilityViewId, int virtualDescendantId) {
        return (((long) virtualDescendantId) << 32) | ((long) accessibilityViewId);
    }

    private AccessibilityNodeInfo() {
    }

    public void setSource(View source) {
        setSource(source, -1);
    }

    public void setSource(View root, int virtualDescendantId) {
        enforceNotSealed();
        int rootAccessibilityViewId = Integer.MAX_VALUE;
        this.mWindowId = root != null ? root.getAccessibilityWindowId() : Integer.MAX_VALUE;
        if (root != null) {
            rootAccessibilityViewId = root.getAccessibilityViewId();
        }
        this.mSourceNodeId = makeNodeId(rootAccessibilityViewId, virtualDescendantId);
    }

    public AccessibilityNodeInfo findFocus(int focus) {
        enforceSealed();
        enforceValidFocusType(focus);
        if (canPerformRequestOverConnection(this.mSourceNodeId)) {
            return AccessibilityInteractionClient.getInstance().findFocus(this.mConnectionId, this.mWindowId, this.mSourceNodeId, focus);
        }
        return null;
    }

    public AccessibilityNodeInfo focusSearch(int direction) {
        enforceSealed();
        enforceValidFocusDirection(direction);
        if (canPerformRequestOverConnection(this.mSourceNodeId)) {
            return AccessibilityInteractionClient.getInstance().focusSearch(this.mConnectionId, this.mWindowId, this.mSourceNodeId, direction);
        }
        return null;
    }

    public int getWindowId() {
        return this.mWindowId;
    }

    public boolean refresh(Bundle arguments, boolean bypassCache) {
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return false;
        }
        AccessibilityNodeInfo refreshedInfo = AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfoByAccessibilityId(this.mConnectionId, this.mWindowId, this.mSourceNodeId, bypassCache, 0, arguments);
        if (refreshedInfo == null) {
            return false;
        }
        enforceSealed();
        init(refreshedInfo);
        refreshedInfo.recycle();
        return true;
    }

    public boolean refresh() {
        return refresh(null, true);
    }

    public boolean refreshWithExtraData(String extraDataKey, Bundle args) {
        args.putString(EXTRA_DATA_REQUESTED_KEY, extraDataKey);
        return refresh(args, true);
    }

    public LongArray getChildNodeIds() {
        return this.mChildNodeIds;
    }

    public long getChildId(int index) {
        if (this.mChildNodeIds != null) {
            return this.mChildNodeIds.get(index);
        }
        throw new IndexOutOfBoundsException();
    }

    public int getChildCount() {
        return this.mChildNodeIds == null ? 0 : this.mChildNodeIds.size();
    }

    public AccessibilityNodeInfo getChild(int index) {
        enforceSealed();
        if (this.mChildNodeIds == null || !canPerformRequestOverConnection(this.mSourceNodeId)) {
            return null;
        }
        long childId = this.mChildNodeIds.get(index);
        return AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfoByAccessibilityId(this.mConnectionId, this.mWindowId, childId, false, 4, null);
    }

    public void addChild(View child) {
        addChildInternal(child, -1, true);
    }

    public void addChildUnchecked(View child) {
        addChildInternal(child, -1, false);
    }

    public boolean removeChild(View child) {
        return removeChild(child, -1);
    }

    public void addChild(View root, int virtualDescendantId) {
        addChildInternal(root, virtualDescendantId, true);
    }

    private void addChildInternal(View root, int virtualDescendantId, boolean checked) {
        enforceNotSealed();
        if (this.mChildNodeIds == null) {
            this.mChildNodeIds = new LongArray();
        }
        long childNodeId = makeNodeId(root != null ? root.getAccessibilityViewId() : Integer.MAX_VALUE, virtualDescendantId);
        if (!checked || this.mChildNodeIds.indexOf(childNodeId) < 0) {
            this.mChildNodeIds.add(childNodeId);
        }
    }

    public boolean removeChild(View root, int virtualDescendantId) {
        enforceNotSealed();
        LongArray childIds = this.mChildNodeIds;
        if (childIds == null) {
            return false;
        }
        int index = childIds.indexOf(makeNodeId(root != null ? root.getAccessibilityViewId() : Integer.MAX_VALUE, virtualDescendantId));
        if (index < 0) {
            return false;
        }
        childIds.remove(index);
        return true;
    }

    public List<AccessibilityAction> getActionList() {
        return CollectionUtils.emptyIfNull(this.mActions);
    }

    @Deprecated
    public int getActions() {
        int returnValue = 0;
        if (this.mActions == null) {
            return 0;
        }
        int actionSize = this.mActions.size();
        for (int i = 0; i < actionSize; i++) {
            int actionId = ((AccessibilityAction) this.mActions.get(i)).getId();
            if (actionId <= 2097152) {
                returnValue |= actionId;
            }
        }
        return returnValue;
    }

    public void addAction(AccessibilityAction action) {
        enforceNotSealed();
        addActionUnchecked(action);
    }

    private void addActionUnchecked(AccessibilityAction action) {
        if (action != null) {
            if (this.mActions == null) {
                this.mActions = new ArrayList();
            }
            this.mActions.remove(action);
            this.mActions.add(action);
        }
    }

    @Deprecated
    public void addAction(int action) {
        enforceNotSealed();
        if ((-16777216 & action) == 0) {
            addStandardActions((long) action);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Action is not a combination of the standard actions: ");
        stringBuilder.append(action);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @Deprecated
    public void removeAction(int action) {
        enforceNotSealed();
        removeAction(getActionSingleton(action));
    }

    public boolean removeAction(AccessibilityAction action) {
        enforceNotSealed();
        if (this.mActions == null || action == null) {
            return false;
        }
        return this.mActions.remove(action);
    }

    public void removeAllActions() {
        if (this.mActions != null) {
            this.mActions.clear();
        }
    }

    public AccessibilityNodeInfo getTraversalBefore() {
        enforceSealed();
        return getNodeForAccessibilityId(this.mTraversalBefore);
    }

    public void setTraversalBefore(View view) {
        setTraversalBefore(view, -1);
    }

    public void setTraversalBefore(View root, int virtualDescendantId) {
        enforceNotSealed();
        this.mTraversalBefore = makeNodeId(root != null ? root.getAccessibilityViewId() : Integer.MAX_VALUE, virtualDescendantId);
    }

    public AccessibilityNodeInfo getTraversalAfter() {
        enforceSealed();
        return getNodeForAccessibilityId(this.mTraversalAfter);
    }

    public void setTraversalAfter(View view) {
        setTraversalAfter(view, -1);
    }

    public void setTraversalAfter(View root, int virtualDescendantId) {
        enforceNotSealed();
        this.mTraversalAfter = makeNodeId(root != null ? root.getAccessibilityViewId() : Integer.MAX_VALUE, virtualDescendantId);
    }

    public List<String> getAvailableExtraData() {
        if (this.mExtraDataKeys != null) {
            return Collections.unmodifiableList(this.mExtraDataKeys);
        }
        return Collections.EMPTY_LIST;
    }

    public void setAvailableExtraData(List<String> extraDataKeys) {
        enforceNotSealed();
        this.mExtraDataKeys = new ArrayList(extraDataKeys);
    }

    public void setMaxTextLength(int max) {
        enforceNotSealed();
        this.mMaxTextLength = max;
    }

    public int getMaxTextLength() {
        return this.mMaxTextLength;
    }

    public void setMovementGranularities(int granularities) {
        enforceNotSealed();
        this.mMovementGranularities = granularities;
    }

    public int getMovementGranularities() {
        return this.mMovementGranularities;
    }

    public boolean performAction(int action) {
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return false;
        }
        return AccessibilityInteractionClient.getInstance().performAccessibilityAction(this.mConnectionId, this.mWindowId, this.mSourceNodeId, action, null);
    }

    public boolean performAction(int action, Bundle arguments) {
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return false;
        }
        return AccessibilityInteractionClient.getInstance().performAccessibilityAction(this.mConnectionId, this.mWindowId, this.mSourceNodeId, action, arguments);
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String text) {
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return Collections.emptyList();
        }
        return AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfosByText(this.mConnectionId, this.mWindowId, this.mSourceNodeId, text);
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewId(String viewId) {
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return Collections.emptyList();
        }
        return AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfosByViewId(this.mConnectionId, this.mWindowId, this.mSourceNodeId, viewId);
    }

    public AccessibilityWindowInfo getWindow() {
        enforceSealed();
        if (canPerformRequestOverConnection(this.mSourceNodeId)) {
            return AccessibilityInteractionClient.getInstance().getWindow(this.mConnectionId, this.mWindowId);
        }
        return null;
    }

    public AccessibilityNodeInfo getParent() {
        enforceSealed();
        return getNodeForAccessibilityId(this.mParentNodeId);
    }

    public long getParentNodeId() {
        return this.mParentNodeId;
    }

    public void setParent(View parent) {
        setParent(parent, -1);
    }

    public void setParent(View root, int virtualDescendantId) {
        enforceNotSealed();
        this.mParentNodeId = makeNodeId(root != null ? root.getAccessibilityViewId() : Integer.MAX_VALUE, virtualDescendantId);
    }

    public void getBoundsInParent(Rect outBounds) {
        outBounds.set(this.mBoundsInParent.left, this.mBoundsInParent.top, this.mBoundsInParent.right, this.mBoundsInParent.bottom);
    }

    public void setBoundsInParent(Rect bounds) {
        enforceNotSealed();
        this.mBoundsInParent.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    public void getBoundsInScreen(Rect outBounds) {
        outBounds.set(this.mBoundsInScreen.left, this.mBoundsInScreen.top, this.mBoundsInScreen.right, this.mBoundsInScreen.bottom);
    }

    public Rect getBoundsInScreen() {
        return this.mBoundsInScreen;
    }

    public void setBoundsInScreen(Rect bounds) {
        enforceNotSealed();
        this.mBoundsInScreen.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    public boolean isCheckable() {
        return getBooleanProperty(1);
    }

    public void setCheckable(boolean checkable) {
        setBooleanProperty(1, checkable);
    }

    public boolean isChecked() {
        return getBooleanProperty(2);
    }

    public void setChecked(boolean checked) {
        setBooleanProperty(2, checked);
    }

    public boolean isFocusable() {
        return getBooleanProperty(4);
    }

    public void setFocusable(boolean focusable) {
        setBooleanProperty(4, focusable);
    }

    public boolean isFocused() {
        return getBooleanProperty(8);
    }

    public void setFocused(boolean focused) {
        setBooleanProperty(8, focused);
    }

    public boolean isVisibleToUser() {
        return getBooleanProperty(2048);
    }

    public void setVisibleToUser(boolean visibleToUser) {
        setBooleanProperty(2048, visibleToUser);
    }

    public boolean isAccessibilityFocused() {
        return getBooleanProperty(1024);
    }

    public void setAccessibilityFocused(boolean focused) {
        setBooleanProperty(1024, focused);
    }

    public boolean isSelected() {
        return getBooleanProperty(16);
    }

    public void setSelected(boolean selected) {
        setBooleanProperty(16, selected);
    }

    public boolean isClickable() {
        return getBooleanProperty(32);
    }

    public void setClickable(boolean clickable) {
        setBooleanProperty(32, clickable);
    }

    public boolean isLongClickable() {
        return getBooleanProperty(64);
    }

    public void setLongClickable(boolean longClickable) {
        setBooleanProperty(64, longClickable);
    }

    public boolean isEnabled() {
        return getBooleanProperty(128);
    }

    public void setEnabled(boolean enabled) {
        setBooleanProperty(128, enabled);
    }

    public boolean isPassword() {
        return getBooleanProperty(256);
    }

    public void setPassword(boolean password) {
        setBooleanProperty(256, password);
    }

    public boolean isScrollable() {
        return getBooleanProperty(512);
    }

    public void setScrollable(boolean scrollable) {
        setBooleanProperty(512, scrollable);
    }

    public boolean isEditable() {
        return getBooleanProperty(4096);
    }

    public void setEditable(boolean editable) {
        setBooleanProperty(4096, editable);
    }

    public void setPaneTitle(CharSequence paneTitle) {
        enforceNotSealed();
        this.mPaneTitle = paneTitle == null ? null : paneTitle.subSequence(0, paneTitle.length());
    }

    public CharSequence getPaneTitle() {
        return this.mPaneTitle;
    }

    public int getDrawingOrder() {
        return this.mDrawingOrderInParent;
    }

    public void setDrawingOrder(int drawingOrderInParent) {
        enforceNotSealed();
        this.mDrawingOrderInParent = drawingOrderInParent;
    }

    public CollectionInfo getCollectionInfo() {
        return this.mCollectionInfo;
    }

    public void setCollectionInfo(CollectionInfo collectionInfo) {
        enforceNotSealed();
        this.mCollectionInfo = collectionInfo;
    }

    public CollectionItemInfo getCollectionItemInfo() {
        return this.mCollectionItemInfo;
    }

    public void setCollectionItemInfo(CollectionItemInfo collectionItemInfo) {
        enforceNotSealed();
        this.mCollectionItemInfo = collectionItemInfo;
    }

    public RangeInfo getRangeInfo() {
        return this.mRangeInfo;
    }

    public void setRangeInfo(RangeInfo rangeInfo) {
        enforceNotSealed();
        this.mRangeInfo = rangeInfo;
    }

    public boolean isContentInvalid() {
        return getBooleanProperty(65536);
    }

    public void setContentInvalid(boolean contentInvalid) {
        setBooleanProperty(65536, contentInvalid);
    }

    public boolean isContextClickable() {
        return getBooleanProperty(131072);
    }

    public void setContextClickable(boolean contextClickable) {
        setBooleanProperty(131072, contextClickable);
    }

    public int getLiveRegion() {
        return this.mLiveRegion;
    }

    public void setLiveRegion(int mode) {
        enforceNotSealed();
        this.mLiveRegion = mode;
    }

    public boolean isMultiLine() {
        return getBooleanProperty(32768);
    }

    public void setMultiLine(boolean multiLine) {
        setBooleanProperty(32768, multiLine);
    }

    public boolean canOpenPopup() {
        return getBooleanProperty(8192);
    }

    public void setCanOpenPopup(boolean opensPopup) {
        enforceNotSealed();
        setBooleanProperty(8192, opensPopup);
    }

    public boolean isDismissable() {
        return getBooleanProperty(16384);
    }

    public void setDismissable(boolean dismissable) {
        setBooleanProperty(16384, dismissable);
    }

    public boolean isImportantForAccessibility() {
        return getBooleanProperty(262144);
    }

    public void setImportantForAccessibility(boolean important) {
        setBooleanProperty(262144, important);
    }

    public boolean isScreenReaderFocusable() {
        return getBooleanProperty(524288);
    }

    public void setScreenReaderFocusable(boolean screenReaderFocusable) {
        setBooleanProperty(524288, screenReaderFocusable);
    }

    public boolean isShowingHintText() {
        return getBooleanProperty(1048576);
    }

    public void setShowingHintText(boolean showingHintText) {
        setBooleanProperty(1048576, showingHintText);
    }

    public boolean isHeading() {
        boolean z = true;
        if (getBooleanProperty(2097152)) {
            return true;
        }
        CollectionItemInfo itemInfo = getCollectionItemInfo();
        if (itemInfo == null || !itemInfo.mHeading) {
            z = false;
        }
        return z;
    }

    public void setHeading(boolean isHeading) {
        setBooleanProperty(2097152, isHeading);
    }

    public CharSequence getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(CharSequence packageName) {
        enforceNotSealed();
        this.mPackageName = packageName;
    }

    public CharSequence getClassName() {
        return this.mClassName;
    }

    public void setClassName(CharSequence className) {
        enforceNotSealed();
        this.mClassName = className;
    }

    public CharSequence getText() {
        if (this.mText instanceof Spanned) {
            Spanned spanned = this.mText;
            int i = 0;
            AccessibilityClickableSpan[] clickableSpans = (AccessibilityClickableSpan[]) spanned.getSpans(0, this.mText.length(), AccessibilityClickableSpan.class);
            for (AccessibilityClickableSpan copyConnectionDataFrom : clickableSpans) {
                copyConnectionDataFrom.copyConnectionDataFrom(this);
            }
            AccessibilityURLSpan[] urlSpans = (AccessibilityURLSpan[]) spanned.getSpans(0, this.mText.length(), AccessibilityURLSpan.class);
            while (i < urlSpans.length) {
                urlSpans[i].copyConnectionDataFrom(this);
                i++;
            }
        }
        return this.mText;
    }

    public CharSequence getOriginalText() {
        return this.mOriginalText;
    }

    public void setText(CharSequence text) {
        enforceNotSealed();
        this.mOriginalText = text;
        int i = 0;
        if (text instanceof Spanned) {
            ClickableSpan[] spans = (ClickableSpan[]) ((Spanned) text).getSpans(0, text.length(), ClickableSpan.class);
            if (spans.length > 0) {
                Spannable spannable = new SpannableStringBuilder(text);
                while (i < spans.length) {
                    ClickableSpan span = spans[i];
                    if ((span instanceof AccessibilityClickableSpan) || (span instanceof AccessibilityURLSpan)) {
                        break;
                    }
                    ClickableSpan replacementSpan;
                    int spanToReplaceStart = spannable.getSpanStart(span);
                    int spanToReplaceEnd = spannable.getSpanEnd(span);
                    int spanToReplaceFlags = spannable.getSpanFlags(span);
                    spannable.removeSpan(span);
                    if (span instanceof URLSpan) {
                        replacementSpan = new AccessibilityURLSpan((URLSpan) span);
                    } else {
                        replacementSpan = new AccessibilityClickableSpan(span.getId());
                    }
                    spannable.setSpan(replacementSpan, spanToReplaceStart, spanToReplaceEnd, spanToReplaceFlags);
                    i++;
                }
                this.mText = spannable;
                return;
            }
        }
        this.mText = text == null ? null : text.subSequence(0, text.length());
    }

    public CharSequence getHintText() {
        return this.mHintText;
    }

    public void setHintText(CharSequence hintText) {
        enforceNotSealed();
        this.mHintText = hintText == null ? null : hintText.subSequence(0, hintText.length());
    }

    public void setError(CharSequence error) {
        enforceNotSealed();
        this.mError = error == null ? null : error.subSequence(0, error.length());
    }

    public CharSequence getError() {
        return this.mError;
    }

    public CharSequence getContentDescription() {
        return this.mContentDescription;
    }

    public void setContentDescription(CharSequence contentDescription) {
        CharSequence charSequence;
        enforceNotSealed();
        if (contentDescription == null) {
            charSequence = null;
        } else {
            charSequence = contentDescription.subSequence(0, contentDescription.length());
        }
        this.mContentDescription = charSequence;
    }

    public CharSequence getTooltipText() {
        return this.mTooltipText;
    }

    public void setTooltipText(CharSequence tooltipText) {
        CharSequence charSequence;
        enforceNotSealed();
        if (tooltipText == null) {
            charSequence = null;
        } else {
            charSequence = tooltipText.subSequence(0, tooltipText.length());
        }
        this.mTooltipText = charSequence;
    }

    public void setLabelFor(View labeled) {
        setLabelFor(labeled, -1);
    }

    public void setLabelFor(View root, int virtualDescendantId) {
        enforceNotSealed();
        this.mLabelForId = makeNodeId(root != null ? root.getAccessibilityViewId() : Integer.MAX_VALUE, virtualDescendantId);
    }

    public AccessibilityNodeInfo getLabelFor() {
        enforceSealed();
        return getNodeForAccessibilityId(this.mLabelForId);
    }

    public void setLabeledBy(View label) {
        setLabeledBy(label, -1);
    }

    public void setLabeledBy(View root, int virtualDescendantId) {
        enforceNotSealed();
        this.mLabeledById = makeNodeId(root != null ? root.getAccessibilityViewId() : Integer.MAX_VALUE, virtualDescendantId);
    }

    public AccessibilityNodeInfo getLabeledBy() {
        enforceSealed();
        return getNodeForAccessibilityId(this.mLabeledById);
    }

    public void setViewIdResourceName(String viewIdResName) {
        enforceNotSealed();
        this.mViewIdResourceName = viewIdResName;
    }

    public String getViewIdResourceName() {
        return this.mViewIdResourceName;
    }

    public int getTextSelectionStart() {
        return this.mTextSelectionStart;
    }

    public int getTextSelectionEnd() {
        return this.mTextSelectionEnd;
    }

    public void setTextSelection(int start, int end) {
        enforceNotSealed();
        this.mTextSelectionStart = start;
        this.mTextSelectionEnd = end;
    }

    public int getInputType() {
        return this.mInputType;
    }

    public void setInputType(int inputType) {
        enforceNotSealed();
        this.mInputType = inputType;
    }

    public Bundle getExtras() {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        return this.mExtras;
    }

    public boolean hasExtras() {
        return this.mExtras != null;
    }

    private boolean getBooleanProperty(int property) {
        return (this.mBooleanProperties & property) != 0;
    }

    private void setBooleanProperty(int property, boolean value) {
        enforceNotSealed();
        if (value) {
            this.mBooleanProperties |= property;
        } else {
            this.mBooleanProperties &= ~property;
        }
    }

    public void setConnectionId(int connectionId) {
        enforceNotSealed();
        this.mConnectionId = connectionId;
    }

    public int getConnectionId() {
        return this.mConnectionId;
    }

    public int describeContents() {
        return 0;
    }

    public void setSourceNodeId(long sourceId, int windowId) {
        enforceNotSealed();
        this.mSourceNodeId = sourceId;
        this.mWindowId = windowId;
    }

    public long getSourceNodeId() {
        return this.mSourceNodeId;
    }

    public void setSealed(boolean sealed) {
        this.mSealed = sealed;
    }

    public boolean isSealed() {
        return this.mSealed;
    }

    protected void enforceSealed() {
        if (!isSealed()) {
            throw new IllegalStateException("Cannot perform this action on a not sealed instance.");
        }
    }

    private void enforceValidFocusDirection(int direction) {
        if (direction != 17 && direction != 33 && direction != 66 && direction != 130) {
            switch (direction) {
                case 1:
                case 2:
                    return;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown direction: ");
                    stringBuilder.append(direction);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    private void enforceValidFocusType(int focusType) {
        switch (focusType) {
            case 1:
            case 2:
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown focus type: ");
                stringBuilder.append(focusType);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    protected void enforceNotSealed() {
        if (isSealed()) {
            throw new IllegalStateException("Cannot perform this action on a sealed instance.");
        }
    }

    public static AccessibilityNodeInfo obtain(View source) {
        AccessibilityNodeInfo info = obtain();
        info.setSource(source);
        return info;
    }

    public static AccessibilityNodeInfo obtain(View root, int virtualDescendantId) {
        AccessibilityNodeInfo info = obtain();
        info.setSource(root, virtualDescendantId);
        return info;
    }

    public static AccessibilityNodeInfo obtain() {
        AccessibilityNodeInfo info = (AccessibilityNodeInfo) sPool.acquire();
        if (sNumInstancesInUse != null) {
            sNumInstancesInUse.incrementAndGet();
        }
        return info != null ? info : new AccessibilityNodeInfo();
    }

    public static AccessibilityNodeInfo obtain(AccessibilityNodeInfo info) {
        AccessibilityNodeInfo infoClone = obtain();
        infoClone.init(info);
        return infoClone;
    }

    public void recycle() {
        clear();
        sPool.release(this);
        if (sNumInstancesInUse != null) {
            sNumInstancesInUse.decrementAndGet();
        }
    }

    public static void setNumInstancesInUseCounter(AtomicInteger counter) {
        sNumInstancesInUse = counter;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        writeToParcelNoRecycle(parcel, flags);
        recycle();
    }

    public void writeToParcelNoRecycle(Parcel parcel, int flags) {
        int childIdsSize;
        long nonDefaultFields = 0;
        if (isSealed() != DEFAULT.isSealed()) {
            nonDefaultFields = 0 | BitUtils.bitAt(0);
        }
        int fieldIndex = 0 + 1;
        if (this.mSourceNodeId != DEFAULT.mSourceNodeId) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mWindowId != DEFAULT.mWindowId) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mParentNodeId != DEFAULT.mParentNodeId) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mLabelForId != DEFAULT.mLabelForId) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mLabeledById != DEFAULT.mLabeledById) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mTraversalBefore != DEFAULT.mTraversalBefore) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mTraversalAfter != DEFAULT.mTraversalAfter) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mConnectionId != DEFAULT.mConnectionId) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!LongArray.elementsEqual(this.mChildNodeIds, DEFAULT.mChildNodeIds)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mBoundsInParent, DEFAULT.mBoundsInParent)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mBoundsInScreen, DEFAULT.mBoundsInScreen)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mActions, DEFAULT.mActions)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mMaxTextLength != DEFAULT.mMaxTextLength) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mMovementGranularities != DEFAULT.mMovementGranularities) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mBooleanProperties != DEFAULT.mBooleanProperties) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mPackageName, DEFAULT.mPackageName)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mClassName, DEFAULT.mClassName)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mText, DEFAULT.mText)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mHintText, DEFAULT.mHintText)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mError, DEFAULT.mError)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mContentDescription, DEFAULT.mContentDescription)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mPaneTitle, DEFAULT.mPaneTitle)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mTooltipText, DEFAULT.mTooltipText)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mViewIdResourceName, DEFAULT.mViewIdResourceName)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mTextSelectionStart != DEFAULT.mTextSelectionStart) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mTextSelectionEnd != DEFAULT.mTextSelectionEnd) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mInputType != DEFAULT.mInputType) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mLiveRegion != DEFAULT.mLiveRegion) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (this.mDrawingOrderInParent != DEFAULT.mDrawingOrderInParent) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mExtraDataKeys, DEFAULT.mExtraDataKeys)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mExtras, DEFAULT.mExtras)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mRangeInfo, DEFAULT.mRangeInfo)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mCollectionInfo, DEFAULT.mCollectionInfo)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        fieldIndex++;
        if (!Objects.equals(this.mCollectionItemInfo, DEFAULT.mCollectionItemInfo)) {
            nonDefaultFields |= BitUtils.bitAt(fieldIndex);
        }
        int totalFields = fieldIndex;
        parcel.writeLong(nonDefaultFields);
        int fieldIndex2 = 0 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, 0) != 0) {
            parcel.writeInt(isSealed());
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeLong(this.mSourceNodeId);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeInt(this.mWindowId);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeLong(this.mParentNodeId);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeLong(this.mLabelForId);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeLong(this.mLabeledById);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeLong(this.mTraversalBefore);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeLong(this.mTraversalAfter);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeInt(this.mConnectionId);
        }
        fieldIndex = fieldIndex2 + 1;
        int i = 0;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2)) {
            LongArray childIds = this.mChildNodeIds;
            if (childIds == null) {
                parcel.writeInt(0);
            } else {
                childIdsSize = childIds.size();
                parcel.writeInt(childIdsSize);
                for (int i2 = 0; i2 < childIdsSize; i2++) {
                    parcel.writeLong(childIds.get(i2));
                }
            }
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeInt(this.mBoundsInParent.top);
            parcel.writeInt(this.mBoundsInParent.bottom);
            parcel.writeInt(this.mBoundsInParent.left);
            parcel.writeInt(this.mBoundsInParent.right);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeInt(this.mBoundsInScreen.top);
            parcel.writeInt(this.mBoundsInScreen.bottom);
            parcel.writeInt(this.mBoundsInScreen.left);
            parcel.writeInt(this.mBoundsInScreen.right);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            if (this.mActions == null || this.mActions.isEmpty()) {
                parcel.writeLong(0);
                parcel.writeInt(0);
            } else {
                fieldIndex = this.mActions.size();
                long defaultStandardActions = 0;
                int nonStandardActionCount = 0;
                for (childIdsSize = 0; childIdsSize < fieldIndex; childIdsSize++) {
                    AccessibilityAction action = (AccessibilityAction) this.mActions.get(childIdsSize);
                    if (isDefaultStandardAction(action)) {
                        defaultStandardActions |= action.mSerializationFlag;
                    } else {
                        nonStandardActionCount++;
                    }
                }
                parcel.writeLong(defaultStandardActions);
                parcel.writeInt(nonStandardActionCount);
                while (i < fieldIndex) {
                    AccessibilityAction action2 = (AccessibilityAction) this.mActions.get(i);
                    if (!isDefaultStandardAction(action2)) {
                        parcel.writeInt(action2.getId());
                        parcel.writeCharSequence(action2.getLabel());
                    }
                    i++;
                }
            }
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeInt(this.mMaxTextLength);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeInt(this.mMovementGranularities);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeInt(this.mBooleanProperties);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeCharSequence(this.mPackageName);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeCharSequence(this.mClassName);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeCharSequence(this.mText);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeCharSequence(this.mHintText);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeCharSequence(this.mError);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeCharSequence(this.mContentDescription);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeCharSequence(this.mPaneTitle);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeCharSequence(this.mTooltipText);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeString(this.mViewIdResourceName);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeInt(this.mTextSelectionStart);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeInt(this.mTextSelectionEnd);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeInt(this.mInputType);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeInt(this.mLiveRegion);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeInt(this.mDrawingOrderInParent);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeStringList(this.mExtraDataKeys);
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeBundle(this.mExtras);
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeInt(this.mRangeInfo.getType());
            parcel.writeFloat(this.mRangeInfo.getMin());
            parcel.writeFloat(this.mRangeInfo.getMax());
            parcel.writeFloat(this.mRangeInfo.getCurrent());
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            parcel.writeInt(this.mCollectionInfo.getRowCount());
            parcel.writeInt(this.mCollectionInfo.getColumnCount());
            parcel.writeInt(this.mCollectionInfo.isHierarchical());
            parcel.writeInt(this.mCollectionInfo.getSelectionMode());
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            parcel.writeInt(this.mCollectionItemInfo.getRowIndex());
            parcel.writeInt(this.mCollectionItemInfo.getRowSpan());
            parcel.writeInt(this.mCollectionItemInfo.getColumnIndex());
            parcel.writeInt(this.mCollectionItemInfo.getColumnSpan());
            parcel.writeInt(this.mCollectionItemInfo.isHeading());
            parcel.writeInt(this.mCollectionItemInfo.isSelected());
        }
    }

    private void init(AccessibilityNodeInfo other) {
        this.mSealed = other.mSealed;
        this.mSourceNodeId = other.mSourceNodeId;
        this.mParentNodeId = other.mParentNodeId;
        this.mLabelForId = other.mLabelForId;
        this.mLabeledById = other.mLabeledById;
        this.mTraversalBefore = other.mTraversalBefore;
        this.mTraversalAfter = other.mTraversalAfter;
        this.mWindowId = other.mWindowId;
        this.mConnectionId = other.mConnectionId;
        this.mBoundsInParent.set(other.mBoundsInParent);
        this.mBoundsInScreen.set(other.mBoundsInScreen);
        this.mPackageName = other.mPackageName;
        this.mClassName = other.mClassName;
        this.mText = other.mText;
        this.mOriginalText = other.mOriginalText;
        this.mHintText = other.mHintText;
        this.mError = other.mError;
        this.mContentDescription = other.mContentDescription;
        this.mPaneTitle = other.mPaneTitle;
        this.mTooltipText = other.mTooltipText;
        this.mViewIdResourceName = other.mViewIdResourceName;
        if (this.mActions != null) {
            this.mActions.clear();
        }
        ArrayList<AccessibilityAction> otherActions = other.mActions;
        if (otherActions != null && otherActions.size() > 0) {
            if (this.mActions == null) {
                this.mActions = new ArrayList(otherActions);
            } else {
                this.mActions.addAll(other.mActions);
            }
        }
        this.mBooleanProperties = other.mBooleanProperties;
        this.mMaxTextLength = other.mMaxTextLength;
        this.mMovementGranularities = other.mMovementGranularities;
        if (this.mChildNodeIds != null) {
            this.mChildNodeIds.clear();
        }
        LongArray otherChildNodeIds = other.mChildNodeIds;
        if (otherChildNodeIds != null && otherChildNodeIds.size() > 0) {
            if (this.mChildNodeIds == null) {
                this.mChildNodeIds = otherChildNodeIds.clone();
            } else {
                this.mChildNodeIds.addAll(otherChildNodeIds);
            }
        }
        this.mTextSelectionStart = other.mTextSelectionStart;
        this.mTextSelectionEnd = other.mTextSelectionEnd;
        this.mInputType = other.mInputType;
        this.mLiveRegion = other.mLiveRegion;
        this.mDrawingOrderInParent = other.mDrawingOrderInParent;
        this.mExtraDataKeys = other.mExtraDataKeys;
        CollectionItemInfo collectionItemInfo = null;
        this.mExtras = other.mExtras != null ? new Bundle(other.mExtras) : null;
        if (this.mRangeInfo != null) {
            this.mRangeInfo.recycle();
        }
        this.mRangeInfo = other.mRangeInfo != null ? RangeInfo.obtain(other.mRangeInfo) : null;
        if (this.mCollectionInfo != null) {
            this.mCollectionInfo.recycle();
        }
        this.mCollectionInfo = other.mCollectionInfo != null ? CollectionInfo.obtain(other.mCollectionInfo) : null;
        if (this.mCollectionItemInfo != null) {
            this.mCollectionItemInfo.recycle();
        }
        if (other.mCollectionItemInfo != null) {
            collectionItemInfo = CollectionItemInfo.obtain(other.mCollectionItemInfo);
        }
        this.mCollectionItemInfo = collectionItemInfo;
    }

    private void initFromParcel(Parcel parcel) {
        ArrayList createStringArrayList;
        Bundle readBundle;
        RangeInfo obtain;
        CollectionInfo obtain2;
        long nonDefaultFields = parcel.readLong();
        int fieldIndex = 0 + 1;
        boolean sealed = BitUtils.isBitSet(nonDefaultFields, 0) ? parcel.readInt() == 1 : DEFAULT.mSealed;
        int fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mSourceNodeId = parcel.readLong();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mWindowId = parcel.readInt();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mParentNodeId = parcel.readLong();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mLabelForId = parcel.readLong();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mLabeledById = parcel.readLong();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mTraversalBefore = parcel.readLong();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mTraversalAfter = parcel.readLong();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mConnectionId = parcel.readInt();
        }
        fieldIndex2 = fieldIndex + 1;
        CollectionItemInfo collectionItemInfo = null;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex)) {
            fieldIndex = parcel.readInt();
            if (fieldIndex <= 0) {
                this.mChildNodeIds = null;
            } else {
                this.mChildNodeIds = new LongArray(fieldIndex);
                for (int i = 0; i < fieldIndex; i++) {
                    this.mChildNodeIds.add(parcel.readLong());
                }
            }
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mBoundsInParent.top = parcel.readInt();
            this.mBoundsInParent.bottom = parcel.readInt();
            this.mBoundsInParent.left = parcel.readInt();
            this.mBoundsInParent.right = parcel.readInt();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mBoundsInScreen.top = parcel.readInt();
            this.mBoundsInScreen.bottom = parcel.readInt();
            this.mBoundsInScreen.left = parcel.readInt();
            this.mBoundsInScreen.right = parcel.readInt();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            addStandardActions(parcel.readLong());
            fieldIndex2 = parcel.readInt();
            for (int i2 = 0; i2 < fieldIndex2; i2++) {
                addActionUnchecked(new AccessibilityAction(parcel.readInt(), parcel.readCharSequence()));
            }
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mMaxTextLength = parcel.readInt();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mMovementGranularities = parcel.readInt();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mBooleanProperties = parcel.readInt();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mPackageName = parcel.readCharSequence();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mClassName = parcel.readCharSequence();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mText = parcel.readCharSequence();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mHintText = parcel.readCharSequence();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mError = parcel.readCharSequence();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mContentDescription = parcel.readCharSequence();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mPaneTitle = parcel.readCharSequence();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mTooltipText = parcel.readCharSequence();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mViewIdResourceName = parcel.readString();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mTextSelectionStart = parcel.readInt();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mTextSelectionEnd = parcel.readInt();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mInputType = parcel.readInt();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            this.mLiveRegion = parcel.readInt();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            this.mDrawingOrderInParent = parcel.readInt();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            createStringArrayList = parcel.createStringArrayList();
        } else {
            createStringArrayList = null;
        }
        this.mExtraDataKeys = createStringArrayList;
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            readBundle = parcel.readBundle();
        } else {
            readBundle = null;
        }
        this.mExtras = readBundle;
        if (this.mRangeInfo != null) {
            this.mRangeInfo.recycle();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            obtain = RangeInfo.obtain(parcel.readInt(), parcel.readFloat(), parcel.readFloat(), parcel.readFloat());
        } else {
            obtain = null;
        }
        this.mRangeInfo = obtain;
        if (this.mCollectionInfo != null) {
            this.mCollectionInfo.recycle();
        }
        fieldIndex2 = fieldIndex + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex) != 0) {
            obtain2 = CollectionInfo.obtain(parcel.readInt(), parcel.readInt(), parcel.readInt() == 1, parcel.readInt());
        } else {
            obtain2 = null;
        }
        this.mCollectionInfo = obtain2;
        if (this.mCollectionItemInfo != null) {
            this.mCollectionItemInfo.recycle();
        }
        fieldIndex = fieldIndex2 + 1;
        if (BitUtils.isBitSet(nonDefaultFields, fieldIndex2) != 0) {
            collectionItemInfo = CollectionItemInfo.obtain(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt() == 1, parcel.readInt() == 1);
        }
        this.mCollectionItemInfo = collectionItemInfo;
        this.mSealed = sealed;
    }

    private void clear() {
        init(DEFAULT);
    }

    private static boolean isDefaultStandardAction(AccessibilityAction action) {
        return action.mSerializationFlag != -1 && TextUtils.isEmpty(action.getLabel());
    }

    private static AccessibilityAction getActionSingleton(int actionId) {
        int actions = AccessibilityAction.sStandardActions.size();
        for (int i = 0; i < actions; i++) {
            AccessibilityAction currentAction = (AccessibilityAction) AccessibilityAction.sStandardActions.valueAt(i);
            if (actionId == currentAction.getId()) {
                return currentAction;
            }
        }
        return null;
    }

    private static AccessibilityAction getActionSingletonBySerializationFlag(long flag) {
        int actions = AccessibilityAction.sStandardActions.size();
        for (int i = 0; i < actions; i++) {
            AccessibilityAction currentAction = (AccessibilityAction) AccessibilityAction.sStandardActions.valueAt(i);
            if (flag == currentAction.mSerializationFlag) {
                return currentAction;
            }
        }
        return null;
    }

    private void addStandardActions(long serializationIdMask) {
        long remainingIds = serializationIdMask;
        while (remainingIds > 0) {
            long id = 1 << Long.numberOfTrailingZeros(remainingIds);
            remainingIds &= ~id;
            addAction(getActionSingletonBySerializationFlag(id));
        }
    }

    private static String getActionSymbolicName(int action) {
        switch (action) {
            case 1:
                return "ACTION_FOCUS";
            case 2:
                return "ACTION_CLEAR_FOCUS";
            default:
                switch (action) {
                    case 16908342:
                        return "ACTION_SHOW_ON_SCREEN";
                    case 16908343:
                        return "ACTION_SCROLL_TO_POSITION";
                    case 16908344:
                        return "ACTION_SCROLL_UP";
                    case 16908345:
                        return "ACTION_SCROLL_LEFT";
                    case 16908346:
                        return "ACTION_SCROLL_DOWN";
                    case 16908347:
                        return "ACTION_SCROLL_RIGHT";
                    case 16908348:
                        return "ACTION_CONTEXT_CLICK";
                    case 16908349:
                        return "ACTION_SET_PROGRESS";
                    default:
                        switch (action) {
                            case 16908356:
                                return "ACTION_SHOW_TOOLTIP";
                            case 16908357:
                                return "ACTION_HIDE_TOOLTIP";
                            default:
                                switch (action) {
                                    case 4:
                                        return "ACTION_SELECT";
                                    case 8:
                                        return "ACTION_CLEAR_SELECTION";
                                    case 16:
                                        return "ACTION_CLICK";
                                    case 32:
                                        return "ACTION_LONG_CLICK";
                                    case 64:
                                        return "ACTION_ACCESSIBILITY_FOCUS";
                                    case 128:
                                        return "ACTION_CLEAR_ACCESSIBILITY_FOCUS";
                                    case 256:
                                        return "ACTION_NEXT_AT_MOVEMENT_GRANULARITY";
                                    case 512:
                                        return "ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY";
                                    case 1024:
                                        return "ACTION_NEXT_HTML_ELEMENT";
                                    case 2048:
                                        return "ACTION_PREVIOUS_HTML_ELEMENT";
                                    case 4096:
                                        return "ACTION_SCROLL_FORWARD";
                                    case 8192:
                                        return "ACTION_SCROLL_BACKWARD";
                                    case 16384:
                                        return "ACTION_COPY";
                                    case 32768:
                                        return "ACTION_PASTE";
                                    case 65536:
                                        return "ACTION_CUT";
                                    case 131072:
                                        return "ACTION_SET_SELECTION";
                                    case 262144:
                                        return "ACTION_EXPAND";
                                    case 524288:
                                        return "ACTION_COLLAPSE";
                                    case 1048576:
                                        return "ACTION_DISMISS";
                                    case 2097152:
                                        return "ACTION_SET_TEXT";
                                    default:
                                        return "ACTION_UNKNOWN";
                                }
                        }
                }
        }
    }

    private static String getMovementGranularitySymbolicName(int granularity) {
        if (granularity == 4) {
            return "MOVEMENT_GRANULARITY_LINE";
        }
        if (granularity == 8) {
            return "MOVEMENT_GRANULARITY_PARAGRAPH";
        }
        if (granularity == 16) {
            return "MOVEMENT_GRANULARITY_PAGE";
        }
        switch (granularity) {
            case 1:
                return "MOVEMENT_GRANULARITY_CHARACTER";
            case 2:
                return "MOVEMENT_GRANULARITY_WORD";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown movement granularity: ");
                stringBuilder.append(granularity);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private boolean canPerformRequestOverConnection(long accessibilityNodeId) {
        return (this.mWindowId == -1 || getAccessibilityViewId(accessibilityNodeId) == Integer.MAX_VALUE || this.mConnectionId == -1) ? false : true;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        AccessibilityNodeInfo other = (AccessibilityNodeInfo) object;
        if (this.mSourceNodeId == other.mSourceNodeId && this.mWindowId == other.mWindowId) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * 1) + getAccessibilityViewId(this.mSourceNodeId))) + getVirtualDescendantId(this.mSourceNodeId))) + this.mWindowId;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append("; boundsInParent: ");
        builder.append(this.mBoundsInParent);
        builder.append("; boundsInScreen: ");
        builder.append(this.mBoundsInScreen);
        builder.append("; packageName: ");
        builder.append(this.mPackageName);
        builder.append("; className: ");
        builder.append(this.mClassName);
        builder.append("; text: ");
        builder.append(this.mText);
        builder.append("; error: ");
        builder.append(this.mError);
        builder.append("; maxTextLength: ");
        builder.append(this.mMaxTextLength);
        builder.append("; contentDescription: ");
        builder.append(this.mContentDescription);
        builder.append("; tooltipText: ");
        builder.append(this.mTooltipText);
        builder.append("; viewIdResName: ");
        builder.append(this.mViewIdResourceName);
        builder.append("; checkable: ");
        builder.append(isCheckable());
        builder.append("; checked: ");
        builder.append(isChecked());
        builder.append("; focusable: ");
        builder.append(isFocusable());
        builder.append("; focused: ");
        builder.append(isFocused());
        builder.append("; selected: ");
        builder.append(isSelected());
        builder.append("; clickable: ");
        builder.append(isClickable());
        builder.append("; longClickable: ");
        builder.append(isLongClickable());
        builder.append("; contextClickable: ");
        builder.append(isContextClickable());
        builder.append("; enabled: ");
        builder.append(isEnabled());
        builder.append("; password: ");
        builder.append(isPassword());
        builder.append("; scrollable: ");
        builder.append(isScrollable());
        builder.append("; importantForAccessibility: ");
        builder.append(isImportantForAccessibility());
        builder.append("; visible: ");
        builder.append(isVisibleToUser());
        builder.append("; actions: ");
        builder.append(this.mActions);
        return builder.toString();
    }

    private AccessibilityNodeInfo getNodeForAccessibilityId(long accessibilityId) {
        if (!canPerformRequestOverConnection(accessibilityId)) {
            return null;
        }
        return AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfoByAccessibilityId(this.mConnectionId, this.mWindowId, accessibilityId, false, 7, null);
    }

    public static String idToString(long accessibilityId) {
        int accessibilityViewId = getAccessibilityViewId(accessibilityId);
        int virtualDescendantId = getVirtualDescendantId(accessibilityId);
        if (virtualDescendantId == -1) {
            return idItemToString(accessibilityViewId);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(idItemToString(accessibilityViewId));
        stringBuilder.append(SettingsStringUtil.DELIMITER);
        stringBuilder.append(idItemToString(virtualDescendantId));
        return stringBuilder.toString();
    }

    private static String idItemToString(int item) {
        if (item == -1) {
            return "HOST";
        }
        switch (item) {
            case 2147483646:
                return "ROOT";
            case Integer.MAX_VALUE:
                return "UNDEFINED";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(item);
                return stringBuilder.toString();
        }
    }
}
