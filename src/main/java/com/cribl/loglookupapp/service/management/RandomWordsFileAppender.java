package com.cribl.loglookupapp.service.management;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.stream.Stream;

@Service
public class RandomWordsFileAppender {

    @Value("${cribl.log.directory}")
    private String directory;

    public void appendFile(String fileName, int numberOfLinesToAppend) {
        fileName = String.format("%s/%s", directory, fileName);
        int existingLineCount = countExistingLines(fileName) + 1; // you want to start at the next line

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) { // Append mode
            for (int i = 0; i < numberOfLinesToAppend; i++) {
                String randomWord = generateRandomWord();
                writer.write(System.currentTimeMillis() + ":[" + (existingLineCount + i) + "]: I am eating a(n) " + randomWord + " and that's very delicious!!!!");
                writer.newLine();
            }
            System.out.println("Appended " + (numberOfLinesToAppend) + " lines to '" + fileName + "'.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int countExistingLines(String fileName) {
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            return (int) stream.count();
        } catch (IOException e) {
            // If the file does not exist, return 0
            return 0;
        }
    }

    private static String generateRandomWord() {
        String[] words = {
                "apple", "banana", "cherry", "date", "elderberry", "fig", "grape",
                "honeydew", "kiwi", "lemon", "mango", "nectarine", "orange", "pear",
                "quince", "raspberry", "strawberry", "tangerine", "watermelon"
        };
        int randomIndex = new Random().nextInt(words.length);
        return words[randomIndex];
    }
}
