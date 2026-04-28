package attendancetracker.job;

import attendancetracker.model.AttendanceReport;
import attendancetracker.service.EmailService;
import attendancetracker.service.GoogleSheetsService;
import attendancetracker.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceJob {

    private final GoogleSheetsService googleSheetsService;
    private final EmailService emailService;
    private final TelegramService telegramService;

    @Scheduled(cron = "0 0 18 * * MON-FRI")

    public void generateAndSendAttendanceReport() {
        log.info("===== Запуск задачі відвідуваності =====");
        try {
            AttendanceReport report = googleSheetsService.getAttendanceForToday();

            if (report == null) {
                log.warn("Дані за сьогодні не знайдено. Звіт не відправлено.");
                return;
            }

            log.info("Дані зчитано. Студентів: {}, Відсутніх: {}",
                    report.getTotalStudents(), report.getAbsent());


            emailService.sendAttendanceReport(report);

            telegramService.sendAttendanceReport(report);

            log.info("===== Задача виконана успішно =====");
        } catch (Exception e) {
            log.error("===== ПОМИЛКА: {} =====", e.getMessage(), e);
        }
    }
}