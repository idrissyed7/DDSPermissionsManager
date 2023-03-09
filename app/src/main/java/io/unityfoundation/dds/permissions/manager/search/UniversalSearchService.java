package io.unityfoundation.dds.permissions.manager.search;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;

public class UniversalSearchService {

    private final GroupRepository groupRepository;

    public UniversalSearchService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }


    public Page<SearchResponseDTO> search(UniversalSearchParams searchParams) {
        Pageable pageable;
        if (searchParams.getPageable() == null) {
            pageable = Pageable.from(0, 3);
        } else {
            Pageable searchPageable = searchParams.getPageable();
            int size = searchPageable.getSize();
            long rounded = Math.round((double)size / 3);
            pageable = Pageable.from(searchPageable.getNumber(), (int) rounded, searchPageable.getSort());
        }

        // query all three entities in a pageable manner...
        // todo can't progress due to unmerged code.....
//        groupRepository.findAllBy

        return null;
    }
}
