package be.ehb.catalogusservice.dto;

import java.util.List;

public record ReviewStatistiek(String isbn, Double gemiddelde, Long aantal) {

    public static final ReviewStatistiek GEEN = new ReviewStatistiek(null, null, 0L);

    public static ReviewStatistiek van(String isbn, List<Integer> scores) {
        if (scores.isEmpty()) {
            return GEEN;
        }
        double gemiddelde = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        return new ReviewStatistiek(isbn, gemiddelde, (long) scores.size());
    }

    /** Gemiddelde afgerond op één cijfer na de komma, of null als er nog geen reviews zijn. */
    public Double afgerondGemiddelde() {
        return gemiddelde == null ? null : Math.round(gemiddelde * 10) / 10.0;
    }
}
