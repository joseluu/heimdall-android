package pm.gnosis.heimdall.ui.transactions.details.safe

import android.arch.lifecycle.ViewModel
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class ChangeSafeSettingsDetailsContract : ViewModel() {
    abstract fun loadAction(safeAddress: BigInteger?, transaction: Transaction?): Single<Action>
    abstract fun loadFormData(preset: Transaction?): Single<Pair<String, Int>>
    abstract fun inputTransformer(safeAddress: BigInteger?): ObservableTransformer<CharSequence, Result<Transaction>>

    sealed class Action {
        data class RemoveOwner(val owner: BigInteger) : Action()
        data class AddOwner(val owner: BigInteger) : Action()
        data class ReplaceOwner(val newOwner: BigInteger, val previousOwner: BigInteger) : Action()
    }
}
