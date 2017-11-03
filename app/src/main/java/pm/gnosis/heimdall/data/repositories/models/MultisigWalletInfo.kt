package pm.gnosis.heimdall.data.repositories.models

import pm.gnosis.heimdall.data.models.Wei


data class MultisigWalletInfo(
        val address: String,
        val balance: Wei,
        val requiredConfirmations: Long,
        val owners: List<String>
)