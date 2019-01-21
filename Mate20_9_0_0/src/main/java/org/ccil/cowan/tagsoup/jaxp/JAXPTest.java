package org.ccil.cowan.tagsoup.jaxp;

import java.io.File;
import java.io.PrintStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.Document;
import org.xml.sax.helpers.DefaultHandler;

public class JAXPTest {
    public static void main(String[] args) throws Exception {
        new JAXPTest().test(args);
    }

    private void test(String[] args) throws Exception {
        if (args.length != 1) {
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Usage: java ");
            stringBuilder.append(getClass());
            stringBuilder.append(" [input-file]");
            printStream.println(stringBuilder.toString());
            System.exit(1);
        }
        File f = new File(args[0]);
        System.setProperty("javax.xml.parsers.SAXParserFactory", "org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl");
        SAXParserFactory spf = SAXParserFactory.newInstance();
        PrintStream printStream2 = System.out;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Ok, SAX factory JAXP creates is: ");
        stringBuilder2.append(spf);
        printStream2.println(stringBuilder2.toString());
        System.out.println("Let's parse...");
        spf.newSAXParser().parse(f, new DefaultHandler());
        System.out.println("Done. And then DOM build:");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
        PrintStream printStream3 = System.out;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Succesfully built DOM tree from '");
        stringBuilder3.append(f);
        stringBuilder3.append("', -> ");
        stringBuilder3.append(doc);
        printStream3.println(stringBuilder3.toString());
    }
}
