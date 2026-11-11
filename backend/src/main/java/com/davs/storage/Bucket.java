package com.davs.storage;

/**
 * Standard MinIO object storage buckets used by DAVS (skill S7).
 */
public enum Bucket {

    WORKBOOKS("workbooks"),
    DATASETS("datasets"),
    EXPORTS("exports"),
    ATTACHMENTS("attachments");

    private final String bucketName;

    Bucket(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBucketName() {
        return bucketName;
    }
}
