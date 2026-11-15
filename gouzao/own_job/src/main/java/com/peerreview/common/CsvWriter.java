package com.peerreview.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CSV 导出工具（对应课程要求「互评结果导出 CSV，供教师直接复制到成绩表」）。
 *
 * <p>输出 UTF-8 + BOM，保证 Excel 双击打开不乱码；对含逗号、引号、换行的单元格做标准转义。</p>
 */
public final class CsvWriter {

    private static final char BOM = '\uFEFF';

    private CsvWriter() {
    }

    public static void write(Path file, List<String> headers, List<List<String>> rows) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append(BOM);
        appendRow(content, headers);
        for (List<String> row : rows) {
            appendRow(content, row);
        }
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, content.toString(), StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder content, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                content.append(',');
            }
            content.append(escape(cells.get(i)));
        }
        content.append("\r\n");
    }

    private static String escape(String value) {
        String text = value == null ? "" : value;
        if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
