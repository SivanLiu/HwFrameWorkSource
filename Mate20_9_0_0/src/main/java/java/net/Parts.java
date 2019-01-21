package java.net;

/* compiled from: URL */
class Parts {
    String path;
    String query;
    String ref;

    Parts(String file, String host) {
        int ind = file.indexOf(35);
        this.ref = ind < 0 ? null : file.substring(ind + 1);
        file = ind < 0 ? file : file.substring(0, ind);
        int q = file.lastIndexOf(63);
        if (q != -1) {
            this.query = file.substring(q + 1);
            this.path = file.substring(0, q);
        } else {
            this.path = file;
        }
        if (this.path != null && this.path.length() > 0 && this.path.charAt(0) != '/' && host != null && !host.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('/');
            stringBuilder.append(this.path);
            this.path = stringBuilder.toString();
        }
    }

    String getPath() {
        return this.path;
    }

    String getQuery() {
        return this.query;
    }

    String getRef() {
        return this.ref;
    }
}
