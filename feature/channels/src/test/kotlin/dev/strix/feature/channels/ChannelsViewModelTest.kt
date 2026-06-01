package dev.strix.feature.channels

import androidx.paging.PagingData
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelId
import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.core.common.repository.ChannelRepository
import dev.strix.core.common.result.StrixError
import dev.strix.core.common.result.StrixResult
import dev.strix.core.common.result.asFailure
import dev.strix.core.common.result.asSuccess
import dev.strix.core.data.ChannelPagingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class ChannelsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private val pagingRepository =
        object : ChannelPagingRepository {
            override fun pagedChannels(
                query: String?,
                category: String?,
            ): Flow<PagingData<Channel>> = flowOf(PagingData.empty())

            override fun categories(): Flow<List<String>> = flowOf(emptyList())
        }

    private val epgRepository =
        object : dev.strix.core.common.epg.EpgRepository {
            override suspend fun nowNext(channel: Channel) = null

            override suspend fun refresh() = Unit
        }

    @androidx.media3.common.util.UnstableApi
    private val playerFactory =
        object : dev.strix.core.player.StrixPlayerFactory {
            override fun create() = error("preview player is not used in these tests")
        }

    private class FakeChannelRepository(
        var refreshResult: StrixResult<Int> = 0.asSuccess(),
    ) : ChannelRepository {
        override fun observeChannels(query: String?): Flow<List<Channel>> = flowOf(emptyList())

        override suspend fun channelById(id: ChannelId): Channel? = null

        override suspend fun nextChannel(id: ChannelId): Channel? = null

        override suspend fun previousChannel(id: ChannelId): Channel? = null

        override suspend fun variants(id: ChannelId): List<Channel> = emptyList()

        override suspend fun refreshFrom(source: StreamSourceConfig): StrixResult<Int> = refreshResult
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @androidx.media3.common.util.UnstableApi
    private fun viewModel(repo: ChannelRepository = FakeChannelRepository()) =
        ChannelsViewModel(pagingRepository, repo, epgRepository, playerFactory)

    @Test
    fun `search intent updates the query in state`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.uiState.test {
                assertThat(awaitItem().query).isEmpty()
                vm.onIntent(ChannelsIntent.SearchChanged("bbc"))
                assertThat(awaitItem().query).isEqualTo("bbc")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `rapid focus changes emit only the settled channel after debounce`() =
        runTest(dispatcher) {
            val vm = viewModel()
            val emissions = mutableListOf<Channel>()
            val job = backgroundScope.launch { vm.playbackTarget.collect { emissions += it } }

            val a = channel("a")
            val b = channel("b")
            vm.onIntent(ChannelsIntent.ChannelFocused(a))
            vm.onIntent(ChannelsIntent.ChannelFocused(b))
            advanceTimeBy(301)

            assertThat(emissions).containsExactly(b)
            job.cancel()
        }

    @Test
    fun `refresh failure surfaces the error message and clears refreshing`() =
        runTest(dispatcher) {
            val repo = FakeChannelRepository(refreshResult = StrixError.Network("down").asFailure())
            val vm = viewModel(repo)
            backgroundScope.launch { vm.uiState.collect { } }

            vm.onIntent(ChannelsIntent.Refresh(StreamSourceConfig.M3u("http://host/x.m3u")))
            advanceUntilIdle()

            assertThat(vm.uiState.value.errorMessage).isEqualTo("down")
            assertThat(vm.uiState.value.isRefreshing).isFalse()
        }

    @Test
    fun `refresh success leaves no error`() =
        runTest(dispatcher) {
            val repo = FakeChannelRepository(refreshResult = 42.asSuccess())
            val vm = viewModel(repo)
            backgroundScope.launch { vm.uiState.collect { } }

            vm.onIntent(ChannelsIntent.Refresh(StreamSourceConfig.M3u("http://host/x.m3u")))
            advanceUntilIdle()

            assertThat(vm.uiState.value.errorMessage).isNull()
            assertThat(vm.uiState.value.isRefreshing).isFalse()
        }

    private fun channel(id: String) = Channel(id = ChannelId(id), name = id, streamUrl = "http://host/$id")
}
