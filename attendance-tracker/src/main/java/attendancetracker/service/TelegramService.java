package attendancetracker.service;

import attendancetracker.model.AttendanceReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
@Service
public class TelegramService {

    @Value("${TELEGRAM_BOT_TOKEN:${telegram.bot.token:}}")
    private String botToken;

    @Value("${TELEGRAM_CHAT_ID:${telegram.bot.chat-id:}}")
    private String chatId;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendAttendanceReport(AttendanceReport report) {
        sendText(buildMessage(report));
    }

    private String buildMessage(AttendanceReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%s %s%n", report.getGroupName(), report.getReportDate()));

        int totalPresent = report.getTotalStudents()
                - report.getOnDuty()
                - report.getSick()
                - report.getExcused()
                - report.getAbsent()
                - report.getIllegallyAbsent()
                - report.getBusinessTrip();

        sb.append(String.format("З/с - %d%n", report.getTotalStudents()));
        sb.append(String.format("В/н - %d%n", totalPresent));
        sb.append(formatLine("Зв", report.getExcused(), report.getExcusedList()));
        sb.append(formatLine("Відр", report.getBusinessTrip(), report.getBusinessTripList()));
        sb.append(formatLine("Хв", report.getSick(), report.getSickList()));
        sb.append(formatLine("Н/в", report.getIllegallyAbsent(), report.getIllegallyAbsentList()));
        sb.append(formatLine("Наряд", report.getOnDuty(), report.getOnDutyList()));

        return sb.toString().trim();
    }

    private String formatLine(String label, int count, List<String> names) {
        if (count == 0) {
            return String.format("%s -%n", label);
        }
        return String.format("%s - %d (%s)%n", label, count, String.join(" ", names));
    }

    public void sendText(String text) {
        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

            String escapedText = text
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");

            String body = String.format(
                    "{\"chat_id\":\"%s\",\"text\":\"%s\"}",
                    chatId, escapedText
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Telegram повідомлення успішно відправлено");
            } else {
                log.error("Помилка відправки Telegram: {} {}", response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("Помилка відправки Telegram повідомлення: {}", e.getMessage(), e);
        }
    }
}