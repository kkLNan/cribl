package com.cribl.loglookupapp.service;


import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.StandardOpenOption.APPEND;
import static org.assertj.core.api.Assertions.assertThat;

public class LogFileIndexerTest {

    private LogFileIndexer logFileIndexer = new LogFileIndexer(
            "./", false
    );

    private final List<String> numList = List.of("1", "2", "3", "4", "5");
    private final List<Long> expectedIndexes = List.of(0L, 2L, 4L, 6L, 8L, 10L);


    @Test
    void testCreateFile() throws IOException {
        Files.write(Path.of("./tmp.log"), numList);
        logFileIndexer.initialize();

        List<Long> indexMap = logFileIndexer.getIndexMap("./tmp.log");
        assertThat(indexMap).hasSize(6);
        assertThat(indexMap).containsAll(expectedIndexes);


    }

    @Test
    void testUpdateIndexAsync() throws IOException, InterruptedException {
        String temp2Path = "./tmp2.log";

        if (Files.exists(Path.of(temp2Path)))
            Files.delete(Path.of(temp2Path));
        if (Files.exists(Path.of(temp2Path + ".index")))
            Files.delete(Path.of(temp2Path + ".index"));

        Files.writeString(Path.of(temp2Path),"a\nb\nc\n");
        logFileIndexer.createIndexForFile(temp2Path);
        assertThat(logFileIndexer.getIndexMap(temp2Path)).hasSize(4);

        Files.writeString(Path.of(temp2Path), "a\nb\nc\n", APPEND);

        // Give some breath room for the WatchService to pick up the index change.
        logFileIndexer.updateIndex(temp2Path);

        assertThat(Files.readAllLines(Path.of(temp2Path + ".index"))).hasSize(7);



    }
}
