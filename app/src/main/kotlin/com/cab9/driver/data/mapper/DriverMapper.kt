package com.cab9.driver.data.mapper

import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.DriverModel

class DriverMapper : Mapper<Driver, DriverModel> {
    override fun map(input: Driver) = DriverModel(
        id = input.id.orEmpty(),
        callSign = input.callSign.orEmpty(),
        firstName = input.firstName.orEmpty(),
        lastName = input.lastName.orEmpty(),
        mobile = input.mobile.orEmpty(),
        imageUrl = input.imageUrl.orEmpty(),
        status = input.status ?: Driver.Status.OFFLINE,
        email = input.email.orEmpty(),
        address1 = input.address1.orEmpty(),
        address2 = input.address2.orEmpty(),
        townCity = input.townCity.orEmpty(),
        postcode = input.postcode.orEmpty(),
        country = input.country.orEmpty()
    )
}