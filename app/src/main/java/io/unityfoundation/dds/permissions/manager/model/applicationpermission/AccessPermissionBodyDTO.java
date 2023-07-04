package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.core.annotation.Introspected;
import io.unityfoundation.dds.permissions.manager.model.EntityDTO;

import java.util.Set;

@Introspected
public class AccessPermissionBodyDTO implements EntityDTO {

    private Set<String> partitions;

    public AccessPermissionBodyDTO(Set<String> partitions) {
        this.partitions = partitions;
    }

    public Set<String> getPartitions() {
        return partitions;
    }

    public void setPartitions(Set<String> partitions) {
        this.partitions = partitions;
    }
}
