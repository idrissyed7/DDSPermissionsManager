package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.security.token.jwt.generator.claims.JwtClaims;
import io.micronaut.security.token.jwt.generator.claims.JwtClaimsSetAdapter;
import io.micronaut.security.token.jwt.validator.JwtTokenValidator;
import io.unityfoundation.dds.permissions.manager.exception.DPMException;
import io.unityfoundation.dds.permissions.manager.ResponseStatusCodes;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Singleton
public class ApplicationPermissionService {

    public static final String APPLICATION_BIND_TOKEN = "APPLICATION_BIND_TOKEN";

    private final ApplicationPermissionRepository applicationPermissionRepository;
    private final ApplicationRepository applicationRepository;
    private final TopicRepository topicRepository;
    private final SecurityUtil securityUtil;
    private final GroupUserService groupUserService;
    private final JwtTokenValidator jwtTokenValidator;

    public ApplicationPermissionService(ApplicationPermissionRepository applicationPermissionRepository,
                                        ApplicationRepository applicationRepository, TopicRepository topicRepository,
                                        SecurityUtil securityUtil, GroupUserService groupUserService, JwtTokenValidator jwtTokenValidator) {
        this.applicationPermissionRepository = applicationPermissionRepository;
        this.applicationRepository = applicationRepository;
        this.topicRepository = topicRepository;
        this.securityUtil = securityUtil;
        this.groupUserService = groupUserService;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    public Page<AccessPermissionDTO> indexByTopicId(Long topicId, Pageable pageable) {
        Page<ApplicationPermission> page;
        boolean isTopic = true;
        boolean publicMode = determinePublicMode(isTopic, topicId);

        if (publicMode) {
            page = getPublicApplicationPermissionsPage(isTopic, topicId, pageable);
        } else {
            page = getApplicationPermissionsPage(isTopic, topicId, pageable);
        }

        return getAccessPermissionDTOPage(page);
    }

    public Page<AccessPermissionDTO> indexByApplicationId(Long applicationId, Pageable pageable) {
        Page<ApplicationPermission> page;
        boolean isTopic = false;
        boolean publicMode = determinePublicMode(isTopic, applicationId);

        if (publicMode) {
            page = getPublicApplicationPermissionsPage(isTopic, applicationId, pageable);
        } else {
            page = getApplicationPermissionsPage(isTopic, applicationId, pageable);
        }

        return getAccessPermissionDTOPage(page);
    }

    private static Page<AccessPermissionDTO> getAccessPermissionDTOPage(Page<ApplicationPermission> page) {
        return page.map(applicationPermission -> new AccessPermissionDTO(
                applicationPermission.getId(),
                applicationPermission.getPermissionsTopic().getId(),
                applicationPermission.getPermissionsTopic().getName(),
                applicationPermission.getPermissionsTopic().getPermissionsGroup().getName(),
                applicationPermission.getPermissionsApplication().getId(),
                applicationPermission.getPermissionsApplication().getName(),
                applicationPermission.getPermissionsApplication().getPermissionsGroup().getName(),
                applicationPermission.getAccessType()
        ));
    }

    private boolean determinePublicMode(boolean isTopic, Long entityId) {
        if (securityUtil.isCurrentUserAdmin()) {
            return false;
        }

        if (isTopic) {
            Optional<Topic> topicOptional = topicRepository.findById(entityId);
            if (topicOptional.isEmpty()) {
                throw new DPMException(ResponseStatusCodes.TOPIC_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            return !groupUserService.isCurrentUserMemberOfGroup(entityId) && topicOptional.get().getMakePublic();
        } else {
            Optional<Application> applicationOptional = applicationRepository.findById(entityId);
            if (applicationOptional.isEmpty()) {
                throw new DPMException(ResponseStatusCodes.APPLICATION_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            return !groupUserService.isCurrentUserMemberOfGroup(entityId) && applicationOptional.get().getMakePublic();
        }
    }

    private Page<ApplicationPermission> getApplicationPermissionsPage(boolean isTopic, Long entityId, Pageable pageable) {
        if (securityUtil.isCurrentUserAdmin()) {
            if (isTopic) {
                return applicationPermissionRepository.findByPermissionsTopicId(entityId, pageable);
            } else {
                return applicationPermissionRepository.findByPermissionsApplicationId(entityId, pageable);
            }
        } else {
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();
            List<Long> groups = groupUserService.getAllGroupsUserIsAMemberOf(user.getId());
            if (groups.isEmpty()) {
                return Page.empty();
            }

            List<Long> groupsApplications = applicationRepository.findIdByPermissionsGroupIdIn(groups);
            List<Long> groupsTopics = topicRepository.findIdByPermissionsGroupIdIn(groups);

            if (isTopic) {
                return applicationPermissionRepository.findByPermissionsTopicIdAndPermissionsTopicIdIn(entityId, groupsTopics, pageable);
            } else {
                return applicationPermissionRepository.findByPermissionsApplicationIdAndPermissionsApplicationIdIn(entityId, groupsApplications, pageable);
            }
        }
    }

    private Page<ApplicationPermission> getPublicApplicationPermissionsPage(boolean isTopic, Long entityId, Pageable pageable) {
        if (isTopic) {
            return applicationPermissionRepository.findByPermissionsApplicationMakePublicTrueAndPermissionsTopicMakePublicTrueAndPermissionsTopicId(entityId, pageable);
        } else {
            return applicationPermissionRepository.findByPermissionsApplicationMakePublicTrueAndPermissionsTopicMakePublicTrueAndPermissionsApplicationId(entityId, pageable);
        }
    }

    public Publisher<HttpResponse<AccessPermissionDTO>> addAccess(String bindToken, Long topicId, AccessType access) {
        return Publishers.map(jwtTokenValidator.validateToken(bindToken, null), authentication -> {
            JWT jwt;
            JwtClaims claims;
            try {
                jwt = JWTParser.parse(bindToken);
                claims = new JwtClaimsSetAdapter(jwt.getJWTClaimsSet());
                if (claims.get(JwtClaims.SUBJECT) != null) {
                    Long applicationId = Long.valueOf((String) claims.get(JwtClaims.SUBJECT));
                    return addAccess(applicationId, topicId, access);
                } else {
                    throw new DPMException(ResponseStatusCodes.APPLICATION_BIND_TOKEN_PARSE_EXCEPTION, HttpStatus.BAD_REQUEST);
                }
            } catch (ParseException e) {
                throw new DPMException(ResponseStatusCodes.APPLICATION_BIND_TOKEN_PARSE_EXCEPTION, HttpStatus.BAD_REQUEST);
            }
        });
    }

    public HttpResponse<AccessPermissionDTO> addAccess(Long applicationId, Long topicId, AccessType access) {
        final HttpResponse response;

        Optional<Application> applicationById = applicationRepository.findById(applicationId);
        if (applicationById.isEmpty()) {
            throw new DPMException(ResponseStatusCodes.APPLICATION_NOT_FOUND, HttpStatus.NOT_FOUND);
        } else {
            Optional<Topic> topicById = topicRepository.findById(topicId);
            if (topicById.isEmpty()) {
                throw new DPMException(ResponseStatusCodes.TOPIC_NOT_FOUND, HttpStatus.NOT_FOUND);
            } else {
                Topic topic = topicById.get();

                User user = securityUtil.getCurrentlyAuthenticatedUser().get();
                if (!securityUtil.isCurrentUserAdmin() &&
                        !groupUserService.isUserTopicAdminOfGroup(topic.getPermissionsGroup().getId(), user.getId())) {
                    throw new DPMException(ResponseStatusCodes.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
                } else {
                    Application application = applicationById.get();
                    response = addAccess(application, topic, access);
                }
            }
        }

        return response;
    }

    public HttpResponse addAccess(Application application, Topic topic, AccessType access) {
        ApplicationPermission newPermission = saveNewPermission(application, topic, access);
        AccessPermissionDTO dto = createDTO(newPermission);
        return HttpResponse.created(dto);
    }

    public ApplicationPermission saveNewPermission(Application application, Topic topic, AccessType access) {
        if (applicationPermissionRepository.existsByPermissionsApplicationAndPermissionsTopic(application, topic)) {
            throw new DPMException(ResponseStatusCodes.APPLICATION_PERMISSION_ALREADY_EXISTS);
        }
        ApplicationPermission applicationPermission = new ApplicationPermission(application, topic, access);
        return applicationPermissionRepository.save(applicationPermission);
    }

    public AccessPermissionDTO createDTO(ApplicationPermission applicationPermission) {
        Long topicId = applicationPermission.getPermissionsTopic().getId();
        String topicName = applicationPermission.getPermissionsTopic().getName();
        String topicGroup = applicationPermission.getPermissionsTopic().getPermissionsGroup().getName();
        Long applicationId = applicationPermission.getPermissionsApplication().getId();
        String applicationName = applicationPermission.getPermissionsApplication().getName();
        String applicationGroupName = applicationPermission.getPermissionsApplication().getPermissionsGroup().getName();
        AccessType accessType = applicationPermission.getAccessType();
        return new AccessPermissionDTO(
                applicationPermission.getId(),
                topicId,
                topicName,
                topicGroup,
                applicationId,
                applicationName,
                applicationGroupName,
                accessType
        );
    }

    public HttpResponse deleteById(Long permissionId) {

        Optional<ApplicationPermission> applicationPermissionOptional = applicationPermissionRepository.findById(permissionId);

        if (applicationPermissionOptional.isEmpty()) {
            throw new DPMException(ResponseStatusCodes.APPLICATION_PERMISSION_NOT_FOUND, HttpStatus.NOT_FOUND);
        } else {
            Topic topic = applicationPermissionOptional.get().getPermissionsTopic();
            Application application = applicationPermissionOptional.get().getPermissionsApplication();
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();

            if (!securityUtil.isCurrentUserAdmin() &&
                    !(
                            groupUserService.isUserTopicAdminOfGroup(topic.getPermissionsGroup().getId(), user.getId()) ||
                            groupUserService.isUserApplicationAdminOfGroup(application.getPermissionsGroup().getId(), user.getId())
                    )) {
                throw new DPMException(ResponseStatusCodes.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
            }
        }

        applicationPermissionRepository.deleteById(permissionId);
        return HttpResponse.noContent();
    }

    public HttpResponse<AccessPermissionDTO> updateAccess(Long permissionId, AccessType access) {
        if (Arrays.stream(AccessType.values()).noneMatch(accessType -> accessType.equals(access))) {
            return HttpResponse.badRequest();
        }

        Optional<ApplicationPermission> applicationPermissionOptional = applicationPermissionRepository.findById(permissionId);

        if (applicationPermissionOptional.isEmpty()) {
            throw new DPMException(ResponseStatusCodes.APPLICATION_PERMISSION_NOT_FOUND, HttpStatus.NOT_FOUND);
        } else {
            Topic topic = applicationPermissionOptional.get().getPermissionsTopic();
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();

            if (!securityUtil.isCurrentUserAdmin() && !groupUserService.isUserTopicAdminOfGroup(topic.getPermissionsGroup().getId(), user.getId())) {
                throw new DPMException(ResponseStatusCodes.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
            }
        }

        ApplicationPermission applicationPermission = applicationPermissionOptional.get();
        applicationPermission.setAccessType(access);

        return HttpResponse.ok(createDTO(applicationPermissionRepository.update(applicationPermission)));
    }

    public void deleteAllByTopic(Topic topic) {
        applicationPermissionRepository.deleteByPermissionsTopicEquals(topic);
    }
    public void deleteAllByApplication(Application application) {
        applicationPermissionRepository.deleteByPermissionsApplicationEquals(application);
    }

    public List<ApplicationPermission> findAllByApplication(Application application) {
        return applicationPermissionRepository.findByPermissionsApplication(application);
    }
}
