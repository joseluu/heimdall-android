package pm.gnosis.android.app.wallet.di.component

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import dagger.Component
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.data.GethRepository
import pm.gnosis.android.app.wallet.di.ApplicationContext
import pm.gnosis.android.app.wallet.di.module.ApplicationModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(ApplicationModule::class))
interface ApplicationComponent {
    fun application(): Application
    @ApplicationContext fun context(): Context
    fun keyStore(): KeyStore
    fun moshi(): Moshi
    fun sharedPreferences(): SharedPreferences
    fun gethRepo(): GethRepository
}