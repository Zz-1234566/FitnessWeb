package com.zz.usercenter.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天气服务
 * 主源：OpenWeatherMap（主流，免费 1000 次/天）
 * 降级：wttr.in（免注册，无需 API Key）
 */
@Component
public class WeatherHelper {

    private static final Logger log = LoggerFactory.getLogger(WeatherHelper.class);

    @Value("${weather.owm-api-key:}")
    private String owmApiKey;

    /** IP → 城市缓存，避免重复查询 */
    private final Map<String, String> ipCityCache = new ConcurrentHashMap<>();
    /** IP → 英文城市缓存 */
    private final Map<String, String> ipCityEnCache = new ConcurrentHashMap<>();

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(128 * 1024))
            .build();

    @Resource
    private ObjectMapper objectMapper;

    // ==================== 数据结构 ====================

    public record DailyWeather(
            String date, String dateLabel,
            int tempMax, int tempMin,
            String textDay, String textNight,
            int windSpeed, int humidity,
            int pop // 降水概率 0-100
    ) {}

    // ==================== 公开方法 ====================

    /**
     * 根据 IP 获取用户城市（中文，带缓存）
     */
    public String getCityByIp(String ip) {
        if (ip == null || ip.isBlank() || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "北京";
        }
        return ipCityCache.computeIfAbsent(ip, this::fetchCityByIp);
    }

    /**
     * 根据 IP 获取用户城市英文名（带缓存）
     */
    public String getCityEnByIp(String ip) {
        if (ip == null || ip.isBlank() || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "Beijing";
        }
        return ipCityEnCache.computeIfAbsent(ip, this::fetchCityEnByIp);
    }

    private String fetchCityByIp(String ip) {
        try {
            String body = webClient.get()
                    .uri("http://ip-api.com/json/{ip}?lang=zh-CN&fields=city,status", ip)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(3))
                    .block();
            if (body == null) return "北京";
            JsonNode root = objectMapper.readTree(body);
            if (!"success".equals(root.path("status").asText())) return "北京";
            String city = root.path("city").asText("");
            return (city != null && !city.isBlank()) ? city : "北京";
        } catch (Exception e) {
            log.warn("[Weather] IP定位失败 ip={}, error={}", ip, e.getMessage());
            return "北京";
        }
    }

    private String fetchCityEnByIp(String ip) {
        try {
            String body = webClient.get()
                    .uri("http://ip-api.com/json/{ip}?lang=en&fields=city,status", ip)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(3))
                    .block();
            if (body == null) return "Beijing";
            JsonNode root = objectMapper.readTree(body);
            if (!"success".equals(root.path("status").asText())) return "Beijing";
            String city = root.path("city").asText("");
            return (city != null && !city.isBlank()) ? city : "Beijing";
        } catch (Exception e) {
            log.warn("[Weather] IP定位(英文)失败 ip={}, error={}", ip, e.getMessage());
            return "Beijing";
        }
    }

    /**
     * 根据 IP 获取天气上下文
     */
    public String buildWeatherContextByIp(String ip) {
        String city = getCityByIp(ip);
        String cityEn = getCityEnByIp(ip);
        return buildWeatherContext(city, cityEn);
    }

    /**
     * 获取用户所在城市的 3 天天气预报（英文城市名查API）
     */
    public List<DailyWeather> getDailyForecast(String cityEn) {
        if (cityEn == null || cityEn.isBlank()) cityEn = "Beijing";

        // 优先 OpenWeatherMap
        if (owmApiKey != null && !owmApiKey.isBlank()) {
            List<DailyWeather> result = getOwmForecast(cityEn);
            if (!result.isEmpty()) return result;
        }

        // 降级 wttr.in
        return getWttrForecast(cityEn);
    }

    /**
     * 构建 AI prompt 天气上下文（中文展示，英文查API）
     * 注意：天气评估标签由后端硬阈值判定，AI不可自行解读温度/湿度等原始数据。
     */
    public String buildWeatherContext(String cityZh, String cityEn) {
        List<DailyWeather> forecast = getDailyForecast(cityEn);
        if (forecast.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("【").append(cityZh != null ? cityZh : cityEn).append(" · 天气预报】\n");

        // 第一段：原始数据
        for (DailyWeather w : forecast) {
            sb.append(String.format("  %s：%s，%d°C / %d°C，湿度%d%%",
                    w.dateLabel, w.textDay, w.tempMax, w.tempMin, w.humidity));
            if (w.pop > 30) sb.append("，降水概率").append(w.pop).append("%");
            if (w.windSpeed > 30) sb.append("，大风");
            sb.append("\n");
        }

        // 第二段：天气评估标签（硬阈值判定，AI不可自行解读）
        sb.append("  天气评估：");
        DailyWeather today = forecast.get(0);
        List<String> labels = new ArrayList<>();

        if (today.tempMax >= 35) {
            labels.add("酷热");
        } else if (today.tempMax >= 30) {
            labels.add("炎热");
        } else if (today.tempMax >= 25) {
            labels.add("温暖");
        } else if (today.tempMax <= 10) {
            labels.add("寒冷");
        }

        if (today.humidity >= 80 && today.tempMax >= 26) {
            labels.add("湿热");
        } else if (today.humidity >= 70 && today.tempMax >= 28) {
            labels.add("闷热");
        } else if (today.humidity >= 85) {
            labels.add("潮湿");
        }

        if (today.pop >= 80) {
            labels.add("有雨");
        } else if (today.pop >= 50) {
            labels.add("可能有雨");
        }

        if (today.windSpeed > 40) {
            labels.add("大风");
        }

        if (labels.isEmpty()) {
            labels.add("舒适");
        }
        sb.append(String.join("、", labels)).append("\n");

        // 第三段：运动建议（硬阈值，独立于天气描述文本）
        sb.append("  运动建议：");
        if (today.tempMax >= 35 || (today.tempMax >= 30 && today.humidity >= 75)) {
            sb.append("高温，建议室内训练，注意补水和防暑");
        } else if (today.tempMax >= 30) {
            sb.append("天气热，建议室内训练或清晨傍晚运动，注意补水");
        } else if (today.tempMax >= 25 && today.humidity >= 80) {
            sb.append("潮湿闷热，体感温度偏高，建议室内训练或清晨傍晚运动");
        } else if (today.pop >= 80) {
            sb.append("有雨，建议室内训练");
        } else if (today.pop >= 50) {
            sb.append("可能有雨，建议室内训练或备好雨具");
        } else if (today.tempMax <= 0) {
            sb.append("寒冷，建议室内训练，注意保暖");
        } else if (today.windSpeed > 40) {
            sb.append("大风，建议室内训练");
        } else if (today.tempMax >= 20 && today.tempMax < 30 && today.humidity < 70) {
            sb.append("天气舒适，适合户外运动");
        } else {
            sb.append("适合运动");
        }
        return sb.toString();
    }

    // ==================== OpenWeatherMap ====================

    private List<DailyWeather> getOwmForecast(String city) {
        try {
            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/forecast?q=%s&lang=zh_cn&units=metric&appid=%s",
                    city, owmApiKey);

            String body = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .block();
            if (body == null) return List.of();

            JsonNode root = objectMapper.readTree(body);
            if (!"200".equals(root.path("cod").asText())) {
                log.warn("[Weather] OWM 返回错误: {}", root.path("message").asText());
                return List.of();
            }

            JsonNode list = root.path("list");
            if (!list.isArray()) return List.of();

            LocalDate today = LocalDate.now();
            java.util.LinkedHashMap<String, DailyWeather> dayMap = new java.util.LinkedHashMap<>();

            for (JsonNode item : list) {
                String dt = item.path("dt_txt").asText();
                // dt_txt 格式: "2026-05-28 12:00:00"，取日期部分
                String dateStr = dt.length() >= 10 ? dt.substring(0, 10) : dt;
                if (dayMap.containsKey(dateStr)) continue;

                JsonNode main = item.path("main");
                JsonNode weather = item.path("weather");
                String text = weather.isArray() && !weather.isEmpty()
                        ? weather.get(0).path("description").asText("") : "";
                JsonNode wind = item.path("wind");

                dayMap.put(dateStr, new DailyWeather(
                        dateStr,
                        formatDateLabel(dateStr, today),
                        main.path("temp_max").asInt(0),
                        main.path("temp_min").asInt(0),
                        text, text,
                        wind.path("speed").asInt(0),
                        main.path("humidity").asInt(0),
                        (int) (item.path("pop").asDouble(0) * 100)
                ));

                if (dayMap.size() >= 3) break;
            }
            return new ArrayList<>(dayMap.values());
        } catch (Exception e) {
            log.warn("[Weather] OWM 请求失败: {}", e.getMessage());
            return List.of();
        }
    }

    // ==================== wttr.in 降级 ====================

    private List<DailyWeather> getWttrForecast(String city) {
        try {
            String body = webClient.get()
                    .uri("https://wttr.in/{city}?format=j1", city)
                    .header("User-Agent", "curl/7.68.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (body == null) return List.of();

            JsonNode root = objectMapper.readTree(body);
            JsonNode weatherArray = root.path("weather");
            if (!weatherArray.isArray()) return List.of();

            LocalDate today = LocalDate.now();
            List<DailyWeather> result = new ArrayList<>();

            for (int i = 0; i < Math.min(3, weatherArray.size()); i++) {
                JsonNode day = weatherArray.get(i);
                String date = day.path("date").asText();

                // 取中间时段的天气描述
                String text = "";
                JsonNode hourly = day.path("hourly");
                if (hourly.isArray() && hourly.size() > 4) {
                    JsonNode mid = hourly.get(hourly.size() / 2);
                    text = mid.path("weatherDesc").isArray() && !mid.path("weatherDesc").isEmpty()
                            ? mid.path("weatherDesc").get(0).path("value").asText("").trim() : "";
                }

                result.add(new DailyWeather(
                        date, formatDateLabel(date, today),
                        day.path("maxtempC").asInt(0),
                        day.path("mintempC").asInt(0),
                        text, text,
                        hourly.isArray() && !hourly.isEmpty() ? hourly.get(0).path("windspeedKmph").asInt(0) : 0,
                        hourly.isArray() && !hourly.isEmpty() ? hourly.get(0).path("humidity").asInt(0) : 0,
                        0
                ));
            }
            return result;
        } catch (Exception e) {
            log.warn("[Weather] wttr.in 请求失败: {}", e.getMessage());
            return List.of();
        }
    }

    // ==================== 辅助 ====================

    private String formatDateLabel(String date, LocalDate today) {
        try {
            LocalDate d = LocalDate.parse(date, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            if (d.equals(today)) return "今天";
            if (d.equals(today.plusDays(1))) return "明天";
            if (d.equals(today.plusDays(2))) return "后天";
            return date;
        } catch (Exception e) {
            return date;
        }
    }
}
