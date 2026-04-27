package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import com.fasterxml.jackson.databind.ObjectMapper;

class ChapterTipsCatalogTest {

    @DisplayName("Loads configured tips by chapter id")
    @Test
    void tipsForChapter_whenChapterHasTips_returnsConfiguredTips() throws Exception {
        ChapterTipsCatalog catalog = catalog("""
                {
                  "1": ["First tip", "Second tip"]
                }
                """);

        List<String> tips = catalog.tipsForChapter(1L);

        assertEquals(List.of("First tip", "Second tip"), tips);
    }

    @DisplayName("Returns empty list when chapter has no configured tips")
    @Test
    void tipsForChapter_whenChapterHasNoTips_returnsEmptyList() throws Exception {
        ChapterTipsCatalog catalog = catalog("""
                {
                  "1": ["First tip"]
                }
                """);

        List<String> tips = catalog.tipsForChapter(2L);

        assertEquals(List.of(), tips);
    }

    @DisplayName("Ignores blank and duplicate tips while preserving order")
    @Test
    void tipsForChapter_whenTipsContainBlanksAndDuplicates_returnsCleanedTips() throws Exception {
        ChapterTipsCatalog catalog = catalog("""
                {
                  "1": ["First tip", " ", "Second tip", "First tip"]
                }
                """);

        List<String> tips = catalog.tipsForChapter(1L);

        assertEquals(List.of("First tip", "Second tip"), tips);
    }

    private ChapterTipsCatalog catalog(String json) throws Exception {
        return new ChapterTipsCatalog(
                new ObjectMapper(),
                new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)));
    }
}
