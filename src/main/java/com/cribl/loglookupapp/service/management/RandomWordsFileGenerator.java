package com.cribl.loglookupapp.service.management;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

@Service
@Slf4j
public class RandomWordsFileGenerator {

    @Value("${cribl.log.directory}")
    private String directory;

    public long generateFile(String fileName, int numberOfLines) {
        fileName = String.format("%s/%s", directory, fileName);
        log.info("Create a new file: {}", fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (int lineNumber = 1; lineNumber <= numberOfLines; lineNumber++) {
                String randomWord = generateRandomWord();
                writer.write(System.currentTimeMillis() + ":[" + lineNumber + "]: I am eating a(n) " + randomWord + " and that's very delicious!!!!");
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("{} has been created. {} rows", fileName, numberOfLines);
        File file = new File(fileName);
        long fileSize = file.length();
        log.info("{} has been created with {} rows. File size: {} bytes", fileName, numberOfLines, fileSize);

        return fileSize;
    }

    private static String generateRandomWord() {
        String[] words = {
                "apple", "banana", "cherry", "date", "elderberry", "fig", "grape",
                "honeydew", "kiwi", "lemon", "mango", "nectarine", "orange", "pear",
                "quince", "raspberry", "strawberry", "tangerine", "watermelon"
        };

        // Generate a random index to select a word from the list
        int randomIndex = new Random().nextInt(words.length);

        return words[randomIndex];
    }
}
