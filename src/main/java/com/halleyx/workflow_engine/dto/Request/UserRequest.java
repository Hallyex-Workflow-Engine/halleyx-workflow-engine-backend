package com.halleyx.workflow_engine.dto.Request;


import com.halleyx.workflow_engine.entity.Enum.Role;
import lombok.Data;

@Data
public class UserRequest {
    private String name;
    private String email;
    private String password;
    private Role role;
    private String phone;
}