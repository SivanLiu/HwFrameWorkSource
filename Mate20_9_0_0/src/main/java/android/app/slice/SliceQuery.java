package android.app.slice;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SliceQuery {
    private static final String TAG = "SliceQuery";

    public static SliceItem getPrimaryIcon(Slice slice) {
        for (SliceItem item : slice.getItems()) {
            if (Objects.equals(item.getFormat(), SliceItem.FORMAT_IMAGE)) {
                return item;
            }
            if (!((compareTypes(item, "slice") && item.hasHint(Slice.HINT_LIST)) || item.hasHint(Slice.HINT_ACTIONS) || item.hasHint(Slice.HINT_LIST_ITEM) || compareTypes(item, "action"))) {
                SliceItem icon = find(item, SliceItem.FORMAT_IMAGE);
                if (icon != null) {
                    return icon;
                }
            }
        }
        return null;
    }

    public static SliceItem findNotContaining(SliceItem container, List<SliceItem> list) {
        SliceItem ret = null;
        while (ret == null && list.size() != 0) {
            SliceItem remove = (SliceItem) list.remove(0);
            if (!contains(container, remove)) {
                ret = remove;
            }
        }
        return ret;
    }

    private static boolean contains(SliceItem container, SliceItem item) {
        if (container == null || item == null) {
            return false;
        }
        return stream(container).filter(new -$$Lambda$SliceQuery$fdDPNErwIni-vCQ6k-MlGGBunoE(item)).findAny().isPresent();
    }

    static /* synthetic */ boolean lambda$contains$0(SliceItem item, SliceItem s) {
        return s == item;
    }

    public static List<SliceItem> findAll(SliceItem s, String type) {
        return findAll(s, type, null, null);
    }

    public static List<SliceItem> findAll(SliceItem s, String type, String hints, String nonHints) {
        return findAll(s, type, new String[]{hints}, new String[]{nonHints});
    }

    public static List<SliceItem> findAll(SliceItem s, String type, String[] hints, String[] nonHints) {
        return (List) stream(s).filter(new -$$Lambda$SliceQuery$hLToAajdaMbaf8BUtZ8fpGK980E(type, hints, nonHints)).collect(Collectors.toList());
    }

    static /* synthetic */ boolean lambda$findAll$1(String type, String[] hints, String[] nonHints, SliceItem item) {
        return compareTypes(item, type) && item.hasHints(hints) && !item.hasAnyHints(nonHints);
    }

    public static SliceItem find(Slice s, String type, String hints, String nonHints) {
        return find(s, type, new String[]{hints}, new String[]{nonHints});
    }

    public static SliceItem find(Slice s, String type) {
        return find(s, type, null, null);
    }

    public static SliceItem find(SliceItem s, String type) {
        return find(s, type, null, null);
    }

    public static SliceItem find(SliceItem s, String type, String hints, String nonHints) {
        return find(s, type, new String[]{hints}, new String[]{nonHints});
    }

    public static SliceItem find(Slice s, String type, String[] hints, String[] nonHints) {
        List<String> h = s.getHints();
        return find(new SliceItem((Object) s, "slice", null, (String[]) h.toArray(new String[h.size()])), type, hints, nonHints);
    }

    public static SliceItem find(SliceItem s, String type, String[] hints, String[] nonHints) {
        return (SliceItem) stream(s).filter(new -$$Lambda$SliceQuery$cG9kHpHpv4nbm7p3sCvlkQGlqQw(type, hints, nonHints)).findFirst().orElse(null);
    }

    static /* synthetic */ boolean lambda$find$2(String type, String[] hints, String[] nonHints, SliceItem item) {
        return compareTypes(item, type) && item.hasHints(hints) && !item.hasAnyHints(nonHints);
    }

    public static Stream<SliceItem> stream(SliceItem slice) {
        final Queue<SliceItem> items = new LinkedList();
        items.add(slice);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<SliceItem>() {
            public boolean hasNext() {
                return items.size() != 0;
            }

            public SliceItem next() {
                SliceItem item = (SliceItem) items.poll();
                if (SliceQuery.compareTypes(item, "slice") || SliceQuery.compareTypes(item, "action")) {
                    items.addAll(item.getSlice().getItems());
                }
                return item;
            }
        }, 0), false);
    }

    public static boolean compareTypes(SliceItem item, String desiredType) {
        if (desiredType.length() == 3 && desiredType.equals("*/*")) {
            return true;
        }
        if (item.getSubType() == null && desiredType.indexOf(47) < 0) {
            return item.getFormat().equals(desiredType);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(item.getFormat());
        stringBuilder.append("/");
        stringBuilder.append(item.getSubType());
        return stringBuilder.toString().matches(desiredType.replaceAll("\\*", ".*"));
    }
}
