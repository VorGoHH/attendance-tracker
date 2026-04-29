package attendancetracker.model;

public enum AttendanceStatus {

    PRESENT(""),
    ON_DUTY("Н"),
    SICK("Хв"),
    EXCUSED("Зв"),
    ABSENT("В"),
    ILLEGALLYABSENT("Х"),
    BUSINESS_TRIP("Відр");

    private final String marker;

    AttendanceStatus(String marker) {
        this.marker = marker;
    }

    public String getMarker() {
        return marker;
    }

    public static AttendanceStatus fromMarker(String value) {
        if (value == null || value.isBlank()) {
            return PRESENT;
        }

        String trimmed = value.trim();

        return switch (trimmed) {
            case "Наряд" -> ON_DUTY;
            case "Хворий" -> SICK;
            case "Звільнення" -> EXCUSED;
            case "Відсутній" -> ABSENT;
            case "Незаконно відсутній" -> ILLEGALLYABSENT;
            case "Відрядження" -> BUSINESS_TRIP;
            default -> PRESENT;
        };
    }
}