package com.farm.portal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(FarmUiTest.ScreenshotOnFailure.class)
class FarmUiTest {

    @LocalServerPort int port;
    static WebDriver driver;

    @BeforeAll
    static void start() {
        ChromeOptions o = new ChromeOptions();
        o.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1280,900");
        driver = new ChromeDriver(o);
    }

    @AfterAll
    static void stop() { driver.quit(); }

    private String url(String path) { return "http://localhost:" + port + path; }

    private boolean waitForText(String text) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void addBatch(String name, String farm) {
        driver.get(url("/produce/new"));
        driver.findElement(By.id("name")).sendKeys(name);
        driver.findElement(By.id("farm")).sendKeys(farm);
        driver.findElement(By.id("harvestDate")).sendKeys("2026-10-01");
        driver.findElement(By.id("save")).click();
    }

    @Test
    void addRecord() {
        addBatch("Tomato", "Farm A");
        assertTrue(waitForText("Tomato"));
    }

    @Test
    void searchRecord() {
        addBatch("Mango", "Farm B");
        driver.get(url("/produce?q=Mango"));
        assertTrue(driver.getPageSource().contains("Farm B"));
    }

    @Test
    void statusWorkflow() {
        addBatch("Onion", "Farm C");
        driver.get(url("/produce?q=Onion"));
        new Select(driver.findElement(By.name("role"))).selectByVisibleText("DISTRIBUTOR");
        driver.findElement(By.className("advance")).click();
        assertTrue(waitForText("IN_TRANSIT"));
    }

    @Test
    void dashboardShowsTotals() {
        driver.get(url("/dashboard"));
        assertTrue(driver.getPageSource().contains("Total batches"));
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