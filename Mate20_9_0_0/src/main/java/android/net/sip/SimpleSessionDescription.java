package android.net.sip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

public class SimpleSessionDescription {
    private final Fields mFields = new Fields("voscbtka");
    private final ArrayList<Media> mMedia = new ArrayList();

    private static class Fields {
        private final ArrayList<String> mLines = new ArrayList();
        private final String mOrder;

        Fields(String order) {
            this.mOrder = order;
        }

        public String getAddress() {
            String address = get("c", '=');
            if (address == null) {
                return null;
            }
            String[] parts = address.split(" ");
            if (parts.length != 3) {
                return null;
            }
            int slash = parts[2].indexOf(47);
            return slash < 0 ? parts[2] : parts[2].substring(0, slash);
        }

        public void setAddress(String address) {
            if (address != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(address.indexOf(58) < 0 ? "IN IP4 " : "IN IP6 ");
                stringBuilder.append(address);
                address = stringBuilder.toString();
            }
            set("c", '=', address);
        }

        public String getEncryptionMethod() {
            String encryption = get("k", '=');
            if (encryption == null) {
                return null;
            }
            int colon = encryption.indexOf(58);
            return colon == -1 ? encryption : encryption.substring(0, colon);
        }

        public String getEncryptionKey() {
            String encryption = get("k", '=');
            String str = null;
            if (encryption == null) {
                return null;
            }
            int colon = encryption.indexOf(58);
            if (colon != -1) {
                str = encryption.substring(0, colon + 1);
            }
            return str;
        }

        public void setEncryption(String method, String key) {
            String str;
            String str2 = "k";
            if (method == null || key == null) {
                str = method;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(method);
                stringBuilder.append(':');
                stringBuilder.append(key);
                str = stringBuilder.toString();
            }
            set(str2, '=', str);
        }

        public String[] getBandwidthTypes() {
            return cut("b=", ':');
        }

