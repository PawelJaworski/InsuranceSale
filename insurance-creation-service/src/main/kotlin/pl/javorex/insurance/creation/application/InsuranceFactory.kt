package pl.javorex.insurance.creation.application

import pl.javorex.insurance.creation.domain.event.InsuranceCreated
import pl.javorex.insurance.creation.domain.event.InsuranceCreationStarted
import pl.javorex.insurance.premium.domain.event.PremiumCalculationCompleted

class InsuranceFactory(private val insuranceNoGenerator: InsuranceNoGenerator) {
    fun createInsurance(insuranceCreationStarted: InsuranceCreationStarted, premiumCalculationCompleted: PremiumCalculationCompleted) : InsuranceCreated {
        val creationSource = insuranceCreationStarted.source
        val insuranceNo = insuranceNoGenerator.generateInsuranceNo(
                insuranceCreationStarted.insuranceProduct
        )
        return InsuranceCreated(creationSource, insuranceNo)
    }
}