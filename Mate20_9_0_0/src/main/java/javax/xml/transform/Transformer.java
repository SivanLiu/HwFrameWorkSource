package javax.xml.transform;

import java.util.Properties;

public abstract class Transformer {
    public abstract void clearParameters();

    public abstract ErrorListener getErrorListener();

    public abstract Properties getOutputProperties();

    public abstract String getOutputProperty(String str) throws IllegalArgumentException;

    public abstract Object getParameter(String str);

    public abstract URIResolver getURIResolver();

    public abstract void setErrorListener(ErrorListener errorListener) throws IllegalArgumentException;

    public abstract void setOutputProperties(Properties properties);

    public abstract void setOutputProperty(String str, String str2) throws IllegalArgumentException;

    public abstract void setParameter(String str, Object obj);

    public abstract void setURIResolver(URIResolver uRIResolver);

    public abstract void transform(Source source, Result result) throws TransformerException;

    protected Transformer() {
    }

    public void reset() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("This Transformer, \"");
        stringBuilder.append(getClass().getName());
        stringBuilder.append("\", does not support the reset functionality.  Specification \"");
        stringBuilder.append(getClass().getPackage().getSpecificationTitle());
        stringBuilder.append("\" version \"");
        stringBuilder.append(getClass().getPackage().getSpecificationVersion());
        stringBuilder.append("\"");
        throw new UnsupportedOperationException(stringBuilder.toString());
    }
}
