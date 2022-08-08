package io.github.colintimbarndt.chat_emotes.data.unicode;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public final class UnicodeModifierType implements Comparable<UnicodeModifierType> {
    public final @NotNull String name;
    public final int priority;
    public final int defaultChoice;
    public final @NotNull String format;
    private @NotNull Modifier[] values = null;
    private @NotNull Pattern @Nullable[] replace;

    public UnicodeModifierType(@NotNull String name, int priority, int defaultChoice, @NotNull String format) {
        this.name = name;
        this.priority = priority;
        this.defaultChoice = defaultChoice;
        this.format = format;
    }

    public Modifier[] values() {
        return values.clone();
    }

    public @NotNull Pattern @Nullable[] replace() {
        return replace == null ? null : replace.clone();
    }

    @Contract("_, _, _ -> new")
    public static @NotNull Builder build(@NotNull String name, int defaultChoice, @NotNull String format) {
        return new Builder(name, defaultChoice, format);
    }

    @Override
    public int compareTo(@NotNull UnicodeModifierType other) {
        return Integer.compare(priority, other.priority);
    }

    public final class Modifier implements Comparable<Modifier> {
        public final int ordinal;
        public final @NotNull String sequence;
        private final @NotNull String @NotNull[] names;
        private Modifier(@NotNull String sequence, int ordinal, @NotNull String @NotNull[] names) {
            this.ordinal = ordinal;
            this.sequence = sequence;
            this.names = names;
        }

        public UnicodeModifierType getType() {
            return UnicodeModifierType.this;
        }

        public String[] names() {
            return names.clone();
        }

        public String getModifiedName(String alias) {
            if (replace != null) {
                for (int j = 0; j < replace.length; j++) {
                    final Pattern pattern = replace[j];
                    final var matcher = pattern.matcher(alias);
                    if (matcher.find()) {
                        return matcher.replaceFirst(names[j]);
                    }
                }
            }
            return format.formatted(alias, names[defaultChoice]);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Modifier modifier = (Modifier) o;

            return ordinal == modifier.ordinal
                    && getType() == modifier.getType();
        }

        @Override
        public int hashCode() {
            return ordinal + 15 * getType().hashCode();
        }

        @Override
        public int compareTo(@NotNull UnicodeModifierType.Modifier other) {
            return UnicodeModifierType.this.compareTo(other.getType());
        }
    }

    public static final class Builder {
        private static final Modifier[] EMPTY_MODIFIERS = {};
        private static final Pattern[] EMPTY_PATTERNS = {};
        private final Set<String> sequences = new HashSet<>();
        private final Set<String> names = new HashSet<>();
        private final List<Modifier> modifiers = new ArrayList<>(8);
        private final List<Pattern> replace = new ArrayList<>(0);
        private final UnicodeModifierType type;
        private int ordinal = 0;
        private int priority = 0;
        private boolean isBuilt = false;

        private Builder(@NotNull String name, int defaultChoice, @NotNull String format) {
            this.type = new UnicodeModifierType(name, priority, defaultChoice, format);
        }

        public UnicodeModifierType create() {
            if (isBuilt)
                throw new IllegalStateException("already built");
            isBuilt = true;
            if (!replace.isEmpty()) {
                for (Modifier mod : modifiers) {
                    if (mod.names.length != replace.size()) {
                        throw new IllegalStateException(
                                "length of replace is not equal to the length of all names arrays of the modifiers"
                        );
                    }
                }
                type.replace = replace.toArray(EMPTY_PATTERNS);
            }
            type.values = modifiers.toArray(EMPTY_MODIFIERS);
            return type;
        }

        public Builder priority(int prio) {
            priority = prio;
            return this;
        }

        public Builder replace(Pattern pattern) {
            if (isBuilt)
                throw new IllegalStateException("already built");
            replace.add(Objects.requireNonNull(pattern));
            return this;
        }

        public Builder addVariant(String sequence, String[] names) {
            if (isBuilt)
                throw new IllegalStateException("already built");
            if (sequences.contains(sequence)) {
                throw new IllegalStateException("sequence already registered");
            }
            Objects.requireNonNull(sequence);
            Objects.requireNonNull(names);
            if (Arrays.stream(names).anyMatch(this::isNameUsed)) {
                throw new IllegalStateException("name already in use");
            }
            sequences.add(sequence);
            Collections.addAll(this.names, names);
            modifiers.add(type.new Modifier(sequence, ordinal++, names));
            return this;
        }

        public boolean isNameUsed(String name) {
            return names.contains(name);
        }
    }
}
