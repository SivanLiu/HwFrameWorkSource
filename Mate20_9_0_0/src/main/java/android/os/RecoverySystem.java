package android.os;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.IRecoverySystemProgressListener.Stub;
import android.provider.Settings.Global;
import android.rms.AppAssociate;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.WindowManager;
import com.android.internal.logging.MetricsLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import libcore.io.Streams;

public class RecoverySystem {
    private static final String ACTION_EUICC_FACTORY_RESET = "com.android.internal.action.EUICC_FACTORY_RESET";
    public static final File BLOCK_MAP_FILE = new File(RECOVERY_DIR, "block.map");
    private static String COMMAND_FILE_STRING = "command";
    private static final long DEFAULT_EUICC_FACTORY_RESET_TIMEOUT_MILLIS = 30000;
    private static final File DEFAULT_KEYSTORE = new File("/system/etc/security/otacerts.zip");
    private static final File LAST_INSTALL_FILE = new File(RECOVERY_DIR, "last_install");
    private static final String LAST_PREFIX = "last_";
    private static final File LOG_FILE = new File(RECOVERY_DIR, "log");
    private static final int LOG_FILE_MAX_LENGTH = 65536;
    private static final long MAX_EUICC_FACTORY_RESET_TIMEOUT_MILLIS = 60000;
    private static final long MIN_EUICC_FACTORY_RESET_TIMEOUT_MILLIS = 5000;
    private static final String PACKAGE_NAME_WIPING_EUICC_DATA_CALLBACK = "android";
    private static final long PUBLISH_PROGRESS_INTERVAL_MS = 500;
    private static final File RECOVERY_DIR = new File("/cache/recovery");
    private static final String TAG = "RecoverySystem";
    public static final File UNCRYPT_PACKAGE_FILE = new File(RECOVERY_DIR, "uncrypt_file");
    public static final File UNCRYPT_STATUS_FILE = new File(RECOVERY_DIR, "uncrypt_status");
    private static final Object sRequestLock = new Object();
    private final IRecoverySystem mService;

    public interface ProgressListener {
        void onProgress(int i);
    }

