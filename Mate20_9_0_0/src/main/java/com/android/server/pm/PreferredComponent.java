package com.android.server.pm;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.util.Slog;
import com.android.internal.util.XmlUtils;
import huawei.cust.HwCustUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PreferredComponent {
    private static final String ATTR_ALWAYS = "always";
    private static final String ATTR_MATCH = "match";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SET = "set";
    private static final String TAG_SET = "set";
    public boolean mAlways;
    private final Callbacks mCallbacks;
    public final ComponentName mComponent;
    private HwCustPreferredComponent mCustPc;
    public final int mMatch;
    private String mParseError;
    final String[] mSetClasses;
    final String[] mSetComponents;
    final String[] mSetPackages;
    final String mShortComponent;

    public interface Callbacks {
        boolean onReadTag(String str, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException;
    }

    public PreferredComponent(Callbacks callbacks, int match, ComponentName[] set, ComponentName component, boolean always) {
        int i = 0;
        this.mCustPc = (HwCustPreferredComponent) HwCustUtils.createObj(HwCustPreferredComponent.class, new Object[0]);
        this.mCallbacks = callbacks;
        this.mMatch = 268369920 & match;
        this.mComponent = component;
        this.mAlways = always;
        this.mShortComponent = component.flattenToShortString();
        this.mParseError = null;
        if (set != null) {
            int N = set.length;
            String[] myPackages = new String[N];
            String[] myClasses = new String[N];
            String[] myComponents = new String[N];
            while (i < N) {
                ComponentName cn = set[i];
                if (cn == null) {
                    this.mSetPackages = null;
                    this.mSetClasses = null;
                    this.mSetComponents = null;
                    return;
                }
                myPackages[i] = cn.getPackageName().intern();
                myClasses[i] = cn.getClassName().intern();
                myComponents[i] = cn.flattenToShortString();
                i++;
            }
            this.mSetPackages = myPackages;
            this.mSetClasses = myClasses;
            this.mSetComponents = myComponents;
        } else {
            this.mSetPackages = null;
            this.mSetClasses = null;
            this.mSetComponents = null;
        }
    }

    public PreferredComponent(Callbacks callbacks, XmlPullParser parser) throws XmlPullParserException, IOException {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        XmlPullParser xmlPullParser = parser;
        int setCount = 0;
        this.mCustPc = (HwCustPreferredComponent) HwCustUtils.createObj(HwCustPreferredComponent.class, new Object[0]);
        this.mCallbacks = callbacks;
        String str = null;
        this.mShortComponent = xmlPullParser.getAttributeValue(null, "name");
        this.mComponent = ComponentName.unflattenFromString(this.mShortComponent);
        if (this.mComponent == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad activity name ");
            stringBuilder.append(this.mShortComponent);
            this.mParseError = stringBuilder.toString();
        }
        String matchStr = xmlPullParser.getAttributeValue(null, ATTR_MATCH);
        this.mMatch = matchStr != null ? Integer.parseInt(matchStr, 16) : 0;
        String setCountStr = xmlPullParser.getAttributeValue(null, "set");
        if (setCountStr != null) {
            setCount = Integer.parseInt(setCountStr);
        }
        String alwaysStr = xmlPullParser.getAttributeValue(null, ATTR_ALWAYS);
        int i = 1;
        this.mAlways = alwaysStr != null ? Boolean.parseBoolean(alwaysStr) : true;
        String[] myPackages = setCount > 0 ? new String[setCount] : null;
        String[] myClasses = setCount > 0 ? new String[setCount] : null;
        String[] myComponents = setCount > 0 ? new String[setCount] : null;
        int setPos = 0;
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            String str2;
            if (next != i) {
                if (type == 3 && parser.getDepth() <= outerDepth) {
                    str2 = matchStr;
                    break;
                }
                if (type == 3) {
                    str2 = matchStr;
                } else if (type == 4) {
                    str2 = matchStr;
                } else {
                    String tagName = parser.getName();
                    if (tagName.equals("set")) {
                        String name = xmlPullParser.getAttributeValue(str, "name");
                        StringBuilder stringBuilder3;
                        if (name == null) {
                            if (this.mParseError == null) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("No name in set tag in preferred activity ");
                                stringBuilder3.append(this.mShortComponent);
                                this.mParseError = stringBuilder3.toString();
                            }
                        } else if (setPos < setCount) {
                            ComponentName cn = ComponentName.unflattenFromString(name);
                            if (cn != null) {
                                str2 = matchStr;
                                myPackages[setPos] = cn.getPackageName();
                                myClasses[setPos] = cn.getClassName();
                                myComponents[setPos] = name;
                                setPos++;
                            } else if (this.mParseError == null) {
                                stringBuilder3 = new StringBuilder();
                                str2 = matchStr;
                                stringBuilder3.append("Bad set name ");
                                stringBuilder3.append(name);
                                stringBuilder3.append(" in preferred activity ");
                                stringBuilder3.append(this.mShortComponent);
                                this.mParseError = stringBuilder3.toString();
                            } else {
                                str2 = matchStr;
                            }
                            XmlUtils.skipCurrentTag(parser);
                        } else if (this.mParseError == null) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Too many set tags in preferred activity ");
                            stringBuilder2.append(this.mShortComponent);
                            this.mParseError = stringBuilder2.toString();
                        }
                        str2 = matchStr;
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        str2 = matchStr;
                        if (!this.mCallbacks.onReadTag(tagName, xmlPullParser)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown element: ");
                            stringBuilder.append(parser.getName());
                            Slog.w("PreferredComponent", stringBuilder.toString());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
                matchStr = str2;
                Callbacks callbacks2 = callbacks;
                str = null;
                i = 1;
            } else {
                str2 = matchStr;
                break;
            }
        }
        if (setPos != setCount && this.mParseError == null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Not enough set tags (expected ");
            stringBuilder2.append(setCount);
            stringBuilder2.append(" but found ");
            stringBuilder2.append(setPos);
            stringBuilder2.append(") in ");
            stringBuilder2.append(this.mShortComponent);
            this.mParseError = stringBuilder2.toString();
        }
        this.mSetPackages = myPackages;
        this.mSetClasses = myClasses;
        this.mSetComponents = myComponents;
    }

    public String getParseError() {
        return this.mParseError;
    }

    public void writeToXml(XmlSerializer serializer, boolean full) throws IOException {
        int s = 0;
        int NS = this.mSetClasses != null ? this.mSetClasses.length : 0;
        serializer.attribute(null, "name", this.mShortComponent);
        if (full) {
            if (this.mMatch != 0) {
                serializer.attribute(null, ATTR_MATCH, Integer.toHexString(this.mMatch));
            }
            serializer.attribute(null, ATTR_ALWAYS, Boolean.toString(this.mAlways));
            serializer.attribute(null, "set", Integer.toString(NS));
            while (s < NS) {
                serializer.startTag(null, "set");
                serializer.attribute(null, "name", this.mSetComponents[s]);
                serializer.endTag(null, "set");
                s++;
            }
        }
    }

    public boolean sameSet(List<ResolveInfo> query) {
        boolean z = true;
        if (this.mSetPackages == null) {
            if (query != null) {
                z = false;
            }
            return z;
        } else if (query == null) {
            return false;
        } else {
            int NQ = query.size();
            int NS = this.mSetPackages.length;
            int numMatch = 0;
            for (int i = 0; i < NQ; i++) {
                ActivityInfo ai = ((ResolveInfo) query.get(i)).activityInfo;
                boolean good = false;
                int j = 0;
                while (j < NS) {
                    if (this.mSetPackages[j].equals(ai.packageName) && this.mSetClasses[j].equals(ai.name)) {
                        numMatch++;
                        good = true;
                        break;
                    }
                    j++;
                }
                if (!good && (this.mCustPc == null || !this.mCustPc.isSkipHwStarupGuide(this.mCallbacks, ai))) {
                    return false;
                }
            }
            if (numMatch != NS) {
                z = false;
            }
            return z;
        }
    }

    public boolean sameSet(ComponentName[] comps) {
        boolean z = false;
        if (this.mSetPackages == null) {
            return false;
        }
        int NS = this.mSetPackages.length;
        int numMatch = 0;
        for (ComponentName cn : comps) {
            boolean good = false;
            int j = 0;
            while (j < NS) {
                if (this.mSetPackages[j].equals(cn.getPackageName()) && this.mSetClasses[j].equals(cn.getClassName())) {
                    numMatch++;
                    good = true;
                    break;
                }
                j++;
            }
            if (!good) {
                return false;
            }
        }
        if (numMatch == NS) {
            z = true;
        }
        return z;
    }

    public boolean isSuperset(List<ResolveInfo> query) {
        boolean z = false;
        if (this.mSetPackages == null) {
            if (query == null) {
                z = true;
            }
            return z;
        } else if (query == null) {
            return true;
        } else {
            int NQ = query.size();
            int NS = this.mSetPackages.length;
            if (NS < NQ) {
                return false;
            }
            for (int i = 0; i < NQ; i++) {
                ActivityInfo ai = ((ResolveInfo) query.get(i)).activityInfo;
                boolean foundMatch = false;
                int j = 0;
                while (j < NS) {
                    if (this.mSetPackages[j].equals(ai.packageName) && this.mSetClasses[j].equals(ai.name)) {
                        foundMatch = true;
                        break;
                    }
                    j++;
                }
                if (!foundMatch) {
                    return false;
                }
            }
            return true;
        }
    }

    public ComponentName[] discardObsoleteComponents(List<ResolveInfo> query) {
        if (this.mSetPackages == null || query == null) {
            return new ComponentName[0];
        }
        int NQ = query.size();
        int NS = this.mSetPackages.length;
        ArrayList<ComponentName> aliveComponents = new ArrayList();
        for (int i = 0; i < NQ; i++) {
            ActivityInfo ai = ((ResolveInfo) query.get(i)).activityInfo;
            int j = 0;
            while (j < NS) {
                if (this.mSetPackages[j].equals(ai.packageName) && this.mSetClasses[j].equals(ai.name)) {
                    aliveComponents.add(new ComponentName(this.mSetPackages[j], this.mSetClasses[j]));
                    break;
                }
                j++;
            }
        }
        return (ComponentName[]) aliveComponents.toArray(new ComponentName[aliveComponents.size()]);
    }

    public void dump(PrintWriter out, String prefix, Object ident) {
        out.print(prefix);
        out.print(Integer.toHexString(System.identityHashCode(ident)));
        out.print(' ');
        out.println(this.mShortComponent);
        out.print(prefix);
        out.print(" mMatch=0x");
        out.print(Integer.toHexString(this.mMatch));
        out.print(" mAlways=");
        out.println(this.mAlways);
        if (this.mSetComponents != null) {
            out.print(prefix);
            out.println("  Selected from:");
            for (String println : this.mSetComponents) {
                out.print(prefix);
                out.print("    ");
                out.println(println);
            }
        }
    }
}
