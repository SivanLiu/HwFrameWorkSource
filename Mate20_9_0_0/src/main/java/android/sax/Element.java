package android.sax;

import android.provider.SettingsStringUtil;
import java.util.ArrayList;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

public class Element {
    Children children;
    final int depth;
    EndElementListener endElementListener;
    EndTextElementListener endTextElementListener;
    final String localName;
    final Element parent;
    ArrayList<Element> requiredChilden;
    StartElementListener startElementListener;
    final String uri;
    boolean visited;

    Element(Element parent, String uri, String localName, int depth) {
        this.parent = parent;
        this.uri = uri;
        this.localName = localName;
        this.depth = depth;
    }

    public Element getChild(String localName) {
        return getChild("", localName);
    }

    public Element getChild(String uri, String localName) {
        if (this.endTextElementListener == null) {
            if (this.children == null) {
                this.children = new Children();
            }
            return this.children.getOrCreate(this, uri, localName);
        }
        throw new IllegalStateException("This element already has an end text element listener. It cannot have children.");
    }

    public Element requireChild(String localName) {
        return requireChild("", localName);
    }

    public Element requireChild(String uri, String localName) {
        Element child = getChild(uri, localName);
        if (this.requiredChilden == null) {
            this.requiredChilden = new ArrayList();
            this.requiredChilden.add(child);
        } else if (!this.requiredChilden.contains(child)) {
            this.requiredChilden.add(child);
        }
        return child;
    }

    public void setElementListener(ElementListener elementListener) {
        setStartElementListener(elementListener);
        setEndElementListener(elementListener);
    }

    public void setTextElementListener(TextElementListener elementListener) {
        setStartElementListener(elementListener);
        setEndTextElementListener(elementListener);
    }

    public void setStartElementListener(StartElementListener startElementListener) {
        if (this.startElementListener == null) {
            this.startElementListener = startElementListener;
            return;
        }
        throw new IllegalStateException("Start element listener has already been set.");
    }

    public void setEndElementListener(EndElementListener endElementListener) {
        if (this.endElementListener == null) {
            this.endElementListener = endElementListener;
            return;
        }
        throw new IllegalStateException("End element listener has already been set.");
    }

    public void setEndTextElementListener(EndTextElementListener endTextElementListener) {
        if (this.endTextElementListener != null) {
            throw new IllegalStateException("End text element listener has already been set.");
        } else if (this.children == null) {
            this.endTextElementListener = endTextElementListener;
        } else {
            throw new IllegalStateException("This element already has children. It cannot have an end text element listener.");
        }
    }

    public String toString() {
        return toString(this.uri, this.localName);
    }

    static String toString(String uri, String localName) {
        String str;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("'");
        if (uri.equals("")) {
            str = localName;
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(uri);
            stringBuilder2.append(SettingsStringUtil.DELIMITER);
            stringBuilder2.append(localName);
            str = stringBuilder2.toString();
        }
        stringBuilder.append(str);
        stringBuilder.append("'");
        return stringBuilder.toString();
    }

    void resetRequiredChildren() {
        ArrayList<Element> requiredChildren = this.requiredChilden;
        if (requiredChildren != null) {
            for (int i = requiredChildren.size() - 1; i >= 0; i--) {
                ((Element) requiredChildren.get(i)).visited = false;
            }
        }
    }

    void checkRequiredChildren(Locator locator) throws SAXParseException {
        ArrayList<Element> requiredChildren = this.requiredChilden;
        if (requiredChildren != null) {
            int i = requiredChildren.size() - 1;
            while (i >= 0) {
                Element child = (Element) requiredChildren.get(i);
                if (child.visited) {
                    i--;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Element named ");
                    stringBuilder.append(this);
                    stringBuilder.append(" is missing required child element named ");
                    stringBuilder.append(child);
                    stringBuilder.append(".");
                    throw new BadXmlException(stringBuilder.toString(), locator);
                }
            }
        }
    }
}
