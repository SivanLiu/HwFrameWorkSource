package org.apache.harmony.xml.dom;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Node;

public final class CDATASectionImpl extends TextImpl implements CDATASection {
    public CDATASectionImpl(DocumentImpl document, String data) {
        super(document, data);
    }

    public String getNodeName() {
        return "#cdata-section";
    }

    public short getNodeType() {
        return (short) 4;
    }

    public void split() {
        if (needsSplitting()) {
            Node parent = getParentNode();
            String[] parts = getData().split("\\]\\]>");
            DocumentImpl documentImpl = this.document;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(parts[0]);
            stringBuilder.append("]]");
            parent.insertBefore(new CDATASectionImpl(documentImpl, stringBuilder.toString()), this);
            for (int p = 1; p < parts.length - 1; p++) {
                DocumentImpl documentImpl2 = this.document;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(">");
                stringBuilder2.append(parts[p]);
                stringBuilder2.append("]]");
                parent.insertBefore(new CDATASectionImpl(documentImpl2, stringBuilder2.toString()), this);
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(">");
            stringBuilder3.append(parts[parts.length - 1]);
            setData(stringBuilder3.toString());
        }
    }

    public boolean needsSplitting() {
        return this.buffer.indexOf("]]>") != -1;
    }

    public TextImpl replaceWithText() {
        TextImpl replacement = new TextImpl(this.document, getData());
        this.parent.insertBefore(replacement, this);
        this.parent.removeChild(this);
        return replacement;
    }
}
