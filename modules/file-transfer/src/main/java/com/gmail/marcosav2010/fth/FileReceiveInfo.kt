package com.gmail.marcosav2010.fth

class FileReceiveInfo internal constructor(val fileName: String, val blocks: Int = 0) {

    internal var firstArrivalTime: Long = -1
        private set

    private var lastArrivalTime: Long = -1

    val isSingle: Boolean get() = blocks == 1

    @Synchronized
    fun setFirstArrivalTime() {
        if (firstArrivalTime == -1L) {
            firstArrivalTime = System.currentTimeMillis()
            lastArrivalTime = firstArrivalTime
        }
    }

    @Synchronized
    fun updateLastArrivalTime(req: Long): Boolean {
        val current = System.currentTimeMillis()
        val diff = current - lastArrivalTime
        return if (diff >= req) {
            lastArrivalTime = current
            true
        } else false
    }
}