package android.content;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class UriMatcher {
    private static final int EXACT = 0;
    public static final int NO_MATCH = -1;
    private static final int NUMBER = 1;
    private static final int TEXT = 2;
    private ArrayList<UriMatcher> mChildren;
    private int mCode;
    private String mText;
    private int mWhich;

    public UriMatcher(int code) {
        this.mCode = code;
        this.mWhich = -1;
        this.mChildren = new ArrayList();
        this.mText = null;
    }

    private UriMatcher() {
        this.mCode = -1;
        this.mWhich = -1;
        this.mChildren = new ArrayList();
        this.mText = null;
    }

    public void addURI(String authority, String path, int code) {
        if (code >= 0) {
            String[] tokens = null;
            if (path != null) {
                String newPath = path;
                if (path.length() > 1 && path.charAt(0) == '/') {
                    newPath = path.substring(1);
                }
                tokens = newPath.split("/");
            }
            int numTokens = tokens != null ? tokens.length : 0;
            UriMatcher node = this;
            int i = -1;
            while (i < numTokens) {
                UriMatcher child;
                String token = i < 0 ? authority : tokens[i];
                ArrayList<UriMatcher> children = node.mChildren;
                int numChildren = children.size();
                int j = 0;
                while (j < numChildren) {
                    child = (UriMatcher) children.get(j);
                    if (token.equals(child.mText)) {
                        node = child;
                        break;
                    }
                    j++;
                }
                if (j == numChildren) {
                    child = new UriMatcher();
                    if (token.equals("#")) {
                        child.mWhich = 1;
                    } else if (token.equals("*")) {
                        child.mWhich = 2;
                    } else {
                        child.mWhich = 0;
                    }
                    child.mText = token;
                    node.mChildren.add(child);
                    node = child;
                }
                i++;
            }
            node.mCode = code;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("code ");
        stringBuilder.append(code);
        stringBuilder.append(" is invalid: it must be positive");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:34:0x0074 A:{LOOP_END, LOOP:0: B:7:0x001c->B:34:0x0074} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x0073 A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int match(Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        int li = pathSegments.size();
        if (li == 0 && uri.getAuthority() == null) {
            return this.mCode;
        }
        UriMatcher node = this;
        int i = -1;
        while (i < li) {
            String u = i < 0 ? uri.getAuthority() : (String) pathSegments.get(i);
            ArrayList<UriMatcher> list = node.mChildren;
            if (list == null) {
                return node.mCode;
            }
            int lj = list.size();
            UriMatcher node2 = null;
            int j = 0;
            while (j < lj) {
                UriMatcher n = (UriMatcher) list.get(j);
                switch (n.mWhich) {
                    case 0:
                        if (n.mText.equals(u)) {
                            node2 = n;
                            break;
                        }
                        break;
                    case 1:
                        int lk = u.length();
                        int k = 0;
                        while (k < lk) {
                            char c = u.charAt(k);
                            if (c >= '0' && c <= '9') {
                                k++;
                            }
                        }
                        node2 = n;
                        break;
                    case 2:
                        node2 = n;
                        break;
                }
                if (node2 != null) {
                    node = node2;
                    if (node != null) {
                        return -1;
                    }
                    i++;
                } else {
                    j++;
                }
            }
            node = node2;
            if (node != null) {
            }
        }
        return node.mCode;
    }
}
