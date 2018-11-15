package org.bouncycastle;

import org.bouncycastle.util.Strings;

public class LICENSE {
    public static String licenseText;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Copyright (c) 2000-2017 The Legion of the Bouncy Castle Inc. (http://www.bouncycastle.org) ");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("Permission is hereby granted, free of charge, to any person obtaining a copy of this software ");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("and associated documentation files (the \"Software\"), to deal in the Software without restriction, ");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, ");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("subject to the following conditions:");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("The above copyright notice and this permission notice shall be included in all copies or substantial");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("portions of the Software.");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER");
        stringBuilder.append(Strings.lineSeparator());
        stringBuilder.append("DEALINGS IN THE SOFTWARE.");
        licenseText = stringBuilder.toString();
    }

    public static void main(String[] strArr) {
        System.out.println(licenseText);
    }
}
