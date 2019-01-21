package org.kxml2.io;

import android.icu.impl.PatternTokenizer;
import android.icu.util.ULocale;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import libcore.internal.StringPool;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class KXmlParser implements XmlPullParser, Closeable {
    private static final char[] ANY = new char[]{'A', 'N', 'Y'};
    private static final int ATTLISTDECL = 13;
    private static final char[] COMMENT_DOUBLE_DASH = new char[]{'-', '-'};
    private static final Map<String, String> DEFAULT_ENTITIES = new HashMap();
    private static final char[] DOUBLE_QUOTE = new char[]{'\"'};
    private static final int ELEMENTDECL = 11;
    private static final char[] EMPTY = new char[]{'E', 'M', 'P', 'T', 'Y'};
    private static final char[] END_CDATA = new char[]{']', ']', '>'};
    private static final char[] END_COMMENT = new char[]{'-', '-', '>'};
    private static final char[] END_PROCESSING_INSTRUCTION = new char[]{'?', '>'};
    private static final int ENTITYDECL = 12;
    private static final String FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed";
    private static final char[] FIXED = new char[]{'F', 'I', 'X', 'E', 'D'};
    private static final String ILLEGAL_TYPE = "Wrong event type";
    private static final char[] IMPLIED = new char[]{'I', 'M', 'P', 'L', 'I', 'E', 'D'};
    private static final char[] NDATA = new char[]{'N', 'D', 'A', 'T', 'A'};
    private static final char[] NOTATION = new char[]{'N', 'O', 'T', 'A', 'T', 'I', 'O', 'N'};
    private static final int NOTATIONDECL = 14;
    private static final int PARAMETER_ENTITY_REF = 15;
    private static final String PROPERTY_LOCATION = "http://xmlpull.org/v1/doc/properties.html#location";
    private static final String PROPERTY_XMLDECL_STANDALONE = "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone";
    private static final String PROPERTY_XMLDECL_VERSION = "http://xmlpull.org/v1/doc/properties.html#xmldecl-version";
    private static final char[] PUBLIC = new char[]{'P', 'U', 'B', 'L', 'I', 'C'};
    private static final char[] REQUIRED = new char[]{'R', 'E', 'Q', 'U', 'I', 'R', 'E', 'D'};
    private static final char[] SINGLE_QUOTE = new char[]{PatternTokenizer.SINGLE_QUOTE};
    private static final char[] START_ATTLIST = new char[]{'<', '!', 'A', 'T', 'T', 'L', 'I', 'S', 'T'};
    private static final char[] START_CDATA = new char[]{'<', '!', '[', 'C', 'D', 'A', 'T', 'A', '['};
    private static final char[] START_COMMENT = new char[]{'<', '!', '-', '-'};
    private static final char[] START_DOCTYPE = new char[]{'<', '!', 'D', 'O', 'C', 'T', 'Y', 'P', 'E'};
    private static final char[] START_ELEMENT = new char[]{'<', '!', 'E', 'L', 'E', 'M', 'E', 'N', 'T'};
    private static final char[] START_ENTITY = new char[]{'<', '!', 'E', 'N', 'T', 'I', 'T', 'Y'};
    private static final char[] START_NOTATION = new char[]{'<', '!', 'N', 'O', 'T', 'A', 'T', 'I', 'O', 'N'};
    private static final char[] START_PROCESSING_INSTRUCTION = new char[]{'<', '?'};
    private static final char[] SYSTEM = new char[]{'S', 'Y', 'S', 'T', 'E', 'M'};
    private static final String UNEXPECTED_EOF = "Unexpected EOF";
    private static final int XML_DECLARATION = 998;
    private int attributeCount;
    private String[] attributes = new String[16];
    private char[] buffer = new char[8192];
    private StringBuilder bufferCapture;
    private int bufferStartColumn;
    private int bufferStartLine;
    private Map<String, Map<String, String>> defaultAttributes;
    private boolean degenerated;
    private int depth;
    private Map<String, char[]> documentEntities;
    private String[] elementStack = new String[16];
    private String encoding;
    private String error;
    private boolean isWhitespace;
    private boolean keepNamespaceAttributes;
    private int limit = 0;
    private String location;
    private String name;
    private String namespace;
    private ContentSource nextContentSource;
    private int[] nspCounts = new int[4];
    private String[] nspStack = new String[8];
    private boolean parsedTopLevelStartTag;
    private int position = 0;
    private String prefix;
    private boolean processDocDecl;
    private boolean processNsp;
    private String publicId;
    private Reader reader;
    private boolean relaxed;
    private String rootElementName;
    private Boolean standalone;
    public final StringPool stringPool = new StringPool();
    private String systemId;
    private String text;
    private int type;
    private boolean unresolved;
    private String version;

    static class ContentSource {
        private final char[] buffer;
        private final int limit;
        private final ContentSource next;
        private final int position;

        ContentSource(ContentSource next, char[] buffer, int position, int limit) {
            this.next = next;
            this.buffer = buffer;
            this.position = position;
            this.limit = limit;
        }
    }

    enum ValueContext {
        ATTRIBUTE,
        TEXT,
        ENTITY_DECLARATION
    }

    static {
        DEFAULT_ENTITIES.put("lt", "<");
        DEFAULT_ENTITIES.put("gt", ">");
        DEFAULT_ENTITIES.put("amp", "&");
        DEFAULT_ENTITIES.put("apos", "'");
        DEFAULT_ENTITIES.put("quot", "\"");
    }

    public void keepNamespaceAttributes() {
        this.keepNamespaceAttributes = true;
    }

    private boolean adjustNsp() throws XmlPullParserException {
        String attrName;
        int j;
        boolean any = false;
        int i = 0;
        while (i < (this.attributeCount << 2)) {
            String prefix;
            attrName = this.attributes[i + 2];
            int cut = attrName.indexOf(58);
            if (cut != -1) {
                prefix = attrName.substring(0, cut);
                attrName = attrName.substring(cut + 1);
            } else if (attrName.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                prefix = attrName;
                attrName = null;
            } else {
                i += 4;
            }
            if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                int[] iArr = this.nspCounts;
                int i2 = this.depth;
                int i3 = iArr[i2];
                iArr[i2] = i3 + 1;
                j = i3 << 1;
                this.nspStack = ensureCapacity(this.nspStack, j + 2);
                this.nspStack[j] = attrName;
                this.nspStack[j + 1] = this.attributes[i + 3];
                if (attrName != null && this.attributes[i + 3].isEmpty()) {
                    checkRelaxed("illegal empty namespace");
                }
                if (this.keepNamespaceAttributes) {
                    this.attributes[i] = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                    any = true;
                } else {
                    String[] strArr = this.attributes;
                    i3 = i + 4;
                    String[] strArr2 = this.attributes;
                    int i4 = this.attributeCount - 1;
                    this.attributeCount = i4;
                    System.arraycopy(strArr, i3, strArr2, i, (i4 << 2) - i);
                    i -= 4;
                }
            } else {
                any = true;
            }
            i += 4;
        }
        if (any) {
            i = (this.attributeCount << 2) - 4;
            while (i >= 0) {
                attrName = this.attributes[i + 2];
                j = attrName.indexOf(58);
                StringBuilder stringBuilder;
                if (j != 0 || this.relaxed) {
                    if (j != -1) {
                        String attrPrefix = attrName.substring(0, j);
                        attrName = attrName.substring(j + 1);
                        String attrNs = getNamespace(attrPrefix);
                        if (attrNs != null || this.relaxed) {
                            this.attributes[i] = attrNs;
                            this.attributes[i + 1] = attrPrefix;
                            this.attributes[i + 2] = attrName;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Undefined Prefix: ");
                            stringBuilder.append(attrPrefix);
                            stringBuilder.append(" in ");
                            stringBuilder.append(this);
                            throw new RuntimeException(stringBuilder.toString());
                        }
                    }
                    i -= 4;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("illegal attribute name: ");
                    stringBuilder.append(attrName);
                    stringBuilder.append(" at ");
                    stringBuilder.append(this);
                    throw new RuntimeException(stringBuilder.toString());
                }
            }
        }
        i = this.name.indexOf(58);
        if (i == 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("illegal tag name: ");
            stringBuilder2.append(this.name);
            checkRelaxed(stringBuilder2.toString());
        }
        if (i != -1) {
            this.prefix = this.name.substring(0, i);
            this.name = this.name.substring(i + 1);
        }
        this.namespace = getNamespace(this.prefix);
        if (this.namespace == null) {
            if (this.prefix != null) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("undefined prefix: ");
                stringBuilder3.append(this.prefix);
                checkRelaxed(stringBuilder3.toString());
            }
            this.namespace = "";
        }
        return any;
    }

    private String[] ensureCapacity(String[] arr, int required) {
        if (arr.length >= required) {
            return arr;
        }
        String[] bigger = new String[(required + 16)];
        System.arraycopy(arr, 0, bigger, 0, arr.length);
        return bigger;
    }

    private void checkRelaxed(String errorMessage) throws XmlPullParserException {
        if (!this.relaxed) {
            throw new XmlPullParserException(errorMessage, this, null);
        } else if (this.error == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error: ");
            stringBuilder.append(errorMessage);
            this.error = stringBuilder.toString();
        }
    }

    public int next() throws XmlPullParserException, IOException {
        return next(false);
    }

    public int nextToken() throws XmlPullParserException, IOException {
        return next(true);
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int next(boolean justOneToken) throws IOException, XmlPullParserException {
        if (this.reader != null) {
            if (this.type == 3) {
                this.depth--;
            }
            if (this.degenerated) {
                this.degenerated = false;
                this.type = 3;
                return this.type;
            }
            if (this.error != null) {
                if (justOneToken) {
                    this.text = this.error;
                    this.type = 9;
                    this.error = null;
                    return this.type;
                }
                this.error = null;
            }
            this.type = peekType(false);
            if (this.type == XML_DECLARATION) {
                readXmlDeclaration();
                this.type = peekType(false);
            }
            this.text = null;
            this.isWhitespace = true;
            this.prefix = null;
            this.name = null;
            this.namespace = null;
            this.attributeCount = -1;
            boolean throwOnResolveFailure = justOneToken ^ 1;
            while (true) {
                String processingInstruction;
                switch (this.type) {
                    case 1:
                        return this.type;
                    case 2:
                        parseStartTag(false, throwOnResolveFailure);
                        return this.type;
                    case 3:
                        readEndTag();
                        return this.type;
                    case 4:
                        this.text = readValue('<', justOneToken ^ 1, throwOnResolveFailure, ValueContext.TEXT);
                        if (this.depth == 0 && this.isWhitespace) {
                            this.type = 7;
                            break;
                        }
                    case 5:
                        read(START_CDATA);
                        this.text = readUntil(END_CDATA, true);
                        break;
                    case 6:
                        if (justOneToken) {
                            StringBuilder entityTextBuilder = new StringBuilder();
                            readEntity(entityTextBuilder, true, throwOnResolveFailure, ValueContext.TEXT);
                            this.text = entityTextBuilder.toString();
                            break;
                        }
                    case 8:
                        read(START_PROCESSING_INSTRUCTION);
                        processingInstruction = readUntil(END_PROCESSING_INSTRUCTION, justOneToken);
                        if (justOneToken) {
                            this.text = processingInstruction;
                            break;
                        }
                        break;
                    case 9:
                        processingInstruction = readComment(justOneToken);
                        if (justOneToken) {
                            this.text = processingInstruction;
                            break;
                        }
                        break;
                    case 10:
                        readDoctype(justOneToken);
                        if (this.parsedTopLevelStartTag) {
                            throw new XmlPullParserException("Unexpected token", this, null);
                        }
                        break;
                    default:
                        throw new XmlPullParserException("Unexpected token", this, null);
                }
                if (!(this.depth == 0 && (this.type == 6 || this.type == 4 || this.type == 5))) {
                    if (justOneToken) {
                        return this.type;
                    }
                    if (this.type == 7) {
                        this.text = null;
                    }
                    int peek = peekType(false);
                    if (this.text == null || this.text.isEmpty() || peek >= 4) {
                        this.type = peek;
                    } else {
                        this.type = 4;
                        return this.type;
                    }
                }
            }
            throw new XmlPullParserException("Unexpected token", this, null);
        }
        throw new XmlPullParserException("setInput() must be called first.", this, null);
    }

    private String readUntil(char[] delimiter, boolean returnText) throws IOException, XmlPullParserException {
        int start = this.position;
        StringBuilder result = null;
        if (returnText && this.text != null) {
            result = new StringBuilder();
            result.append(this.text);
        }
        while (true) {
            if (this.position + delimiter.length > this.limit) {
                if (start < this.position && returnText) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(this.buffer, start, this.position - start);
                }
                if (fillBuffer(delimiter.length)) {
                    start = this.position;
                } else {
                    checkRelaxed(UNEXPECTED_EOF);
                    this.type = 9;
                    return null;
                }
            }
            int i = 0;
            while (i < delimiter.length) {
                if (this.buffer[this.position + i] != delimiter[i]) {
                    this.position++;
                } else {
                    i++;
                }
            }
            i = this.position;
            this.position += delimiter.length;
            if (!returnText) {
                return null;
            }
            if (result == null) {
                return this.stringPool.get(this.buffer, start, i - start);
            }
            result.append(this.buffer, start, i - start);
            return result.toString();
        }
    }

    private void readXmlDeclaration() throws IOException, XmlPullParserException {
        if (!(this.bufferStartLine == 0 && this.bufferStartColumn == 0 && this.position == 0)) {
            checkRelaxed("processing instructions must not start with xml");
        }
        read(START_PROCESSING_INSTRUCTION);
        parseStartTag(true, true);
        if (this.attributeCount < 1 || !OutputKeys.VERSION.equals(this.attributes[2])) {
            checkRelaxed("version expected");
        }
        this.version = this.attributes[3];
        int pos = 1;
        if (1 < this.attributeCount && OutputKeys.ENCODING.equals(this.attributes[6])) {
            this.encoding = this.attributes[7];
            pos = 1 + 1;
        }
        if (pos < this.attributeCount && OutputKeys.STANDALONE.equals(this.attributes[(4 * pos) + 2])) {
            String st = this.attributes[3 + (4 * pos)];
            if ("yes".equals(st)) {
                this.standalone = Boolean.TRUE;
            } else if ("no".equals(st)) {
                this.standalone = Boolean.FALSE;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("illegal standalone value: ");
                stringBuilder.append(st);
                checkRelaxed(stringBuilder.toString());
            }
            pos++;
        }
        if (pos != this.attributeCount) {
            checkRelaxed("unexpected attributes in XML declaration");
        }
        this.isWhitespace = true;
        this.text = null;
    }

    private String readComment(boolean returnText) throws IOException, XmlPullParserException {
        read(START_COMMENT);
        if (this.relaxed) {
            return readUntil(END_COMMENT, returnText);
        }
        String commentText = readUntil(COMMENT_DOUBLE_DASH, returnText);
        if (peekCharacter() == 62) {
            this.position++;
            return commentText;
        }
        throw new XmlPullParserException("Comments may not contain --", this, null);
    }

    private void readDoctype(boolean saveDtdText) throws IOException, XmlPullParserException {
        read(START_DOCTYPE);
        int startPosition = -1;
        if (saveDtdText) {
            this.bufferCapture = new StringBuilder();
            startPosition = this.position;
        }
        try {
            skip();
            this.rootElementName = readName();
            readExternalId(true, true);
            skip();
            if (peekCharacter() == 91) {
                readInternalSubset();
            }
            skip();
            read('>');
            skip();
        } finally {
            if (saveDtdText) {
                this.bufferCapture.append(this.buffer, 0, this.position);
                this.bufferCapture.delete(0, startPosition);
                this.text = this.bufferCapture.toString();
                this.bufferCapture = null;
            }
        }
    }

    private boolean readExternalId(boolean requireSystemName, boolean assignFields) throws IOException, XmlPullParserException {
        skip();
        int c = peekCharacter();
        if (c == 83) {
            read(SYSTEM);
        } else if (c != 80) {
            return false;
        } else {
            read(PUBLIC);
            skip();
            if (assignFields) {
                this.publicId = readQuotedId(true);
            } else {
                readQuotedId(false);
            }
        }
        skip();
        if (!requireSystemName) {
            int delimiter = peekCharacter();
            if (!(delimiter == 34 || delimiter == 39)) {
                return true;
            }
        }
        if (assignFields) {
            this.systemId = readQuotedId(true);
        } else {
            readQuotedId(false);
        }
        return true;
    }

    private String readQuotedId(boolean returnText) throws IOException, XmlPullParserException {
        char[] delimiter;
        int quote = peekCharacter();
        if (quote == 34) {
            delimiter = DOUBLE_QUOTE;
        } else if (quote == 39) {
            delimiter = SINGLE_QUOTE;
        } else {
            throw new XmlPullParserException("Expected a quoted string", this, null);
        }
        this.position++;
        return readUntil(delimiter, returnText);
    }

    private void readInternalSubset() throws IOException, XmlPullParserException {
        read('[');
        while (true) {
            skip();
            if (peekCharacter() == 93) {
                this.position++;
                return;
            }
            switch (peekType(true)) {
                case 8:
                    read(START_PROCESSING_INSTRUCTION);
                    readUntil(END_PROCESSING_INSTRUCTION, false);
                    break;
                case 9:
                    readComment(false);
                    break;
                case 11:
                    readElementDeclaration();
                    break;
                case 12:
                    readEntityDeclaration();
                    break;
                case 13:
                    readAttributeListDeclaration();
                    break;
                case 14:
                    readNotationDeclaration();
                    break;
                case 15:
                    throw new XmlPullParserException("Parameter entity references are not supported", this, null);
                default:
                    throw new XmlPullParserException("Unexpected token", this, null);
            }
        }
    }

    private void readElementDeclaration() throws IOException, XmlPullParserException {
        read(START_ELEMENT);
        skip();
        readName();
        readContentSpec();
        skip();
        read('>');
    }

    private void readContentSpec() throws IOException, XmlPullParserException {
        skip();
        char c = peekCharacter();
        int depth = 0;
        if (c == '(') {
            int c2;
            do {
                if (c2 == 40) {
                    depth++;
                } else if (c2 == 41) {
                    depth--;
                } else if (c2 == -1) {
                    throw new XmlPullParserException("Unterminated element content spec", this, null);
                }
                this.position++;
                c2 = peekCharacter();
            } while (depth > 0);
            if (c2 == 42 || c2 == 63 || c2 == 43) {
                this.position++;
            }
        } else if (c2 == EMPTY[0]) {
            read(EMPTY);
        } else if (c2 == ANY[0]) {
            read(ANY);
        } else {
            throw new XmlPullParserException("Expected element content spec", this, null);
        }
    }

    private void readAttributeListDeclaration() throws IOException, XmlPullParserException {
        read(START_ATTLIST);
        skip();
        String elementName = readName();
        while (true) {
            skip();
            if (peekCharacter() == 62) {
                this.position++;
                return;
            }
            String attributeName = readName();
            skip();
            if (this.position + 1 < this.limit || fillBuffer(2)) {
                int c;
                if (this.buffer[this.position] == NOTATION[0] && this.buffer[this.position + 1] == NOTATION[1]) {
                    read(NOTATION);
                    skip();
                }
                if (peekCharacter() == 40) {
                    this.position++;
                    while (true) {
                        skip();
                        readName();
                        skip();
                        c = peekCharacter();
                        if (c == 41) {
                            this.position++;
                            break;
                        } else if (c == 124) {
                            this.position++;
                        } else {
                            throw new XmlPullParserException("Malformed attribute type", this, null);
                        }
                    }
                }
                readName();
                skip();
                c = peekCharacter();
                if (c == 35) {
                    this.position++;
                    c = peekCharacter();
                    if (c == 82) {
                        read(REQUIRED);
                    } else if (c == 73) {
                        read(IMPLIED);
                    } else if (c == 70) {
                        read(FIXED);
                    } else {
                        throw new XmlPullParserException("Malformed attribute type", this, null);
                    }
                    skip();
                    c = peekCharacter();
                }
                if (c == 34 || c == 39) {
                    this.position++;
                    String value = readValue((char) c, true, true, ValueContext.ATTRIBUTE);
                    if (peekCharacter() == c) {
                        this.position++;
                    }
                    defineAttributeDefault(elementName, attributeName, value);
                }
            } else {
                throw new XmlPullParserException("Malformed attribute list", this, null);
            }
        }
    }

    private void defineAttributeDefault(String elementName, String attributeName, String value) {
        if (this.defaultAttributes == null) {
            this.defaultAttributes = new HashMap();
        }
        Map<String, String> elementAttributes = (Map) this.defaultAttributes.get(elementName);
        if (elementAttributes == null) {
            elementAttributes = new HashMap();
            this.defaultAttributes.put(elementName, elementAttributes);
        }
        elementAttributes.put(attributeName, value);
    }

    private void readEntityDeclaration() throws IOException, XmlPullParserException {
        String entityValue;
        read(START_ENTITY);
        boolean generalEntity = true;
        skip();
        if (peekCharacter() == 37) {
            generalEntity = false;
            this.position++;
            skip();
        }
        String name = readName();
        skip();
        int quote = peekCharacter();
        if (quote == 34 || quote == 39) {
            this.position++;
            String entityValue2 = readValue((char) quote, true, false, ValueContext.ENTITY_DECLARATION);
            if (peekCharacter() == quote) {
                this.position++;
            }
            entityValue = entityValue2;
        } else if (readExternalId(true, false)) {
            entityValue = "";
            skip();
            if (peekCharacter() == NDATA[0]) {
                read(NDATA);
                skip();
                readName();
            }
        } else {
            throw new XmlPullParserException("Expected entity value or external ID", this, null);
        }
        if (generalEntity && this.processDocDecl) {
            if (this.documentEntities == null) {
                this.documentEntities = new HashMap();
            }
            this.documentEntities.put(name, entityValue.toCharArray());
        }
        skip();
        read('>');
    }

    private void readNotationDeclaration() throws IOException, XmlPullParserException {
        read(START_NOTATION);
        skip();
        readName();
        if (readExternalId(false, false)) {
            skip();
            read('>');
            return;
        }
        throw new XmlPullParserException("Expected external ID or public ID for notation", this, null);
    }

    private void readEndTag() throws IOException, XmlPullParserException {
        read('<');
        read('/');
        this.name = readName();
        skip();
        read('>');
        int sp = (this.depth - 1) * 4;
        if (this.depth == 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("read end tag ");
            stringBuilder.append(this.name);
            stringBuilder.append(" with no tags open");
            checkRelaxed(stringBuilder.toString());
            this.type = 9;
            return;
        }
        if (this.name.equals(this.elementStack[sp + 3])) {
            this.namespace = this.elementStack[sp];
            this.prefix = this.elementStack[sp + 1];
            this.name = this.elementStack[sp + 2];
        } else if (!this.relaxed) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("expected: /");
            stringBuilder2.append(this.elementStack[sp + 3]);
            stringBuilder2.append(" read: ");
            stringBuilder2.append(this.name);
            throw new XmlPullParserException(stringBuilder2.toString(), this, null);
        }
    }

    private int peekType(boolean inDeclaration) throws IOException, XmlPullParserException {
        if (this.position >= this.limit && !fillBuffer(1)) {
            return 1;
        }
        char c = this.buffer[this.position];
        int i = 4;
        if (c != '<') {
            switch (c) {
                case '%':
                    if (inDeclaration) {
                        i = 15;
                    }
                    return i;
                case '&':
                    return 6;
                default:
                    return 4;
            }
        } else if (this.position + 3 < this.limit || fillBuffer(4)) {
            c = this.buffer[this.position + 1];
            if (c == '!') {
                c = this.buffer[this.position + 2];
                if (c == '-') {
                    return 9;
                }
                if (c == 'A') {
                    return 13;
                }
                if (c == 'N') {
                    return 14;
                }
                if (c == '[') {
                    return 5;
                }
                switch (c) {
                    case 'D':
                        return 10;
                    case 'E':
                        c = this.buffer[this.position + 3];
                        if (c == 'L') {
                            return 11;
                        }
                        if (c == 'N') {
                            return 12;
                        }
                        break;
                }
                throw new XmlPullParserException("Unexpected <!", this, null);
            } else if (c == '/') {
                return 3;
            } else {
                if (c != '?') {
                    return 2;
                }
                if ((this.position + 5 < this.limit || fillBuffer(6)) && ((this.buffer[this.position + 2] == ULocale.PRIVATE_USE_EXTENSION || this.buffer[this.position + 2] == 'X') && ((this.buffer[this.position + 3] == 'm' || this.buffer[this.position + 3] == 'M') && ((this.buffer[this.position + 4] == 'l' || this.buffer[this.position + 4] == 'L') && this.buffer[this.position + 5] == ' ')))) {
                    return XML_DECLARATION;
                }
                return 8;
            }
        } else {
            throw new XmlPullParserException("Dangling <", this, null);
        }
    }

    private void parseStartTag(boolean xmldecl, boolean throwOnResolveFailure) throws IOException, XmlPullParserException {
        int c;
        int i;
        if (!xmldecl) {
            read('<');
        }
        this.name = readName();
        this.attributeCount = 0;
        while (true) {
            skip();
            if (this.position < this.limit || fillBuffer(1)) {
                c = this.buffer[this.position];
                if (!xmldecl) {
                    if (c != 47) {
                        if (c == 62) {
                            this.position++;
                            break;
                        }
                    }
                    this.degenerated = true;
                    this.position++;
                    skip();
                    read('>');
                    break;
                } else if (c == 63) {
                    this.position++;
                    read('>');
                    return;
                }
                String attrName = readName();
                i = this.attributeCount;
                this.attributeCount = i + 1;
                i *= 4;
                this.attributes = ensureCapacity(this.attributes, i + 4);
                this.attributes[i] = "";
                this.attributes[i + 1] = null;
                this.attributes[i + 2] = attrName;
                skip();
                if (this.position >= this.limit && !fillBuffer(1)) {
                    checkRelaxed(UNEXPECTED_EOF);
                    return;
                } else if (this.buffer[this.position] == '=') {
                    this.position++;
                    skip();
                    if (this.position < this.limit || fillBuffer(1)) {
                        char delimiter = this.buffer[this.position];
                        if (delimiter == PatternTokenizer.SINGLE_QUOTE || delimiter == '\"') {
                            this.position++;
                        } else if (this.relaxed) {
                            delimiter = ' ';
                        } else {
                            throw new XmlPullParserException("attr value delimiter missing!", this, null);
                        }
                        this.attributes[i + 3] = readValue(delimiter, true, throwOnResolveFailure, ValueContext.ATTRIBUTE);
                        if (delimiter != ' ' && peekCharacter() == delimiter) {
                            this.position++;
                        }
                    } else {
                        checkRelaxed(UNEXPECTED_EOF);
                        return;
                    }
                } else if (this.relaxed) {
                    this.attributes[i + 3] = attrName;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attr.value missing f. ");
                    stringBuilder.append(attrName);
                    checkRelaxed(stringBuilder.toString());
                    this.attributes[i + 3] = attrName;
                }
            } else {
                checkRelaxed(UNEXPECTED_EOF);
                return;
            }
        }
        c = this.depth;
        this.depth = c + 1;
        c *= 4;
        if (this.depth == 1) {
            this.parsedTopLevelStartTag = true;
        }
        this.elementStack = ensureCapacity(this.elementStack, c + 4);
        this.elementStack[c + 3] = this.name;
        if (this.depth >= this.nspCounts.length) {
            int[] bigger = new int[(this.depth + 4)];
            System.arraycopy(this.nspCounts, 0, bigger, 0, this.nspCounts.length);
            this.nspCounts = bigger;
        }
        this.nspCounts[this.depth] = this.nspCounts[this.depth - 1];
        if (this.processNsp) {
            adjustNsp();
        } else {
            this.namespace = "";
        }
        if (this.defaultAttributes != null) {
            Map<String, String> elementDefaultAttributes = (Map) this.defaultAttributes.get(this.name);
            if (elementDefaultAttributes != null) {
                for (Entry<String, String> entry : elementDefaultAttributes.entrySet()) {
                    if (getAttributeValue(null, (String) entry.getKey()) == null) {
                        i = this.attributeCount;
                        this.attributeCount = i + 1;
                        i *= 4;
                        this.attributes = ensureCapacity(this.attributes, i + 4);
                        this.attributes[i] = "";
                        this.attributes[i + 1] = null;
                        this.attributes[i + 2] = (String) entry.getKey();
                        this.attributes[i + 3] = (String) entry.getValue();
                    }
                }
            }
        }
        this.elementStack[c] = this.namespace;
        this.elementStack[c + 1] = this.prefix;
        this.elementStack[c + 2] = this.name;
    }

    private void readEntity(StringBuilder out, boolean isEntityToken, boolean throwOnResolveFailure, ValueContext valueContext) throws IOException, XmlPullParserException {
        StringBuilder stringBuilder;
        int start = out.length();
        char[] cArr = this.buffer;
        int i = this.position;
        this.position = i + 1;
        if (cArr[i] == '&') {
            out.append('&');
            while (true) {
                int c = peekCharacter();
                if (c == 59) {
                    out.append(';');
                    this.position++;
                    c = out.substring(start + 1, out.length() - 1);
                    if (isEntityToken) {
                        this.name = c;
                    }
                    if (c.startsWith("#")) {
                        try {
                            if (c.startsWith("#x")) {
                                i = Integer.parseInt(c.substring(2), 16);
                            } else {
                                i = Integer.parseInt(c.substring(1));
                            }
                            out.delete(start, out.length());
                            out.appendCodePoint(i);
                            this.unresolved = false;
                            return;
                        } catch (NumberFormatException e) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Invalid character reference: &");
                            stringBuilder.append(c);
                            throw new XmlPullParserException(stringBuilder.toString());
                        } catch (IllegalArgumentException e2) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Invalid character reference: &");
                            stringBuilder.append(c);
                            throw new XmlPullParserException(stringBuilder.toString());
                        }
                    } else if (valueContext != ValueContext.ENTITY_DECLARATION) {
                        String defaultEntity = (String) DEFAULT_ENTITIES.get(c);
                        if (defaultEntity != null) {
                            out.delete(start, out.length());
                            this.unresolved = false;
                            out.append(defaultEntity);
                            return;
                        }
                        if (this.documentEntities != null) {
                            char[] cArr2 = (char[]) this.documentEntities.get(c);
                            char[] resolved = cArr2;
                            if (cArr2 != null) {
                                out.delete(start, out.length());
                                this.unresolved = false;
                                if (this.processDocDecl) {
                                    pushContentSource(resolved);
                                } else {
                                    out.append(resolved);
                                }
                                return;
                            }
                        }
                        if (this.systemId != null) {
                            out.delete(start, out.length());
                            return;
                        }
                        this.unresolved = true;
                        if (throwOnResolveFailure) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("unresolved: &");
                            stringBuilder2.append(c);
                            stringBuilder2.append(";");
                            checkRelaxed(stringBuilder2.toString());
                        }
                        return;
                    } else {
                        return;
                    }
                } else if (c >= 128 || ((c >= 48 && c <= 57) || ((c >= 97 && c <= 122) || ((c >= 65 && c <= 90) || c == 95 || c == 45 || c == 35)))) {
                    this.position++;
                    out.append((char) c);
                } else if (!this.relaxed) {
                    throw new XmlPullParserException("unterminated entity ref", this, null);
                } else {
                    return;
                }
            }
        }
        throw new AssertionError();
    }

    private String readValue(char delimiter, boolean resolveEntities, boolean throwOnResolveFailure, ValueContext valueContext) throws IOException, XmlPullParserException {
        boolean z;
        char c = delimiter;
        ValueContext valueContext2 = valueContext;
        int start = this.position;
        StringBuilder result = null;
        if (valueContext2 == ValueContext.TEXT && this.text != null) {
            result = new StringBuilder();
            result.append(this.text);
        }
        while (true) {
            if (this.position >= this.limit) {
                if (start < this.position) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(this.buffer, start, this.position - start);
                }
                if (fillBuffer(1)) {
                    start = this.position;
                } else {
                    return result != null ? result.toString() : "";
                }
            }
            char c2 = this.buffer[this.position];
            if (c2 == c || ((c == ' ' && (c2 <= ' ' || c2 == '>')) || (c2 == '&' && !resolveEntities))) {
                z = throwOnResolveFailure;
            } else {
                char c3 = 10;
                if (c2 == 13 || ((c2 == 10 && valueContext2 == ValueContext.ATTRIBUTE) || c2 == '&' || c2 == '<' || ((c2 == ']' && valueContext2 == ValueContext.TEXT) || (c2 == '%' && valueContext2 == ValueContext.ENTITY_DECLARATION)))) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(this.buffer, start, this.position - start);
                    if (c2 == 13) {
                        if ((this.position + 1 < this.limit || fillBuffer(2)) && this.buffer[this.position + 1] == 10) {
                            this.position++;
                        }
                        if (valueContext2 == ValueContext.ATTRIBUTE) {
                            c3 = ' ';
                        }
                        c2 = c3;
                    } else if (c2 == 10) {
                        c2 = ' ';
                    } else if (c2 == '&') {
                        this.isWhitespace = false;
                        readEntity(result, false, throwOnResolveFailure, valueContext2);
                        start = this.position;
                    } else {
                        z = throwOnResolveFailure;
                        if (c2 == '<') {
                            if (valueContext2 == ValueContext.ATTRIBUTE) {
                                checkRelaxed("Illegal: \"<\" inside attribute value");
                            }
                            this.isWhitespace = false;
                        } else if (c2 == ']') {
                            if ((this.position + 2 < this.limit || fillBuffer(3)) && this.buffer[this.position + 1] == ']' && this.buffer[this.position + 2] == '>') {
                                checkRelaxed("Illegal: \"]]>\" outside CDATA section");
                            }
                            this.isWhitespace = false;
                        } else if (c2 == '%') {
                            throw new XmlPullParserException("This parser doesn't support parameter entities", this, null);
                        } else {
                            throw new AssertionError();
                        }
                        this.position++;
                        result.append(c2);
                        start = this.position;
                    }
                    z = throwOnResolveFailure;
                    this.position++;
                    result.append(c2);
                    start = this.position;
                } else {
                    this.isWhitespace &= c2 <= ' ' ? 1 : 0;
                    this.position++;
                }
            }
        }
        z = throwOnResolveFailure;
        if (result == null) {
            return this.stringPool.get(this.buffer, start, this.position - start);
        }
        result.append(this.buffer, start, this.position - start);
        return result.toString();
    }

    private void read(char expected) throws IOException, XmlPullParserException {
        char c = peekCharacter();
        if (c != expected) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("expected: '");
            stringBuilder.append(expected);
            stringBuilder.append("' actual: '");
            stringBuilder.append((char) c);
            stringBuilder.append("'");
            checkRelaxed(stringBuilder.toString());
            if (c == 65535) {
                return;
            }
        }
        this.position++;
    }

    private void read(char[] chars) throws IOException, XmlPullParserException {
        if (this.position + chars.length <= this.limit || fillBuffer(chars.length)) {
            for (int i = 0; i < chars.length; i++) {
                if (this.buffer[this.position + i] != chars[i]) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("expected: \"");
                    stringBuilder.append(new String(chars));
                    stringBuilder.append("\" but was \"");
                    stringBuilder.append(new String(this.buffer, this.position, chars.length));
                    stringBuilder.append("...\"");
                    checkRelaxed(stringBuilder.toString());
                }
            }
            this.position += chars.length;
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("expected: '");
        stringBuilder2.append(new String(chars));
        stringBuilder2.append("' but was EOF");
        checkRelaxed(stringBuilder2.toString());
    }

    private int peekCharacter() throws IOException, XmlPullParserException {
        if (this.position < this.limit || fillBuffer(1)) {
            return this.buffer[this.position];
        }
        return -1;
    }

    private boolean fillBuffer(int minimum) throws IOException, XmlPullParserException {
        int i;
        while (this.nextContentSource != null) {
            if (this.position >= this.limit) {
                popContentSource();
                if (this.limit - this.position >= minimum) {
                    return true;
                }
            }
            throw new XmlPullParserException("Unbalanced entity!", this, null);
        }
        for (i = 0; i < this.position; i++) {
            if (this.buffer[i] == 10) {
                this.bufferStartLine++;
                this.bufferStartColumn = 0;
            } else {
                this.bufferStartColumn++;
            }
        }
        if (this.bufferCapture != null) {
            this.bufferCapture.append(this.buffer, 0, this.position);
        }
        if (this.limit != this.position) {
            this.limit -= this.position;
            System.arraycopy(this.buffer, this.position, this.buffer, 0, this.limit);
        } else {
            this.limit = 0;
        }
        this.position = 0;
        do {
            i = this.reader.read(this.buffer, this.limit, this.buffer.length - this.limit);
            int total = i;
            if (i == -1) {
                return false;
            }
            this.limit += total;
        } while (this.limit < minimum);
        return true;
    }

    private String readName() throws IOException, XmlPullParserException {
        if (this.position < this.limit || fillBuffer(1)) {
            int start = this.position;
            StringBuilder result = null;
            char c = this.buffer[this.position];
            if ((c < 'a' || c > 'z') && !((c >= 'A' && c <= 'Z') || c == '_' || c == ':' || c >= 192 || this.relaxed)) {
                checkRelaxed("name expected");
                return "";
            }
            this.position++;
            while (true) {
                if (this.position >= this.limit) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(this.buffer, start, this.position - start);
                    if (!fillBuffer(1)) {
                        return result.toString();
                    }
                    start = this.position;
                }
                c = this.buffer[this.position];
                if ((c >= 'a' && c <= 'z') || ((c >= 'A' && c <= 'Z') || ((c >= '0' && c <= '9') || c == '_' || c == '-' || c == ':' || c == '.' || c >= 183))) {
                    this.position++;
                } else if (result == null) {
                    return this.stringPool.get(this.buffer, start, this.position - start);
                } else {
                    result.append(this.buffer, start, this.position - start);
                    return result.toString();
                }
            }
        }
        checkRelaxed("name expected");
        return "";
    }

    private void skip() throws IOException, XmlPullParserException {
        while (true) {
            if ((this.position < this.limit || fillBuffer(1)) && this.buffer[this.position] <= 32) {
                this.position++;
            } else {
                return;
            }
        }
    }

    public void setInput(Reader reader) throws XmlPullParserException {
        this.reader = reader;
        this.type = 0;
        this.parsedTopLevelStartTag = false;
        this.name = null;
        this.namespace = null;
        this.degenerated = false;
        this.attributeCount = -1;
        this.encoding = null;
        this.version = null;
        this.standalone = null;
        if (reader != null) {
            this.position = 0;
            this.limit = 0;
            this.bufferStartLine = 0;
            this.bufferStartColumn = 0;
            this.depth = 0;
            this.documentEntities = null;
        }
    }

    public void setInput(InputStream is, String charset) throws XmlPullParserException {
        this.position = 0;
        this.limit = 0;
        boolean detectCharset = charset == null;
        if (is != null) {
            int firstFourBytes;
            if (detectCharset) {
                int i;
                char[] cArr;
                int i2;
                firstFourBytes = 0;
                while (this.limit < 4) {
                    try {
                        i = is.read();
                        if (i == -1) {
                            break;
                        }
                        firstFourBytes = (firstFourBytes << 8) | i;
                        cArr = this.buffer;
                        i2 = this.limit;
                        this.limit = i2 + 1;
                        cArr[i2] = (char) i;
                    } catch (Exception e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid stream or encoding: ");
                        stringBuilder.append(e);
                        throw new XmlPullParserException(stringBuilder.toString(), this, e);
                    }
                }
                if (this.limit == 4) {
                    if (firstFourBytes == -131072) {
                        charset = "UTF-32LE";
                        this.limit = 0;
                    } else if (firstFourBytes == 60) {
                        charset = "UTF-32BE";
                        this.buffer[0] = '<';
                        this.limit = 1;
                    } else if (firstFourBytes == 65279) {
                        charset = "UTF-32BE";
                        this.limit = 0;
                    } else if (firstFourBytes == 3932223) {
                        charset = "UTF-16BE";
                        this.buffer[0] = '<';
                        this.buffer[1] = '?';
                        this.limit = 2;
                    } else if (firstFourBytes == 1006632960) {
                        charset = "UTF-32LE";
                        this.buffer[0] = '<';
                        this.limit = 1;
                    } else if (firstFourBytes == 1006649088) {
                        charset = "UTF-16LE";
                        this.buffer[0] = '<';
                        this.buffer[1] = '?';
                        this.limit = 2;
                    } else if (firstFourBytes == 1010792557) {
                        while (true) {
                            i = is.read();
                            if (i == -1) {
                                break;
                            }
                            cArr = this.buffer;
                            int i3 = this.limit;
                            this.limit = i3 + 1;
                            cArr[i3] = (char) i;
                            if (i == 62) {
                                String s = new String(this.buffer, 0, this.limit);
                                char deli = s.indexOf(OutputKeys.ENCODING);
                                if (deli != 65535) {
                                    while (s.charAt(deli) != '\"' && s.charAt(deli) != PatternTokenizer.SINGLE_QUOTE) {
                                        deli++;
                                    }
                                    i2 = deli + 1;
                                    charset = s.substring(i2, s.indexOf(s.charAt(deli), i2));
                                }
                            }
                        }
                    } else if ((firstFourBytes & -65536) == -16842752) {
                        charset = "UTF-16BE";
                        this.buffer[0] = (char) ((this.buffer[2] << 8) | this.buffer[3]);
                        this.limit = 1;
                    } else if ((-65536 & firstFourBytes) == -131072) {
                        charset = "UTF-16LE";
                        this.buffer[0] = (char) ((this.buffer[3] << 8) | this.buffer[2]);
                        this.limit = 1;
                    } else if ((firstFourBytes & -256) == -272908544) {
                        charset = "UTF-8";
                        this.buffer[0] = this.buffer[3];
                        this.limit = 1;
                    }
                }
            }
            if (charset == null) {
                charset = "UTF-8";
            }
            firstFourBytes = this.limit;
            setInput(new InputStreamReader(is, charset));
            this.encoding = charset;
            this.limit = firstFourBytes;
            if (!detectCharset && peekCharacter() == 65279) {
                this.limit--;
                System.arraycopy(this.buffer, 1, this.buffer, 0, this.limit);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("is == null");
    }

    public void close() throws IOException {
        if (this.reader != null) {
            this.reader.close();
        }
    }

    public boolean getFeature(String feature) {
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(feature)) {
            return this.processNsp;
        }
        if (FEATURE_RELAXED.equals(feature)) {
            return this.relaxed;
        }
        if (XmlPullParser.FEATURE_PROCESS_DOCDECL.equals(feature)) {
            return this.processDocDecl;
        }
        return false;
    }

    public String getInputEncoding() {
        return this.encoding;
    }

    public void defineEntityReplacementText(String entity, String value) throws XmlPullParserException {
        if (this.processDocDecl) {
            throw new IllegalStateException("Entity replacement text may not be defined with DOCTYPE processing enabled.");
        } else if (this.reader != null) {
            if (this.documentEntities == null) {
                this.documentEntities = new HashMap();
            }
            this.documentEntities.put(entity, value.toCharArray());
        } else {
            throw new IllegalStateException("Entity replacement text must be defined after setInput()");
        }
    }

    public Object getProperty(String property) {
        if (property.equals(PROPERTY_XMLDECL_VERSION)) {
            return this.version;
        }
        if (property.equals(PROPERTY_XMLDECL_STANDALONE)) {
            return this.standalone;
        }
        if (!property.equals(PROPERTY_LOCATION)) {
            return null;
        }
        return this.location != null ? this.location : this.reader.toString();
    }

    public String getRootElementName() {
        return this.rootElementName;
    }

    public String getSystemId() {
        return this.systemId;
    }

    public String getPublicId() {
        return this.publicId;
    }

    public int getNamespaceCount(int depth) {
        if (depth <= this.depth) {
            return this.nspCounts[depth];
        }
        throw new IndexOutOfBoundsException();
    }

    public String getNamespacePrefix(int pos) {
        return this.nspStack[pos * 2];
    }

    public String getNamespaceUri(int pos) {
        return this.nspStack[(pos * 2) + 1];
    }

    public String getNamespace(String prefix) {
        if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        }
        for (int i = (getNamespaceCount(this.depth) << 1) - 2; i >= 0; i -= 2) {
            if (prefix == null) {
                if (this.nspStack[i] == null) {
                    return this.nspStack[i + 1];
                }
            } else if (prefix.equals(this.nspStack[i])) {
                return this.nspStack[i + 1];
            }
        }
        return null;
    }

    public int getDepth() {
        return this.depth;
    }

    public String getPositionDescription() {
        StringBuilder buf = new StringBuilder(this.type < TYPES.length ? TYPES[this.type] : "unknown");
        buf.append(' ');
        int i = 0;
        StringBuilder stringBuilder;
        if (this.type == 2 || this.type == 3) {
            if (this.degenerated) {
                buf.append("(empty) ");
            }
            buf.append('<');
            if (this.type == 3) {
                buf.append('/');
            }
            if (this.prefix != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("{");
                stringBuilder.append(this.namespace);
                stringBuilder.append("}");
                stringBuilder.append(this.prefix);
                stringBuilder.append(":");
                buf.append(stringBuilder.toString());
            }
            buf.append(this.name);
            int cnt = this.attributeCount * 4;
            while (i < cnt) {
                StringBuilder stringBuilder2;
                buf.append(' ');
                if (this.attributes[i + 1] != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("{");
                    stringBuilder2.append(this.attributes[i]);
                    stringBuilder2.append("}");
                    stringBuilder2.append(this.attributes[i + 1]);
                    stringBuilder2.append(":");
                    buf.append(stringBuilder2.toString());
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.attributes[i + 2]);
                stringBuilder2.append("='");
                stringBuilder2.append(this.attributes[i + 3]);
                stringBuilder2.append("'");
                buf.append(stringBuilder2.toString());
                i += 4;
            }
            buf.append('>');
        } else if (this.type != 7) {
            if (this.type != 4) {
                buf.append(getText());
            } else if (this.isWhitespace) {
                buf.append("(whitespace)");
            } else {
                String text = getText();
                if (text.length() > 16) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(text.substring(0, 16));
                    stringBuilder.append("...");
                    text = stringBuilder.toString();
                }
                buf.append(text);
            }
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("@");
        stringBuilder3.append(getLineNumber());
        stringBuilder3.append(":");
        stringBuilder3.append(getColumnNumber());
        buf.append(stringBuilder3.toString());
        if (this.location != null) {
            buf.append(" in ");
            buf.append(this.location);
        } else if (this.reader != null) {
            buf.append(" in ");
            buf.append(this.reader.toString());
        }
        return buf.toString();
    }

    public int getLineNumber() {
        int result = this.bufferStartLine;
        for (int i = 0; i < this.position; i++) {
            if (this.buffer[i] == 10) {
                result++;
            }
        }
        return result + 1;
    }

    public int getColumnNumber() {
        int result = this.bufferStartColumn;
        for (int i = 0; i < this.position; i++) {
            if (this.buffer[i] == 10) {
                result = 0;
            } else {
                result++;
            }
        }
        return result + 1;
    }

    public boolean isWhitespace() throws XmlPullParserException {
        if (this.type == 4 || this.type == 7 || this.type == 5) {
            return this.isWhitespace;
        }
        throw new XmlPullParserException(ILLEGAL_TYPE, this, null);
    }

    public String getText() {
        if (this.type < 4 || (this.type == 6 && this.unresolved)) {
            return null;
        }
        if (this.text == null) {
            return "";
        }
        return this.text;
    }

    public char[] getTextCharacters(int[] poslen) {
        String text = getText();
        if (text == null) {
            poslen[0] = -1;
            poslen[1] = -1;
            return null;
        }
        char[] result = text.toCharArray();
        poslen[0] = 0;
        poslen[1] = result.length;
        return result;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getName() {
        return this.name;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public boolean isEmptyElementTag() throws XmlPullParserException {
        if (this.type == 2) {
            return this.degenerated;
        }
        throw new XmlPullParserException(ILLEGAL_TYPE, this, null);
    }

    public int getAttributeCount() {
        return this.attributeCount;
    }

    public String getAttributeType(int index) {
        return "CDATA";
    }

    public boolean isAttributeDefault(int index) {
        return false;
    }

    public String getAttributeNamespace(int index) {
        if (index < this.attributeCount) {
            return this.attributes[index * 4];
        }
        throw new IndexOutOfBoundsException();
    }

    public String getAttributeName(int index) {
        if (index < this.attributeCount) {
            return this.attributes[(index * 4) + 2];
        }
        throw new IndexOutOfBoundsException();
    }

    public String getAttributePrefix(int index) {
        if (index < this.attributeCount) {
            return this.attributes[(index * 4) + 1];
        }
        throw new IndexOutOfBoundsException();
    }

    public String getAttributeValue(int index) {
        if (index < this.attributeCount) {
            return this.attributes[(index * 4) + 3];
        }
        throw new IndexOutOfBoundsException();
    }

    public String getAttributeValue(String namespace, String name) {
        int i = (this.attributeCount * 4) - 4;
        while (i >= 0) {
            if (this.attributes[i + 2].equals(name) && (namespace == null || this.attributes[i].equals(namespace))) {
                return this.attributes[i + 3];
            }
            i -= 4;
        }
        return null;
    }

    public int getEventType() throws XmlPullParserException {
        return this.type;
    }

    public int nextTag() throws XmlPullParserException, IOException {
        next();
        if (this.type == 4 && this.isWhitespace) {
            next();
        }
        if (this.type == 3 || this.type == 2) {
            return this.type;
        }
        throw new XmlPullParserException("unexpected type", this, null);
    }

    public void require(int type, String namespace, String name) throws XmlPullParserException, IOException {
        if (type != this.type || ((namespace != null && !namespace.equals(getNamespace())) || (name != null && !name.equals(getName())))) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("expected: ");
            stringBuilder.append(TYPES[type]);
            stringBuilder.append(" {");
            stringBuilder.append(namespace);
            stringBuilder.append("}");
            stringBuilder.append(name);
            throw new XmlPullParserException(stringBuilder.toString(), this, null);
        }
    }

    public String nextText() throws XmlPullParserException, IOException {
        if (this.type == 2) {
            String result;
            next();
            if (this.type == 4) {
                result = getText();
                next();
            } else {
                result = "";
            }
            if (this.type == 3) {
                return result;
            }
            throw new XmlPullParserException("END_TAG expected", this, null);
        }
        throw new XmlPullParserException("precondition: START_TAG", this, null);
    }

    public void setFeature(String feature, boolean value) throws XmlPullParserException {
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(feature)) {
            this.processNsp = value;
        } else if (XmlPullParser.FEATURE_PROCESS_DOCDECL.equals(feature)) {
            this.processDocDecl = value;
        } else if (FEATURE_RELAXED.equals(feature)) {
            this.relaxed = value;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unsupported feature: ");
            stringBuilder.append(feature);
            throw new XmlPullParserException(stringBuilder.toString(), this, null);
        }
    }

    public void setProperty(String property, Object value) throws XmlPullParserException {
        if (property.equals(PROPERTY_LOCATION)) {
            this.location = String.valueOf(value);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unsupported property: ");
        stringBuilder.append(property);
        throw new XmlPullParserException(stringBuilder.toString());
    }

    private void pushContentSource(char[] newBuffer) {
        this.nextContentSource = new ContentSource(this.nextContentSource, this.buffer, this.position, this.limit);
        this.buffer = newBuffer;
        this.position = 0;
        this.limit = newBuffer.length;
    }

    private void popContentSource() {
        this.buffer = this.nextContentSource.buffer;
        this.position = this.nextContentSource.position;
        this.limit = this.nextContentSource.limit;
        this.nextContentSource = this.nextContentSource.next;
    }
}
