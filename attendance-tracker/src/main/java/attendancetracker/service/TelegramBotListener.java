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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotListener {

    private final GoogleSheetsService googleSheetsService;
    private final TelegramService telegramService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String allowedChatId;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Зберігаємо offset щоб не обробляти одне повідомлення двічі
    private long lastUpdateId = 0;

    /**
     * Кожні 5 секунд перевіряє нові повідомлення боту.
     * Якщо прийшла команда /звіт або /report — формує і відправляє звіт.
     */
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

                // Перевіряємо що повідомлення від дозволеного chat_id
                String chatId = message.get("chat").get("id").asText();
                if (!chatId.equals(allowedChatId)) {
                    log.warn("Повідомлення від невідомого chat_id: {}", chatId);
                    continue;
                }

                JsonNode textNode = message.get("text");
                if (textNode == null) continue;

                String text = textNode.asText().trim().toLowerCase();

                if (text.equals("/звіт") || text.equals("/звіт@" + getBotUsername())
                        || text.equals("/report") || text.equals("/z")) {
                    handleReportCommand(chatId);
                } else if (text.equals("/start") || text.equals("/help")) {
                    handleHelpCommand();
                }
            }

        } catch (Exception e) {
            log.error("Помилка при отриманні оновлень Telegram: {}", e.getMessage());
        }
    }

    private void handleReportCommand(String chatId) {
        log.info("Отримана команда /звіт від chat_id: {}", chatId);
        try {
            AttendanceReport report = googleSheetsService.getAttendanceForToday();

            if (report == null) {
                telegramService.sendText("❌ Дані за сьогодні не знайдено.\nПеревір що вкладка називається правильно і колонка з датою існує.");
                return;
            }

            telegramService.sendAttendanceReport(report);
            log.info("Звіт за запитом успішно відправлено");

        } catch (Exception e) {
            log.error("Помилка при формуванні звіту за запитом: {}", e.getMessage(), e);
            telegramService.sendText("❌ Помилка при читанні таблиці: " + e.getMessage());
        }
    }

    private void handleHelpCommand() {
        String helpText = """
                👋 Привіт! Доступні команди:
                
                /звіт — отримати звіт відвідуваності за сьогодні
                /report — те саме (англійська)
                /z — скорочена команда
                
                Автоматичний звіт надходить щодня о 18:00 (Пн-Пт)
                """;
        telegramService.sendText(helpText);
    }

    private String getBotUsername() {
        // Повертає порожній рядок якщо не вдалось отримати username
        // Це потрібно для обробки команд вигляду /звіт@bot_username
        return "";
    }
}