package org.apache.xml.dtm.ref;

import javax.xml.transform.SourceLocator;

public class NodeLocator implements SourceLocator {
    protected int m_columnNumber;
    protected int m_lineNumber;
    protected String m_publicId;
    protected String m_systemId;

    public NodeLocator(String publicId, String systemId, int lineNumber, int columnNumber) {
        this.m_publicId = publicId;
        this.m_systemId = systemId;
        this.m_lineNumber = lineNumber;
        this.m_columnNumber = columnNumber;
    }

    public String getPublicId() {
        return this.m_publicId;
    }

    public String getSystemId() {
        return this.m_systemId;
    }

    public int getLineNumber() {
        return this.m_lineNumber;
    }

    public int getColumnNumber() {
        return this.m_columnNumber;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("file '");
        stringBuilder.append(this.m_systemId);
        stringBuilder.append("', line #");
        stringBuilder.append(this.m_lineNumber);
        stringBuilder.append(", column #");
        stringBuilder.append(this.m_columnNumber);
        return stringBuilder.toString();
    }
}
