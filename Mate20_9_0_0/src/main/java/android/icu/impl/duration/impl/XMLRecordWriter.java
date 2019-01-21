package android.icu.impl.duration.impl;

import android.icu.lang.UCharacter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class XMLRecordWriter implements RecordWriter {
    private static final String INDENT = "    ";
    static final String NULL_NAME = "Null";
    private List<String> nameStack = new ArrayList();
    private Writer w;

    public XMLRecordWriter(Writer w) {
        this.w = w;
    }

    public boolean open(String title) {
        newline();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<");
        stringBuilder.append(title);
        stringBuilder.append(">");
        writeString(stringBuilder.toString());
        this.nameStack.add(title);
        return true;
    }

    public boolean close() {
        int ix = this.nameStack.size() - 1;
        if (ix < 0) {
            return false;
        }
        String name = (String) this.nameStack.remove(ix);
        newline();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("</");
        stringBuilder.append(name);
        stringBuilder.append(">");
        writeString(stringBuilder.toString());
        return true;
    }

    public void flush() {
        try {
            this.w.flush();
        } catch (IOException e) {
        }
    }

    public void bool(String name, boolean value) {
        internalString(name, String.valueOf(value));
    }

    public void boolArray(String name, boolean[] values) {
        if (values != null) {
            String[] stringValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                stringValues[i] = String.valueOf(values[i]);
            }
            stringArray(name, stringValues);
        }
    }

    private static String ctos(char value) {
        if (value == '<') {
            return "&lt;";
        }
        if (value == '&') {
            return "&amp;";
        }
        return String.valueOf(value);
    }

    public void character(String name, char value) {
        if (value != 65535) {
            internalString(name, ctos(value));
        }
    }

    public void characterArray(String name, char[] values) {
        if (values != null) {
            String[] stringValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                char value = values[i];
                if (value == 65535) {
                    stringValues[i] = NULL_NAME;
                } else {
                    stringValues[i] = ctos(value);
                }
            }
            internalStringArray(name, stringValues);
        }
    }

    public void namedIndex(String name, String[] names, int value) {
        if (value >= 0) {
            internalString(name, names[value]);
        }
    }

    public void namedIndexArray(String name, String[] names, byte[] values) {
        if (values != null) {
            String[] stringValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                int value = values[i];
                if (value < 0) {
                    stringValues[i] = NULL_NAME;
                } else {
                    stringValues[i] = names[value];
                }
            }
            internalStringArray(name, stringValues);
        }
    }

    public static String normalize(String str) {
        if (str == null) {
            return null;
        }
        boolean special = false;
        boolean inWhitespace = false;
        StringBuilder sb = null;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (UCharacter.isWhitespace(c)) {
                if (sb == null && (inWhitespace || c != ' ')) {
                    sb = new StringBuilder(str.substring(0, i));
                }
                if (inWhitespace) {
                } else {
                    inWhitespace = true;
                    special = false;
                    c = ' ';
                }
            } else {
                inWhitespace = false;
                boolean z = c == '<' || c == '&';
                special = z;
                if (special && sb == null) {
                    sb = new StringBuilder(str.substring(0, i));
                }
            }
            if (sb != null) {
                if (special) {
                    sb.append(c == '<' ? "&lt;" : "&amp;");
                } else {
                    sb.append(c);
                }
            }
        }
        if (sb != null) {
            return sb.toString();
        }
        return str;
    }

    private void internalString(String name, String normalizedValue) {
        if (normalizedValue != null) {
            newline();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<");
            stringBuilder.append(name);
            stringBuilder.append(">");
            stringBuilder.append(normalizedValue);
            stringBuilder.append("</");
            stringBuilder.append(name);
            stringBuilder.append(">");
            writeString(stringBuilder.toString());
        }
    }

    private void internalStringArray(String name, String[] normalizedValues) {
        if (normalizedValues != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append("List");
            push(stringBuilder.toString());
            for (String value : normalizedValues) {
                String value2;
                if (value2 == null) {
                    value2 = NULL_NAME;
                }
                string(name, value2);
            }
            pop();
        }
    }

    public void string(String name, String value) {
        internalString(name, normalize(value));
    }

    public void stringArray(String name, String[] values) {
        if (values != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append("List");
            push(stringBuilder.toString());
            for (String value : values) {
                String value2 = normalize(value2);
                if (value2 == null) {
                    value2 = NULL_NAME;
                }
                internalString(name, value2);
            }
            pop();
        }
    }

    public void stringTable(String name, String[][] values) {
        if (values != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append("Table");
            push(stringBuilder.toString());
            for (String[] rowValues : values) {
                if (rowValues == null) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(name);
                    stringBuilder2.append("List");
                    internalString(stringBuilder2.toString(), NULL_NAME);
                } else {
                    stringArray(name, rowValues);
                }
            }
            pop();
        }
    }

    private void push(String name) {
        newline();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<");
        stringBuilder.append(name);
        stringBuilder.append(">");
        writeString(stringBuilder.toString());
        this.nameStack.add(name);
    }

    private void pop() {
        String name = (String) this.nameStack.remove(this.nameStack.size() - 1);
        newline();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("</");
        stringBuilder.append(name);
        stringBuilder.append(">");
        writeString(stringBuilder.toString());
    }

    private void newline() {
        writeString("\n");
        for (int i = 0; i < this.nameStack.size(); i++) {
            writeString(INDENT);
        }
    }

    private void writeString(String str) {
        if (this.w != null) {
            try {
                this.w.write(str);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                this.w = null;
            }
        }
    }
}
