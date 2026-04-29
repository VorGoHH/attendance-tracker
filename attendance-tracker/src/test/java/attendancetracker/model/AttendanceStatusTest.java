package attendancetracker.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AttendanceStatus — розпізнавання маркерів")
class AttendanceStatusTest {

    @Test
    @DisplayName("Порожній рядок → PRESENT")
    void emptyString_returnsPresent() {
        assertThat(AttendanceStatus.fromMarker("")).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("Null → PRESENT")
    void null_returnsPresent() {
        assertThat(AttendanceStatus.fromMarker(null)).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("Пробіли → PRESENT")
    void whitespace_returnsPresent() {
        assertThat(AttendanceStatus.fromMarker("   ")).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("Наряд → ON_DUTY")
    void duty_returnsOnDuty() {
        assertThat(AttendanceStatus.fromMarker("Наряд")).isEqualTo(AttendanceStatus.ON_DUTY);
    }

    @Test
    @DisplayName("Хворий → SICK")
    void sick_returnsSick() {
        assertThat(AttendanceStatus.fromMarker("Хворий")).isEqualTo(AttendanceStatus.SICK);
    }

    @Test
    @DisplayName("Звільнення → EXCUSED")
    void excused_returnsExcused() {
        assertThat(AttendanceStatus.fromMarker("Звільнення")).isEqualTo(AttendanceStatus.EXCUSED);
    }

    @Test
    @DisplayName("Відсутній → ABSENT")
    void absent_returnsAbsent() {
        assertThat(AttendanceStatus.fromMarker("Відсутній")).isEqualTo(AttendanceStatus.ABSENT);
    }

    @Test
    @DisplayName("Незаконно відсутній → ILLEGALLYABSENT")
    void illegallyAbsent_returnsIllegallyAbsent() {
        assertThat(AttendanceStatus.fromMarker("Незаконно відсутній"))
                .isEqualTo(AttendanceStatus.ILLEGALLYABSENT);
    }

    @Test
    @DisplayName("Відрядження → BUSINESS_TRIP")
    void businessTrip_returnsBusinessTrip() {
        assertThat(AttendanceStatus.fromMarker("Відрядження")).isEqualTo(AttendanceStatus.BUSINESS_TRIP);
    }

    @Test
    @DisplayName("Невідомий маркер → PRESENT")
    void unknownMarker_returnsPresent() {
        assertThat(AttendanceStatus.fromMarker("???")).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("Маркер з пробілами → правильний статус")
    void markerWithSpaces_trimmedCorrectly() {
        assertThat(AttendanceStatus.fromMarker("  Наряд  ")).isEqualTo(AttendanceStatus.ON_DUTY);
        assertThat(AttendanceStatus.fromMarker("  Хворий  ")).isEqualTo(AttendanceStatus.SICK);
    }
}