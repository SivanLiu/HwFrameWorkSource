package gov.nist.javax.sip.message;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.HeaderFactoryExt;
import gov.nist.javax.sip.header.HeaderFactoryImpl;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;

public class MultipartMimeContentImpl implements MultipartMimeContent {
    public static String BOUNDARY = "boundary";
    private String boundary;
    private List<Content> contentList = new LinkedList();
    private ContentTypeHeader multipartMimeContentTypeHeader;

    public MultipartMimeContentImpl(ContentTypeHeader contentTypeHeader) {
        this.multipartMimeContentTypeHeader = contentTypeHeader;
        this.boundary = contentTypeHeader.getParameter(BOUNDARY);
    }

    public boolean add(Content content) {
        return this.contentList.add((ContentImpl) content);
    }

    public ContentTypeHeader getContentTypeHeader() {
        return this.multipartMimeContentTypeHeader;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (Content content : this.contentList) {
            stringBuffer.append(content.toString());
        }
        return stringBuffer.toString();
    }

    public void createContentList(String body) throws ParseException {
        String str = body;
        int i = 0;
        try {
            HeaderFactoryExt headerFactory = new HeaderFactoryImpl();
            String delimiter = getContentTypeHeader().getParameter(BOUNDARY);
            if (delimiter == null) {
                this.contentList = new LinkedList();
                ContentImpl content = new ContentImpl(str, delimiter);
                content.setContentTypeHeader(getContentTypeHeader());
                this.contentList.add(content);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("--");
            stringBuilder.append(delimiter);
            stringBuilder.append(Separators.NEWLINE);
            String[] fragments = str.split(stringBuilder.toString());
            int length = fragments.length;
            int i2 = 0;
            while (i2 < length) {
                String nextPart = fragments[i2];
                if (nextPart != null) {
                    HeaderFactoryExt headerFactory2;
                    StringBuffer strbuf = new StringBuffer(nextPart);
                    while (strbuf.length() > 0 && (strbuf.charAt(i) == 13 || strbuf.charAt(i) == 10)) {
                        strbuf.deleteCharAt(i);
                    }
                    if (strbuf.length() == 0) {
                        headerFactory2 = headerFactory;
                    } else {
                        nextPart = strbuf.toString();
                        int position = nextPart.indexOf("\r\n\r\n");
                        int off = 4;
                        if (position == -1) {
                            position = nextPart.indexOf(Separators.RETURN);
                            off = 2;
                        }
                        StringBuilder stringBuilder2;
                        if (position != -1) {
                            String rest = nextPart.substring(position + off);
                            if (rest != null) {
                                String headers = nextPart.substring(i, position);
                                ContentImpl content2 = new ContentImpl(rest, this.boundary);
                                String[] headerArray = headers.split(Separators.NEWLINE);
                                i = headerArray.length;
                                int i3 = 0;
                                while (i3 < i) {
                                    int i4 = i;
                                    String hdr = headerArray[i3];
                                    headerFactory2 = headerFactory;
                                    Header headerFactory3 = headerFactory.createHeader(hdr);
                                    if ((headerFactory3 instanceof ContentTypeHeader) != null) {
                                        content2.setContentTypeHeader((ContentTypeHeader) headerFactory3);
                                    } else if (headerFactory3 instanceof ContentDispositionHeader) {
                                        content2.setContentDispositionHeader((ContentDispositionHeader) headerFactory3);
                                    } else {
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("Unexpected header type ");
                                        stringBuilder3.append(headerFactory3.getName());
                                        throw new ParseException(stringBuilder3.toString(), 0);
                                    }
                                    this.contentList.add(content2);
                                    i3++;
                                    i = i4;
                                    headerFactory = headerFactory2;
                                }
                                headerFactory2 = headerFactory;
                            } else {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("No content [");
                                stringBuilder2.append(nextPart);
                                stringBuilder2.append("]");
                                throw new ParseException(stringBuilder2.toString(), 0);
                            }
                        }
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("no content type header found in ");
                        stringBuilder2.append(nextPart);
                        throw new ParseException(stringBuilder2.toString(), 0);
                    }
                    i2++;
                    headerFactory = headerFactory2;
                    str = body;
                    i = 0;
                } else {
                    return;
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ParseException("Invalid Multipart mime format", 0);
        }
    }

    public Content getContentByType(String contentType, String contentSubtype) {
        Content retval = null;
        if (this.contentList == null) {
            return null;
        }
        for (Content content : this.contentList) {
            if (content.getContentTypeHeader().getContentType().equalsIgnoreCase(contentType) && content.getContentTypeHeader().getContentSubType().equalsIgnoreCase(contentSubtype)) {
                retval = content;
                break;
            }
        }
        return retval;
    }

    public void addContent(Content content) {
        add(content);
    }

    public Iterator<Content> getContents() {
        return this.contentList.iterator();
    }

    public int getContentCount() {
        return this.contentList.size();
    }
}
