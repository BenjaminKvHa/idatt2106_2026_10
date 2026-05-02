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

/**
 * Catalog service that loads and provides chapter-specific tips from a JSON resource.
 * 
 * <p>Tips are loaded during initialization from {@code classpath:chapter-tips.json}.
 * The catalog ensures that tips are cleaned (no blanks) and de-duplicated.
 */
@Component
public class ChapterTipsCatalog {

    private final Map<Long, List<String>> tipsByChapterId;

    /**
     * Initializes the catalog by reading tips from the provided JSON resource.
     *
     * @param objectMapper the Jackson mapper for JSON parsing
     * @param tipsResource the resource pointing to the tips JSON file
     * @throws IOException if the resource cannot be read
     */
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

    /**
     * Retrieves the list of tips associated with a specific chapter.
     *
     * @param chapterId the ID of the chapter
     * @return a list of tip strings, or an empty list if no tips are found for the ID
     */
    public List<String> tipsForChapter(Long chapterId) {
        return tipsByChapterId.getOrDefault(chapterId, List.of());
    }

    /**
     * Cleans and de-duplicates a list of tips.
     * 
     * @param tips the raw list of tips
     * @return a cleaned list of unique, non-blank tip strings
     */
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
