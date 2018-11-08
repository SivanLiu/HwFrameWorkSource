package com.android.server.backup.utils;

import android.app.backup.IBackupManagerMonitor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Slog;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.restore.RestorePolicy;
import com.android.server.usb.descriptors.UsbEndpointDescriptor;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TarBackupReader {
    private static final int TAR_HEADER_LENGTH_FILESIZE = 12;
    private static final int TAR_HEADER_LENGTH_MODE = 8;
    private static final int TAR_HEADER_LENGTH_MODTIME = 12;
    private static final int TAR_HEADER_LENGTH_PATH = 100;
    private static final int TAR_HEADER_LENGTH_PATH_PREFIX = 155;
    private static final int TAR_HEADER_LONG_RADIX = 8;
    private static final int TAR_HEADER_OFFSET_FILESIZE = 124;
    private static final int TAR_HEADER_OFFSET_MODE = 100;
    private static final int TAR_HEADER_OFFSET_MODTIME = 136;
    private static final int TAR_HEADER_OFFSET_PATH = 0;
    private static final int TAR_HEADER_OFFSET_PATH_PREFIX = 345;
    private static final int TAR_HEADER_OFFSET_TYPE_CHAR = 156;
    private final BytesReadListener mBytesReadListener;
    private final InputStream mInputStream;
    private IBackupManagerMonitor mMonitor;
    private byte[] mWidgetData = null;

    public TarBackupReader(InputStream inputStream, BytesReadListener bytesReadListener, IBackupManagerMonitor monitor) {
        this.mInputStream = inputStream;
        this.mBytesReadListener = bytesReadListener;
        this.mMonitor = monitor;
    }

    public FileMetadata readTarHeaders() throws IOException {
        IOException e;
        byte[] block = new byte[512];
        FileMetadata fileMetadata = null;
        if (readTarHeader(block)) {
            try {
                FileMetadata info = new FileMetadata();
                try {
                    info.size = extractRadix(block, TAR_HEADER_OFFSET_FILESIZE, 12, 8);
                    info.mtime = extractRadix(block, 136, 12, 8);
                    info.mode = extractRadix(block, 100, 8, 8);
                    info.path = extractString(block, TAR_HEADER_OFFSET_PATH_PREFIX, TAR_HEADER_LENGTH_PATH_PREFIX);
                    String path = extractString(block, 0, 100);
                    if (path.length() > 0) {
                        if (info.path.length() > 0) {
                            info.path += '/';
                        }
                        info.path += path;
                    }
                    int typeChar = block[TAR_HEADER_OFFSET_TYPE_CHAR];
                    if (typeChar == 120) {
                        boolean gotHeader = readPaxExtendedHeader(info);
                        if (gotHeader) {
                            gotHeader = readTarHeader(block);
                        }
                        if (gotHeader) {
                            typeChar = block[TAR_HEADER_OFFSET_TYPE_CHAR];
                        } else {
                            throw new IOException("Bad or missing pax header");
                        }
                    }
                    switch (typeChar) {
                        case 0:
                            return null;
                        case 48:
                            info.type = 1;
                            break;
                        case 53:
                            info.type = 2;
                            if (info.size != 0) {
                                Slog.w(RefactoredBackupManagerService.TAG, "Directory entry with nonzero size in header");
                                info.size = 0;
                                break;
                            }
                            break;
                        default:
                            Slog.e(RefactoredBackupManagerService.TAG, "Unknown tar entity type: " + typeChar);
                            throw new IOException("Unknown entity type " + typeChar);
                    }
                    if ("shared/".regionMatches(0, info.path, 0, "shared/".length())) {
                        info.path = info.path.substring("shared/".length());
                        info.packageName = RefactoredBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE;
                        info.domain = "shared";
                        Slog.i(RefactoredBackupManagerService.TAG, "File in shared storage: " + info.path);
                    } else if ("apps/".regionMatches(0, info.path, 0, "apps/".length())) {
                        info.path = info.path.substring("apps/".length());
                        int slash = info.path.indexOf(47);
                        if (slash < 0) {
                            throw new IOException("Illegal semantic path in " + info.path);
                        }
                        info.packageName = info.path.substring(0, slash);
                        info.path = info.path.substring(slash + 1);
                        if (!(info.path.equals(RefactoredBackupManagerService.BACKUP_MANIFEST_FILENAME) || (info.path.equals(RefactoredBackupManagerService.BACKUP_METADATA_FILENAME) ^ 1) == 0)) {
                            slash = info.path.indexOf(47);
                            if (slash < 0) {
                                throw new IOException("Illegal semantic path in non-manifest " + info.path);
                            }
                            info.domain = info.path.substring(0, slash);
                            info.path = info.path.substring(slash + 1);
                        }
                    }
                    fileMetadata = info;
                } catch (IOException e2) {
                    e = e2;
                    fileMetadata = info;
                    Slog.e(RefactoredBackupManagerService.TAG, "Parse error in header: " + e.getMessage());
                    throw e;
                }
            } catch (IOException e3) {
                e = e3;
                Slog.e(RefactoredBackupManagerService.TAG, "Parse error in header: " + e.getMessage());
                throw e;
            }
        }
        return fileMetadata;
    }

    private static int readExactly(InputStream in, byte[] buffer, int offset, int size) throws IOException {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        int soFar = 0;
        while (soFar < size) {
            int nRead = in.read(buffer, offset + soFar, size - soFar);
            if (nRead <= 0) {
                break;
            }
            soFar += nRead;
        }
        return soFar;
    }

    public Signature[] readAppManifestAndReturnSignatures(FileMetadata info) throws IOException {
        if (info.size > 65536) {
            throw new IOException("Restore manifest too big; corrupt? size=" + info.size);
        }
        byte[] buffer = new byte[((int) info.size)];
        if (((long) readExactly(this.mInputStream, buffer, 0, (int) info.size)) == info.size) {
            this.mBytesReadListener.onBytesRead(info.size);
            String[] str = new String[1];
            try {
                int offset = extractLine(buffer, 0, str);
                int version = Integer.parseInt(str[0]);
                if (version == 1) {
                    offset = extractLine(buffer, offset, str);
                    String manifestPackage = str[0];
                    if (manifestPackage.equals(info.packageName)) {
                        offset = extractLine(buffer, offset, str);
                        info.version = Integer.parseInt(str[0]);
                        offset = extractLine(buffer, offset, str);
                        Integer.parseInt(str[0]);
                        offset = extractLine(buffer, offset, str);
                        info.installerPackageName = str[0].length() > 0 ? str[0] : null;
                        offset = extractLine(buffer, offset, str);
                        info.hasApk = str[0].equals("1");
                        offset = extractLine(buffer, offset, str);
                        int numSigs = Integer.parseInt(str[0]);
                        if (numSigs > 0) {
                            Signature[] sigs = new Signature[numSigs];
                            for (int i = 0; i < numSigs; i++) {
                                offset = extractLine(buffer, offset, str);
                                sigs[i] = new Signature(str[0]);
                            }
                            return sigs;
                        }
                        Slog.i(RefactoredBackupManagerService.TAG, "Missing signature on backed-up package " + info.packageName);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 42, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
                    } else {
                        Slog.i(RefactoredBackupManagerService.TAG, "Expected package " + info.packageName + " but restore manifest claims " + manifestPackage);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 43, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName), "android.app.backup.extra.LOG_MANIFEST_PACKAGE_NAME", manifestPackage));
                    }
                    return null;
                }
                Slog.i(RefactoredBackupManagerService.TAG, "Unknown restore manifest version " + version + " for package " + info.packageName);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 44, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName), "android.app.backup.extra.LOG_EVENT_PACKAGE_VERSION", (long) version));
                return null;
            } catch (NumberFormatException e) {
                Slog.w(RefactoredBackupManagerService.TAG, "Corrupt restore manifest for package " + info.packageName);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 46, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
            } catch (IllegalArgumentException e2) {
                Slog.w(RefactoredBackupManagerService.TAG, e2.getMessage());
            }
        } else {
            throw new IOException("Unexpected EOF in manifest");
        }
    }

    public RestorePolicy chooseRestorePolicy(PackageManager packageManager, boolean allowApks, FileMetadata info, Signature[] signatures) {
        if (signatures == null) {
            return RestorePolicy.IGNORE;
        }
        RestorePolicy policy = RestorePolicy.IGNORE;
        try {
            PackageInfo pkgInfo = packageManager.getPackageInfo(info.packageName, 64);
            if ((32768 & pkgInfo.applicationInfo.flags) == 0) {
                Slog.i(RefactoredBackupManagerService.TAG, "Restore manifest from " + info.packageName + " but allowBackup=false");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 39, pkgInfo, 3, null);
            } else if (pkgInfo.applicationInfo.uid < 10000 && pkgInfo.applicationInfo.backupAgentName == null) {
                Slog.w(RefactoredBackupManagerService.TAG, "Package " + info.packageName + " is system level with no agent");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 38, pkgInfo, 2, null);
            } else if (!AppBackupUtils.signaturesMatch(signatures, pkgInfo)) {
                Slog.w(RefactoredBackupManagerService.TAG, "Restore manifest signatures do not match installed application for " + info.packageName);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 37, pkgInfo, 3, null);
            } else if ((pkgInfo.applicationInfo.flags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0) {
                Slog.i(RefactoredBackupManagerService.TAG, "Package has restoreAnyVersion; taking data");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 34, pkgInfo, 3, null);
                policy = RestorePolicy.ACCEPT;
            } else if (pkgInfo.versionCode >= info.version) {
                Slog.i(RefactoredBackupManagerService.TAG, "Sig + version match; taking data");
                policy = RestorePolicy.ACCEPT;
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 35, pkgInfo, 3, null);
            } else if (allowApks) {
                Slog.i(RefactoredBackupManagerService.TAG, "Data version " + info.version + " is newer than installed " + "version " + pkgInfo.versionCode + " - requiring apk");
                policy = RestorePolicy.ACCEPT_IF_APK;
            } else {
                Slog.i(RefactoredBackupManagerService.TAG, "Data requires newer version " + info.version + "; ignoring");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 36, pkgInfo, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_OLD_VERSION", (long) info.version));
                policy = RestorePolicy.IGNORE;
            }
        } catch (NameNotFoundException e) {
            if (allowApks) {
                Slog.i(RefactoredBackupManagerService.TAG, "Package " + info.packageName + " not installed; requiring apk in dataset");
                policy = RestorePolicy.ACCEPT_IF_APK;
            } else {
                policy = RestorePolicy.IGNORE;
            }
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 40, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName), "android.app.backup.extra.LOG_POLICY_ALLOW_APKS", allowApks));
        }
        if (policy == RestorePolicy.ACCEPT_IF_APK && (info.hasApk ^ 1) != 0) {
            Slog.i(RefactoredBackupManagerService.TAG, "Cannot restore package " + info.packageName + " without the matching .apk");
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 41, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
        }
        return policy;
    }

    public void skipTarPadding(long size) throws IOException {
        long partial = (size + 512) % 512;
        if (partial > 0) {
            int needed = 512 - ((int) partial);
            if (readExactly(this.mInputStream, new byte[needed], 0, needed) == needed) {
                this.mBytesReadListener.onBytesRead((long) needed);
                return;
            }
            throw new IOException("Unexpected EOF in padding");
        }
    }

    public void readMetadata(FileMetadata info) throws IOException {
        if (info.size > 65536) {
            throw new IOException("Metadata too big; corrupt? size=" + info.size);
        }
        byte[] buffer = new byte[((int) info.size)];
        if (((long) readExactly(this.mInputStream, buffer, 0, (int) info.size)) == info.size) {
            this.mBytesReadListener.onBytesRead(info.size);
            String[] str = new String[1];
            int offset = extractLine(buffer, 0, str);
            int version = Integer.parseInt(str[0]);
            if (version == 1) {
                offset = extractLine(buffer, offset, str);
                String pkg = str[0];
                if (info.packageName.equals(pkg)) {
                    ByteArrayInputStream bin = new ByteArrayInputStream(buffer, offset, buffer.length - offset);
                    DataInputStream in = new DataInputStream(bin);
                    while (bin.available() > 0) {
                        int token = in.readInt();
                        int size = in.readInt();
                        if (size <= 65536) {
                            switch (token) {
                                case RefactoredBackupManagerService.BACKUP_WIDGET_METADATA_TOKEN /*33549569*/:
                                    this.mWidgetData = new byte[size];
                                    in.read(this.mWidgetData);
                                    break;
                                default:
                                    Slog.i(RefactoredBackupManagerService.TAG, "Ignoring metadata blob " + Integer.toHexString(token) + " for " + info.packageName);
                                    in.skipBytes(size);
                                    break;
                            }
                        }
                        throw new IOException("Datum " + Integer.toHexString(token) + " too big; corrupt? size=" + info.size);
                    }
                    return;
                }
                Slog.w(RefactoredBackupManagerService.TAG, "Metadata mismatch: package " + info.packageName + " but widget data for " + pkg);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 47, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName), "android.app.backup.extra.LOG_WIDGET_PACKAGE_NAME", pkg));
                return;
            }
            Slog.w(RefactoredBackupManagerService.TAG, "Unsupported metadata version " + version);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 48, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName), "android.app.backup.extra.LOG_EVENT_PACKAGE_VERSION", (long) version));
            return;
        }
        throw new IOException("Unexpected EOF in widget data");
    }

    private static int extractLine(byte[] buffer, int offset, String[] outStr) throws IOException {
        int end = buffer.length;
        if (offset >= end) {
            throw new IOException("Incomplete data");
        }
        int pos = offset;
        while (pos < end && buffer[pos] != (byte) 10) {
            pos++;
        }
        outStr[0] = new String(buffer, offset, pos - offset);
        return pos + 1;
    }

    private boolean readTarHeader(byte[] block) throws IOException {
        int got = readExactly(this.mInputStream, block, 0, 512);
        if (got == 0) {
            return false;
        }
        if (got < 512) {
            throw new IOException("Unable to read full block header");
        }
        this.mBytesReadListener.onBytesRead(512);
        return true;
    }

    private boolean readPaxExtendedHeader(FileMetadata info) throws IOException {
        if (info.size > 32768) {
            Slog.w(RefactoredBackupManagerService.TAG, "Suspiciously large pax header size " + info.size + " - aborting");
            throw new IOException("Sanity failure: pax header size " + info.size);
        }
        byte[] data = new byte[(((int) ((info.size + 511) >> 9)) * 512)];
        if (readExactly(this.mInputStream, data, 0, data.length) < data.length) {
            throw new IOException("Unable to read full pax header");
        }
        this.mBytesReadListener.onBytesRead((long) data.length);
        int contentSize = (int) info.size;
        int offset = 0;
        do {
            int eol = offset + 1;
            while (eol < contentSize && data[eol] != UsbEndpointDescriptor.USEAGE_EXPLICIT) {
                eol++;
            }
            if (eol >= contentSize) {
                throw new IOException("Invalid pax data");
            }
            int linelen = (int) extractRadix(data, offset, eol - offset, 10);
            int key = eol + 1;
            eol = (offset + linelen) - 1;
            int value = key + 1;
            while (data[value] != (byte) 61 && value <= eol) {
                value++;
            }
            if (value > eol) {
                throw new IOException("Invalid pax declaration");
            }
            String keyStr = new String(data, key, value - key, "UTF-8");
            String valStr = new String(data, value + 1, (eol - value) - 1, "UTF-8");
            if ("path".equals(keyStr)) {
                info.path = valStr;
            } else if ("size".equals(keyStr)) {
                info.size = Long.parseLong(valStr);
            } else {
                Slog.i(RefactoredBackupManagerService.TAG, "Unhandled pax key: " + key);
            }
            offset += linelen;
        } while (offset < contentSize);
        return true;
    }

    private static long extractRadix(byte[] data, int offset, int maxChars, int radix) throws IOException {
        long value = 0;
        int end = offset + maxChars;
        int i = offset;
        while (i < end) {
            byte b = data[i];
            if (b == (byte) 0 || b == UsbEndpointDescriptor.USEAGE_EXPLICIT) {
                break;
            } else if (b < (byte) 48 || b > (radix + 48) - 1) {
                throw new IOException("Invalid number in header: '" + ((char) b) + "' for radix " + radix);
            } else {
                value = (((long) radix) * value) + ((long) (b - 48));
                i++;
            }
        }
        return value;
    }

    private static String extractString(byte[] data, int offset, int maxChars) throws IOException {
        int end = offset + maxChars;
        int eos = offset;
        while (eos < end && data[eos] != (byte) 0) {
            eos++;
        }
        return new String(data, offset, eos - offset, "US-ASCII");
    }

    private static void hexLog(byte[] block) {
        int offset = 0;
        int todo = block.length;
        StringBuilder buf = new StringBuilder(64);
        while (todo > 0) {
            buf.append(String.format("%04x   ", new Object[]{Integer.valueOf(offset)}));
            int numThisLine = todo > 16 ? 16 : todo;
            for (int i = 0; i < numThisLine; i++) {
                buf.append(String.format("%02x ", new Object[]{Byte.valueOf(block[offset + i])}));
            }
            Slog.i("hexdump", buf.toString());
            buf.setLength(0);
            todo -= numThisLine;
            offset += numThisLine;
        }
    }

    public IBackupManagerMonitor getMonitor() {
        return this.mMonitor;
    }

    public byte[] getWidgetData() {
        return this.mWidgetData;
    }
}
