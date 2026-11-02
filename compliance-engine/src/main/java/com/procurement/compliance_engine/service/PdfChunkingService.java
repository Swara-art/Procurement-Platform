package com.procurement.compliance_engine.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfChunkingService {

    private static final int CHUNK_SIZE = 1200;
    private static final int OVERLAP = 200;
    private static final int MAX_PAGES = 500;
    private static final int MAX_EXTRACTED_CHARACTERS = 2_000_000;

    public record PdfChunk(int pageNumber, int chunkIndex, String content) {
    }

    public List<PdfChunk> extractAndChunk(Path pdfPath) throws IOException {
        List<PdfChunk> chunks = new ArrayList<>();
        int totalCharacters = 0;

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            if (document.isEncrypted()) {
                throw new IOException("Encrypted PDFs are not supported");
            }
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new IOException("PDF exceeds the 500 page processing limit");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            int chunkIndex = 0;
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).replaceAll("\\s+", " ").trim();
                if (text.isBlank()) {
                    continue;
                }
                totalCharacters += text.length();
                if (totalCharacters > MAX_EXTRACTED_CHARACTERS) {
                    throw new IOException("PDF exceeds the text extraction limit");
                }

                for (int start = 0; start < text.length();) {
                    int end = Math.min(start + CHUNK_SIZE, text.length());
                    chunks.add(new PdfChunk(page, chunkIndex++, text.substring(start, end)));
                    if (end == text.length()) {
                        break;
                    }
                    start = end - OVERLAP;
                }
            }
        }
        return chunks;
    }
}
