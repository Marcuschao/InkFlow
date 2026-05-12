package com.blog.personalblogbackend.dto.stat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PvTrendDto {
    private List<String> labels;
    private List<Long> values;
}
