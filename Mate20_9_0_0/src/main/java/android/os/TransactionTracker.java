package android.os;

import android.util.Log;
import com.android.internal.util.FastPrintWriter;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TransactionTracker {
    private Map<String, Long> mTraces;

    private void resetTraces() {
        synchronized (this) {
            this.mTraces = new HashMap();
        }
    }

    TransactionTracker() {
        resetTraces();
    }

    public void addTrace(Throwable tr) {
        String trace = Log.getStackTraceString(tr);
        synchronized (this) {
            if (this.mTraces.containsKey(trace)) {
                this.mTraces.put(trace, Long.valueOf(((Long) this.mTraces.get(trace)).longValue() + 1));
            } else {
                this.mTraces.put(trace, Long.valueOf(1));
            }
        }
    }

    public void writeTracesToFile(ParcelFileDescriptor fd) {
        if (!this.mTraces.isEmpty()) {
            PrintWriter pw = new FastPrintWriter(new FileOutputStream(fd.getFileDescriptor()));
            synchronized (this) {
                for (String trace : this.mTraces.keySet()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Count: ");
                    stringBuilder.append(this.mTraces.get(trace));
                    pw.println(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Trace: ");
                    stringBuilder.append(trace);
                    pw.println(stringBuilder.toString());
                    pw.println();
                }
            }
            pw.flush();
        }
    }

    public void clearTraces() {
        resetTraces();
    }
}
