package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.SigningDetails;
import android.content.pm.Signature;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* compiled from: SELinuxMMAC */
final class Policy {
    private final Set<Signature> mCerts;
    private final Map<String, String> mPkgMap;
    private final String mSeinfo;

    /* compiled from: SELinuxMMAC */
    public static final class PolicyBuilder {
        private final Set<Signature> mCerts = new HashSet(2);
        private final Map<String, String> mPkgMap = new HashMap(2);
        private String mSeinfo;

        public PolicyBuilder addSignature(String cert) {
            if (cert != null) {
                this.mCerts.add(new Signature(cert));
                return this;
            }
            String err = new StringBuilder();
            err.append("Invalid signature value ");
            err.append(cert);
            throw new IllegalArgumentException(err.toString());
        }

        public PolicyBuilder setGlobalSeinfoOrThrow(String seinfo) {
            if (!validateValue(seinfo)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid seinfo value ");
                stringBuilder.append(seinfo);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (this.mSeinfo == null || this.mSeinfo.equals(seinfo)) {
                this.mSeinfo = seinfo;
                return this;
            } else {
                throw new IllegalStateException("Duplicate seinfo tag found");
            }
        }

        public PolicyBuilder addInnerPackageMapOrThrow(String pkgName, String seinfo) {
            String pkgValue;
            if (!validateValue(pkgName)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid package name ");
                stringBuilder.append(pkgName);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (validateValue(seinfo)) {
                pkgValue = (String) this.mPkgMap.get(pkgName);
                if (pkgValue == null || pkgValue.equals(seinfo)) {
                    this.mPkgMap.put(pkgName, seinfo);
                    return this;
                }
                throw new IllegalStateException("Conflicting seinfo value found");
            } else {
                pkgValue = new StringBuilder();
                pkgValue.append("Invalid seinfo value ");
                pkgValue.append(seinfo);
                throw new IllegalArgumentException(pkgValue.toString());
            }
        }

        private boolean validateValue(String name) {
            if (name != null && name.matches("\\A[\\.\\w]+\\z")) {
                return true;
            }
            return false;
        }

        public Policy build() {
            Policy p = new Policy(this);
            if (p.mCerts.isEmpty()) {
                throw new IllegalStateException("Missing certs with signer tag. Expecting at least one.");
            }
            if (((p.mSeinfo == null ? 1 : 0) ^ p.mPkgMap.isEmpty()) != 0) {
                return p;
            }
            throw new IllegalStateException("Only seinfo tag XOR package tags are allowed within a signer stanza.");
        }
    }

    private Policy(PolicyBuilder builder) {
        this.mSeinfo = builder.mSeinfo;
        this.mCerts = Collections.unmodifiableSet(builder.mCerts);
        this.mPkgMap = Collections.unmodifiableMap(builder.mPkgMap);
    }

    public Set<Signature> getSignatures() {
        return this.mCerts;
    }

    public boolean hasInnerPackages() {
        return this.mPkgMap.isEmpty() ^ 1;
    }

    public Map<String, String> getInnerPackages() {
        return this.mPkgMap;
    }

    public boolean hasGlobalSeinfo() {
        return this.mSeinfo != null;
    }

    public String toString() {
        StringBuilder stringBuilder;
        StringBuilder sb = new StringBuilder();
        for (Signature cert : this.mCerts) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("cert=");
            stringBuilder.append(cert.toCharsString().substring(0, 11));
            stringBuilder.append("... ");
            sb.append(stringBuilder.toString());
        }
        if (this.mSeinfo != null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("seinfo=");
            stringBuilder2.append(this.mSeinfo);
            sb.append(stringBuilder2.toString());
        }
        for (String name : this.mPkgMap.keySet()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" ");
            stringBuilder.append(name);
            stringBuilder.append("=");
            stringBuilder.append((String) this.mPkgMap.get(name));
            sb.append(stringBuilder.toString());
        }
        return sb.toString();
    }

    public String getMatchedSeInfo(Package pkg) {
        Signature[] certs = (Signature[]) this.mCerts.toArray(new Signature[0]);
        if (pkg.mSigningDetails != SigningDetails.UNKNOWN && !Signature.areExactMatch(certs, pkg.mSigningDetails.signatures) && (certs.length > 1 || !pkg.mSigningDetails.hasCertificate(certs[0]))) {
            return null;
        }
        String seinfoValue = (String) this.mPkgMap.get(pkg.packageName);
        if (seinfoValue != null) {
            return seinfoValue;
        }
        return this.mSeinfo;
    }
}
