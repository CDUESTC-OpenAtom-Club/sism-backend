package com.sism.execution.domain.repository;

public interface PlanStatusSyncGateway {

    void syncBackToDraft(Long planId);
}
