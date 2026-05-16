package com.digitalsignage.admin.entity;

import com.digitalsignage.admin.common.enums.PermissionAccessMode;
import com.digitalsignage.admin.common.enums.PermissionPrincipalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "api_permission")
public class ApiPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String code;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    @Column(name = "path_pattern", nullable = false, length = 512)
    private String pathPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false, length = 16)
    private PermissionPrincipalType principalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false, length = 32)
    private PermissionAccessMode accessMode;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(length = 512)
    private String description;
}
