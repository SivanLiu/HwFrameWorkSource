package org.bouncycastle.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bouncycastle.util.Iterable;

public class SignerInformationStore implements Iterable<SignerInformation> {
    private List all;
    private Map table;

    public SignerInformationStore(Collection<SignerInformation> collection) {
        this.all = new ArrayList();
        this.table = new HashMap();
        for (SignerInformation signerInformation : collection) {
            SignerId sid = signerInformation.getSID();
            List list = (ArrayList) this.table.get(sid);
            if (list == null) {
                list = new ArrayList(1);
                this.table.put(sid, list);
            }
            list.add(signerInformation);
        }
        this.all = new ArrayList(collection);
    }

    public SignerInformationStore(SignerInformation signerInformation) {
        this.all = new ArrayList();
        this.table = new HashMap();
        this.all = new ArrayList(1);
        this.all.add(signerInformation);
        this.table.put(signerInformation.getSID(), this.all);
    }

    public SignerInformation get(SignerId signerId) {
        Collection signers = getSigners(signerId);
        return signers.size() == 0 ? null : (SignerInformation) signers.iterator().next();
    }

    public Collection<SignerInformation> getSigners() {
        return new ArrayList(this.all);
    }

    public Collection<SignerInformation> getSigners(SignerId signerId) {
        if (signerId.getIssuer() == null || signerId.getSubjectKeyIdentifier() == null) {
            ArrayList arrayList = (ArrayList) this.table.get(signerId);
            return arrayList == null ? new ArrayList() : new ArrayList(arrayList);
        } else {
            ArrayList arrayList2 = new ArrayList();
            Collection signers = getSigners(new SignerId(signerId.getIssuer(), signerId.getSerialNumber()));
            if (signers != null) {
                arrayList2.addAll(signers);
            }
            Collection signers2 = getSigners(new SignerId(signerId.getSubjectKeyIdentifier()));
            if (signers2 != null) {
                arrayList2.addAll(signers2);
            }
            return arrayList2;
        }
    }

    public Iterator<SignerInformation> iterator() {
        return getSigners().iterator();
    }

    public int size() {
        return this.all.size();
    }
}
