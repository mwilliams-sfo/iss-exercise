package com.example.iss.ui

import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.iss.api.iss.ISSApi
import com.example.iss.api.iss.model.Position
import com.example.iss.api.iss.model.response.ISSNowResponse
import com.example.iss.db.AppDatabase
import com.example.iss.db.dao.PositionDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.example.iss.db.entity.Position as DBPosition

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var issApi: ISSApi

    @MockK
    private lateinit var database: AppDatabase

    @MockK
    private lateinit var positionDao: PositionDao

    private val allPositions = MutableLiveData(emptyList<DBPosition>())

    private lateinit var viewModel: MainViewModel

    private fun createViewModel() {
        viewModel = MainViewModel(issApi, database)
    }

    @Before
    fun setUp() {
        every { database.positionDao() } returns positionDao
        every { positionDao.getAll() } returns allPositions
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `LiveData initial values`() {
        createViewModel()
        assertNull(viewModel.gpsLocation.value)
        assertNull(viewModel.issPosition.value)
        assertNull(viewModel.nadirDistance.value)
        assertNull(viewModel.astronautNames.value)
        assertEquals(emptyList<DBPosition>(), viewModel.positionLog.value)
    }

    @Test
    fun `onLocationChanged updates the GPS location`() = runTest {
        createViewModel()
        val location = mockk<Location>()
        viewModel.gpsLocation.observeForever {}
        viewModel.onLocationChanged(location)
        runCurrent()

        val gpsLocation = viewModel.gpsLocation.value
        assertSame(gpsLocation, location)
    }

    @Test
    fun `updateIssPosition updates the position and the log`() = runTest {
        createViewModel()
        coEvery { issApi.issNow() } returns ISSNowResponse(
            message = "success",
            timestamp = 1L,
            issPosition = Position(latitude = 10.0, longitude = 20.0)
        )
        mockkConstructor(Location::class)
        every { anyConstructed<Location>().latitude = any() } returns Unit
        every { anyConstructed<Location>().latitude } returns 10.0
        every { anyConstructed<Location>().longitude = any() } returns Unit
        every { anyConstructed<Location>().longitude } returns 20.0
        every { positionDao.insert(any()) } answers {
            val position = invocation.args[0] as DBPosition
            allPositions.value = listOf(position)
        }
        viewModel.issPosition.observeForever {}
        viewModel.positionLog.observeForever {}

        viewModel.updateIssPosition()

        coVerify { issApi.issNow() }

        val issPosition = viewModel.issPosition.value
        assertEquals(10.0, issPosition?.latitude)
        assertEquals(20.0, issPosition?.longitude)

        val positionSlot = slot<DBPosition>()
        verify { positionDao.insert(capture(positionSlot)) }
        assertEquals(1L, positionSlot.captured.time)
        assertEquals(10.0, positionSlot.captured.latitude, 0.0)
        assertEquals(20.0, positionSlot.captured.longitude, 0.0)
    }
}
