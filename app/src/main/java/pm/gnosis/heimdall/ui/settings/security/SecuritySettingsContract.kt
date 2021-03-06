package pm.gnosis.heimdall.ui.settings.security

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result

abstract class SecuritySettingsContract : ViewModel() {
    abstract fun isFingerprintAvailable(): Boolean
    abstract fun clearFingerprintData(): Single<Result<Unit>>
}
