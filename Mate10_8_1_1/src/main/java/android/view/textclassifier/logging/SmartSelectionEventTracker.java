package android.view.textclassifier.logging;

import android.content.Context;
import android.metrics.LogMaker;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextSelection;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.Preconditions;
import java.util.Objects;
import java.util.UUID;

public final class SmartSelectionEventTracker {
    private static final boolean DEBUG_LOG_ENABLED = false;
    private static final String EDITTEXT = "edittext";
    private static final String EDIT_WEBVIEW = "edit-webview";
    private static final int EVENT_INDICES = 1122;
    private static final int INDEX = 1120;
    private static final String LOG_TAG = "SmartSelectEventTracker";
    private static final int PREV_EVENT_DELTA = 1118;
    private static final int SESSION_ID = 1119;
    private static final int SMART_INDICES = 1123;
    private static final int START_EVENT_DELTA = 1117;
    private static final String TEXTVIEW = "textview";
    private static final String UNKNOWN = "unknown";
    private static final int VERSION_TAG = 1121;
    private static final String WEBVIEW = "webview";
    private static final String ZERO = "0";
    private final Context mContext;
    private int mIndex;
    private long mLastEventTime;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private int mOrigStart;
    private final int[] mPrevIndices = new int[2];
    private String mSessionId;
    private long mSessionStartTime;
    private final int[] mSmartIndices = new int[2];
    private boolean mSmartSelectionTriggered;
    private String mVersionTag;
    private final int mWidgetType;

    public static final class SelectionEvent {
        private static final String NO_VERSION_TAG = "";
        public static final int OUT_OF_BOUNDS = 32767;
        public static final int OUT_OF_BOUNDS_NEGATIVE = -32768;
        private final int mEnd;
        private final String mEntityType;
        private int mEventType;
        private final int mStart;
        private final String mVersionTag;

        private SelectionEvent(int start, int end, int eventType, String entityType, String versionTag) {
            Preconditions.checkArgument(end >= start, "end cannot be less than start");
            this.mStart = start;
            this.mEnd = end;
            this.mEventType = eventType;
            this.mEntityType = (String) Preconditions.checkNotNull(entityType);
            this.mVersionTag = (String) Preconditions.checkNotNull(versionTag);
        }

        public static SelectionEvent selectionStarted(int start) {
            return new SelectionEvent(start, start + 1, 1, "", "");
        }

        public static SelectionEvent selectionModified(int start, int end) {
            return new SelectionEvent(start, end, 2, "", "");
        }

        public static SelectionEvent selectionModified(int start, int end, TextClassification classification) {
            String entityType;
            if (classification.getEntityCount() > 0) {
                entityType = classification.getEntity(0);
            } else {
                entityType = "";
            }
            return new SelectionEvent(start, end, 2, entityType, classification.getVersionInfo());
        }

        public static SelectionEvent selectionModified(int start, int end, TextSelection selection) {
            int eventType;
            String entityType;
            if (!selection.getSourceClassifier().equals(TextClassifier.DEFAULT_LOG_TAG)) {
                eventType = 5;
            } else if (end - start > 1) {
                eventType = 4;
            } else {
                eventType = 3;
            }
            if (selection.getEntityCount() > 0) {
                entityType = selection.getEntity(0);
            } else {
                entityType = "";
            }
            return new SelectionEvent(start, end, eventType, entityType, selection.getVersionInfo());
        }

        public static SelectionEvent selectionAction(int start, int end, int actionType) {
            return new SelectionEvent(start, end, actionType, "", "");
        }

        public static SelectionEvent selectionAction(int start, int end, int actionType, TextClassification classification) {
            String entityType;
            if (classification.getEntityCount() > 0) {
                entityType = classification.getEntity(0);
            } else {
                entityType = "";
            }
            return new SelectionEvent(start, end, actionType, entityType, classification.getVersionInfo());
        }

        private boolean isActionType() {
            switch (this.mEventType) {
                case 100:
                case 101:
                case 102:
                case 103:
                case 104:
                case 105:
                case 106:
                case 107:
                case 200:
                case 201:
                    return true;
                default:
                    return false;
            }
        }

        private boolean isTerminal() {
            switch (this.mEventType) {
                case 100:
                case 101:
                case 102:
                case 103:
                case 104:
                case 105:
                case 106:
                case 107:
                case 108:
                    return true;
                default:
                    return false;
            }
        }
    }

