package pl.javorex.insurance.creation.adapter

import pl.javorex.insurance.creation.application.InsuranceNoGenerator

internal class InsuranceNoGeneratorTimestampImpl : InsuranceNoGenerator {
    override fun generateInsuranceNo(product: String) = "$product/${start++}"
    private companion object {
        var start = System.currentTimeMillis()

    }
}