package fr.itineclair.outdoor;

import java.time.LocalDateTime;

public record OutdoorPlanCommand(
        LocalDateTime plannedStartLocal,
        int plannedDurationMinutes,
        String timeZone,
        boolean shareStartPointWithWeatherProvider) {
}
