package dev.ruibot.haiku.domain

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dev.ruibot.haiku.data.HaikuRepository

@Module
@InstallIn(ViewModelComponent::class)
abstract class DomainModule {
    @Binds
    abstract fun bindSyllablesRepository(
        repository: HaikuRepository
    ): SyllablesRepository
}
