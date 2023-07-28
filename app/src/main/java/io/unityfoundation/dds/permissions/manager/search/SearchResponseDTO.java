// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.search;

import io.micronaut.core.annotation.Introspected;
import io.unityfoundation.dds.permissions.manager.model.DPMEntity;
import io.unityfoundation.dds.permissions.manager.model.EntityDTO;


@Introspected
public class SearchResponseDTO {

    private DPMEntity type;
    private EntityDTO entity;

    public SearchResponseDTO(DPMEntity type, EntityDTO entity) {
        this.type = type;
        this.entity = entity;
    }

    public DPMEntity getType() {
        return type;
    }

    public void setType(DPMEntity type) {
        this.type = type;
    }

    public EntityDTO getEntity() {
        return entity;
    }

    public void setEntity(EntityDTO entity) {
        this.entity = entity;
    }
}
