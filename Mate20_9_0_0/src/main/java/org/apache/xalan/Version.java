package org.apache.xalan;

import org.apache.xalan.templates.Constants;

public class Version {
    public static String getVersion() {
        StringBuilder stringBuilder;
        int developmentVersionNum;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(getProduct());
        stringBuilder2.append(" ");
        stringBuilder2.append(getImplementationLanguage());
        stringBuilder2.append(" ");
        stringBuilder2.append(getMajorVersionNum());
        stringBuilder2.append(Constants.ATTRVAL_THIS);
        stringBuilder2.append(getReleaseVersionNum());
        stringBuilder2.append(Constants.ATTRVAL_THIS);
        if (getDevelopmentVersionNum() > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("D");
            developmentVersionNum = getDevelopmentVersionNum();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("");
            developmentVersionNum = getMaintenanceVersionNum();
        }
        stringBuilder.append(developmentVersionNum);
        stringBuilder2.append(stringBuilder.toString());
        return stringBuilder2.toString();
    }

    public static void main(String[] argv) {
        System.out.println(getVersion());
    }

    public static String getProduct() {
        return "Xalan";
    }

    public static String getImplementationLanguage() {
        return "Java";
    }

    public static int getMajorVersionNum() {
        return 2;
    }

    public static int getReleaseVersionNum() {
        return 7;
    }

    public static int getMaintenanceVersionNum() {
        return 1;
    }

    public static int getDevelopmentVersionNum() {
        try {
            if (new String("").length() == 0) {
                return 0;
            }
            return Integer.parseInt("");
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
