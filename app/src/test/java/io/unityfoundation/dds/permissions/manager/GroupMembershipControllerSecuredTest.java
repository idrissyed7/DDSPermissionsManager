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
package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserDTO;
import io.unityfoundation.dds.permissions.manager.testing.util.SecurityAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

@MicronautTest
@Property(name = "micronaut.security.reject-not-found", value = StringUtils.FALSE)
class GroupMembershipControllerSecuredTest {
    @Test
    void getApiGroupMembershipIsSecured(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/group_membership"));
    }

    @Test
    void postApiGroupMembershipIsSecured(@Client("/") HttpClient httpClient) throws IOException {
        String bobEmail = "bob.builder@test.test";
        GroupUserDTO dto = new GroupUserDTO();
        dto.setPermissionsGroup(99L);
        dto.setEmail(bobEmail);
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.POST("/api/group_membership", dto));
    }

    @Test
    void putApiGroupMembershipIsSecured(@Client("/") HttpClient httpClient) throws IOException {
        String bobEmail = "bob.builder@test.test";
        GroupUserDTO dto = new GroupUserDTO();
        dto.setPermissionsGroup(99L);
        dto.setEmail(bobEmail);
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.PUT("/api/group_membership", dto));
    }

    @Test
    void deleteApiGroupMembershipIsSecured(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.PUT("/api/group_membership", Collections.emptyMap()));
    }

    @Test
    void getApiGroupMembershipUserExistsIsSecured(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/group_membership/user_exists/99"));
    }
}
