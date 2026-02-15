package com.sism.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;

/**
 * 数据库表重命名监听器
 * 在Spring容器刷新后、Hibernate初始化前执行
 *
 * DEPRECATED: This listener has been disabled as the table rename is now complete.
 * The strategic_task table has been successfully renamed to sys_task.
 */
// @Component  // DISABLED - Migration complete
@RequiredArgsConstructor
@Slf4j
public class TableRenameListener implements ApplicationListener<ContextRefreshedEvent> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            // 检查是否需要重命名
            Integer tableExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'strategic_task'",
                Integer.class
            );

            if (tableExists != null && tableExists > 0) {
                log.info("检测到 strategic_task 表，重命名为 sys_task...");
                jdbcTemplate.execute("ALTER TABLE strategic_task RENAME TO sys_task");
                log.info("✓ 表重命名完成");
            }
        } catch (Exception e) {
            log.warn("表重命名检查失败: {}", e.getMessage());
        }
    }
}
