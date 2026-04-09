package com.sism.iam.application.service;

public interface LoginAttemptService {

    void assertNotBlocked(String username, String clientKey);

    void recordFailure(String username, String clientKey);

    void recordSuccess(String username, String clientKey);
}
