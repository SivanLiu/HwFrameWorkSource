package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser.Package;
import android.os.Environment;
import android.util.Slog;
import android.util.Xml;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.pm.Policy.PolicyBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class SELinuxMMAC {
    private static final boolean DEBUG_POLICY = false;
    private static final boolean DEBUG_POLICY_INSTALL = false;
    private static final boolean DEBUG_POLICY_ORDER = false;
    private static final File[] MAC_PERMISSIONS = new File[]{new File(Environment.getRootDirectory(), "/etc/selinux/plat_mac_permissions.xml"), new File(Environment.getVendorDirectory(), "/etc/selinux/nonplat_mac_permissions.xml")};
    private static final String PRIVILEGED_APP_STR = ":privapp";
    private static final String SANDBOX_V2_STR = ":v2";
    static final String TAG = "SELinuxMMAC";
    private static final String TARGETSDKVERSION_STR = ":targetSdkVersion=";
    private static List<Policy> sPolicies = new ArrayList();

    public static boolean readInstallPolicy() {
        Exception ex;
        IOException ioe;
        List<Policy> policies = new ArrayList();
        AutoCloseable policyFile = null;
        XmlPullParser parser = Xml.newPullParser();
        int i = 0;
        while (i < MAC_PERMISSIONS.length) {
            try {
                FileReader policyFile2 = new FileReader(MAC_PERMISSIONS[i]);
                Object policyFile3;
                try {
                    Slog.d(TAG, "Using policy file " + MAC_PERMISSIONS[i]);
                    parser.setInput(policyFile2);
                    parser.nextTag();
                    parser.require(2, null, "policy");
                    while (parser.next() != 3) {
                        if (parser.getEventType() == 2) {
                            if (parser.getName().equals("signer")) {
                                policies.add(readSignerOrThrow(parser));
                            } else {
                                skip(parser);
                            }
                        }
                    }
                    IoUtils.closeQuietly(policyFile2);
                    i++;
                    policyFile3 = policyFile2;
                } catch (IllegalStateException e) {
                    ex = e;
                    policyFile = policyFile2;
                } catch (IOException e2) {
                    ioe = e2;
                    policyFile = policyFile2;
                } catch (Throwable th) {
                    Throwable th2 = th;
                    policyFile3 = policyFile2;
                }
            } catch (IllegalStateException e3) {
                ex = e3;
            } catch (IOException e4) {
                ioe = e4;
            }
        }
        PolicyComparator policySort = new PolicyComparator();
        Collections.sort(policies, policySort);
        if (policySort.foundDuplicate()) {
            Slog.w(TAG, "ERROR! Duplicate entries found parsing mac_permissions.xml files");
            return false;
        }
        synchronized (sPolicies) {
            sPolicies = policies;
        }
        return true;
        Slog.w(TAG, "Exception parsing " + MAC_PERMISSIONS[i], ioe);
        IoUtils.closeQuietly(policyFile);
        return false;
        try {
            StringBuilder sb = new StringBuilder("Exception @");
            sb.append(parser.getPositionDescription());
            sb.append(" while parsing ");
            sb.append(MAC_PERMISSIONS[i]);
            sb.append(":");
            sb.append(ex);
            Slog.w(TAG, sb.toString());
            IoUtils.closeQuietly(policyFile);
            return false;
        } catch (Throwable th3) {
            th2 = th3;
            IoUtils.closeQuietly(policyFile);
            throw th2;
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
                } else if (HwBroadcastRadarUtil.KEY_PACKAGE.equals(tagName)) {
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
        parser.require(2, null, HwBroadcastRadarUtil.KEY_PACKAGE);
        String pkgName = parser.getAttributeValue(null, "name");
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
        if (p.getEventType() != 2) {
            throw new IllegalStateException();
        }
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
    }

    public static void assignSeInfoValue(Package pkg) {
        ApplicationInfo applicationInfo;
        synchronized (sPolicies) {
            for (Policy policy : sPolicies) {
                String seInfo = policy.getMatchedSeInfo(pkg);
                if (seInfo != null) {
                    pkg.applicationInfo.seInfo = seInfo;
                    break;
                }
            }
        }
        if (pkg.applicationInfo.targetSandboxVersion == 2) {
            applicationInfo = pkg.applicationInfo;
            applicationInfo.seInfo += SANDBOX_V2_STR;
        }
        if (pkg.applicationInfo.isPrivilegedApp()) {
            applicationInfo = pkg.applicationInfo;
            applicationInfo.seInfo += PRIVILEGED_APP_STR;
        }
        applicationInfo = pkg.applicationInfo;
        applicationInfo.seInfo += TARGETSDKVERSION_STR + pkg.applicationInfo.targetSdkVersion;
    }
}
