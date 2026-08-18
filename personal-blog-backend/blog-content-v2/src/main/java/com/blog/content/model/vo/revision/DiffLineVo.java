package com.blog.content.model.vo.revision;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiffLineVo {

    private String type;
    private Integer leftLineNo;
    private Integer rightLineNo;
    private String text;
}
