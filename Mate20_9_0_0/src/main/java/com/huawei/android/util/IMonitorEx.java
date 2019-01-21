package com.huawei.android.util;

import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import java.util.Date;

public class IMonitorEx {

    public static class EventStreamEx {
        private EventStream mEventStream;

        private EventStreamEx(EventStream eStream) {
            this.mEventStream = eStream;
        }

        protected EventStream getEventStream() {
            return this.mEventStream;
        }

        public EventStreamEx setParam(EventStreamEx eStream, short paramID, int value) {
            this.mEventStream.setParam(paramID, value);
            return eStream;
        }

        public EventStreamEx setParam(EventStreamEx eStream, short paramID, long value) {
            this.mEventStream.setParam(paramID, value);
            return eStream;
        }

        public EventStreamEx setParam(EventStreamEx eStream, short paramID, String value) {
            this.mEventStream.setParam(paramID, value);
            return eStream;
        }

        public EventStreamEx setParam(EventStreamEx eStream, String param, Object value) {
            if (value instanceof Integer) {
                this.mEventStream.setParam(param, ((Integer) value).intValue());
            } else if (value instanceof Long) {
                this.mEventStream.setParam(param, ((Long) value).longValue());
            } else if (value instanceof String) {
                this.mEventStream.setParam(param, (String) value);
            } else if (value instanceof Float) {
                this.mEventStream.setParam(param, ((Float) value).floatValue());
            } else if (value instanceof Date) {
                this.mEventStream.setParam(param, (Date) value);
            } else if (value instanceof Short) {
                this.mEventStream.setParam(param, ((Short) value).shortValue());
            } else if (value instanceof Boolean) {
                this.mEventStream.setParam(param, (Boolean) value);
            } else if (value instanceof Byte) {
                this.mEventStream.setParam(param, ((Byte) value).byteValue());
            } else if (value instanceof EventStreamEx) {
                this.mEventStream.setParam(param, ((EventStreamEx) value).getEventStream());
            }
            return eStream;
        }

        public EventStreamEx fillArrayParam(EventStreamEx eStream, String param, Object value) {
            if (value instanceof Integer) {
                this.mEventStream.fillArrayParam(param, ((Integer) value).intValue());
            } else if (value instanceof Long) {
                this.mEventStream.fillArrayParam(param, ((Long) value).longValue());
            } else if (value instanceof Boolean) {
                this.mEventStream.fillArrayParam(param, (Boolean) value);
            } else if (value instanceof Byte) {
                this.mEventStream.fillArrayParam(param, ((Byte) value).byteValue());
            } else if (value instanceof Short) {
                this.mEventStream.fillArrayParam(param, ((Short) value).shortValue());
            } else if (value instanceof String) {
                this.mEventStream.fillArrayParam(param, (String) value);
            } else if (value instanceof EventStreamEx) {
                this.mEventStream.fillArrayParam(param, ((EventStreamEx) value).getEventStream());
            }
            return eStream;
        }
    }

    public static EventStreamEx openEventStream(int eventID) {
        return new EventStreamEx(IMonitor.openEventStream(eventID));
    }

    public static void closeEventStream(EventStreamEx eStream) {
        IMonitor.closeEventStream(eStream.getEventStream());
    }

    public static boolean sendEvent(EventStreamEx eStream) {
        return IMonitor.sendEvent(eStream.getEventStream());
    }
}
