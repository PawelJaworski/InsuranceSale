package pl.javorex.insurance.creation.adapter

import pl.javorex.insurance.creation.application.InsuranceNoGenerator

internal class InsuranceNoGeneratorTimestampImpl : InsuranceNoGenerator {
    var start = System.currentTimeMillis()

    override fun generateInsuranceNo(product: String) = "$product/${start++}"
}