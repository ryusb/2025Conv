package util;

import persistence.dto.RestaurantDTO;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class TimeSlotUtil {
    /**
     * 식당의 현재 시간 기준으로 영업 중인 시간대 이름 반환
     * @param restaurant DTO
     * @return "아침", "점심", "저녁" 중 하나
     */
    public static String getCurrentOrNearestMealTime(RestaurantDTO restaurant) {
        LocalTime now = LocalTime.now();

        // 각 구간 이름과 시간 범위를 맵으로 정의
        Map<String, LocalTime[]> slots = new HashMap<>();
        if (restaurant.getOpenTime1() != null && restaurant.getCloseTime1() != null) {
            slots.put("아침", new LocalTime[]{restaurant.getOpenTime1(), restaurant.getCloseTime1()});
        }
        if (restaurant.getOpenTime2() != null && restaurant.getCloseTime2() != null) {
            slots.put("점심", new LocalTime[]{restaurant.getOpenTime2(), restaurant.getCloseTime2()});
        }

        if (slots.containsKey("점심") && slots.get("점심")[0].isAfter(LocalTime.of(15,0))) {
            slots.put("저녁", slots.remove("점심"));
        }

        for (Map.Entry<String, LocalTime[]> entry : slots.entrySet()) {
            LocalTime start = entry.getValue()[0];
            LocalTime end = entry.getValue()[1];
            if (!now.isBefore(start) && !now.isAfter(end)) {
                return entry.getKey();
            }
        }

        String nearest = null;
        long minMinutes = Long.MAX_VALUE;
        for (Map.Entry<String, LocalTime[]> entry : slots.entrySet()) {
            LocalTime start = entry.getValue()[0];
            long diff = Math.abs(java.time.Duration.between(now, start).toMinutes());
            if (diff < minMinutes) {
                minMinutes = diff;
                nearest = entry.getKey();
            }
        }

        return nearest;
    }
}
