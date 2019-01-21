package sun.security.x509;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import sun.misc.HexDumpEncoder;
import sun.security.util.BitArray;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class IPAddressName implements GeneralNameInterface {
    private static final int MASKSIZE = 16;
    private byte[] address;
    private boolean isIPv4;
    private String name;

    public IPAddressName(DerValue derValue) throws IOException {
        this(derValue.getOctetString());
    }

    public IPAddressName(byte[] address) throws IOException {
        if (address.length == 4 || address.length == 8) {
            this.isIPv4 = true;
        } else if (address.length == 16 || address.length == 32) {
            this.isIPv4 = false;
        } else {
            throw new IOException("Invalid IPAddressName");
        }
        this.address = address;
    }

    public IPAddressName(String name) throws IOException {
        StringBuilder stringBuilder;
        if (name == null || name.length() == 0) {
            throw new IOException("IPAddress cannot be null or empty");
        } else if (name.charAt(name.length() - 1) == '/') {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid IPAddress: ");
            stringBuilder.append(name);
            throw new IOException(stringBuilder.toString());
        } else if (name.indexOf(58) >= 0) {
            parseIPv6(name);
            this.isIPv4 = false;
        } else if (name.indexOf(46) >= 0) {
            parseIPv4(name);
            this.isIPv4 = true;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid IPAddress: ");
            stringBuilder.append(name);
            throw new IOException(stringBuilder.toString());
        }
    }

    private void parseIPv4(String name) throws IOException {
        int slashNdx = name.indexOf(47);
        if (slashNdx == -1) {
            this.address = InetAddress.getByName(name).getAddress();
            return;
        }
        this.address = new byte[8];
        byte[] mask = InetAddress.getByName(name.substring(slashNdx + 1)).getAddress();
        System.arraycopy(InetAddress.getByName(name.substring(0, slashNdx)).getAddress(), 0, this.address, 0, 4);
        System.arraycopy(mask, 0, this.address, 4, 4);
    }

    private void parseIPv6(String name) throws IOException {
        int slashNdx = name.indexOf(47);
        if (slashNdx == -1) {
            this.address = InetAddress.getByName(name).getAddress();
            return;
        }
        this.address = new byte[32];
        int i = 0;
        System.arraycopy(InetAddress.getByName(name.substring(0, slashNdx)).getAddress(), 0, this.address, 0, 16);
        int prefixLen = Integer.parseInt(name.substring(slashNdx + 1));
        if (prefixLen < 0 || prefixLen > 128) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IPv6Address prefix length (");
            stringBuilder.append(prefixLen);
            stringBuilder.append(") in out of valid range [0,128]");
            throw new IOException(stringBuilder.toString());
        }
        BitArray bitArray = new BitArray(128);
        for (int i2 = 0; i2 < prefixLen; i2++) {
            bitArray.set(i2, true);
        }
        byte[] maskArray = bitArray.toByteArray();
        while (i < 16) {
            this.address[16 + i] = maskArray[i];
            i++;
        }
    }

    public int getType() {
        return 7;
    }

    public void encode(DerOutputStream out) throws IOException {
        out.putOctetString(this.address);
    }

    public String toString() {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IPAddress: ");
            stringBuilder.append(getName());
            return stringBuilder.toString();
        } catch (IOException e) {
            HexDumpEncoder enc = new HexDumpEncoder();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("IPAddress: ");
            stringBuilder2.append(enc.encodeBuffer(this.address));
            return stringBuilder2.toString();
        }
    }

    public String getName() throws IOException {
        if (this.name != null) {
            return this.name;
        }
        int i = 0;
        byte[] host;
        byte[] mask;
        if (this.isIPv4) {
            host = new byte[4];
            System.arraycopy(this.address, 0, host, 0, 4);
            this.name = InetAddress.getByAddress(host).getHostAddress();
            if (this.address.length == 8) {
                mask = new byte[4];
                System.arraycopy(this.address, 4, mask, 0, 4);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.name);
                stringBuilder.append("/");
                stringBuilder.append(InetAddress.getByAddress(mask).getHostAddress());
                this.name = stringBuilder.toString();
            }
        } else {
            int i2 = 16;
            host = new byte[16];
            System.arraycopy(this.address, 0, host, 0, 16);
            this.name = InetAddress.getByAddress(host).getHostAddress();
            if (this.address.length == 32) {
                mask = new byte[16];
                while (i2 < 32) {
                    mask[i2 - 16] = this.address[i2];
                    i2++;
                }
                BitArray ba = new BitArray(128, mask);
                while (i < 128 && ba.get(i)) {
                    i++;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.name);
                stringBuilder2.append("/");
                stringBuilder2.append(i);
                this.name = stringBuilder2.toString();
                while (i < 128) {
                    if (ba.get(i)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Invalid IPv6 subdomain - set bit ");
                        stringBuilder2.append(i);
                        stringBuilder2.append(" not contiguous");
                        throw new IOException(stringBuilder2.toString());
                    }
                    i++;
                }
            }
        }
        return this.name;
    }

    public byte[] getBytes() {
        return (byte[]) this.address.clone();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IPAddressName)) {
            return false;
        }
        byte[] other = ((IPAddressName) obj).address;
        if (other.length != this.address.length) {
            return false;
        }
        if (this.address.length != 8 && this.address.length != 32) {
            return Arrays.equals(other, this.address);
        }
        int i;
        int maskLen = this.address.length / 2;
        for (i = 0; i < maskLen; i++) {
            if (((byte) (this.address[i] & this.address[i + maskLen])) != ((byte) (other[i] & other[i + maskLen]))) {
                return false;
            }
        }
        for (i = maskLen; i < this.address.length; i++) {
            if (this.address[i] != other[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int retval = 0;
        for (int i = 0; i < this.address.length; i++) {
            retval += this.address[i] * i;
        }
        return retval;
    }

    public int constrains(GeneralNameInterface inputName) throws UnsupportedOperationException {
        if (inputName == null) {
            return -1;
        }
        if (inputName.getType() != 7) {
            return -1;
        }
        if (((IPAddressName) inputName).equals(this)) {
            return 0;
        }
        byte[] otherAddress = ((IPAddressName) inputName).address;
        if (otherAddress.length == 4 && this.address.length == 4) {
            return 3;
        }
        int i;
        int maskOffset;
        if ((otherAddress.length == 8 && this.address.length == 8) || (otherAddress.length == 32 && this.address.length == 32)) {
            boolean otherSubsetOfThis = true;
            boolean thisSubsetOfOther = true;
            boolean thisEmpty = false;
            boolean otherEmpty = false;
            int maskOffset2 = this.address.length / 2;
            int i2 = 0;
            while (i2 < maskOffset2) {
                if (((byte) (this.address[i2] & this.address[i2 + maskOffset2])) != this.address[i2]) {
                    thisEmpty = true;
                }
                if (((byte) (otherAddress[i2] & otherAddress[i2 + maskOffset2])) != otherAddress[i2]) {
                    otherEmpty = true;
                }
                if (!(((byte) (this.address[i2 + maskOffset2] & otherAddress[i2 + maskOffset2])) == this.address[i2 + maskOffset2] && ((byte) (this.address[i2] & this.address[i2 + maskOffset2])) == ((byte) (otherAddress[i2] & this.address[i2 + maskOffset2])))) {
                    otherSubsetOfThis = false;
                }
                if (((byte) (otherAddress[i2 + maskOffset2] & this.address[i2 + maskOffset2])) != otherAddress[i2 + maskOffset2] || ((byte) (otherAddress[i2] & otherAddress[i2 + maskOffset2])) != ((byte) (this.address[i2] & otherAddress[i2 + maskOffset2]))) {
                    thisSubsetOfOther = false;
                }
                i2++;
            }
            if (thisEmpty || otherEmpty) {
                if (thisEmpty && otherEmpty) {
                    return 0;
                }
                if (thisEmpty) {
                    return 2;
                }
                return 1;
            } else if (otherSubsetOfThis) {
                return 1;
            } else {
                if (thisSubsetOfOther) {
                    return 2;
                }
                return 3;
            }
        } else if (otherAddress.length == 8 || otherAddress.length == 32) {
            i = 0;
            maskOffset = otherAddress.length / 2;
            while (i < maskOffset && (this.address[i] & otherAddress[i + maskOffset]) == otherAddress[i]) {
                i++;
            }
            if (i == maskOffset) {
                return 2;
            }
            return 3;
        } else if (this.address.length != 8 && this.address.length != 32) {
            return 3;
        } else {
            i = 0;
            maskOffset = this.address.length / 2;
            while (i < maskOffset && (otherAddress[i] & this.address[i + maskOffset]) == this.address[i]) {
                i++;
            }
            if (i == maskOffset) {
                return 1;
            }
            return 3;
        }
    }

    public int subtreeDepth() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("subtreeDepth() not defined for IPAddressName");
    }
}
