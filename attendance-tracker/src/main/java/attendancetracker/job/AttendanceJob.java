package attendancetracker.job;

import attendancetracker.model.AttendanceReport;
import attendancetracker.service.EmailService;
import attendancetracker.service.GoogleSheetsService;
import attendancetracker.service.TelegramService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceJob {

    private final GoogleSheetsService googleSheetsService;
    private final EmailService emailService;
    private final TelegramService telegramService;


    @PostConstruct
    public void runOnStartup() {
        log.info("===== Запуск при старті =====");
        generateAndSendAttendanceReport();
    }


    public void generateAndSendAttendanceReport() {
        log.info("===== Запуск задачі відвідуваності =====");
        try {
            AttendanceReport report = googleSheetsService.getAttendanceForToday();

            if (report == null) {
                log.warn("Дані за сьогодні не знайдено. Звіт не відправлено.");
                return;
            }

            log.info("Дані зчитано. Студентів: {}", report.getTotalStudents());

            emailService.sendAttendanceReport(report);
            telegramService.sendAttendanceReport(report);

            log.info("===== Задача виконана успішно! =====");
        } catch (Exception e) {
            log.error("===== ПОМИЛКА: {} =====", e.getMessage(), e);
        }
    }
}