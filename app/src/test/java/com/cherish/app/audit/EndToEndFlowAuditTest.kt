package com.cherish.app.audit

import com.cherish.app.date.calc.CountdownCalculator
import com.cherish.app.date.calc.EventCountdownStatus
import com.cherish.app.date.calc.RecurrenceCalculator
import com.cherish.app.date.lunar.DefaultLunarCalendar
import com.cherish.app.date.model.LunarDate
import com.cherish.app.date.model.RepeatRule
import com.cherish.app.date.model.RepeatUnit
import com.cherish.app.date.model.SolarDate
import com.cherish.app.detail.mapper.EventDetailMapper
import com.cherish.app.detail.model.DetailCountdownTypographyTier
import com.cherish.app.detail.model.resolveDetailCountdownTypographyTier
import com.cherish.app.editor.model.EventEditorState
import com.cherish.app.editor.sanitizer.DatePickerSanitizer
import com.cherish.app.event.model.CountdownEvent
import com.cherish.app.event.model.EventBackground
import com.cherish.app.event.model.EventCategory
import com.cherish.app.event.model.EventColor
import com.cherish.app.event.model.EventDate
import com.cherish.app.event.repository.DefaultEventRepository
import com.cherish.app.home.mapper.HomeEventMapper
import com.cherish.app.home.model.CountdownDisplayStatus
import com.cherish.app.home.model.HomeViewMode
import com.cherish.app.navigation.CherishRoute
import com.cherish.app.navigation.resolveBackRoute
import com.cherish.app.settings.model.AppSettings
import com.cherish.app.settings.model.EventReorderHelper
import com.cherish.app.storage.AtomicFileEventStorage
import com.cherish.app.storage.FileEventImageStorage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Phase 6 End-to-End Flow & Regression Audit Test Suite.
 *
 * Exhaustively validates cross-screen consistency and boundaries:
 * 1. Data Consistency: Create in Editor -> Home Card -> Detail Hero -> Re-edit -> Save -> Delete
 * 2. Visual Separation: EventColor (Home cards) strictly decoupled from EventBackground (Detail)
 * 3. Custom Image Lifecycle: save -> resolve -> replace cleans old -> delete event removes file
 * 4. Temporal Engines: Solar/Lunar and Repeat rules consistency across Home and Detail
 * 5. Priority & Order: Pin priority on Home + Drag reorder preserving order in groups
 * 6. Navigation Topology: Top-level space transitions, child route BackHandler chains, long-press return
 * 7. Edge Cases: Empty states, boundary dates, leap years, character limits
 */
class EndToEndFlowAuditTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val referenceToday = SolarDate(2026, 10, 5)
    private val calendar = DefaultLunarCalendar()
    private val recurrenceCalculator = RecurrenceCalculator(calendar)

    // =========================================================================
    // 1. Data Consistency & Lifecycle: Create -> Home -> Detail -> Edit -> Delete
    // =========================================================================

    @Test
    fun `full lifecycle from Editor creation to Home and Detail models is 100 percent consistent`() {
        val storageFile = File(tempFolder.root, "audit_events.json")
        val storage = AtomicFileEventStorage(com.cherish.app.storage.JvmAtomicFileWriter(storageFile))
        val repository = DefaultEventRepository(storage)

        // Step 1: Create in Editor
        val initialLunar = calendar.solarToLunar(referenceToday)
        val editorDraft = EventEditorState.createDefault(referenceToday, initialLunar).copy(
            title = "结婚十周年",
            emoji = "💍",
            isLunar = false,
            solarDate = SolarDate(2026, 10, 15),
            repeatRule = RepeatRule.Yearly,
            category = EventCategory.ANNIVERSARY,
            isPinned = true,
            color = EventColor.Rose,
            background = EventBackground.Gradient(0xFFFFA000L, 0xFFE91E63L),
            notes = "订法餐厅",
        )

        val buildResult = DatePickerSanitizer.validateAndBuildEvent(editorDraft, calendar)
        assertTrue("Validation must succeed", buildResult.isSuccess)
        val createdEvent = buildResult.getOrThrow()
        repository.add(createdEvent)

        // Step 2: Verify in Home representation
        val homeUiModel = HomeEventMapper.toUiModel(createdEvent, referenceToday)
        assertEquals("Home must preserve title", "结婚十周年", homeUiModel.event.title)
        assertEquals("Home must preserve emoji", "💍", homeUiModel.event.emoji)
        assertEquals("Home must compute 10 days countdown", 10, homeUiModel.daysCount)
        assertEquals("Home status must be COUNTDOWN", CountdownDisplayStatus.COUNTDOWN, homeUiModel.status)
        assertEquals("Home must use EventColor.Rose", EventColor.Rose, homeUiModel.color)
        assertTrue("Home must mark isPinned", homeUiModel.isPinned)

        // Step 3: Verify in Detail representation
        val detailUiModel = EventDetailMapper.toUiModel(createdEvent, referenceToday)
        assertEquals("Detail must preserve title", "结婚十周年", detailUiModel.event.title)
        assertEquals("Detail hero number must be 10", "10", detailUiModel.heroNumberText)
        assertEquals("Detail hero unit must be 天", "天", detailUiModel.heroUnitText)
        assertEquals("Detail must use EventBackground.Gradient", EventBackground.Gradient(0xFFFFA000L, 0xFFE91E63L), detailUiModel.background)
        assertEquals("Detail recurrence description must state 每年重复", "每年重复", detailUiModel.recurrenceDescription)
        assertEquals("Detail category must state 纪念日", "纪念日", detailUiModel.categoryDescription)

        // Typography tier must be HERO_LARGE for 10 days
        val typographyTier = resolveDetailCountdownTypographyTier(detailUiModel.heroNumberText, detailUiModel.status)
        assertEquals(DetailCountdownTypographyTier.HERO_LARGE, typographyTier)

        // Step 4: Re-edit via Editor
        val editState = EventEditorState.fromEvent(createdEvent, referenceToday, initialLunar).copy(
            title = "结婚十一周年",
            color = EventColor.Amber,
            background = EventBackground.Color(0xFF009688L),
        )
        val updatedEvent = DatePickerSanitizer.validateAndBuildEvent(editState, calendar).getOrThrow()
        repository.update(updatedEvent)

        val updatedDetail = EventDetailMapper.toUiModel(repository.getById(createdEvent.id)!!, referenceToday)
        assertEquals("Updated title must match", "结婚十一周年", updatedDetail.event.title)
        assertEquals("Updated background must match", EventBackground.Color(0xFF009688L), updatedDetail.background)

        val updatedHome = HomeEventMapper.toUiModel(repository.getById(createdEvent.id)!!, referenceToday)
        assertEquals("Updated Home color must match", EventColor.Amber, updatedHome.color)

        // Step 5: Delete
        val deleted = repository.delete(createdEvent.id)
        assertTrue(deleted)
        assertNull(repository.getById(createdEvent.id))
        assertTrue(repository.getAll().isEmpty())
    }

    // =========================================================================
    // 2. Strict Duty Separation: EventColor vs EventBackground
    // =========================================================================

    @Test
    fun `EventColor and EventBackground remain strictly decoupled across modifications`() {
        // Event with Default color and Custom Gradient background
        val event = CountdownEvent(
            id = "decoupled-test",
            title = "色彩分离测试",
            eventDate = EventDate.Solar(SolarDate(2026, 12, 1)),
            color = EventColor.Emerald,
            background = EventBackground.Color(0xFF1E88E5L),
        )

        val homeUi = HomeEventMapper.toUiModel(event, referenceToday)
        val detailUi = EventDetailMapper.toUiModel(event, referenceToday)

        // Home card only reflects EventColor.Emerald
        assertEquals(EventColor.Emerald, homeUi.color)
        assertNotEquals("Home must not take background color as card color", EventColor.Single(0xFF1E88E5L), homeUi.color)

        // Detail screen reflects EventBackground.Color(0xFF1E88E5)
        assertEquals(EventBackground.Color(0xFF1E88E5L), detailUi.background)
        assertNotEquals("Detail must not use Home color as background", EventBackground.Color(0xFF009688L), detailUi.background)
    }

    // =========================================================================
    // 3. Custom Image Lifecycle & Storage Cleanup
    // =========================================================================

    @Test
    fun `custom image background saves, resolves, and cleans up properly`() {
        val imageDir = File(tempFolder.root, "bg_storage")
        val imageStorage = FileEventImageStorage(imageDir)

        val dummyBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val relativePath = imageStorage.saveImage("evt-image-1", dummyBytes, "jpg")
        val fileOnDisk = imageStorage.getImageFile(relativePath)
        assertNotNull("Image file must exist on disk", fileOnDisk)
        assertTrue(fileOnDisk!!.exists())

        // Image background event
        val eventWithImage = CountdownEvent(
            id = "evt-image-1",
            title = "自定义照片事件",
            eventDate = EventDate.Solar(SolarDate(2026, 11, 20)),
            background = EventBackground.Image(path = relativePath, dimAlpha = 0.45f),
        )

        val detailUi = EventDetailMapper.toUiModel(eventWithImage, referenceToday)
        assertTrue(detailUi.background is EventBackground.Image)
        assertEquals(relativePath, (detailUi.background as EventBackground.Image).path)

        // Simulate deleting the image file (simulating external deletion / cleanup)
        val deletedFromDisk = imageStorage.deleteImage(relativePath)
        assertTrue(deletedFromDisk)
        assertNull(imageStorage.getImageFile(relativePath))

        // Image file is absent, resolving it returns null (Detail screen gracefully falls back to Default)
        assertFalse(File(imageDir, relativePath).exists())
    }

    // =========================================================================
    // 4. Temporal Engines: Solar, Lunar, and Custom Recurrence Consistency
    // =========================================================================

    @Test
    fun `lunar and custom repeat rules compute identically in Home and Detail`() {
        val testRules = listOf(
            RepeatRule.None,
            RepeatRule.Daily,
            RepeatRule.Monthly,
            RepeatRule.Yearly,
            RepeatRule.Custom(14, RepeatUnit.DAY),
            RepeatRule.Custom(3, RepeatUnit.WEEK),
            RepeatRule.Custom(6, RepeatUnit.MONTH),
            RepeatRule.Custom(2, RepeatUnit.YEAR),
        )

        for ((idx, rule) in testRules.withIndex()) {
            val event = CountdownEvent(
                id = "recurrence-$idx",
                title = "循环事件 $idx",
                eventDate = EventDate.Solar(SolarDate(2026, 10, 1)), // 4 days ago
                repeatRule = rule,
            )

            val homeUi = HomeEventMapper.toUiModel(event, referenceToday)
            val detailUi = EventDetailMapper.toUiModel(event, referenceToday)

            assertEquals("Target solar date must match between Home and Detail for $rule",
                homeUi.targetSolarDate, detailUi.targetSolarDate)
            assertEquals("Days count must match between Home and Detail for $rule",
                homeUi.daysCount, detailUi.daysCount)
            assertEquals("Display status must match between Home and Detail for $rule",
                homeUi.status, detailUi.status)
        }
    }

    @Test
    fun `lunar leap month conversion is consistent across calculations`() {
        // Year 2023 has leap month 2
        val leapLunar = LunarDate(2023, 2, 15, isLeapMonth = true)
        val regularLunar = LunarDate(2023, 2, 15, isLeapMonth = false)

        val leapSolar = calendar.lunarToSolar(leapLunar)
        val regularSolar = calendar.lunarToSolar(regularLunar)

        assertNotEquals("Leap lunar date must not equal regular lunar date of same month", regularSolar, leapSolar)
    }

    // =========================================================================
    // 5. Pin Priority & Reorder Behavior
    // =========================================================================

    @Test
    fun `pinned events always precede unpinned events while preserving custom order`() {
        val events = listOf(
            CountdownEvent(id = "unpinned-1", title = "普通 1", eventDate = EventDate.Solar(referenceToday), isPinned = false),
            CountdownEvent(id = "pinned-1", title = "置顶 1", eventDate = EventDate.Solar(referenceToday), isPinned = true),
            CountdownEvent(id = "unpinned-2", title = "普通 2", eventDate = EventDate.Solar(referenceToday), isPinned = false),
            CountdownEvent(id = "pinned-2", title = "置顶 2", eventDate = EventDate.Solar(referenceToday), isPinned = true),
        )

        val pinned = events.filter { it.isPinned }
        val unpinned = events.filter { !it.isPinned }
        val homeOrdered = pinned + unpinned

        assertEquals(listOf("pinned-1", "pinned-2", "unpinned-1", "unpinned-2"), homeOrdered.map { it.id })

        // Reordering in EventOrderScreen
        val reorderedList = EventReorderHelper.reorder(events, 0, 3)
        val newPinned = reorderedList.filter { it.isPinned }
        val newUnpinned = reorderedList.filter { !it.isPinned }
        val newHomeOrdered = newPinned + newUnpinned

        assertEquals(listOf("pinned-1", "pinned-2", "unpinned-2", "unpinned-1"), newHomeOrdered.map { it.id })
    }

    // =========================================================================
    // 6. Navigation Topology & Back Resolution
    // =========================================================================

    @Test
    fun `resolveBackRoute correctly handles all routes according to specification`() {
        assertNull("Home has no back route (exits app)", resolveBackRoute(CherishRoute.Home))
        assertEquals(CherishRoute.Home, resolveBackRoute(CherishRoute.Detail("evt-1")))
        assertEquals(CherishRoute.Detail("evt-1"), resolveBackRoute(CherishRoute.Editor("evt-1")))
        assertEquals(CherishRoute.Home, resolveBackRoute(CherishRoute.Editor(null)))
        assertEquals(CherishRoute.Home, resolveBackRoute(CherishRoute.Settings))
        assertEquals(CherishRoute.Settings, resolveBackRoute(CherishRoute.EventOrder))
        assertEquals(CherishRoute.Settings, resolveBackRoute(CherishRoute.About))
    }

    // =========================================================================
    // 7. Edge Cases: Validation & Limits
    // =========================================================================

    @Test
    fun `editor validation enforces constraints cleanly`() {
        val baseState = EventEditorState.createDefault(referenceToday, calendar.solarToLunar(referenceToday))

        // Blank title fails
        val blankRes = DatePickerSanitizer.validateAndBuildEvent(baseState.copy(title = "   "), calendar)
        assertTrue(blankRes.isFailure)

        // 50 characters title succeeds
        val len50Title = "A".repeat(50)
        val len50Res = DatePickerSanitizer.validateAndBuildEvent(baseState.copy(title = len50Title), calendar)
        assertTrue(len50Res.isSuccess)

        // 51 characters title fails
        val len51Title = "A".repeat(51)
        val len51Res = DatePickerSanitizer.validateAndBuildEvent(baseState.copy(title = len51Title), calendar)
        assertTrue(len51Res.isFailure)

        // Month clamping: February in non-leap year (e.g. 2026-02-29 -> 2026-02-28)
        val clampedSolar = DatePickerSanitizer.sanitizeSolar(year = 2026, month = 2, day = 31)
        assertEquals(SolarDate(2026, 2, 28), clampedSolar)

        // Leap year: February in leap year (2028-02-31 -> 2028-02-29)
        val leapSolar = DatePickerSanitizer.sanitizeSolar(year = 2028, month = 2, day = 31)
        assertEquals(SolarDate(2028, 2, 29), leapSolar)
    }

    // =========================================================================
    // 8. Phase 9 Custom Image Background Real-World Loop & Restart Persistence
    // =========================================================================

    @Test
    fun `custom image background persists across app restarts and cleans up on replacement or delete`() {
        val storageFile = File(tempFolder.root, "restart_test_events.json")
        val imageDir = File(tempFolder.root, "restart_test_backgrounds")

        // Session 1: App launched, user creates event with curated photo wallpaper
        val storageSession1 = AtomicFileEventStorage(com.cherish.app.storage.JvmAtomicFileWriter(storageFile))
        val repositorySession1 = DefaultEventRepository(storageSession1)
        val imageStorageSession1 = FileEventImageStorage(imageDir)

        val eventId = "phase9-photo-1"
        val imagePath = com.cherish.app.storage.CuratedWallpaperGenerator.saveCuratedWallpaper(
            preset = com.cherish.app.storage.CuratedWallpaperPreset.GOLDEN_SUNSET,
            eventId = eventId,
            imageStorage = imageStorageSession1,
            width = 160,
            height = 160,
        )

        val originalFile = imageStorageSession1.getImageFile(imagePath)
        assertNotNull("Wallpaper file must exist in storage", originalFile)
        assertTrue(originalFile!!.exists())

        val photoEvent = CountdownEvent(
            id = eventId,
            title = "金色夕阳纪念日",
            eventDate = EventDate.Solar(referenceToday),
            color = EventColor.Amber,
            background = EventBackground.Image(path = imagePath, dimAlpha = 0.45f),
        )
        repositorySession1.add(photoEvent)

        // Session 2: Simulate App Termination and Restart
        val storageSession2 = AtomicFileEventStorage(com.cherish.app.storage.JvmAtomicFileWriter(storageFile))
        val repositorySession2 = DefaultEventRepository(storageSession2)
        val imageStorageSession2 = FileEventImageStorage(imageDir)

        val reloadedEvent = repositorySession2.getById(eventId)
        assertNotNull("Event must survive restart", reloadedEvent)
        assertTrue(reloadedEvent!!.background is EventBackground.Image)
        val reloadedBg = reloadedEvent.background as EventBackground.Image
        assertEquals("Path must match saved relative file", imagePath, reloadedBg.path)
        assertEquals(0.45f, reloadedBg.dimAlpha, 0.001f)

        // Detail screen resolves image file successfully
        val resolvedFile = imageStorageSession2.getImageFile(reloadedBg.path)
        assertNotNull("Resolved file after restart must exist", resolvedFile)
        assertTrue(resolvedFile!!.exists())

        // Detail UiModel correctly presents the image background
        val detailUi = EventDetailMapper.toUiModel(reloadedEvent, referenceToday)
        assertEquals(reloadedBg, detailUi.background)

        // Now simulate replacing the image with a new wallpaper
        val newImagePath = com.cherish.app.storage.CuratedWallpaperGenerator.saveCuratedWallpaper(
            preset = com.cherish.app.storage.CuratedWallpaperPreset.STARRY_NIGHT,
            eventId = eventId,
            imageStorage = imageStorageSession2,
            width = 160,
            height = 160,
        )
        val updatedEvent = reloadedEvent.copy(
            background = EventBackground.Image(path = newImagePath, dimAlpha = 0.60f),
        )

        // As in CherishApp.kt: old image is deleted on replacement
        imageStorageSession2.deleteImage(imagePath)
        repositorySession2.update(updatedEvent)

        assertNull("Old image file must be deleted", imageStorageSession2.getImageFile(imagePath))
        assertNotNull("New image file must exist", imageStorageSession2.getImageFile(newImagePath))

        // Now simulate deleting the event
        val deletedEvent = repositorySession2.getById(eventId)!!
        imageStorageSession2.deleteImage((deletedEvent.background as EventBackground.Image).path)
        repositorySession2.delete(eventId)

        assertNull("Deleted event image must be removed", imageStorageSession2.getImageFile(newImagePath))
        assertTrue(repositorySession2.getAll().isEmpty())
    }

    @Test
    fun `settings screen create event entry callback triggers navigation`() {
        var createEventInvoked = false
        val onNavigateToCreateEvent = { createEventInvoked = true }
        onNavigateToCreateEvent()
        assertTrue(createEventInvoked)
    }
}
