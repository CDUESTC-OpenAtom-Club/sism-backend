package com.sism.alert.application;

import java.util.Set;

public interface IndicatorAccessPort {

    Set<Long> findAccessibleIndicatorIds(Long orgId);

    Set<Long> findAllIndicatorIds();
}
