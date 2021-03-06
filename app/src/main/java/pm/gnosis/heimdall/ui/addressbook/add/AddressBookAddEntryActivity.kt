package pm.gnosis.heimdall.ui.addressbook.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_address_book_add_entry.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.heimdall.utils.parseEthereumAddress
import pm.gnosis.heimdall.utils.setupToolbar
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.scanQrCode
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.exceptions.InvalidAddressException
import timber.log.Timber
import javax.inject.Inject

class AddressBookAddEntryActivity : BaseActivity() {

    override fun screenId() = ScreenId.ADDRESS_BOOK_ENTRY

    @Inject
    lateinit var viewModel: AddressBookContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_address_book_add_entry)
        setupToolbar(layout_add_address_book_entry_toolbar)
    }

    override fun onStart() {
        super.onStart()

        disposables += layout_add_address_book_entry_scan.clicks()
            .subscribeBy(onNext = { scanQrCode() })

        disposables += layout_add_address_book_entry_address.textChanges()
            .subscribeBy(onNext = { layout_add_address_book_entry_address_container.error = null })

        disposables += layout_add_address_book_entry_name.textChanges()
            .subscribeBy(onNext = { layout_add_address_book_entry_name_container.error = null })

        disposables += layout_add_address_book_entry_save.clicks()
            .flatMap {
                viewModel.addAddressBookEntry(
                    layout_add_address_book_entry_address.text.toString(),
                    layout_add_address_book_entry_name.text.toString(),
                    layout_add_address_book_entry_description.text.toString()
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onAddressBookEntryAdded, onError = ::onAddressBookEntryAddError)
    }

    private fun onAddressBookEntryAdded(entry: AddressBookEntry) {
        toast("Added ${entry.name}")
        finish()
    }

    private fun onAddressBookEntryAddError(throwable: Throwable) {
        Timber.e(throwable)
        when (throwable) {
            is InvalidAddressException -> layout_add_address_book_entry_address_container.error = getString(R.string.invalid_ethereum_address)
            is AddressBookContract.NameIsBlankException -> layout_add_address_book_entry_name_container.error =
                    getString(R.string.name_cannot_be_blank)
            is AddressBookContract.AddressAlreadyAddedException -> layout_add_address_book_entry_address_container.error =
                    getString(R.string.address_already_in_address_book)
            else -> snackbar(layout_add_address_book_entry_coordinator, getString(R.string.unknown_error))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data,
            {
                parseEthereumAddress(it)?.let {
                    layout_add_address_book_entry_address.setText(it.asEthereumAddressString())
                } ?: run {
                    snackbar(layout_add_address_book_entry_coordinator, R.string.invalid_ethereum_address)
                }
            })
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build()
            .inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, AddressBookAddEntryActivity::class.java)
    }
}
