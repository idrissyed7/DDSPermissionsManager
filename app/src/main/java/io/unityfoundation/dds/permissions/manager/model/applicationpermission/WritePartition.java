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

    private String partition;

    public WritePartition() {
    }

    public WritePartition(ApplicationPermission applicationPermission, String partition) {
        this.wApplicationPermission = applicationPermission;
        this.partition = partition;
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

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }
}
