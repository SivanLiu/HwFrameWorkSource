package android.view.textclassifier;

import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.textclassifier.TextClassifier.EntityConfig;
import android.view.textclassifier.TextLinks.TextLink;
import android.view.textclassifier.TextLinks.TextLinkSpan;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public final class TextLinksParams {
    private static final Function<TextLink, TextLinkSpan> DEFAULT_SPAN_FACTORY = -$$Lambda$TextLinksParams$km8pN8nazHT6NQiHykIrRALWbkE.INSTANCE;
    private final int mApplyStrategy;
    private final EntityConfig mEntityConfig;
    private final Function<TextLink, TextLinkSpan> mSpanFactory;

    public static final class Builder {
        private int mApplyStrategy = 0;
        private Function<TextLink, TextLinkSpan> mSpanFactory = TextLinksParams.DEFAULT_SPAN_FACTORY;

        public Builder setApplyStrategy(int applyStrategy) {
            this.mApplyStrategy = TextLinksParams.checkApplyStrategy(applyStrategy);
            return this;
        }

        public Builder setSpanFactory(Function<TextLink, TextLinkSpan> spanFactory) {
            this.mSpanFactory = spanFactory == null ? TextLinksParams.DEFAULT_SPAN_FACTORY : spanFactory;
            return this;
        }

        public Builder setEntityConfig(EntityConfig entityConfig) {
            return this;
        }

        public TextLinksParams build() {
            return new TextLinksParams(this.mApplyStrategy, this.mSpanFactory);
        }
    }

    private TextLinksParams(int applyStrategy, Function<TextLink, TextLinkSpan> spanFactory) {
        this.mApplyStrategy = applyStrategy;
        this.mSpanFactory = spanFactory;
        this.mEntityConfig = EntityConfig.createWithHints(null);
    }

    public static TextLinksParams fromLinkMask(int mask) {
        List<String> entitiesToFind = new ArrayList();
        if ((mask & 1) != 0) {
            entitiesToFind.add("url");
        }
        if ((mask & 2) != 0) {
            entitiesToFind.add("email");
        }
        if ((mask & 4) != 0) {
            entitiesToFind.add("phone");
        }
        if ((mask & 8) != 0) {
            entitiesToFind.add("address");
        }
        return new Builder().setEntityConfig(EntityConfig.createWithExplicitEntityList(entitiesToFind)).build();
    }

    public EntityConfig getEntityConfig() {
        return this.mEntityConfig;
    }

    public int apply(Spannable text, TextLinks textLinks) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(textLinks);
        if (!text.toString().startsWith(textLinks.getText())) {
            return 3;
        }
        if (textLinks.getLinks().isEmpty()) {
            return 1;
        }
        int applyCount = 0;
        Iterator it = textLinks.getLinks().iterator();
        while (true) {
            int i = 0;
            if (!it.hasNext()) {
                break;
            }
            TextLink link = (TextLink) it.next();
            TextLinkSpan span = (TextLinkSpan) this.mSpanFactory.apply(link);
            if (span != null) {
                ClickableSpan[] existingSpans = (ClickableSpan[]) text.getSpans(link.getStart(), link.getEnd(), ClickableSpan.class);
                if (existingSpans.length <= 0) {
                    text.setSpan(span, link.getStart(), link.getEnd(), 33);
                    applyCount++;
                } else if (this.mApplyStrategy == 1) {
                    int length = existingSpans.length;
                    while (i < length) {
                        text.removeSpan(existingSpans[i]);
                        i++;
                    }
                    text.setSpan(span, link.getStart(), link.getEnd(), 33);
                    applyCount++;
                }
            }
        }
        if (applyCount == 0) {
            return 2;
        }
        return 0;
    }

    private static int checkApplyStrategy(int applyStrategy) {
        if (applyStrategy == 0 || applyStrategy == 1) {
            return applyStrategy;
        }
        throw new IllegalArgumentException("Invalid apply strategy. See TextLinksParams.ApplyStrategy for options.");
    }
}
