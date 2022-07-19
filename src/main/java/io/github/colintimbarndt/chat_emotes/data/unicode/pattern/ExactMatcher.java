package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

public record ExactMatcher(String text, String name) implements Matcher {
    @Override
    public MatchResult matchWith(String s, int offset) {
        return s.startsWith(text, offset) ? new MatchResult(text, 0, name) : null;
    }

    @Override
    public int width() {
        return 1;
    }

    @Override
    public boolean hasName() {
        return true;
    }
}
