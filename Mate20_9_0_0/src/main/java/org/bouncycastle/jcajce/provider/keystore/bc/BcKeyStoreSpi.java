package org.bouncycastle.jcajce.provider.keystore.bc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.io.DigestInputStream;
import org.bouncycastle.crypto.io.DigestOutputStream;
import org.bouncycastle.crypto.io.MacInputStream;
import org.bouncycastle.crypto.io.MacOutputStream;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.jcajce.util.BCJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.interfaces.BCKeyStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.io.Streams;
import org.bouncycastle.util.io.TeeOutputStream;

public class BcKeyStoreSpi extends KeyStoreSpi implements BCKeyStore {
    static final int CERTIFICATE = 1;
    static final int KEY = 2;
    private static final String KEY_CIPHER = "PBEWithSHAAnd3-KeyTripleDES-CBC";
    static final int KEY_PRIVATE = 0;
    static final int KEY_PUBLIC = 1;
    private static final int KEY_SALT_SIZE = 20;
    static final int KEY_SECRET = 2;
    private static final int MIN_ITERATIONS = 1024;
    static final int NULL = 0;
    static final int SEALED = 4;
    static final int SECRET = 3;
    private static final String STORE_CIPHER = "PBEWithSHAAndTwofish-CBC";
    private static final int STORE_SALT_SIZE = 20;
    private static final int STORE_VERSION = 2;
    private final JcaJceHelper helper = new BCJcaJceHelper();
    protected SecureRandom random = new SecureRandom();
    protected Hashtable table = new Hashtable();
    protected int version;

    private class StoreEntry {
        String alias;
        Certificate[] certChain;
        Date date;
        Object obj;
        int type;

        StoreEntry(String str, Key key, char[] cArr, Certificate[] certificateArr) throws Exception {
            this.date = new Date();
            this.type = 4;
            this.alias = str;
            this.certChain = certificateArr;
            byte[] bArr = new byte[20];
            BcKeyStoreSpi.this.random.setSeed(System.currentTimeMillis());
            BcKeyStoreSpi.this.random.nextBytes(bArr);
            int nextInt = 1024 + (BcKeyStoreSpi.this.random.nextInt() & 1023);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeInt(bArr.length);
            dataOutputStream.write(bArr);
            dataOutputStream.writeInt(nextInt);
            DataOutputStream dataOutputStream2 = new DataOutputStream(new CipherOutputStream(dataOutputStream, BcKeyStoreSpi.this.makePBECipher(BcKeyStoreSpi.KEY_CIPHER, 1, cArr, bArr, nextInt)));
            BcKeyStoreSpi.this.encodeKey(key, dataOutputStream2);
            dataOutputStream2.close();
            this.obj = byteArrayOutputStream.toByteArray();
        }

        StoreEntry(String str, Certificate certificate) {
            this.date = new Date();
            this.type = 1;
            this.alias = str;
            this.obj = certificate;
            this.certChain = null;
        }

        StoreEntry(String str, Date date, int i, Object obj) {
            this.date = new Date();
            this.alias = str;
            this.date = date;
            this.type = i;
            this.obj = obj;
        }

        StoreEntry(String str, Date date, int i, Object obj, Certificate[] certificateArr) {
            this.date = new Date();
            this.alias = str;
            this.date = date;
            this.type = i;
            this.obj = obj;
            this.certChain = certificateArr;
        }

        StoreEntry(String str, byte[] bArr, Certificate[] certificateArr) {
            this.date = new Date();
            this.type = 3;
            this.alias = str;
            this.obj = bArr;
            this.certChain = certificateArr;
        }

        String getAlias() {
            return this.alias;
        }

        Certificate[] getCertificateChain() {
            return this.certChain;
        }

        Date getDate() {
            return this.date;
        }

        Object getObject() {
            return this.obj;
        }

