package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.core.annotation.Introspected;
import io.unityfoundation.dds.permissions.manager.model.EntityDTO;

import java.util.Set;

@Introspected
public class AccessPermissionBodyDTO implements EntityDTO {

    private Set<String> readPartitions;
    private Set<String> writePartitions;

    public AccessPermissionBodyDTO(Set<String> readPartitions, Set<String> writePartitions) {
        this.readPartitions = readPartitions;
        this.writePartitions = writePartitions;
    }

    public Set<String> getReadPartitions() {
        return readPartitions;
    }

    public void setReadPartitions(Set<String> readPartitions) {
        this.readPartitions = readPartitions;
    }

    public Set<String> getWritePartitions() {
        return writePartitions;
    }

    public void setWritePartitions(Set<String> writePartitions) {
        this.writePartitions = writePartitions;
    }
}
