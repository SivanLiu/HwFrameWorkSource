package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import android.os.Environment;
import com.android.server.pm.Policy.PolicyBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class SELinuxMMAC {
    private static final boolean DEBUG_POLICY = false;
    private static final boolean DEBUG_POLICY_INSTALL = false;
    private static final boolean DEBUG_POLICY_ORDER = false;
    private static final String DEFAULT_SEINFO = "default";
    private static final String PRIVILEGED_APP_STR = ":privapp";
    private static final String SANDBOX_V2_STR = ":v2";
    static final String TAG = "SELinuxMMAC";
    private static final String TARGETSDKVERSION_STR = ":targetSdkVersion=";
    private static List<File> sMacPermissions = new ArrayList();
    private static List<Policy> sPolicies = new ArrayList();
    private static boolean sPolicyRead;

    static {
        sMacPermissions.add(new File(Environment.getRootDirectory(), "/etc/selinux/plat_mac_permissions.xml"));
        File vendorMacPermission = new File(Environment.getVendorDirectory(), "/etc/selinux/vendor_mac_permissions.xml");
        if (vendorMacPermission.exists()) {
            sMacPermissions.add(vendorMacPermission);
        } else {
            sMacPermissions.add(new File(Environment.getVendorDirectory(), "/etc/selinux/nonplat_mac_permissions.xml"));
        }
        File odmMacPermission = new File(Environment.getOdmDirectory(), "/etc/selinux/odm_mac_permissions.xml");
        if (odmMacPermission.exists()) {
            sMacPermissions.add(odmMacPermission);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:34:0x00ad A:{Splitter: B:11:0x0029, ExcHandler: java.lang.IllegalStateException (r2_3 'ex' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00ad A:{Splitter: B:11:0x0029, ExcHandler: java.lang.IllegalStateException (r2_3 'ex' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:8:0x000b, code:
            r1 = new java.util.ArrayList();
            r3 = android.util.Xml.newPullParser();
            r4 = sMacPermissions.size();
            r6 = null;
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:9:0x001f, code:
            if (r0 >= r4) goto L_0x00de;
     */
    /* JADX WARNING: Missing block: B:10:0x0021, code:
            r7 = (java.io.File) sMacPermissions.get(r0);
     */
    /* JADX WARNING: Missing block: B:12:?, code:
            r6 = new java.io.FileReader(r7);
            r8 = TAG;
            r9 = new java.lang.StringBuilder();
            r9.append("Using policy file ");
            r9.append(r7);
            android.util.Slog.d(r8, r9.toString());
            r3.setInput(r6);
            r3.nextTag();
            r3.require(2, null, "policy");
     */
    /* JADX WARNING: Missing block: B:14:0x0058, code:
            if (r3.next() == 3) goto L_0x0088;
     */
    /* JADX WARNING: Missing block: B:16:0x005e, code:
            if (r3.getEventType() == 2) goto L_0x0061;
     */
    /* JADX WARNING: Missing block: B:18:0x0061, code:
            r8 = r3.getName();
            r9 = true;
     */
    /* JADX WARNING: Missing block: B:19:0x006d, code:
            if (r8.hashCode() == -902467798) goto L_0x0070;
     */
    /* JADX WARNING: Missing block: B:21:0x0077, code:
            if (r8.equals("signer") == false) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:22:0x0079, code:
            r9 = false;
     */
    /* JADX WARNING: Missing block: B:23:0x007a, code:
            if (r9 == false) goto L_0x0080;
     */
    /* JADX WARNING: Missing block: B:24:0x007c, code:
            skip(r3);
     */
    /* JADX WARNING: Missing block: B:25:0x0080, code:
            r1.add(readSignerOrThrow(r3));
     */
    /* JADX WARNING: Missing block: B:27:0x0088, code:
            libcore.io.IoUtils.closeQuietly(r6);
            r0 = r0 + 1;
     */
    /* JADX WARNING: Missing block: B:28:0x008f, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:29:0x0091, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:31:?, code:
            r8 = TAG;
            r9 = new java.lang.StringBuilder();
            r9.append("Exception parsing ");
            r9.append(r7);
            android.util.Slog.w(r8, r9.toString(), r2);
     */
    /* JADX WARNING: Missing block: B:32:0x00a8, code:
            libcore.io.IoUtils.closeQuietly(r6);
     */
    /* JADX WARNING: Missing block: B:33:0x00ac, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:34:0x00ad, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:36:?, code:
            r8 = new java.lang.StringBuilder("Exception @");
            r8.append(r3.getPositionDescription());
            r8.append(" while parsing ");
            r8.append(r7);
            r8.append(":");
            r8.append(r2);
            android.util.Slog.w(TAG, r8.toString());
     */
    /* JADX WARNING: Missing block: B:37:0x00d5, code:
            libcore.io.IoUtils.closeQuietly(r6);
     */
    /* JADX WARNING: Missing block: B:38:0x00d9, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:39:0x00da, code:
            libcore.io.IoUtils.closeQuietly(r6);
     */
    /* JADX WARNING: Missing block: B:40:0x00dd, code:
            throw r2;
     */
    /* JADX WARNING: Missing block: B:41:0x00de, code:
            r7 = new com.android.server.pm.PolicyComparator();
            java.util.Collections.sort(r1, r7);
     */
    /* JADX WARNING: Missing block: B:42:0x00eb, code:
            if (r7.foundDuplicate() == false) goto L_0x00f5;
     */
    /* JADX WARNING: Missing block: B:43:0x00ed, code:
            android.util.Slog.w(TAG, "ERROR! Duplicate entries found parsing mac_permissions.xml files");
     */
    /* JADX WARNING: Missing block: B:44:0x00f4, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:45:0x00f5, code:
            r5 = sPolicies;
     */
    /* JADX WARNING: Missing block: B:46:0x00f7, code:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:48:?, code:
            sPolicies.clear();
            sPolicies.addAll(r1);
            sPolicyRead = true;
     */
    /* JADX WARNING: Missing block: B:49:0x0104, code:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:50:0x0105, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:51:0x0106, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:53:0x0108, code:
            throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean readInstallPolicy() {
        synchronized (sPolicies) {
            try {
                if (sPolicyRead) {
                    return true;
                }
            } catch (Throwable th) {
                while (true) {
                    throw th;
                }
            }
        }
    }

    private static Policy readSignerOrThrow(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, null, "signer");
        PolicyBuilder pb = new PolicyBuilder();
        String cert = parser.getAttributeValue(null, "signature");
        if (cert != null) {
            pb.addSignature(cert);
        }
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if ("seinfo".equals(tagName)) {
                    pb.setGlobalSeinfoOrThrow(parser.getAttributeValue(null, "value"));
                    readSeinfo(parser);
                } else if ("package".equals(tagName)) {
                    readPackageOrThrow(parser, pb);
                } else if ("cert".equals(tagName)) {
                    pb.addSignature(parser.getAttributeValue(null, "signature"));
                    readCert(parser);
                } else {
                    skip(parser);
                }
            }
        }
        return pb.build();
    }

    private static void readPackageOrThrow(XmlPullParser parser, PolicyBuilder pb) throws IOException, XmlPullParserException {
        parser.require(2, null, "package");
        String pkgName = parser.getAttributeValue(null, Settings.ATTR_NAME);
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                if ("seinfo".equals(parser.getName())) {
                    pb.addInnerPackageMapOrThrow(pkgName, parser.getAttributeValue(null, "value"));
                    readSeinfo(parser);
                } else {
                    skip(parser);
                }
            }
        }
    }

    private static void readCert(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, null, "cert");
        parser.nextTag();
    }

    private static void readSeinfo(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, null, "seinfo");
        parser.nextTag();
    }

    private static void skip(XmlPullParser p) throws IOException, XmlPullParserException {
        if (p.getEventType() == 2) {
            int depth = 1;
            while (depth != 0) {
                switch (p.next()) {
                    case 2:
                        depth++;
                        break;
                    case 3:
                        depth--;
                        break;
                    default:
                        break;
                }
            }
            return;
        }
        throw new IllegalStateException();
    }

    public static String getSeInfo(Package pkg, boolean isPrivileged, int targetSandboxVersion, int targetSdkVersion) {
        StringBuilder stringBuilder;
        String seInfo = null;
        synchronized (sPolicies) {
            if (sPolicyRead) {
                for (Policy policy : sPolicies) {
                    seInfo = policy.getMatchedSeInfo(pkg);
                    if (seInfo != null) {
                        break;
                    }
                }
            }
        }
        if (seInfo == null) {
            seInfo = "default";
        }
        if (targetSandboxVersion == 2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(seInfo);
            stringBuilder.append(SANDBOX_V2_STR);
            seInfo = stringBuilder.toString();
        }
        if (isPrivileged) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(seInfo);
            stringBuilder.append(PRIVILEGED_APP_STR);
            seInfo = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(seInfo);
        stringBuilder.append(TARGETSDKVERSION_STR);
        stringBuilder.append(targetSdkVersion);
        return stringBuilder.toString();
    }
}
