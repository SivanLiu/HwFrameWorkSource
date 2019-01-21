package android.icu.impl.duration.impl;

import android.icu.lang.UCharacter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class XMLRecordReader implements RecordReader {
    private boolean atTag;
    private List<String> nameStack = new ArrayList();
    private Reader r;
    private String tag;

    public XMLRecordReader(Reader r) {
        this.r = r;
        if (getTag().startsWith("?xml")) {
            advance();
        }
        if (getTag().startsWith("!--")) {
            advance();
        }
    }

    public boolean open(String title) {
        if (!getTag().equals(title)) {
            return false;
        }
        this.nameStack.add(title);
        advance();
        return true;
    }

    public boolean close() {
        int ix = this.nameStack.size() - 1;
        String name = (String) this.nameStack.get(ix);
        String tag = getTag();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/");
        stringBuilder.append(name);
        if (!tag.equals(stringBuilder.toString())) {
            return false;
        }
        this.nameStack.remove(ix);
        advance();
        return true;
    }

    public boolean bool(String name) {
        String s = string(name);
        if (s != null) {
            return "true".equals(s);
        }
        return false;
    }

    public boolean[] boolArray(String name) {
        String[] sa = stringArray(name);
        if (sa == null) {
            return null;
        }
        boolean[] result = new boolean[sa.length];
        for (int i = 0; i < sa.length; i++) {
            result[i] = "true".equals(sa[i]);
        }
        return result;
    }

    public char character(String name) {
        String s = string(name);
        if (s != null) {
            return s.charAt(0);
        }
        return 65535;
    }

    public char[] characterArray(String name) {
        String[] sa = stringArray(name);
        if (sa == null) {
            return null;
        }
        char[] result = new char[sa.length];
        for (int i = 0; i < sa.length; i++) {
            result[i] = sa[i].charAt(0);
        }
        return result;
    }

    public byte namedIndex(String name, String[] names) {
        String sa = string(name);
        if (sa != null) {
            for (int i = 0; i < names.length; i++) {
                if (sa.equals(names[i])) {
                    return (byte) i;
                }
            }
        }
        return (byte) -1;
    }

    public byte[] namedIndexArray(String name, String[] names) {
        String[] sa = stringArray(name);
        if (sa == null) {
            return null;
        }
        byte[] result = new byte[sa.length];
        for (int i = 0; i < sa.length; i++) {
            String s = sa[i];
            for (int j = 0; j < names.length; j++) {
                if (names[j].equals(s)) {
                    result[i] = (byte) j;
                    break;
                }
            }
            result[i] = (byte) -1;
        }
        return result;
    }

    public String string(String name) {
        if (match(name)) {
            String result = readData();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/");
            stringBuilder.append(name);
            if (match(stringBuilder.toString())) {
                return result;
            }
        }
        return null;
    }

    public String[] stringArray(String name) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append("List");
        if (match(stringBuilder.toString())) {
            List<String> list = new ArrayList();
            while (true) {
                String string = string(name);
                String s = string;
                if (string == null) {
                    break;
                }
                if ("Null".equals(s)) {
                    s = null;
                }
                list.add(s);
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("/");
            stringBuilder2.append(name);
            stringBuilder2.append("List");
            if (match(stringBuilder2.toString())) {
                return (String[]) list.toArray(new String[list.size()]);
            }
        }
        return null;
    }

    public String[][] stringTable(String name) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append("Table");
        if (match(stringBuilder.toString())) {
            List<String[]> list = new ArrayList();
            while (true) {
                String[] stringArray = stringArray(name);
                String[] sa = stringArray;
                if (stringArray == null) {
                    break;
                }
                list.add(sa);
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("/");
            stringBuilder2.append(name);
            stringBuilder2.append("Table");
            if (match(stringBuilder2.toString())) {
                return (String[][]) list.toArray(new String[list.size()][]);
            }
        }
        return null;
    }

    private boolean match(String target) {
        if (!getTag().equals(target)) {
            return false;
        }
        advance();
        return true;
    }

    private String getTag() {
        if (this.tag == null) {
            this.tag = readNextTag();
        }
        return this.tag;
    }

    private void advance() {
        this.tag = null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:48:0x00f5  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String readData() {
        int c;
        StringBuilder sb = new StringBuilder();
        boolean z = false;
        boolean inWhitespace = false;
        while (true) {
            c = readChar();
            if (c != -1 && c != 60) {
                if (c == 38) {
                    c = readChar();
                    StringBuilder numBuf;
                    if (c == 35) {
                        numBuf = new StringBuilder();
                        int radix = 10;
                        c = readChar();
                        if (c == 120) {
                            radix = 16;
                            c = readChar();
                        }
                        while (c != 59 && c != -1) {
                            numBuf.append((char) c);
                            c = readChar();
                        }
                        try {
                            c = (char) Integer.parseInt(numBuf.toString(), radix);
                        } catch (NumberFormatException ex) {
                            PrintStream printStream = System.err;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("numbuf: ");
                            stringBuilder.append(numBuf.toString());
                            stringBuilder.append(" radix: ");
                            stringBuilder.append(radix);
                            printStream.println(stringBuilder.toString());
                            throw ex;
                        }
                    }
                    numBuf = new StringBuilder();
                    while (c != 59 && c != -1) {
                        numBuf.append((char) c);
                        c = readChar();
                    }
                    String charName = numBuf.toString();
                    if (charName.equals("lt")) {
                        c = 60;
                    } else if (charName.equals("gt")) {
                        c = 62;
                    } else if (charName.equals("quot")) {
                        c = 34;
                    } else if (charName.equals("apos")) {
                        c = 39;
                    } else if (charName.equals("amp")) {
                        c = 38;
                    } else {
                        PrintStream printStream2 = System.err;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("unrecognized character entity: '");
                        stringBuilder2.append(charName);
                        stringBuilder2.append("'");
                        printStream2.println(stringBuilder2.toString());
                    }
                }
                if (!UCharacter.isWhitespace(c)) {
                    inWhitespace = false;
                } else if (!inWhitespace) {
                    c = 32;
                    inWhitespace = true;
                }
                sb.append((char) c);
            } else if (c == 60) {
                z = true;
            }
        }
        if (c == 60) {
        }
        this.atTag = z;
        return sb.toString();
    }

    private String readNextTag() {
        int c = 0;
        while (!this.atTag) {
            c = readChar();
            if (c != 60 && c != -1) {
                if (!UCharacter.isWhitespace(c)) {
                    PrintStream printStream = System.err;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected non-whitespace character ");
                    stringBuilder.append(Integer.toHexString(c));
                    printStream.println(stringBuilder.toString());
                    break;
                }
            } else if (c == 60) {
                this.atTag = true;
            }
        }
        if (!this.atTag) {
            return null;
        }
        this.atTag = false;
        StringBuilder sb = new StringBuilder();
        while (true) {
            c = readChar();
            if (c != 62 && c != -1) {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }

    int readChar() {
        try {
            return this.r.read();
        } catch (IOException e) {
            return -1;
        }
    }
}
