package com.windrr.hyundaesub.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.windrr.hyundaesub.data.local.entity.DimensionSource
import com.windrr.hyundaesub.data.local.entity.Orientation
import com.windrr.hyundaesub.data.local.entity.SavedItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedItemDaoTest {

    private lateinit var db: AppDatabase

    private val chair = SavedItemEntity(
        savedItemId = "chair",
        name = "캠핑 의자",
        widthCm = 20,
        depthCm = 20,
        heightCm = 60,
        defaultQuantity = 2,
        dimensionSource = DimensionSource.AR,
        createdAt = 1_000L,
    )
    private val box = SavedItemEntity(
        savedItemId = "box",
        name = "캠핑박스 L",
        widthCm = 60,
        depthCm = 40,
        heightCm = 35,
        allowedOrientations = Orientation.entries,
        dimensionSource = DimensionSource.MANUAL,
        createdAt = 2_000L,
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun observeAll_newestFirst_withOrientations() = runBlocking {
        db.savedItemDao().upsert(chair, box)

        val items = db.savedItemDao().observeAll().first()
        assertEquals(listOf("box", "chair"), items.map { it.savedItemId })
        assertEquals(listOf(Orientation.WDH, Orientation.DWH), items.last().allowedOrientations)
        assertEquals(6, items.first().allowedOrientations.size)
        assertEquals(DimensionSource.AR, items.last().dimensionSource)
    }

    @Test
    fun upsert_sameId_replaces_andDeleteRemoves() = runBlocking {
        db.savedItemDao().upsert(chair)
        db.savedItemDao().upsert(chair.copy(defaultQuantity = 4))
        assertEquals(4, db.savedItemDao().get("chair")?.defaultQuantity)

        db.savedItemDao().delete("chair")
        assertNull(db.savedItemDao().get("chair"))
    }
}
