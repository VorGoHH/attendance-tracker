package attendancetracker.service;

import attendancetracker.model.AttendanceReport;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleSheetsService — логіка побудови звіту")
class GoogleSheetsServiceTest {

    @Mock
    private Sheets sheetsService;

    @Mock
    private Sheets.Spreadsheets spreadsheets;

    @Mock
    private Sheets.Spreadsheets.Values values;

    @Mock
    private Sheets.Spreadsheets.Values.Get get;

    @InjectMocks
    private GoogleSheetsService googleSheetsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleSheetsService, "spreadsheetId", "test-id");
        ReflectionTestUtils.setField(googleSheetsService, "groupName", "241 н.г.");
    }

    @Test
    @DisplayName("Повертає null якщо таблиця порожня")
    void emptySheet_returnsNull() throws IOException {
        mockSheetResponse(List.of());
        assertThat(googleSheetsService.getAttendanceForDate(LocalDate.of(2026, 4, 27))).isNull();
    }

    @Test
    @DisplayName("Повертає null якщо колонка дати не знайдена")
    void noDateColumn_returnsNull() throws IOException {
        List<List<Object>> rows = List.of(
                List.of("ПІБ курсанта", "01.01", "02.01")
        );
        mockSheetResponse(rows);
        assertThat(googleSheetsService.getAttendanceForDate(LocalDate.of(2026, 1, 3))).isNull();
    }

    @Test
    @DisplayName("Правильно рахує кількість студентів")
    void correctStudentCount() throws IOException {
        LocalDate date = LocalDate.of(2026, 4, 27);
        String dateStr = date.format(DateTimeFormatter.ofPattern("dd.MM"));

        List<List<Object>> rows = Arrays.asList(
                Arrays.asList("ПІБ курсанта", dateStr),
                Arrays.asList("Базелюк О.В.", ""),
                Arrays.asList("Богаченко П.І.", ""),
                Arrays.asList("Бондаренко А.А.", "")
        );
        mockSheetResponse(rows);

        AttendanceReport report = googleSheetsService.getAttendanceForDate(date);

        assertThat(report).isNotNull();
        assertThat(report.getTotalStudents()).isEqualTo(3);
    }

    @Test
    @DisplayName("Правильно розподіляє студентів по статусах")
    void correctStatusDistribution() throws IOException {
        LocalDate date = LocalDate.of(2026, 4, 27);
        String dateStr = date.format(DateTimeFormatter.ofPattern("dd.MM"));

        List<List<Object>> rows = Arrays.asList(
                Arrays.asList("ПІБ курсанта", dateStr),
                Arrays.asList("Базелюк О.В.", "Наряд"),
                Arrays.asList("Богаченко П.І.", "Хворий"),
                Arrays.asList("Бондаренко А.А.", "Звільнення"),
                Arrays.asList("Вашуленко Д.А.", "Індивідуальні заняття"),
                Arrays.asList("Вітвіцький О.В.", "Незаконно відсутній"),
                Arrays.asList("Кінах В.О.", "Відрядження"),
                Arrays.asList("Каніболоцький М.Є.", "") // Присутній
        );
        mockSheetResponse(rows);

        AttendanceReport report = googleSheetsService.getAttendanceForDate(date);

        assertThat(report).isNotNull();
        assertThat(report.getTotalStudents()).isEqualTo(7);
        assertThat(report.getOnDuty()).isEqualTo(1);
        assertThat(report.getSick()).isEqualTo(1);
        assertThat(report.getExcused()).isEqualTo(1);
        assertThat(report.getIndividual()).isEqualTo(1);
        assertThat(report.getIllegallyAbsent()).isEqualTo(1);
        assertThat(report.getBusinessTrip()).isEqualTo(1);

        assertThat(report.getOnDutyList()).containsExactly("Базелюк О.В.");
        assertThat(report.getSickList()).containsExactly("Богаченко П.І.");
        assertThat(report.getExcusedList()).containsExactly("Бондаренко А.А.");
        assertThat(report.getIndividualList()).containsExactly("Вашуленко Д.А.");
        assertThat(report.getIllegallyAbsentList()).containsExactly("Вітвіцький О.В.");
        assertThat(report.getBusinessTripList()).containsExactly("Кінах В.О.");
    }

    @Test
    @DisplayName("Пропускає порожні рядки та рядки без імені")
    void skipsEmptyRows() throws IOException {
        LocalDate date = LocalDate.of(2026, 4, 27);
        String dateStr = date.format(DateTimeFormatter.ofPattern("dd.MM"));

        List<List<Object>> rows = Arrays.asList(
                Arrays.asList("ПІБ курсанта", dateStr),
                Arrays.asList("Базелюк О.В.", ""),
                List.of(), // Повністю порожній
                Arrays.asList("", "Хворий"), // Без імені
                Arrays.asList("Бондаренко А.А.", "")
        );
        mockSheetResponse(rows);

        AttendanceReport report = googleSheetsService.getAttendanceForDate(date);

        assertThat(report).isNotNull();
        assertThat(report.getTotalStudents()).isEqualTo(2);
    }

    @Test
    @DisplayName("Правильно визначає назву вкладки (місяць) та передає правильний range в API")
    void correctSheetNameAndRangeRequested() throws IOException {
        LocalDate date = LocalDate.of(2026, 4, 27); // Квітень
        mockSheetResponse(List.of(
                Arrays.asList("ПІБ курсанта", "27.04"),
                Arrays.asList("Базелюк О.В.", "")
        ));

        googleSheetsService.getAttendanceForDate(date);

        verify(values).get("test-id", "Квітень!A:Z");
    }

    @Test
    @DisplayName("Правильно встановлює назву групи та повну дату у звіт")
    void correctGroupNameAndFullDate() throws IOException {
        LocalDate date = LocalDate.of(2026, 5, 15); // Травень

        List<List<Object>> rows = Arrays.asList(
                Arrays.asList("ПІБ курсанта", "15.05"),
                Arrays.asList("Базелюк О.В.", "")
        );
        mockSheetResponse(rows);

        AttendanceReport report = googleSheetsService.getAttendanceForDate(date);

        assertThat(report.getGroupName()).isEqualTo("241 н.г.");
        assertThat(report.getReportDate()).isEqualTo("15.05.2026");
        verify(values).get("test-id", "Травень!A:Z");
    }

    private void mockSheetResponse(List<List<Object>> rows) throws IOException {
        ValueRange valueRange = new ValueRange();
        valueRange.setValues(rows.isEmpty() ? null : rows);

        lenient().when(sheetsService.spreadsheets()).thenReturn(spreadsheets);
        lenient().when(spreadsheets.values()).thenReturn(values);
        lenient().when(values.get(anyString(), anyString())).thenReturn(get);
        lenient().when(get.execute()).thenReturn(valueRange);
    }
}