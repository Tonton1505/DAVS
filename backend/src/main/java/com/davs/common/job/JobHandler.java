package com.davs.common.job;

public interface JobHandler {
    JobType supports();
    void handle(JobEntity job) throws Exception;
}
