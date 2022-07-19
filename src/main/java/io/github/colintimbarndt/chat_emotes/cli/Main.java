package io.github.colintimbarndt.chat_emotes.cli;

import io.github.colintimbarndt.chat_emotes.data.unicode.EmoteTextureArchive;
import org.jetbrains.annotations.Contract;

import java.io.*;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

public final class Main {
    private Main() {}
    public static void main(String[] args) {
        if (args.length < 2 && !args[0].equals("import")) {
            printHelp();
        }
        try {
            switch (args[1]) {
                case "emoji" -> {
                    if (args.length != 4) {
                        printHelp();
                    }
                        importIndex(args[2], args[3]);
                }
                default -> printHelp();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Contract("-> fail")
    private static void printHelp() {
        System.err.println("Syntax:");
        System.err.println("\timport emoji <file.zip> <out.txt>");
        System.exit(1);
    }

    private static void importIndex(String input, String output) throws IOException {
        final Pattern pattern = EmoteTextureArchive.FILENAME_PATTERN;
        final Pattern separator = EmoteTextureArchive.FILENAME_SEPARATOR_PATTERN;
        final File inFile = Path.of(input).toFile();
        final File outFile = Path.of(output).toFile();
        if (!inFile.exists()) {
            System.err.println("File '" + input + "' does not exist");
            System.exit(1);
        }
        if (!(outFile.exists() || outFile.createNewFile())) {
            System.err.println("Unable to create file '" + output + "'");
            System.exit(1);
        }
        try (final var outStream = new BufferedOutputStream(new FileOutputStream(outFile))) {
            outStream.write(0xFE);
            outStream.write(0xFF);
            try (final var zip = new ZipInputStream(new FileInputStream(inFile))) {
                while (true) {
                    final var entry = zip.getNextEntry();
                    if (entry == null) break;
                    final var name = entry.getName();
                    zip.closeEntry();
                    final var match = pattern.matcher(name);
                    if (!match.matches()) {
                        System.err.println("Invalid file name: " + name);
                        continue;
                    }
                    final var split = separator.splitAsStream(name.substring(0, name.length() - 4)).iterator();
                    while (split.hasNext()) {
                        final int cp = Integer.parseInt(split.next(), 16);
                        if (Character.isBmpCodePoint(cp)) {
                            outStream.write(cp >> 8);
                            outStream.write(cp);
                        } else {
                            char sur = Character.highSurrogate(cp);
                            outStream.write(sur >> 8);
                            outStream.write(sur);
                            sur = Character.lowSurrogate(cp);
                            outStream.write(sur >> 8);
                            outStream.write(sur);
                        }
                    }
                    outStream.write(0);
                    outStream.write('\n');
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            outStream.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
