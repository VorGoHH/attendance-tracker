package attendancetracker.service;

import attendancetracker.model.AttendanceReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class TelegramService {

    @Value("${TELEGRAM_BOT_TOKEN:${telegram.bot.token:}}")
    private String botToken;

    @Value("${TELEGRAM_CHAT_ID:${telegram.bot.chat-id:}}")
    private String defaultChatId;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendAttendanceReport(AttendanceReport report) {
        sendAttendanceReport(defaultChatId, report);
    }

    public void sendAttendanceReport(String targetChatId, AttendanceReport report) {
        sendText(targetChatId, buildMessage(report));
    }

    private String buildMessage(AttendanceReport report) {
        StringBuilder sb = new StringBuilder();

        int totalPresent = report.getTotalStudents()
                - report.getOnDuty()
                - report.getSick()
                - report.getExcused()
                - report.getIndividual()
                - report.getIllegallyAbsent()
                - report.getBusinessTrip();

        sb.append(report.getGroupName()).append(" ").append(report.getReportDate()).append("\n");
        sb.append("З/с - ").append(report.getTotalStudents()).append("\n");
        sb.append("В/н - ").append(totalPresent).append("\n");

        appendLine(sb, "Зв", report.getExcused(), report.getExcusedList());
        appendLine(sb, "Відр", report.getBusinessTrip(), report.getBusinessTripList());
        appendLine(sb, "Хв", report.getSick(), report.getSickList());
        appendLine(sb, "І/з", report.getIndividual(), report.getIndividualList());
        appendLine(sb, "Н/в", report.getIllegallyAbsent(), report.getIllegallyAbsentList());
        appendLine(sb, "Наряд", report.getOnDuty(), report.getOnDutyList());

        return sb.toString().trim();
    }

    private void appendLine(StringBuilder sb, String label, int count, List<String> names) {
        if (count > 0) {
            sb.append(label).append(" - ").append(count)
                    .append(" (").append(String.join(", ", names)).append(")\n");
        }
    }

    public void sendText(String targetChatId, String text) {
        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
            String body = "chat_id=" + encode(targetChatId) + "&text=" + encode(text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Помилка відправки Telegram: {} {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Помилка відправки повідомлення: {}", e.getMessage());
        }
    }

    public void sendMenu(String targetChatId) {
        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
            String text = "Оберіть потрібний звіт:";
            String keyboardJson = "{\"keyboard\":[[{\"text\":\"📊 Звіт за сьогодні\"}],[{\"text\":\"📅 Звіт за іншу дату\"}]],\"resize_keyboard\":true}";

            String body = "chat_id=" + encode(targetChatId) + "&text=" + encode(text) + "&reply_markup=" + encode(keyboardJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("Помилка відправки меню: {}", e.getMessage());
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}