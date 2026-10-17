package io.github.annakryzhanovska.summarizer;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Summarizes several files in parallel on a fixed thread pool. */
public final class BatchSummarizer {

    private BatchSummarizer() {
    }

    /** Outcome for one file: either a summary or an error message. */
    public record Result(Path file, Summary summary, String error) {

        public boolean failed() {
            return error != null;
        }
    }

    /**
     * Summarizes all files and returns the results in the same order as the input,
     * no matter which thread finished first. A file that cannot be read does not stop the others.
     */
    public static List<Result> summarizeAll(List<Path> files, SummaryOptions options, int threads)
            throws InterruptedException {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1");
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(threads, Math.max(1, files.size())));
        try {
            List<Callable<Result>> tasks = new ArrayList<>();
            for (Path file : files) {
                tasks.add(() -> summarizeFile(file, options));
            }
            List<Result> results = new ArrayList<>(files.size());
            // invokeAll waits for every task and returns the futures in task order
            for (Future<Result> future : pool.invokeAll(tasks)) {
                results.add(future.get());
            }
            return results;
        } catch (ExecutionException e) {
            // summarizeFile catches expected failures, so this is a programming error
            throw new IllegalStateException("Unexpected error while summarizing", e.getCause());
        } finally {
            pool.shutdown();
        }
    }

    static Result summarizeFile(Path file, SummaryOptions options) {
        try {
            String text = Files.readString(file);
            return new Result(file, Summarizer.summarize(text, options), null);
        } catch (NoSuchFileException e) {
            return new Result(file, null, file + ": file not found");
        } catch (MalformedInputException e) {
            return new Result(file, null, file + ": not a UTF-8 text file");
        } catch (IOException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return new Result(file, null, file + ": could not read (" + reason + ")");
        }
    }
}
