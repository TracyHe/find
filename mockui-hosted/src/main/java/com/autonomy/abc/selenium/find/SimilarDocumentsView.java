package com.autonomy.abc.selenium.find;

import com.autonomy.abc.selenium.util.ParametrizedFactory;
import com.hp.autonomy.frontend.selenium.util.AppPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SimilarDocumentsView implements AppPage {
    private WebDriver driver;
    private WebElement container;

    //TODO find somewhere more suitable for this
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, MMMMM dd, yyyy kk:mm a");

    private SimilarDocumentsView(WebDriver driver) {
        this.driver = driver;
        this.container = driver.findElement(By.className("suggest-service-view-container"));
    }

    public WebElement backButton() {
        return findElement(By.className("service-view-back-button"));
    }

    public String getTitle() {
        return title().getText();
    }

    public WebElement seedLink() {
        return title().findElement(By.tagName("a"));
    }

    private WebElement title() {
        return findElement(By.cssSelector(".results-message-container h4"));
    }

    /**
     * Y in 'X to Y of Z'
     */
    public int getVisibleResultsCount() {
        return Integer.valueOf(findElement(By.className("current-results-number")).getText());
    }

    /**
     * Z in 'X to Y of Z'
     */
    public int getTotalResults() {
        return Integer.valueOf(findElement(By.className("total-results-number")).getText());
    }

    public List<FindSearchResult> getResults(){
        List<FindSearchResult> results = new ArrayList<>();
        for(WebElement result : findElements(By.cssSelector("[data-rel='results']"))){
            results.add(new FindSearchResult(result, getDriver()));
        }
        return results;
    }

    public List<FindSearchResult> getResults(int maxResults) {
        List<FindSearchResult> results = getResults();
        return results.subList(0, Math.min(maxResults, results.size()));
    }

    public FindSearchResult getResult(int i) {
        return new FindSearchResult(findElement(By.cssSelector(".main-results-container:nth-of-type(" + i + ")")), getDriver());
    }

    @Override
    public void waitForLoad() {
        new WebDriverWait(getDriver(), 20)
                .withMessage("loading similar results view")
                .until(ExpectedConditions.visibilityOf(backButton()));
    }

    private WebElement findElement(By locator) {
        return container.findElement(locator);
    }

    private List<WebElement> findElements(By locator) {
        return container.findElements(locator);
    }

    private WebDriver getDriver() {
        return driver;
    }

    public WebElement resultsContainer() {
        return findElement(By.className("results-container"));
    }

    public void sortByDate() {
        sortBy(1);
    }

    public void sortByRelevance() {
        sortBy(2);
    }

    private void sortBy(int dropdownRow){
        findElement(By.className("current-search-sort")).click();
        findElement(By.cssSelector(".search-results-sort li:nth-child(" + dropdownRow + ")")).click();
    }

    static class Factory implements ParametrizedFactory<WebDriver, SimilarDocumentsView> {
        public SimilarDocumentsView create(WebDriver context) {
            return new SimilarDocumentsView(context);
        }
    }
}
