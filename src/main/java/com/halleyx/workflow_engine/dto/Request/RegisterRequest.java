package com.halleyx.workflow_engine.dto.Request;

import com.halleyx.workflow_engine.entity.Enum.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String email;
    private String name;
    private String password;
    private Role role;
}
