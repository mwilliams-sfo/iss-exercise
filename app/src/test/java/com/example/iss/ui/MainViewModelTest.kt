package com.example.iss.ui

import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.iss.api.iss.ISSApi
import com.example.iss.api.iss.model.Person
import com.example.iss.api.iss.model.Position
import com.example.iss.api.iss.model.response.AstrosResponse
import com.example.iss.api.iss.model.response.ISSNowResponse
import com.example.iss.db.AppDatabase
import com.example.iss.db.dao.PositionDao
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.just
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
        every { anyConstructed<Location>().latitude = any() } just Runs
        every { anyConstructed<Location>().latitude } returns 10.0
        every { anyConstructed<Location>().longitude = any() } just Runs
        every { anyConstructed<Location>().longitude } returns 20.0
        every { positionDao.insert(any()) } just Runs
        viewModel.issPosition.observeForever {}
        viewModel.positionLog.observeForever {}

        viewModel.updateIssPosition()

        coVerify { issApi.issNow() }

        val issPosition = viewModel.issPosition.value
        assertEquals(10.0, issPosition?.latitude)
        assertEquals(20.0, issPosition?.longitude)

        val positionSlot = slot<DBPosition>()
        verify { positionDao.insert(capture(positionSlot)) }
        assertEquals(
            DBPosition(id = 0, time = 1L, latitude = 10.0, longitude = 20.0),
            positionSlot.captured
        )
    }

    @Test
    fun `nadirDistance is updated from the GPS and ISS positions`() = runTest {
        createViewModel()
        val gpsLocation = mockk<Location>().apply {
            every { latitude } returns 10.0
            every { longitude } returns 20.0
            every { distanceTo(any()) } returns 1000f
        }
        coEvery { issApi.issNow() } returns ISSNowResponse(
            message = "success",
            timestamp = 1L,
            issPosition = Position(latitude = 10.0, longitude = 20.0)
        )
        mockkConstructor(Location::class)
        every { anyConstructed<Location>().latitude = any() } just Runs
        every { anyConstructed<Location>().longitude = any() } just Runs
        every { positionDao.insert(any()) } just Runs
        viewModel.nadirDistance.observeForever {}

        viewModel.onLocationChanged(gpsLocation)
        viewModel.updateIssPosition()

        assertEquals(1000f, viewModel.nadirDistance.value)
    }

    @Test
    fun `updateAstronautNames updates the astronaut names`() = runTest {
        createViewModel()
        coEvery { issApi.astros() } returns AstrosResponse(
            message = "success",
            number = 2,
            people = listOf(
                Person(craft = "Skylab", name = "Neil Armstrong"),
                Person(craft = "ISS", name = "Buzz Lightyear")
            )
        )
        viewModel.astronautNames.observeForever {}

        viewModel.updateAstronautNames()

        coVerify { issApi.astros() }
        assertEquals(listOf("Buzz Lightyear"), viewModel.astronautNames.value)
    }

    @Test
    fun `positionLog reflects the database contents`() {
        createViewModel()
        viewModel.positionLog.observeForever {}

        val position = DBPosition(id = 0, time = 1L, latitude = 10.0, longitude = 20.0)
        allPositions.value = listOf(position)

        assertEquals(listOf(position), viewModel.positionLog.value)
    }
}
