package org.bouncycastle.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.util.Iterable;

public class RecipientInformationStore implements Iterable<RecipientInformation> {
    private final List all;
    private final Map table;

    public RecipientInformationStore(Collection<RecipientInformation> collection) {
        this.table = new HashMap();
        for (RecipientInformation recipientInformation : collection) {
            RecipientId rid = recipientInformation.getRID();
            List list = (ArrayList) this.table.get(rid);
            if (list == null) {
                list = new ArrayList(1);
                this.table.put(rid, list);
            }
            list.add(recipientInformation);
        }
        this.all = new ArrayList(collection);
    }

    public RecipientInformationStore(RecipientInformation recipientInformation) {
        this.table = new HashMap();
        this.all = new ArrayList(1);
        this.all.add(recipientInformation);
        this.table.put(recipientInformation.getRID(), this.all);
    }

    public RecipientInformation get(RecipientId recipientId) {
        Collection recipients = getRecipients(recipientId);
        return recipients.size() == 0 ? null : (RecipientInformation) recipients.iterator().next();
    }

    public Collection<RecipientInformation> getRecipients() {
        return new ArrayList(this.all);
    }

    public Collection<Recipient> getRecipients(RecipientId recipientId) {
        if (recipientId instanceof KeyTransRecipientId) {
            KeyTransRecipientId keyTransRecipientId = (KeyTransRecipientId) recipientId;
            X500Name issuer = keyTransRecipientId.getIssuer();
            byte[] subjectKeyIdentifier = keyTransRecipientId.getSubjectKeyIdentifier();
            if (!(issuer == null || subjectKeyIdentifier == null)) {
                Collection arrayList = new ArrayList();
                Collection recipients = getRecipients(new KeyTransRecipientId(issuer, keyTransRecipientId.getSerialNumber()));
                if (recipients != null) {
                    arrayList.addAll(recipients);
                }
                recipients = getRecipients(new KeyTransRecipientId(subjectKeyIdentifier));
                if (recipients != null) {
                    arrayList.addAll(recipients);
                }
                return arrayList;
            }
        }
        ArrayList arrayList2 = (ArrayList) this.table.get(recipientId);
        return arrayList2 == null ? new ArrayList() : new ArrayList(arrayList2);
    }

    public Iterator<RecipientInformation> iterator() {
        return getRecipients().iterator();
    }

    public int size() {
        return this.all.size();
    }
}
