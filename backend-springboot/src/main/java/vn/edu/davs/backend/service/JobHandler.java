package vn.edu.davs.backend.service;

import vn.edu.davs.backend.model.JobRecord;

/**
 * Handler for a specific background job {@link #type()}.
 */
public interface JobHandler {

    /**
     * Unique job type identifier matching {@code jobs.type}.
     */
    String type();

    /**
     * Executes the job. Throwing any exception triggers retry until {@code max_attempts} is reached.
     */
    void handle(JobRecord job) throws Exception;
}
