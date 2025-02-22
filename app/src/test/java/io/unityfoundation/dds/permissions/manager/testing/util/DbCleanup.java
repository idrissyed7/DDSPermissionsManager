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
package io.unityfoundation.dds.permissions.manager.testing.util;

import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionRepository;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;

@Singleton
public class DbCleanup {
    private final TopicRepository topicRepository;
    private final GroupRepository groupRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicationPermissionRepository applicationPermissionRepository;
    private final GroupUserRepository groupUserRepository;

    public DbCleanup(TopicRepository topicRepository, GroupRepository groupRepository, ApplicationRepository applicationRepository, UserRepository userRepository, ApplicationPermissionRepository applicationPermissionRepository, GroupUserRepository groupUserRepository) {
        this.topicRepository = topicRepository;
        this.groupRepository = groupRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.applicationPermissionRepository = applicationPermissionRepository;
        this.groupUserRepository = groupUserRepository;
    }

    @Transactional
    public void cleanup() {
        groupUserRepository.deleteAll();
        applicationPermissionRepository.deleteAll();
        topicRepository.deleteAll();
        applicationRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }
}