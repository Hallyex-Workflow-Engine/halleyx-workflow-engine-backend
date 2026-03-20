package com.halleyx.workflow_engine.dto.Request;

import com.halleyx.workflow_engine.entity.Enum.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {


    @Size(max = 100)
    private String name;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String avatarUrl;

    private Role role;

    private Boolean isActive;
}