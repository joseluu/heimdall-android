package pm.gnosis.heimdall.ui.safe.add

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_address_input.view.*
import kotlinx.android.synthetic.main.include_gas_price_selection.*
import kotlinx.android.synthetic.main.layout_additional_owner_item.view.*
import kotlinx.android.synthetic.main.layout_address_item.view.*
import kotlinx.android.synthetic.main.layout_deploy_new_safe.*
import kotlinx.android.synthetic.main.layout_security_bars.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.heimdall.helpers.GasPriceHelper
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.utils.*
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.ticker.data.repositories.models.Currency
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.asEthereumAddressStringOrNull
import pm.gnosis.utils.isValidEthereumAddress
import pm.gnosis.utils.stringWithNoTrailingZeroes
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

class DeployNewSafeFragment : BaseFragment() {

    @Inject
    lateinit var gasPriceHelper: GasPriceHelper

    @Inject
    lateinit var viewModel: AddSafeContract

    private var displayFeesTransformer = ObservableTransformer<Pair<Result<GasEstimate>, Result<Wei>>, Result<Pair<BigDecimal, Currency>>> {
        it
            .map { (estimate, overrideGasPrice) ->
                // If we have an estimate calculate the price
                estimate.map {
                    val override = (overrideGasPrice as? DataResult)?.data
                    Wei((override ?: it.gasPrice).value * it.gasCosts)
                }
            }
            // Update price
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNextForResult(::updateEstimate)
            // Request fiat value for price
            .flatMapResult({ viewModel.loadFiatConversion(it) })
            // Update fiat value
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNextForResult(::onFiat, ::onFiatError)
    }

    private var deployButtonTransformer = ObservableTransformer<Pair<Result<GasEstimate>, Result<Wei>>, Result<Unit>> {
        it.switchMap { (_, overrideGasPrice) ->
            layout_deploy_new_safe_deploy_button.clicks()
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap {
                    viewModel.deployNewSafe(layout_deploy_new_safe_name_input.text.toString(), (overrideGasPrice as? DataResult)?.data)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { toggleDeploying(true) }
                        .doAfterTerminate { toggleDeploying(false) }
                }
                .doOnNextForResult(::safeDeployed, ::errorDeploying)
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.setupDeploy()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                layout_deploy_new_safe_device_info.apply {
                    layout_address_item_icon.setAddress(it)
                    layout_address_item_name.visible(true)
                    layout_address_item_name.text = getString(R.string.this_device)
                    layout_address_item_value.visible(false)
                }
            }
            .flatMapObservable {
                handleUserInput()
            }
            .subscribeBy(onError = Timber::e)

        disposables += viewModel.observeAdditionalOwners()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::updateOwners, Timber::e)

