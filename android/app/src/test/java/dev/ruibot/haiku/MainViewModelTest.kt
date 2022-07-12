package dev.ruibot.haiku

import dev.ruibot.haiku.data.HaikuRepository
import dev.ruibot.haiku.data.Syllables
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class MainViewModelTest {
    // https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test

    @MockK
    lateinit var repository: HaikuRepository

    companion object {
        val dispatcher: TestDispatcher = StandardTestDispatcher()

        @BeforeAll
        @JvmStatic
        fun setup() {
            Dispatchers.setMain(dispatcher)
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun test() = runTest {
        coEvery { repository.getPoem(any()) }.returns(
            Result.success(
                Syllables(
                    count = 1, split = listOf(listOf(listOf("ola")))
                )
            )
        )
        val viewModel = MainViewModel(repository)
        val observed = mutableListOf<UiState>()
        viewModel.uiState
            .onEach { observed.add(it) }
            .launchIn(CoroutineScope(dispatcher))

        viewModel.inputChanged(0, "ola")
        dispatcher.scheduler.advanceTimeBy(5000)

        coVerify { repository.getPoem(any()) }
        println(observed)
    }

}