        public int getBandwidth(String type) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("b=");
            stringBuilder.append(type);
            String value = get(stringBuilder.toString(), ':');
            if (value != null) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    setBandwidth(type, -1);
                }
            }
            return -1;
        }

        public void setBandwidth(String type, int value) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("b=");
            stringBuilder.append(type);
            set(stringBuilder.toString(), ':', value < 0 ? null : String.valueOf(value));
        }

        public String[] getAttributeNames() {
            return cut("a=", ':');
        }

        public String getAttribute(String name) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("a=");
            stringBuilder.append(name);
            return get(stringBuilder.toString(), ':');
        }

        public void setAttribute(String name, String value) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("a=");
            stringBuilder.append(name);
            set(stringBuilder.toString(), ':', value);
        }

        private void write(StringBuilder buffer) {
            for (int i = 0; i < this.mOrder.length(); i++) {
                char type = this.mOrder.charAt(i);
                Iterator it = this.mLines.iterator();
                while (it.hasNext()) {
                    String line = (String) it.next();
                    if (line.charAt(0) == type) {
                        buffer.append(line);
                        buffer.append("\r\n");
                    }
                }
            }
        }

        private void parse(String line) {
            char type = line.charAt(0);
            if (this.mOrder.indexOf(type) != -1) {
                char delimiter = '=';
                if (line.startsWith("a=rtpmap:") || line.startsWith("a=fmtp:")) {
                    delimiter = ' ';
                } else if (type == 'b' || type == 'a') {
                    delimiter = ':';
                }
                int i = line.indexOf(delimiter);
                if (i == -1) {
                    set(line, delimiter, "");
                } else {
                    set(line.substring(0, i), delimiter, line.substring(i + 1));
                }
            }
        }

        private String[] cut(String prefix, char delimiter) {
            String[] names = new String[this.mLines.size()];
            int length = 0;
            Iterator it = this.mLines.iterator();
            while (it.hasNext()) {
                String line = (String) it.next();
                if (line.startsWith(prefix)) {
                    int i = line.indexOf(delimiter);
                    if (i == -1) {
                        i = line.length();
                    }
                    names[length] = line.substring(prefix.length(), i);
                    length++;
                }
            }
            return (String[]) Arrays.copyOf(names, length);
        }

        private int find(String key, char delimiter) {
            int length = key.length();
            for (int i = this.mLines.size() - 1; i >= 0; i--) {
                String line = (String) this.mLines.get(i);
                if (line.startsWith(key) && (line.length() == length || line.charAt(length) == delimiter)) {
                    return i;
                }
            }
            return -1;
        }

        private void set(String key, char delimiter, String value) {
            int index = find(key, delimiter);
            if (value != null) {
                if (value.length() != 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(key);
                    stringBuilder.append(delimiter);
                    stringBuilder.append(value);
                    key = stringBuilder.toString();
                }
                if (index == -1) {
                    this.mLines.add(key);
                } else {
                    this.mLines.set(index, key);
                }
            } else if (index != -1) {
                this.mLines.remove(index);
            }
        }

        private String get(String key, char delimiter) {
            int index = find(key, delimiter);
            if (index == -1) {
                return null;
            }
            String line = (String) this.mLines.get(index);
            int length = key.length();
            return line.length() == length ? "" : line.substring(length + 1);
        }
    }

    public static class Media extends Fields {
        private ArrayList<String> mFormats;
        private final int mPort;
        private final int mPortCount;
        private final String mProtocol;
        private final String mType;

        public /* bridge */ /* synthetic */ String getAddress() {
            return super.getAddress();
        }

        public /* bridge */ /* synthetic */ String getAttribute(String str) {
            return super.getAttribute(str);
        }

        public /* bridge */ /* synthetic */ String[] getAttributeNames() {
            return super.getAttributeNames();
        }

        public /* bridge */ /* synthetic */ int getBandwidth(String str) {
            return super.getBandwidth(str);
        }

        public /* bridge */ /* synthetic */ String[] getBandwidthTypes() {
            return super.getBandwidthTypes();
        }

        public /* bridge */ /* synthetic */ String getEncryptionKey() {
            return super.getEncryptionKey();
        }

        public /* bridge */ /* synthetic */ String getEncryptionMethod() {
            return super.getEncryptionMethod();
        }

        public /* bridge */ /* synthetic */ void setAddress(String str) {
            super.setAddress(str);
        }

        public /* bridge */ /* synthetic */ void setAttribute(String str, String str2) {
            super.setAttribute(str, str2);
        }

        public /* bridge */ /* synthetic */ void setBandwidth(String str, int i) {
            super.setBandwidth(str, i);
        }

        public /* bridge */ /* synthetic */ void setEncryption(String str, String str2) {
            super.setEncryption(str, str2);
        }

        private Media(String type, int port, int portCount, String protocol) {
            super("icbka");
            this.mFormats = new ArrayList();
            this.mType = type;
            this.mPort = port;
            this.mPortCount = portCount;
            this.mProtocol = protocol;
        }

        public String getType() {
            return this.mType;
        }

        public int getPort() {
            return this.mPort;
        }

        public int getPortCount() {
            return this.mPortCount;
        }

        public String getProtocol() {
            return this.mProtocol;
        }

        public String[] getFormats() {
            return (String[]) this.mFormats.toArray(new String[this.mFormats.size()]);
        }

        public String getFmtp(String format) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("a=fmtp:");
            stringBuilder.append(format);
            return get(stringBuilder.toString(), ' ');
        }

        public void setFormat(String format, String fmtp) {
            this.mFormats.remove(format);
            this.mFormats.add(format);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("a=rtpmap:");
            stringBuilder.append(format);
            set(stringBuilder.toString(), ' ', null);
            stringBuilder = new StringBuilder();
            stringBuilder.append("a=fmtp:");
            stringBuilder.append(format);
            set(stringBuilder.toString(), ' ', fmtp);
        }

        public void removeFormat(String format) {
            this.mFormats.remove(format);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("a=rtpmap:");
            stringBuilder.append(format);
            set(stringBuilder.toString(), ' ', null);
            stringBuilder = new StringBuilder();
            stringBuilder.append("a=fmtp:");
            stringBuilder.append(format);
            set(stringBuilder.toString(), ' ', null);
        }

        public int[] getRtpPayloadTypes() {
            int[] types = new int[this.mFormats.size()];
            int length = 0;
            Iterator it = this.mFormats.iterator();
            while (it.hasNext()) {
                try {
                    types[length] = Integer.parseInt((String) it.next());
                    length++;
                } catch (NumberFormatException e) {
                }
            }
            return Arrays.copyOf(types, length);
        }

        public String getRtpmap(int type) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("a=rtpmap:");
            stringBuilder.append(type);
            return get(stringBuilder.toString(), ' ');
        }

        public String getFmtp(int type) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("a=fmtp:");
            stringBuilder.append(type);
            return get(stringBuilder.toString(), ' ');
        }

        public void setRtpPayload(int type, String rtpmap, String fmtp) {
            String format = String.valueOf(type);
            this.mFormats.remove(format);
            this.mFormats.add(format);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("a=rtpmap:");
            stringBuilder.append(format);
            set(stringBuilder.toString(), ' ', rtpmap);
            stringBuilder = new StringBuilder();
            stringBuilder.append("a=fmtp:");
            stringBuilder.append(format);
            set(stringBuilder.toString(), ' ', fmtp);
        }

        public void removeRtpPayload(int type) {
            removeFormat(String.valueOf(type));
        }

        private void write(StringBuilder buffer) {
            buffer.append("m=");
            buffer.append(this.mType);
            buffer.append(' ');
            buffer.append(this.mPort);
            if (this.mPortCount != 1) {
                buffer.append('/');
                buffer.append(this.mPortCount);
            }
            buffer.append(' ');
            buffer.append(this.mProtocol);
            Iterator it = this.mFormats.iterator();
            while (it.hasNext()) {
                String format = (String) it.next();
                buffer.append(' ');
                buffer.append(format);
            }
            buffer.append("\r\n");
            write(buffer);
        }
    }

    public SimpleSessionDescription(long sessionId, String address) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(address.indexOf(58) < 0 ? "IN IP4 " : "IN IP6 ");
        stringBuilder.append(address);
        address = stringBuilder.toString();
        this.mFields.parse("v=0");
        this.mFields.parse(String.format(Locale.US, "o=- %d %d %s", new Object[]{Long.valueOf(sessionId), Long.valueOf(System.currentTimeMillis()), address}));
        this.mFields.parse("s=-");
        this.mFields.parse("t=0 0");
        Fields fields = this.mFields;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("c=");
        stringBuilder2.append(address);
        fields.parse(stringBuilder2.toString());
    }

    public SimpleSessionDescription(String message) {
        String[] lines = message.trim().replaceAll(" +", " ").split("[\r\n]+");
        Fields fields = this.mFields;
        int length = lines.length;
        Fields fields2 = fields;
        int fields3 = 0;
        while (fields3 < length) {
            String line = lines[fields3];
            Media media = true;
            try {
                if (line.charAt(1) == '=') {
                    if (line.charAt(0) == 'm') {
                        String[] parts = line.substring(2).split(" ", 4);
                        String[] ports = parts[1].split("/", 2);
                        String str = parts[0];
                        int parseInt = Integer.parseInt(ports[0]);
                        if (ports.length >= 2) {
                            media = Integer.parseInt(ports[1]);
                        }
                        Fields media2 = newMedia(str, parseInt, media, parts[2]);
                        for (String format : parts[3].split(" ")) {
                            media2.setFormat(format, null);
                        }
                        fields2 = media2;
                    } else {
                        fields2.parse(line);
                    }
                    fields3++;
                } else {
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid SDP: ");
                stringBuilder.append(line);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    public Media newMedia(String type, int port, int portCount, String protocol) {
        Media media = new Media(type, port, portCount, protocol);
        this.mMedia.add(media);
        return media;
    }

    public Media[] getMedia() {
        return (Media[]) this.mMedia.toArray(new Media[this.mMedia.size()]);
    }

    public String encode() {
        StringBuilder buffer = new StringBuilder();
        this.mFields.write(buffer);
        Iterator it = this.mMedia.iterator();
        while (it.hasNext()) {
            ((Media) it.next()).write(buffer);
        }
        return buffer.toString();
    }

    public String getAddress() {
        return this.mFields.getAddress();
    }

    public void setAddress(String address) {
        this.mFields.setAddress(address);
    }

    public String getEncryptionMethod() {
        return this.mFields.getEncryptionMethod();
    }

    public String getEncryptionKey() {
        return this.mFields.getEncryptionKey();
    }

    public void setEncryption(String method, String key) {
        this.mFields.setEncryption(method, key);
    }

    public String[] getBandwidthTypes() {
        return this.mFields.getBandwidthTypes();
    }

    public int getBandwidth(String type) {
        return this.mFields.getBandwidth(type);
    }

    public void setBandwidth(String type, int value) {
        this.mFields.setBandwidth(type, value);
    }

    public String[] getAttributeNames() {
        return this.mFields.getAttributeNames();
    }

    public String getAttribute(String name) {
        return this.mFields.getAttribute(name);
    }

    public void setAttribute(String name, String value) {
        this.mFields.setAttribute(name, value);
    }
}
