package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.group.GroupService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;

import javax.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Controller("/groups")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @Get
    public HttpResponse index(@Valid Pageable pageable) {
        return HttpResponse.ok(groupService.findAll(pageable));
    }

    @Get("/create")
    public HttpResponse create() {
        return HttpResponse.ok();
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    HttpResponse<?> save(@Body Group group) {
        groupService.save(group);
        return HttpResponse.seeOther(URI.create("/groups/"));
    }


    @Post("/delete/{id}")
    HttpResponse<?> delete(Long id) {
        groupService.deleteById(id);
        return HttpResponse.seeOther(URI.create("/groups"));
    }

    @Get("/{id}")
    HttpResponse show(Long id) {
        Optional<Map> groupOptional = groupService.getGroupAndCandidates(id);
        if (groupOptional.isPresent()) {
            Map payload = groupOptional.get();
            return HttpResponse.ok(payload);
        }
        return HttpResponse.notFound();
    }

    @Post("/remove_member/{groupId}/{memberId}")
    HttpResponse removeMember(Long groupId, Long memberId) {
        boolean success = groupService.removeMember(groupId, memberId);
        if (!success) {
            return HttpResponse.notFound();
        }
        return HttpResponse.seeOther(URI.create("/groups/" + groupId));
    }

    @Post("/add_member/{groupId}/{candidateId}")
    HttpResponse addMember(Long groupId, Long candidateId) {
        if (groupService.addMember(groupId, candidateId)) {
            return HttpResponse.seeOther(URI.create("/groups/" + groupId));
        }
        return HttpResponse.notFound();
    }

}
