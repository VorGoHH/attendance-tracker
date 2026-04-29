package attendancetracker.model;

public enum AttendanceStatus {

    PRESENT(""),
    ON_DUTY("Наряд"),
    SICK("Хворий"),
    EXCUSED("Звільнення"),
    INDIVIDUAL("Індивідуальні заняття"),
    ILLEGALLYABSENT("Незаконно відсутній"),
    BUSINESS_TRIP("Відрядження");

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
            case "Індивідуальні заняття" -> INDIVIDUAL;
            case "Незаконно відсутній" -> ILLEGALLYABSENT;
            case "Відрядження" -> BUSINESS_TRIP;
            default -> PRESENT;
        };
    }
}