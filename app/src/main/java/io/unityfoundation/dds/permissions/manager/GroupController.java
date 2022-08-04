package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupService;
import org.hibernate.DuplicateMappingException;

import javax.persistence.PersistenceException;
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
        try {
            groupService.save(group);
        } catch (DuplicateMappingException duplicateMappingException) {
            return HttpResponse.badRequest(duplicateMappingException.getMessage());
        }
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

    @Post(uris = {"/remove_member/{groupId}/{memberId}", "/remove_admin/{groupId}/{memberId}"})
    HttpResponse removeMember(Long groupId, Long memberId, HttpRequest request) {

        String path = request.getPath();
        if (groupService.isAdminOrGroupAdmin(groupId)) {
            if (groupService.removeMember(groupId, memberId, path.contains("admin"))) {
                return HttpResponse.seeOther(URI.create("/groups/" + groupId));
            }
        } else {
            return HttpResponse.unauthorized();
        }

        return HttpResponse.notFound();
    }

    @Post(uris = {"/add_member/{groupId}/{candidateId}", "/add_admin/{groupId}/{candidateId}"})
    HttpResponse addMember(Long groupId, Long candidateId, HttpRequest request) {

        String path = request.getPath();
        if (groupService.isAdminOrGroupAdmin(groupId)) {
            if (groupService.addMember(groupId, candidateId, path.contains("admin"))) {
                return HttpResponse.seeOther(URI.create("/groups/" + groupId));
            }
        } else {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.notFound();
    }

}
