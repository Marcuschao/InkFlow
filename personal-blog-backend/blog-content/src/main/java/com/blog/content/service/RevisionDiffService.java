package com.blog.content.service;

import com.blog.content.model.vo.revision.DiffLineVo;
import com.blog.content.model.vo.revision.RevisionDiffResponseVo;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class RevisionDiffService {

    public RevisionDiffResponseVo build(Long leftRevisionId, Long rightRevisionId, String leftText, String rightText) {
        List<String> left = splitLines(leftText);
        List<String> right = splitLines(rightText);
        Patch<String> patch = DiffUtils.diff(left, right);
        List<DiffLineVo> lines = new ArrayList<>();
        int i = 0;
        int j = 0;
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            while (i < delta.getSource().getPosition()) {
                lines.add(new DiffLineVo("EQUAL", i + 1, j + 1, left.get(i)));
                i++;
                j++;
            }
            switch (delta.getType()) {
                case DELETE -> {
                    for (String line : delta.getSource().getLines()) {
                        lines.add(new DiffLineVo("DELETE", i + 1, null, line));
                        i++;
                    }
                }
                case INSERT -> {
                    for (String line : delta.getTarget().getLines()) {
                        lines.add(new DiffLineVo("INSERT", null, j + 1, line));
                        j++;
                    }
                }
                case CHANGE -> {
                    for (String line : delta.getSource().getLines()) {
                        lines.add(new DiffLineVo("DELETE", i + 1, null, line));
                        i++;
                    }
                    for (String line : delta.getTarget().getLines()) {
                        lines.add(new DiffLineVo("INSERT", null, j + 1, line));
                        j++;
                    }
                }
                default -> {
                }
            }
        }
        while (i < left.size()) {
            lines.add(new DiffLineVo("EQUAL", i + 1, j + 1, left.get(i)));
            i++;
            j++;
        }
        RevisionDiffResponseVo vo = new RevisionDiffResponseVo();
        vo.setLeftRevisionId(leftRevisionId);
        vo.setRightRevisionId(rightRevisionId);
        vo.setLines(lines);
        return vo;
    }

    private static List<String> splitLines(String s) {
        if (s == null || s.isEmpty()) {
            return new ArrayList<>(List.of(""));
        }
        return new ArrayList<>(Arrays.asList(s.split("\n", -1)));
    }
}
