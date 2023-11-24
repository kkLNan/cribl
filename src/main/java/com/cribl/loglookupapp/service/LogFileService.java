package com.cribl.loglookupapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LogFileService {

    @Value("${cribl.log.directory}")
    private String directory;

    private final LogFileIndexer logFileIndexer;

    public LogFileService(LogFileIndexer logFileIndexer) {
        this.logFileIndexer = logFileIndexer;
    }
    public List<String> getLastMatchingLines(String logFilePath, String keyword, int numberOfLines) throws IOException {
        log.info("Searching {}, {}, {}", logFilePath, keyword, numberOfLines);
        logFilePath = String.format("%s/%s", directory, logFilePath);
        List<Long> indexes = logFileIndexer.getIndexMap(logFilePath);


        try (RandomAccessFile logFile = new RandomAccessFile(logFilePath, "r")) {
            List<String> matchingLines = new ArrayList<>();
            long fileLength = logFile.length();
            int indexStep = (indexes.size() / 250) == 0 ? 1 : indexes.size() / 250;

            // Start from the end of the file and move backwards
            int indexPos = !indexes.isEmpty() ? indexes.size() - 1 : -1;

            while (indexPos >= 0 && matchingLines.size() < numberOfLines) {
                long currentBlockStartPosition = (indexPos > 0) ? indexes.get(Math.max(indexPos - indexStep, 0)) : 0;
                long nextBlockEndPosition = Math.min(indexes.get(indexPos), fileLength);

                // Calculate dynamic block size
                int dynamicBlockSize = (int) (nextBlockEndPosition - currentBlockStartPosition);
                byte[] block = new byte[dynamicBlockSize];

                logFile.seek(currentBlockStartPosition);
                int bytesRead = logFile.read(block, 0, dynamicBlockSize);

                List<Integer> newlinePositions = new ArrayList<>();
                for (int i = Math.max(indexPos - indexStep, 0); i <= indexPos; i++) {
                    if (indexes.get(i) >= currentBlockStartPosition && indexes.get(i) < nextBlockEndPosition) {
                        newlinePositions.add((int)(indexes.get(i) - currentBlockStartPosition));
                    }
                }

                matchingLines.addAll(findMatchesInBlock(block, bytesRead, keyword, numberOfLines - matchingLines.size(), newlinePositions));

                // Move to the previous 8th block
                indexPos -= indexStep;
            }
            log.info("Found: {}", matchingLines.size());
            return matchingLines;
        } catch (FileNotFoundException e) {
            log.error("No file found: {}", logFilePath);
            return List.of();
        }
    }


    private List<String> findMatchesInBlock(byte[] block, int length, String keyword, int neededMatches, List<Integer> newlinePositions) {
        List<String> matches = new ArrayList<>();
        int startOfLine = length;
        int linesFound = 0;

        // Iterate through the newline positions in reverse
        for (int i = newlinePositions.size() - 1; i >= 0 && linesFound < neededMatches; i--) {
            int endOfLine = newlinePositions.get(i);
            String line = new String(block, endOfLine, startOfLine - endOfLine, StandardCharsets.UTF_8);

            if (!line.isEmpty() && line.contains(keyword)) {
                matches.add(line.trim());
                linesFound++;
            }
            startOfLine = endOfLine;
        }

        return matches;
    }




}
