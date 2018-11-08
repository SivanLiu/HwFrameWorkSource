package huawei.com.android.server.policy.fingersense;

import android.util.IMonitor;
import android.util.IMonitor.EventStream;

public class Reporter {
    public static void reportUserData(int eventID, short paramID, int value) {
        EventStream eStream = IMonitor.openEventStream(eventID);
        if (eStream != null) {
            eStream.setParam(paramID, value);
            IMonitor.sendEvent(eStream);
            IMonitor.closeEventStream(eStream);
        }
    }

    public static void reportUserData(int eventID, short paramID, long value) {
        EventStream eStream = IMonitor.openEventStream(eventID);
        if (eStream != null) {
            eStream.setParam(paramID, value);
            IMonitor.sendEvent(eStream);
            IMonitor.closeEventStream(eStream);
        }
    }

    public static void reportUserData(int eventID, short paramID, float value) {
        EventStream eStream = IMonitor.openEventStream(eventID);
        if (eStream != null) {
            eStream.setParam(paramID, value);
            IMonitor.sendEvent(eStream);
            IMonitor.closeEventStream(eStream);
        }
    }

    public static void reportUserData(int eventID, short paramID, String value) {
        EventStream eStream = IMonitor.openEventStream(eventID);
        if (eStream != null) {
            eStream.setParam(paramID, value);
            IMonitor.sendEvent(eStream);
            IMonitor.closeEventStream(eStream);
        }
    }
}
