package dev.ruibot.haiku

import dev.ruibot.haiku.domain.GetPoemSyllablesUseCase
import dev.ruibot.haiku.domain.Syllables
import dev.ruibot.haiku.presentation.write.PoemState
import dev.ruibot.haiku.presentation.write.WriteUiState
import dev.ruibot.haiku.presentation.write.WriteViewModel
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
class WriteViewModelTest {
    // https://phauer.com/2018/best-practices-unit-testing-kotlin/
    // https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-test/MIGRATION.md

    @MockK
    lateinit var useCase: GetPoemSyllablesUseCase

    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher() // StandardTestDispatcher()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterAll
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is an empty poem`() = runTest {
        coEvery { useCase.execute(any()) }.returns(
            Result.success(
                Syllables(
                    totalCount = 1,
                    syllables = listOf(
                        listOf(listOf("ola")),
                        listOf(listOf("")),
                        listOf(listOf(""))
                    ),
                )
            )
        )
        val viewModel = WriteViewModel(useCase)
        val observed = viewModel.observed()

        observed shouldHaveSize 1
        observed.first() shouldBe WriteUiState.Content(PoemState())
    }

    @Test
    fun `data is queried when input is changed`() = runTest {
        coEvery { useCase.execute(any()) }.returns(
            Result.success(
                Syllables(
                    totalCount = 1,
                    syllables = listOf(
                        listOf(listOf("ola")),
                        listOf(listOf("")),
                        listOf(listOf(""))
                    ),
                )
            )
        )
        val viewModel = WriteViewModel(useCase)

        viewModel.inputChanged(0, "ola")
        dispatcher.scheduler.advanceTimeBy(5000) // runCurrent() is not enough

        coVerify { useCase.execute(listOf("ola", "", "")) }
    }

    @Test
    fun `loading is emitted when input is changed`() = runTest {
        coEvery { useCase.execute(any()) }.returns(
            Result.success(
                Syllables(
                    totalCount = 1,
                    syllables = listOf(
                        listOf(listOf("ola")),
                        listOf(listOf("")),
                        listOf(listOf(""))
                    ),
                )
            )
        )
        val viewModel = WriteViewModel(useCase)
        val observed = viewModel.observed()

        viewModel.inputChanged(0, "ola")
        advanceUntilIdle() // we have to wait for the delay even though it's "immediate"

        observed shouldHaveSize 3
        observed[0] should { it is WriteUiState.Content }
        observed[1] should { it is WriteUiState.Loading }
        observed[2] should { it is WriteUiState.Content }
    }

    @Test
    fun `updated syllable count is emitted when input is changed`() = runTest {
        coEvery { useCase.execute(any()) }.returns(
            Result.success(
                Syllables(
                    totalCount = 3,
                    syllables = listOf(
                        listOf(listOf("ola")),
                        listOf(listOf("")),
                        listOf(listOf(""))
                    ),
                )
            )
        )
        val viewModel = WriteViewModel(useCase)
        val observed = viewModel.observed()

        viewModel.inputChanged(0, "primeiro")
        advanceUntilIdle() // we have to wait for the delay even though it's "immediate"

        (observed.last() as WriteUiState.Content).poemState.totalCount shouldBe 3
    }

    @Test
    fun `lines' content remains unchanged when syllable count is updated`() = runTest {
        coEvery { useCase.execute(any()) }.returns(
            Result.success(
                Syllables(
                    totalCount = 1,
                    syllables = listOf(
                        listOf(listOf("ola")),
                        listOf(listOf("")),
                        listOf(listOf(""))
                    ),
                )
            )
        )
        val viewModel = WriteViewModel(useCase)
        val observed = viewModel.observed()

        viewModel.inputChanged(0, "ola")
        advanceUntilIdle() // we have to wait for the delay even though it's "immediate"

        (observed[0] as WriteUiState.Content).poemState.lines.size shouldBe 3
        (observed[1] as WriteUiState.Loading).poemState.lines.size shouldBe 3
        (observed[2] as WriteUiState.Content).poemState.lines.size shouldBe 3
    }

    private fun WriteViewModel.observed(): List<WriteUiState> =
        mutableListOf<WriteUiState>().apply {
            uiState
                .onEach { add(it) }
                .launchIn(CoroutineScope(dispatcher))
        }
}
