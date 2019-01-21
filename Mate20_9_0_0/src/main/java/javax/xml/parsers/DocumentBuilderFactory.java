package javax.xml.parsers;

import javax.xml.validation.Schema;
import org.apache.harmony.xml.parsers.DocumentBuilderFactoryImpl;

public abstract class DocumentBuilderFactory {
    private boolean coalescing = false;
    private boolean expandEntityRef = true;
    private boolean ignoreComments = false;
    private boolean namespaceAware = false;
    private boolean validating = false;
    private boolean whitespace = false;

    public abstract Object getAttribute(String str) throws IllegalArgumentException;

    public abstract boolean getFeature(String str) throws ParserConfigurationException;

    public abstract DocumentBuilder newDocumentBuilder() throws ParserConfigurationException;

    public abstract void setAttribute(String str, Object obj) throws IllegalArgumentException;

    public abstract void setFeature(String str, boolean z) throws ParserConfigurationException;

    protected DocumentBuilderFactory() {
    }

    public static DocumentBuilderFactory newInstance() {
        return new DocumentBuilderFactoryImpl();
    }

    public static DocumentBuilderFactory newInstance(String factoryClassName, ClassLoader classLoader) {
        if (factoryClassName != null) {
            ClassNotFoundException e;
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if (classLoader != null) {
                try {
                    e = classLoader.loadClass(factoryClassName);
                } catch (ClassNotFoundException e2) {
                    throw new FactoryConfigurationError(e2);
                } catch (InstantiationException e22) {
                    throw new FactoryConfigurationError(e22);
                } catch (IllegalAccessException e222) {
                    throw new FactoryConfigurationError(e222);
                }
            }
            e = Class.forName(factoryClassName);
            return (DocumentBuilderFactory) e.newInstance();
        }
        throw new FactoryConfigurationError("factoryClassName == null");
    }

    public void setNamespaceAware(boolean awareness) {
        this.namespaceAware = awareness;
    }

    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    public void setIgnoringElementContentWhitespace(boolean whitespace) {
        this.whitespace = whitespace;
    }

    public void setExpandEntityReferences(boolean expandEntityRef) {
        this.expandEntityRef = expandEntityRef;
    }

    public void setIgnoringComments(boolean ignoreComments) {
        this.ignoreComments = ignoreComments;
    }

    public void setCoalescing(boolean coalescing) {
        this.coalescing = coalescing;
    }

    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    public boolean isValidating() {
        return this.validating;
    }

    public boolean isIgnoringElementContentWhitespace() {
        return this.whitespace;
    }

    public boolean isExpandEntityReferences() {
        return this.expandEntityRef;
    }

    public boolean isIgnoringComments() {
        return this.ignoreComments;
    }

    public boolean isCoalescing() {
        return this.coalescing;
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

    public void setSchema(Schema schema) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("This parser does not support specification \"");
        stringBuilder.append(getClass().getPackage().getSpecificationTitle());
        stringBuilder.append("\" version \"");
        stringBuilder.append(getClass().getPackage().getSpecificationVersion());
        stringBuilder.append("\"");
        throw new UnsupportedOperationException(stringBuilder.toString());
    }

    public void setXIncludeAware(boolean state) {
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
