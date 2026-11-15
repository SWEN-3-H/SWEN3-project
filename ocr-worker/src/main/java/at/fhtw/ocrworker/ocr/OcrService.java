package at.fhtw.ocrworker.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OcrService {

    private static final Logger log =
            LoggerFactory.getLogger(OcrService.class);

    private static final int MAX_PAGES = 50;

    private final String languages;
    private final long timeoutSeconds;

    public OcrService(
            @Value("${app.ocr.languages}") String languages,
            @Value("${app.ocr.timeout-seconds}") long timeoutSeconds) {

        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException(
                    "OCR timeout must be greater than zero"
            );
        }

        this.languages = languages;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String extractText(Path pdf) {
        Path workDirectory = null;

        try {
            workDirectory = Files.createTempDirectory("ocr-pages-");
            Path commandLog = workDirectory.resolve("command.log");

            runCommand(
                    List.of(
                            "gs",
                            "-dSAFER",
                            "-dBATCH",
                            "-dNOPAUSE",
                            "-sDEVICE=pnggray",
                            "-r300",
                            "-dLastPage=" + (MAX_PAGES + 1),
                            "-sOutputFile=" + workDirectory.resolve("page-%04d.png"),
                            "-f",
                            pdf.toAbsolutePath().toString()
                    ),
                    commandLog
            );

            List<Path> pages;

            try (var files = Files.list(workDirectory)) {
                pages = files
                        .filter(path -> path.getFileName()
                                .toString().matches("page-\\d+\\.png"))
                        .sorted()
                        .toList();
            }

            if (pages.isEmpty()) {
                throw new OcrException("PDF contains no renderable pages");
            }

            if (pages.size() > MAX_PAGES) {
                throw new OcrException(
                        "OCR supports at most " + MAX_PAGES + " pages"
                );
            }

            StringBuilder documentText = new StringBuilder();

            for (Path page : pages) {
                Path outputBase = workDirectory.resolve("recognized");

                runCommand(
                        List.of(
                                "tesseract",
                                page.toString(),
                                outputBase.toString(),
                                "-l",
                                languages
                        ),
                        commandLog
                );

                String pageText = Files.readString(
                        workDirectory.resolve("recognized.txt"),
                        StandardCharsets.UTF_8
                );

                if (!documentText.isEmpty()) {
                    documentText.append("\n\n");
                }

                documentText.append(pageText.strip());
            }

            log.info(
                    "OCR extraction completed: pages={}, characters={}",
                    pages.size(),
                    documentText.length()
            );

            return documentText.toString();

        } catch (IOException e) {
            throw new OcrException("Could not read or write OCR files", e);
        } finally {
            if (workDirectory != null) {
                cleanup(workDirectory);
            }
        }
    }

    private void runCommand(List<String> command, Path commandLog)
            throws IOException {

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(commandLog.toFile())
                .start();

        try {
            boolean finished = process.waitFor(
                    timeoutSeconds,
                    TimeUnit.SECONDS
            );

            if (!finished) {
                throw new OcrException(
                        command.getFirst() + " exceeded timeout of "
                                + timeoutSeconds + " seconds"
                );
            }

            if (process.exitValue() != 0) {
                String diagnostic;

                try (var input = Files.newInputStream(commandLog)) {
                    diagnostic = new String(
                            input.readNBytes(4096),
                            StandardCharsets.UTF_8
                    );
                }

                throw new OcrException(
                        command.getFirst() + " failed with exit code "
                                + process.exitValue() + ": " + diagnostic
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcrException("OCR processing interrupted", e);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();

                try {
                    process.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void cleanup(Path directory) {
        try (var files = Files.walk(directory)) {
            List<Path> paths = files
                    .sorted(Comparator.reverseOrder())
                    .toList();

            for (Path path : paths) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("Could not remove OCR temporary path={}", path, e);
                }
            }

        } catch (IOException e) {
            log.warn("Could not inspect OCR temporary directory={}", directory, e);
        }
    }
}