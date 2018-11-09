package com.android.server.timezone;

import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.FastXmlSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class PackageStatusStorage {
    private static final String ATTRIBUTE_CHECK_STATUS = "checkStatus";
    private static final String ATTRIBUTE_DATA_APP_VERSION = "dataAppPackageVersion";
    private static final String ATTRIBUTE_OPTIMISTIC_LOCK_ID = "optimisticLockId";
    private static final String ATTRIBUTE_UPDATE_APP_VERSION = "updateAppPackageVersion";
    private static final String LOG_TAG = "timezone.PackageStatusStorage";
    private static final String TAG_PACKAGE_STATUS = "PackageStatus";
    private static final int UNKNOWN_PACKAGE_VERSION = -1;
    private final AtomicFile mPackageStatusFile;

    PackageStatusStorage(File storageDir) {
        this.mPackageStatusFile = new AtomicFile(new File(storageDir, "package-status.xml"));
        if (!this.mPackageStatusFile.getBaseFile().exists()) {
            try {
                insertInitialPackageStatus();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    void deleteFileForTests() {
        synchronized (this) {
            this.mPackageStatusFile.delete();
        }
    }

    PackageStatus getPackageStatus() {
        PackageStatus packageStatusLocked;
        synchronized (this) {
            try {
                packageStatusLocked = getPackageStatusLocked();
            } catch (ParseException e2) {
                throw new IllegalStateException("Recovery from bad file failed", e2);
            } catch (ParseException e) {
                Slog.e(LOG_TAG, "Package status invalid, resetting and retrying", e);
                recoverFromBadData(e);
                return getPackageStatusLocked();
            }
        }
        return packageStatusLocked;
    }

    @GuardedBy("this")
    private PackageStatus getPackageStatusLocked() throws ParseException {
        Throwable th;
        Throwable th2 = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = this.mPackageStatusFile.openRead();
            XmlPullParser parser = parseToPackageStatusTag(fileInputStream);
            Integer checkStatus = getNullableIntAttribute(parser, ATTRIBUTE_CHECK_STATUS);
            if (checkStatus == null) {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                th = null;
                if (th == null) {
                    return null;
                }
                try {
                    throw th;
                } catch (IOException e) {
                    ParseException e2 = new ParseException("Error reading package status", 0);
                    e2.initCause(e);
                    throw e2;
                }
            }
            PackageStatus packageStatus = new PackageStatus(checkStatus.intValue(), new PackageVersions(getIntAttribute(parser, ATTRIBUTE_UPDATE_APP_VERSION), getIntAttribute(parser, ATTRIBUTE_DATA_APP_VERSION)));
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Throwable th4) {
                    th2 = th4;
                }
            }
            if (th2 == null) {
                return packageStatus;
            }
            throw th2;
        } catch (Throwable th22) {
            Throwable th5 = th22;
            th22 = th;
            th = th5;
        }
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (Throwable th6) {
                if (th22 == null) {
                    th22 = th6;
                } else if (th22 != th6) {
                    th22.addSuppressed(th6);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    @GuardedBy("this")
    private int recoverFromBadData(Exception cause) {
        this.mPackageStatusFile.delete();
        try {
            return insertInitialPackageStatus();
        } catch (IOException e) {
            IllegalStateException fatal = new IllegalStateException(e);
            fatal.addSuppressed(cause);
            throw fatal;
        }
    }

    private int insertInitialPackageStatus() throws IOException {
        int initialOptimisticLockId = (int) System.currentTimeMillis();
        writePackageStatusLocked(null, initialOptimisticLockId, null);
        return initialOptimisticLockId;
    }

    CheckToken generateCheckToken(PackageVersions currentInstalledVersions) {
        if (currentInstalledVersions == null) {
            throw new NullPointerException("currentInstalledVersions == null");
        }
        CheckToken checkToken;
        synchronized (this) {
            int optimisticLockId;
            try {
                optimisticLockId = getCurrentOptimisticLockId();
            } catch (ParseException e) {
                Slog.w(LOG_TAG, "Unable to find optimistic lock ID from package status");
                optimisticLockId = recoverFromBadData(e);
            }
            int newOptimisticLockId = optimisticLockId + 1;
            try {
                if (writePackageStatusWithOptimisticLockCheck(optimisticLockId, newOptimisticLockId, Integer.valueOf(1), currentInstalledVersions)) {
                    checkToken = new CheckToken(newOptimisticLockId, currentInstalledVersions);
                } else {
                    throw new IllegalStateException("Unable to update status to CHECK_STARTED. synchronization failure?");
                }
            } catch (IOException e2) {
                throw new IllegalStateException(e2);
            }
        }
        return checkToken;
    }

    void resetCheckState() {
        synchronized (this) {
            int optimisticLockId;
            try {
                optimisticLockId = getCurrentOptimisticLockId();
            } catch (ParseException e) {
                Slog.w(LOG_TAG, "resetCheckState: Unable to find optimistic lock ID from package status");
                optimisticLockId = recoverFromBadData(e);
            }
            int newOptimisticLockId = optimisticLockId + 1;
            try {
                if (writePackageStatusWithOptimisticLockCheck(optimisticLockId, newOptimisticLockId, null, null)) {
                } else {
                    throw new IllegalStateException("resetCheckState: Unable to reset package status, newOptimisticLockId=" + newOptimisticLockId);
                }
            } catch (IOException e2) {
                throw new IllegalStateException(e2);
            }
        }
    }

    boolean markChecked(CheckToken checkToken, boolean succeeded) {
        boolean writePackageStatusWithOptimisticLockCheck;
        synchronized (this) {
            int optimisticLockId = checkToken.mOptimisticLockId;
            try {
                writePackageStatusWithOptimisticLockCheck = writePackageStatusWithOptimisticLockCheck(optimisticLockId, optimisticLockId + 1, Integer.valueOf(succeeded ? 2 : 3), checkToken.mPackageVersions);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return writePackageStatusWithOptimisticLockCheck;
    }

    @GuardedBy("this")
    private int getCurrentOptimisticLockId() throws ParseException {
        Throwable th;
        Throwable th2 = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = this.mPackageStatusFile.openRead();
            int intAttribute = getIntAttribute(parseToPackageStatusTag(fileInputStream), ATTRIBUTE_OPTIMISTIC_LOCK_ID);
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 == null) {
                return intAttribute;
            }
            try {
                throw th2;
            } catch (IOException e) {
                ParseException e2 = new ParseException("Unable to read file", 0);
                e2.initCause(e);
                throw e2;
            }
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    private static XmlPullParser parseToPackageStatusTag(FileInputStream fis) throws ParseException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    String tag = parser.getName();
                    if (type == 2 && TAG_PACKAGE_STATUS.equals(tag)) {
                        return parser;
                    }
                } else {
                    throw new ParseException("Unable to find PackageStatus tag", 0);
                }
            }
        } catch (XmlPullParserException e) {
            throw new IllegalStateException("Unable to configure parser", e);
        } catch (IOException e2) {
            ParseException e22 = new ParseException("Error reading XML", 0);
            e2.initCause(e2);
            throw e22;
        }
    }

    @GuardedBy("this")
    private boolean writePackageStatusWithOptimisticLockCheck(int optimisticLockId, int newOptimisticLockId, Integer status, PackageVersions packageVersions) throws IOException {
        try {
            if (getCurrentOptimisticLockId() != optimisticLockId) {
                return false;
            }
            writePackageStatusLocked(status, newOptimisticLockId, packageVersions);
            return true;
        } catch (ParseException e) {
            recoverFromBadData(e);
            return false;
        }
    }

    @GuardedBy("this")
    private void writePackageStatusLocked(Integer status, int optimisticLockId, PackageVersions packageVersions) throws IOException {
        Object obj;
        Object obj2 = 1;
        if (status == null) {
            obj = 1;
        } else {
            obj = null;
        }
        if (packageVersions != null) {
            obj2 = null;
        }
        if (obj != obj2) {
            throw new IllegalArgumentException("Provide both status and packageVersions, or neither.");
        }
        try {
            FileOutputStream fos = this.mPackageStatusFile.startWrite();
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(fos, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.startTag(null, TAG_PACKAGE_STATUS);
            serializer.attribute(null, ATTRIBUTE_CHECK_STATUS, status == null ? "" : Integer.toString(status.intValue()));
            serializer.attribute(null, ATTRIBUTE_OPTIMISTIC_LOCK_ID, Integer.toString(optimisticLockId));
            serializer.attribute(null, ATTRIBUTE_UPDATE_APP_VERSION, Integer.toString(status == null ? -1 : packageVersions.mUpdateAppVersion));
            serializer.attribute(null, ATTRIBUTE_DATA_APP_VERSION, Integer.toString(status == null ? -1 : packageVersions.mDataAppVersion));
            serializer.endTag(null, TAG_PACKAGE_STATUS);
            serializer.endDocument();
            serializer.flush();
            this.mPackageStatusFile.finishWrite(fos);
        } catch (IOException e) {
            if (null != null) {
                this.mPackageStatusFile.failWrite(null);
            }
            throw e;
        }
    }

    public void forceCheckStateForTests(int checkStatus, PackageVersions packageVersions) {
        synchronized (this) {
            try {
                int optimisticLockId = getCurrentOptimisticLockId();
                writePackageStatusWithOptimisticLockCheck(optimisticLockId, optimisticLockId, Integer.valueOf(checkStatus), packageVersions);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static Integer getNullableIntAttribute(XmlPullParser parser, String attributeName) throws ParseException {
        String attributeValue = parser.getAttributeValue(null, attributeName);
        if (attributeValue == null) {
            try {
                throw new ParseException("Attribute " + attributeName + " missing", 0);
            } catch (NumberFormatException e) {
                throw new ParseException("Bad integer for attributeName=" + attributeName + ": " + attributeValue, 0);
            }
        } else if (attributeValue.isEmpty()) {
            return null;
        } else {
            return Integer.valueOf(Integer.parseInt(attributeValue));
        }
    }

    private static int getIntAttribute(XmlPullParser parser, String attributeName) throws ParseException {
        Integer value = getNullableIntAttribute(parser, attributeName);
        if (value != null) {
            return value.intValue();
        }
        throw new ParseException("Missing attribute " + attributeName, 0);
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("Package status: " + getPackageStatus());
    }
}
