package com.nettdet.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NotebookJsonService {

    private final ObjectMapper objectMapper;

    public NotebookJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChapterNotebookData parse(String notebookJson) {
        if (notebookJson == null || notebookJson.isBlank()) {
            return ChapterNotebookData.empty();
        }

        try {
            ChapterNotebookData notebookData = objectMapper.readValue(notebookJson, ChapterNotebookData.class);
            List<String> tips = notebookData.tips() == null ? List.of() : deduplicate(notebookData.tips());
            String ownNotes = notebookData.ownNotes() == null ? "" : notebookData.ownNotes();
            return new ChapterNotebookData(tips, ownNotes);
        } catch (JsonProcessingException exception) {
            return ChapterNotebookData.empty();
        }
    }

    public String serialize(ChapterNotebookData notebookData) {
        try {
            return objectMapper.writeValueAsString(notebookData);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize notebook data");
        }
    }

    public String defaultNotebookJson() {
        return serialize(ChapterNotebookData.empty());
    }

    public TipAddResult addTip(String notebookJson, String tip) {
        if (tip == null) {
            return addTips(notebookJson, List.of());
        }
        return addTips(notebookJson, List.of(tip));
    }

    public TipAddResult addTips(String notebookJson, List<String> tipsToAdd) {
        ChapterNotebookData existing = parse(notebookJson);
        LinkedHashSet<String> tips = new LinkedHashSet<>(existing.tips());

        boolean changed = false;
        for (String tip : tipsToAdd) {
            if (tip != null && !tip.isBlank() && tips.add(tip)) {
                changed = true;
            }
        }

        ChapterNotebookData updated = new ChapterNotebookData(new ArrayList<>(tips), existing.ownNotes());
        return new TipAddResult(updated, serialize(updated), changed);
    }

    private List<String> deduplicate(List<String> tips) {
        LinkedHashSet<String> deduplicated = new LinkedHashSet<>();
        for (String tip : tips) {
            if (tip != null && !tip.isBlank()) {
                deduplicated.add(tip);
            }
        }
        return new ArrayList<>(deduplicated);
    }

    public record TipAddResult(
        ChapterNotebookData notebookData,
        String notebookJson,
        boolean changed
    ) {
    }
}
