package io.unityfoundation.dds.permissions.manager.search;

import io.micronaut.core.annotation.Introspected;
import io.unityfoundation.dds.permissions.manager.model.DPDEntity;
import io.unityfoundation.dds.permissions.manager.model.EntityDTO;


@Introspected
public class SearchResponseDTO {

    private DPDEntity type;
    private EntityDTO entity;

    public SearchResponseDTO(DPDEntity type, EntityDTO entity) {
        this.type = type;
        this.entity = entity;
    }

    public DPDEntity getType() {
        return type;
    }

    public void setType(DPDEntity type) {
        this.type = type;
    }

    public EntityDTO getEntity() {
        return entity;
    }

    public void setEntity(EntityDTO entity) {
        this.entity = entity;
    }
}