    public SmartSelectionEventTracker(Context context, int widgetType) {
        this.mWidgetType = widgetType;
        this.mContext = (Context) Preconditions.checkNotNull(context);
    }

    public void logEvent(SelectionEvent event) {
        boolean z = true;
        Preconditions.checkNotNull(event);
        if (event.mEventType != 1) {
            String str = this.mSessionId;
        }
        long now = System.currentTimeMillis();
        switch (event.mEventType) {
            case 1:
                this.mSessionId = startNewSession();
                if (event.mEnd != event.mStart + 1) {
                    z = false;
                }
                Preconditions.checkArgument(z);
                this.mOrigStart = event.mStart;
                this.mSessionStartTime = now;
                break;
            case 2:
            case 5:
                if (this.mPrevIndices[0] == event.mStart && this.mPrevIndices[1] == event.mEnd) {
                    return;
                }
            case 3:
            case 4:
                this.mSmartSelectionTriggered = true;
                this.mVersionTag = getVersionTag(event);
                this.mSmartIndices[0] = event.mStart;
                this.mSmartIndices[1] = event.mEnd;
                break;
        }
        writeEvent(event, now);
        if (event.isTerminal()) {
            endSession();
        }
    }

    private void writeEvent(SelectionEvent event, long now) {
        LogMaker log = new LogMaker(1100).setType(getLogType(event)).setSubtype(getLogSubType(event)).setPackageName(this.mContext.getPackageName()).setTimestamp(now).addTaggedData(1117, Long.valueOf(now - this.mSessionStartTime)).addTaggedData(1118, Long.valueOf(this.mLastEventTime == 0 ? 0 : now - this.mLastEventTime)).addTaggedData(1120, Integer.valueOf(this.mIndex)).addTaggedData(1121, this.mVersionTag).addTaggedData(1123, Integer.valueOf(getSmartDelta())).addTaggedData(1122, Integer.valueOf(getEventDelta(event))).addTaggedData(1119, this.mSessionId);
        this.mMetricsLogger.write(log);
        debugLog(log);
        this.mLastEventTime = now;
        this.mPrevIndices[0] = event.mStart;
        this.mPrevIndices[1] = event.mEnd;
        this.mIndex++;
    }

    private String startNewSession() {
        endSession();
        this.mSessionId = createSessionId();
        return this.mSessionId;
    }

    private void endSession() {
        this.mOrigStart = 0;
        int[] iArr = this.mSmartIndices;
        this.mSmartIndices[1] = 0;
        iArr[0] = 0;
        iArr = this.mPrevIndices;
        this.mPrevIndices[1] = 0;
        iArr[0] = 0;
        this.mIndex = 0;
        this.mSessionStartTime = 0;
        this.mLastEventTime = 0;
        this.mSmartSelectionTriggered = false;
        this.mVersionTag = getVersionTag(null);
        this.mSessionId = null;
    }

    private static int getLogType(SelectionEvent event) {
        switch (event.mEventType) {
            case 1:
                return 1101;
            case 2:
                return 1102;
            case 3:
                return MetricsEvent.ACTION_TEXT_SELECTION_SMART_SINGLE;
            case 4:
                return MetricsEvent.ACTION_TEXT_SELECTION_SMART_MULTI;
            case 5:
                return MetricsEvent.ACTION_TEXT_SELECTION_AUTO;
            case 100:
                return MetricsEvent.ACTION_TEXT_SELECTION_OVERTYPE;
            case 101:
                return MetricsEvent.ACTION_TEXT_SELECTION_COPY;
            case 102:
                return MetricsEvent.ACTION_TEXT_SELECTION_PASTE;
            case 103:
                return MetricsEvent.ACTION_TEXT_SELECTION_CUT;
            case 104:
                return MetricsEvent.ACTION_TEXT_SELECTION_SHARE;
            case 105:
                return MetricsEvent.ACTION_TEXT_SELECTION_SMART_SHARE;
            case 106:
                return MetricsEvent.ACTION_TEXT_SELECTION_DRAG;
            case 107:
                return MetricsEvent.ACTION_TEXT_SELECTION_ABANDON;
            case 108:
                return MetricsEvent.ACTION_TEXT_SELECTION_OTHER;
            case 200:
                return MetricsEvent.ACTION_TEXT_SELECTION_SELECT_ALL;
            case 201:
                return MetricsEvent.ACTION_TEXT_SELECTION_RESET;
            default:
                return 0;
        }
    }

