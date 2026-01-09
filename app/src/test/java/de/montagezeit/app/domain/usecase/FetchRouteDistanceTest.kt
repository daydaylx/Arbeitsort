package de.montagezeit.app.domain.usecase

import de.montagezeit.app.data.local.dao.RouteCacheDao
import de.montagezeit.app.data.local.entity.RouteCacheEntry
import de.montagezeit.app.data.network.DistanceResult
import de.montagezeit.app.data.network.DistanceService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FetchRouteDistanceTest {

    @Test
    fun `uses cache on second request`() = runTest {
        val fakeService = FakeDistanceService()
        val fakeCache = FakeRouteCacheDao()
        val useCase = FetchRouteDistance(fakeService, fakeCache)

        val first = useCase("Leipzig", "Berlin")
        val second = useCase("Leipzig", "Berlin")

        assertEquals(1, fakeService.calls)
        assertEquals(first::class, second::class)
    }
}

private class FakeDistanceService : DistanceService {
    var calls = 0

    override suspend fun fetchRouteDistanceMeters(
        fromLabel: String,
        toLabel: String
    ): DistanceResult {
        calls += 1
        return DistanceResult.Success(distanceMeters = 125000.0)
    }
}

private class FakeRouteCacheDao : RouteCacheDao {
    private val cache = mutableMapOf<String, RouteCacheEntry>()

    override suspend fun getEntry(fromLabel: String, toLabel: String): RouteCacheEntry? {
        return cache[key(fromLabel, toLabel)]
    }

    override suspend fun upsert(entry: RouteCacheEntry) {
        cache[key(entry.fromLabel, entry.toLabel)] = entry
    }

    private fun key(from: String, to: String): String {
        return "$from|$to"
    }
}
