package com.android.server.pm;

import android.content.pm.PackageParser.SigningDetails;
import android.content.pm.PackageParser.SigningDetails.Builder;
import android.content.pm.Signature;
import com.android.internal.util.XmlUtils;
import com.android.server.am.AssistDataRequester;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class PackageSignatures {
    SigningDetails mSigningDetails;

    PackageSignatures(PackageSignatures orig) {
        if (orig == null || orig.mSigningDetails == SigningDetails.UNKNOWN) {
            this.mSigningDetails = SigningDetails.UNKNOWN;
        } else {
            this.mSigningDetails = new SigningDetails(orig.mSigningDetails);
        }
    }

    PackageSignatures(SigningDetails signingDetails) {
        this.mSigningDetails = signingDetails;
    }

    PackageSignatures() {
        this.mSigningDetails = SigningDetails.UNKNOWN;
    }

    void writeXml(XmlSerializer serializer, String tagName, ArrayList<Signature> writtenSignatures) throws IOException {
        if (this.mSigningDetails.signatures != null) {
            serializer.startTag(null, tagName);
            serializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.toString(this.mSigningDetails.signatures.length));
            serializer.attribute(null, "schemeVersion", Integer.toString(this.mSigningDetails.signatureSchemeVersion));
            writeCertsListXml(serializer, writtenSignatures, this.mSigningDetails.signatures, null);
            if (this.mSigningDetails.pastSigningCertificates != null) {
                serializer.startTag(null, "pastSigs");
                serializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.toString(this.mSigningDetails.pastSigningCertificates.length));
                writeCertsListXml(serializer, writtenSignatures, this.mSigningDetails.pastSigningCertificates, this.mSigningDetails.pastSigningCertificatesFlags);
                serializer.endTag(null, "pastSigs");
            }
            serializer.endTag(null, tagName);
        }
    }

    private void writeCertsListXml(XmlSerializer serializer, ArrayList<Signature> writtenSignatures, Signature[] signatures, int[] flags) throws IOException {
        for (int i = 0; i < signatures.length; i++) {
            serializer.startTag(null, "cert");
            Signature sig = signatures[i];
            int sigHash = sig.hashCode();
            int numWritten = writtenSignatures.size();
            int j = 0;
            while (j < numWritten) {
                Signature writtenSig = (Signature) writtenSignatures.get(j);
                if (writtenSig.hashCode() == sigHash && writtenSig.equals(sig)) {
                    serializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX, Integer.toString(j));
                    break;
                }
                j++;
            }
            if (j >= numWritten) {
                writtenSignatures.add(sig);
                serializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX, Integer.toString(numWritten));
                serializer.attribute(null, "key", sig.toCharsString());
            }
            if (flags != null) {
                serializer.attribute(null, "flags", Integer.toString(flags[i]));
            }
            serializer.endTag(null, "cert");
        }
    }

    void readXml(XmlPullParser parser, ArrayList<Signature> readSignatures) throws IOException, XmlPullParserException {
        StringBuilder stringBuilder;
        int signatureSchemeVersion;
        Builder builder = new Builder();
        String countStr = parser.getAttributeValue(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT);
        if (countStr == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error in package manager settings: <sigs> has no count at ");
            stringBuilder.append(parser.getPositionDescription());
            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
            XmlUtils.skipCurrentTag(parser);
        }
        int count = Integer.parseInt(countStr);
        String schemeVersionStr = parser.getAttributeValue(null, "schemeVersion");
        if (schemeVersionStr == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error in package manager settings: <sigs> has no schemeVersion at ");
            stringBuilder.append(parser.getPositionDescription());
            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
            signatureSchemeVersion = 0;
        } else {
            signatureSchemeVersion = Integer.parseInt(schemeVersionStr);
        }
        builder.setSignatureSchemeVersion(signatureSchemeVersion);
        Signature[] signatures = new Signature[count];
        signatureSchemeVersion = readCertsListXml(parser, readSignatures, signatures, null, builder);
        builder.setSignatures(signatures);
        if (signatureSchemeVersion < count) {
            Signature[] newSigs = new Signature[signatureSchemeVersion];
            System.arraycopy(signatures, 0, newSigs, 0, signatureSchemeVersion);
            builder = builder.setSignatures(newSigs);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error in package manager settings: <sigs> count does not match number of  <cert> entries");
            stringBuilder2.append(parser.getPositionDescription());
            PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
        }
        try {
            this.mSigningDetails = builder.build();
        } catch (CertificateException e) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> unable to convert certificate(s) to public key(s).");
            this.mSigningDetails = SigningDetails.UNKNOWN;
        }
    }

    private int readCertsListXml(XmlPullParser parser, ArrayList<Signature> readSignatures, Signature[] signatures, int[] flags, Builder builder) throws IOException, XmlPullParserException {
        String countStr;
        ArrayList<Signature> arrayList;
        XmlPullParser xmlPullParser = parser;
        ArrayList arrayList2 = readSignatures;
        Signature[] signatureArr = signatures;
        int count = signatureArr.length;
        int outerDepth = parser.getDepth();
        Builder builder2 = builder;
        int pos = 0;
        while (true) {
            int outerDepth2 = outerDepth;
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth2)) {
                return pos;
            }
            if (!(type == 3 || type == 4)) {
                String tagName = parser.getName();
                StringBuilder stringBuilder;
                StringBuilder stringBuilder2;
                if (tagName.equals("cert")) {
                    if (pos < count) {
                        String index = xmlPullParser.getAttributeValue(null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX);
                        if (index != null) {
                            try {
                                next = Integer.parseInt(index);
                                String key = xmlPullParser.getAttributeValue(null, "key");
                                if (key != null) {
                                    while (readSignatures.size() <= next) {
                                        arrayList2.add(null);
                                    }
                                    Signature sig = new Signature(key);
                                    arrayList2.set(next, sig);
                                    signatureArr[pos] = sig;
                                } else if (next < 0 || next >= readSignatures.size()) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Error in package manager settings: <cert> index ");
                                    stringBuilder.append(index);
                                    stringBuilder.append(" is out of bounds at ");
                                    stringBuilder.append(parser.getPositionDescription());
                                    PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                                } else if (((Signature) arrayList2.get(next)) != null) {
                                    signatureArr[pos] = (Signature) arrayList2.get(next);
                                } else {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Error in package manager settings: <cert> index ");
                                    stringBuilder.append(index);
                                    stringBuilder.append(" is not defined at ");
                                    stringBuilder.append(parser.getPositionDescription());
                                    PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                                }
                            } catch (NumberFormatException e) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Error in package manager settings: <cert> index ");
                                stringBuilder.append(index);
                                stringBuilder.append(" is not a number at ");
                                stringBuilder.append(parser.getPositionDescription());
                                PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                            } catch (IllegalArgumentException e2) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Error in package manager settings: <cert> index ");
                                stringBuilder.append(index);
                                stringBuilder.append(" has an invalid signature at ");
                                stringBuilder.append(parser.getPositionDescription());
                                stringBuilder.append(": ");
                                stringBuilder.append(e2.getMessage());
                                PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                            }
                            if (flags != null) {
                                String flagsStr = xmlPullParser.getAttributeValue(null, "flags");
                                if (flagsStr != null) {
                                    try {
                                        flags[pos] = Integer.parseInt(flagsStr);
                                    } catch (NumberFormatException e3) {
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("Error in package manager settings: <cert> flags ");
                                        stringBuilder3.append(flagsStr);
                                        stringBuilder3.append(" is not a number at ");
                                        stringBuilder3.append(parser.getPositionDescription());
                                        PackageManagerService.reportSettingsProblem(5, stringBuilder3.toString());
                                    }
                                } else {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Error in package manager settings: <cert> has no flags at ");
                                    stringBuilder2.append(parser.getPositionDescription());
                                    PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                                }
                            }
                        } else {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Error in package manager settings: <cert> has no index at ");
                            stringBuilder2.append(parser.getPositionDescription());
                            PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                        }
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error in package manager settings: too many <cert> tags, expected ");
                        stringBuilder2.append(count);
                        stringBuilder2.append(" at ");
                        stringBuilder2.append(parser.getPositionDescription());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                    }
                    pos++;
                    XmlUtils.skipCurrentTag(parser);
                } else if (!tagName.equals("pastSigs")) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unknown element under <sigs>: ");
                    stringBuilder2.append(parser.getName());
                    PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                    XmlUtils.skipCurrentTag(parser);
                } else if (flags == null) {
                    int i;
                    String countStr2 = xmlPullParser.getAttributeValue(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT);
                    if (countStr2 == null) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error in package manager settings: <pastSigs> has no count at ");
                        stringBuilder2.append(parser.getPositionDescription());
                        i = 5;
                        PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        i = 5;
                    }
                    try {
                        next = Integer.parseInt(countStr2);
                        int[] pastSignaturesFlags = new int[next];
                        Signature[] pastSignatures = new Signature[next];
                        ArrayList arrayList3 = arrayList2;
                        countStr = countStr2;
                        int i2 = i;
                        try {
                            outerDepth = readCertsListXml(xmlPullParser, arrayList3, pastSignatures, pastSignaturesFlags, builder2);
                            Signature[] pastSignatures2 = pastSignatures;
                            int[] pastSignaturesFlags2 = pastSignaturesFlags;
                            builder2 = builder2.setPastSigningCertificates(pastSignatures2).setPastSigningCertificatesFlags(pastSignaturesFlags2);
                            if (outerDepth < next) {
                                Signature[] newSigs = new Signature[outerDepth];
                                System.arraycopy(pastSignatures2, 0, newSigs, 0, outerDepth);
                                tagName = new int[outerDepth];
                                System.arraycopy(pastSignaturesFlags2, 0, tagName, 0, outerDepth);
                                builder2 = builder2.setPastSigningCertificates(newSigs).setPastSigningCertificatesFlags(tagName);
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Error in package manager settings: <pastSigs> count does not match number of <cert> entries ");
                                stringBuilder4.append(parser.getPositionDescription());
                                PackageManagerService.reportSettingsProblem(5, stringBuilder4.toString());
                            }
                        } catch (NumberFormatException e4) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error in package manager settings: <pastSigs> count ");
                            stringBuilder.append(countStr);
                            stringBuilder.append(" is not a number at ");
                            stringBuilder.append(parser.getPositionDescription());
                            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                            outerDepth = outerDepth2;
                            arrayList2 = readSignatures;
                            signatureArr = signatures;
                        }
                    } catch (NumberFormatException e5) {
                        countStr = countStr2;
                        String str = tagName;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error in package manager settings: <pastSigs> count ");
                        stringBuilder.append(countStr);
                        stringBuilder.append(" is not a number at ");
                        stringBuilder.append(parser.getPositionDescription());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                        outerDepth = outerDepth2;
                        arrayList2 = readSignatures;
                        signatureArr = signatures;
                    }
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("<pastSigs> encountered multiple times under the same <sigs> at ");
                    stringBuilder2.append(parser.getPositionDescription());
                    PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
            outerDepth = outerDepth2;
            arrayList2 = readSignatures;
            signatureArr = signatures;
        }
        return pos;
    }

    public String toString() {
        int i;
        StringBuffer buf = new StringBuffer(128);
        buf.append("PackageSignatures{");
        buf.append(Integer.toHexString(System.identityHashCode(this)));
        buf.append(" version:");
        buf.append(this.mSigningDetails.signatureSchemeVersion);
        buf.append(", signatures:[");
        int i2 = 0;
        if (this.mSigningDetails.signatures != null) {
            for (i = 0; i < this.mSigningDetails.signatures.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(Integer.toHexString(this.mSigningDetails.signatures[i].hashCode()));
            }
        }
        buf.append("]");
        buf.append(", past signatures:[");
        if (this.mSigningDetails.pastSigningCertificates != null) {
            while (true) {
                i = i2;
                if (i >= this.mSigningDetails.pastSigningCertificates.length) {
                    break;
                }
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(Integer.toHexString(this.mSigningDetails.pastSigningCertificates[i].hashCode()));
                buf.append(" flags: ");
                buf.append(Integer.toHexString(this.mSigningDetails.pastSigningCertificatesFlags[i]));
                i2 = i + 1;
            }
        }
        buf.append("]}");
        return buf.toString();
    }
}
