package org.bouncycastle.pqc.asn1;

import java.math.BigInteger;
import java.util.Vector;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.pqc.crypto.gmss.GMSSLeaf;
import org.bouncycastle.pqc.crypto.gmss.GMSSParameters;
import org.bouncycastle.pqc.crypto.gmss.GMSSRootCalc;
import org.bouncycastle.pqc.crypto.gmss.GMSSRootSig;
import org.bouncycastle.pqc.crypto.gmss.Treehash;

public class GMSSPrivateKey extends ASN1Object {
    private ASN1Primitive primitive;

    private GMSSPrivateKey(ASN1Sequence aSN1Sequence) {
        int i;
        ASN1Sequence aSN1Sequence2;
        int i2;
        ASN1Sequence aSN1Sequence3 = (ASN1Sequence) aSN1Sequence.getObjectAt(0);
        int[] iArr = new int[aSN1Sequence3.size()];
        for (i = 0; i < aSN1Sequence3.size(); i++) {
            iArr[i] = checkBigIntegerInIntRange(aSN1Sequence3.getObjectAt(i));
        }
        aSN1Sequence3 = (ASN1Sequence) aSN1Sequence.getObjectAt(1);
        byte[][] bArr = new byte[aSN1Sequence3.size()][];
        for (i = 0; i < bArr.length; i++) {
            bArr[i] = ((DEROctetString) aSN1Sequence3.getObjectAt(i)).getOctets();
        }
        aSN1Sequence3 = (ASN1Sequence) aSN1Sequence.getObjectAt(2);
        bArr = new byte[aSN1Sequence3.size()][];
        for (i = 0; i < bArr.length; i++) {
            bArr[i] = ((DEROctetString) aSN1Sequence3.getObjectAt(i)).getOctets();
        }
        aSN1Sequence3 = (ASN1Sequence) aSN1Sequence.getObjectAt(3);
        byte[][][] bArr2 = new byte[aSN1Sequence3.size()][][];
        for (i = 0; i < bArr2.length; i++) {
            aSN1Sequence2 = (ASN1Sequence) aSN1Sequence3.getObjectAt(i);
            bArr2[i] = new byte[aSN1Sequence2.size()][];
            for (i2 = 0; i2 < bArr2[i].length; i2++) {
                bArr2[i][i2] = ((DEROctetString) aSN1Sequence2.getObjectAt(i2)).getOctets();
            }
        }
        aSN1Sequence3 = (ASN1Sequence) aSN1Sequence.getObjectAt(4);
        bArr2 = new byte[aSN1Sequence3.size()][][];
        for (i = 0; i < bArr2.length; i++) {
            aSN1Sequence2 = (ASN1Sequence) aSN1Sequence3.getObjectAt(i);
            bArr2[i] = new byte[aSN1Sequence2.size()][];
            for (i2 = 0; i2 < bArr2[i].length; i2++) {
                bArr2[i][i2] = ((DEROctetString) aSN1Sequence2.getObjectAt(i2)).getOctets();
            }
        }
        Treehash[][] treehashArr = new Treehash[((ASN1Sequence) aSN1Sequence.getObjectAt(5)).size()][];
    }

    public GMSSPrivateKey(int[] iArr, byte[][] bArr, byte[][] bArr2, byte[][][] bArr3, byte[][][] bArr4, Treehash[][] treehashArr, Treehash[][] treehashArr2, Vector[] vectorArr, Vector[] vectorArr2, Vector[][] vectorArr3, Vector[][] vectorArr4, byte[][][] bArr5, GMSSLeaf[] gMSSLeafArr, GMSSLeaf[] gMSSLeafArr2, GMSSLeaf[] gMSSLeafArr3, int[] iArr2, byte[][] bArr6, GMSSRootCalc[] gMSSRootCalcArr, byte[][] bArr7, GMSSRootSig[] gMSSRootSigArr, GMSSParameters gMSSParameters, AlgorithmIdentifier algorithmIdentifier) {
        this.primitive = encode(iArr, bArr, bArr2, bArr3, bArr4, bArr5, treehashArr, treehashArr2, vectorArr, vectorArr2, vectorArr3, vectorArr4, gMSSLeafArr, gMSSLeafArr2, gMSSLeafArr3, iArr2, bArr6, gMSSRootCalcArr, bArr7, gMSSRootSigArr, gMSSParameters, new AlgorithmIdentifier[]{algorithmIdentifier});
    }

