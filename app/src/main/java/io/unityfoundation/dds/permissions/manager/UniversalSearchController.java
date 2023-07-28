// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Page;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.search.SearchResponseDTO;
import io.unityfoundation.dds.permissions.manager.search.UniversalSearchParams;
import io.unityfoundation.dds.permissions.manager.search.UniversalSearchService;

import javax.validation.Valid;

@Controller("/api/search")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "Search")
public class UniversalSearchController {

    private final UniversalSearchService searchService;

    public UniversalSearchController(UniversalSearchService searchService) {
        this.searchService = searchService;
    }

    @Get
    @ExecuteOn(TaskExecutors.IO)
    public Page<SearchResponseDTO> index(@Valid @RequestBean UniversalSearchParams searchParams) {
        return searchService.search(searchParams);
    }
}
