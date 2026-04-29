package attendancetracker.service;

import attendancetracker.model.AttendanceReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotListener {

    private final GoogleSheetsService googleSheetsService;
    private final TelegramService telegramService;

    @Value("${TELEGRAM_BOT_TOKEN:${telegram.bot.token:}}")
    private String botToken;

    @Value("${TELEGRAM_CHAT_ID:${telegram.bot.chat-id:}}")
    private String allowedChatId;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private long lastUpdateId = 0;

    @Scheduled(fixedDelay = 5000)
    public void pollUpdates() {
        try {
            String url = String.format(
                    "https://api.telegram.org/bot%s/getUpdates?offset=%d&timeout=0",
                    botToken, lastUpdateId + 1
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.get("result");
            if (results == null || !results.isArray()) return;

            for (JsonNode update : results) {
                long updateId = update.get("update_id").asLong();
                lastUpdateId = updateId;

                JsonNode message = update.get("message");
                if (message == null) continue;

                String chatId = message.get("chat").get("id").asText();
                if (!chatId.equals(allowedChatId)) {
                    log.warn("Повідомлення від невідомого chat_id: {}", chatId);
                    continue;
                }

                JsonNode textNode = message.get("text");
                if (textNode == null) continue;

                String text = textNode.asText().trim();
                String textLower = text.toLowerCase();

                if (textLower.equals("/start") || textLower.equals("/help")) {
                    handleHelpCommand();
                } else if (textLower.startsWith("/звіт") || textLower.startsWith("/report") || textLower.startsWith("/z")) {
                    handleReportCommand(text);
                }
            }

        } catch (Exception e) {
            log.error("Помилка при отриманні оновлень Telegram: {}", e.getMessage());
        }
    }


    private void handleReportCommand(String text) {
        LocalDate date = parseDate(text);

        if (date == null) {
            telegramService.sendText(
                    "Невірний формат дати.\n" +
                            "Використовуй:\n" +
                            "/звіт — за сьогодні\n" +
                            "/звіт 27.04 — за конкретну дату\n" +
                            "/звіт 27.04.2026 — за дату з роком"
            );
            return;
        }

        log.info("Запит звіту за дату: {}", date);

        try {
            AttendanceReport report = googleSheetsService.getAttendanceForDate(date);

            if (report == null) {
                telegramService.sendText(
                        "Дані за " + date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) +
                                " не знайдено.\nПеревір що колонка з датою існує у таблиці."
                );
                return;
            }

            telegramService.sendAttendanceReport(report);
            log.info("Звіт за {} успішно відправлено", date);

        } catch (Exception e) {
            log.error("Помилка при формуванні звіту: {}", e.getMessage(), e);
            telegramService.sendText("Помилка при читанні таблиці: " + e.getMessage());
        }
    }


    private LocalDate parseDate(String text) {
        String datePart = text.trim()
                .replaceFirst("(?i)/звіт", "")
                .replaceFirst("(?i)/report", "")
                .replaceFirst("(?i)/z", "")
                .trim();

        // Якщо дата не вказана — сьогодні
        if (datePart.isEmpty()) {
            return LocalDate.now();
        }

        // dd.MM.yyyy
        try {
            return LocalDate.parse(datePart, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException ignored) {}

        // dd.MM (поточний рік)
        try {
            String withYear = datePart + "." + LocalDate.now().getYear();
            return LocalDate.parse(withYear, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException ignored) {}

        return null;
    }

    private void handleHelpCommand() {
        String helpText =
                "Доступні команди:\n\n" +
                        "/звіт — звіт за сьогодні\n" +
                        "/звіт 27.04 — звіт за конкретну дату\n" +
                        "/звіт 27.04.2026 — звіт за дату з роком\n";
        telegramService.sendText(helpText);
    }
}