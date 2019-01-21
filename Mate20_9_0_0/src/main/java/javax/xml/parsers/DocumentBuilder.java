package javax.xml.parsers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.validation.Schema;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class DocumentBuilder {
    private static final boolean DEBUG = false;

    public abstract DOMImplementation getDOMImplementation();

    public abstract boolean isNamespaceAware();

    public abstract boolean isValidating();

    public abstract Document newDocument();

    public abstract Document parse(InputSource inputSource) throws SAXException, IOException;

    public abstract void setEntityResolver(EntityResolver entityResolver);

    public abstract void setErrorHandler(ErrorHandler errorHandler);

    protected DocumentBuilder() {
    }

    public void reset() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("This DocumentBuilder, \"");
        stringBuilder.append(getClass().getName());
        stringBuilder.append("\", does not support the reset functionality.  Specification \"");
        stringBuilder.append(getClass().getPackage().getSpecificationTitle());
        stringBuilder.append("\" version \"");
        stringBuilder.append(getClass().getPackage().getSpecificationVersion());
        stringBuilder.append("\"");
        throw new UnsupportedOperationException(stringBuilder.toString());
    }

    public Document parse(InputStream is) throws SAXException, IOException {
        if (is != null) {
            return parse(new InputSource(is));
        }
        throw new IllegalArgumentException("InputStream cannot be null");
    }

    public Document parse(InputStream is, String systemId) throws SAXException, IOException {
        if (is != null) {
            InputSource in = new InputSource(is);
            in.setSystemId(systemId);
            return parse(in);
        }
        throw new IllegalArgumentException("InputStream cannot be null");
    }

    public Document parse(String uri) throws SAXException, IOException {
        if (uri != null) {
            return parse(new InputSource(uri));
        }
        throw new IllegalArgumentException("URI cannot be null");
    }

    public Document parse(File f) throws SAXException, IOException {
        if (f != null) {
            return parse(new InputSource(FilePathToURI.filepath2URI(f.getAbsolutePath())));
        }
        throw new IllegalArgumentException("File cannot be null");
    }

    public Schema getSchema() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("This parser does not support specification \"");
        stringBuilder.append(getClass().getPackage().getSpecificationTitle());
        stringBuilder.append("\" version \"");
        stringBuilder.append(getClass().getPackage().getSpecificationVersion());
        stringBuilder.append("\"");
        throw new UnsupportedOperationException(stringBuilder.toString());
    }

    public boolean isXIncludeAware() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("This parser does not support specification \"");
        stringBuilder.append(getClass().getPackage().getSpecificationTitle());
        stringBuilder.append("\" version \"");
        stringBuilder.append(getClass().getPackage().getSpecificationVersion());
        stringBuilder.append("\"");
        throw new UnsupportedOperationException(stringBuilder.toString());
    }
}
