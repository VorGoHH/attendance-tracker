package attendancetracker.service;

import attendancetracker.model.AttendanceReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${teacher.email}")
    private String teacherEmail;

    public void sendAttendanceReport(AttendanceReport report) {
        String subject = String.format("Звіт за %s %s", report.getReportDate(), report.getGroupName());
        String body = buildEmailBody(report);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(teacherEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Звіт успішно відправлено на {}", teacherEmail);
        } catch (Exception e) {
            log.error("Помилка відправки звіту: {}", e.getMessage(), e);
            throw new RuntimeException("Не вдалося відправити звіт: " + e.getMessage(), e);
        }
    }

    private String buildEmailBody(AttendanceReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Звіт за %s %s%n%n", report.getReportDate(), report.getGroupName()));
        sb.append(String.format("За списком – %d%n", report.getTotalStudents()));
        sb.append(String.format("Наряд – %d%n", report.getOnDuty()));
        sb.append(String.format("Хворі – %d%n", report.getSick()));
        sb.append(String.format("Звільнення – %d%n", report.getExcused()));
        sb.append(String.format("Відрядження – %d%n", report.getBusinessTrip()));
        sb.append(String.format("Відсутні – %d%n", report.getAbsent()));
        sb.append(String.format("Незаконно відсутні – %d%n", report.getIllegallyAbsent()));
        sb.append("\nСписок відсутніх:\n");

        appendList(sb, "Наряд", report.getOnDutyList());
        appendList(sb, "Хворі", report.getSickList());
        appendList(sb, "Звільнення", report.getExcusedList());
        appendList(sb, "Відрядження", report.getBusinessTripList());
        appendList(sb, "Відсутні", report.getAbsentList());
        appendList(sb, "Незаконно відсутні", report.getIllegallyAbsentList());

        return sb.toString();
    }

    private void appendList(StringBuilder sb, String label, List<String> students) {
        if (students != null && !students.isEmpty()) {
            sb.append(String.format("%s – %s%n", label, String.join(", ", students)));
        }
    }
}