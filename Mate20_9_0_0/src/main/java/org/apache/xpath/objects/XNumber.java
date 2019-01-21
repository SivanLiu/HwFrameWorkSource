package org.apache.xpath.objects;

import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.Constants;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;

public class XNumber extends XObject {
    static final long serialVersionUID = -2720400709619020193L;
    double m_val;

    public XNumber(double d) {
        this.m_val = d;
    }

    public XNumber(Number num) {
        this.m_val = num.doubleValue();
        setObject(num);
    }

    public int getType() {
        return 2;
    }

    public String getTypeString() {
        return "#NUMBER";
    }

    public double num() {
        return this.m_val;
    }

    public double num(XPathContext xctxt) throws TransformerException {
        return this.m_val;
    }

    public boolean bool() {
        return (Double.isNaN(this.m_val) || this.m_val == XPath.MATCH_SCORE_QNAME) ? false : true;
    }

    public String str() {
        if (Double.isNaN(this.m_val)) {
            return "NaN";
        }
        if (!Double.isInfinite(this.m_val)) {
            String s = Double.toString(this.m_val);
            int len = s.length();
            if (s.charAt(len - 2) == '.' && s.charAt(len - 1) == '0') {
                s = s.substring(0, len - 2);
                if (s.equals("-0")) {
                    return "0";
                }
                return s;
            }
            int e = s.indexOf(69);
            if (e >= 0) {
                String sign;
                int exp = Integer.parseInt(s.substring(e + 1));
                if (s.charAt(0) == '-') {
                    sign = "-";
                    s = s.substring(1);
                    e--;
                } else {
                    sign = "";
                }
                int nDigits = e - 2;
                StringBuilder stringBuilder;
                if (exp >= nDigits) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(sign);
                    stringBuilder.append(s.substring(0, 1));
                    stringBuilder.append(s.substring(2, e));
                    stringBuilder.append(zeros(exp - nDigits));
                    return stringBuilder.toString();
                }
                while (s.charAt(e - 1) == '0') {
                    e--;
                }
                if (exp > 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(sign);
                    stringBuilder.append(s.substring(0, 1));
                    stringBuilder.append(s.substring(2, 2 + exp));
                    stringBuilder.append(Constants.ATTRVAL_THIS);
                    stringBuilder.append(s.substring(2 + exp, e));
                    return stringBuilder.toString();
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(sign);
                stringBuilder.append("0.");
                stringBuilder.append(zeros(-1 - exp));
                stringBuilder.append(s.substring(0, 1));
                stringBuilder.append(s.substring(2, e));
                return stringBuilder.toString();
            } else if (s.charAt(len - 1) == '0') {
                return s.substring(0, len - 1);
            } else {
                return s;
            }
        } else if (this.m_val > XPath.MATCH_SCORE_QNAME) {
            return Constants.ATTRVAL_INFINITY;
        } else {
            return "-Infinity";
        }
    }

    private static String zeros(int n) {
        if (n < 1) {
            return "";
        }
        char[] buf = new char[n];
        for (int i = 0; i < n; i++) {
            buf[i] = '0';
        }
        return new String(buf);
    }

    public Object object() {
        if (this.m_obj == null) {
            setObject(new Double(this.m_val));
        }
        return this.m_obj;
    }

    public boolean equals(XObject obj2) {
        TransformerException te;
        int t = obj2.getType();
        if (t == 4) {
            try {
                return obj2.equals(this);
            } catch (TransformerException te2) {
                throw new WrappedRuntimeException(te2);
            }
        }
        te2 = null;
        if (t == 1) {
            boolean te3;
            if (obj2.bool() == bool()) {
                te3 = true;
            }
            return te3;
        }
        if (this.m_val == obj2.num()) {
            te2 = 1;
        }
        return te2;
    }

    public boolean isStableNumber() {
        return true;
    }

    public void callVisitors(ExpressionOwner owner, XPathVisitor visitor) {
        visitor.visitNumberLiteral(owner, this);
    }
}
