package org.bouncycastle.asn1.isismtt.x509;

import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x500.DirectoryString;

public class ProfessionInfo extends ASN1Object {
    public static final ASN1ObjectIdentifier Notar;
    public static final ASN1ObjectIdentifier Notariatsverwalter;
    public static final ASN1ObjectIdentifier Notariatsverwalterin;
    public static final ASN1ObjectIdentifier Notarin;
    public static final ASN1ObjectIdentifier Notarvertreter;
    public static final ASN1ObjectIdentifier Notarvertreterin;
    public static final ASN1ObjectIdentifier Patentanwalt;
    public static final ASN1ObjectIdentifier Patentanwltin;
    public static final ASN1ObjectIdentifier Rechtsanwalt;
    public static final ASN1ObjectIdentifier Rechtsanwltin;
    public static final ASN1ObjectIdentifier Rechtsbeistand;
    public static final ASN1ObjectIdentifier Steuerberater;
    public static final ASN1ObjectIdentifier Steuerberaterin;
    public static final ASN1ObjectIdentifier Steuerbevollmchtigte;
    public static final ASN1ObjectIdentifier Steuerbevollmchtigter;
    public static final ASN1ObjectIdentifier VereidigteBuchprferin;
    public static final ASN1ObjectIdentifier VereidigterBuchprfer;
    public static final ASN1ObjectIdentifier Wirtschaftsprfer;
    public static final ASN1ObjectIdentifier Wirtschaftsprferin;
    private ASN1OctetString addProfessionInfo;
    private NamingAuthority namingAuthority;
    private ASN1Sequence professionItems;
    private ASN1Sequence professionOIDs;
    private String registrationNumber;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".1");
        Rechtsanwltin = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".2");
        Rechtsanwalt = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".3");
        Rechtsbeistand = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".4");
        Steuerberaterin = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".5");
        Steuerberater = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".6");
        Steuerbevollmchtigte = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".7");
        Steuerbevollmchtigter = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".8");
        Notarin = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".9");
        Notar = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".10");
        Notarvertreterin = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".11");
        Notarvertreter = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".12");
        Notariatsverwalterin = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".13");
        Notariatsverwalter = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".14");
        Wirtschaftsprferin = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".15");
        Wirtschaftsprfer = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".16");
        VereidigteBuchprferin = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".17");
        VereidigterBuchprfer = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".18");
        Patentanwltin = new ASN1ObjectIdentifier(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern);
        stringBuilder.append(".19");
        Patentanwalt = new ASN1ObjectIdentifier(stringBuilder.toString());
    }

    private ProfessionInfo(ASN1Sequence aSN1Sequence) {
        StringBuilder stringBuilder;
        if (aSN1Sequence.size() <= 5) {
            ASN1Encodable aSN1Encodable;
            Enumeration objects = aSN1Sequence.getObjects();
            Object obj = (ASN1Encodable) objects.nextElement();
            if (obj instanceof ASN1TaggedObject) {
                ASN1TaggedObject aSN1TaggedObject = (ASN1TaggedObject) obj;
                if (aSN1TaggedObject.getTagNo() == 0) {
                    this.namingAuthority = NamingAuthority.getInstance(aSN1TaggedObject, true);
                    obj = (ASN1Encodable) objects.nextElement();
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad tag number: ");
                    stringBuilder.append(aSN1TaggedObject.getTagNo());
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            this.professionItems = ASN1Sequence.getInstance(obj);
            if (objects.hasMoreElements()) {
                aSN1Encodable = (ASN1Encodable) objects.nextElement();
                if (aSN1Encodable instanceof ASN1Sequence) {
                    this.professionOIDs = ASN1Sequence.getInstance(aSN1Encodable);
                } else if (aSN1Encodable instanceof DERPrintableString) {
                    this.registrationNumber = DERPrintableString.getInstance(aSN1Encodable).getString();
                } else if (aSN1Encodable instanceof ASN1OctetString) {
                    this.addProfessionInfo = ASN1OctetString.getInstance(aSN1Encodable);
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad object encountered: ");
                    stringBuilder.append(aSN1Encodable.getClass());
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            if (objects.hasMoreElements()) {
                aSN1Encodable = (ASN1Encodable) objects.nextElement();
                if (aSN1Encodable instanceof DERPrintableString) {
                    this.registrationNumber = DERPrintableString.getInstance(aSN1Encodable).getString();
                } else if (aSN1Encodable instanceof DEROctetString) {
                    this.addProfessionInfo = (DEROctetString) aSN1Encodable;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad object encountered: ");
                    stringBuilder.append(aSN1Encodable.getClass());
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            if (objects.hasMoreElements()) {
                ASN1Encodable aSN1Encodable2 = (ASN1Encodable) objects.nextElement();
                if (aSN1Encodable2 instanceof DEROctetString) {
                    this.addProfessionInfo = (DEROctetString) aSN1Encodable2;
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Bad object encountered: ");
                stringBuilder.append(aSN1Encodable2.getClass());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Bad sequence size: ");
        stringBuilder.append(aSN1Sequence.size());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public ProfessionInfo(NamingAuthority namingAuthority, DirectoryString[] directoryStringArr, ASN1ObjectIdentifier[] aSN1ObjectIdentifierArr, String str, ASN1OctetString aSN1OctetString) {
        this.namingAuthority = namingAuthority;
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        int i = 0;
        for (int i2 = 0; i2 != directoryStringArr.length; i2++) {
            aSN1EncodableVector.add(directoryStringArr[i2]);
        }
        this.professionItems = new DERSequence(aSN1EncodableVector);
        if (aSN1ObjectIdentifierArr != null) {
            aSN1EncodableVector = new ASN1EncodableVector();
            while (i != aSN1ObjectIdentifierArr.length) {
                aSN1EncodableVector.add(aSN1ObjectIdentifierArr[i]);
                i++;
            }
            this.professionOIDs = new DERSequence(aSN1EncodableVector);
        }
        this.registrationNumber = str;
        this.addProfessionInfo = aSN1OctetString;
    }

    public static ProfessionInfo getInstance(Object obj) {
        if (obj == null || (obj instanceof ProfessionInfo)) {
            return (ProfessionInfo) obj;
        }
        if (obj instanceof ASN1Sequence) {
            return new ProfessionInfo((ASN1Sequence) obj);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("illegal object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public ASN1OctetString getAddProfessionInfo() {
        return this.addProfessionInfo;
    }

    public NamingAuthority getNamingAuthority() {
        return this.namingAuthority;
    }

    public DirectoryString[] getProfessionItems() {
        DirectoryString[] directoryStringArr = new DirectoryString[this.professionItems.size()];
        Enumeration objects = this.professionItems.getObjects();
        int i = 0;
        while (objects.hasMoreElements()) {
            int i2 = i + 1;
            directoryStringArr[i] = DirectoryString.getInstance(objects.nextElement());
            i = i2;
        }
        return directoryStringArr;
    }

    public ASN1ObjectIdentifier[] getProfessionOIDs() {
        int i = 0;
        if (this.professionOIDs == null) {
            return new ASN1ObjectIdentifier[0];
        }
        ASN1ObjectIdentifier[] aSN1ObjectIdentifierArr = new ASN1ObjectIdentifier[this.professionOIDs.size()];
        Enumeration objects = this.professionOIDs.getObjects();
        while (objects.hasMoreElements()) {
            int i2 = i + 1;
            aSN1ObjectIdentifierArr[i] = ASN1ObjectIdentifier.getInstance(objects.nextElement());
            i = i2;
        }
        return aSN1ObjectIdentifierArr;
    }

    public String getRegistrationNumber() {
        return this.registrationNumber;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.namingAuthority != null) {
            aSN1EncodableVector.add(new DERTaggedObject(true, 0, this.namingAuthority));
        }
        aSN1EncodableVector.add(this.professionItems);
        if (this.professionOIDs != null) {
            aSN1EncodableVector.add(this.professionOIDs);
        }
        if (this.registrationNumber != null) {
            aSN1EncodableVector.add(new DERPrintableString(this.registrationNumber, true));
        }
        if (this.addProfessionInfo != null) {
            aSN1EncodableVector.add(this.addProfessionInfo);
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
