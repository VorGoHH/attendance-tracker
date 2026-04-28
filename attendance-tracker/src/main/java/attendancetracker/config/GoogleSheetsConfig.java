package attendancetracker.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleSheetsConfig {

    private static final String APPLICATION_NAME = "Attendance Tracker";

    @Bean
    public Sheets sheetsService() throws IOException, GeneralSecurityException {
        InputStream credentialsStream = getCredentialsStream();

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(credentialsStream)
                .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private InputStream getCredentialsStream() throws IOException {
        // Продакшн (Railway) — читаємо з змінної середовища
        String credentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            return new ByteArrayInputStream(credentialsJson.getBytes());
        }

        // Локально — читаємо з файлу в resources
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("google-credentials.json");

        if (stream == null) {
            throw new IOException(
                    "Не знайдено google-credentials.json. " +
                            "Локально: поклади файл у src/main/resources/. " +
                            "Продакшн: додай змінну GOOGLE_CREDENTIALS_JSON."
            );
        }

        return stream;
    }
}