    private static HashSet<X509Certificate> getTrustedCerts(File keystore) throws IOException, GeneralSecurityException {
        HashSet<X509Certificate> trusted = new HashSet();
        if (keystore == null) {
            keystore = DEFAULT_KEYSTORE;
        }
        ZipFile zip = new ZipFile(keystore);
        InputStream is;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                is = zip.getInputStream((ZipEntry) entries.nextElement());
                trusted.add((X509Certificate) cf.generateCertificate(is));
                is.close();
            }
            zip.close();
            return trusted;
        } catch (Throwable th) {
            zip.close();
        }
    }

    /* JADX WARNING: Missing block: B:36:0x00a1, code skipped:
            r9 = new sun.security.pkcs.PKCS7(new java.io.ByteArrayInputStream(r4, (r15 + 22) - r7, r7));
            r2 = r9.getCertificates();
     */
    /* JADX WARNING: Missing block: B:37:0x00b4, code skipped:
            if (r2 == null) goto L_0x01a4;
     */
    /* JADX WARNING: Missing block: B:39:0x00b7, code skipped:
            if (r2.length == 0) goto L_0x01a4;
     */
    /* JADX WARNING: Missing block: B:40:0x00b9, code skipped:
            r3 = r2[0];
            r1 = r3.getPublicKey();
            r8 = r9.getSignerInfos();
     */
    /* JADX WARNING: Missing block: B:41:0x00c4, code skipped:
            if (r8 == null) goto L_0x018b;
     */
    /* JADX WARNING: Missing block: B:43:0x00c7, code skipped:
            if (r8.length == 0) goto L_0x018b;
     */
    /* JADX WARNING: Missing block: B:44:0x00c9, code skipped:
            r0 = r8[null];
            r16 = false;
     */
    /* JADX WARNING: Missing block: B:45:0x00ce, code skipped:
            if (r33 != null) goto L_0x00d7;
     */
    /* JADX WARNING: Missing block: B:46:0x00d0, code skipped:
            r24 = r2;
            r2 = DEFAULT_KEYSTORE;
     */
    /* JADX WARNING: Missing block: B:47:0x00d7, code skipped:
            r24 = r2;
            r2 = r33;
     */
    /* JADX WARNING: Missing block: B:48:0x00db, code skipped:
            r2 = getTrustedCerts(r2);
            r25 = r3;
            r3 = r2.iterator();
     */
    /* JADX WARNING: Missing block: B:50:0x00e9, code skipped:
            if (r3.hasNext() == false) goto L_0x010c;
     */
    /* JADX WARNING: Missing block: B:51:0x00eb, code skipped:
            r27 = r2;
            r28 = r3;
     */
    /* JADX WARNING: Missing block: B:52:0x0101, code skipped:
            if (((java.security.cert.X509Certificate) r3.next()).getPublicKey().equals(r1) == false) goto L_0x0106;
     */
    /* JADX WARNING: Missing block: B:53:0x0103, code skipped:
            r16 = true;
     */
    /* JADX WARNING: Missing block: B:54:0x0106, code skipped:
            r2 = r27;
            r3 = r28;
     */
    /* JADX WARNING: Missing block: B:55:0x010c, code skipped:
            r27 = r2;
     */
    /* JADX WARNING: Missing block: B:56:0x010e, code skipped:
            if (r16 == false) goto L_0x0170;
     */
    /* JADX WARNING: Missing block: B:57:0x0110, code skipped:
            r13.seek(0);
     */
    /* JADX WARNING: Missing block: B:58:0x0115, code skipped:
            r18 = r8;
            r2 = r32;
            r8 = r2;
     */
    /* JADX WARNING: Missing block: B:61:0x011c, code skipped:
            r19 = r1;
            r1 = r1;
            r12 = r1;
            r29 = r14;
            r20 = r24;
            r21 = r25;
            r22 = r27;
            r14 = r2;
            r2 = r10;
            r23 = r4;
            r4 = r15;
            r24 = r7;
            r7 = r13;
     */
    /* JADX WARNING: Missing block: B:63:?, code skipped:
            r1 = new android.os.RecoverySystem.AnonymousClass1();
            r1 = r9.verify(r0, r12);
            r2 = java.lang.Thread.interrupted();
     */
    /* JADX WARNING: Missing block: B:64:0x013b, code skipped:
            if (r14 == null) goto L_0x0142;
     */
    /* JADX WARNING: Missing block: B:65:0x013d, code skipped:
            r14.onProgress(100);
     */
    /* JADX WARNING: Missing block: B:66:0x0142, code skipped:
            if (r2 != false) goto L_0x0163;
     */
    /* JADX WARNING: Missing block: B:67:0x0144, code skipped:
            if (r1 == null) goto L_0x015a;
     */
    /* JADX WARNING: Missing block: B:68:0x0146, code skipped:
            r13.close();
     */
    /* JADX WARNING: Missing block: B:69:0x014e, code skipped:
            if (readAndVerifyPackageCompatibilityEntry(r31) == false) goto L_0x0151;
     */
    /* JADX WARNING: Missing block: B:70:0x0150, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:72:0x0159, code skipped:
            throw new java.security.SignatureException("package compatibility verification failed");
     */
    /* JADX WARNING: Missing block: B:75:0x0162, code skipped:
            throw new java.security.SignatureException("signature digest verification failed");
     */
    /* JADX WARNING: Missing block: B:77:0x016b, code skipped:
            throw new java.security.SignatureException("verification was interrupted");
     */
    /* JADX WARNING: Missing block: B:78:0x016c, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:79:0x016d, code skipped:
            r14 = r2;
     */
    /* JADX WARNING: Missing block: B:80:0x0170, code skipped:
            r19 = r1;
            r23 = r4;
            r18 = r8;
            r29 = r14;
            r20 = r24;
            r21 = r25;
            r22 = r27;
            r14 = r32;
            r24 = r7;
     */
    /* JADX WARNING: Missing block: B:81:0x018a, code skipped:
            throw new java.security.SignatureException("signature doesn't match any trusted key");
     */
    /* JADX WARNING: Missing block: B:82:0x018b, code skipped:
            r19 = r1;
            r20 = r2;
            r21 = r3;
            r23 = r4;
            r24 = r7;
            r18 = r8;
            r29 = r14;
            r14 = r32;
     */
    /* JADX WARNING: Missing block: B:83:0x01a3, code skipped:
            throw new java.security.SignatureException("signature contains no signedData");
     */
    /* JADX WARNING: Missing block: B:84:0x01a4, code skipped:
            r20 = r2;
            r23 = r4;
            r24 = r7;
            r29 = r14;
            r14 = r32;
     */
    /* JADX WARNING: Missing block: B:85:0x01b6, code skipped:
            throw new java.security.SignatureException("signature contains no certificates");
     */
    /* JADX WARNING: Missing block: B:92:0x01d8, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void verifyPackage(File packageFile, ProgressListener listener, File deviceCertsZipFile) throws IOException, GeneralSecurityException {
        Throwable th;
        ProgressListener progressListener;
        ProgressListener progressListener2 = listener;
        long fileLen = packageFile.length();
        RandomAccessFile raf = new RandomAccessFile(packageFile, "r");
        try {
            byte[] bArr;
            final long startTimeMillis = System.currentTimeMillis();
            if (progressListener2 != null) {
                progressListener2.onProgress(0);
            }
            raf.seek(fileLen - 6);
            byte[] footer = new byte[6];
            raf.readFully(footer);
            if (footer[2] == (byte) -1) {
                int i = 3;
                if (footer[3] == (byte) -1) {
                    int commentSize = ((footer[5] & 255) << 8) | (footer[4] & 255);
                    int signatureStart = (footer[0] & 255) | ((footer[1] & 255) << 8);
                    byte[] eocd = new byte[(commentSize + 22)];
                    try {
                        raf.seek(fileLen - ((long) (commentSize + 22)));
                        raf.readFully(eocd);
                        if (eocd[0] == (byte) 80 && eocd[1] == (byte) 75 && eocd[2] == (byte) 5 && eocd[3] == (byte) 6) {
                            int i2 = 4;
                            while (true) {
                                int i3 = i2;
                                if (i3 >= eocd.length - i) {
                                    break;
                                }
                                if (eocd[i3] == (byte) 80 && eocd[i3 + 1] == (byte) 75 && eocd[i3 + 2] == (byte) 5) {
                                    if (eocd[i3 + 3] == (byte) 6) {
                                        throw new SignatureException("EOCD marker found after start of EOCD");
                                    }
                                }
                                i2 = i3 + 1;
                                i = 3;
                            }
                        } else {
                            int i4 = signatureStart;
                            bArr = footer;
                            footer = listener;
                            throw new SignatureException("no signature in file (bad footer)");
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        progressListener = listener;
                        raf.close();
                        throw th;
                    }
                }
            }
            bArr = footer;
            footer = progressListener2;
            throw new SignatureException("no signature in file (no footer)");
        } catch (Throwable th3) {
            th = th3;
            progressListener = progressListener2;
            raf.close();
            throw th;
        }
    }

    private static boolean verifyPackageCompatibility(InputStream inputStream) throws IOException {
        long entrySize;
        StringBuilder stringBuilder;
        ArrayList<String> list = new ArrayList();
        ZipInputStream zis = new ZipInputStream(inputStream);
        while (true) {
            ZipEntry nextEntry = zis.getNextEntry();
            ZipEntry entry = nextEntry;
            if (nextEntry != null) {
                entrySize = entry.getSize();
                if (entrySize > 2147483647L || entrySize < 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("invalid entry size (");
                    stringBuilder.append(entrySize);
                    stringBuilder.append(") in the compatibility file");
                } else {
                    byte[] bytes = new byte[((int) entrySize)];
                    Streams.readFully(zis, bytes);
                    list.add(new String(bytes, StandardCharsets.UTF_8));
                }
            } else if (!list.isEmpty()) {
                return VintfObject.verify((String[]) list.toArray(new String[list.size()])) == 0;
            } else {
                throw new IOException("no entries found in the compatibility file");
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("invalid entry size (");
        stringBuilder.append(entrySize);
        stringBuilder.append(") in the compatibility file");
        throw new IOException(stringBuilder.toString());
    }

    private static boolean readAndVerifyPackageCompatibilityEntry(File packageFile) throws IOException {
        ZipFile zip = new ZipFile(packageFile);
        InputStream inputStream;
        try {
            Boolean bl = Boolean.valueOf(null);
            ZipEntry entry = zip.getEntry("compatibility.zip");
            if (entry == null) {
                $closeResource(null, zip);
                return true;
            }
            inputStream = zip.getInputStream(entry);
            bl = Boolean.valueOf(verifyPackageCompatibility(inputStream));
            inputStream.close();
            boolean booleanValue = bl.booleanValue();
            $closeResource(null, zip);
            return booleanValue;
        } catch (IOException e) {
            Log.e(TAG, "Exception happend when excute verifyPackageCompatibility");
            throw e;
        } catch (IOException e2) {
            Log.e(TAG, "InputStream close failure");
        } catch (Throwable th) {
            try {
            } catch (Throwable th2) {
                $closeResource(th, zip);
            }
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

    /* JADX WARNING: Missing block: B:9:0x0012, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @SuppressLint({"Doclava125"})
    @SystemApi
    public static boolean verifyPackageCompatibility(File compatibilityFile) throws IOException {
        InputStream inputStream = new FileInputStream(compatibilityFile);
        boolean verifyPackageCompatibility = verifyPackageCompatibility(inputStream);
        $closeResource(null, inputStream);
        return verifyPackageCompatibility;
    }

    @SystemApi
    public static void processPackage(Context context, File packageFile, final ProgressListener listener, Handler handler) throws IOException {
        String filename = packageFile.getCanonicalPath();
        if (filename.startsWith("/data/")) {
            RecoverySystem rs = (RecoverySystem) context.getSystemService(PowerManager.REBOOT_RECOVERY);
            IRecoverySystemProgressListener progressListener = null;
            if (listener != null) {
                Handler progressHandler;
                if (handler != null) {
                    progressHandler = handler;
                } else {
                    progressHandler = new Handler(context.getMainLooper());
                }
                progressListener = new Stub() {
                    int lastProgress = 0;
                    long lastPublishTime = System.currentTimeMillis();

                    public void onProgress(final int progress) {
                        final long now = System.currentTimeMillis();
                        progressHandler.post(new Runnable() {
                            public void run() {
                                if (progress > AnonymousClass2.this.lastProgress && now - AnonymousClass2.this.lastPublishTime > 500) {
                                    AnonymousClass2.this.lastProgress = progress;
                                    AnonymousClass2.this.lastPublishTime = now;
                                    listener.onProgress(progress);
                                }
                            }
                        });
                    }
                };
            }
            if (!rs.uncrypt(filename, progressListener)) {
                throw new IOException("process package failed");
            }
        }
    }

    @SystemApi
    public static void processPackage(Context context, File packageFile, ProgressListener listener) throws IOException {
        processPackage(context, packageFile, listener, null);
    }

    public static void installPackage(Context context, File packageFile) throws IOException {
        installPackage(context, packageFile, false);
    }

    @SystemApi
    public static void installPackage(Context context, File packageFile, boolean processed) throws IOException {
        synchronized (sRequestLock) {
            String str;
            LOG_FILE.delete();
            UNCRYPT_PACKAGE_FILE.delete();
            String filename = packageFile.getCanonicalPath();
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("!!! REBOOTING TO INSTALL ");
            stringBuilder.append(filename);
            stringBuilder.append(" !!!");
            Log.w(str2, stringBuilder.toString());
            boolean securityUpdate = filename.endsWith("_s.zip");
            if (filename.startsWith("/data/")) {
                if (!processed) {
                    FileWriter uncryptFile = new FileWriter(UNCRYPT_PACKAGE_FILE);
                    try {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(filename);
                        stringBuilder2.append("\n");
                        uncryptFile.write(stringBuilder2.toString());
                        if (!(UNCRYPT_PACKAGE_FILE.setReadable(true, false) && UNCRYPT_PACKAGE_FILE.setWritable(true, false))) {
                            str = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Error setting permission for ");
                            stringBuilder3.append(UNCRYPT_PACKAGE_FILE);
                            Log.e(str, stringBuilder3.toString());
                        }
                        BLOCK_MAP_FILE.delete();
                    } finally {
                        uncryptFile.close();
                    }
                } else if (!BLOCK_MAP_FILE.exists()) {
                    Log.e(TAG, "Package claimed to have been processed but failed to find the block map file.");
                    throw new IOException("Failed to find block map file");
                }
                filename = "@/cache/recovery/block.map";
            }
            String filenameArg = new StringBuilder();
            filenameArg.append("--update_package=");
            filenameArg.append(filename);
            filenameArg.append("\n");
            filenameArg = filenameArg.toString();
            str = new StringBuilder();
            str.append("--locale=");
            str.append(Locale.getDefault().toLanguageTag());
            str.append("\n");
            str = str.toString();
            String securityArg = "--security\n";
            String command = new StringBuilder();
            command.append(filenameArg);
            command.append(str);
            command = command.toString();
            if (securityUpdate) {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(command);
                stringBuilder4.append("--security\n");
                command = stringBuilder4.toString();
            }
            if (((RecoverySystem) context.getSystemService(PowerManager.REBOOT_RECOVERY)).setupBcb(command)) {
                PowerManager pm = (PowerManager) context.getSystemService("power");
                String reason = PowerManager.REBOOT_RECOVERY_UPDATE;
                if (context.getPackageManager().hasSystemFeature("android.software.leanback") && ((WindowManager) context.getSystemService(AppAssociate.ASSOC_WINDOW)).getDefaultDisplay().getState() != 2) {
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append(reason);
                    stringBuilder5.append(",quiescent");
                    reason = stringBuilder5.toString();
                }
                pm.reboot(reason);
                throw new IOException("Reboot failed (no permissions?)");
            }
            throw new IOException("Setup BCB failed");
        }
    }

    @SystemApi
    public static void scheduleUpdateOnBoot(Context context, File packageFile) throws IOException {
        String filename = packageFile.getCanonicalPath();
        boolean securityUpdate = filename.endsWith("_s.zip");
        if (filename.startsWith("/data/")) {
            filename = "@/cache/recovery/block.map";
        }
        String filenameArg = new StringBuilder();
        filenameArg.append("--update_package=");
        filenameArg.append(filename);
        filenameArg.append("\n");
        filenameArg = filenameArg.toString();
        String localeArg = new StringBuilder();
        localeArg.append("--locale=");
        localeArg.append(Locale.getDefault().toLanguageTag());
        localeArg.append("\n");
        localeArg = localeArg.toString();
        String securityArg = "--security\n";
        String command = new StringBuilder();
        command.append(filenameArg);
        command.append(localeArg);
        command = command.toString();
        if (securityUpdate) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(command);
            stringBuilder.append("--security\n");
            command = stringBuilder.toString();
        }
        if (!((RecoverySystem) context.getSystemService(PowerManager.REBOOT_RECOVERY)).setupBcb(command)) {
            throw new IOException("schedule update on boot failed");
        }
    }

    @SystemApi
    public static void cancelScheduledUpdate(Context context) throws IOException {
        if (!((RecoverySystem) context.getSystemService(PowerManager.REBOOT_RECOVERY)).clearBcb()) {
            throw new IOException("cancel scheduled update failed");
        }
    }

    public static void rebootWipeUserData(Context context) throws IOException {
        rebootWipeUserData(context, false, context.getPackageName(), false, false);
    }

    public static void rebootWipeUserData(Context context, String reason) throws IOException {
        rebootWipeUserData(context, false, reason, false, false);
    }

    public static void rebootWipeUserData(Context context, boolean shutdown) throws IOException {
        rebootWipeUserData(context, shutdown, context.getPackageName(), false, false);
    }

    public static void rebootWipeUserData(Context context, boolean shutdown, String reason, boolean force) throws IOException {
        rebootWipeUserData(context, shutdown, reason, force, false);
    }

    public static void rebootWipeUserData(Context context, boolean shutdown, String reason, boolean force, boolean wipeEuicc) throws IOException {
        Context context2 = context;
        UserManager um = (UserManager) context2.getSystemService("user");
        if (force || !um.hasUserRestriction(UserManager.DISALLOW_FACTORY_RESET)) {
            Intent intent;
            final ConditionVariable condition = new ConditionVariable();
            if (SystemProperties.get("persist.sys.cc_mode", WifiEnterpriseConfig.ENGINE_DISABLE).equals(WifiEnterpriseConfig.ENGINE_DISABLE)) {
                intent = new Intent("android.intent.action.MASTER_CLEAR_NOTIFICATION");
            } else {
                intent = new Intent("android.intent.action.MASTER_CLEAR");
                intent.putExtra("masterClearWipeDataFactoryLowlevel", true);
            }
            Intent intent2 = intent;
            intent2.addFlags(285212672);
            context2.sendOrderedBroadcastAsUser(intent2, UserHandle.SYSTEM, "android.permission.MASTER_CLEAR", new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    condition.open();
                }
            }, null, 0, null, null);
            condition.block();
            if (wipeEuicc) {
                wipeEuiccData(context2, "android");
            }
            String shutdownArg = null;
            if (shutdown) {
                shutdownArg = "--shutdown_after";
            }
            String reasonArg = null;
            if (!TextUtils.isEmpty(reason)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("--reason=");
                stringBuilder.append(sanitizeArg(reason));
                reasonArg = stringBuilder.toString();
            }
            String localeArg = new StringBuilder();
            localeArg.append("--locale=");
            localeArg.append(Locale.getDefault().toLanguageTag());
            localeArg = localeArg.toString();
            bootCommand(context2, shutdownArg, "--wipe_data", reasonArg, localeArg);
            return;
        }
        throw new SecurityException("Wiping data is not allowed for this user.");
    }

    public static boolean wipeEuiccData(Context context, String packageName) {
        InterruptedException e;
        CountDownLatch countDownLatch;
        Throwable th;
        Context context2 = context;
        if (Global.getInt(context.getContentResolver(), Global.EUICC_PROVISIONED, 0) == 0) {
            Log.d(TAG, "Skipping eUICC wipe/retain as it is not provisioned");
            return true;
        }
        EuiccManager euiccManager = (EuiccManager) context2.getSystemService("euicc");
        if (euiccManager == null || !euiccManager.isEnabled()) {
            String str = packageName;
            return false;
        }
        final CountDownLatch euiccFactoryResetLatch = new CountDownLatch(1);
        final AtomicBoolean wipingSucceeded = new AtomicBoolean(false);
        BroadcastReceiver euiccWipeFinishReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (RecoverySystem.ACTION_EUICC_FACTORY_RESET.equals(intent.getAction())) {
                    if (getResultCode() != 0) {
                        int detailedCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, 0);
                        String str = RecoverySystem.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error wiping euicc data, Detailed code = ");
                        stringBuilder.append(detailedCode);
                        Log.e(str, stringBuilder.toString());
                    } else {
                        Log.d(RecoverySystem.TAG, "Successfully wiped euicc data.");
                        wipingSucceeded.set(true);
                    }
                    euiccFactoryResetLatch.countDown();
                }
            }
        };
        Intent intent = new Intent(ACTION_EUICC_FACTORY_RESET);
        intent.setPackage(packageName);
        PendingIntent callbackIntent = PendingIntent.getBroadcastAsUser(context2, 0, intent, 134217728, UserHandle.SYSTEM);
        IntentFilter filterConsent = new IntentFilter();
        filterConsent.addAction(ACTION_EUICC_FACTORY_RESET);
        HandlerThread euiccHandlerThread = new HandlerThread("euiccWipeFinishReceiverThread");
        euiccHandlerThread.start();
        context.getApplicationContext().registerReceiver(euiccWipeFinishReceiver, filterConsent, null, new Handler(euiccHandlerThread.getLooper()));
        euiccManager.eraseSubscriptions(callbackIntent);
        try {
            long waitingTimeMillis;
            CountDownLatch euiccFactoryResetLatch2 = euiccFactoryResetLatch;
            try {
                waitingTimeMillis = Global.getLong(context.getContentResolver(), Global.EUICC_FACTORY_RESET_TIMEOUT_MILLIS, DEFAULT_EUICC_FACTORY_RESET_TIMEOUT_MILLIS);
                if (waitingTimeMillis < 5000) {
                    waitingTimeMillis = 5000;
                } else if (waitingTimeMillis > 60000) {
                    waitingTimeMillis = 60000;
                }
            } catch (InterruptedException e2) {
                e = e2;
                countDownLatch = euiccFactoryResetLatch2;
                try {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "Wiping eUICC data interrupted", e);
                    context.getApplicationContext().unregisterReceiver(euiccWipeFinishReceiver);
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    context.getApplicationContext().unregisterReceiver(euiccWipeFinishReceiver);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                countDownLatch = euiccFactoryResetLatch2;
                context.getApplicationContext().unregisterReceiver(euiccWipeFinishReceiver);
                throw th;
            }
            try {
                if (euiccFactoryResetLatch2.await(waitingTimeMillis, TimeUnit.MILLISECONDS)) {
                    context.getApplicationContext().unregisterReceiver(euiccWipeFinishReceiver);
                    return wipingSucceeded.get();
                }
                Log.e(TAG, "Timeout wiping eUICC data.");
                context.getApplicationContext().unregisterReceiver(euiccWipeFinishReceiver);
                return false;
            } catch (InterruptedException e3) {
                e = e3;
                Thread.currentThread().interrupt();
                Log.e(TAG, "Wiping eUICC data interrupted", e);
                context.getApplicationContext().unregisterReceiver(euiccWipeFinishReceiver);
                return false;
            }
        } catch (InterruptedException e4) {
            e = e4;
            countDownLatch = euiccFactoryResetLatch;
            Thread.currentThread().interrupt();
            Log.e(TAG, "Wiping eUICC data interrupted", e);
            context.getApplicationContext().unregisterReceiver(euiccWipeFinishReceiver);
            return false;
        } catch (Throwable th4) {
            th = th4;
            countDownLatch = euiccFactoryResetLatch;
            context.getApplicationContext().unregisterReceiver(euiccWipeFinishReceiver);
            throw th;
        }
    }

    public static void rebootPromptAndWipeUserData(Context context, String reason) throws IOException {
        String reasonArg = null;
        if (!TextUtils.isEmpty(reason)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("--reason=");
            stringBuilder.append(sanitizeArg(reason));
            reasonArg = stringBuilder.toString();
        }
        String localeArg = new StringBuilder();
        localeArg.append("--locale=");
        localeArg.append(Locale.getDefault().toString());
        localeArg = localeArg.toString();
        bootCommand(context, null, "--prompt_and_wipe_data", reasonArg, localeArg);
    }

    public static void rebootWipeCache(Context context) throws IOException {
        rebootWipeCache(context, context.getPackageName());
    }

    public static void rebootWipeCache(Context context, String reason) throws IOException {
        String reasonArg = null;
        if (!TextUtils.isEmpty(reason)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("--reason=");
            stringBuilder.append(sanitizeArg(reason));
            reasonArg = stringBuilder.toString();
        }
        String localeArg = new StringBuilder();
        localeArg.append("--locale=");
        localeArg.append(Locale.getDefault().toLanguageTag());
        localeArg = localeArg.toString();
        bootCommand(context, "--wipe_cache", reasonArg, localeArg);
    }

    @SystemApi
    public static void rebootWipeAb(Context context, File packageFile, String reason) throws IOException {
        String reasonArg = null;
        if (!TextUtils.isEmpty(reason)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("--reason=");
            stringBuilder.append(sanitizeArg(reason));
            reasonArg = stringBuilder.toString();
        }
        String filename = packageFile.getCanonicalPath();
        String filenameArg = new StringBuilder();
        filenameArg.append("--wipe_package=");
        filenameArg.append(filename);
        filenameArg = filenameArg.toString();
        String localeArg = new StringBuilder();
        localeArg.append("--locale=");
        localeArg.append(Locale.getDefault().toLanguageTag());
        localeArg = localeArg.toString();
        bootCommand(context, "--wipe_ab", filenameArg, reasonArg, localeArg);
    }

    private static void bootCommand(Context context, String... args) throws IOException {
        LOG_FILE.delete();
        StringBuilder command = new StringBuilder();
        for (String arg : args) {
            if (!TextUtils.isEmpty(arg)) {
                command.append(arg);
                command.append("\n");
            }
        }
        ((RecoverySystem) context.getSystemService(PowerManager.REBOOT_RECOVERY)).rebootRecoveryWithCommand(command.toString());
        throw new IOException("Reboot failed (no permissions?)");
    }

    private static void parseLastInstallLog(Context context) {
        int causeCode;
        BufferedReader in;
        int errorCode;
        ArithmeticException ignored;
        StringBuilder stringBuilder;
        Throwable th;
        Throwable th2;
        Context context2 = context;
        try {
            BufferedReader in2 = new BufferedReader(new FileReader(LAST_INSTALL_FILE));
            int timeTotal = -1;
            int uncryptTime = -1;
            int i = -1;
            int errorCode2 = -1;
            int temperatureMax = -1;
            int temperatureEnd = -1;
            int temperatureStart = -1;
            int sourceVersion = -1;
            int bytesStashedInMiB = -1;
            int bytesWrittenInMiB = -1;
            String line = null;
            int causeCode2 = -1;
            while (true) {
                causeCode = causeCode2;
                try {
                    String readLine = in2.readLine();
                    line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    try {
                        int numIndex = line.indexOf(58);
                        if (numIndex == i) {
                            in = in2;
                            errorCode = errorCode2;
                        } else if (numIndex + 1 >= line.length()) {
                            in = in2;
                            errorCode = errorCode2;
                        } else {
                            String numString = line.substring(numIndex + 1).trim();
                            int i2;
                            String str;
                            try {
                                long parsedNum = Long.parseLong(numString);
                                int MiB = 1048576;
                                try {
                                    if (line.startsWith("bytes")) {
                                        in = in2;
                                        errorCode = errorCode2;
                                        try {
                                            causeCode2 = Math.toIntExact(parsedNum / Trace.TRACE_TAG_DATABASE);
                                        } catch (ArithmeticException e) {
                                            ignored = e;
                                            numString = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Number overflows in ");
                                            stringBuilder.append(line);
                                            Log.e(numString, stringBuilder.toString());
                                            causeCode2 = causeCode;
                                            in2 = in;
                                            errorCode2 = errorCode;
                                            i = -1;
                                        }
                                    } else {
                                        in = in2;
                                        i2 = numIndex;
                                        str = numString;
                                        errorCode = errorCode2;
                                        causeCode2 = Math.toIntExact(parsedNum);
                                    }
                                } catch (ArithmeticException e2) {
                                    ignored = e2;
                                    in = in2;
                                    i2 = numIndex;
                                    str = numString;
                                    errorCode = errorCode2;
                                    in2 = parsedNum;
                                    numString = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Number overflows in ");
                                    stringBuilder.append(line);
                                    Log.e(numString, stringBuilder.toString());
                                    causeCode2 = causeCode;
                                    in2 = in;
                                    errorCode2 = errorCode;
                                    i = -1;
                                }
                                try {
                                    if (line.startsWith("time")) {
                                        timeTotal = causeCode2;
                                    } else if (line.startsWith("uncrypt_time")) {
                                        uncryptTime = causeCode2;
                                    } else if (line.startsWith("source_build")) {
                                        sourceVersion = causeCode2;
                                    } else if (line.startsWith("bytes_written")) {
                                        bytesWrittenInMiB = bytesWrittenInMiB == -1 ? causeCode2 : bytesWrittenInMiB + causeCode2;
                                    } else if (line.startsWith("bytes_stashed")) {
                                        bytesStashedInMiB = bytesStashedInMiB == -1 ? causeCode2 : bytesStashedInMiB + causeCode2;
                                    } else if (line.startsWith("temperature_start")) {
                                        temperatureStart = causeCode2;
                                    } else if (line.startsWith("temperature_end")) {
                                        temperatureEnd = causeCode2;
                                    } else if (line.startsWith("temperature_max")) {
                                        temperatureMax = causeCode2;
                                    } else if (line.startsWith("error")) {
                                        errorCode2 = causeCode2;
                                        causeCode2 = causeCode;
                                        in2 = in;
                                        i = -1;
                                    } else if (line.startsWith("cause") != null) {
                                        errorCode2 = errorCode;
                                        in2 = in;
                                        i = -1;
                                    }
                                    causeCode2 = causeCode;
                                    errorCode2 = errorCode;
                                    in2 = in;
                                } catch (Throwable th3) {
                                    th2 = th3;
                                    in2 = in;
                                    th = null;
                                    $closeResource(th, in2);
                                    throw th2;
                                }
                            } catch (NumberFormatException ignored2) {
                                in = in2;
                                i2 = numIndex;
                                str = numString;
                                errorCode = errorCode2;
                                NumberFormatException in3 = ignored2;
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Failed to parse numbers in ");
                                stringBuilder2.append(line);
                                Log.e(str2, stringBuilder2.toString());
                            }
                            i = -1;
                        }
                        causeCode2 = causeCode;
                        in2 = in;
                        errorCode2 = errorCode;
                        i = -1;
                    } catch (Throwable th4) {
                        th2 = th4;
                        th = null;
                        $closeResource(th, in2);
                        throw th2;
                    }
                } catch (Throwable th5) {
                    th2 = th5;
                    th = null;
                    $closeResource(th, in2);
                    throw th2;
                }
            }
            in = in2;
            errorCode = errorCode2;
            if (timeTotal != -1) {
                MetricsLogger.histogram(context2, "ota_time_total", timeTotal);
            }
            if (uncryptTime != -1) {
                MetricsLogger.histogram(context2, "ota_uncrypt_time", uncryptTime);
            }
            if (sourceVersion != -1) {
                MetricsLogger.histogram(context2, "ota_source_version", sourceVersion);
            }
            if (bytesWrittenInMiB != -1) {
                MetricsLogger.histogram(context2, "ota_written_in_MiBs", bytesWrittenInMiB);
            }
            if (bytesStashedInMiB != -1) {
                MetricsLogger.histogram(context2, "ota_stashed_in_MiBs", bytesStashedInMiB);
            }
            if (temperatureStart != -1) {
                MetricsLogger.histogram(context2, "ota_temperature_start", temperatureStart);
            }
            if (temperatureEnd != -1) {
                MetricsLogger.histogram(context2, "ota_temperature_end", temperatureEnd);
            }
            if (temperatureMax != -1) {
                MetricsLogger.histogram(context2, "ota_temperature_max", temperatureMax);
            }
            errorCode2 = errorCode;
            if (errorCode2 != -1) {
                MetricsLogger.histogram(context2, "ota_non_ab_error_code", errorCode2);
            }
            if (causeCode != -1) {
                MetricsLogger.histogram(context2, "ota_non_ab_cause_code", causeCode);
            }
            $closeResource(null, in);
        } catch (IOException e3) {
            Log.e(TAG, "Failed to read lines in last_install", e3);
        }
    }

    public static String handleAftermath(Context context) {
        String log = null;
        try {
            log = FileUtils.readTextFile(LOG_FILE, Menu.CATEGORY_MASK, "...\n");
        } catch (FileNotFoundException e) {
            Log.i(TAG, "No recovery log file");
        } catch (IOException e2) {
            Log.e(TAG, "Error reading recovery log", e2);
        }
        if (log != null) {
            parseLastInstallLog(context);
        }
        boolean reservePackage = BLOCK_MAP_FILE.exists();
        int i = 0;
        if (!reservePackage && UNCRYPT_PACKAGE_FILE.exists()) {
            String filename = null;
            try {
                filename = FileUtils.readTextFile(UNCRYPT_PACKAGE_FILE, 0, null);
            } catch (IOException e3) {
                Log.e(TAG, "Error reading uncrypt file", e3);
            }
            if (filename != null && filename.startsWith("/data")) {
                String str;
                StringBuilder stringBuilder;
                if (UNCRYPT_PACKAGE_FILE.delete()) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Deleted: ");
                    stringBuilder.append(filename);
                    Log.i(str, stringBuilder.toString());
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't delete: ");
                    stringBuilder.append(filename);
                    Log.e(str, stringBuilder.toString());
                }
            }
        }
        String[] names = RECOVERY_DIR.list();
        while (names != null && i < names.length) {
            if (!(names[i].startsWith(LAST_PREFIX) || ((reservePackage && names[i].equals(BLOCK_MAP_FILE.getName())) || ((reservePackage && names[i].equals(UNCRYPT_PACKAGE_FILE.getName())) || names[i].trim().equals(COMMAND_FILE_STRING))))) {
                recursiveDelete(new File(RECOVERY_DIR, names[i]));
            }
            i++;
        }
        return log;
    }

    private static void recursiveDelete(File name) {
        if (name.isDirectory()) {
            String[] files = name.list();
            int i = 0;
            while (files != null && i < files.length) {
                recursiveDelete(new File(name, files[i]));
                i++;
            }
        }
        String str;
        StringBuilder stringBuilder;
        if (name.delete()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Deleted: ");
            stringBuilder.append(name);
            Log.i(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Can't delete: ");
        stringBuilder.append(name);
        Log.e(str, stringBuilder.toString());
    }

    private boolean uncrypt(String packageFile, IRecoverySystemProgressListener listener) {
        try {
            return this.mService.uncrypt(packageFile, listener);
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean setupBcb(String command) {
        try {
            return this.mService.setupBcb(command);
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean clearBcb() {
        try {
            return this.mService.clearBcb();
        } catch (RemoteException e) {
            return false;
        }
    }

    private void rebootRecoveryWithCommand(String command) {
        try {
            this.mService.rebootRecoveryWithCommand(command);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to execute rebootRecoveryWithCommand!");
        }
    }

    private static String sanitizeArg(String arg) {
        return arg.replace(0, '?').replace(10, '?');
    }

    public RecoverySystem() {
        this.mService = null;
    }

    public RecoverySystem(IRecoverySystem service) {
        this.mService = service;
    }

    public static void hwBootCommand(Context context, String arg) throws IOException {
        bootCommand(context, arg);
    }

    public static void hwWipeEuiccData(Context context) {
        wipeEuiccData(context, "android");
    }
}
