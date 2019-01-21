package javax.xml.parsers;

import javax.xml.validation.Schema;
import org.apache.harmony.xml.parsers.SAXParserFactoryImpl;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public abstract class SAXParserFactory {
    private boolean namespaceAware = false;
    private boolean validating = false;

    public abstract boolean getFeature(String str) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException;

    public abstract SAXParser newSAXParser() throws ParserConfigurationException, SAXException;

    public abstract void setFeature(String str, boolean z) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException;

    protected SAXParserFactory() {
    }

    public static SAXParserFactory newInstance() {
        return new SAXParserFactoryImpl();
    }

    public static SAXParserFactory newInstance(String factoryClassName, ClassLoader classLoader) {
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
            return (SAXParserFactory) e.newInstance();
        }
        throw new FactoryConfigurationError("factoryClassName == null");
    }

    public void setNamespaceAware(boolean awareness) {
        this.namespaceAware = awareness;
    }

    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    public boolean isValidating() {
        return this.validating;
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
