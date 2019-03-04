package pl.javorex.insurance.premium.domain

val ALLOWED_NUMBER_OF_PREMIUM = setOf(1, 2)
internal data class NumberOfPremium(val value: Int) : ValueObject {
    init {
        if (!ALLOWED_NUMBER_OF_PREMIUM.contains(value)){
            throw IllegalStateException("$value is not allowed as number of premium")
        }
    }
}