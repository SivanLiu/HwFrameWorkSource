package com.android.server.pm;

class IntentFilterVerificationKey {
    public String className;
    public String domains;
    public String packageName;

    public IntentFilterVerificationKey(String[] domains, String packageName, String className) {
        StringBuilder sb = new StringBuilder();
        for (String host : domains) {
            sb.append(host);
        }
        this.domains = sb.toString();
        this.packageName = packageName;
        this.className = className;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IntentFilterVerificationKey that = (IntentFilterVerificationKey) o;
        if (!this.domains == null ? this.domains.equals(that.domains) : that.domains == null) {
            return false;
        }
        if (!this.className == null ? this.className.equals(that.className) : that.className == null) {
            return false;
        }
        if (this.packageName == null ? that.packageName == null : this.packageName.equals(that.packageName)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int i = 0;
        int result = 31 * ((31 * (this.domains != null ? this.domains.hashCode() : 0)) + (this.packageName != null ? this.packageName.hashCode() : 0));
        if (this.className != null) {
            i = this.className.hashCode();
        }
        return result + i;
    }
}
