package com.android.uiautomator.core;

@Deprecated
public class UiCollection extends UiObject {
    public UiCollection(UiSelector selector) {
        super(selector);
    }

    public UiObject getChildByDescription(UiSelector childPattern, String text) throws UiObjectNotFoundException {
        r0 = new Object[2];
        int x = 0;
        r0[0] = childPattern;
        r0[1] = text;
        Tracer.trace(r0);
        if (text != null) {
            int count = getChildCount(childPattern);
            while (x < count) {
                UiObject row = getChildByInstance(childPattern, x);
                String nodeDesc = row.getContentDescription();
                if ((nodeDesc != null && nodeDesc.contains(text)) || row.getChild(new UiSelector().descriptionContains(text)).exists()) {
                    return row;
                }
                x++;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("for description= \"");
        stringBuilder.append(text);
        stringBuilder.append("\"");
        throw new UiObjectNotFoundException(stringBuilder.toString());
    }

    public UiObject getChildByInstance(UiSelector childPattern, int instance) throws UiObjectNotFoundException {
        Tracer.trace(childPattern, Integer.valueOf(instance));
        return new UiObject(UiSelector.patternBuilder(getSelector(), UiSelector.patternBuilder(childPattern).instance(instance)));
    }

    public UiObject getChildByText(UiSelector childPattern, String text) throws UiObjectNotFoundException {
        r0 = new Object[2];
        int x = 0;
        r0[0] = childPattern;
        r0[1] = text;
        Tracer.trace(r0);
        if (text != null) {
            int count = getChildCount(childPattern);
            while (x < count) {
                UiObject row = getChildByInstance(childPattern, x);
                if (text.equals(row.getText()) || row.getChild(new UiSelector().text(text)).exists()) {
                    return row;
                }
                x++;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("for text= \"");
        stringBuilder.append(text);
        stringBuilder.append("\"");
        throw new UiObjectNotFoundException(stringBuilder.toString());
    }

    public int getChildCount(UiSelector childPattern) {
        Tracer.trace(childPattern);
        return getQueryController().getPatternCount(UiSelector.patternBuilder(getSelector(), UiSelector.patternBuilder(childPattern)));
    }
}
