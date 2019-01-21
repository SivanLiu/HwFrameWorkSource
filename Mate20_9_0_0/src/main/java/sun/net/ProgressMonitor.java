package sun.net;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class ProgressMonitor {
    private static ProgressMeteringPolicy meteringPolicy = new DefaultProgressMeteringPolicy();
    private static ProgressMonitor pm = new ProgressMonitor();
    private ArrayList<ProgressListener> progressListenerList = new ArrayList();
    private ArrayList<ProgressSource> progressSourceList = new ArrayList();

    public static synchronized ProgressMonitor getDefault() {
        ProgressMonitor progressMonitor;
        synchronized (ProgressMonitor.class) {
            progressMonitor = pm;
        }
        return progressMonitor;
    }

    public static synchronized void setDefault(ProgressMonitor m) {
        synchronized (ProgressMonitor.class) {
            if (m != null) {
                pm = m;
            }
        }
    }

    public static synchronized void setMeteringPolicy(ProgressMeteringPolicy policy) {
        synchronized (ProgressMonitor.class) {
            if (policy != null) {
                meteringPolicy = policy;
            }
        }
    }

    public ArrayList<ProgressSource> getProgressSources() {
        ArrayList<ProgressSource> snapshot = new ArrayList();
        try {
            synchronized (this.progressSourceList) {
                Iterator<ProgressSource> iter = this.progressSourceList.iterator();
                while (iter.hasNext()) {
                    snapshot.add((ProgressSource) ((ProgressSource) iter.next()).clone());
                }
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return snapshot;
    }

    public synchronized int getProgressUpdateThreshold() {
        return meteringPolicy.getProgressUpdateThreshold();
    }

    public boolean shouldMeterInput(URL url, String method) {
        return meteringPolicy.shouldMeterInput(url, method);
    }

    /* JADX WARNING: Missing block: B:10:0x0019, code skipped:
            if (r14.progressListenerList.size() <= 0) goto L_0x0070;
     */
    /* JADX WARNING: Missing block: B:11:0x001b, code skipped:
            r0 = new java.util.ArrayList();
            r1 = r14.progressListenerList;
     */
    /* JADX WARNING: Missing block: B:12:0x0022, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:14:?, code skipped:
            r2 = r14.progressListenerList.iterator();
     */
    /* JADX WARNING: Missing block: B:16:0x002d, code skipped:
            if (r2.hasNext() == false) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:17:0x002f, code skipped:
            r0.add((sun.net.ProgressListener) r2.next());
     */
    /* JADX WARNING: Missing block: B:18:0x0039, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:19:0x003a, code skipped:
            r1 = r0.iterator();
     */
    /* JADX WARNING: Missing block: B:21:0x0042, code skipped:
            if (r1.hasNext() == false) goto L_0x0070;
     */
    /* JADX WARNING: Missing block: B:22:0x0044, code skipped:
            ((sun.net.ProgressListener) r1.next()).progressStart(new sun.net.ProgressEvent(r15, r15.getURL(), r15.getMethod(), r15.getContentType(), r15.getState(), r15.getProgress(), r15.getExpected()));
     */
    /* JADX WARNING: Missing block: B:27:0x0070, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void registerSource(ProgressSource pi) {
        synchronized (this.progressSourceList) {
            if (this.progressSourceList.contains(pi)) {
                return;
            }
            this.progressSourceList.add(pi);
        }
    }

    /* JADX WARNING: Missing block: B:10:0x001c, code skipped:
            if (r14.progressListenerList.size() <= 0) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:11:0x001e, code skipped:
            r0 = new java.util.ArrayList();
            r1 = r14.progressListenerList;
     */
    /* JADX WARNING: Missing block: B:12:0x0025, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:14:?, code skipped:
            r2 = r14.progressListenerList.iterator();
     */
    /* JADX WARNING: Missing block: B:16:0x0030, code skipped:
            if (r2.hasNext() == false) goto L_0x003c;
     */
    /* JADX WARNING: Missing block: B:17:0x0032, code skipped:
            r0.add((sun.net.ProgressListener) r2.next());
     */
    /* JADX WARNING: Missing block: B:18:0x003c, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:19:0x003d, code skipped:
            r1 = r0.iterator();
     */
    /* JADX WARNING: Missing block: B:21:0x0045, code skipped:
            if (r1.hasNext() == false) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:22:0x0047, code skipped:
            ((sun.net.ProgressListener) r1.next()).progressFinish(new sun.net.ProgressEvent(r15, r15.getURL(), r15.getMethod(), r15.getContentType(), r15.getState(), r15.getProgress(), r15.getExpected()));
     */
    /* JADX WARNING: Missing block: B:27:0x0073, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void unregisterSource(ProgressSource pi) {
        synchronized (this.progressSourceList) {
            if (this.progressSourceList.contains(pi)) {
                pi.close();
                this.progressSourceList.remove((Object) pi);
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0014, code skipped:
            if (r14.progressListenerList.size() <= 0) goto L_0x006b;
     */
    /* JADX WARNING: Missing block: B:10:0x0016, code skipped:
            r0 = new java.util.ArrayList();
            r1 = r14.progressListenerList;
     */
    /* JADX WARNING: Missing block: B:11:0x001d, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:13:?, code skipped:
            r2 = r14.progressListenerList.iterator();
     */
    /* JADX WARNING: Missing block: B:15:0x0028, code skipped:
            if (r2.hasNext() == false) goto L_0x0034;
     */
    /* JADX WARNING: Missing block: B:16:0x002a, code skipped:
            r0.add((sun.net.ProgressListener) r2.next());
     */
    /* JADX WARNING: Missing block: B:17:0x0034, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:18:0x0035, code skipped:
            r1 = r0.iterator();
     */
    /* JADX WARNING: Missing block: B:20:0x003d, code skipped:
            if (r1.hasNext() == false) goto L_0x006b;
     */
    /* JADX WARNING: Missing block: B:21:0x003f, code skipped:
            ((sun.net.ProgressListener) r1.next()).progressUpdate(new sun.net.ProgressEvent(r15, r15.getURL(), r15.getMethod(), r15.getContentType(), r15.getState(), r15.getProgress(), r15.getExpected()));
     */
    /* JADX WARNING: Missing block: B:26:0x006b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateProgress(ProgressSource pi) {
        synchronized (this.progressSourceList) {
            if (!this.progressSourceList.contains(pi)) {
            }
        }
    }

    public void addProgressListener(ProgressListener l) {
        synchronized (this.progressListenerList) {
            this.progressListenerList.add(l);
        }
    }

    public void removeProgressListener(ProgressListener l) {
        synchronized (this.progressListenerList) {
            this.progressListenerList.remove((Object) l);
        }
    }
}
