package dev.ruibot.haiku

import dev.ruibot.haiku.data.HaikuRepository
import dev.ruibot.haiku.data.Syllables
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainViewModelTest {
    // https://phauer.com/2018/best-practices-unit-testing-kotlin/
    // https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-test/MIGRATION.md

    @MockK
    lateinit var repository: HaikuRepository

    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher() // StandardTestDispatcher()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val arr = IntArray(2)
        for (i in arr.indices) {

        }
    }

    @AfterAll
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is an empty poem`() = runTest {
        coEvery { repository.getPoem(any()) }.returns(
            Result.success(
                Syllables(
                    count = 1,
                    split = listOf(
                        listOf(listOf("ola")),
                        listOf(listOf("")),
                        listOf(listOf(""))
                    ),
                )
            )
        )
        val viewModel = MainViewModel(repository)
        val observed = viewModel.observed()

        observed shouldHaveSize 1
        observed.first() shouldBe UiState.Content(PoemState())
    }

    @Test
    fun `data is queried when input is changed`() = runTest {
        coEvery { repository.getPoem(any()) }.returns(
            Result.success(
                Syllables(
                    count = 1,
                    split = listOf(
                        listOf(listOf("ola")),
                        listOf(listOf("")),
                        listOf(listOf(""))
                    ),
                )
            )
        )
        val viewModel = MainViewModel(repository)

        viewModel.inputChanged(0, "ola")
        dispatcher.scheduler.advanceTimeBy(5000) // runCurrent() is not enough

        coVerify { repository.getPoem(any()) }
    }

    @Test
    fun `loading is emitted when input is changed`() = runTest {
        coEvery { repository.getPoem(any()) }.returns(
            Result.success(
                Syllables(
                    count = 1,
                    split = listOf(
                        listOf(listOf("ola")),
                        listOf(listOf("")),
                        listOf(listOf(""))
                    ),
                )
            )
        )
        val viewModel = MainViewModel(repository)
        val observed = viewModel.observed()

        viewModel.inputChanged(0, "ola")
        advanceUntilIdle() // we have to wait for the delay even though it's "immediate"

        observed shouldHaveSize 3
        observed[0] should { it is UiState.Content }
        observed[1] should { it is UiState.Loading }
        observed[2] should { it is UiState.Content }
    }

    @Test
    fun `updated syllable count is emitted when input is changed`() = runTest {
        coEvery { repository.getPoem(any()) }.returns(
            Result.success(
                Syllables(
                    count = 3,
                    split = listOf(
                        listOf(listOf("ola")),
                        listOf(listOf("")),
                        listOf(listOf(""))
                    ),
                )
            )
        )
        val viewModel = MainViewModel(repository)
        val observed = viewModel.observed()

        viewModel.inputChanged(0, "primeiro")
        advanceUntilIdle() // we have to wait for the delay even though it's "immediate"

        (observed.last() as UiState.Content).poemState.totalCount shouldBe 3
    }

    @Test
    fun `lines' content remains unchanged when syllable count is updated`() = runTest {
        coEvery { repository.getPoem(any()) }.returns(
            Result.success(
                Syllables(
                    count = 1,
                    split = listOf(
                        listOf(listOf("ola")),
                        listOf(listOf("")),
                        listOf(listOf(""))
                    ),
                )
            )
        )
        val viewModel = MainViewModel(repository)
        val observed = viewModel.observed()

        viewModel.inputChanged(0, "ola")
        advanceUntilIdle() // we have to wait for the delay even though it's "immediate"

        (observed[0] as UiState.Content).poemState.lines.size shouldBe 3
        (observed[1] as UiState.Loading).poemState.lines.size shouldBe 3
        (observed[2] as UiState.Content).poemState.lines.size shouldBe 3
    }

    private fun MainViewModel.observed(): List<UiState> =
        mutableListOf<UiState>().apply {
            uiState
                .onEach { add(it) }
                .launchIn(CoroutineScope(dispatcher))
        }
}
