package vn.edu.davs.backend.enu;

/**
 * MinIO object storage buckets provisioned for DAVS (T-009 / infra/docker-compose.yml).
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
