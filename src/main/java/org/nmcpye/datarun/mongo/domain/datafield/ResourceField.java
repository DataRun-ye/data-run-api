package org.nmcpye.datarun.mongo.domain.datafield;

import jakarta.validation.constraints.NotNull;
import org.nmcpye.datarun.datatemplateelement.enumeration.ResourceTransactionType;
import org.springframework.data.mongodb.core.mapping.Field;

public class ResourceField extends DefaultField {
    @NotNull
    @Field("resourceUid")
    private String resourceUid;

    // "IN/OUT" with assignment resource mapping,
    @Field("transactionType")
    private ResourceTransactionType transactionType;

    public String getResourceUid() {
        return resourceUid;
    }

    public void setResourceUid(String resourceUid) {
        this.resourceUid = resourceUid;
    }

    public ResourceTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(ResourceTransactionType transactionType) {
        this.transactionType = transactionType;
    }
}
