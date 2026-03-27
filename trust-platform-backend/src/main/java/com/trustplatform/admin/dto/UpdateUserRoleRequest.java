package com.trustplatform.admin.dto;

import com.trustplatform.user.Role;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    private Role role;
}