package com.trustplatform.user;

import java.util.Set;

public enum Role {
    USER(Set.of(
            Permission.READ_CONTENT
    )),
    DONOR(Set.of(
            Permission.READ_CONTENT
    )),
    VOLUNTEER(Set.of(
            Permission.READ_CONTENT,
            Permission.SUBMIT_VOLUNTEER
    )),
    APPLICANT(Set.of(
            Permission.READ_CONTENT,
            Permission.SUBMIT_ASSISTANCE
    )),
    ADMIN(Set.of(
            Permission.READ_CONTENT,
            Permission.SUBMIT_VOLUNTEER,
            Permission.MANAGE_EVENTS,
            Permission.MANAGE_STORIES,
            Permission.MANAGE_MEMBERS,
            Permission.MANAGE_SETTINGS,
            Permission.VIEW_AUDIT_LOGS,
            Permission.MANAGE_MEDIA,
            Permission.VIEW_ANALYTICS,
            Permission.MANAGE_USERS,
            Permission.EXPORT_REPORTS,
            Permission.MANAGE_APPLICATIONS
    ));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}
