package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.model.entity.ContentRevision;
import com.blog.personalblogbackend.common.revision.RevisionTargetType;

public interface RevisionContentStorage {

    void persistAfterInsert(ContentRevision revision, RevisionTargetType type, String content);

    String loadContent(ContentRevision revision);

    int migrateLegacyBatch(int batchSize);
}
