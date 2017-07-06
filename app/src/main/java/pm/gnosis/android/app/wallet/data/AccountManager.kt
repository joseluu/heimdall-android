package pm.gnosis.android.app.wallet.data

import org.ethereum.geth.Account
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.util.edit
import pm.gnosis.android.app.wallet.util.generateRandomString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountManager @Inject constructor(val preferencesManager: PreferencesManager, val keyStore: KeyStore) {
    fun getAccount(): Account = //Only one account per installation
            if (keyStore.accounts.size() > 0) keyStore.accounts[0] else keyStore.newAccount(getAccountPassphrase())

    fun getAccountPassphrase(): String {
        var passphrase = preferencesManager.prefs.getString(PreferencesManager.PASSPHRASE_KEY, "")
        if (passphrase.isEmpty()) {
            preferencesManager.prefs.edit {
                passphrase = generateRandomString()
                putString(PreferencesManager.PASSPHRASE_KEY, passphrase)
            }
        }
        return passphrase
    }
}