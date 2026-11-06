package com.ecommerce.auth.dto;

import com.ecommerce.auth.model.Role;

public record UserSummaryDto(
    Long id,
    String email,
    String fullName,
    Role role
) {}
