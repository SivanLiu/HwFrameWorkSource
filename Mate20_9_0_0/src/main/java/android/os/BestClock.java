package android.os;

import android.util.Log;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;

public class BestClock extends SimpleClock {
    private static final String TAG = "BestClock";
    private final Clock[] clocks;

    public BestClock(ZoneId zone, Clock... clocks) {
        super(zone);
        this.clocks = clocks;
    }

    public long millis() {
        Clock[] clockArr = this.clocks;
        int length = clockArr.length;
        int i = 0;
        while (i < length) {
            try {
                return clockArr[i].millis();
            } catch (DateTimeException e) {
                Log.w(TAG, e.toString());
                i++;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No clocks in ");
        stringBuilder.append(Arrays.toString(this.clocks));
        stringBuilder.append(" were able to provide time");
        throw new DateTimeException(stringBuilder.toString());
    }
}
