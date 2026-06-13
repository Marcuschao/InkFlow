package com.blog.auth.model.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称最多50字")
    private String nickname;

    @Size(max = 255, message = "头像URL过长")
    private String avatar;

    @Min(value = 0, message = "性别无效")
    @Max(value = 2, message = "性别无效")
    private Integer gender;

    @Size(max = 500, message = "简介最多500字")
    private String bio;
}
