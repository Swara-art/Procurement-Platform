package com.procurement.compliance_engine.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfChunkingServiceTest {

    @Test
    void shouldExtractAndChunkPdf() throws Exception {
        Path pdf = Files.createTempFile("financial-statement-", ".pdf");

        try {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText("Annual turnover: Rs. 3200000");
                    stream.endText();
                }

                document.save(pdf.toFile());
            }

            List<PdfChunkingService.PdfChunk> chunks =
                    new PdfChunkingService().extractAndChunk(pdf);

            assertFalse(chunks.isEmpty());
            assertEquals(1, chunks.get(0).pageNumber());
            assertTrue(chunks.get(0).content().contains("3200000"));
        } finally {
            Files.deleteIfExists(pdf);
        }
    }
}
