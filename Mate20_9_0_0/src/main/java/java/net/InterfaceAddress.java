package java.net;

public class InterfaceAddress {
    private InetAddress address = null;
    private Inet4Address broadcast = null;
    private short maskLength = (short) 0;

    InterfaceAddress() {
    }

    InterfaceAddress(InetAddress address, Inet4Address broadcast, InetAddress netmask) {
        this.address = address;
        this.broadcast = broadcast;
        this.maskLength = countPrefixLength(netmask);
    }

    private short countPrefixLength(InetAddress netmask) {
        short count = (short) 0;
        for (byte b : netmask.getAddress()) {
            byte b2;
            while (b2 != (byte) 0) {
                b2 = (byte) (b2 << 1);
                count = (short) (count + 1);
            }
        }
        return count;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public InetAddress getBroadcast() {
        return this.broadcast;
    }

    public short getNetworkPrefixLength() {
        return this.maskLength;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof InterfaceAddress)) {
            return false;
        }
        InterfaceAddress cmp = (InterfaceAddress) obj;
        if (!this.address != null ? cmp.address == null : this.address.equals(cmp.address)) {
            return false;
        }
        if (!this.broadcast != null ? cmp.broadcast == null : this.broadcast.equals(cmp.broadcast)) {
            return false;
        }
        if (this.maskLength != cmp.maskLength) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return (this.address.hashCode() + (this.broadcast != null ? this.broadcast.hashCode() : 0)) + this.maskLength;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.address);
        stringBuilder.append("/");
        stringBuilder.append(this.maskLength);
        stringBuilder.append(" [");
        stringBuilder.append(this.broadcast);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
