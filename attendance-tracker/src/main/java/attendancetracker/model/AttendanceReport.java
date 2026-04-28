package attendancetracker.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AttendanceReport {

    private String groupName;
    private String reportDate;
    private int totalStudents;
    private int onDuty;
    private int sick;
    private int excused;
    private int absent;
    private int illegallyAbsent;
    private int businessTrip;
    private List<String> onDutyList;
    private List<String> sickList;
    private List<String> excusedList;
    private List<String> absentList;
    private List<String> illegallyAbsentList;
    private List<String> businessTripList;
}