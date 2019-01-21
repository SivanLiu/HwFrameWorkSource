package android.telecom.Logging;

import android.provider.SettingsStringUtil;
import android.telecom.Log;
import android.telecom.Logging.SessionManager.ISessionIdQueryHandler;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class EventManager {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    @VisibleForTesting
    public static final int DEFAULT_EVENTS_TO_CACHE = 10;
    public static final String TAG = "Logging.Events";
    private static final Object mSync = new Object();
    private final Map<Loggable, EventRecord> mCallEventRecordMap = new HashMap();
    private List<EventListener> mEventListeners = new ArrayList();
    private LinkedBlockingQueue<EventRecord> mEventRecords = new LinkedBlockingQueue(10);
    private ISessionIdQueryHandler mSessionIdHandler;
    private final Map<String, List<TimedEventPair>> requestResponsePairs = new HashMap();

    public static class Event {
        public Object data;
        public String eventId;
        public String sessionId;
        public long time;
        public final String timestampString;

        public Event(String eventId, String sessionId, long time, Object data) {
            this.eventId = eventId;
            this.sessionId = sessionId;
            this.time = time;
            this.timestampString = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()).format(EventManager.DATE_TIME_FORMATTER);
            this.data = data;
        }
    }

    public interface EventListener {
        void eventRecordAdded(EventRecord eventRecord);
    }

    public class EventRecord {
        private final List<Event> mEvents = new LinkedList();
        private final Loggable mRecordEntry;

        private class PendingResponse {
            String name;
            String requestEventId;
            long requestEventTimeMillis;
            long timeoutMillis;

            public PendingResponse(String requestEventId, long requestEventTimeMillis, long timeoutMillis, String name) {
                this.requestEventId = requestEventId;
                this.requestEventTimeMillis = requestEventTimeMillis;
                this.timeoutMillis = timeoutMillis;
                this.name = name;
            }
        }

        public class EventTiming extends TimedEvent<String> {
            public String name;
            public long time;

            public EventTiming(String name, long time) {
                this.name = name;
                this.time = time;
            }

            public String getKey() {
                return this.name;
            }

            public long getTime() {
                return this.time;
            }
        }

        public EventRecord(Loggable recordEntry) {
            this.mRecordEntry = recordEntry;
        }

        public Loggable getRecordEntry() {
            return this.mRecordEntry;
        }

        public void addEvent(String event, String sessionId, Object data) {
            this.mEvents.add(new Event(event, sessionId, System.currentTimeMillis(), data));
            Log.i("Event", "RecordEntry %s: %s, %s", this.mRecordEntry.getId(), event, data);
        }

        public List<Event> getEvents() {
            return this.mEvents;
        }

        public List<EventTiming> extractEventTimings() {
            if (this.mEvents == null) {
                return Collections.emptyList();
            }
            LinkedList<EventTiming> result = new LinkedList();
            HashMap pendingResponses = new HashMap();
            Iterator it = this.mEvents.iterator();
            while (it.hasNext()) {
                Iterator it2;
                PendingResponse pendingResponse;
                Event event = (Event) it.next();
                if (EventManager.this.requestResponsePairs.containsKey(event.eventId)) {
                    for (TimedEventPair p : (List) EventManager.this.requestResponsePairs.get(event.eventId)) {
                        String str = p.mResponse;
                        it2 = it;
                        PendingResponse pendingResponse2 = pendingResponse;
                        pendingResponse = new PendingResponse(event.eventId, event.time, p.mTimeoutMillis, p.mName);
                        pendingResponses.put(str, pendingResponse2);
                        it = it2;
                    }
                }
                it2 = it;
                pendingResponse = (PendingResponse) pendingResponses.remove(event.eventId);
                if (pendingResponse != null) {
                    long elapsedTime = event.time - pendingResponse.requestEventTimeMillis;
                    if (elapsedTime < pendingResponse.timeoutMillis) {
                        result.add(new EventTiming(pendingResponse.name, elapsedTime));
                    }
                }
                it = it2;
            }
            return result;
        }

        public void dump(IndentingPrintWriter pw) {
            pw.print(this.mRecordEntry.getDescription());
            pw.increaseIndent();
            for (Event event : this.mEvents) {
                pw.print(event.timestampString);
                pw.print(" - ");
                pw.print(event.eventId);
                if (event.data != null) {
                    pw.print(" (");
                    Object data = event.data;
                    if (data instanceof Loggable) {
                        EventRecord record = (EventRecord) EventManager.this.mCallEventRecordMap.get(data);
                        if (record != null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("RecordEntry ");
                            stringBuilder.append(record.mRecordEntry.getId());
                            data = stringBuilder.toString();
                        }
                    }
                    pw.print(data);
                    pw.print(")");
                }
                if (!TextUtils.isEmpty(event.sessionId)) {
                    pw.print(SettingsStringUtil.DELIMITER);
                    pw.print(event.sessionId);
                }
                pw.println();
            }
            pw.println("Timings (average for this call, milliseconds):");
            pw.increaseIndent();
            List<String> eventNames = new ArrayList(TimedEvent.averageTimings(extractEventTimings()).keySet());
            Collections.sort(eventNames);
            for (String eventName : eventNames) {
                pw.printf("%s: %.2f\n", new Object[]{eventName, avgEventTimings.get(eventName)});
            }
            pw.decreaseIndent();
            pw.decreaseIndent();
        }
    }

    public interface Loggable {
        String getDescription();

        String getId();
    }

    public static class TimedEventPair {
        private static final long DEFAULT_TIMEOUT = 3000;
        String mName;
        String mRequest;
        String mResponse;
        long mTimeoutMillis = DEFAULT_TIMEOUT;

        public TimedEventPair(String request, String response, String name) {
            this.mRequest = request;
            this.mResponse = response;
            this.mName = name;
        }

        public TimedEventPair(String request, String response, String name, long timeoutMillis) {
            this.mRequest = request;
            this.mResponse = response;
            this.mName = name;
            this.mTimeoutMillis = timeoutMillis;
        }
    }

    public void addRequestResponsePair(TimedEventPair p) {
        if (this.requestResponsePairs.containsKey(p.mRequest)) {
            ((List) this.requestResponsePairs.get(p.mRequest)).add(p);
            return;
        }
        ArrayList<TimedEventPair> responses = new ArrayList();
        responses.add(p);
        this.requestResponsePairs.put(p.mRequest, responses);
    }

    public EventManager(ISessionIdQueryHandler l) {
        this.mSessionIdHandler = l;
    }

    public void event(Loggable recordEntry, String event, Object data) {
        String currentSessionID = this.mSessionIdHandler.getSessionId();
        if (recordEntry == null) {
            Log.i(TAG, "Non-call EVENT: %s, %s", event, data);
            return;
        }
        synchronized (this.mEventRecords) {
            if (!this.mCallEventRecordMap.containsKey(recordEntry)) {
                addEventRecord(new EventRecord(recordEntry));
            }
            ((EventRecord) this.mCallEventRecordMap.get(recordEntry)).addEvent(event, currentSessionID, data);
        }
    }

    public void event(Loggable recordEntry, String event, String format, Object... args) {
        String msg;
        if (args != null) {
            try {
                if (args.length != 0) {
                    msg = String.format(Locale.US, format, args);
                    event(recordEntry, event, msg);
                }
            } catch (IllegalFormatException ife) {
                Log.e((Object) this, ife, "IllegalFormatException: formatString='%s' numArgs=%d", format, Integer.valueOf(args.length));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(format);
                stringBuilder.append(" (An error occurred while formatting the message.)");
                msg = stringBuilder.toString();
            }
        }
        msg = format;
        event(recordEntry, event, msg);
    }

    public void dumpEvents(IndentingPrintWriter pw) {
        pw.println("Historical Events:");
        pw.increaseIndent();
        Iterator it = this.mEventRecords.iterator();
        while (it.hasNext()) {
            ((EventRecord) it.next()).dump(pw);
        }
        pw.decreaseIndent();
    }

    public void dumpEventsTimeline(IndentingPrintWriter pw) {
        pw.println("Historical Events (sorted by time):");
        List<Pair<Loggable, Event>> events = new ArrayList();
        Iterator it = this.mEventRecords.iterator();
        while (it.hasNext()) {
            EventRecord er = (EventRecord) it.next();
            for (Event ev : er.getEvents()) {
                events.add(new Pair(er.getRecordEntry(), ev));
            }
        }
        events.sort(Comparator.comparingLong(-$$Lambda$EventManager$weOtitr8e1cZeiy1aDSqzNoKaY8.INSTANCE));
        pw.increaseIndent();
        for (Pair<Loggable, Event> event : events) {
            pw.print(((Event) event.second).timestampString);
            pw.print(",");
            pw.print(((Loggable) event.first).getId());
            pw.print(",");
            pw.print(((Event) event.second).eventId);
            pw.print(",");
            pw.println(((Event) event.second).data);
        }
        pw.decreaseIndent();
    }

    public void changeEventCacheSize(int newSize) {
        LinkedBlockingQueue<EventRecord> oldEventLog = this.mEventRecords;
        this.mEventRecords = new LinkedBlockingQueue(newSize);
        this.mCallEventRecordMap.clear();
        oldEventLog.forEach(new -$$Lambda$EventManager$uddp6XAJ90VBwdTiuzBdSYelntQ(this));
    }

    public static /* synthetic */ void lambda$changeEventCacheSize$1(EventManager eventManager, EventRecord newRecord) {
        Loggable recordEntry = newRecord.getRecordEntry();
        if (eventManager.mEventRecords.remainingCapacity() == 0) {
            EventRecord record = (EventRecord) eventManager.mEventRecords.poll();
            if (record != null) {
                eventManager.mCallEventRecordMap.remove(record.getRecordEntry());
            }
        }
        eventManager.mEventRecords.add(newRecord);
        eventManager.mCallEventRecordMap.put(recordEntry, newRecord);
    }

    public void registerEventListener(EventListener e) {
        if (e != null) {
            synchronized (mSync) {
                this.mEventListeners.add(e);
            }
        }
    }

    @VisibleForTesting
    public LinkedBlockingQueue<EventRecord> getEventRecords() {
        return this.mEventRecords;
    }

    @VisibleForTesting
    public Map<Loggable, EventRecord> getCallEventRecordMap() {
        return this.mCallEventRecordMap;
    }

    private void addEventRecord(EventRecord newRecord) {
        Loggable recordEntry = newRecord.getRecordEntry();
        if (this.mEventRecords.remainingCapacity() == 0) {
            EventRecord record = (EventRecord) this.mEventRecords.poll();
            if (record != null) {
                this.mCallEventRecordMap.remove(record.getRecordEntry());
            }
        }
        this.mEventRecords.add(newRecord);
        this.mCallEventRecordMap.put(recordEntry, newRecord);
        synchronized (mSync) {
            for (EventListener l : this.mEventListeners) {
                l.eventRecordAdded(newRecord);
            }
        }
    }
}
