package com.blog.content.service;

import com.blog.content.model.entity.ContentRevision;
import com.blog.content.common.revision.RevisionTargetType;

public interface RevisionContentStorage {

    void persistAfterInsert(ContentRevision revision, RevisionTargetType type, String content);

    String loadContent(ContentRevision revision);

    int migrateLegacyBatch(int batchSize);
}
