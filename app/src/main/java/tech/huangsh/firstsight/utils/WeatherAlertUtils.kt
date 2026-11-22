package tech.huangsh.firstsight.utils

import tech.huangsh.firstsight.data.model.WeatherInfo

data class WeatherAlert(
    val key: String,
    val title: String,
    val message: String
)

/**
 * 天气接口没有单独返回预警字段，这里按天气描述和温度做保守判断，
 * 只在明显恶劣天气时提示，避免每天弹无意义卡片。
 */
object WeatherAlertUtils {

    fun fromWeather(info: WeatherInfo?): WeatherAlert? {
        if (info == null || info.error != null) return null
        val weather = info.weather.trim()
        if (weather.isBlank()) return null

        val city = info.city.ifBlank { "本地" }
        val high = info.high ?: info.temperature
        val low = info.low ?: info.temperature

        return when {
            weather.contains("台风") -> WeatherAlert(
                key = "typhoon",
                title = "台风提醒",
                message = "${city}今天有台风，请尽量留在室内，关好门窗，减少外出。"
            )
            weather.contains("暴雨") || weather.contains("大暴雨") -> WeatherAlert(
                key = "rainstorm",
                title = "暴雨提醒",
                message = "${city}今天有暴雨，尽量留在家里，出门注意积水和路滑。"
            )
            weather.contains("大雨") -> WeatherAlert(
                key = "heavy_rain",
                title = "大雨提醒",
                message = "${city}今天有大雨，出门记得带伞，尽量避开低洼路段。"
            )
            weather.contains("雪") -> WeatherAlert(
                key = "snow",
                title = "雨雪提醒",
                message = "${city}今天有雨雪，路面可能湿滑，出门请穿防滑鞋，注意保暖。"
            )
            weather.contains("冰雹") -> WeatherAlert(
                key = "hail",
                title = "冰雹提醒",
                message = "${city}今天可能有冰雹，请尽量不要外出，远离窗户和广告牌。"
            )
            weather.contains("雷电") || weather.contains("雷暴") -> WeatherAlert(
                key = "thunder",
                title = "雷电提醒",
                message = "${city}今天有雷电，尽量待在室内，不要在大树或电线杆下停留。"
            )
            high >= 35 -> WeatherAlert(
                key = "heat",
                title = "高温提醒",
                message = "${city}今天最高${high}度，注意防暑，多喝水，尽量避开中午外出。"
            )
            low <= -10 -> WeatherAlert(
                key = "cold",
                title = "低温提醒",
                message = "${city}今天最低${low}度，出门注意保暖，路面可能结冰。"
            )
            weather.contains("寒潮") -> WeatherAlert(
                key = "cold_wave",
                title = "寒潮提醒",
                message = "${city}今天有寒潮，气温下降明显，请及时添衣。"
            )
            weather.contains("大风") -> WeatherAlert(
                key = "wind",
                title = "大风提醒",
                message = "${city}今天有大风，出门注意高空坠物，尽量远离广告牌和临时搭建物。"
            )
            weather.contains("霾") -> WeatherAlert(
                key = "haze",
                title = "雾霾提醒",
                message = "${city}今天有雾霾，出门建议戴口罩，减少长时间户外活动。"
            )
            weather.contains("雾") -> WeatherAlert(
                key = "fog",
                title = "大雾提醒",
                message = "${city}今天有大雾，能见度低，出门注意交通安全。"
            )
            else -> null
        }
    }
}
