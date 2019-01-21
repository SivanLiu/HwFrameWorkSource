package java.text;

public final class Bidi {
    public static final int DIRECTION_DEFAULT_LEFT_TO_RIGHT = -2;
    public static final int DIRECTION_DEFAULT_RIGHT_TO_LEFT = -1;
    public static final int DIRECTION_LEFT_TO_RIGHT = 0;
    public static final int DIRECTION_RIGHT_TO_LEFT = 1;
    private final android.icu.text.Bidi bidiBase;

    private static int translateConstToIcu(int javaInt) {
        switch (javaInt) {
            case -2:
                return 126;
            case -1:
                return 127;
            case 0:
                return 0;
            case 1:
                return 1;
            default:
                return 0;
        }
    }

    public Bidi(String paragraph, int flags) {
        if (paragraph != null) {
            this.bidiBase = new android.icu.text.Bidi(paragraph.toCharArray(), 0, null, 0, paragraph.length(), translateConstToIcu(flags));
            return;
        }
        throw new IllegalArgumentException("paragraph is null");
    }

    public Bidi(AttributedCharacterIterator paragraph) {
        if (paragraph != null) {
            this.bidiBase = new android.icu.text.Bidi(paragraph);
            return;
        }
        throw new IllegalArgumentException("paragraph is null");
    }

    public Bidi(char[] text, int textStart, byte[] embeddings, int embStart, int paragraphLength, int flags) {
        StringBuilder stringBuilder;
        if (text == null) {
            throw new IllegalArgumentException("text is null");
        } else if (paragraphLength < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("bad length: ");
            stringBuilder.append(paragraphLength);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (textStart < 0 || paragraphLength > text.length - textStart) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("bad range: ");
            stringBuilder.append(textStart);
            stringBuilder.append(" length: ");
            stringBuilder.append(paragraphLength);
            stringBuilder.append(" for text of length: ");
            stringBuilder.append(text.length);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (embeddings == null || (embStart >= 0 && paragraphLength <= embeddings.length - embStart)) {
            this.bidiBase = new android.icu.text.Bidi(text, textStart, embeddings, embStart, paragraphLength, translateConstToIcu(flags));
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("bad range: ");
            stringBuilder.append(embStart);
            stringBuilder.append(" length: ");
            stringBuilder.append(paragraphLength);
            stringBuilder.append(" for embeddings of length: ");
            stringBuilder.append(text.length);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private Bidi(android.icu.text.Bidi bidiBase) {
        this.bidiBase = bidiBase;
    }

    public Bidi createLineBidi(int lineStart, int lineLimit) {
        if (lineStart < 0 || lineLimit < 0 || lineStart > lineLimit || lineLimit > getLength()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid ranges (start=");
            stringBuilder.append(lineStart);
            stringBuilder.append(", limit=");
            stringBuilder.append(lineLimit);
            stringBuilder.append(", length=");
            stringBuilder.append(getLength());
            stringBuilder.append(")");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (lineStart == lineLimit) {
            return new Bidi(new android.icu.text.Bidi(new char[0], 0, new byte[0], 0, 0, translateConstToIcu(0)));
        } else {
            return new Bidi(this.bidiBase.createLineBidi(lineStart, lineLimit));
        }
    }

    public boolean isMixed() {
        return this.bidiBase.isMixed();
    }

    public boolean isLeftToRight() {
        return this.bidiBase.isLeftToRight();
    }

    public boolean isRightToLeft() {
        return this.bidiBase.isRightToLeft();
    }

    public int getLength() {
        return this.bidiBase.getLength();
    }

    public boolean baseIsLeftToRight() {
        return this.bidiBase.baseIsLeftToRight();
    }

    public int getBaseLevel() {
        return this.bidiBase.getParaLevel();
    }

    public int getLevelAt(int offset) {
        try {
            return this.bidiBase.getLevelAt(offset);
        } catch (IllegalArgumentException e) {
            return getBaseLevel();
        }
    }

    public int getRunCount() {
        int runCount = this.bidiBase.countRuns();
        return runCount == 0 ? 1 : runCount;
    }

    public int getRunLevel(int run) {
        if (run == getRunCount()) {
            return getBaseLevel();
        }
        return this.bidiBase.countRuns() == 0 ? this.bidiBase.getBaseLevel() : this.bidiBase.getRunLevel(run);
    }

    public int getRunStart(int run) {
        if (run == getRunCount()) {
            return getBaseLevel();
        }
        return this.bidiBase.countRuns() == 0 ? 0 : this.bidiBase.getRunStart(run);
    }

    public int getRunLimit(int run) {
        if (run == getRunCount()) {
            return getBaseLevel();
        }
        return this.bidiBase.countRuns() == 0 ? this.bidiBase.getLength() : this.bidiBase.getRunLimit(run);
    }

    public static boolean requiresBidi(char[] text, int start, int limit) {
        if (start >= 0 && start <= limit && limit <= text.length) {
            return android.icu.text.Bidi.requiresBidi(text, start, limit);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Value start ");
        stringBuilder.append(start);
        stringBuilder.append(" is out of range 0 to ");
        stringBuilder.append(limit);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static void reorderVisually(byte[] levels, int levelStart, Object[] objects, int objectStart, int count) {
        StringBuilder stringBuilder;
        if (levelStart < 0 || levels.length <= levelStart) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Value levelStart ");
            stringBuilder.append(levelStart);
            stringBuilder.append(" is out of range 0 to ");
            stringBuilder.append(levels.length - 1);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (objectStart < 0 || objects.length <= objectStart) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Value objectStart ");
            stringBuilder.append(levelStart);
            stringBuilder.append(" is out of range 0 to ");
            stringBuilder.append(objects.length - 1);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (count < 0 || objects.length < objectStart + count) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Value count ");
            stringBuilder.append(levelStart);
            stringBuilder.append(" is out of range 0 to ");
            stringBuilder.append(objects.length - objectStart);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            android.icu.text.Bidi.reorderVisually(levels, levelStart, objects, objectStart, count);
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getName());
        stringBuilder.append("[direction: ");
        stringBuilder.append(this.bidiBase.getDirection());
        stringBuilder.append(" baseLevel: ");
        stringBuilder.append(this.bidiBase.getBaseLevel());
        stringBuilder.append(" length: ");
        stringBuilder.append(this.bidiBase.getLength());
        stringBuilder.append(" runs: ");
        stringBuilder.append(this.bidiBase.getRunCount());
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
