package com.android.commands.am;

import android.app.IActivityManager;
import android.app.IInstrumentationWatcher.Stub;
import android.app.UiAutomationConnection;
import android.content.ComponentName;
import android.content.pm.IPackageManager;
import android.content.pm.InstrumentationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ServiceManager;
import android.util.AndroidException;
import android.util.proto.ProtoOutputStream;
import android.view.IWindowManager;
import com.android.commands.am.InstrumentationData.ResultsBundleEntry;
import com.android.commands.am.InstrumentationData.Session;
import com.android.commands.am.InstrumentationData.SessionStatus;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Instrument {
    public static final String DEFAULT_LOG_DIR = "instrument-logs";
    private static final int INSTRUMENTATION_FLAG_DISABLE_HIDDEN_API_CHECKS = 1;
    public String abi = null;
    public Bundle args = new Bundle();
    public String componentNameArg;
    public boolean disableHiddenApiChecks = false;
    String logPath = null;
    private final IActivityManager mAm;
    private final IPackageManager mPm;
    private final IWindowManager mWm;
    public boolean noWindowAnimation = false;
    public String profileFile = null;
    boolean protoFile = false;
    boolean protoStd = false;
    public boolean rawMode = false;
    public int userId = -2;
    public boolean wait = false;

    private class InstrumentationWatcher extends Stub {
        private boolean mFinished = false;
        private final StatusReporter mReporter;

        public InstrumentationWatcher(StatusReporter reporter) {
            this.mReporter = reporter;
        }

        public void instrumentationStatus(ComponentName name, int resultCode, Bundle results) {
            synchronized (this) {
                this.mReporter.onInstrumentationStatusLocked(name, resultCode, results);
                notifyAll();
            }
        }

        public void instrumentationFinished(ComponentName name, int resultCode, Bundle results) {
            synchronized (this) {
                this.mReporter.onInstrumentationFinishedLocked(name, resultCode, results);
                this.mFinished = true;
                notifyAll();
            }
        }

        public boolean waitForFinish() {
            synchronized (this) {
                while (!this.mFinished) {
                    try {
                        if (Instrument.this.mAm.asBinder().pingBinder()) {
                            wait(1000);
                        } else {
                            return false;
                        }
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }
                return true;
            }
        }
    }

    private interface StatusReporter {
        void onError(String str, boolean z);

        void onInstrumentationFinishedLocked(ComponentName componentName, int i, Bundle bundle);

        void onInstrumentationStatusLocked(ComponentName componentName, int i, Bundle bundle);
    }

    private class ProtoStatusReporter implements StatusReporter {
        private File mLog;

        ProtoStatusReporter() {
            if (Instrument.this.protoFile) {
                File logDir;
                if (Instrument.this.logPath == null) {
                    logDir = new File(Environment.getLegacyExternalStorageDirectory(), Instrument.DEFAULT_LOG_DIR);
                    if (logDir.exists() || logDir.mkdirs()) {
                        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-hhmmss-SSS", Locale.US);
                        this.mLog = new File(logDir, String.format("log-%s.instrumentation_data_proto", new Object[]{format.format(new Date())}));
                    } else {
                        System.err.format("Unable to create log directory: %s\n", new Object[]{logDir.getAbsolutePath()});
                        Instrument.this.protoFile = false;
                        return;
                    }
                }
                this.mLog = new File(Environment.getLegacyExternalStorageDirectory(), Instrument.this.logPath);
                logDir = this.mLog.getParentFile();
                if (!(logDir.exists() || logDir.mkdirs())) {
                    System.err.format("Unable to create log directory: %s\n", new Object[]{logDir.getAbsolutePath()});
                    Instrument.this.protoFile = false;
                    return;
                }
                if (this.mLog.exists()) {
                    this.mLog.delete();
                }
            }
        }

        public void onInstrumentationStatusLocked(ComponentName name, int resultCode, Bundle results) {
            ProtoOutputStream proto = new ProtoOutputStream();
            long token = proto.start(2246267895809L);
            proto.write(1172526071811L, resultCode);
            writeBundle(proto, 1146756268036L, results);
            proto.end(token);
            outputProto(proto);
        }

        public void onInstrumentationFinishedLocked(ComponentName name, int resultCode, Bundle results) {
            ProtoOutputStream proto = new ProtoOutputStream();
            long token = proto.start(Session.SESSION_STATUS);
            proto.write(SessionStatus.STATUS_CODE, 0);
            proto.write(1172526071811L, resultCode);
            writeBundle(proto, 1146756268036L, results);
            proto.end(token);
            outputProto(proto);
        }

        public void onError(String errorText, boolean commandError) {
            ProtoOutputStream proto = new ProtoOutputStream();
            long token = proto.start(Session.SESSION_STATUS);
            proto.write(SessionStatus.STATUS_CODE, 1);
            proto.write(1138166333442L, errorText);
            proto.end(token);
            outputProto(proto);
        }

        private void writeBundle(ProtoOutputStream proto, long fieldId, Bundle bundle) {
            long bundleToken = proto.start(fieldId);
            for (String key : Instrument.sorted(bundle.keySet())) {
                long entryToken = proto.startRepeatedObject(2246267895809L);
                proto.write(ResultsBundleEntry.KEY, key);
                Object val = bundle.get(key);
                if (val instanceof String) {
                    proto.write(1138166333442L, (String) val);
                } else if (val instanceof Byte) {
                    proto.write(1172526071811L, ((Byte) val).intValue());
                } else if (val instanceof Double) {
                    proto.write(ResultsBundleEntry.VALUE_DOUBLE, ((Double) val).doubleValue());
                } else if (val instanceof Float) {
                    proto.write(ResultsBundleEntry.VALUE_FLOAT, ((Float) val).floatValue());
                } else if (val instanceof Integer) {
                    proto.write(1172526071811L, ((Integer) val).intValue());
                } else if (val instanceof Long) {
                    proto.write(ResultsBundleEntry.VALUE_LONG, ((Long) val).longValue());
                } else if (val instanceof Short) {
                    proto.write(1172526071811L, ((Short) val).shortValue());
                } else if (val instanceof Bundle) {
                    writeBundle(proto, ResultsBundleEntry.VALUE_BUNDLE, (Bundle) val);
                } else if (val instanceof byte[]) {
                    proto.write(ResultsBundleEntry.VALUE_BYTES, (byte[]) val);
                }
                proto.end(entryToken);
            }
            proto.end(bundleToken);
        }

        private void outputProto(ProtoOutputStream proto) {
            byte[] out = proto.getBytes();
            if (Instrument.this.protoStd) {
                try {
                    System.out.write(out);
                    System.out.flush();
                } catch (IOException ex) {
                    System.err.println("Error writing finished response: ");
                    ex.printStackTrace(System.err);
                }
            }
            if (Instrument.this.protoFile) {
                OutputStream os;
                try {
                    os = new FileOutputStream(this.mLog, true);
                    os.write(proto.getBytes());
                    os.flush();
                    os.close();
                } catch (IOException ex2) {
                    System.err.format("Cannot write to %s:\n", new Object[]{this.mLog.getAbsolutePath()});
                    ex2.printStackTrace();
                } catch (Throwable th) {
                    r3.addSuppressed(th);
                }
            }
        }
    }

    private class TextStatusReporter implements StatusReporter {
        private boolean mRawMode;

        public TextStatusReporter(boolean rawMode) {
            this.mRawMode = rawMode;
        }

        public void onInstrumentationStatusLocked(ComponentName name, int resultCode, Bundle results) {
            String pretty = null;
            if (!(this.mRawMode || results == null)) {
                pretty = results.getString("stream");
            }
            if (pretty != null) {
                System.out.print(pretty);
                return;
            }
            if (results != null) {
                for (String key : Instrument.sorted(results.keySet())) {
                    PrintStream printStream = System.out;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("INSTRUMENTATION_STATUS: ");
                    stringBuilder.append(key);
                    stringBuilder.append("=");
                    stringBuilder.append(results.get(key));
                    printStream.println(stringBuilder.toString());
                }
            }
            PrintStream printStream2 = System.out;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("INSTRUMENTATION_STATUS_CODE: ");
            stringBuilder2.append(resultCode);
            printStream2.println(stringBuilder2.toString());
        }

        public void onInstrumentationFinishedLocked(ComponentName name, int resultCode, Bundle results) {
            String pretty = null;
            if (!(this.mRawMode || results == null)) {
                pretty = results.getString("stream");
            }
            if (pretty != null) {
                System.out.println(pretty);
                return;
            }
            if (results != null) {
                for (String key : Instrument.sorted(results.keySet())) {
                    PrintStream printStream = System.out;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("INSTRUMENTATION_RESULT: ");
                    stringBuilder.append(key);
                    stringBuilder.append("=");
                    stringBuilder.append(results.get(key));
                    printStream.println(stringBuilder.toString());
                }
            }
            PrintStream printStream2 = System.out;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("INSTRUMENTATION_CODE: ");
            stringBuilder2.append(resultCode);
            printStream2.println(stringBuilder2.toString());
        }

        public void onError(String errorText, boolean commandError) {
            if (this.mRawMode) {
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onError: commandError=");
                stringBuilder.append(commandError);
                stringBuilder.append(" message=");
                stringBuilder.append(errorText);
                printStream.println(stringBuilder.toString());
            }
            if (!commandError) {
                System.out.println(errorText);
            }
        }
    }

    public Instrument(IActivityManager am, IPackageManager pm) {
        this.mAm = am;
        this.mPm = pm;
        this.mWm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    }

    private static Collection<String> sorted(Collection<String> list) {
        ArrayList<String> copy = new ArrayList(list);
        Collections.sort(copy);
        return copy;
    }

    private ComponentName parseComponentName(String cnArg) throws Exception {
        if (cnArg.contains("/")) {
            ComponentName cn = ComponentName.unflattenFromString(cnArg);
            if (cn != null) {
                return cn;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad component name: ");
            stringBuilder.append(cnArg);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        int i = 0;
        List<InstrumentationInfo> infos = this.mPm.queryInstrumentation(null, 0).getList();
        int numInfos = infos == null ? 0 : infos.size();
        ArrayList<ComponentName> cns = new ArrayList();
        for (int i2 = 0; i2 < numInfos; i2++) {
            InstrumentationInfo info = (InstrumentationInfo) infos.get(i2);
            ComponentName c = new ComponentName(info.packageName, info.name);
            if (cnArg.equals(info.packageName)) {
                cns.add(c);
            }
        }
        StringBuilder stringBuilder2;
        if (cns.size() == 0) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No instrumentation found for: ");
            stringBuilder2.append(cnArg);
            throw new IllegalArgumentException(stringBuilder2.toString());
        } else if (cns.size() == 1) {
            return (ComponentName) cns.get(0);
        } else {
            stringBuilder2 = new StringBuilder();
            int numCns = cns.size();
            while (i < numCns) {
                stringBuilder2.append(((ComponentName) cns.get(i)).flattenToString());
                stringBuilder2.append(", ");
                i++;
            }
            stringBuilder2.setLength(stringBuilder2.length() - 2);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Found multiple instrumentations: ");
            stringBuilder3.append(stringBuilder2.toString());
            throw new IllegalArgumentException(stringBuilder3.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0026 A:{Catch:{ Exception -> 0x00e0, all -> 0x00de }} */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0037 A:{Catch:{ Exception -> 0x00e0, all -> 0x00de }} */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x005a A:{Catch:{ Exception -> 0x00e0, all -> 0x00de }} */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00c3 A:{SYNTHETIC, Splitter:B:41:0x00c3} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x00a6 A:{Catch:{ Exception -> 0x00e0, all -> 0x00de }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() throws Exception {
        StatusReporter reporter = null;
        float[] oldAnims = null;
        try {
            InstrumentationWatcher watcher;
            UiAutomationConnection connection;
            ComponentName cn;
            ComponentName cn2;
            if (!this.protoFile) {
                if (!this.protoStd) {
                    if (this.wait) {
                        reporter = new TextStatusReporter(this.rawMode);
                    }
                    watcher = null;
                    connection = null;
                    if (reporter != null) {
                        watcher = new InstrumentationWatcher(reporter);
                        connection = new UiAutomationConnection();
                    }
                    if (this.noWindowAnimation) {
                        oldAnims = this.mWm.getAnimationScales();
                        this.mWm.setAnimationScale(0, 0.0f);
                        this.mWm.setAnimationScale(1, 0.0f);
                        this.mWm.setAnimationScale(2, 0.0f);
                    }
                    cn = parseComponentName(this.componentNameArg);
                    if (this.abi != null) {
                        boolean matched = false;
                        for (String supportedAbi : Build.SUPPORTED_ABIS) {
                            if (supportedAbi.equals(this.abi)) {
                                matched = true;
                                break;
                            }
                        }
                        if (!matched) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("INSTRUMENTATION_FAILED: Unsupported instruction set ");
                            stringBuilder.append(this.abi);
                            throw new AndroidException(stringBuilder.toString());
                        }
                    }
                    cn2 = cn;
                    if (this.mAm.startInstrumentation(cn, this.profileFile, this.disableHiddenApiChecks, this.args, watcher, connection, this.userId, this.abi)) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("INSTRUMENTATION_FAILED: ");
                        stringBuilder2.append(cn2.flattenToString());
                        throw new AndroidException(stringBuilder2.toString());
                    } else if (watcher == null || watcher.waitForFinish()) {
                        if (oldAnims != null) {
                            this.mWm.setAnimationScales(oldAnims);
                        }
                        return;
                    } else {
                        reporter.onError("INSTRUMENTATION_ABORTED: System has crashed.", false);
                        if (oldAnims != null) {
                            this.mWm.setAnimationScales(oldAnims);
                        }
                        return;
                    }
                }
            }
            reporter = new ProtoStatusReporter();
            watcher = null;
            connection = null;
            if (reporter != null) {
            }
            if (this.noWindowAnimation) {
            }
            cn = parseComponentName(this.componentNameArg);
            if (this.abi != null) {
            }
            cn2 = cn;
            if (this.mAm.startInstrumentation(cn, this.profileFile, this.disableHiddenApiChecks, this.args, watcher, connection, this.userId, this.abi)) {
            }
        } catch (Exception ex) {
            if (reporter != null) {
                reporter.onError(ex.getMessage(), true);
            }
            throw ex;
        } catch (Throwable th) {
            if (oldAnims != null) {
                this.mWm.setAnimationScales(oldAnims);
            }
        }
    }
}
