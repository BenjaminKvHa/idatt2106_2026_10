package com.nettdet.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Marshalling layer for the JSON document persisted in
 * {@code pupil_chapter_bridge_progression.notebook_json}.
 * <p>
 * Each chapter notebook is a single JSON blob containing the collected tips the pupil has
 * unlocked plus the pupil's free-form notes. This service centralises parsing, serialising
 * and mutating that blob so that callers (such as {@link NotebookService} and
 * {@link TipService}) never have to touch raw JSON.
 * <p>
 * Parsing is intentionally lenient: malformed or empty payloads collapse to an empty
 * {@link ChapterNotebookData} rather than throwing, so a corrupted notebook column never
 * blocks chapter access. Tip lists are normalised by trimming nulls/blanks and removing
 * duplicates while preserving insertion order.
 */
@Service
public class NotebookJsonService {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a {@code NotebookJsonService} backed by the application's shared Jackson mapper.
     *
     * @param objectMapper the JSON marshaller used for both parsing and serialising notebooks
     */
    public NotebookJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses a notebook JSON blob into its in-memory representation.
     * <p>
     * Returns {@link ChapterNotebookData#empty()} if the input is {@code null}, blank, or
     * malformed. The returned object always has a non-null tip list (deduplicated, blanks
     * removed) and a non-null {@code ownNotes} string.
     *
     * @param notebookJson the raw JSON stored in the progression row, may be {@code null}
     * @return a never-{@code null} {@link ChapterNotebookData}
     */
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

    /**
     * Serialises a {@link ChapterNotebookData} value to its JSON representation.
     *
     * @param notebookData the in-memory notebook to encode
     * @return the JSON string suitable for storing in the progression row
     * @throws IllegalStateException if Jackson fails to serialise the value (should not happen
     *                               for the well-formed records this service constructs)
     */
    public String serialize(ChapterNotebookData notebookData) {
        try {
            return objectMapper.writeValueAsString(notebookData);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize notebook data");
        }
    }

    /**
     * Returns the JSON encoding of a fresh, empty notebook.
     * <p>
     * Used to seed {@code notebook_json} when a new {@code PupilChapterBridgeProgression} row
     * is created so the column always contains valid JSON.
     *
     * @return JSON string for an empty notebook (no tips, blank own-notes)
     */
    public String defaultNotebookJson() {
        return serialize(ChapterNotebookData.empty());
    }

    /**
     * Adds a single tip to an existing notebook.
     * <p>
     * Convenience wrapper around {@link #addTips(String, List)}. A {@code null} or blank tip
     * is silently dropped, leaving the notebook unchanged.
     *
     * @param notebookJson the existing notebook JSON, may be {@code null}/blank
     * @param tip the tip to insert
     * @return the merged notebook plus a {@code changed} flag indicating whether the tip was new
     */
    public TipAddResult addTip(String notebookJson, String tip) {
        if (tip == null) {
            return addTips(notebookJson, List.of());
        }
        return addTips(notebookJson, List.of(tip));
    }

    /**
     * Adds multiple tips to an existing notebook, preserving order and skipping duplicates.
     * <p>
     * Tips already present in the notebook are not re-inserted; {@code null} and blank tips
     * are filtered out. The pupil's {@code ownNotes} field is preserved verbatim. The
     * returned {@link TipAddResult#changed()} flag is {@code true} only if at least one new
     * tip was actually added.
     *
     * @param notebookJson the existing notebook JSON, may be {@code null}/blank
     * @param tipsToAdd the candidate tips to merge in
     * @return the merged notebook, its serialised form, and a flag for whether anything changed
     */
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

    /**
     * Result of an add-tip operation.
     *
     * @param notebookData the merged notebook after insertion
     * @param notebookJson the serialised form of {@code notebookData}, ready to persist
     * @param changed     {@code true} iff at least one new tip was actually added
     */
    public record TipAddResult(
        ChapterNotebookData notebookData,
        String notebookJson,
        boolean changed
    ) {
    }
}
