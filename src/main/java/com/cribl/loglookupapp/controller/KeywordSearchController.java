package com.cribl.loglookupapp.controller;

import com.cribl.loglookupapp.service.LogFileService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@AllArgsConstructor
public class KeywordSearchController {

    private final LogFileService logFileService;

    @GetMapping("/api/keyword")
    public List<String> getLogEntries(
            @RequestParam String filename,
            @RequestParam(required = false) String keyword,
            @RequestParam int lastN) throws IOException {
        return logFileService.getLastMatchingLines(filename, keyword, lastN);
    }
}
