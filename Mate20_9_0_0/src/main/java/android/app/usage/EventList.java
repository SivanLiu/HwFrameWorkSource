package android.app.usage;

import android.app.usage.UsageEvents.Event;
import java.util.ArrayList;

public class EventList {
    private final ArrayList<Event> mEvents = new ArrayList();

    public int size() {
        return this.mEvents.size();
    }

    public void clear() {
        this.mEvents.clear();
    }

    public Event get(int index) {
        return (Event) this.mEvents.get(index);
    }

    public void insert(Event event) {
        int size = this.mEvents.size();
        if (size == 0 || event.mTimeStamp >= ((Event) this.mEvents.get(size - 1)).mTimeStamp) {
            this.mEvents.add(event);
            return;
        }
        this.mEvents.add(firstIndexOnOrAfter(event.mTimeStamp + 1), event);
    }

    public int firstIndexOnOrAfter(long timeStamp) {
        int size = this.mEvents.size();
        int result = size;
        int lo = 0;
        int hi = size - 1;
        while (lo <= hi) {
            int lo2 = (lo + hi) >>> 1;
            if (((Event) this.mEvents.get(lo2)).mTimeStamp >= timeStamp) {
                hi = lo2 - 1;
                result = lo2;
            } else {
                lo = lo2 + 1;
            }
        }
        return result;
    }
}
