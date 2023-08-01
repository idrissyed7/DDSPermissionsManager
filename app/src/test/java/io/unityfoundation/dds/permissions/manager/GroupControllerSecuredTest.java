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
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.group.SimpleGroupDTO;
import io.unityfoundation.dds.permissions.manager.testing.util.SecurityAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

@Property(name = "micronaut.security.reject-not-found", value = StringUtils.FALSE)
@MicronautTest
class GroupControllerSecuredTest {
    @Test
    void groupControllerRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        URI uri = UriBuilder.of("/api").path("groups").queryParam("filter", "foo").build();
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET(uri));
    }

    @Test
    void groupControllerSearchRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        URI uri = UriBuilder.of("/api").path("groups").path("search").path("foo").build();
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET(uri));
    }

    @Test
    void deleteGroupControllerDeleteRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        URI uri = UriBuilder.of("/api").path("/groups").path("99").build();
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.DELETE(uri, Collections.emptyMap()));
    }

    @Test
    void postGroupControllerSaveRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SimpleGroupDTO group = new SimpleGroupDTO();
        group.setName("Beta");
        group.setDescription("myDescription");
        group.setPublic(true);
        URI uri = UriBuilder.of("/api").path("/groups").path("save").build();
        HttpRequest<?> request = HttpRequest.POST(uri, group);
        SecurityAssertions.assertUnauthorized(httpClient, request);
    }
}
