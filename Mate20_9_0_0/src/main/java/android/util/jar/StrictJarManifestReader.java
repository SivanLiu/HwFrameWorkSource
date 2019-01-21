package android.util.jar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;

class StrictJarManifestReader {
    private final HashMap<String, Name> attributeNameCache = new HashMap();
    private final byte[] buf;
    private int consecutiveLineBreaks = 0;
    private final int endOfMainSection;
    private Name name;
    private int pos;
    private String value;
    private final ByteArrayOutputStream valueBuffer = new ByteArrayOutputStream(80);

    public StrictJarManifestReader(byte[] buf, Attributes main) throws IOException {
        this.buf = buf;
        while (readHeader()) {
            main.put(this.name, this.value);
        }
        this.endOfMainSection = this.pos;
    }

    public void readEntries(Map<String, Attributes> entries, Map<String, Chunk> chunks) throws IOException {
        int mark = this.pos;
        while (readHeader()) {
            if (Name.NAME.equals(this.name)) {
                String entryNameValue = this.value;
                Attributes entry = (Attributes) entries.get(entryNameValue);
                if (entry == null) {
                    entry = new Attributes(12);
                }
                while (readHeader()) {
                    entry.put(this.name, this.value);
                }
                if (chunks != null) {
                    if (chunks.get(entryNameValue) == null) {
                        chunks.put(entryNameValue, new Chunk(mark, this.pos));
                        mark = this.pos;
                    } else {
                        throw new IOException("A jar verifier does not support more than one entry with the same name");
                    }
                }
                entries.put(entryNameValue, entry);
            } else {
                throw new IOException("Entry is not named");
            }
        }
    }

    public int getEndOfMainSection() {
        return this.endOfMainSection;
    }

    private boolean readHeader() throws IOException {
        boolean z = true;
        if (this.consecutiveLineBreaks > 1) {
            this.consecutiveLineBreaks = 0;
            return false;
        }
        readName();
        this.consecutiveLineBreaks = 0;
        readValue();
        if (this.consecutiveLineBreaks <= 0) {
            z = false;
        }
        return z;
    }

    private void readName() throws IOException {
        int mark = this.pos;
        while (this.pos < this.buf.length) {
            byte[] bArr = this.buf;
            int i = this.pos;
            this.pos = i + 1;
            if (bArr[i] == (byte) 58) {
                String nameString = new String(this.buf, mark, (this.pos - mark) - 1, StandardCharsets.US_ASCII);
                byte[] bArr2 = this.buf;
                int i2 = this.pos;
                this.pos = i2 + 1;
                if (bArr2[i2] == (byte) 32) {
                    try {
                        this.name = (Name) this.attributeNameCache.get(nameString);
                        if (this.name == null) {
                            this.name = new Name(nameString);
                            this.attributeNameCache.put(nameString, this.name);
                        }
                        return;
                    } catch (IllegalArgumentException e) {
                        throw new IOException(e.getMessage());
                    }
                }
                throw new IOException(String.format("Invalid value for attribute '%s'", new Object[]{nameString}));
            }
        }
    }

    private void readValue() throws IOException {
        boolean lastCr = false;
        int mark = this.pos;
        int last = this.pos;
        this.valueBuffer.reset();
        while (this.pos < this.buf.length) {
            byte next = this.buf;
            int i = this.pos;
            this.pos = i + 1;
            next = next[i];
            if (next == (byte) 0) {
                throw new IOException("NUL character in a manifest");
            } else if (next != (byte) 10) {
                if (next == (byte) 13) {
                    lastCr = true;
                    this.consecutiveLineBreaks++;
                } else if (next == (byte) 32 && this.consecutiveLineBreaks == 1) {
                    this.valueBuffer.write(this.buf, mark, last - mark);
                    mark = this.pos;
                    this.consecutiveLineBreaks = 0;
                } else if (this.consecutiveLineBreaks >= 1) {
                    this.pos--;
                    break;
                } else {
                    last = this.pos;
                }
            } else if (lastCr) {
                lastCr = false;
            } else {
                this.consecutiveLineBreaks++;
            }
        }
        this.valueBuffer.write(this.buf, mark, last - mark);
        this.value = this.valueBuffer.toString(StandardCharsets.UTF_8.name());
    }
}
