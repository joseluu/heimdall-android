package pm.gnosis.heimdall.data.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import pm.gnosis.heimdall.data.db.model.ERC20TokenDb
import pm.gnosis.heimdall.data.db.model.MultisigWalletDb

@Database(entities = arrayOf(MultisigWalletDb::class, ERC20TokenDb::class), version = 1)
abstract class GnosisAuthenticatorDb : RoomDatabase() {
    companion object {
        const val DB_NAME = "gnosis-authenticator-db"
    }

    abstract fun multisigWalletDao(): MultisigWalletDao
    abstract fun erc20TokenDao(): ERC20TokenDao
}
