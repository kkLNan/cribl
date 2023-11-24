package com.cribl.loglookupapp.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

@Slf4j
@Service
public class LogFileIndexer {


    private final String logDirectory;
    private final boolean dirtyLoad;

    public LogFileIndexer(
            @Value("${cribl.log.directory}") String logDirectory,
            @Value("${cribl.log.dirty-load}") boolean dirtyLoad) {
        this.logDirectory = logDirectory;
        this.dirtyLoad = dirtyLoad;
    }

    public List<Long> getIndexMap(String file) throws IOException {

        if (indexMap.get(file) == null) {
            List<Long> linePositions = readIndexFile(file.toString()+".index");
            indexMap.put(file, linePositions);
        }
        return indexMap.get(file);
    }

    private final Map<String, List<Long>> indexMap = new HashMap<>();

    @PostConstruct
    public void initialize() throws IOException {
        if (!dirtyLoad) {
            createIndexesForDirectory(logDirectory);
        }
        loadIndexesIntoMemory(logDirectory);
        startMonitoring();
    }

    public void createIndexesForDirectory(String directoryPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            if (!file.getFileName().toString().startsWith(".") && file.getFileName().toString().endsWith(".log"))
                                createIndexForFile(file.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }


    /**
     * This method will create the index based off the newline location, which will help later on in reading the content more efficient
     *
     * @param logFilePath
     * @throws IOException
     */
    public void createIndexForFile(String logFilePath) throws IOException {
        String indexFilePath = logFilePath + ".index";
        try (RandomAccessFile logFile = new RandomAccessFile(logFilePath, "r");
             FileChannel logChannel = logFile.getChannel();
             BufferedWriter indexWriter = new BufferedWriter(new FileWriter(indexFilePath))
        ) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);

            long position = 0;
            long lineStart = 0;

            while (logChannel.read(buffer) != -1) {
                buffer.flip();

                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    position++;

                    if (b == '\n') {
                        indexWriter.write(lineStart + "\n");
                        lineStart = position;
                    }
                }

                buffer.compact();
            }

            // Always write the start position of the last line
            indexWriter.write(lineStart + "\n");
            log.info("Created Index for: {}", logFilePath);
        }
    }


    private void loadIndexesIntoMemory(String directoryPath) throws IOException {

        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".index"))
                    .forEach(file -> {
                        try {
                            List<Long> linePositions = readIndexFile(file.toString());
                            indexMap.put(file.toString().replace(".index", ""), linePositions);
                            log.info("Warm up index in memory: {}", file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }


    private List<Long> readIndexFile(String indexFilePath) throws IOException {
        List<Long> linePositions = new ArrayList<>();

        try (BufferedReader indexReader = new BufferedReader(new FileReader(indexFilePath))) {
            String line;
            while ((line = indexReader.readLine()) != null) {
                linePositions.add(Long.parseLong(line));
            }
        }

        return linePositions;
    }

    public void startMonitoring() throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Paths.get(logDirectory).register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

        new Thread(() -> {
            while (true) {
                WatchKey key;
                try {
                    key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path filePath = ((WatchEvent<Path>) event).context();
                        if ((kind == ENTRY_MODIFY || kind == ENTRY_CREATE) && !filePath.toString().endsWith(".index")) {
                            String logFilePath = logDirectory + "/" + filePath;
                            String indexFilePath = logFilePath + ".index";
                            if (Files.exists(Path.of(indexFilePath))) {
                                updateIndex(logFilePath);
                            }
                            else {
                                createIndexForFile(logFilePath);
                                List<Long> linePositions = readIndexFile(indexFilePath);
                                indexMap.put(logFilePath, linePositions);
                            }
                        }
                        if (kind == ENTRY_DELETE && filePath.toString().endsWith(".log")) {
                            String logPath = logDirectory + "/" + filePath;
                            indexMap.remove(logPath);
                            String indexFilePath = logPath + ".index";
                            Files.delete(Path.of(indexFilePath));
                        }
                    }
                    key.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        log.info("Dynamic index update started, monitoring {}", logDirectory);

    }

    public void updateIndex(String logFilePath) throws IOException {
        log.info("File change detected, update log index for file: {}", logFilePath);

        List<Long> currentPositions = indexMap.getOrDefault(logFilePath, new ArrayList<>());
        long lastPosition = currentPositions.isEmpty() ? 0 : currentPositions.get(currentPositions.size() - 1);

        String indexFilePath = logFilePath + ".index";
        try (RandomAccessFile logFile = new RandomAccessFile(logFilePath, "r");
             FileChannel logChannel = logFile.getChannel();
             BufferedWriter indexWriter = new BufferedWriter(new FileWriter(indexFilePath, true))) { // Append mode

            logFile.seek(lastPosition);
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            long position = lastPosition;
            long lineStart = lastPosition;

            while (logChannel.read(buffer) != -1) {
                buffer.flip();

                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    position++;

                    if (b == '\n') {
                        indexWriter.write(lineStart + "\n");
                        lineStart = position;
                        currentPositions.add(lineStart);
                    }
                }

                buffer.compact();
            }

            if (lineStart != position) {
                indexWriter.write(lineStart + "\n");
            }
            indexWriter.write(logFile.length() + "\n");

            indexMap.put(logFilePath, currentPositions);
        }
        log.info("File change detected, update log index for file: {} completed", logFilePath);

    }

}
