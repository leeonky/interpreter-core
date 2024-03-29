package com.github.leeonky.interpreter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StringWithPosition {
    private final String content;
    private final Set<Integer> positions = new LinkedHashSet<>();
    private final Set<Integer> rows = new LinkedHashSet<>();
    private final Set<Integer> columns = new LinkedHashSet<>();

    public StringWithPosition(String content) {
        this.content = content;
    }

    public StringWithPosition position(int position) {
        if (position >= 0 && position <= content.length())
            positions.add(position);
        return this;
    }

    public StringWithPosition row(int position) {
        if (position >= 0 && position <= content.length())
            rows.add(position);
        return this;
    }

    public StringWithPosition column(int position) {
        if (position >= 0 && position <= content.length())
            columns.add(position);
        return this;
    }

    public String result() {
        return result(0);
    }

    public String result(int offset) {
        return result(content.substring(offset), offset);
    }

    private String result(String content, int offset) {
        try {
            StringBuilder result = new StringBuilder();
            SeparatedString separatedString = new SeparatedString(content, 0, offset, content);
            while (separatedString.printLine(result).hasNextLine())
                separatedString = separatedString.separatedNext().newLine(result);
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return content;
        }
    }

    public String result(String prefix) {
        return result(prefix + content, -prefix.length());
    }

    public class SeparatedString {
        private final int startPosition;
        private final int offset;
        private final String content;
        private final String newLine;
        private final String[] lines;

        public SeparatedString(String leftContent, int startPosition, int offset, String content) {
            lines = leftContent.split("\r\n|\n\r|\n|\r", 2);
            newLine = fetchNewLine(leftContent);
            this.startPosition = startPosition;
            this.offset = offset;
            this.content = content;
        }

        private String fetchNewLine(String content) {
            return hasNextLine() ? content.substring(lines[0].length(), content.length() - lines[1].length()) : "\n";
        }

        public boolean hasNextLine() {
            return lines.length == 2;
        }

        private SeparatedString printLine(StringBuilder builder) {
            builder.append(lines[0]);
            printPositions(builder, positions);
            printPositions(builder, columns);
            printWholeLine(builder);
            return this;
        }

        private void printWholeLine(StringBuilder builder) {
            List<Integer> linePositions = linePosition(rows);
            if (!linePositions.isEmpty()) {
                builder.append(newLine);
                for (int i = lengthWithFullWidthChar(lines[0]); i > -1; i--)
                    builder.append('^');
            }
        }

        private int lengthWithFullWidthChar(String line) {
            return line.length() + (int) line.chars().filter(this::isFullWidth).count();
        }

        private void printPositions(StringBuilder builder, Collection<Integer> positions) {
            List<Integer> linePositions = linePosition(positions);
            if (!linePositions.isEmpty()) {
                int lastPosition = startPosition;
                builder.append(newLine);
                for (int eachPosition : linePositions) {
                    builder.append(NotationMark(lengthWithFullWidthChar(content.substring(lastPosition, eachPosition))));
                    if (eachPosition < content.length() && isFullWidth(content.charAt(eachPosition)))
                        builder.append(' ');
                    lastPosition = eachPosition + newLine.length();
                }
                if (builder.charAt(builder.length() - 1) == ' ')
                    builder.deleteCharAt(builder.length() - 1);
            }
        }

        private String NotationMark(int count) {
            return String.format("%" + (count + 1) + "c", '^');
        }

        private List<Integer> linePosition(Collection<Integer> positions) {
            return positions.stream().map(i -> i - offset)
                    .filter(i -> i >= startPosition && i <= startPosition + lines[0].length())
                    .sorted().collect(Collectors.toList());
        }

        private SeparatedString separatedNext() {
            return new SeparatedString(lines[1], startPosition + lines[0].length() + newLine.length(), offset, content);
        }

        private SeparatedString newLine(StringBuilder result) {
            result.append(newLine);
            return this;
        }

        private boolean isFullWidth(int c) {
            return !('\u0000' <= c && c <= '\u00FF' || '\uFF61' <= c && c <= '\uFFDC' || '\uFFE8' <= c && c <= '\uFFEE');
        }
    }
}