        disposables += layout_deploy_new_safe_deploy_external.clicks()
            .flatMapSingle { viewModel.loadDeployData(layout_deploy_new_safe_name_input.text.toString()) }
            .subscribeForResult(onNext = {
                startActivityWithTransaction(it, onActivityNotFound = {
                    activity?.toast(R.string.no_external_wallets)
                    Timber.w("No activity resolved for intent")
                })
            }, onError = Timber::e)
    }

    private fun handleUserInput() =
        Observable.merge(
            setupDeploySafe(),
            setupAddAddress()
        )

    private fun setupDeploySafe() =
        Observable.combineLatest(
            // Estimates to deploy safe
            viewModel.observeEstimate(),
            // Price data
            gasPriceHelper.let {
                it.setup(include_gas_price_selection_root_container)
                it.observe()
            }.startWith(ErrorResult(Exception())),
            BiFunction { estimate: Result<GasEstimate>, prices: Result<Wei> -> estimate to prices }
        )
            .observeOn(AndroidSchedulers.mainThread())
            .publish {
                Observable.merge(
                    // Display fees
                    it.compose(displayFeesTransformer),
                    // Setup deploy button
                    it.compose(deployButtonTransformer)
                )
            }

    private fun toggleDeploying(inProgress: Boolean) {
        layout_deploy_new_safe_deploy_button.isEnabled = !inProgress
        layout_deploy_new_safe_name_input.isEnabled = !inProgress
        layout_deploy_new_safe_progress.visible(inProgress)
    }

    private fun setupAddAddress() =
        layout_deploy_new_safe_add_owner_button.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                showAddOwnerDialog()
            }

    private fun showAddOwnerDialog() {
        // TODO: add proper dialog once design is known
        val dialogView = layoutInflater.inflate(R.layout.dialog_address_input, null)
        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton(R.string.add, { _, _ -> addOwner(dialogView.dialog_address_input_address.text.toString()) })
            .setNeutralButton(R.string.scan, { _, _ -> scanQrCode() })
            .setOnDismissListener { activity?.hideSoftKeyboard() }
            .show()
    }

    private fun addOwner(address: String) {
        disposables += viewModel.addAdditionalOwner(address)
            .subscribeForResult(onError = { errorSnackbar(layout_deploy_new_safe_name_input, it) })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleTransactionHashResult(requestCode, resultCode, data,
            { transactionHash ->
                disposables += viewModel.saveTransactionHash(transactionHash, layout_deploy_new_safe_name_input.text.toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = { safeDeployed(Unit) }, onError = Timber::e)
            })

        handleQrCodeActivityResult(requestCode, resultCode, data,
            { parseEthereumAddress(it)?.let { addOwner(it.asEthereumAddressString()) } })
    }

    private fun safeDeployed(ignored: Unit) {
        activity?.finish()
    }

    private fun errorDeploying(throwable: Throwable) {
        view?.let { errorSnackbar(it, throwable) }
    }

    private fun updateEstimate(estimate: Wei) {
        layout_deploy_new_safe_transaction_fee_fiat.visibility = View.INVISIBLE
        layout_deploy_new_safe_transaction_fee.text = estimate.displayString(context!!)
    }

    private fun onFiat(fiat: Pair<BigDecimal, Currency>) {
        layout_deploy_new_safe_transaction_fee_fiat.visibility = View.VISIBLE
        layout_deploy_new_safe_transaction_fee_fiat.text =
                getString(
                    R.string.fiat_approximation,
                    fiat.first.stringWithNoTrailingZeroes(),
                    fiat.second.getFiatSymbol()
                )
    }

    private fun onFiatError(throwable: Throwable) {
        Timber.e(throwable)
        layout_deploy_new_safe_transaction_fee_fiat.visibility = View.GONE
    }

    private fun updateOwners(additionalOwners: List<BigInteger>) {
        layout_deploy_new_safe_additional_owners_container.removeAllViews()
        additionalOwners.forEach { address ->
            if (address.isValidEthereumAddress()) {
                val view = layoutInflater.inflate(R.layout.layout_additional_owner_item, layout_deploy_new_safe_additional_owners_container, false)
                address.asEthereumAddressStringOrNull()?.let { view.layout_address_item_value.text = it }
                view.layout_address_item_icon.setAddress(address)
                disposables += view.layout_additional_owner_delete_button.clicks()
                    .flatMap { viewModel.removeAdditionalOwner(address) }
                    .subscribeForResult(
                        { snackbar(layout_deploy_new_safe_additional_owners_container, getString(R.string.removed_x, address)) },
                        { errorSnackbar(layout_deploy_new_safe_additional_owners_container, it) }
                    )
                layout_deploy_new_safe_additional_owners_container.addView(view)
            }
        }

        layout_deploy_new_safe_add_owner_button.visible(additionalOwners.size < 2)
        updateSecurityBar(additionalOwners.size)
    }


    private fun updateSecurityBar(additionalOwners: Int) {
        val securityInfoTextResource: Int
        val colorResource: Int
        val securityLevelTextResource: Int
        when (additionalOwners) {
            0 -> {
                securityInfoTextResource = R.string.security_info_weak
                colorResource = R.color.security_bar_low
                securityLevelTextResource = R.string.weak
            }
            1 -> {
                securityInfoTextResource = R.string.security_info_good
                colorResource = R.color.security_bar_good
                securityLevelTextResource = R.string.good
            }
            else -> {
                securityInfoTextResource = R.string.security_info_best
                colorResource = R.color.security_bar_best
                securityLevelTextResource = R.string.best
            }
        }

        layout_security_bars_first.setColorFilterCompat(if (additionalOwners >= 0) colorResource else R.color.security_bar_default)
        layout_security_bars_second.setColorFilterCompat(if (additionalOwners >= 1) colorResource else R.color.security_bar_default)
        layout_security_bars_third.setColorFilterCompat(if (additionalOwners >= 2) colorResource else R.color.security_bar_default)
        layout_deploy_new_safe_security_level_text.text = SpannableStringBuilder("")
            .appendText(getString(R.string.security_level), ForegroundColorSpan(context!!.getColorCompat(R.color.gnosis_dark_blue)))
            .append(": ")
            .appendText(getString(securityLevelTextResource), ForegroundColorSpan(context!!.getColorCompat(colorResource)))
        layout_deploy_new_safe_security_info.text = getString(securityInfoTextResource)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.layout_deploy_new_safe, container, false)!!

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .applicationComponent(component)
            .viewModule(ViewModule(context!!))
            .build().inject(this)
    }

    companion object {
        fun createInstance() = DeployNewSafeFragment()
    }
}
