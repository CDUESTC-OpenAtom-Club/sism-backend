package com.sism.strategy.domain.repository;

import com.sism.strategy.domain.Indicator;
import com.sism.organization.domain.SysOrg;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * IndicatorRepository - 指标仓储接口
 * 定义在领域层，由基础设施层实现
 */
public interface IndicatorRepository {

    /**
     * 根据ID查询指标
     */
    Optional<Indicator> findById(Long id);

    /**
     * 查询所有指标
     */
    List<Indicator> findAll();

    /**
     * 分页查询所有指标
     */
    Page<Indicator> findAll(Pageable pageable);

    /**
     * 根据目标组织查询指标
     */
    List<Indicator> findByTargetOrg(SysOrg targetOrg);

    /**
     * 根据目标组织ID查询指标
     */
    List<Indicator> findByTargetOrgId(Long targetOrgId);

    /**
     * 根据拥有组织查询指标
     */
    List<Indicator> findByOwnerOrg(SysOrg ownerOrg);

    /**
     * 根据拥有组织ID查询指标
     */
    List<Indicator> findByOwnerOrgId(Long ownerOrgId);

    /**
     * 根据状态查询指标
     */
    List<Indicator> findByStatus(String status);

    /**
     * 根据状态分页查询指标
     */
    Page<Indicator> findByStatus(String status, Pageable pageable);

    /**
     * 根据父指标查询子指标
     */
    List<Indicator> findByParentIndicatorId(Long parentIndicatorId);

    /**
     * 根据任务ID查询指标
     */
    List<Indicator> findByTaskId(Long taskId);

    /**
     * 保存指标
     */
    Indicator save(Indicator indicator);

    /**
     * 删除指标
     */
    void delete(Indicator indicator);

    /**
     * 检查指标是否存在
     */
    boolean existsById(Long id);

    /**
     * 查询一级指标（战略到职能）
     */
    List<Indicator> findFirstLevelIndicators();

    /**
     * 查询二级指标（职能到学院）
     */
    List<Indicator> findSecondLevelIndicators();

    /**
     * 根据关键字搜索指标
     */
    List<Indicator> findByKeyword(String keyword);

    /**
     * 根据任务ID列表分页查询指标
     */
    Page<Indicator> findByTaskIds(List<Long> taskIds, Pageable pageable);
}
