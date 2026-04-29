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

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return;

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.get("result");
            if (results == null || !results.isArray()) return;

            for (JsonNode update : results) {
                lastUpdateId = update.get("update_id").asLong();

                JsonNode message = update.get("message");
                if (message == null) continue;

                String currentChatId = message.get("chat").get("id").asText();

                JsonNode textNode = message.get("text");
                if (textNode == null) continue;

                String text = textNode.asText().trim();
                String textLower = text.toLowerCase();

                if (textLower.equals("/start") || textLower.equals("/help")) {
                    telegramService.sendMenu(currentChatId);
                    handleHelpCommand(currentChatId);
                } else if (text.equals("📊 Звіт за сьогодні")) {
                    handleReportCommand("", currentChatId);
                } else if (text.equals("📅 Звіт за іншу дату")) {
                    telegramService.sendText(currentChatId, "Надішліть дату у форматі дд.мм (наприклад: 27.04)");
                } else if (textLower.startsWith("/звіт") || textLower.startsWith("/report") || textLower.startsWith("/z")) {
                    handleReportCommand(text, currentChatId);
                } else {
                    // Обробка простої дати без команд
                    LocalDate parsedDate = parseDate(text);
                    if (parsedDate != null && !text.isEmpty()) {
                        handleReportCommand("/звіт " + text, currentChatId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Помилка оновлень Telegram: {}", e.getMessage());
        }
    }

    private void handleReportCommand(String text, String chatId) {
        LocalDate date = parseDate(text);

        if (date == null) {
            telegramService.sendText(chatId, "Невірний формат дати. Напишіть, наприклад, 27.04");
            return;
        }

        try {
            AttendanceReport report = googleSheetsService.getAttendanceForDate(date);

            if (report == null) {
                telegramService.sendText(chatId, "Дані за " + date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " не знайдено.");
                return;
            }

            telegramService.sendAttendanceReport(chatId, report);
        } catch (Exception e) {
            log.error("Помилка звіту: {}", e.getMessage());
            telegramService.sendText(chatId, "Помилка при читанні таблиці.");
        }
    }

    private LocalDate parseDate(String text) {
        String datePart = text.trim()
                .replaceFirst("(?i)/звіт", "")
                .replaceFirst("(?i)/report", "")
                .replaceFirst("(?i)/z", "")
                .trim();

        if (datePart.isEmpty()) return LocalDate.now();

        try {
            return LocalDate.parse(datePart, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException ignored) {}

        try {
            String withYear = datePart + "." + LocalDate.now().getYear();
            return LocalDate.parse(withYear, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException ignored) {}

        return null;
    }

    private void handleHelpCommand(String chatId) {
        String helpText = "Використовуйте кнопки або надішліть дату текстом (наприклад 25.04).";
        telegramService.sendText(chatId, helpText);
    }
}