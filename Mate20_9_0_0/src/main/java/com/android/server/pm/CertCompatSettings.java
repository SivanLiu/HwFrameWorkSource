package com.android.server.pm;

import android.content.pm.Signature;
import android.os.Environment;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class CertCompatSettings {
    private static final String ATTR_CERT = "cert";
    private static final String ATTR_CODEPATH = "codePath";
    private static final String ATTR_FT = "ft";
    private static final String ATTR_HASH = "hash";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_NAME = "name";
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    private static final String SIGNATURE_VERSION_ONE = "HwSignature_V1";
    private static final String SIGNATURE_VERSION_TWO = "HwSignature_V2";
    private static final String TAG = "CertCompatSettings";
    private static final String TAG_ITEM = "item";
    private static final String TAG_VERSIONS = "versions";
    private final File mBackupCompatiblePackageFilename;
    private boolean mCompatAll = false;
    private final File mCompatiblePackageFilename;
    private boolean mFoundSystemCertFile;
    private boolean mFoundWhiteListFile;
    private HashMap<String, Signature[]> mNewSigns = new HashMap();
    private HashMap<String, Signature[]> mOldSigns = new HashMap();
    final HashMap<String, Package> mPackages = new HashMap();
    private final File mSyscertFilename;
    final HashMap<String, WhiteListPackage> mWhiteList = new HashMap();
    private final File mWhiteListFilename;

    static final class Package {
        String certType;
        String codePath;
        String packageName;
        long timeStamp;

        Package(String packageName, String codePath, long timeStamp, String certType) {
            this.packageName = packageName;
            this.codePath = codePath;
            this.timeStamp = timeStamp;
            this.certType = certType;
        }
    }

    private static final class WhiteListPackage {
        List<byte[]> hashList = new ArrayList();
        String packageName;

        WhiteListPackage(String packageName) {
            this.packageName = packageName;
        }
    }

    CertCompatSettings() {
        File securityDir = new File(Environment.getRootDirectory(), "etc/security");
        this.mWhiteListFilename = new File(securityDir, "trusted_app.xml");
        this.mSyscertFilename = new File(securityDir, "hwsyscert.xml");
        File systemDir = new File(Environment.getDataDirectory(), "system");
        this.mCompatiblePackageFilename = new File(systemDir, "certcompat.xml");
        this.mBackupCompatiblePackageFilename = new File(systemDir, "certcompat-backup.xml");
        this.mFoundSystemCertFile = loadTrustedCerts(this.mSyscertFilename);
        this.mFoundWhiteListFile = this.mWhiteListFilename.exists();
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0037 A:{SYNTHETIC, Splitter: B:15:0x0037} */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x0032  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean loadTrustedCerts(File file) {
        FileInputStream str;
        if (file.exists()) {
            str = null;
            try {
                int type;
                str = new FileInputStream(file);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(str, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = parser.next();
                    type = next;
                    if (next == 2 || type == 1) {
                        if (type == 2) {
                            closeStream(str);
                            return false;
                        }
                        next = parser.getDepth();
                        while (true) {
                            int next2 = parser.next();
                            type = next2;
                            if (next2 == 1 || (type == 3 && parser.getDepth() <= next)) {
                                closeStream(str);
                            } else if (type != 3) {
                                if (type != 4) {
                                    if (parser.getName().equals("sigs")) {
                                        readCerts(parser);
                                    } else {
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                }
                            }
                        }
                        closeStream(str);
                        return true;
                    }
                }
                if (type == 2) {
                }
            } catch (XmlPullParserException e) {
                Slog.e(TAG, "getTrustedCerts error duing to XmlPullParserException");
            } catch (IOException e2) {
                Slog.e(TAG, "getTrustedCerts error duing to IOException");
            } catch (Throwable th) {
                closeStream(str);
            }
        } else {
            Slog.d(TAG, "system cert file not found");
            return false;
        }
        closeStream(str);
        return false;
    }

    private void closeStream(FileInputStream str) {
        if (str != null) {
            try {
                str.close();
            } catch (IOException e) {
                Slog.e(TAG, "close FileInputStream failed");
            }
        }
    }

    private void readCerts(XmlPullParser parser) throws XmlPullParserException, IOException {
        String certVersion = parser.getAttributeValue(null, "name");
        if (certVersion.equals(SIGNATURE_VERSION_ONE) || certVersion.equals(SIGNATURE_VERSION_TWO)) {
            int outerDepth = parser.getDepth();
            while (true) {
                int next = parser.next();
                int type = next;
                if (next != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                    if (type != 3) {
                        if (type != 4) {
                            if (parser.getName().equals("cert")) {
                                String signType = parser.getAttributeValue(null, "name");
                                String sign = parser.getAttributeValue(null, "key");
                                if (certVersion.equals(SIGNATURE_VERSION_ONE)) {
                                    this.mOldSigns.put(signType, new Signature[]{new Signature(sign)});
                                } else if (certVersion.equals(SIGNATURE_VERSION_TWO)) {
                                    this.mNewSigns.put(signType, new Signature[]{new Signature(sign)});
                                }
                            } else {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown element under <sigs>: ");
                                stringBuilder.append(parser.getName());
                                Slog.w(str, stringBuilder.toString());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0037 A:{SYNTHETIC, Splitter: B:17:0x0037} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x002a A:{SYNTHETIC, Splitter: B:12:0x002a} */
    /* JADX WARNING: Missing block: B:35:?, code:
            r0.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readWhiteList() {
        if (this.mFoundWhiteListFile) {
            FileInputStream str = null;
            try {
                int type;
                str = new FileInputStream(this.mWhiteListFilename);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(str, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = parser.next();
                    type = next;
                    if (next == 2 || type == 1) {
                        if (type == 2) {
                            try {
                                str.close();
                            } catch (IOException e) {
                                Slog.e(TAG, "close FileInputStream failed");
                            }
                            return;
                        }
                        next = parser.getDepth();
                        while (true) {
                            int next2 = parser.next();
                            type = next2;
                            if (next2 == 1 || (type == 3 && parser.getDepth() <= next)) {
                                try {
                                    break;
                                } catch (IOException e2) {
                                    Slog.e(TAG, "close FileInputStream failed");
                                }
                            } else if (type != 3) {
                                if (type != 4) {
                                    if (parser.getName().equals("package")) {
                                        readPackageLPw(parser);
                                    } else {
                                        String str2 = TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Unknown element under <packages>: ");
                                        stringBuilder.append(parser.getName());
                                        Slog.w(str2, stringBuilder.toString());
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                }
                            }
                        }
                        return;
                    }
                }
                if (type == 2) {
                }
            } catch (XmlPullParserException e3) {
                Slog.e(TAG, "readWhiteList error duing to XmlPullParserException");
                if (str != null) {
                    str.close();
                }
            } catch (IOException e4) {
                Slog.e(TAG, "readWhiteList error duing to IOException");
                if (str != null) {
                    str.close();
                }
            } catch (Throwable th) {
                if (str != null) {
                    try {
                        str.close();
                    } catch (IOException e5) {
                        Slog.e(TAG, "close FileInputStream failed");
                    }
                }
            }
        }
    }

    private void readVersionsLPw(XmlPullParser parser, WhiteListPackage info) throws XmlPullParserException, IOException, IllegalArgumentException {
        if (info != null && parser != null) {
            int outerDepth = parser.getDepth();
            while (true) {
                int next = parser.next();
                int type = next;
                if (next != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                    if (type != 3) {
                        if (type != 4) {
                            if (parser.getName().equals(TAG_ITEM)) {
                                info.hashList.add(decodeHash(parser.getAttributeValue(null, ATTR_HASH)));
                            } else {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown element under <versions>: ");
                                stringBuilder.append(parser.getName());
                                Slog.w(str, stringBuilder.toString());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:60:0x010f A:{SYNTHETIC, Splitter: B:60:0x010f} */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x00c5 A:{SYNTHETIC, Splitter: B:43:0x00c5} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x00b6  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean readCertCompatPackages() {
        int type;
        FileInputStream str = null;
        if (this.mBackupCompatiblePackageFilename.exists()) {
            try {
                str = new FileInputStream(this.mBackupCompatiblePackageFilename);
                if (this.mCompatiblePackageFilename.exists()) {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Cleaning up whitelist file ");
                    stringBuilder.append(this.mWhiteListFilename);
                    Slog.w(str2, stringBuilder.toString());
                    if (!this.mCompatiblePackageFilename.delete()) {
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to clean up CompatPackage back up file: ");
                        stringBuilder.append(this.mBackupCompatiblePackageFilename);
                        Slog.wtf(str2, stringBuilder.toString());
                    }
                }
            } catch (IOException e) {
                Slog.e(TAG, "init FileInputStream failed due to IOException");
            } catch (SecurityException e2) {
                Slog.e(TAG, "delete CompatiblePackage File failed due to SecurityException");
            }
        }
        if (str == null) {
            try {
                if (this.mCompatiblePackageFilename.exists()) {
                    str = new FileInputStream(this.mCompatiblePackageFilename);
                } else {
                    PackageManagerService.reportSettingsProblem(4, "No whitelist file; creating initial state");
                    if (str != null) {
                        try {
                            str.close();
                        } catch (IOException e3) {
                            Slog.e(TAG, "close the FileInputStream failed");
                        }
                    }
                    return false;
                }
            } catch (XmlPullParserException e4) {
                Slog.e(TAG, "readCompatPackages error duing to XmlPullParserException");
                if (str != null) {
                    str.close();
                }
                return false;
            } catch (IOException e5) {
                Slog.e(TAG, "readCompatPackages error duing to IOException");
                if (str != null) {
                    str.close();
                }
                return false;
            } catch (IllegalArgumentException e6) {
                Slog.e(TAG, "readCompatPackages error duing to IllegalArgumentException");
                if (str != null) {
                    try {
                        str.close();
                    } catch (IOException e7) {
                        Slog.e(TAG, "close the FileInputStream failed");
                    }
                }
                return false;
            } catch (Throwable th) {
                if (str != null) {
                    try {
                        str.close();
                    } catch (IOException e8) {
                        Slog.e(TAG, "close the FileInputStream failed");
                    }
                }
            }
        }
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(str, StandardCharsets.UTF_8.name());
        while (true) {
            int next = parser.next();
            type = next;
            if (next == 2 || type == 1) {
                if (type == 2) {
                    if (str != null) {
                        try {
                            str.close();
                        } catch (IOException e9) {
                            Slog.e(TAG, "close the FileInputStream failed");
                        }
                    }
                    return false;
                }
                next = parser.getDepth();
                while (true) {
                    int next2 = parser.next();
                    type = next2;
                    if (next2 == 1 || (type == 3 && parser.getDepth() <= next)) {
                        if (str != null) {
                            try {
                                str.close();
                            } catch (IOException e10) {
                                Slog.e(TAG, "close the FileInputStream failed");
                            }
                        }
                    } else if (type != 3) {
                        if (type != 4) {
                            if (parser.getName().equals("package")) {
                                readCompatPackage(parser);
                            } else {
                                String str3 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unknown element under <packages>: ");
                                stringBuilder2.append(parser.getName());
                                Slog.w(str3, stringBuilder2.toString());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                }
                if (str != null) {
                }
                return true;
            }
        }
        if (type == 2) {
        }
    }

    void readCompatPackage(XmlPullParser parser) throws XmlPullParserException, IOException {
        String packName = parser.getAttributeValue(null, "name");
        String codePath = parser.getAttributeValue(null, ATTR_CODEPATH);
        String timeStamp = parser.getAttributeValue(null, ATTR_FT);
        String certType = parser.getAttributeValue(null, "cert");
        if (TextUtils.isEmpty(packName) || TextUtils.isEmpty(codePath) || TextUtils.isEmpty(timeStamp) || TextUtils.isEmpty(certType)) {
            Slog.d(TAG, "invalid compat package, skip it");
            return;
        }
        this.mPackages.put(packName, new Package(packName, codePath, Long.parseLong(timeStamp), certType));
    }

    private void readPackageLPw(XmlPullParser parser) throws XmlPullParserException, IOException, IllegalArgumentException {
        String packageName = parser.getAttributeValue(null, "name");
        if (!TextUtils.isEmpty(packageName)) {
            WhiteListPackage info;
            if (this.mWhiteList.containsKey(packageName)) {
                info = (WhiteListPackage) this.mWhiteList.get(packageName);
            } else {
                info = new WhiteListPackage(packageName);
            }
            if (info != null) {
                int outerDepth = parser.getDepth();
                while (true) {
                    int next = parser.next();
                    int type = next;
                    if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    } else if (type != 3) {
                        if (type != 4) {
                            if (parser.getName().equals(TAG_VERSIONS)) {
                                readVersionsLPw(parser, info);
                            } else {
                                XmlUtils.skipCurrentTag(parser);
                            }
                            this.mWhiteList.put(info.packageName, info);
                        }
                    }
                }
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
    }

    void insertCompatPackage(String packageName, PackageSetting ps) {
        if (packageName != null && ps != null) {
            String type = getNewSignTpye(ps.signatures.mSigningDetails.signatures);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insertCompatPackage:");
            stringBuilder.append(packageName);
            stringBuilder.append(" and the codePath is ");
            stringBuilder.append(ps.codePathString);
            Slog.d(str, stringBuilder.toString());
            this.mPackages.put(packageName, new Package(packageName, ps.codePathString, ps.timeStamp, type));
        }
    }

    void writeCertCompatPackages() {
        if (this.mCompatiblePackageFilename.exists()) {
            if (this.mBackupCompatiblePackageFilename.exists()) {
                if (this.mCompatiblePackageFilename.delete()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to clean up CompatPackage file: ");
                    stringBuilder.append(this.mCompatiblePackageFilename);
                    Slog.wtf(str, stringBuilder.toString());
                }
            } else if (!this.mCompatiblePackageFilename.renameTo(this.mBackupCompatiblePackageFilename)) {
                return;
            }
        }
        FileOutputStream fstr = null;
        BufferedOutputStream str2 = null;
        String str3;
        try {
            fstr = new FileOutputStream(this.mCompatiblePackageFilename);
            str2 = new BufferedOutputStream(fstr);
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(str2, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, HwSecDiagnoseConstant.MALAPP_APK_PACKAGES);
            for (Package info : this.mPackages.values()) {
                serializer.startTag(null, "package");
                XmlUtils.writeStringAttribute(serializer, "name", info.packageName);
                XmlUtils.writeStringAttribute(serializer, ATTR_CODEPATH, info.codePath);
                XmlUtils.writeStringAttribute(serializer, ATTR_FT, String.valueOf(info.timeStamp));
                XmlUtils.writeStringAttribute(serializer, "cert", info.certType);
                serializer.endTag(null, "package");
            }
            serializer.endTag(null, HwSecDiagnoseConstant.MALAPP_APK_PACKAGES);
            serializer.endDocument();
            str2.flush();
            FileUtils.sync(fstr);
            if (this.mBackupCompatiblePackageFilename.exists() && !this.mBackupCompatiblePackageFilename.delete()) {
                str3 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to clean up CompatPackage back up file: ");
                stringBuilder2.append(this.mBackupCompatiblePackageFilename);
                Slog.wtf(str3, stringBuilder2.toString());
            }
            FileUtils.setPermissions(this.mCompatiblePackageFilename.toString(), 432, -1, -1);
        } catch (IOException e) {
            Slog.wtf(TAG, "Unable to write CompatPackage, current changes will be lost at reboot", e);
            if (this.mCompatiblePackageFilename.exists() && !this.mCompatiblePackageFilename.delete()) {
                str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Failed to clean up CompatPackage file: ");
                stringBuilder3.append(this.mCompatiblePackageFilename);
                Slog.wtf(str3, stringBuilder3.toString());
            }
        } finally {
            IoUtils.closeQuietly(fstr);
        }
    }

    boolean isSystemSignatureUpdated(Signature[] oldSignature, Signature[] newSignature) {
        if (!this.mFoundSystemCertFile) {
            return false;
        }
        String oldSign = getOldSignTpye(oldSignature);
        String newSign = getNewSignTpye(newSignature);
        if (oldSign == null || newSign == null) {
            return false;
        }
        return oldSign.equals(newSign);
    }

    void removeCertCompatPackage(String name) {
        if (this.mPackages.containsKey(name)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeCertCompatPackage");
            stringBuilder.append(name);
            Slog.d(str, stringBuilder.toString());
            this.mPackages.remove(name);
        }
    }

    Package getCompatPackage(String name) {
        return (Package) this.mPackages.get(name);
    }

    Collection<Package> getALLCompatPackages() {
        return this.mPackages.values();
    }

    boolean isOldSystemSignature(Signature[] signs) {
        if (!this.mFoundSystemCertFile) {
            return false;
        }
        for (Signature[] s : this.mOldSigns.values()) {
            if (PackageManagerServiceUtils.compareSignatures(s, signs) == 0) {
                return true;
            }
        }
        return false;
    }

    boolean isWhiteListedApp(android.content.pm.PackageParser.Package pkg) {
        if (this.mCompatAll) {
            return true;
        }
        if (!this.mFoundWhiteListFile) {
            return false;
        }
        if (this.mWhiteList.size() == 0) {
            readWhiteList();
        }
        File file = new File(pkg.baseCodePath);
        String pkgName = pkg.packageName;
        if (!this.mWhiteList.containsKey(pkgName)) {
            return false;
        }
        List<byte[]> storedHash = ((WhiteListPackage) this.mWhiteList.get(pkgName)).hashList;
        byte[] computedHash = getSHA256(file);
        for (byte[] b : storedHash) {
            if (MessageDigest.isEqual(b, computedHash)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("found whitelist package:");
                stringBuilder.append(pkgName);
                Slog.i(str, stringBuilder.toString());
                return true;
            }
        }
        return false;
    }

    String getOldSignTpye(Signature[] signs) {
        if (!this.mFoundSystemCertFile) {
            return null;
        }
        for (Entry<String, Signature[]> entry : this.mOldSigns.entrySet()) {
            if (PackageManagerServiceUtils.compareSignatures(signs, (Signature[]) entry.getValue()) == 0) {
                return (String) entry.getKey();
            }
        }
        return null;
    }

    String getNewSignTpye(Signature[] signs) {
        if (!this.mFoundSystemCertFile) {
            return null;
        }
        for (Entry<String, Signature[]> entry : this.mNewSigns.entrySet()) {
            if (PackageManagerServiceUtils.compareSignatures(signs, (Signature[]) entry.getValue()) == 0) {
                return (String) entry.getKey();
            }
        }
        return null;
    }

    Signature[] getOldSign(String type) {
        if (this.mFoundSystemCertFile) {
            return (Signature[]) this.mOldSigns.get(type);
        }
        return new Signature[0];
    }

    Signature[] getNewSign(String type) {
        if (this.mFoundSystemCertFile) {
            return (Signature[]) this.mNewSigns.get(type);
        }
        return new Signature[0];
    }

    private byte[] getSHA256(File file) {
        byte[] manifest = getManifestFile(file);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(manifest);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            Slog.e(TAG, "get sha256 failed");
            return new byte[0];
        }
    }

    private byte[] getManifestFile(File apkFile) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        ZipFile zipFile = null;
        int length = 0;
        try {
            zipFile = new ZipFile(apkFile);
            ZipEntry ze = zipFile.getEntry("META-INF/MANIFEST.MF");
            if (ze != null) {
                try {
                    InputStream zipInputStream = zipFile.getInputStream(ze);
                    if (zipInputStream != null) {
                        while (true) {
                            int read = zipInputStream.read(b);
                            length = read;
                            if (read <= 0) {
                                break;
                            }
                            os.write(b, 0, length);
                        }
                        byte[] toByteArray = os.toByteArray();
                        IoUtils.closeQuietly(zipInputStream);
                        try {
                            zipFile.close();
                        } catch (IOException e) {
                        }
                        return toByteArray;
                    }
                    IoUtils.closeQuietly(zipInputStream);
                } catch (IOException e2) {
                    Slog.e(TAG, "get manifest file failed due to IOException");
                    IoUtils.closeQuietly(null);
                }
            }
            try {
                zipFile.close();
            } catch (IOException e3) {
            }
        } catch (IOException e4) {
            try {
                Slog.e(TAG, " get manifest file failed due to IOException");
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (Throwable th) {
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException e5) {
                    }
                }
            }
        } catch (Throwable th2) {
            IoUtils.closeQuietly(null);
        }
        return new byte[0];
    }

    private byte[] decodeHash(String hash) throws IllegalArgumentException {
        byte[] input = hash.getBytes(StandardCharsets.UTF_8);
        int N = input.length;
        if (N % 2 == 0) {
            byte[] sig = new byte[(N / 2)];
            int sigIndex = 0;
            int i = 0;
            while (i < N) {
                int i2 = i + 1;
                int i3 = i2 + 1;
                int sigIndex2 = sigIndex + 1;
                sig[sigIndex] = (byte) ((parseHexDigit(input[i]) << 4) | parseHexDigit(input[i2]));
                i = i3;
                sigIndex = sigIndex2;
            }
            return sig;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("text size ");
        stringBuilder.append(N);
        stringBuilder.append(" is not even");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private int parseHexDigit(int nibble) throws IllegalArgumentException {
        if (48 <= nibble && nibble <= 57) {
            return nibble - 48;
        }
        if (97 <= nibble && nibble <= 102) {
            return (nibble - 97) + 10;
        }
        if (65 <= nibble && nibble <= 70) {
            return (nibble - 65) + 10;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid character ");
        stringBuilder.append(nibble);
        stringBuilder.append(" in hex string");
        throw new IllegalArgumentException(stringBuilder.toString());
    }
}