    private static String getLogTypeString(int logType) {
        switch (logType) {
            case 1101:
                return "SELECTION_STARTED";
            case 1102:
                return "SELECTION_MODIFIED";
            case MetricsEvent.ACTION_TEXT_SELECTION_SELECT_ALL /*1103*/:
                return "SELECT_ALL";
            case MetricsEvent.ACTION_TEXT_SELECTION_RESET /*1104*/:
                return "RESET";
            case MetricsEvent.ACTION_TEXT_SELECTION_SMART_SINGLE /*1105*/:
                return "SMART_SELECTION_SINGLE";
            case MetricsEvent.ACTION_TEXT_SELECTION_SMART_MULTI /*1106*/:
                return "SMART_SELECTION_MULTI";
            case MetricsEvent.ACTION_TEXT_SELECTION_AUTO /*1107*/:
                return "AUTO_SELECTION";
            case MetricsEvent.ACTION_TEXT_SELECTION_OVERTYPE /*1108*/:
                return "OVERTYPE";
            case MetricsEvent.ACTION_TEXT_SELECTION_COPY /*1109*/:
                return "COPY";
            case MetricsEvent.ACTION_TEXT_SELECTION_PASTE /*1110*/:
                return "PASTE";
            case MetricsEvent.ACTION_TEXT_SELECTION_CUT /*1111*/:
                return "CUT";
            case MetricsEvent.ACTION_TEXT_SELECTION_SHARE /*1112*/:
                return "SHARE";
            case MetricsEvent.ACTION_TEXT_SELECTION_SMART_SHARE /*1113*/:
                return "SMART_SHARE";
            case MetricsEvent.ACTION_TEXT_SELECTION_DRAG /*1114*/:
                return "DRAG";
            case MetricsEvent.ACTION_TEXT_SELECTION_ABANDON /*1115*/:
                return "ABANDON";
            case MetricsEvent.ACTION_TEXT_SELECTION_OTHER /*1116*/:
                return "OTHER";
            default:
                return "unknown";
        }
    }

    private static int getLogSubType(SelectionEvent event) {
        String -get1 = event.mEntityType;
        if (-get1.equals(TextClassifier.TYPE_OTHER)) {
            return 2;
        }
        if (-get1.equals("email")) {
            return 3;
        }
        if (-get1.equals("phone")) {
            return 4;
        }
        if (-get1.equals("address")) {
            return 5;
        }
        if (-get1.equals("url")) {
            return 6;
        }
        return 1;
    }

    private static String getLogSubTypeString(int logSubType) {
        switch (logSubType) {
            case 2:
                return TextClassifier.TYPE_OTHER;
            case 3:
                return "email";
            case 4:
                return "phone";
            case 5:
                return "address";
            case 6:
                return "url";
            default:
                return "";
        }
    }

    private int getSmartDelta() {
        if (this.mSmartSelectionTriggered) {
            return (clamp(this.mSmartIndices[0] - this.mOrigStart) << 16) | (clamp(this.mSmartIndices[1] - this.mOrigStart) & 65535);
        }
        return 0;
    }

    private int getEventDelta(SelectionEvent event) {
        return (clamp(event.mStart - this.mOrigStart) << 16) | (clamp(event.mEnd - this.mOrigStart) & 65535);
    }

    private String getVersionTag(SelectionEvent event) {
        String widgetType;
        String version;
        switch (this.mWidgetType) {
            case 1:
                widgetType = TEXTVIEW;
                break;
            case 2:
                widgetType = WEBVIEW;
                break;
            case 3:
                widgetType = EDITTEXT;
                break;
            case 4:
                widgetType = EDIT_WEBVIEW;
                break;
            default:
                widgetType = "unknown";
                break;
        }
        if (event == null) {
            version = "";
        } else {
            version = Objects.toString(event.mVersionTag, "");
        }
        return String.format("%s/%s", new Object[]{widgetType, version});
    }

    private static String createSessionId() {
        return UUID.randomUUID().toString();
    }

    private static int clamp(int val) {
        return Math.max(Math.min(val, SelectionEvent.OUT_OF_BOUNDS), SelectionEvent.OUT_OF_BOUNDS_NEGATIVE);
    }

    private static void debugLog(LogMaker log) {
    }
}
