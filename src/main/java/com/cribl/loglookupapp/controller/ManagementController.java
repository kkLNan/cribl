package com.cribl.loglookupapp.controller;

import com.cribl.loglookupapp.service.management.RandomWordsFileAppender;
import com.cribl.loglookupapp.service.management.RandomWordsFileGenerator;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ManagementController {

    private final RandomWordsFileAppender randomWordsFileAppender;
    private final RandomWordsFileGenerator randomWordsFileGenerator;

    @GetMapping("/api/manage/generate")
    public long createFile(
            @RequestParam String filename,
            @RequestParam String lines) {
        return randomWordsFileGenerator.generateFile(filename, Integer.parseInt(lines));
    }

    @GetMapping("/api/manage/append")
    public void appendFile(
            @RequestParam String filename,
            @RequestParam String lines) {
        randomWordsFileAppender.appendFile(filename, Integer.parseInt(lines));
    }
}
