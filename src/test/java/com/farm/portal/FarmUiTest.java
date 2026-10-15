package com.farm.portal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.*;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.openqa.selenium.support.ui.Select;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(FarmUiTest.ScreenshotOnFailure.class)
class FarmUiTest {

    @LocalServerPort int port;
    static WebDriver driver;

    // Starts a brand-new browser (no old cookies, no old login)
    private static void startBrowser() {
        if (driver != null) driver.quit();
        ChromeOptions o = new ChromeOptions();
        o.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1280,900");
        driver = new ChromeDriver(o);
    }

    @AfterAll
    static void stop() { if (driver != null) driver.quit(); }

    private String url(String path) { return "http://localhost:" + port + path; }

    private boolean waitForText(String text) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text));
            return true;
        } catch (TimeoutException e) {
            String page = driver.findElement(By.tagName("body")).getText().replace("\n", " | ");
            System.out.println(">>> WAIT FAILED for '" + text + "' at " + driver.getCurrentUrl());
            System.out.println(">>> PAGE TEXT: " + (page.length() > 500 ? page.substring(0, 500) : page));
            return false;
        }
    }

    // Waits until the button can be clicked, then clicks it with JavaScript
    private void safeClick(By locator) {
        WebElement el = new WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.elementToBeClickable(locator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    // Returns true when the browser has moved away from the given page
    private boolean waitUntilLeaves(String urlPart) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.not(ExpectedConditions.urlContains(urlPart)));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // Types a value and checks that the field really contains it (retries 3 times)
    private void fill(By locator, String value) {
        WebElement el = new WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.visibilityOfElementLocated(locator));
        for (int i = 0; i < 3; i++) {
            el.clear();
            el.sendKeys(value);
            if (value.equals(el.getAttribute("value"))) return;
        }
        // last resort: set the value with JavaScript
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", el, value);
    }

    // Opens a fresh browser and logs in with the given user
    private void login(String user, String pass) {
        startBrowser();
        driver.get(url("/login"));
        driver.findElement(By.id("username")).sendKeys(user);
        driver.findElement(By.id("password")).sendKeys(pass);
        safeClick(By.id("login"));
    }

    private void addBatch(String name, String farm) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            driver.get(url("/produce/new"));
            fill(By.id("name"), name);
            fill(By.id("farm"), farm);
            fill(By.id("harvestDate"), "2026-10-01");
            safeClick(By.id("save"));
            if (waitUntilLeaves("/produce/new")) return;
            Object info = ((JavascriptExecutor) driver).executeScript(
                "return 'name=' + document.getElementById('name').value"
                + " + ' | farm=' + document.getElementById('farm').value"
                + " + ' | date=' + document.getElementById('harvestDate').value"
                + " + ' | formValid=' + document.getElementById('save').form.checkValidity();");
            System.out.println(">>> SAVE DID NOT SUBMIT (attempt " + attempt + "): " + info);
        }
    }

    @Test
    void addRecord() {
        login("farmer", "farmer123");
        assertTrue(waitForText("Logged in as"));
        addBatch("Tomato", "Farm A");
        assertTrue(waitForText("Tomato"));
    }

    @Test
    void searchRecord() {
        login("farmer", "farmer123");
        assertTrue(waitForText("Logged in as"));
        addBatch("Mango", "Farm B");
        assertTrue(waitForText("Mango"));
        driver.get(url("/produce?q=Mango"));
        assertTrue(waitForText("Farm B"));
    }

    @Test
    void statusWorkflow() {
        login("farmer", "farmer123");
        assertTrue(waitForText("Logged in as"));
        addBatch("Onion", "Farm C");
        assertTrue(waitForText("Onion"));

        login("distributor", "distributor123");
        assertTrue(waitForText("Logged in as"));
        driver.get(url("/produce?q=Onion"));
        safeClick(By.className("advance"));
        assertTrue(waitForText("IN_TRANSIT"));
    }

    @Test
    void dashboardShowsTotals() {
        login("admin", "admin123");
        assertTrue(waitForText("Logged in as"));
        driver.get(url("/dashboard"));
        assertTrue(waitForText("Total batches"));
    }

    @Test
    void wrongPasswordIsRejected() {
        login("farmer", "wrong-password");
        assertTrue(waitForText("Invalid username or password"));
    }

    @Test
    void farmerCannotOpenDashboard() {
        login("farmer", "farmer123");
        assertTrue(waitForText("Logged in as"));
        driver.get(url("/dashboard"));
        assertTrue(waitForText("403"));
    }

    @Test
    void farmerHasNoAdvanceButton() {
        login("farmer", "farmer123");
        assertTrue(waitForText("Logged in as"));
        driver.get(url("/produce"));
        assertTrue(waitForText("Produce Batches"));
        assertTrue(driver.findElements(By.className("advance")).isEmpty());
    }
    @Test
    void adminCanCorrectStatus() {
        login("farmer", "farmer123");
        assertTrue(waitForText("Logged in as"));
        addBatch("Garlic", "Farm D");
        assertTrue(waitForText("Garlic"));

        login("admin", "admin123");
        assertTrue(waitForText("Logged in as"));
        // set IN_TRANSIT "by mistake", then correct it back to HARVESTED
        driver.get(url("/produce?q=Garlic"));
        new Select(driver.findElement(By.name("status"))).selectByValue("IN_TRANSIT");
        safeClick(By.className("set-status"));
        assertTrue(waitForText("to IN_TRANSIT"));

        driver.get(url("/produce?q=Garlic"));
        new Select(driver.findElement(By.name("status"))).selectByValue("HARVESTED");
        safeClick(By.className("set-status"));
        assertTrue(waitForText("from IN_TRANSIT to HARVESTED"));
    }

    @Test
    void onlyAdminSeesStatusCorrection() {
        login("distributor", "distributor123");
        assertTrue(waitForText("Logged in as"));
        driver.get(url("/produce"));
        assertTrue(waitForText("Produce Batches"));
        assertTrue(driver.findElements(By.className("set-status")).isEmpty());
    }
    
    // Takes a screenshot when a test fails -> target/screenshots/
    static class ScreenshotOnFailure implements TestWatcher {
        @Override
        public void testFailed(ExtensionContext ctx, Throwable cause) {
            try {
                File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Path dir = Path.of("target", "screenshots");
                Files.createDirectories(dir);
                String name = ctx.getDisplayName().replace("()", "") + ".png";
                Files.copy(src.toPath(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) { }
        }
    }
}