package com.nettdet.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ChapterTipsCatalog {

    private final Map<Long, List<String>> tipsByChapterId;

    public ChapterTipsCatalog(ObjectMapper objectMapper,
                              @Value("classpath:chapter-tips.json") Resource tipsResource) throws IOException {
        Map<String, List<String>> rawTips = objectMapper.readValue(
                tipsResource.getInputStream(),
                new TypeReference<>() {
                });

        Map<Long, List<String>> parsedTips = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : rawTips.entrySet()) {
            parsedTips.put(Long.valueOf(entry.getKey()), cleanTips(entry.getValue()));
        }

        this.tipsByChapterId = Map.copyOf(parsedTips);
    }

    public List<String> tipsForChapter(Long chapterId) {
        return tipsByChapterId.getOrDefault(chapterId, List.of());
    }

    private List<String> cleanTips(List<String> tips) {
        if (tips == null) {
            return List.of();
        }

        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String tip : tips) {
            if (tip != null && !tip.isBlank()) {
                cleaned.add(tip);
            }
        }

        return List.copyOf(new ArrayList<>(cleaned));
    }
}
