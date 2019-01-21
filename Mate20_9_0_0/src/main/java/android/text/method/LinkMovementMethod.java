package android.text.method;

import android.text.Layout;
import android.text.NoCopySpan.Concrete;
import android.text.Selection;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.textclassifier.TextLinks.TextLinkSpan;
import android.widget.TextView;

public class LinkMovementMethod extends ScrollingMovementMethod {
    private static final int CLICK = 1;
    private static final int DOWN = 3;
    private static Object FROM_BELOW = new Concrete();
    private static final int HIDE_FLOATING_TOOLBAR_DELAY_MS = 200;
    private static final int UP = 2;
    private static LinkMovementMethod sInstance;

    public boolean canSelectArbitrarily() {
        return true;
    }

    protected boolean handleMovementKey(TextView widget, Spannable buffer, int keyCode, int movementMetaState, KeyEvent event) {
        if ((keyCode == 23 || keyCode == 66) && KeyEvent.metaStateHasNoModifiers(movementMetaState) && event.getAction() == 0 && event.getRepeatCount() == 0 && action(1, widget, buffer)) {
            return true;
        }
        return super.handleMovementKey(widget, buffer, keyCode, movementMetaState, event);
    }

    protected boolean up(TextView widget, Spannable buffer) {
        if (action(2, widget, buffer)) {
            return true;
        }
        return super.up(widget, buffer);
    }

    protected boolean down(TextView widget, Spannable buffer) {
        if (action(3, widget, buffer)) {
            return true;
        }
        return super.down(widget, buffer);
    }

    protected boolean left(TextView widget, Spannable buffer) {
        if (action(2, widget, buffer)) {
            return true;
        }
        return super.left(widget, buffer);
    }

    protected boolean right(TextView widget, Spannable buffer) {
        if (action(3, widget, buffer)) {
            return true;
        }
        return super.right(widget, buffer);
    }

    private boolean action(int what, TextView widget, Spannable buffer) {
        int length;
        View view = widget;
        Spannable spannable = buffer;
        Layout layout = widget.getLayout();
        int padding = widget.getTotalPaddingTop() + widget.getTotalPaddingBottom();
        int areaTop = widget.getScrollY();
        int areaBot = (widget.getHeight() + areaTop) - padding;
        int lineTop = layout.getLineForVertical(areaTop);
        int lineBot = layout.getLineForVertical(areaBot);
        int first = layout.getLineStart(lineTop);
        int last = layout.getLineEnd(lineBot);
        ClickableSpan[] candidates = (ClickableSpan[]) spannable.getSpans(first, last, ClickableSpan.class);
        int a = Selection.getSelectionStart(buffer);
        int b = Selection.getSelectionEnd(buffer);
        int selStart = Math.min(a, b);
        int selEnd = Math.max(a, b);
        if (selStart < 0 && spannable.getSpanStart(FROM_BELOW) >= 0) {
            length = buffer.length();
            selEnd = length;
            selStart = length;
        }
        if (selStart > last) {
            selEnd = Integer.MAX_VALUE;
            selStart = Integer.MAX_VALUE;
        }
        if (selEnd < first) {
            selEnd = -1;
            selStart = -1;
        }
        int i;
        int i2;
        int i3;
        switch (what) {
            case 1:
                i = padding;
                i2 = areaTop;
                i3 = areaBot;
                if (selStart != selEnd) {
                    ClickableSpan[] links = (ClickableSpan[]) spannable.getSpans(selStart, selEnd, ClickableSpan.class);
                    if (links.length == 1) {
                        ClickableSpan link = links[0];
                        if (!(link instanceof TextLinkSpan)) {
                            link.onClick(view);
                            break;
                        }
                        ((TextLinkSpan) link).onClick(view, 1);
                        break;
                    }
                    return false;
                }
                return false;
            case 2:
                i = padding;
                i2 = areaTop;
                areaTop = -1;
                padding = -1;
                layout = null;
                while (layout < candidates.length) {
                    length = spannable.getSpanEnd(candidates[layout]);
                    if (length >= selEnd && selStart != selEnd) {
                        i3 = areaBot;
                    } else if (length > areaTop) {
                        i3 = areaBot;
                        padding = spannable.getSpanStart(candidates[layout]);
                        areaTop = length;
                    } else {
                        i3 = areaBot;
                    }
                    layout++;
                    areaBot = i3;
                }
                if (padding >= 0) {
                    Selection.setSelection(spannable, areaTop, padding);
                    return true;
                }
                break;
            case 3:
                Layout layout2 = layout;
                layout = Integer.MAX_VALUE;
                length = Integer.MAX_VALUE;
                int i4 = 0;
                while (true) {
                    i = padding;
                    i2 = areaTop;
                    areaTop = i4;
                    if (areaTop >= candidates.length) {
                        if (length >= Integer.MAX_VALUE) {
                            i3 = areaBot;
                            break;
                        }
                        Selection.setSelection(spannable, layout, length);
                        return true;
                    }
                    padding = spannable.getSpanStart(candidates[areaTop]);
                    if ((padding > selStart || selStart == selEnd) && padding < layout) {
                        int bestStart = padding;
                        length = spannable.getSpanEnd(candidates[areaTop]);
                        layout = bestStart;
                    }
                    i4 = areaTop + 1;
                    padding = i;
                    areaTop = i2;
                }
            default:
                break;
        }
        return false;
    }

    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();
        if (action == 1 || action == 0) {
            int x = (((int) event.getX()) - widget.getTotalPaddingLeft()) + widget.getScrollX();
            int y = (((int) event.getY()) - widget.getTotalPaddingTop()) + widget.getScrollY();
            Layout layout = widget.getLayout();
            int off = layout.getOffsetForHorizontal(layout.getLineForVertical(y), (float) x);
            ClickableSpan[] links = (ClickableSpan[]) buffer.getSpans(off, off, ClickableSpan.class);
            if (links.length != 0) {
                ClickableSpan link = links[0];
                if (action == 1) {
                    if (link instanceof TextLinkSpan) {
                        ((TextLinkSpan) link).onClick(widget, 0);
                    } else {
                        link.onClick(widget);
                    }
                } else if (action == 0) {
                    if (widget.getContext().getApplicationInfo().targetSdkVersion >= 28) {
                        widget.hideFloatingToolbar(200);
                    }
                    Selection.setSelection(buffer, buffer.getSpanStart(link), buffer.getSpanEnd(link));
                }
                return true;
            }
            Selection.removeSelection(buffer);
        }
        return super.onTouchEvent(widget, buffer, event);
    }

    public void initialize(TextView widget, Spannable text) {
        Selection.removeSelection(text);
        text.removeSpan(FROM_BELOW);
    }

    public void onTakeFocus(TextView view, Spannable text, int dir) {
        Selection.removeSelection(text);
        if ((dir & 1) != 0) {
            text.setSpan(FROM_BELOW, 0, 0, 34);
        } else {
            text.removeSpan(FROM_BELOW);
        }
    }

    public static MovementMethod getInstance() {
        if (sInstance == null) {
            sInstance = new LinkMovementMethod();
        }
        return sInstance;
    }
}
