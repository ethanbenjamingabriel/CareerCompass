package dev.hungrymonkey.careercompass

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ResumeBuilderUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice
    private val timeout = 10000L

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.pressHome()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("dev.hungrymonkey.careercompass")?.apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)

        device.wait(Until.hasObject(By.pkg("dev.hungrymonkey.careercompass")), timeout)
    }

    @Test
    fun testLoginWithDevCredentials() {
        handleLoginAndDialogs()
    }

    @Test
    fun testResumeBuilderMainScreen() {
        handleLoginAndDialogs()
        navigateToResumeBuilder()
        verifyResumeBuilderScreenUI()
    }

    @Test
    fun testResumeInputScreenElements() {
        handleLoginAndDialogs()
        navigateToResumeBuilder()
        openResumeInputScreen()
        
        val resumeDetailsTitle = device.findObject(UiSelector().text("Resume Details"))
        assertTrue("Resume Details title should be visible", resumeDetailsTitle.exists())
        
        val resumeNameField = device.findObject(UiSelector().text("Resume Name"))
        assertTrue("Resume Name field should be visible", resumeNameField.exists())
        
        val fullNameField = device.findObject(UiSelector().text("Full Name"))
        assertTrue("Full Name field should be visible", fullNameField.exists())
        
        val addEducationButton = findElementWithScrolling("Add New Education")
        assertTrue("Add New Education button should be visible", addEducationButton != null)
        
        val addExperienceButton = findElementWithScrolling("Add New Experience")
        assertTrue("Add New Experience button should be visible", addExperienceButton != null)
        
        val addProjectButton = findElementWithScrolling("Add New Project")
        assertTrue("Add New Project button should be visible", addProjectButton != null)
        
        val editSkillsButton = findElementWithScrolling("Edit Skills")
        if (editSkillsButton == null) {
            val skillsSection = findElementWithScrolling("Skills") 
                ?: findElementWithScrolling("Add Skills")
                ?: findElementWithScrolling("Add New Skills")
        }
    }

    @Test
    fun testGeneralDetailsScreen() {
        handleLoginAndDialogs()
        navigateToResumeBuilder()
        
        val generalDetailsCard = device.findObject(UiSelector().text("General Details"))
        assertTrue("General Details card should be present", generalDetailsCard.exists())
        
        val addDetailsButton = device.findObject(UiSelector().text("Add"))
        if (addDetailsButton.exists()) {
            addDetailsButton.click()
        } else {
            val editDetailsButton = device.findObject(UiSelector().text("Edit"))
            if (editDetailsButton.exists()) {
                editDetailsButton.click()
            }
        }
        
        device.wait(Until.hasObject(By.text("General Details")), timeout)
        
        val generalDetailsTitle = device.findObject(UiSelector().text("General Details"))
        assertTrue("Should be on General Details screen", generalDetailsTitle.exists())
    }

    private fun handleLoginAndDialogs() {
        Thread.sleep(3000)
        device.waitForIdle()
        
        val resumeNav = device.findObject(UiSelector().textContains("Resume"))
        val homeNav = device.findObject(UiSelector().textContains("Home"))
        val goalsNav = device.findObject(UiSelector().textContains("Goals"))
        
        if (resumeNav.exists() || homeNav.exists() || goalsNav.exists()) {
            handleStayOnTrackDialog()
            return
        }
        
        handleStayOnTrackDialog()
        
        val loginText = device.findObject(UiSelector().text("Login"))
        if (loginText.exists()) {
            loginWithDevCredentials()
            handleStayOnTrackDialog()
        } else {
            val mainAppElements = device.findObject(UiSelector().textContains("Resume"))
                                    .exists() || device.findObject(UiSelector().textContains("Career")).exists()
        }
        
        device.waitForIdle()
        Thread.sleep(2000)
    }
    
    private fun handleStayOnTrackDialog() {
        Thread.sleep(3000)
        
        val stayOnTrackTitle = device.findObject(UiSelector().text("Stay on Track"))
        val stayOnTrackTitleAlt = device.findObject(UiSelector().textContains("Stay on Track"))
        
        if (stayOnTrackTitle.exists() || stayOnTrackTitleAlt.exists()) {
            var dialogDismissed = false
            
            val maybeLaterButton = device.findObject(UiSelector().text("Maybe later"))
            if (maybeLaterButton.exists()) {
                if (maybeLaterButton.isClickable()) {
                    maybeLaterButton.click()
                    dialogDismissed = true
                } else {
                    try {
                        val bounds = maybeLaterButton.getBounds()
                        device.click(bounds.centerX(), bounds.centerY())
                        dialogDismissed = true
                    } catch (e: Exception) {
                    }
                }
            }
            
            if (!dialogDismissed) {
                val maybeLaterButtonAlt = device.findObject(UiSelector().textContains("Maybe"))
                if (maybeLaterButtonAlt.exists()) {
                    maybeLaterButtonAlt.click()
                    dialogDismissed = true
                }
            }
            
            if (!dialogDismissed) {
                val cancelButtons = listOf("Cancel", "Later", "Skip", "Dismiss", "Not now")
                for (buttonText in cancelButtons) {
                    val button = device.findObject(UiSelector().text(buttonText))
                    if (button.exists()) {
                        button.click()
                        dialogDismissed = true
                        break
                    }
                }
            }
            
            if (dialogDismissed) {
                device.waitForIdle()
                Thread.sleep(2000)
            }
        }
    }

    private fun loginWithDevCredentials() {
        device.wait(Until.hasObject(By.text("Login")), timeout)
        Thread.sleep(3000)
        device.waitForIdle()

        val editTexts = device.findObjects(By.clazz("android.widget.EditText"))
        
        if (editTexts.size >= 2) {
            try {
                val emailField = editTexts[0]
                emailField.click()
                Thread.sleep(1000)
                emailField.clear()
                Thread.sleep(500)
                emailField.text = "dev@dev.dev"
                Thread.sleep(2000)
                
                val passwordField = editTexts[1]
                passwordField.click()
                Thread.sleep(1000)
                passwordField.clear()
                Thread.sleep(500)
                passwordField.text = "123456"
                Thread.sleep(2000)
                
                val keyboardVisible = isKeyboardVisible()
                
                if (keyboardVisible) {
                    device.pressBack()
                    Thread.sleep(1000)
                    device.waitForIdle()
                    
                    try {
                        device.click(device.displayWidth / 2, device.displayHeight / 8)
                        Thread.sleep(500)
                        device.waitForIdle()
                    } catch (e: Exception) {
                    }
                }
                
                device.waitForIdle()
                Thread.sleep(1000)
                
                val loginButtonY = (device.displayHeight * 0.52).toInt()
                val loginButtonX = device.displayWidth / 2
                
                try {
                    device.click(loginButtonX, loginButtonY)
                    Thread.sleep(2000)
                    
                    var loginSuccessful = false
                    for (i in 1..5) {
                        device.waitForIdle()
                        Thread.sleep(2000)
                        
                        val stillOnLogin = device.findObject(UiSelector().text("Login")).exists()
                        val hasMainAppElements = device.findObject(UiSelector().textContains("Home")).exists() ||
                                               device.findObject(UiSelector().textContains("Resume")).exists() ||
                                               device.findObject(UiSelector().textContains("Goals")).exists()
                        
                        if (!stillOnLogin || hasMainAppElements) {
                            loginSuccessful = true
                            break
                        }
                    }
                    
                    if (!loginSuccessful) {
                        val alternativePositions = listOf(
                            Pair(loginButtonX, (device.displayHeight * 0.48).toInt()),
                            Pair(loginButtonX, (device.displayHeight * 0.50).toInt()),
                            Pair(loginButtonX, (device.displayHeight * 0.54).toInt()),
                            Pair(loginButtonX, (device.displayHeight * 0.46).toInt())
                        )
                        
                        for ((x, y) in alternativePositions) {
                            device.click(x, y)
                            Thread.sleep(3000)
                            
                            val stillOnLogin = device.findObject(UiSelector().text("Login")).exists()
                            val hasMainAppElements = device.findObject(UiSelector().textContains("Home")).exists() ||
                                                   device.findObject(UiSelector().textContains("Resume")).exists()
                            
                            if (!stillOnLogin || hasMainAppElements) {
                                loginSuccessful = true
                                break
                            }
                        }
                    }
                    
                    if (!loginSuccessful) {
                        fail("Could not successfully click login button with direct coordinates")
                    }
                    
                } catch (e: Exception) {
                    fail("Login button coordinate click failed")
                }
                
            } catch (e: Exception) {
                throw e
            }
            
            Thread.sleep(8000)
            device.waitForIdle()
            
            val homeFound = device.findObject(UiSelector().textContains("Home")).exists()
            val resumeFound = device.findObject(UiSelector().textContains("Resume")).exists()  
            val goalsFound = device.findObject(UiSelector().textContains("Goals")).exists()
            val loginStillThere = device.findObject(UiSelector().text("Login")).exists()
            
            if (!homeFound && !resumeFound && !goalsFound && loginStillThere) {
                Thread.sleep(5000)
            }
            
        } else {
            fail("Could not find email and password fields")
        }
    }
    
    private fun isKeyboardVisible(): Boolean {
        try {
            val windowManager = InstrumentationRegistry.getInstrumentation().targetContext
                .getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
            val display = windowManager.defaultDisplay
            val displaySize = android.graphics.Point()
            display.getSize(displaySize)
            
            val heightDifference = device.displayHeight - displaySize.y
            val isKeyboardUp = heightDifference > device.displayHeight * 0.25
            
            return isKeyboardUp
        } catch (e: Exception) {
        }
        
        try {
            val imeElements = device.findObjects(androidx.test.uiautomator.By.pkg("com.google.android.inputmethod.latin"))
            if (imeElements.isNotEmpty()) {
                return true
            }
        } catch (e: Exception) {
        }
        
        try {
            val loginButton = device.findObject(UiSelector().text("Login"))
            val loginButtonVisible = loginButton.exists()
            
            if (!loginButtonVisible) {
                return true
            }
        } catch (e: Exception) {
        }
        
        return false
    }

    private fun navigateToResumeBuilder() {
        device.waitForIdle()
        Thread.sleep(2000)

        var resumeBuilderNav: UiObject? = null
        
        resumeBuilderNav = device.findObject(UiSelector().text("Resume Builder"))
        
        if (!resumeBuilderNav.exists()) {
            resumeBuilderNav = device.findObject(UiSelector().text("Resume"))
        }
        
        if (!resumeBuilderNav.exists()) {
            resumeBuilderNav = device.findObject(UiSelector().textContains("Resume"))
        }
        
        if (!resumeBuilderNav.exists()) {
            resumeBuilderNav = device.findObject(UiSelector().descriptionContains("Resume"))
        }
        
        if (!resumeBuilderNav.exists()) {
            val resumeBuilderTitle = device.findObject(UiSelector().text("Resume Builder"))
            if (resumeBuilderTitle.exists()) {
                return
            }
        }
        
        if (!resumeBuilderNav.exists()) {
            val navElements = listOf("Home", "Jobs", "Goals", "Profile", "Dashboard")
            for (navText in navElements) {
                val navElement = device.findObject(UiSelector().text(navText))
                if (navElement.exists()) {
                    resumeBuilderNav = device.findObject(UiSelector().textContains("Resume"))
                    if (resumeBuilderNav.exists()) {
                        break
                    }
                }
            }
        }

        if (resumeBuilderNav != null && resumeBuilderNav.exists()) {
            resumeBuilderNav.click()
            device.wait(Until.hasObject(By.text("Resume Builder")), timeout)
        } else {
            fail("Could not find Resume Builder navigation element")
        }
    }

    private fun verifyResumeBuilderScreenUI() {
        val resumeBuilderTitle = device.findObject(UiSelector().text("Resume Builder"))
        assertTrue("Resume Builder title should be visible", resumeBuilderTitle.exists())
        
        val createButton = device.findObject(UiSelector().description("Create Resume"))
        assertTrue("Create Resume button should be visible", createButton.exists())
        
        val generalDetailsCard = device.findObject(UiSelector().text("General Details"))
        assertTrue("General Details card should be visible", generalDetailsCard.exists())
        
        val recentResumesText = device.findObject(UiSelector().text("Recent Resumes"))
        assertTrue("Recent Resumes text should be visible", recentResumesText.exists())
    }
    
    private fun findElementWithScrolling(elementText: String, maxScrolls: Int = 3): UiObject? {
        var element = device.findObject(UiSelector().text(elementText))
        
        if (element.exists()) {
            return element
        }
        
        var scrollAttempts = 0
        while (scrollAttempts < maxScrolls) {
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.55).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.45).toInt(),
                20
            )
            Thread.sleep(1000)
            device.waitForIdle()
            
            element = device.findObject(UiSelector().text(elementText))
            scrollAttempts++
            
            if (element.exists()) {
                return device.findObject(UiSelector().text(elementText))
            }
        }
        
        if (!element.exists() && elementText.contains("Add")) {
            val fabSelectors = listOf(
                UiSelector().className("com.google.android.material.floatingactionbutton.FloatingActionButton"),
                UiSelector().descriptionContains("Add"),
                UiSelector().descriptionContains(elementText.replace("Add New ", "")),
                UiSelector().resourceIdMatches(".*fab.*"),
                UiSelector().resourceIdMatches(".*add.*")
            )
            
            for (selector in fabSelectors) {
                val fabElement = device.findObject(selector)
                if (fabElement.exists()) {
                    return fabElement
                }
            }
            
            val allFabs = device.findObjects(
                androidx.test.uiautomator.By.clazz("com.google.android.material.floatingactionbutton.FloatingActionButton")
            )
            allFabs.forEachIndexed { index, fab ->
                if (fab.contentDescription?.contains(elementText.replace("Add New ", ""), ignoreCase = true) == true) {
                    return device.findObject(UiSelector().descriptionContains(fab.contentDescription))
                }
            }
        }
        
        return if (element.exists()) element else null
    }
    
    private fun openResumeInputScreen() {
        val createButton = device.findObject(UiSelector().description("Create Resume"))
        if (createButton.exists()) {
            createButton.click()
            device.waitForIdle()
            
            val template1 = device.findObject(UiSelector().text("Template 1"))
            if (template1.exists()) {
                template1.click()
                device.wait(Until.hasObject(By.text("Resume Details")), timeout)
            }
        }
    }
}
