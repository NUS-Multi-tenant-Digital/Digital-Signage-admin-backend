package com.digitalsignage.admin.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RoleApiPermissionId implements Serializable {

    private String role;
    private Long permissionId;
}
