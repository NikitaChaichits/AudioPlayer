package b.com.audioplayerspeed

class ConvertTime {
    /**
     * Метод для того, чтобы перевести миллисекунды
     * в обычный формат времени Часы:Минуты:Секунды
     */
    fun milliSecondsToTimer(milliseconds: Long): String {
        var finalTimerString = ""
        var secondsString: String

        // Перевод продолжительности песни из милисекунд в обычный формат времени
        val hours = (milliseconds / (1000 * 60 * 60)).toInt()
        val minutes = (milliseconds % (1000 * 60 * 60)).toInt() / (1000 * 60)
        val seconds = (milliseconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()
        // Если есть часы, добавляем
        if (hours > 0) {
            finalTimerString = "$hours:"
        }

        // добавляем первой цифрой 0, если секунд меньше 10
        if (seconds < 10) {
            secondsString = "0$seconds"
        } else {
            secondsString = "" + seconds
        }

        finalTimerString = "$finalTimerString$minutes:$secondsString"


        return finalTimerString
    }

    /**
     * Метод для того, чтобы получить процент проигранного времени.
     *
     */
    fun getProgressPercentage(currentDuration: Long, totalDuration: Long): Int {
        var percentage = currentDuration.toDouble() / totalDuration * 100
        return percentage.toInt()
    }

    /**
     * Метод для согласования процента проигранного времени с таймером песни
     * возвращает текущую продолжительность в миллисекундах
     */
    fun progressToTimer(progress: Int, totalDuration: Int): Int {
        return progress * totalDuration / 100
    }
}
