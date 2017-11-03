package pm.gnosis.heimdall.ui.multisig.details

import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.QrCodeGenerator
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils
import pm.gnosis.heimdall.test.utils.TestCompletable
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class MultisigDetailsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var multisigRepository: MultisigRepository

    @Mock
    private lateinit var qrCodeGeneratorMock: QrCodeGenerator

    private lateinit var viewModel: MultisigDetailsViewModel

    private var testAddress = BigInteger.ZERO
    private var invalidAddress = "1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF".hexAsBigInteger()
    private var testName = "testName"

    @Before
    fun setup() {
        viewModel = MultisigDetailsViewModel(contextMock, multisigRepository, qrCodeGeneratorMock)
    }

    @Test
    fun testSetup() {
        given(multisigRepository.observeMultisigWallet(MockUtils.any())).willReturn(Flowable.just(MultisigWallet(testAddress)))

        viewModel.setup(testAddress, testName)
        viewModel.observeMultisig().subscribe(TestSubscriber())

        then(multisigRepository).should().observeMultisigWallet(testAddress)
    }

    @Test
    fun observeMultisig() {
        val testSubscriber = TestSubscriber<MultisigWallet>()
        val wallet = MultisigWallet(testAddress)
        given(multisigRepository.observeMultisigWallet(MockUtils.any())).willReturn(Flowable.just(wallet))
        viewModel.setup(testAddress, testName)

        viewModel.observeMultisig().subscribe(testSubscriber)

        testSubscriber.assertValue(wallet).assertNoErrors()
    }

    @Test
    fun observeMultisigError() {
        val testSubscriber = TestSubscriber<MultisigWallet>()
        val exception = Exception()
        given(multisigRepository.observeMultisigWallet(MockUtils.any())).willReturn(Flowable.error(exception))
        viewModel.setup(testAddress, testName)

        viewModel.observeMultisig().subscribe(testSubscriber)

        testSubscriber.assertError(exception)
    }

    @Test(expected = InvalidAddressException::class)
    fun testSetupWithInvalidAddress() {
        viewModel.setup(invalidAddress, testName)
    }

    @Test
    fun loadQrCode() {
        val bitmapMock = mock(Bitmap::class.java)
        val contents = "contents"
        val testObserver = TestObserver.create<Result<Bitmap>>()
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt())).willReturn(Single.just(bitmapMock))

        viewModel.loadQrCode(contents).subscribe(testObserver)

        then(qrCodeGeneratorMock).should().generateQrCode(contents)
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(bitmapMock)).assertNoErrors()
    }

    @Test
    fun loadQrCodeError() {
        val exception = Exception()
        val contents = "contents"
        val testObserver = TestObserver.create<Result<Bitmap>>()
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt())).willReturn(Single.error(exception))

        viewModel.loadQrCode(contents).subscribe(testObserver)

        then(qrCodeGeneratorMock).should().generateQrCode(contents)
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception)).assertNoErrors()
    }

    @Test
    fun deleteMultisig() {
        val testCompletable = TestCompletable()
        val testObserver = TestObserver<Result<Unit>>()
        given(multisigRepository.removeMultisigWallet(MockUtils.any())).willReturn(testCompletable)
        viewModel.setup(testAddress, testName)

        viewModel.deleteMultisig().subscribe(testObserver)

        assertEquals(1, testCompletable.callCount)
        then(multisigRepository).should().removeMultisigWallet(testAddress)
        then(multisigRepository).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(Unit)).assertNoErrors()
    }

    @Test
    fun deleteMultisigError() {
        val testObserver = TestObserver<Result<Unit>>()
        val exception = Exception()
        given(multisigRepository.removeMultisigWallet(MockUtils.any())).willReturn(Completable.error(exception))
        viewModel.setup(testAddress, testName)

        viewModel.deleteMultisig().subscribe(testObserver)

        then(multisigRepository).should().removeMultisigWallet(testAddress)
        then(multisigRepository).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception)).assertNoErrors()
    }

    @Test
    fun changeMultisigName() {
        val newName = "newName"
        val testCompletable = TestCompletable()
        val testObserver = TestObserver<Result<Unit>>()
        given(multisigRepository.updateMultisigWalletName(MockUtils.any(), anyString())).willReturn(testCompletable)
        viewModel.setup(testAddress, testName)

        viewModel.changeMultisigName(newName).subscribe(testObserver)

        assertEquals(1, testCompletable.callCount)
        then(multisigRepository).should().updateMultisigWalletName(testAddress, newName)
        then(multisigRepository).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(Unit)).assertNoErrors()
    }

    @Test
    fun changeMultisigNameError() {
        val newName = "newName"
        val exception = Exception()
        val testObserver = TestObserver<Result<Unit>>()
        given(multisigRepository.updateMultisigWalletName(MockUtils.any(), anyString())).willReturn(Completable.error(exception))
        viewModel.setup(testAddress, testName)

        viewModel.changeMultisigName(newName).subscribe(testObserver)

        then(multisigRepository).should().updateMultisigWalletName(testAddress, newName)
        then(multisigRepository).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception)).assertNoErrors()
    }
}