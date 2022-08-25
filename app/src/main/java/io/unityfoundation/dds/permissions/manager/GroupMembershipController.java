package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserDTO;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;

import java.util.Map;
import java.util.Optional;

@Controller("/group_membership")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class GroupMembershipController {

    private final GroupUserService groupUserService;

    public GroupMembershipController(GroupUserService groupUserService) {
        this.groupUserService = groupUserService;
    }

    @Post
    HttpResponse addMember(@Body GroupUserDTO dto) {

        if (groupUserService.isAdminOrGroupAdmin(dto.getPermissionsGroup())) {
            return groupUserService.addMember(dto);
        } else {
            return HttpResponse.unauthorized();
        }
    }

    @Put
    HttpResponse updateMember(@Body GroupUser groupUser) {

        if (groupUserService.isAdminOrGroupAdmin(groupUser.getPermissionsGroup())) {
            return groupUserService.updateMember(groupUser);
        } else {
            return HttpResponse.unauthorized();
        }
    }

    @Delete
    HttpResponse removeMember(@Body Map payload) {
        Long id = Long.valueOf((Integer) payload.get("id"));
        Optional<GroupUser> groupUser = groupUserService.findById(id);

        if (groupUser.isPresent()) {
            Long groupId = groupUser.get().getPermissionsGroup();
            if (groupUserService.isAdminOrGroupAdmin(groupId)) {
                if (groupUserService.removeMember(id)) {
                    return HttpResponse.ok();
                }
            } else {
                return HttpResponse.unauthorized();
            }
        }

        return HttpResponse.notFound();
    }
}
