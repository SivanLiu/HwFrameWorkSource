package com.huawei.odmf.model;

import com.huawei.odmf.model.api.Attribute;
import com.huawei.odmf.model.api.Index;
import java.util.ArrayList;
import java.util.List;

public class AIndex implements Index {
    private List<Attribute> compositeIndexAttributes;
    private String indexName;

    AIndex(String indexName2, List<Attribute> compositeIndexAttributes2) {
        this.indexName = indexName2;
        this.compositeIndexAttributes = compositeIndexAttributes2;
    }

    AIndex(String indexName2) {
        this(indexName2, new ArrayList());
    }

    @Override // com.huawei.odmf.model.api.Index
    public List<Attribute> getCompositeIndexAttributes() {
        return this.compositeIndexAttributes;
    }

    @Override // com.huawei.odmf.model.api.Index
    public void setCompositeIndexAttributes(List<Attribute> compositeIndexAttributes2) {
        this.compositeIndexAttributes = compositeIndexAttributes2;
    }

    @Override // com.huawei.odmf.model.api.Index
    public boolean addAttribute(Attribute attribute) {
        return this.compositeIndexAttributes.add(attribute);
    }

    @Override // com.huawei.odmf.model.api.Index
    public String getIndexName() {
        return this.indexName;
    }
}
