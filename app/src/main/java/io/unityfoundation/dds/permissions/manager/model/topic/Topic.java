// Copyright 2023 DDS Permissions Manager Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package io.unityfoundation.dds.permissions.manager.model.topic;


import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.unityfoundation.dds.permissions.manager.model.group.Group;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Entity
@Table(name = "permissions_topic")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @NotBlank
    @Size(min = 3)
    private String name;

    @NonNull
    private TopicKind kind;

    @Size(max = 4000)
    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean makePublic = false;

    @ManyToOne
    @JoinColumn(name = "permissions_group_id", nullable = false)
    private Group permissionsGroup;

    public Topic() {
    }

    public Topic(@NonNull String name, @NonNull TopicKind kind, String description, Boolean isPublic) {
        this.name = name;
        this.kind = kind;
        this.description = description;
        this.makePublic = isPublic;
    }

    public Topic(@NonNull String name, @NonNull TopicKind kind, Group permissionsGroup) {
        this.name = name;
        this.kind = kind;
        this.permissionsGroup = permissionsGroup;
    }

    public Topic(@NonNull String name, @NonNull TopicKind kind, String description, Boolean isPublic, Group permissionsGroup) {
        this.name = name;
        this.kind = kind;
        this.description = description;
        this.makePublic = isPublic;
        this.permissionsGroup = permissionsGroup;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public TopicKind getKind() {
        return kind;
    }

    public void setKind(@NonNull TopicKind kind) {
        this.kind = kind;
    }

    public Group getPermissionsGroup() {
        return permissionsGroup;
    }

    public void setPermissionsGroup(Group permissionsGroup) {
        this.permissionsGroup = permissionsGroup;
    }

    @PrePersist
    void trimName() {
        this.name = this.name.trim();
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public boolean getMakePublic() {
        return makePublic;
    }

    public void setMakePublic(boolean isPublic) {
        makePublic = isPublic;
    }

    public String deriveCanonicalName() {
        return kind + "." + permissionsGroup.getId() + "." + name;
    }
}
