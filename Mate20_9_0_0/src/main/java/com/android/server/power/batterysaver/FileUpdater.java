package com.android.server.power.batterysaver;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.IoThread;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class FileUpdater {
    private static final boolean DEBUG = false;
    private static final String PROP_SKIP_WRITE = "debug.batterysaver.no_write_files";
    private static final String TAG = "BatterySaverController";
    private static final String TAG_DEFAULT_ROOT = "defaults";
    private final int MAX_RETRIES;
    private final long RETRY_INTERVAL_MS;
    private final Context mContext;
    @GuardedBy("mLock")
    private final ArrayMap<String, String> mDefaultValues;
    private Runnable mHandleWriteOnHandlerRunnable;
    private final Handler mHandler;
    private final Object mLock;
    @GuardedBy("mLock")
    private final ArrayMap<String, String> mPendingWrites;
    @GuardedBy("mLock")
    private int mRetries;

    public FileUpdater(Context context) {
        this(context, IoThread.get().getLooper(), 10, 5000);
    }

    @VisibleForTesting
    FileUpdater(Context context, Looper looper, int maxRetries, int retryIntervalMs) {
        this.mLock = new Object();
        this.mPendingWrites = new ArrayMap();
        this.mDefaultValues = new ArrayMap();
        this.mRetries = 0;
        this.mHandleWriteOnHandlerRunnable = new -$$Lambda$FileUpdater$NUmipjKCJwbgmFbIcGS3uaz3QFk(this);
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.MAX_RETRIES = maxRetries;
        this.RETRY_INTERVAL_MS = (long) retryIntervalMs;
    }

    public void systemReady(boolean runtimeRestarted) {
        synchronized (this.mLock) {
            if (runtimeRestarted) {
                try {
                    if (loadDefaultValuesLocked()) {
                        Slog.d(TAG, "Default values loaded after runtime restart; writing them...");
                        restoreDefault();
                    }
                } finally {
                }
            } else {
                injectDefaultValuesFilename().delete();
            }
        }
    }

    public void writeFiles(ArrayMap<String, String> fileValues) {
        synchronized (this.mLock) {
            for (int i = fileValues.size() - 1; i >= 0; i--) {
                this.mPendingWrites.put((String) fileValues.keyAt(i), (String) fileValues.valueAt(i));
            }
            this.mRetries = 0;
            this.mHandler.removeCallbacks(this.mHandleWriteOnHandlerRunnable);
            this.mHandler.post(this.mHandleWriteOnHandlerRunnable);
        }
    }

    public void restoreDefault() {
        synchronized (this.mLock) {
            this.mPendingWrites.clear();
            writeFiles(this.mDefaultValues);
        }
    }

    private String getKeysString(Map<String, String> source) {
        return new ArrayList(source.keySet()).toString();
    }

    private ArrayMap<String, String> cloneMap(ArrayMap<String, String> source) {
        return new ArrayMap(source);
    }

    /* JADX WARNING: Missing block: B:9:0x0014, code skipped:
            r0 = false;
            r2 = r1.size();
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:10:0x001a, code skipped:
            if (r3 >= r2) goto L_0x003b;
     */
    /* JADX WARNING: Missing block: B:11:0x001c, code skipped:
            r4 = (java.lang.String) r1.keyAt(r3);
            r5 = (java.lang.String) r1.valueAt(r3);
     */
    /* JADX WARNING: Missing block: B:12:0x002c, code skipped:
            if (ensureDefaultLoaded(r4) != false) goto L_0x002f;
     */
    /* JADX WARNING: Missing block: B:14:?, code skipped:
            injectWriteToFile(r4, r5);
            removePendingWrite(r4);
     */
    /* JADX WARNING: Missing block: B:16:0x0037, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:18:0x003b, code skipped:
            if (r0 == false) goto L_0x0040;
     */
    /* JADX WARNING: Missing block: B:19:0x003d, code skipped:
            scheduleRetry();
     */
    /* JADX WARNING: Missing block: B:20:0x0040, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleWriteOnHandler() {
        synchronized (this.mLock) {
            if (this.mPendingWrites.size() == 0) {
                return;
            }
            ArrayMap<String, String> writes = cloneMap(this.mPendingWrites);
        }
        int i++;
    }

    private void removePendingWrite(String file) {
        synchronized (this.mLock) {
            this.mPendingWrites.remove(file);
        }
    }

    private void scheduleRetry() {
        synchronized (this.mLock) {
            if (this.mPendingWrites.size() == 0) {
                return;
            }
            this.mRetries++;
            if (this.mRetries > this.MAX_RETRIES) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Gave up writing files: ");
                stringBuilder.append(getKeysString(this.mPendingWrites));
                doWtf(stringBuilder.toString());
                return;
            }
            this.mHandler.removeCallbacks(this.mHandleWriteOnHandlerRunnable);
            this.mHandler.postDelayed(this.mHandleWriteOnHandlerRunnable, this.RETRY_INTERVAL_MS);
        }
    }

    private boolean ensureDefaultLoaded(String file) {
        synchronized (this.mLock) {
            if (this.mDefaultValues.containsKey(file)) {
                return true;
            }
            try {
                String originalValue = injectReadFromFileTrimmed(file);
                synchronized (this.mLock) {
                    this.mDefaultValues.put(file, originalValue);
                    saveDefaultValuesLocked();
                }
                return true;
            } catch (IOException e) {
                injectWtf("Unable to read from file", e);
                removePendingWrite(file);
                return false;
            }
        }
    }

    @VisibleForTesting
    String injectReadFromFileTrimmed(String file) throws IOException {
        return IoUtils.readFileAsString(file).trim();
    }

    @VisibleForTesting
    void injectWriteToFile(String file, String value) throws IOException {
        StringBuilder stringBuilder;
        if (injectShouldSkipWrite()) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Skipped writing to '");
            stringBuilder.append(file);
            stringBuilder.append("'");
            Slog.i(str, stringBuilder.toString());
            return;
        }
        FileWriter out;
        try {
            out = new FileWriter(file);
            out.write(value);
            $closeResource(null, out);
        } catch (IOException | RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed writing '");
            stringBuilder.append(value);
            stringBuilder.append("' to '");
            stringBuilder.append(file);
            stringBuilder.append("': ");
            stringBuilder.append(e.getMessage());
            Slog.w(TAG, stringBuilder.toString());
            throw e;
        } catch (Throwable th) {
            $closeResource(r1, out);
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    @GuardedBy("mLock")
    private void saveDefaultValuesLocked() {
        AtomicFile file = new AtomicFile(injectDefaultValuesFilename());
        FileOutputStream outs = null;
        try {
            file.getBaseFile().getParentFile().mkdirs();
            outs = file.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(outs, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, TAG_DEFAULT_ROOT);
            XmlUtils.writeMapXml(this.mDefaultValues, out, null);
            out.endTag(null, TAG_DEFAULT_ROOT);
            out.endDocument();
            file.finishWrite(outs);
        } catch (IOException | RuntimeException | XmlPullParserException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to write to file ");
            stringBuilder.append(file.getBaseFile());
            Slog.e(str, stringBuilder.toString(), e);
            file.failWrite(outs);
        }
    }

    /* JADX WARNING: Missing block: B:14:0x003b, code skipped:
            r10 = TAG;
            r11 = new java.lang.StringBuilder();
            r11.append("Invalid root tag: ");
            r11.append(r9);
            android.util.Slog.e(r10, r11.toString());
     */
    /* JADX WARNING: Missing block: B:15:0x0052, code skipped:
            if (r5 == null) goto L_0x0057;
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            $closeResource(null, r5);
     */
    /* JADX WARNING: Missing block: B:18:0x0057, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:22:0x0062, code skipped:
            if (r5 == null) goto L_0x0091;
     */
    /* JADX WARNING: Missing block: B:24:?, code skipped:
            $closeResource(null, r5);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mLock")
    @VisibleForTesting
    boolean loadDefaultValuesLocked() {
        AtomicFile file = new AtomicFile(injectDefaultValuesFilename());
        Map<String, String> read = null;
        FileInputStream in;
        try {
            in = file.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(in, StandardCharsets.UTF_8.name());
            while (true) {
                int next = parser.next();
                int type = next;
                if (next == 1) {
                    break;
                } else if (type == 2) {
                    next = parser.getDepth();
                    String tag = parser.getName();
                    if (next != 1) {
                        read = XmlUtils.readThisArrayMapXml(parser, TAG_DEFAULT_ROOT, new String[1], null);
                    } else if (!TAG_DEFAULT_ROOT.equals(tag)) {
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            read = null;
        } catch (IOException | RuntimeException | XmlPullParserException e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read file ");
            stringBuilder.append(file.getBaseFile());
            Slog.e(str, stringBuilder.toString(), e2);
        } catch (Throwable th) {
            if (in != null) {
                $closeResource(r1, in);
            }
        }
        if (read == null) {
            return false;
        }
        this.mDefaultValues.clear();
        this.mDefaultValues.putAll(read);
        return true;
    }

    private void doWtf(String message) {
        injectWtf(message, null);
    }

    @VisibleForTesting
    void injectWtf(String message, Throwable e) {
        Slog.wtf(TAG, message, e);
    }

    File injectDefaultValuesFilename() {
        File dir = new File(Environment.getDataSystemDirectory(), "battery-saver");
        dir.mkdirs();
        return new File(dir, "default-values.xml");
    }

    @VisibleForTesting
    boolean injectShouldSkipWrite() {
        return SystemProperties.getBoolean(PROP_SKIP_WRITE, false);
    }

    @VisibleForTesting
    ArrayMap<String, String> getDefaultValuesForTest() {
        return this.mDefaultValues;
    }
}
