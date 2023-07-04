package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import javax.persistence.*;

@Entity
@Table(name = "permissions_partition")
public class Partition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "permissions_application_permission_id")
    private ApplicationPermission applicationPermission;

    private String partition;

    public Partition() {
    }

    public Partition(ApplicationPermission applicationPermission, String partition) {
        this.applicationPermission = applicationPermission;
        this.partition = partition;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ApplicationPermission getApplicationPermission() {
        return applicationPermission;
    }

    public void setApplicationPermission(ApplicationPermission applicationPermission) {
        this.applicationPermission = applicationPermission;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }
}
