package com.blog.ai.rag.chunk;

import com.blog.ai.rag.model.KnowledgeChunkDoc;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class ChunkingService {

    public List<KnowledgeChunkDoc> chunk(Long docId, String docTitle, String text, int size, int overlap) {
        List<KnowledgeChunkDoc> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        List<String> segments = semanticSplit(text);
        int ordinal = 0;
        StringBuilder buffer = new StringBuilder();
        for (String seg : segments) {
            if (buffer.length() + seg.length() > size && buffer.length() > 0) {
                chunks.add(buildChunk(docId, docTitle, ordinal++, buffer.toString()));
                String tail = tailOverlap(buffer.toString(), overlap);
                buffer.setLength(0);
                if (tail != null && !tail.isBlank()) {
                    buffer.append(tail);
                }
            }
            buffer.append(seg);
            while (buffer.length() >= size) {
                String piece = buffer.substring(0, size);
                chunks.add(buildChunk(docId, docTitle, ordinal++, piece));
                int start = Math.max(0, size - overlap);
                buffer = new StringBuilder(buffer.substring(start));
            }
        }
        if (buffer.length() > 0) {
            chunks.add(buildChunk(docId, docTitle, ordinal++, buffer.toString()));
        }
        return chunks;
    }

    private KnowledgeChunkDoc buildChunk(Long docId, String docTitle, int ordinal, String text) {
        KnowledgeChunkDoc doc = new KnowledgeChunkDoc();
        doc.setChunkId(docId + "_" + ordinal);
        doc.setDocId(docId);
        doc.setDocTitle(docTitle);
        doc.setOrdinal(ordinal);
        doc.setText(text.trim());
        doc.setMetadata(new HashMap<>());
        return doc;
    }

    private List<String> semanticSplit(String text) {
        List<String> out = new ArrayList<>();
        String[] parts = text.split("(?m)\\n\\s*\\n|(?=^#{1,6}\\s)");
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed + "\n");
            }
        }
        if (out.isEmpty()) {
            out.add(text);
        }
        return out;
    }

    private String tailOverlap(String s, int overlap) {
        if (overlap <= 0 || s.length() <= overlap) {
            return s;
        }
        return s.substring(s.length() - overlap);
    }
}
