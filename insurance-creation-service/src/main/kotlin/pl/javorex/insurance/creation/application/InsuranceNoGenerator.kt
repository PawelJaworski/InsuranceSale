package pl.javorex.insurance.creation.application

interface InsuranceNoGenerator {
    fun generateInsuranceNo(product: String): String
}