package android_maps_conflict_avoidance.com.google.googlenav.datarequest;

import android_maps_conflict_avoidance.com.google.common.Clock;
import android_maps_conflict_avoidance.com.google.common.Log;

public class ConnectionWarmUpManager {
    private Clock clock;
    private DataRequestDispatcher drd;
    private Object pendingKey;
    private String pendingSource = null;
    private long pendingWarmUpTime;
    private String requestSource = null;
    private int state = 0;

    public ConnectionWarmUpManager(DataRequestDispatcher drd, Clock clock) {
        this.drd = drd;
        this.clock = clock;
    }

    public void onStartServiceRequests(Object key) {
        synchronized (this) {
            if (this.state == 1) {
                this.state = 2;
                this.pendingWarmUpTime = this.clock.relativeTimeMillis();
            } else if (this.state == 2) {
                this.state = 3;
                this.pendingKey = key;
            }
        }
    }

    public void onFinishServiceRequests(Object key, long startTime, int firstByteLatency, int lastByteLatency) {
        Throwable th;
        synchronized (this) {
            try {
                if (this.state == 3) {
                    if (this.pendingKey == key) {
                        this.state = 0;
                        String source = this.pendingSource;
                        try {
                            long time = this.pendingWarmUpTime;
                            this.pendingKey = null;
                            logUsed(source, (int) (startTime - time), firstByteLatency, lastByteLatency);
                            return;
                        } catch (Throwable th2) {
                            String str = source;
                            th = th2;
                            String source2 = str;
                            throw th;
                        }
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    private void logUsed(String source, int interval, int firstByteLatency, int lastByteLatency) {
        String data = new StringBuilder();
        data.append("|d=");
        data.append(interval);
        data.append("|fb=");
        data.append(firstByteLatency);
        data.append("|lb=");
        data.append(lastByteLatency);
        data.append("|");
        logWithSource("u", source, data.toString());
    }

    private void logWithSource(String status, String source, String data) {
        String fullData = new StringBuilder();
        fullData.append("|s=");
        fullData.append(source);
        fullData.append(data.length() == 0 ? "|" : "");
        fullData.append(data);
        Log.addEvent((short) 64, status, fullData.toString());
    }
}
