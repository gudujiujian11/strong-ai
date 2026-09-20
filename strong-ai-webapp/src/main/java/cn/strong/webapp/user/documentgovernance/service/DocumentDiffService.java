package cn.strong.webapp.user.documentgovernance.service;

import cn.strong.webapp.user.model.documentgovernance.DiffHunk;
import cn.strong.webapp.user.model.documentgovernance.DiffResponse;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 基于 LCS 的行级差异计算。与发版解耦，供任意两版本或版本与草稿的动态比较使用。
 * 返回结构化 hunk，渲染交给前端。
 */
@Service
public class DocumentDiffService {

    /**
     * 生成 before 到 after 的行级差异。相等行也返回，便于前端展示上下文。
     */
    public List<DiffHunk> diffLines(String before, String after) {
        List<String> oldLines = splitLines(before);
        List<String> newLines = splitLines(after);
        Patch<String> patch = DiffUtils.diff(oldLines, newLines);

        List<DiffHunk> hunks = new ArrayList<>();
        int oldIndex = 0;
        int newIndex = 0;
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            Chunk<String> source = delta.getSource();
            // 变更之前的相等行
            while (oldIndex < source.getPosition()) {
                hunks.add(equalHunk(oldLines.get(oldIndex), oldIndex, newIndex));
                oldIndex++;
                newIndex++;
            }
            switch (delta.getType()) {
                case DELETE:
                    for (String line : source.getLines()) {
                        hunks.add(hunk("DELETE", oldIndex + 1, null, line));
                        oldIndex++;
                    }
                    break;
                case INSERT:
                    for (String line : delta.getTarget().getLines()) {
                        hunks.add(hunk("INSERT", null, newIndex + 1, line));
                        newIndex++;
                    }
                    break;
                case CHANGE:
                    for (String line : source.getLines()) {
                        hunks.add(hunk("DELETE", oldIndex + 1, null, line));
                        oldIndex++;
                    }
                    for (String line : delta.getTarget().getLines()) {
                        hunks.add(hunk("INSERT", null, newIndex + 1, line));
                        newIndex++;
                    }
                    break;
                default:
                    break;
            }
        }
        // 尾部剩余的相等行
        while (oldIndex < oldLines.size()) {
            hunks.add(equalHunk(oldLines.get(oldIndex), oldIndex, newIndex));
            oldIndex++;
            newIndex++;
        }
        return hunks;
    }

    /**
     * 组装两个版本之间的差异响应。
     */
    public DiffResponse compare(Long fromVersionNo, String fromContent, Long toVersionNo, String toContent,
                                boolean againstDraft) {
        DiffResponse response = new DiffResponse();
        response.setFromVersionNo(fromVersionNo);
        response.setToVersionNo(toVersionNo);
        response.setAgainstDraft(againstDraft);
        response.setHunks(diffLines(fromContent, toContent));
        return response;
    }

    private DiffHunk equalHunk(String line, int oldIndex, int newIndex) {
        return hunk("EQUAL", oldIndex + 1, newIndex + 1, line);
    }

    private DiffHunk hunk(String type, Integer oldLine, Integer newLine, String text) {
        DiffHunk hunk = new DiffHunk();
        hunk.setType(type);
        hunk.setOldLine(oldLine);
        hunk.setNewLine(newLine);
        hunk.setText(text);
        return hunk;
    }

    private List<String> splitLines(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(value.split("\\r?\\n", -1)));
    }
}
