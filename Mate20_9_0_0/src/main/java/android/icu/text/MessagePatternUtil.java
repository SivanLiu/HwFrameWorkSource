package android.icu.text;

import android.icu.text.MessagePattern.ArgType;
import android.icu.text.MessagePattern.Part;
import android.icu.text.MessagePattern.Part.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MessagePatternUtil {

    public static class Node {
        private Node() {
        }
    }

    public static class ComplexArgStyleNode extends Node {
        private ArgType argType;
        private boolean explicitOffset;
        private volatile List<VariantNode> list;
        private double offset;

        public ArgType getArgType() {
            return this.argType;
        }

        public boolean hasExplicitOffset() {
            return this.explicitOffset;
        }

        public double getOffset() {
            return this.offset;
        }

        public List<VariantNode> getVariants() {
            return this.list;
        }

        public VariantNode getVariantsByType(List<VariantNode> numericVariants, List<VariantNode> keywordVariants) {
            if (numericVariants != null) {
                numericVariants.clear();
            }
            keywordVariants.clear();
            VariantNode other = null;
            for (VariantNode variant : this.list) {
                if (variant.isSelectorNumeric()) {
                    numericVariants.add(variant);
                } else if (!PluralRules.KEYWORD_OTHER.equals(variant.getSelector())) {
                    keywordVariants.add(variant);
                } else if (other == null) {
                    other = variant;
                }
            }
            return other;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(this.argType.toString());
            sb.append(" style) ");
            if (hasExplicitOffset()) {
                sb.append("offset:");
                sb.append(this.offset);
                sb.append(' ');
            }
            sb.append(this.list.toString());
            return sb.toString();
        }

        private ComplexArgStyleNode(ArgType argType) {
            super();
            this.list = new ArrayList();
            this.argType = argType;
        }

        private void addVariant(VariantNode variant) {
            this.list.add(variant);
        }

        private ComplexArgStyleNode freeze() {
            this.list = Collections.unmodifiableList(this.list);
            return this;
        }
    }

    public static class MessageContentsNode extends Node {
        private Type type;

        public enum Type {
            TEXT,
            ARG,
            REPLACE_NUMBER
        }

        public Type getType() {
            return this.type;
        }

        public String toString() {
            return "{REPLACE_NUMBER}";
        }

        private MessageContentsNode(Type type) {
            super();
            this.type = type;
        }

        private static MessageContentsNode createReplaceNumberNode() {
            return new MessageContentsNode(Type.REPLACE_NUMBER);
        }
    }

    public static class MessageNode extends Node {
        private volatile List<MessageContentsNode> list;

        public List<MessageContentsNode> getContents() {
            return this.list;
        }

        public String toString() {
            return this.list.toString();
        }

        private MessageNode() {
            super();
            this.list = new ArrayList();
        }

        private void addContentsNode(MessageContentsNode node) {
            if ((node instanceof TextNode) && !this.list.isEmpty()) {
                MessageContentsNode lastNode = (MessageContentsNode) this.list.get(this.list.size() - 1);
                if (lastNode instanceof TextNode) {
                    TextNode textNode = (TextNode) lastNode;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(textNode.text);
                    stringBuilder.append(((TextNode) node).text);
                    textNode.text = stringBuilder.toString();
                    return;
                }
            }
            this.list.add(node);
        }

        private MessageNode freeze() {
            this.list = Collections.unmodifiableList(this.list);
            return this;
        }
    }

    public static class VariantNode extends Node {
        private MessageNode msgNode;
        private double numericValue;
        private String selector;

        public String getSelector() {
            return this.selector;
        }

        public boolean isSelectorNumeric() {
            return this.numericValue != -1.23456789E8d;
        }

        public double getSelectorValue() {
            return this.numericValue;
        }

        public MessageNode getMessage() {
            return this.msgNode;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (isSelectorNumeric()) {
                sb.append(this.numericValue);
                sb.append(" (");
                sb.append(this.selector);
                sb.append(") {");
            } else {
                sb.append(this.selector);
                sb.append(" {");
            }
            sb.append(this.msgNode.toString());
            sb.append('}');
            return sb.toString();
        }

        private VariantNode() {
            super();
            this.numericValue = -1.23456789E8d;
        }
    }

    public static class ArgNode extends MessageContentsNode {
        private ArgType argType;
        private ComplexArgStyleNode complexStyle;
        private String name;
        private int number = -1;
        private String style;
        private String typeName;

        public ArgType getArgType() {
            return this.argType;
        }

        public String getName() {
            return this.name;
        }

        public int getNumber() {
            return this.number;
        }

        public String getTypeName() {
            return this.typeName;
        }

        public String getSimpleStyle() {
            return this.style;
        }

        public ComplexArgStyleNode getComplexStyle() {
            return this.complexStyle;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            sb.append(this.name);
            if (this.argType != ArgType.NONE) {
                sb.append(',');
                sb.append(this.typeName);
                if (this.argType != ArgType.SIMPLE) {
                    sb.append(',');
                    sb.append(this.complexStyle.toString());
                } else if (this.style != null) {
                    sb.append(',');
                    sb.append(this.style);
                }
            }
            sb.append('}');
            return sb.toString();
        }

        private ArgNode() {
            super(Type.ARG);
        }

        private static ArgNode createArgNode() {
            return new ArgNode();
        }
    }

    public static class TextNode extends MessageContentsNode {
        private String text;

        public String getText() {
            return this.text;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("«");
            stringBuilder.append(this.text);
            stringBuilder.append("»");
            return stringBuilder.toString();
        }

        private TextNode(String text) {
            super(Type.TEXT);
            this.text = text;
        }
    }

    private MessagePatternUtil() {
    }

    public static MessageNode buildMessageNode(String patternString) {
        return buildMessageNode(new MessagePattern(patternString));
    }

    public static MessageNode buildMessageNode(MessagePattern pattern) {
        int limit = pattern.countParts() - 1;
        if (limit < 0) {
            throw new IllegalArgumentException("The MessagePattern is empty");
        } else if (pattern.getPartType(0) == Type.MSG_START) {
            return buildMessageNode(pattern, 0, limit);
        } else {
            throw new IllegalArgumentException("The MessagePattern does not represent a MessageFormat pattern");
        }
    }

    private static MessageNode buildMessageNode(MessagePattern pattern, int start, int limit) {
        int prevPatternIndex = pattern.getPart(start).getLimit();
        MessageNode node = new MessageNode();
        int i = start + 1;
        while (true) {
            Part part = pattern.getPart(i);
            int patternIndex = part.getIndex();
            if (prevPatternIndex < patternIndex) {
                node.addContentsNode(new TextNode(pattern.getPatternString().substring(prevPatternIndex, patternIndex)));
            }
            if (i == limit) {
                return node.freeze();
            }
            Type partType = part.getType();
            if (partType == Type.ARG_START) {
                int argLimit = pattern.getLimitPartIndex(i);
                node.addContentsNode(buildArgNode(pattern, i, argLimit));
                i = argLimit;
                part = pattern.getPart(i);
            } else if (partType == Type.REPLACE_NUMBER) {
                node.addContentsNode(MessageContentsNode.createReplaceNumberNode());
            }
            prevPatternIndex = part.getLimit();
            i++;
        }
    }

    private static ArgNode buildArgNode(MessagePattern pattern, int start, int limit) {
        ArgNode node = ArgNode.createArgNode();
        ArgType argType = node.argType = pattern.getPart(start).getArgType();
        start++;
        Part part = pattern.getPart(start);
        node.name = pattern.getSubstring(part);
        if (part.getType() == Type.ARG_NUMBER) {
            node.number = part.getValue();
        }
        start++;
        switch (argType) {
            case SIMPLE:
                int start2 = start + 1;
                node.typeName = pattern.getSubstring(pattern.getPart(start));
                if (start2 < limit) {
                    node.style = pattern.getSubstring(pattern.getPart(start2));
                }
                start = start2;
                break;
            case CHOICE:
                node.typeName = "choice";
                node.complexStyle = buildChoiceStyleNode(pattern, start, limit);
                break;
            case PLURAL:
                node.typeName = "plural";
                node.complexStyle = buildPluralStyleNode(pattern, start, limit, argType);
                break;
            case SELECT:
                node.typeName = "select";
                node.complexStyle = buildSelectStyleNode(pattern, start, limit);
                break;
            case SELECTORDINAL:
                node.typeName = "selectordinal";
                node.complexStyle = buildPluralStyleNode(pattern, start, limit, argType);
                break;
        }
        return node;
    }

    private static ComplexArgStyleNode buildChoiceStyleNode(MessagePattern pattern, int start, int limit) {
        ComplexArgStyleNode node = new ComplexArgStyleNode(ArgType.CHOICE);
        while (start < limit) {
            int valueIndex = start;
            double value = pattern.getNumericValue(pattern.getPart(start));
            start += 2;
            int msgLimit = pattern.getLimitPartIndex(start);
            VariantNode variant = new VariantNode();
            variant.selector = pattern.getSubstring(pattern.getPart(valueIndex + 1));
            variant.numericValue = value;
            variant.msgNode = buildMessageNode(pattern, start, msgLimit);
            node.addVariant(variant);
            start = msgLimit + 1;
        }
        return node.freeze();
    }

    private static ComplexArgStyleNode buildPluralStyleNode(MessagePattern pattern, int selector, int limit, ArgType argType) {
        Part selector2;
        ComplexArgStyleNode node = new ComplexArgStyleNode(argType);
        Part offset = pattern.getPart(selector2);
        if (offset.getType().hasNumericValue()) {
            node.explicitOffset = true;
            node.offset = pattern.getNumericValue(offset);
            selector2 = selector2 + 1;
        }
        while (selector2 < limit) {
            int start = selector2 + 1;
            selector2 = pattern.getPart(selector2);
            double value = -1.23456789E8d;
            Part part = pattern.getPart(start);
            if (part.getType().hasNumericValue()) {
                value = pattern.getNumericValue(part);
                start++;
            }
            int msgLimit = pattern.getLimitPartIndex(start);
            VariantNode variant = new VariantNode();
            variant.selector = pattern.getSubstring(selector2);
            variant.numericValue = value;
            variant.msgNode = buildMessageNode(pattern, start, msgLimit);
            node.addVariant(variant);
            selector2 = msgLimit + 1;
        }
        return node.freeze();
    }

    private static ComplexArgStyleNode buildSelectStyleNode(MessagePattern pattern, int selector, int limit) {
        ComplexArgStyleNode node = new ComplexArgStyleNode(ArgType.SELECT);
        Part selector2;
        while (selector2 < limit) {
            int start = selector2 + 1;
            selector2 = pattern.getPart(selector2);
            int msgLimit = pattern.getLimitPartIndex(start);
            VariantNode variant = new VariantNode();
            variant.selector = pattern.getSubstring(selector2);
            variant.msgNode = buildMessageNode(pattern, start, msgLimit);
            node.addVariant(variant);
            selector2 = msgLimit + 1;
        }
        return node.freeze();
    }
}
