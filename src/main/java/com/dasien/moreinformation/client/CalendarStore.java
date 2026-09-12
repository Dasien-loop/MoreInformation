package com.dasien.moreinformation.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.List;

final class CalendarStore {
    private CalendarStore() {}

    static CalendarClock load(Path path, long initialTime) throws IOException {
        if (!Files.exists(path)) return new CalendarClock(initialTime);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        try {
            if (lines.size() != 2) throw new NumberFormatException("Expected two calendar values");
            CalendarClock clock = new CalendarClock(Long.parseLong(lines.get(0)), Long.parseLong(lines.get(1)));
            clock.accept(initialTime);
            return clock;
        } catch (NumberFormatException error) {
            throw new IOException("Invalid calendar data: " + path, error);
        }
    }

    static void save(Path path, CalendarClock clock) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "calendar-", ".tmp");
        try {
            Files.write(temporary, List.of(Long.toString(clock.rawTime()), Long.toString(clock.day())), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
