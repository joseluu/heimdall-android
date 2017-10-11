package pm.gnosis.heimdall.data.db.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = MultisigWalletDb.TABLE_NAME)
data class MultisigWalletDb(
        @PrimaryKey
        @ColumnInfo(name = COL_ADDRESS)
        var address: String,

        @ColumnInfo(name = COL_NAME)
        var name: String?
) {
    companion object {
        const val TABLE_NAME = "multisig_wallets"
        const val COL_ADDRESS = "address"
        const val COL_NAME = "name"
    }
}