        Object getObject(char[] cArr) throws NoSuchAlgorithmException, UnrecoverableKeyException {
            Key access$100;
            if ((cArr == null || cArr.length == 0) && (this.obj instanceof Key)) {
                return this.obj;
            }
            if (this.type == 4) {
                DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream((byte[]) this.obj));
                byte[] bArr;
                try {
                    bArr = new byte[dataInputStream.readInt()];
                    dataInputStream.readFully(bArr);
                    return BcKeyStoreSpi.this.decodeKey(new DataInputStream(new CipherInputStream(dataInputStream, BcKeyStoreSpi.this.makePBECipher(BcKeyStoreSpi.KEY_CIPHER, 2, cArr, bArr, dataInputStream.readInt()))));
                } catch (Exception e) {
                    dataInputStream = new DataInputStream(new ByteArrayInputStream((byte[]) this.obj));
                    byte[] bArr2 = new byte[dataInputStream.readInt()];
                    dataInputStream.readFully(bArr2);
                    int readInt = dataInputStream.readInt();
                    try {
                        access$100 = BcKeyStoreSpi.this.decodeKey(new DataInputStream(new CipherInputStream(dataInputStream, BcKeyStoreSpi.this.makePBECipher("BrokenPBEWithSHAAnd3-KeyTripleDES-CBC", 2, cArr, bArr2, readInt))));
                    } catch (Exception e2) {
                        dataInputStream = new DataInputStream(new ByteArrayInputStream((byte[]) this.obj));
                        bArr2 = new byte[dataInputStream.readInt()];
                        dataInputStream.readFully(bArr2);
                        readInt = dataInputStream.readInt();
                        access$100 = BcKeyStoreSpi.this.decodeKey(new DataInputStream(new CipherInputStream(dataInputStream, BcKeyStoreSpi.this.makePBECipher("OldPBEWithSHAAnd3-KeyTripleDES-CBC", 2, cArr, bArr2, readInt))));
                    }
                    bArr = bArr2;
                    int i = readInt;
                    if (access$100 != null) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                        dataOutputStream.writeInt(bArr.length);
                        dataOutputStream.write(bArr);
                        dataOutputStream.writeInt(i);
                        DataOutputStream dataOutputStream2 = new DataOutputStream(new CipherOutputStream(dataOutputStream, BcKeyStoreSpi.this.makePBECipher(BcKeyStoreSpi.KEY_CIPHER, 1, cArr, bArr, i)));
                        BcKeyStoreSpi.this.encodeKey(access$100, dataOutputStream2);
                        dataOutputStream2.close();
                        this.obj = byteArrayOutputStream.toByteArray();
                        return access$100;
                    }
                    throw new UnrecoverableKeyException("no match");
                } catch (Exception e3) {
                    throw new UnrecoverableKeyException("no match");
                }
            }
            throw new RuntimeException("forget something!");
        }

        int getType() {
            return this.type;
        }
    }

    public static class BouncyCastleStore extends BcKeyStoreSpi {
        public BouncyCastleStore() {
            super(1);
        }

        public void engineLoad(InputStream inputStream, char[] cArr) throws IOException {
            this.table.clear();
            if (inputStream != null) {
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                int readInt = dataInputStream.readInt();
                if (readInt == 2 || readInt == 0 || readInt == 1) {
                    byte[] bArr = new byte[dataInputStream.readInt()];
                    if (bArr.length == 20) {
                        dataInputStream.readFully(bArr);
                        int readInt2 = dataInputStream.readInt();
                        if (readInt2 < 0 || readInt2 > PKIFailureInfo.notAuthorized) {
                            throw new IOException("Key store corrupted.");
                        }
                        CipherInputStream cipherInputStream = new CipherInputStream(dataInputStream, makePBECipher(readInt == 0 ? "OldPBEWithSHAAndTwofish-CBC" : BcKeyStoreSpi.STORE_CIPHER, 2, cArr, bArr, readInt2));
                        SHA1Digest sHA1Digest = new SHA1Digest();
                        loadStore(new DigestInputStream(cipherInputStream, sHA1Digest));
                        byte[] bArr2 = new byte[sHA1Digest.getDigestSize()];
                        sHA1Digest.doFinal(bArr2, 0);
                        byte[] bArr3 = new byte[sHA1Digest.getDigestSize()];
                        Streams.readFully(cipherInputStream, bArr3);
                        if (!Arrays.constantTimeAreEqual(bArr2, bArr3)) {
                            this.table.clear();
                            throw new IOException("KeyStore integrity check failed.");
                        }
                        return;
                    }
                    throw new IOException("Key store corrupted.");
                }
                throw new IOException("Wrong version of key store.");
            }
        }

        public void engineStore(OutputStream outputStream, char[] cArr) throws IOException {
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            byte[] bArr = new byte[20];
            int nextInt = 1024 + (this.random.nextInt() & 1023);
            this.random.nextBytes(bArr);
            dataOutputStream.writeInt(this.version);
            dataOutputStream.writeInt(bArr.length);
            dataOutputStream.write(bArr);
            dataOutputStream.writeInt(nextInt);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(dataOutputStream, makePBECipher(BcKeyStoreSpi.STORE_CIPHER, 1, cArr, bArr, nextInt));
            DigestOutputStream digestOutputStream = new DigestOutputStream(new SHA1Digest());
            saveStore(new TeeOutputStream(cipherOutputStream, digestOutputStream));
            cipherOutputStream.write(digestOutputStream.getDigest());
            cipherOutputStream.close();
        }
    }

    public static class Std extends BcKeyStoreSpi {
        public Std() {
            super(2);
        }
    }

    public static class Version1 extends BcKeyStoreSpi {
        public Version1() {
            super(1);
        }
    }

    public BcKeyStoreSpi(int i) {
        this.version = i;
    }

    private Certificate decodeCertificate(DataInputStream dataInputStream) throws IOException {
        String readUTF = dataInputStream.readUTF();
        byte[] bArr = new byte[dataInputStream.readInt()];
        dataInputStream.readFully(bArr);
        try {
            return this.helper.createCertificateFactory(readUTF).generateCertificate(new ByteArrayInputStream(bArr));
        } catch (NoSuchProviderException e) {
            throw new IOException(e.toString());
        } catch (CertificateException e2) {
            throw new IOException(e2.toString());
        }
    }

    private Key decodeKey(DataInputStream dataInputStream) throws IOException {
        KeySpec pKCS8EncodedKeySpec;
        int read = dataInputStream.read();
        String readUTF = dataInputStream.readUTF();
        String readUTF2 = dataInputStream.readUTF();
        byte[] bArr = new byte[dataInputStream.readInt()];
        dataInputStream.readFully(bArr);
        if (readUTF.equals("PKCS#8") || readUTF.equals("PKCS8")) {
            pKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(bArr);
        } else if (readUTF.equals("X.509") || readUTF.equals("X509")) {
            pKCS8EncodedKeySpec = new X509EncodedKeySpec(bArr);
        } else if (readUTF.equals("RAW")) {
            return new SecretKeySpec(bArr, readUTF2);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Key format ");
            stringBuilder.append(readUTF);
            stringBuilder.append(" not recognised!");
            throw new IOException(stringBuilder.toString());
        }
        switch (read) {
            case 0:
                return BouncyCastleProvider.getPrivateKey(PrivateKeyInfo.getInstance(bArr));
            case 1:
                return BouncyCastleProvider.getPublicKey(SubjectPublicKeyInfo.getInstance(bArr));
            case 2:
                return this.helper.createSecretKeyFactory(readUTF2).generateSecret(pKCS8EncodedKeySpec);
            default:
                StringBuilder stringBuilder2;
                try {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Key type ");
                    stringBuilder2.append(read);
                    stringBuilder2.append(" not recognised!");
                    throw new IOException(stringBuilder2.toString());
                } catch (Exception e) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Exception creating key: ");
                    stringBuilder2.append(e.toString());
                    throw new IOException(stringBuilder2.toString());
                }
        }
    }

    private void encodeCertificate(Certificate certificate, DataOutputStream dataOutputStream) throws IOException {
        try {
            byte[] encoded = certificate.getEncoded();
            dataOutputStream.writeUTF(certificate.getType());
            dataOutputStream.writeInt(encoded.length);
            dataOutputStream.write(encoded);
        } catch (CertificateEncodingException e) {
            throw new IOException(e.toString());
        }
    }

    private void encodeKey(Key key, DataOutputStream dataOutputStream) throws IOException {
        byte[] encoded = key.getEncoded();
        int i = key instanceof PrivateKey ? 0 : key instanceof PublicKey ? 1 : 2;
        dataOutputStream.write(i);
        dataOutputStream.writeUTF(key.getFormat());
        dataOutputStream.writeUTF(key.getAlgorithm());
        dataOutputStream.writeInt(encoded.length);
        dataOutputStream.write(encoded);
    }

    public Enumeration engineAliases() {
        return this.table.keys();
    }

    public boolean engineContainsAlias(String str) {
        return this.table.get(str) != null;
    }

    public void engineDeleteEntry(String str) throws KeyStoreException {
        if (this.table.get(str) != null) {
            this.table.remove(str);
        }
    }

    public Certificate engineGetCertificate(String str) {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        if (storeEntry != null) {
            if (storeEntry.getType() == 1) {
                return (Certificate) storeEntry.getObject();
            }
            Certificate[] certificateChain = storeEntry.getCertificateChain();
            if (certificateChain != null) {
                return certificateChain[0];
            }
        }
        return null;
    }

    public String engineGetCertificateAlias(Certificate certificate) {
        Enumeration elements = this.table.elements();
        while (elements.hasMoreElements()) {
            StoreEntry storeEntry = (StoreEntry) elements.nextElement();
            if (!(storeEntry.getObject() instanceof Certificate)) {
                Certificate[] certificateChain = storeEntry.getCertificateChain();
                if (certificateChain != null && certificateChain[0].equals(certificate)) {
                    return storeEntry.getAlias();
                }
            } else if (((Certificate) storeEntry.getObject()).equals(certificate)) {
                return storeEntry.getAlias();
            }
        }
        return null;
    }

    public Certificate[] engineGetCertificateChain(String str) {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        return storeEntry != null ? storeEntry.getCertificateChain() : null;
    }

    public Date engineGetCreationDate(String str) {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        return storeEntry != null ? storeEntry.getDate() : null;
    }

    public Key engineGetKey(String str, char[] cArr) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        return (storeEntry == null || storeEntry.getType() == 1) ? null : (Key) storeEntry.getObject(cArr);
    }

    public boolean engineIsCertificateEntry(String str) {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        return storeEntry != null && storeEntry.getType() == 1;
    }

    public boolean engineIsKeyEntry(String str) {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        return (storeEntry == null || storeEntry.getType() == 1) ? false : true;
    }

    public void engineLoad(InputStream inputStream, char[] cArr) throws IOException {
        this.table.clear();
        if (inputStream != null) {
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            int readInt = dataInputStream.readInt();
            if (readInt == 2 || readInt == 0 || readInt == 1) {
                int readInt2 = dataInputStream.readInt();
                if (readInt2 > 0) {
                    byte[] bArr = new byte[readInt2];
                    dataInputStream.readFully(bArr);
                    int readInt3 = dataInputStream.readInt();
                    HMac hMac = new HMac(new SHA1Digest());
                    if (cArr == null || cArr.length == 0) {
                        loadStore(dataInputStream);
                        dataInputStream.readFully(new byte[hMac.getMacSize()]);
                        return;
                    }
                    byte[] PKCS12PasswordToBytes = PBEParametersGenerator.PKCS12PasswordToBytes(cArr);
                    PKCS12ParametersGenerator pKCS12ParametersGenerator = new PKCS12ParametersGenerator(new SHA1Digest());
                    pKCS12ParametersGenerator.init(PKCS12PasswordToBytes, bArr, readInt3);
                    CipherParameters generateDerivedMacParameters = pKCS12ParametersGenerator.generateDerivedMacParameters(readInt != 2 ? hMac.getMacSize() : hMac.getMacSize() * 8);
                    Arrays.fill(PKCS12PasswordToBytes, (byte) 0);
                    hMac.init(generateDerivedMacParameters);
                    loadStore(new MacInputStream(dataInputStream, hMac));
                    byte[] bArr2 = new byte[hMac.getMacSize()];
                    hMac.doFinal(bArr2, 0);
                    PKCS12PasswordToBytes = new byte[hMac.getMacSize()];
                    dataInputStream.readFully(PKCS12PasswordToBytes);
                    if (!Arrays.constantTimeAreEqual(bArr2, PKCS12PasswordToBytes)) {
                        this.table.clear();
                        throw new IOException("KeyStore integrity check failed.");
                    }
                    return;
                }
                throw new IOException("Invalid salt detected");
            }
            throw new IOException("Wrong version of key store.");
        }
    }

    public void engineSetCertificateEntry(String str, Certificate certificate) throws KeyStoreException {
        StoreEntry storeEntry = (StoreEntry) this.table.get(str);
        if (storeEntry == null || storeEntry.getType() == 1) {
            this.table.put(str, new StoreEntry(str, certificate));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("key store already has a key entry with alias ");
        stringBuilder.append(str);
        throw new KeyStoreException(stringBuilder.toString());
    }

    public void engineSetKeyEntry(String str, Key key, char[] cArr, Certificate[] certificateArr) throws KeyStoreException {
        if ((key instanceof PrivateKey) && certificateArr == null) {
            throw new KeyStoreException("no certificate chain for private key");
        }
        try {
            this.table.put(str, new StoreEntry(str, key, cArr, certificateArr));
        } catch (Exception e) {
            throw new KeyStoreException(e.toString());
        }
    }

    public void engineSetKeyEntry(String str, byte[] bArr, Certificate[] certificateArr) throws KeyStoreException {
        this.table.put(str, new StoreEntry(str, bArr, certificateArr));
    }

    public int engineSize() {
        return this.table.size();
    }

    public void engineStore(OutputStream outputStream, char[] cArr) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        byte[] bArr = new byte[20];
        int nextInt = 1024 + (this.random.nextInt() & 1023);
        this.random.nextBytes(bArr);
        dataOutputStream.writeInt(this.version);
        dataOutputStream.writeInt(bArr.length);
        dataOutputStream.write(bArr);
        dataOutputStream.writeInt(nextInt);
        HMac hMac = new HMac(new SHA1Digest());
        MacOutputStream macOutputStream = new MacOutputStream(hMac);
        PKCS12ParametersGenerator pKCS12ParametersGenerator = new PKCS12ParametersGenerator(new SHA1Digest());
        byte[] PKCS12PasswordToBytes = PBEParametersGenerator.PKCS12PasswordToBytes(cArr);
        pKCS12ParametersGenerator.init(PKCS12PasswordToBytes, bArr, nextInt);
        hMac.init(pKCS12ParametersGenerator.generateDerivedMacParameters(this.version < 2 ? hMac.getMacSize() : hMac.getMacSize() * 8));
        for (nextInt = 0; nextInt != PKCS12PasswordToBytes.length; nextInt++) {
            PKCS12PasswordToBytes[nextInt] = (byte) 0;
        }
        saveStore(new TeeOutputStream(dataOutputStream, macOutputStream));
        PKCS12PasswordToBytes = new byte[hMac.getMacSize()];
        hMac.doFinal(PKCS12PasswordToBytes, 0);
        dataOutputStream.write(PKCS12PasswordToBytes);
        dataOutputStream.close();
    }

    protected void loadStore(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        for (int read = dataInputStream.read(); read > 0; read = dataInputStream.read()) {
            Hashtable hashtable;
            String readUTF = dataInputStream.readUTF();
            Date date = new Date(dataInputStream.readLong());
            int readInt = dataInputStream.readInt();
            Certificate[] certificateArr = null;
            if (readInt != 0) {
                certificateArr = new Certificate[readInt];
                for (int i = 0; i != readInt; i++) {
                    certificateArr[i] = decodeCertificate(dataInputStream);
                }
            }
            Certificate[] certificateArr2 = certificateArr;
            StoreEntry storeEntry;
            switch (read) {
                case 1:
                    Object decodeCertificate = decodeCertificate(dataInputStream);
                    hashtable = this.table;
                    storeEntry = new StoreEntry(readUTF, date, 1, decodeCertificate);
                    break;
                case 2:
                    Key decodeKey = decodeKey(dataInputStream);
                    hashtable = this.table;
                    storeEntry = new StoreEntry(readUTF, date, 2, decodeKey, certificateArr2);
                    break;
                case 3:
                case 4:
                    byte[] bArr = new byte[dataInputStream.readInt()];
                    dataInputStream.readFully(bArr);
                    this.table.put(readUTF, new StoreEntry(readUTF, date, read, bArr, certificateArr2));
                    continue;
                default:
                    throw new IOException("Unknown object type in store.");
            }
            hashtable.put(readUTF, r2);
        }
    }

    protected Cipher makePBECipher(String str, int i, char[] cArr, byte[] bArr, int i2) throws IOException {
        try {
            PBEKeySpec pBEKeySpec = new PBEKeySpec(cArr);
            SecretKeyFactory createSecretKeyFactory = this.helper.createSecretKeyFactory(str);
            PBEParameterSpec pBEParameterSpec = new PBEParameterSpec(bArr, i2);
            Cipher createCipher = this.helper.createCipher(str);
            createCipher.init(i, createSecretKeyFactory.generateSecret(pBEKeySpec), pBEParameterSpec);
            return createCipher;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error initialising store of key store: ");
            stringBuilder.append(e);
            throw new IOException(stringBuilder.toString());
        }
    }

    protected void saveStore(OutputStream outputStream) throws IOException {
        Enumeration elements = this.table.elements();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        while (true) {
            int i = 0;
            if (elements.hasMoreElements()) {
                StoreEntry storeEntry = (StoreEntry) elements.nextElement();
                dataOutputStream.write(storeEntry.getType());
                dataOutputStream.writeUTF(storeEntry.getAlias());
                dataOutputStream.writeLong(storeEntry.getDate().getTime());
                Certificate[] certificateChain = storeEntry.getCertificateChain();
                if (certificateChain == null) {
                    dataOutputStream.writeInt(0);
                } else {
                    dataOutputStream.writeInt(certificateChain.length);
                    while (i != certificateChain.length) {
                        encodeCertificate(certificateChain[i], dataOutputStream);
                        i++;
                    }
                }
                switch (storeEntry.getType()) {
                    case 1:
                        encodeCertificate((Certificate) storeEntry.getObject(), dataOutputStream);
                        break;
                    case 2:
                        encodeKey((Key) storeEntry.getObject(), dataOutputStream);
                        break;
                    case 3:
                    case 4:
                        byte[] bArr = (byte[]) storeEntry.getObject();
                        dataOutputStream.writeInt(bArr.length);
                        dataOutputStream.write(bArr);
                        break;
                    default:
                        throw new IOException("Unknown object type in store.");
                }
            }
            dataOutputStream.write(0);
            return;
        }
    }

    public void setRandom(SecureRandom secureRandom) {
        this.random = secureRandom;
    }
}
