package sun.security.pkcs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyRep;
import java.security.KeyRep.Type;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import sun.security.util.Debug;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.x509.AlgorithmId;

public class PKCS8Key implements PrivateKey {
    private static final long serialVersionUID = -3836890099307167124L;
    public static final BigInteger version = BigInteger.ZERO;
    protected AlgorithmId algid;
    protected byte[] encodedKey;
    protected byte[] key;

    private PKCS8Key(AlgorithmId algid, byte[] key) throws InvalidKeyException {
        this.algid = algid;
        this.key = key;
        encode();
    }

    public static PKCS8Key parse(DerValue in) throws IOException {
        PrivateKey key = parseKey(in);
        if (key instanceof PKCS8Key) {
            return (PKCS8Key) key;
        }
        throw new IOException("Provider did not return PKCS8Key");
    }

    public static PrivateKey parseKey(DerValue in) throws IOException {
        if (in.tag == (byte) 48) {
            BigInteger parsedVersion = in.data.getBigInteger();
            if (version.equals(parsedVersion)) {
                try {
                    PrivateKey privKey = buildPKCS8Key(AlgorithmId.parse(in.data.getDerValue()), in.data.getOctetString());
                    if (in.data.available() == 0) {
                        return privKey;
                    }
                    throw new IOException("excess private key");
                } catch (InvalidKeyException e) {
                    throw new IOException("corrupt private key");
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("version mismatch: (supported: ");
            stringBuilder.append(Debug.toHexString(version));
            stringBuilder.append(", parsed: ");
            stringBuilder.append(Debug.toHexString(parsedVersion));
            throw new IOException(stringBuilder.toString());
        }
        throw new IOException("corrupt private key");
    }

    protected void parseKeyBits() throws IOException, InvalidKeyException {
        encode();
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x0096 A:{Splitter:B:6:0x0024, ExcHandler: InstantiationException (e java.lang.InstantiationException)} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x007e A:{Splitter:B:6:0x0024, PHI: r2 , ExcHandler: IllegalAccessException (e java.lang.IllegalAccessException)} */
    /* JADX WARNING: Missing block: B:33:0x007f, code skipped:
            r5 = new java.lang.StringBuilder();
            r5.append(r2);
            r5.append(" [internal error]");
     */
    /* JADX WARNING: Missing block: B:34:0x0095, code skipped:
            throw new java.io.IOException(r5.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static PrivateKey buildPKCS8Key(AlgorithmId algid, byte[] key) throws IOException, InvalidKeyException {
        DerOutputStream pkcs8EncodedKeyStream = new DerOutputStream();
        encode(pkcs8EncodedKeyStream, algid, key);
        try {
            return KeyFactory.getInstance(algid.getName()).generatePrivate(new PKCS8EncodedKeySpec(pkcs8EncodedKeyStream.toByteArray()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            String classname = "";
            Class<?> keyClass;
            try {
                Provider sunProvider = Security.getProvider("SUN");
                if (sunProvider != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PrivateKey.PKCS#8.");
                    stringBuilder.append(algid.getName());
                    classname = sunProvider.getProperty(stringBuilder.toString());
                    if (classname != null) {
                        keyClass = null;
                        keyClass = Class.forName(classname);
                        PKCS8Key inst = null;
                        if (keyClass != null) {
                            inst = keyClass.newInstance();
                        }
                        if (inst instanceof PKCS8Key) {
                            PKCS8Key result = inst;
                            result.algid = algid;
                            result.key = key;
                            result.parseKeyBits();
                            return result;
                        }
                    }
                    throw new InstantiationException();
                }
                throw new InstantiationException();
            } catch (ClassNotFoundException e2) {
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                if (cl != null) {
                    keyClass = cl.loadClass(classname);
                }
            } catch (InstantiationException e3) {
            } catch (IllegalAccessException e4) {
            }
        } catch (ClassNotFoundException e5) {
        } catch (InstantiationException e32) {
        } catch (IllegalAccessException e42) {
        }
        PKCS8Key result2 = new PKCS8Key();
        result2.algid = algid;
        result2.key = key;
        return result2;
    }

    public String getAlgorithm() {
        return this.algid.getName();
    }

    public AlgorithmId getAlgorithmId() {
        return this.algid;
    }

    public final void encode(DerOutputStream out) throws IOException {
        encode(out, this.algid, this.key);
    }

    public synchronized byte[] getEncoded() {
        byte[] result;
        result = null;
        try {
            result = encode();
        } catch (InvalidKeyException e) {
        }
        return result;
    }

    public String getFormat() {
        return "PKCS#8";
    }

    public byte[] encode() throws InvalidKeyException {
        if (this.encodedKey == null) {
            try {
                DerOutputStream out = new DerOutputStream();
                encode(out);
                this.encodedKey = out.toByteArray();
            } catch (IOException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IOException : ");
                stringBuilder.append(e.getMessage());
                throw new InvalidKeyException(stringBuilder.toString());
            }
        }
        return (byte[]) this.encodedKey.clone();
    }

    public void decode(InputStream in) throws InvalidKeyException {
        try {
            DerValue val = new DerValue(in);
            if (val.tag == (byte) 48) {
                BigInteger version = val.data.getBigInteger();
                if (version.equals(version)) {
                    this.algid = AlgorithmId.parse(val.data.getDerValue());
                    this.key = val.data.getOctetString();
                    parseKeyBits();
                    val.data.available();
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("version mismatch: (supported: ");
                stringBuilder.append(Debug.toHexString(version));
                stringBuilder.append(", parsed: ");
                stringBuilder.append(Debug.toHexString(version));
                throw new IOException(stringBuilder.toString());
            }
            throw new InvalidKeyException("invalid key format");
        } catch (IOException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("IOException : ");
            stringBuilder2.append(e.getMessage());
            throw new InvalidKeyException(stringBuilder2.toString());
        }
    }

    public void decode(byte[] encodedKey) throws InvalidKeyException {
        decode(new ByteArrayInputStream(encodedKey));
    }

    protected Object writeReplace() throws ObjectStreamException {
        return new KeyRep(Type.PRIVATE, getAlgorithm(), getFormat(), getEncoded());
    }

    private void readObject(ObjectInputStream stream) throws IOException {
        try {
            decode((InputStream) stream);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deserialized key is invalid: ");
            stringBuilder.append(e.getMessage());
            throw new IOException(stringBuilder.toString());
        }
    }

    static void encode(DerOutputStream out, AlgorithmId algid, byte[] key) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        tmp.putInteger(version);
        algid.encode(tmp);
        tmp.putOctetString(key);
        out.write((byte) 48, tmp);
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Key)) {
            return false;
        }
        byte[] b1;
        if (this.encodedKey != null) {
            b1 = this.encodedKey;
        } else {
            b1 = getEncoded();
        }
        byte[] b2 = ((Key) object).getEncoded();
        if (b1.length != b2.length) {
            return false;
        }
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int retval = 0;
        byte[] b1 = getEncoded();
        for (int i = 1; i < b1.length; i++) {
            retval += b1[i] * i;
        }
        return retval;
    }
}
