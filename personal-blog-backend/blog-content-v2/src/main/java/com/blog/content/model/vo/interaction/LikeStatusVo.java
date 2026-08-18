package com.blog.content.model.vo.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeStatusVo {
    private Boolean liked;
    private Integer likeCount;
}
