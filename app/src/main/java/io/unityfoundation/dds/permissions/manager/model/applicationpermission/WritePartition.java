// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import javax.persistence.*;

@Entity
@Table(name = "permissions_partition")
public class WritePartition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "permissions_application_permission_id")
    private ApplicationPermission wApplicationPermission;

    private String partitionName;

    public WritePartition() {
    }

    public WritePartition(ApplicationPermission applicationPermission, String partitionName) {
        this.wApplicationPermission = applicationPermission;
        this.partitionName = partitionName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ApplicationPermission getApplicationPermission() {
        return wApplicationPermission;
    }

    public void setApplicationPermission(ApplicationPermission applicationPermission) {
        this.wApplicationPermission = applicationPermission;
    }

    public String getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }
}