    private static int checkBigIntegerInIntRange(ASN1Encodable aSN1Encodable) {
        BigInteger value = ((ASN1Integer) aSN1Encodable).getValue();
        if (value.compareTo(BigInteger.valueOf(2147483647L)) <= 0 && value.compareTo(BigInteger.valueOf(-2147483648L)) >= 0) {
            return value.intValue();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BigInteger not in Range: ");
        stringBuilder.append(value.toString());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private ASN1Primitive encode(int[] iArr, byte[][] bArr, byte[][] bArr2, byte[][][] bArr3, byte[][][] bArr4, byte[][][] bArr5, Treehash[][] treehashArr, Treehash[][] treehashArr2, Vector[] vectorArr, Vector[] vectorArr2, Vector[][] vectorArr3, Vector[][] vectorArr4, GMSSLeaf[] gMSSLeafArr, GMSSLeaf[] gMSSLeafArr2, GMSSLeaf[] gMSSLeafArr3, int[] iArr2, byte[][] bArr6, GMSSRootCalc[] gMSSRootCalcArr, byte[][] bArr7, GMSSRootSig[] gMSSRootSigArr, GMSSParameters gMSSParameters, AlgorithmIdentifier[] algorithmIdentifierArr) {
        int i;
        int i2;
        int i3;
        ASN1EncodableVector aSN1EncodableVector;
        int i4;
        int i5;
        ASN1EncodableVector aSN1EncodableVector2;
        int i6;
        int i7;
        int i8;
        byte[][] statByte;
        int[] statInt;
        ASN1EncodableVector aSN1EncodableVector3;
        int[] iArr3 = iArr;
        byte[][] bArr8 = bArr;
        byte[][] bArr9 = bArr2;
        byte[][][] bArr10 = bArr3;
        byte[][][] bArr11 = bArr4;
        byte[][][] bArr12 = bArr5;
        Treehash[][] treehashArr3 = treehashArr;
        Treehash[][] treehashArr4 = treehashArr2;
        Vector[] vectorArr5 = vectorArr;
        Vector[] vectorArr6 = vectorArr2;
        Vector[][] vectorArr7 = vectorArr3;
        Vector[][] vectorArr8 = vectorArr4;
        GMSSLeaf[] gMSSLeafArr4 = gMSSLeafArr;
        GMSSLeaf[] gMSSLeafArr5 = gMSSLeafArr2;
        GMSSLeaf[] gMSSLeafArr6 = gMSSLeafArr3;
        int[] iArr4 = iArr2;
        AlgorithmIdentifier[] algorithmIdentifierArr2 = algorithmIdentifierArr;
        ASN1EncodableVector aSN1EncodableVector4 = new ASN1EncodableVector();
        ASN1EncodableVector aSN1EncodableVector5 = new ASN1EncodableVector();
        int i9 = 0;
        while (i9 < iArr3.length) {
            aSN1EncodableVector5.add(new ASN1Integer((long) iArr3[i9]));
            i9++;
            vectorArr6 = vectorArr2;
            vectorArr7 = vectorArr3;
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector5));
        ASN1EncodableVector aSN1EncodableVector6 = new ASN1EncodableVector();
        for (byte[] dEROctetString : bArr8) {
            aSN1EncodableVector6.add(new DEROctetString(dEROctetString));
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector6));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        for (byte[] dEROctetString2 : bArr9) {
            aSN1EncodableVector6.add(new DEROctetString(dEROctetString2));
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector6));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        ASN1EncodableVector aSN1EncodableVector7 = new ASN1EncodableVector();
        ASN1EncodableVector aSN1EncodableVector8 = aSN1EncodableVector6;
        for (i2 = 0; i2 < bArr10.length; i2++) {
            for (byte[] dEROctetString3 : bArr10[i2]) {
                aSN1EncodableVector8.add(new DEROctetString(dEROctetString3));
            }
            aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector8));
            aSN1EncodableVector8 = new ASN1EncodableVector();
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector7));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector8 = aSN1EncodableVector6;
        for (i2 = 0; i2 < bArr11.length; i2++) {
            for (byte[] dEROctetString22 : bArr11[i2]) {
                aSN1EncodableVector8.add(new DEROctetString(dEROctetString22));
            }
            aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector8));
            aSN1EncodableVector8 = new ASN1EncodableVector();
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector7));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector8 = new ASN1EncodableVector();
        ASN1EncodableVector aSN1EncodableVector9 = new ASN1EncodableVector();
        ASN1EncodableVector aSN1EncodableVector10 = new ASN1EncodableVector();
        ASN1EncodableVector aSN1EncodableVector11 = aSN1EncodableVector9;
        aSN1EncodableVector9 = aSN1EncodableVector8;
        aSN1EncodableVector8 = aSN1EncodableVector7;
        int i10 = 0;
        while (i10 < treehashArr3.length) {
            aSN1EncodableVector = aSN1EncodableVector10;
            aSN1EncodableVector10 = aSN1EncodableVector11;
            aSN1EncodableVector11 = aSN1EncodableVector9;
            i3 = 0;
            while (i3 < treehashArr3[i10].length) {
                aSN1EncodableVector11.add(new DERSequence(algorithmIdentifierArr2[0]));
                i4 = treehashArr3[i10][i3].getStatInt()[1];
                aSN1EncodableVector10.add(new DEROctetString(treehashArr3[i10][i3].getStatByte()[0]));
                aSN1EncodableVector10.add(new DEROctetString(treehashArr3[i10][i3].getStatByte()[1]));
                aSN1EncodableVector10.add(new DEROctetString(treehashArr3[i10][i3].getStatByte()[2]));
                i9 = 0;
                while (i9 < i4) {
                    aSN1EncodableVector10.add(new DEROctetString(treehashArr3[i10][i3].getStatByte()[3 + i9]));
                    i9++;
                    vectorArr5 = vectorArr;
                }
                aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector10));
                aSN1EncodableVector10 = new ASN1EncodableVector();
                aSN1EncodableVector.add(new ASN1Integer((long) treehashArr3[i10][i3].getStatInt()[0]));
                aSN1EncodableVector.add(new ASN1Integer((long) i4));
                aSN1EncodableVector.add(new ASN1Integer((long) treehashArr3[i10][i3].getStatInt()[2]));
                aSN1EncodableVector.add(new ASN1Integer((long) treehashArr3[i10][i3].getStatInt()[3]));
                aSN1EncodableVector.add(new ASN1Integer((long) treehashArr3[i10][i3].getStatInt()[4]));
                aSN1EncodableVector.add(new ASN1Integer((long) treehashArr3[i10][i3].getStatInt()[5]));
                i5 = 0;
                while (i5 < i4) {
                    aSN1EncodableVector.add(new ASN1Integer((long) treehashArr3[i10][i3].getStatInt()[6 + i5]));
                    i5++;
                    bArr12 = bArr5;
                    treehashArr3 = treehashArr;
                }
                aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector));
                aSN1EncodableVector = new ASN1EncodableVector();
                aSN1EncodableVector8.add(new DERSequence(aSN1EncodableVector11));
                aSN1EncodableVector11 = new ASN1EncodableVector();
                i3++;
                bArr12 = bArr5;
                treehashArr3 = treehashArr;
                vectorArr5 = vectorArr;
            }
            aSN1EncodableVector6.add(new DERSequence(aSN1EncodableVector8));
            aSN1EncodableVector8 = new ASN1EncodableVector();
            i10++;
            aSN1EncodableVector9 = aSN1EncodableVector11;
            aSN1EncodableVector11 = aSN1EncodableVector10;
            aSN1EncodableVector10 = aSN1EncodableVector;
            bArr12 = bArr5;
            treehashArr3 = treehashArr;
            vectorArr5 = vectorArr;
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector6));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector8 = new ASN1EncodableVector();
        aSN1EncodableVector9 = new ASN1EncodableVector();
        ASN1EncodableVector aSN1EncodableVector12 = new ASN1EncodableVector();
        aSN1EncodableVector11 = aSN1EncodableVector9;
        aSN1EncodableVector9 = aSN1EncodableVector8;
        aSN1EncodableVector8 = aSN1EncodableVector7;
        i10 = 0;
        while (i10 < treehashArr4.length) {
            aSN1EncodableVector2 = aSN1EncodableVector12;
            aSN1EncodableVector12 = aSN1EncodableVector11;
            aSN1EncodableVector11 = aSN1EncodableVector9;
            for (i3 = 0; i3 < treehashArr4[i10].length; i3++) {
                aSN1EncodableVector11.add(new DERSequence(algorithmIdentifierArr2[0]));
                i5 = treehashArr4[i10][i3].getStatInt()[1];
                aSN1EncodableVector12.add(new DEROctetString(treehashArr4[i10][i3].getStatByte()[0]));
                aSN1EncodableVector12.add(new DEROctetString(treehashArr4[i10][i3].getStatByte()[1]));
                aSN1EncodableVector12.add(new DEROctetString(treehashArr4[i10][i3].getStatByte()[2]));
                for (i = 0; i < i5; i++) {
                    aSN1EncodableVector12.add(new DEROctetString(treehashArr4[i10][i3].getStatByte()[3 + i]));
                }
                aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector12));
                aSN1EncodableVector12 = new ASN1EncodableVector();
                aSN1EncodableVector2.add(new ASN1Integer((long) treehashArr4[i10][i3].getStatInt()[0]));
                aSN1EncodableVector2.add(new ASN1Integer((long) i5));
                aSN1EncodableVector2.add(new ASN1Integer((long) treehashArr4[i10][i3].getStatInt()[2]));
                aSN1EncodableVector2.add(new ASN1Integer((long) treehashArr4[i10][i3].getStatInt()[3]));
                aSN1EncodableVector2.add(new ASN1Integer((long) treehashArr4[i10][i3].getStatInt()[4]));
                aSN1EncodableVector2.add(new ASN1Integer((long) treehashArr4[i10][i3].getStatInt()[5]));
                for (i = 0; i < i5; i++) {
                    aSN1EncodableVector2.add(new ASN1Integer((long) treehashArr4[i10][i3].getStatInt()[6 + i]));
                }
                aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector2));
                aSN1EncodableVector2 = new ASN1EncodableVector();
                aSN1EncodableVector8.add(new DERSequence(aSN1EncodableVector11));
                aSN1EncodableVector11 = new ASN1EncodableVector();
            }
            aSN1EncodableVector6.add(new DERSequence(new DERSequence(aSN1EncodableVector8)));
            aSN1EncodableVector8 = new ASN1EncodableVector();
            i10++;
            aSN1EncodableVector9 = aSN1EncodableVector11;
            aSN1EncodableVector11 = aSN1EncodableVector12;
            aSN1EncodableVector12 = aSN1EncodableVector2;
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector6));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector9 = aSN1EncodableVector6;
        byte[][][] bArr13 = bArr5;
        for (i6 = 0; i6 < bArr13.length; i6++) {
            for (byte[] dEROctetString4 : bArr13[i6]) {
                aSN1EncodableVector9.add(new DEROctetString(dEROctetString4));
            }
            aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector9));
            aSN1EncodableVector9 = new ASN1EncodableVector();
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector7));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector9 = aSN1EncodableVector6;
        Vector[] vectorArr9 = vectorArr;
        for (i6 = 0; i6 < vectorArr9.length; i6++) {
            for (i7 = 0; i7 < vectorArr9[i6].size(); i7++) {
                aSN1EncodableVector9.add(new DEROctetString((byte[]) vectorArr9[i6].elementAt(i7)));
            }
            aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector9));
            aSN1EncodableVector9 = new ASN1EncodableVector();
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector7));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector9 = aSN1EncodableVector6;
        vectorArr9 = vectorArr2;
        for (i6 = 0; i6 < vectorArr9.length; i6++) {
            for (i7 = 0; i7 < vectorArr9[i6].size(); i7++) {
                aSN1EncodableVector9.add(new DEROctetString((byte[]) vectorArr9[i6].elementAt(i7)));
            }
            aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector9));
            aSN1EncodableVector9 = new ASN1EncodableVector();
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector7));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector8 = new ASN1EncodableVector();
        aSN1EncodableVector9 = aSN1EncodableVector6;
        aSN1EncodableVector11 = aSN1EncodableVector7;
        i10 = 0;
        Vector[][] vectorArr10 = vectorArr3;
        while (i10 < vectorArr10.length) {
            aSN1EncodableVector12 = aSN1EncodableVector9;
            for (i3 = 0; i3 < vectorArr10[i10].length; i3++) {
                for (i8 = 0; i8 < vectorArr10[i10][i3].size(); i8++) {
                    aSN1EncodableVector12.add(new DEROctetString((byte[]) vectorArr10[i10][i3].elementAt(i8)));
                }
                aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector12));
                aSN1EncodableVector12 = new ASN1EncodableVector();
            }
            aSN1EncodableVector8.add(new DERSequence(aSN1EncodableVector11));
            aSN1EncodableVector11 = new ASN1EncodableVector();
            i10++;
            aSN1EncodableVector9 = aSN1EncodableVector12;
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector8));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector8 = new ASN1EncodableVector();
        aSN1EncodableVector9 = aSN1EncodableVector6;
        aSN1EncodableVector11 = aSN1EncodableVector7;
        i10 = 0;
        vectorArr10 = vectorArr4;
        while (i10 < vectorArr10.length) {
            aSN1EncodableVector12 = aSN1EncodableVector9;
            for (i3 = 0; i3 < vectorArr10[i10].length; i3++) {
                for (i8 = 0; i8 < vectorArr10[i10][i3].size(); i8++) {
                    aSN1EncodableVector12.add(new DEROctetString((byte[]) vectorArr10[i10][i3].elementAt(i8)));
                }
                aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector12));
                aSN1EncodableVector12 = new ASN1EncodableVector();
            }
            aSN1EncodableVector8.add(new DERSequence(aSN1EncodableVector11));
            aSN1EncodableVector11 = new ASN1EncodableVector();
            i10++;
            aSN1EncodableVector9 = aSN1EncodableVector12;
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector8));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector11 = new ASN1EncodableVector();
        aSN1EncodableVector12 = new ASN1EncodableVector();
        aSN1EncodableVector9 = aSN1EncodableVector7;
        GMSSLeaf[] gMSSLeafArr7 = gMSSLeafArr;
        for (i6 = 0; i6 < gMSSLeafArr7.length; i6++) {
            aSN1EncodableVector9.add(new DERSequence(algorithmIdentifierArr2[0]));
            statByte = gMSSLeafArr7[i6].getStatByte();
            aSN1EncodableVector11.add(new DEROctetString(statByte[0]));
            aSN1EncodableVector11.add(new DEROctetString(statByte[1]));
            aSN1EncodableVector11.add(new DEROctetString(statByte[2]));
            aSN1EncodableVector11.add(new DEROctetString(statByte[3]));
            aSN1EncodableVector9.add(new DERSequence(aSN1EncodableVector11));
            aSN1EncodableVector11 = new ASN1EncodableVector();
            statInt = gMSSLeafArr7[i6].getStatInt();
            aSN1EncodableVector12.add(new ASN1Integer((long) statInt[0]));
            aSN1EncodableVector12.add(new ASN1Integer((long) statInt[1]));
            aSN1EncodableVector12.add(new ASN1Integer((long) statInt[2]));
            aSN1EncodableVector12.add(new ASN1Integer((long) statInt[3]));
            aSN1EncodableVector9.add(new DERSequence(aSN1EncodableVector12));
            aSN1EncodableVector12 = new ASN1EncodableVector();
            aSN1EncodableVector6.add(new DERSequence(aSN1EncodableVector9));
            aSN1EncodableVector9 = new ASN1EncodableVector();
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector6));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector11 = new ASN1EncodableVector();
        aSN1EncodableVector12 = new ASN1EncodableVector();
        aSN1EncodableVector9 = aSN1EncodableVector7;
        gMSSLeafArr7 = gMSSLeafArr2;
        for (i6 = 0; i6 < gMSSLeafArr7.length; i6++) {
            aSN1EncodableVector9.add(new DERSequence(algorithmIdentifierArr2[0]));
            statByte = gMSSLeafArr7[i6].getStatByte();
            aSN1EncodableVector11.add(new DEROctetString(statByte[0]));
            aSN1EncodableVector11.add(new DEROctetString(statByte[1]));
            aSN1EncodableVector11.add(new DEROctetString(statByte[2]));
            aSN1EncodableVector11.add(new DEROctetString(statByte[3]));
            aSN1EncodableVector9.add(new DERSequence(aSN1EncodableVector11));
            aSN1EncodableVector11 = new ASN1EncodableVector();
            statInt = gMSSLeafArr7[i6].getStatInt();
            aSN1EncodableVector12.add(new ASN1Integer((long) statInt[0]));
            aSN1EncodableVector12.add(new ASN1Integer((long) statInt[1]));
            aSN1EncodableVector12.add(new ASN1Integer((long) statInt[2]));
            aSN1EncodableVector12.add(new ASN1Integer((long) statInt[3]));
            aSN1EncodableVector9.add(new DERSequence(aSN1EncodableVector12));
            aSN1EncodableVector12 = new ASN1EncodableVector();
            aSN1EncodableVector6.add(new DERSequence(aSN1EncodableVector9));
            aSN1EncodableVector9 = new ASN1EncodableVector();
        }
        aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector6));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector11 = new ASN1EncodableVector();
        aSN1EncodableVector12 = new ASN1EncodableVector();
        aSN1EncodableVector2 = new ASN1EncodableVector();
        aSN1EncodableVector8 = aSN1EncodableVector4;
        gMSSLeafArr7 = gMSSLeafArr3;
        for (i3 = 0; i3 < gMSSLeafArr7.length; i3++) {
            aSN1EncodableVector11.add(new DERSequence(algorithmIdentifierArr2[0]));
            byte[][] statByte2 = gMSSLeafArr7[i3].getStatByte();
            aSN1EncodableVector12.add(new DEROctetString(statByte2[0]));
            aSN1EncodableVector12.add(new DEROctetString(statByte2[1]));
            aSN1EncodableVector12.add(new DEROctetString(statByte2[2]));
            aSN1EncodableVector12.add(new DEROctetString(statByte2[3]));
            aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector12));
            aSN1EncodableVector12 = new ASN1EncodableVector();
            int[] statInt2 = gMSSLeafArr7[i3].getStatInt();
            aSN1EncodableVector2.add(new ASN1Integer((long) statInt2[0]));
            aSN1EncodableVector2.add(new ASN1Integer((long) statInt2[1]));
            aSN1EncodableVector2.add(new ASN1Integer((long) statInt2[2]));
            aSN1EncodableVector2.add(new ASN1Integer((long) statInt2[3]));
            aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector2));
            aSN1EncodableVector2 = new ASN1EncodableVector();
            aSN1EncodableVector6.add(new DERSequence(aSN1EncodableVector11));
            aSN1EncodableVector11 = new ASN1EncodableVector();
        }
        aSN1EncodableVector8.add(new DERSequence(aSN1EncodableVector6));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        ASN1Encodable[] aSN1EncodableArr = algorithmIdentifierArr2;
        int[] iArr5 = iArr2;
        for (int i82 : iArr5) {
            aSN1EncodableVector6.add(new ASN1Integer((long) i82));
        }
        aSN1EncodableVector8.add(new DERSequence(aSN1EncodableVector6));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        bArr8 = bArr6;
        for (byte[] dEROctetString42 : bArr8) {
            aSN1EncodableVector6.add(new DEROctetString(dEROctetString42));
        }
        aSN1EncodableVector8.add(new DERSequence(aSN1EncodableVector6));
        aSN1EncodableVector6 = new ASN1EncodableVector();
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector11 = new ASN1EncodableVector();
        aSN1EncodableVector11 = new ASN1EncodableVector();
        aSN1EncodableVector12 = new ASN1EncodableVector();
        ASN1EncodableVector aSN1EncodableVector13 = new ASN1EncodableVector();
        aSN1EncodableVector10 = new ASN1EncodableVector();
        aSN1EncodableVector2 = aSN1EncodableVector11;
        ASN1EncodableVector aSN1EncodableVector14 = aSN1EncodableVector12;
        i7 = 0;
        aSN1EncodableVector12 = aSN1EncodableVector7;
        GMSSRootCalc[] gMSSRootCalcArr2 = gMSSRootCalcArr;
        while (true) {
            i4 = 7;
            i9 = 8;
            if (i7 >= gMSSRootCalcArr2.length) {
                break;
            }
            ASN1EncodableVector aSN1EncodableVector15;
            ASN1EncodableVector aSN1EncodableVector16;
            Object[] objArr;
            aSN1EncodableVector12.add(new DERSequence(aSN1EncodableArr[0]));
            aSN1EncodableVector = new ASN1EncodableVector();
            int i11 = gMSSRootCalcArr2[i7].getStatInt()[0];
            i4 = gMSSRootCalcArr2[i7].getStatInt()[7];
            aSN1EncodableVector2.add(new DEROctetString(gMSSRootCalcArr2[i7].getStatByte()[0]));
            int i12 = 0;
            while (i12 < i11) {
                i12 = 1 + i12;
                aSN1EncodableVector2.add(new DEROctetString(gMSSRootCalcArr2[i7].getStatByte()[i12]));
            }
            for (i12 = 0; i12 < i4; i12++) {
                aSN1EncodableVector2.add(new DEROctetString(gMSSRootCalcArr2[i7].getStatByte()[(1 + i11) + i12]));
            }
            aSN1EncodableVector12.add(new DERSequence(aSN1EncodableVector2));
            aSN1EncodableVector2 = new ASN1EncodableVector();
            aSN1EncodableVector14.add(new ASN1Integer((long) i11));
            aSN1EncodableVector14.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getStatInt()[1]));
            aSN1EncodableVector14.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getStatInt()[2]));
            aSN1EncodableVector14.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getStatInt()[3]));
            aSN1EncodableVector14.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getStatInt()[4]));
            aSN1EncodableVector14.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getStatInt()[5]));
            aSN1EncodableVector14.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getStatInt()[6]));
            aSN1EncodableVector14.add(new ASN1Integer((long) i4));
            i12 = 0;
            while (i12 < i11) {
                int i13 = i12;
                aSN1EncodableVector14.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getStatInt()[i9 + i12]));
                i12 = i13 + 1;
                i9 = 8;
            }
            for (i9 = 0; i9 < i4; i9++) {
                aSN1EncodableVector14.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getStatInt()[(8 + i11) + i9]));
            }
            aSN1EncodableVector12.add(new DERSequence(aSN1EncodableVector14));
            aSN1EncodableVector14 = new ASN1EncodableVector();
            aSN1EncodableVector = new ASN1EncodableVector();
            ASN1EncodableVector aSN1EncodableVector17 = new ASN1EncodableVector();
            ASN1EncodableVector aSN1EncodableVector18 = new ASN1EncodableVector();
            if (gMSSRootCalcArr2[i7].getTreehash() != null) {
                aSN1EncodableVector5 = aSN1EncodableVector18;
                aSN1EncodableVector18 = aSN1EncodableVector17;
                aSN1EncodableVector17 = aSN1EncodableVector;
                i11 = 0;
                while (i11 < gMSSRootCalcArr2[i7].getTreehash().length) {
                    aSN1EncodableVector17.add(new DERSequence(aSN1EncodableArr[0]));
                    int i14 = gMSSRootCalcArr2[i7].getTreehash()[i11].getStatInt()[1];
                    aSN1EncodableVector15 = aSN1EncodableVector2;
                    aSN1EncodableVector18.add(new DEROctetString(gMSSRootCalcArr2[i7].getTreehash()[i11].getStatByte()[0]));
                    aSN1EncodableVector18.add(new DEROctetString(gMSSRootCalcArr2[i7].getTreehash()[i11].getStatByte()[1]));
                    aSN1EncodableVector18.add(new DEROctetString(gMSSRootCalcArr2[i7].getTreehash()[i11].getStatByte()[2]));
                    i82 = 0;
                    while (i82 < i14) {
                        aSN1EncodableVector16 = aSN1EncodableVector14;
                        aSN1EncodableVector18.add(new DEROctetString(gMSSRootCalcArr2[i7].getTreehash()[i11].getStatByte()[3 + i82]));
                        i82++;
                        aSN1EncodableVector14 = aSN1EncodableVector16;
                    }
                    aSN1EncodableVector16 = aSN1EncodableVector14;
                    aSN1EncodableVector17.add(new DERSequence(aSN1EncodableVector18));
                    aSN1EncodableVector18 = new ASN1EncodableVector();
                    aSN1EncodableVector3 = aSN1EncodableVector8;
                    aSN1EncodableVector5.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getTreehash()[i11].getStatInt()[0]));
                    aSN1EncodableVector5.add(new ASN1Integer((long) i14));
                    aSN1EncodableVector5.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getTreehash()[i11].getStatInt()[2]));
                    aSN1EncodableVector5.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getTreehash()[i11].getStatInt()[3]));
                    aSN1EncodableVector5.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getTreehash()[i11].getStatInt()[4]));
                    aSN1EncodableVector5.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getTreehash()[i11].getStatInt()[5]));
                    for (i6 = 0; i6 < i14; i6++) {
                        aSN1EncodableVector5.add(new ASN1Integer((long) gMSSRootCalcArr2[i7].getTreehash()[i11].getStatInt()[6 + i6]));
                    }
                    aSN1EncodableVector17.add(new DERSequence(aSN1EncodableVector5));
                    aSN1EncodableVector5 = new ASN1EncodableVector();
                    aSN1EncodableVector13.add(new DERSequence(aSN1EncodableVector17));
                    aSN1EncodableVector17 = new ASN1EncodableVector();
                    i11++;
                    aSN1EncodableVector2 = aSN1EncodableVector15;
                    aSN1EncodableVector14 = aSN1EncodableVector16;
                    aSN1EncodableVector8 = aSN1EncodableVector3;
                    objArr = algorithmIdentifierArr;
                }
            }
            aSN1EncodableVector3 = aSN1EncodableVector8;
            aSN1EncodableVector15 = aSN1EncodableVector2;
            aSN1EncodableVector16 = aSN1EncodableVector14;
            aSN1EncodableVector12.add(new DERSequence(aSN1EncodableVector13));
            aSN1EncodableVector13 = new ASN1EncodableVector();
            aSN1EncodableVector8 = new ASN1EncodableVector();
            if (gMSSRootCalcArr2[i7].getRetain() != null) {
                aSN1EncodableVector9 = aSN1EncodableVector8;
                for (i6 = 0; i6 < gMSSRootCalcArr2[i7].getRetain().length; i6++) {
                    for (i82 = 0; i82 < gMSSRootCalcArr2[i7].getRetain()[i6].size(); i82++) {
                        aSN1EncodableVector9.add(new DEROctetString((byte[]) gMSSRootCalcArr2[i7].getRetain()[i6].elementAt(i82)));
                    }
                    aSN1EncodableVector10.add(new DERSequence(aSN1EncodableVector9));
                    aSN1EncodableVector9 = new ASN1EncodableVector();
                }
            }
            aSN1EncodableVector12.add(new DERSequence(aSN1EncodableVector10));
            aSN1EncodableVector10 = new ASN1EncodableVector();
            aSN1EncodableVector6.add(new DERSequence(aSN1EncodableVector12));
            aSN1EncodableVector12 = new ASN1EncodableVector();
            i7++;
            aSN1EncodableVector2 = aSN1EncodableVector15;
            aSN1EncodableVector14 = aSN1EncodableVector16;
            aSN1EncodableVector8 = aSN1EncodableVector3;
            objArr = algorithmIdentifierArr;
        }
        aSN1EncodableVector3 = aSN1EncodableVector8;
        DERSequence dERSequence = new DERSequence(aSN1EncodableVector6);
        aSN1EncodableVector6 = aSN1EncodableVector3;
        aSN1EncodableVector6.add(dERSequence);
        aSN1EncodableVector7 = new ASN1EncodableVector();
        bArr9 = bArr7;
        for (byte[] dEROctetString5 : bArr9) {
            aSN1EncodableVector7.add(new DEROctetString(dEROctetString5));
        }
        aSN1EncodableVector6.add(new DERSequence(aSN1EncodableVector7));
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector8 = new ASN1EncodableVector();
        aSN1EncodableVector9 = new ASN1EncodableVector();
        aSN1EncodableVector12 = new ASN1EncodableVector();
        aSN1EncodableVector2 = new ASN1EncodableVector();
        i3 = 0;
        aSN1EncodableVector11 = aSN1EncodableVector8;
        GMSSRootSig[] gMSSRootSigArr2 = gMSSRootSigArr;
        while (i3 < gMSSRootSigArr2.length) {
            aSN1EncodableVector11.add(new DERSequence(algorithmIdentifierArr[0]));
            aSN1EncodableVector14 = new ASN1EncodableVector();
            aSN1EncodableVector12.add(new DEROctetString(gMSSRootSigArr2[i3].getStatByte()[0]));
            aSN1EncodableVector12.add(new DEROctetString(gMSSRootSigArr2[i3].getStatByte()[1]));
            aSN1EncodableVector12.add(new DEROctetString(gMSSRootSigArr2[i3].getStatByte()[2]));
            aSN1EncodableVector12.add(new DEROctetString(gMSSRootSigArr2[i3].getStatByte()[3]));
            aSN1EncodableVector12.add(new DEROctetString(gMSSRootSigArr2[i3].getStatByte()[4]));
            aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector12));
            aSN1EncodableVector12 = new ASN1EncodableVector();
            aSN1EncodableVector2.add(new ASN1Integer((long) gMSSRootSigArr2[i3].getStatInt()[0]));
            aSN1EncodableVector2.add(new ASN1Integer((long) gMSSRootSigArr2[i3].getStatInt()[1]));
            aSN1EncodableVector2.add(new ASN1Integer((long) gMSSRootSigArr2[i3].getStatInt()[2]));
            aSN1EncodableVector2.add(new ASN1Integer((long) gMSSRootSigArr2[i3].getStatInt()[3]));
            aSN1EncodableVector2.add(new ASN1Integer((long) gMSSRootSigArr2[i3].getStatInt()[4]));
            aSN1EncodableVector2.add(new ASN1Integer((long) gMSSRootSigArr2[i3].getStatInt()[5]));
            aSN1EncodableVector2.add(new ASN1Integer((long) gMSSRootSigArr2[i3].getStatInt()[6]));
            aSN1EncodableVector2.add(new ASN1Integer((long) gMSSRootSigArr2[i3].getStatInt()[i4]));
            aSN1EncodableVector2.add(new ASN1Integer((long) gMSSRootSigArr2[i3].getStatInt()[8]));
            aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector2));
            aSN1EncodableVector2 = new ASN1EncodableVector();
            aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector11));
            aSN1EncodableVector11 = new ASN1EncodableVector();
            i3++;
            i4 = 7;
        }
        AlgorithmIdentifier[] algorithmIdentifierArr3 = algorithmIdentifierArr;
        aSN1EncodableVector6.add(new DERSequence(aSN1EncodableVector7));
        aSN1EncodableVector7 = new ASN1EncodableVector();
        aSN1EncodableVector8 = new ASN1EncodableVector();
        aSN1EncodableVector9 = new ASN1EncodableVector();
        aSN1EncodableVector11 = new ASN1EncodableVector();
        for (int i15 = 0; i15 < gMSSParameters.getHeightOfTrees().length; i15++) {
            aSN1EncodableVector8.add(new ASN1Integer((long) gMSSParameters.getHeightOfTrees()[i15]));
            aSN1EncodableVector9.add(new ASN1Integer((long) gMSSParameters.getWinternitzParameter()[i15]));
            aSN1EncodableVector11.add(new ASN1Integer((long) gMSSParameters.getK()[i15]));
        }
        aSN1EncodableVector7.add(new ASN1Integer((long) gMSSParameters.getNumOfLayers()));
        aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector8));
        aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector9));
        aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector11));
        aSN1EncodableVector6.add(new DERSequence(aSN1EncodableVector7));
        aSN1EncodableVector7 = new ASN1EncodableVector();
        for (ASN1Encodable add : algorithmIdentifierArr3) {
            aSN1EncodableVector7.add(add);
        }
        aSN1EncodableVector6.add(new DERSequence(aSN1EncodableVector7));
        return new DERSequence(aSN1EncodableVector6);
    }

    public ASN1Primitive toASN1Primitive() {
        return this.primitive;
    }
}
