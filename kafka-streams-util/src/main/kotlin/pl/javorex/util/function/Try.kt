package pl.javorex.util.function

data class Success<out T>(val success: T) : Try<T>() {
    override fun isSuccess(): Boolean = true
    override fun isFailure(): Boolean = false
}

data class Failure<out T>(val error: Throwable) : Try<T>() {
    override fun isSuccess(): Boolean = false
    override fun isFailure(): Boolean = true
}

sealed class Try<out T> {
    abstract fun isSuccess(): Boolean
    abstract fun isFailure(): Boolean

    companion object {
        operator fun <T> invoke(body: () -> T): Try<T> {
            return try {
                Success(body())
            } catch (e: Exception) {
                Failure(e)
            }
        }
    }
}