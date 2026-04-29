package attendancetracker.service;

import attendancetracker.model.AttendanceReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService — формування та відправка листа")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "sender@gmail.com");
        ReflectionTestUtils.setField(emailService, "teacherEmail", "teacher@gmail.com");
    }

    @Test
    @DisplayName("Тема листа містить дату та назву групи")
    void subjectContainsDateAndGroup() {
        AttendanceReport report = buildReport();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendAttendanceReport(report);

        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject())
                .contains("28.04.2026")
                .contains("241 н.г.");
    }

    @Test
    @DisplayName("Лист відправляється на правильний email")
    void sentToCorrectEmail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendAttendanceReport(buildReport());

        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).contains("teacher@gmail.com");
        assertThat(captor.getValue().getFrom()).isEqualTo("sender@gmail.com");
    }

    @Test
    @DisplayName("Тіло листа містить статистику")
    void bodyContainsStatistics() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendAttendanceReport(buildReport());

        verify(mailSender).send(captor.capture());
        String body = captor.getValue().getText();

        assertThat(body).contains("За списком – 5");
        assertThat(body).contains("Наряд – 1");
        assertThat(body).contains("Хворі – 1");
        assertThat(body).contains("Звільнення – 1");
        assertThat(body).contains("Відсутні – 1");
        assertThat(body).contains("Незаконно відсутні – 1");
    }

    @Test
    @DisplayName("Тіло листа містить імена студентів")
    void bodyContainsStudentNames() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendAttendanceReport(buildReport());

        verify(mailSender).send(captor.capture());
        String body = captor.getValue().getText();

        assertThat(body).contains("Базелюк О.В.");
        assertThat(body).contains("Богаченко П.І.");
        assertThat(body).contains("Бондаренко А.А.");
    }

    @Test
    @DisplayName("Порожні списки не виводяться")
    void emptyListsNotShown() {
        AttendanceReport report = AttendanceReport.builder()
                .groupName("241 н.г.")
                .reportDate("28.04.2026")
                .totalStudents(1)
                .onDuty(0).sick(0).excused(0).absent(0).illegallyAbsent(0).businessTrip(0)
                .onDutyList(List.of()).sickList(List.of()).excusedList(List.of())
                .absentList(List.of()).illegallyAbsentList(List.of()).businessTripList(List.of())
                .build();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        emailService.sendAttendanceReport(report);

        verify(mailSender).send(captor.capture());
        String body = captor.getValue().getText();


        assertThat(body).doesNotContain("Наряд –\n");
    }


    private AttendanceReport buildReport() {
        return AttendanceReport.builder()
                .groupName("241 н.г.")
                .reportDate("28.04.2026")
                .totalStudents(5)
                .onDuty(1).sick(1).excused(1).absent(1).illegallyAbsent(1).businessTrip(0)
                .onDutyList(List.of("Базелюк О.В."))
                .sickList(List.of("Богаченко П.І."))
                .excusedList(List.of("Бондаренко А.А."))
                .absentList(List.of("Кравець О.М."))
                .illegallyAbsentList(List.of("Гнатюк Т.С."))
                .businessTripList(List.of())
                .build();
    }
}