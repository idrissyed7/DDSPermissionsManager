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

    private String partition;

    public ReadPartition() {
    }

    public ReadPartition(ApplicationPermission applicationPermission, String partition) {
        this.rApplicationPermission = applicationPermission;
        this.partition = partition;
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

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }
}
