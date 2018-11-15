package com.huawei.odmf.model;

import com.huawei.odmf.model.api.Attribute;
import com.huawei.odmf.model.api.Index;
import java.util.ArrayList;
import java.util.List;

public class AIndex implements Index {
    List<Attribute> compositeIndexAttributes;
    String indexName;

    public AIndex(String indexName, List<Attribute> compositeIndexAttributes) {
        this.indexName = indexName;
        this.compositeIndexAttributes = compositeIndexAttributes;
    }

    public AIndex(String indexName) {
        this(indexName, new ArrayList());
    }

    public List<Attribute> getCompositeIndexAttributes() {
        return this.compositeIndexAttributes;
    }

    public void setCompositeIndexAttributes(List<Attribute> compositeIndexAttributes) {
        this.compositeIndexAttributes = compositeIndexAttributes;
    }

    public boolean addAttribute(Attribute attribute) {
        return this.compositeIndexAttributes.add(attribute);
    }

    public String getIndexName() {
        return this.indexName;
    }
}
