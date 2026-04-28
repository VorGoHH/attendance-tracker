package attendancetracker.service;

import attendancetracker.model.AttendanceReport;
import attendancetracker.model.AttendanceStatus;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSheetsService {

    private final Sheets sheetsService;

    @Value("${google.spreadsheet.id}")
    private String spreadsheetId;

    @Value("${group.name}")
    private String groupName;

    private static final DateTimeFormatter COLUMN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public AttendanceReport getAttendanceForToday() throws IOException {
        LocalDate today = LocalDate.now();
        String sheetName = getSheetNameForMonth(today);
        String todayColumn = today.format(COLUMN_DATE_FORMAT);

        log.info("Читання даних з вкладки '{}', колонка дати '{}'", sheetName, todayColumn);

        String range = sheetName + "!A:Z";
        ValueRange response;
        try {
            response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
        } catch (Exception e) {
            log.error("Помилка при зчитуванні таблиці: {}", e.getMessage());
            throw new IOException("Не вдалося зчитати дані з Google Sheets: " + e.getMessage(), e);
        }

        List<List<Object>> rows = response.getValues();
        if (rows == null || rows.isEmpty()) {
            log.warn("Таблиця порожня або вкладка '{}' не знайдена", sheetName);
            return null;
        }

        List<Object> headers = rows.get(0);
        int dateColumnIndex = findColumnIndex(headers, todayColumn);

        if (dateColumnIndex == -1) {
            log.warn("Колонка для дати '{}' не знайдена у таблиці", todayColumn);
            return null;
        }

        return buildReport(rows, dateColumnIndex, today);
    }

    private AttendanceReport buildReport(List<List<Object>> rows, int dateColIndex, LocalDate date) {
        List<String> onDutyList = new ArrayList<>();
        List<String> sickList = new ArrayList<>();
        List<String> excusedList = new ArrayList<>();
        List<String> absentList = new ArrayList<>();
        List<String> illegallyAbsentList = new ArrayList<>();
        List<String> businessTripList = new ArrayList<>();
        int totalStudents = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row.isEmpty() || row.get(0) == null || row.get(0).toString().isBlank()) {
                continue;
            }

            String studentName = row.get(0).toString().trim();
            totalStudents++;

            String cellValue = "";
            if (dateColIndex < row.size()) {
                Object cell = row.get(dateColIndex);
                cellValue = cell != null ? cell.toString().trim() : "";
            }

            AttendanceStatus status = AttendanceStatus.fromMarker(cellValue);

            switch (status) {
                case ON_DUTY -> onDutyList.add(studentName);
                case SICK -> sickList.add(studentName);
                case EXCUSED -> excusedList.add(studentName);
                case ABSENT -> absentList.add(studentName);
                case ILLEGALLYABSENT -> illegallyAbsentList.add(studentName);
                case BUSINESS_TRIP -> businessTripList.add(studentName);
                case PRESENT -> {}
            }
        }

        log.info("Оброблено {} студентів. Наряд: {}, Хворі: {}, Звільнені: {}, Відсутні: {}, Незаконно: {}, Відрядження: {}",
                totalStudents, onDutyList.size(), sickList.size(), excusedList.size(),
                absentList.size(), illegallyAbsentList.size(), businessTripList.size());

        return AttendanceReport.builder()
                .groupName(groupName)
                .reportDate(date.format(REPORT_DATE_FORMAT))
                .totalStudents(totalStudents)
                .onDuty(onDutyList.size())
                .sick(sickList.size())
                .excused(excusedList.size())
                .absent(absentList.size())
                .illegallyAbsent(illegallyAbsentList.size())
                .businessTrip(businessTripList.size())
                .onDutyList(onDutyList)
                .sickList(sickList)
                .excusedList(excusedList)
                .absentList(absentList)
                .illegallyAbsentList(illegallyAbsentList)
                .businessTripList(businessTripList)
                .build();
    }

    private int findColumnIndex(List<Object> headers, String columnName) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i) != null && headers.get(i).toString().trim().equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private String getSheetNameForMonth(LocalDate date) {
        return switch (date.getMonthValue()) {
            case 1 -> "Січень";
            case 2 -> "Лютий";
            case 3 -> "Березень";
            case 4 -> "Квітень";
            case 5 -> "Травень";
            case 6 -> "Червень";
            case 7 -> "Липень";
            case 8 -> "Серпень";
            case 9 -> "Вересень";
            case 10 -> "Жовтень";
            case 11 -> "Листопад";
            case 12 -> "Грудень";
            default -> throw new IllegalStateException("Невідомий місяць: " + date.getMonthValue());
        };
    }
}