package io.unityfoundation.dds.permissions.manager;

public interface StatusCodes {
    // generic
    String UNAUTHORIZED = "unauthorized";

    // user
    String USER_ALREADY_EXISTS = "user.exists";
    String USER_NOT_FOUND = "user.not-found";

    // group
    String GROUP_NOT_FOUND = "group.not-found";
    String GROUP_ALREADY_EXISTS = "group.exists";
    String GROUP_NAME_CANNOT_BE_BLANK_OR_NULL = "group.name.cannot-be-blank-or-null";

    // application
    String APPLICATION_NOT_FOUND = "application.not-found";
    String APPLICATION_REQUIRES_GROUP_ASSOCIATION = "application.requires-group-association";
    String APPLICATION_CANNOT_UPDATE_GROUP_ASSOCIATION = "application.cannot-update-group-association";
    String APPLICATION_ALREADY_EXISTS = "application.exists";
    String APPLICATION_NAME_CANNOT_BE_BLANK_OR_NULL = "application.name.cannot-be-blank-or-null";
    String IDENTITY_CERT_NOT_FOUND = "application.identity-ca-cert.not-found";
    String PERMISSIONS_CERT_NOT_FOUND = "application.permissions-ca-cert.not-found";
    String GOVERNANCE_FILE_NOT_FOUND = "application.governance-file.not-found";

    // topic
    String TOPIC_NOT_FOUND = "topic.not-found";
    String TOPIC_UPDATE_NOT_ALLOWED = "topic.update-not-allowed";
    String TOPIC_REQUIRES_GROUP_ASSOCIATION = "topic.requires-group-association";
    String TOPIC_NAME_CANNOT_BE_BLANK_OR_NULL = "topic.name.cannot-be-blank-or-null";
    String TOPIC_ALREADY_EXISTS = "topic.exists";

    // group member
    String USER_IS_VALID = "user.is-valid";
    String USER_IS_NOT_VALID = "user.is-not-valid";
    String GROUP_MEMBERSHIP_NOT_FOUND = "user.group-membership.not-found";

    // application permission
    String APPLICATION_PERMISSION_NOT_FOUND = "application.permission.not-found";
}
