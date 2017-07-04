package pm.gnosis.android.app.wallet.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import pm.gnosis.android.app.wallet.di.ForView
import pm.gnosis.android.app.wallet.di.ViewContext

@Module
class ViewModule(val context: Context) {
    @Provides
    @ForView
    @ViewContext
    fun providesContext() = context
}
