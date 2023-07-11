package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import javax.persistence.*;

@Entity
@Table(name = "permissions_partition")
public class ReadPartition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "permissions_application_permission_id")
    private ApplicationPermission rApplicationPermission;

    private String partitionName;

    public ReadPartition() {
    }

    public ReadPartition(ApplicationPermission applicationPermission, String partitionName) {
        this.rApplicationPermission = applicationPermission;
        this.partitionName = partitionName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ApplicationPermission getApplicationPermission() {
        return rApplicationPermission;
    }

    public void setApplicationPermission(ApplicationPermission applicationPermission) {
        this.rApplicationPermission = applicationPermission;
    }

    public String getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }
}
