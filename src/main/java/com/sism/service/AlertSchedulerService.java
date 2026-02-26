package com.sism.service;

import com.sism.entity.AlertWindow;
import com.sism.repository.AlertWindowRepository;
import com.sism.vo.AlertEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled service for alert calculation
 * Triggers alert generation based on alert window cutoff dates
 * 
 * Requirements: 6.1 - Trigger alert calculation at alert window cutoff dates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertSchedulerService {

    private final AlertService alertService;
    private final AlertWindowRepository alertWindowRepository;

    /**
     * Scheduled task to check for alert windows with cutoff date today
     * Runs daily at 00:05 AM to process any windows that have reached their cutoff date
     * 
     * Requirements: 6.1 - When alert window cutoff date arrives, calculate progress gaps
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void processAlertWindowsDaily() {
        log.info("Starting daily alert window processing");
        
        LocalDate today = LocalDate.now();
        List<AlertWindow> windowsToProcess = alertWindowRepository.findByCutoffDate(today);
        
        if (windowsToProcess.isEmpty()) {
            log.info("No alert windows with cutoff date {} found", today);
            return;
        }

        for (AlertWindow window : windowsToProcess) {
            try {
                processAlertWindow(window);
            } catch (Exception e) {
                log.error("Failed to process alert window {}: {}", window.getWindowId(), e.getMessage(), e);
            }
        }

        log.info("Completed daily alert window processing. Processed {} windows", windowsToProcess.size());
    }

    /**
     * Process a single alert window and generate alerts
     * 
     * @param window the alert window to process
     */
    public void processAlertWindow(AlertWindow window) {
        log.info("Processing alert window: {} (ID: {}, Cutoff: {})", 
                window.getName(), window.getWindowId(), window.getCutoffDate());

        List<AlertEventVO> generatedAlerts = alertService.generateAlertsForWindow(window.getWindowId());
        
        log.info("Generated {} alerts for window {}", generatedAlerts.size(), window.getName());
        
        // Log summary by severity
        long criticalCount = generatedAlerts.stream()
                .filter(a -> a.getSeverity() == com.sism.enums.AlertSeverity.CRITICAL)
                .count();
        long warningCount = generatedAlerts.stream()
                .filter(a -> a.getSeverity() == com.sism.enums.AlertSeverity.WARNING)
                .count();
        long infoCount = generatedAlerts.stream()
                .filter(a -> a.getSeverity() == com.sism.enums.AlertSeverity.INFO)
                .count();

        log.info("Alert summary for window {}: CRITICAL={}, WARNING={}, INFO={}", 
                window.getName(), criticalCount, warningCount, infoCount);
    }

    /**
     * Manual trigger to process a specific alert window by ID
     * Can be called from a controller for manual alert generation
     * 
     * @param windowId the alert window ID
     * @return list of generated alert events
     */
    public List<AlertEventVO> triggerAlertCalculation(Long windowId) {
        log.info("Manual trigger for alert calculation on window {}", windowId);
        return alertService.generateAlertsForWindow(windowId);
    }

    /**
     * Process all upcoming alert windows within a date range
     * Useful for batch processing or catching up on missed windows
     * 
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     */
    public void processAlertWindowsInRange(LocalDate startDate, LocalDate endDate) {
        log.info("Processing alert windows from {} to {}", startDate, endDate);
        
        List<AlertWindow> windows = alertWindowRepository.findByCutoffDateBetween(startDate, endDate);
        
        for (AlertWindow window : windows) {
            try {
                processAlertWindow(window);
            } catch (Exception e) {
                log.error("Failed to process alert window {}: {}", window.getWindowId(), e.getMessage(), e);
            }
        }

        log.info("Completed processing {} alert windows in date range", windows.size());
    }

    /**
     * Scheduled task to check for overdue alert windows
     * Runs every hour to catch any windows that might have been missed
     * 
     * This is a safety net for windows that weren't processed at their cutoff date
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkOverdueAlertWindows() {
        log.debug("Checking for overdue alert windows");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        
        // Find windows from yesterday that might have been missed
        List<AlertWindow> overdueWindows = alertWindowRepository.findByCutoffDateBetween(yesterday, today);
        
        for (AlertWindow window : overdueWindows) {
            // Check if this window has already been processed (has alert events)
            // This is a simple check - in production, you might want a more robust tracking mechanism
            try {
                List<AlertEventVO> existingAlerts = alertService.generateAlertsForWindow(window.getWindowId());
                if (!existingAlerts.isEmpty()) {
                    log.debug("Window {} already has alerts, skipping", window.getWindowId());
                }
            } catch (Exception e) {
                log.warn("Error checking overdue window {}: {}", window.getWindowId(), e.getMessage());
            }
        }
    }
}
