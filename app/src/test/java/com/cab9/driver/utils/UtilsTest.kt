package com.cab9.driver.utils

import org.junit.Test

class UtilsTest {

    companion object {
        private const val JOB_ID_REGEX = "\"Job (\\d+)\""
        private const val JOB_ID = "6164905"
    }

    @Test
    fun `find job number from transaction info`() {
        val data =
            "{\"mAmount\":9.5,\"mCard\":{\"last_4_digits\":\"3372\",\"mType\":\"VISA\"},\"mCurrency\":\"GBP\",\"mEntryMode\":\"contactless\",\"mInstallments\":1,\"mMerchantCode\":\"ME3TQXDC\",\"mPaymentType\":\"POS\",\"mProducts\":[{\"mName\":\"Job $JOB_ID\",\"mPrice\":9.5,\"mQuantity\":1}],\"mStatus\":\"SUCCESSFUL\",\"mTipAmount\":0.0,\"mTransactionCode\":\"TDMN3FKFE2\",\"mVatAmount\":0.0}"

        val pattern = Regex(JOB_ID_REGEX)
        val result = pattern.find(data)?.groupValues?.get(1)
        print("<JobId = $result> ")
        assert(result == JOB_ID)
    }

    @Test
    fun `find job number from transaction info without price`() {
        val data =
            "{\"mAmount\":9.5,\"mCard\":{\"last_4_digits\":\"3372\",\"mType\":\"VISA\"},\"mCurrency\":\"GBP\",\"mEntryMode\":\"contactless\",\"mInstallments\":1,\"mMerchantCode\":\"ME3TQXDC\",\"mPaymentType\":\"POS\",\"mProducts\":[{\"mName\":\"Job $JOB_ID\",\"mQuantity\":1}],\"mStatus\":\"SUCCESSFUL\",\"mTipAmount\":0.0,\"mTransactionCode\":\"TDMN3FKFE2\",\"mVatAmount\":0.0}"

        val pattern = Regex(JOB_ID_REGEX)
        val result = pattern.find(data)?.groupValues?.get(1)
        print("<JobId = $result> ")
        assert(result == JOB_ID)
    }

}