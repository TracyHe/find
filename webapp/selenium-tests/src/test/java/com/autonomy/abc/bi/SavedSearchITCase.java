package com.autonomy.abc.bi;

import com.autonomy.abc.base.IdolFindTestBase;
import com.autonomy.abc.base.Role;
import com.autonomy.abc.selenium.error.Errors;
import com.autonomy.abc.selenium.find.FindService;
import com.autonomy.abc.selenium.find.IdolFindPage;
import com.autonomy.abc.selenium.find.application.BIIdolFind;
import com.autonomy.abc.selenium.find.application.BIIdolFindElementFactory;
import com.autonomy.abc.selenium.find.application.UserRole;
import com.autonomy.abc.selenium.find.bi.SunburstView;
import com.autonomy.abc.selenium.find.bi.TopicMapView;
import com.autonomy.abc.selenium.find.concepts.ConceptsPanel;
import com.autonomy.abc.selenium.find.filters.FilterPanel;
import com.autonomy.abc.selenium.find.numericWidgets.MainNumericWidget;
import com.autonomy.abc.selenium.find.numericWidgets.NumericWidgetService;
import com.autonomy.abc.selenium.find.save.*;
import com.autonomy.abc.selenium.find.results.ResultsView;
import com.autonomy.abc.selenium.query.Query;
import com.hp.autonomy.frontend.selenium.config.TestConfig;
import com.hp.autonomy.frontend.selenium.framework.logging.ResolvedBug;
import com.hp.autonomy.frontend.selenium.util.Waits;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

import static com.autonomy.abc.matchers.ErrorMatchers.isError;
import static com.hp.autonomy.frontend.selenium.framework.state.TestStateAssert.assertThat;
import static com.hp.autonomy.frontend.selenium.framework.state.TestStateAssert.verifyThat;
import static com.hp.autonomy.frontend.selenium.matchers.ElementMatchers.checked;
import static org.hamcrest.Matchers.*;

@Role(UserRole.BIFHI)
public class SavedSearchITCase extends IdolFindTestBase {
    private SearchTabBar searchTabBar;
    private FindService findService;
    private SavedSearchService saveService;
    private BIIdolFindElementFactory elementFactory;

    public SavedSearchITCase(final TestConfig config) {
        super(config);
    }

    private static Matcher<SearchTab> modified() {
        return ModifiedMatcher.INSTANCE;
    }

    @Before
    public void setUp() {
        findService = getApplication().findService();
        saveService = getApplication().savedSearchService();

        elementFactory = (BIIdolFindElementFactory) getElementFactory();
        elementFactory.getFindPage().goToListView();
        searchTabBar = elementFactory.getSearchTabBar();
    }

    @After
    public void tearDown() {
        saveService.deleteAll();
    }

    @Test
    @ResolvedBug("FIND-467")
    public void testCanSaveSearch() {
        findService.search("queen");

        saveService.saveCurrentAs("save me", SearchType.QUERY);

        final SearchTab currentTab = searchTabBar.currentTab();
        assertThat(currentTab.getTitle(), is("save me"));
        assertThat(currentTab.getTitle(), not(containsString("New Search")));
        assertThat(currentTab, not(modified()));
    }

    @Test
    public void testSnapshotSavedInNewTab() {
        findService.search("crocodile");

        saveService.saveCurrentAs("snap", SearchType.SNAPSHOT);

        final List<SearchTab> tabs = searchTabBar.tabs();
        assertThat(tabs, hasSize(2));
        assertThat(tabs.get(0), is(modified()));
        assertThat(tabs.get(0).getType(), is(SearchType.QUERY));
        assertThat(tabs.get(1), not(modified()));
        assertThat(tabs.get(1).getType(), is(SearchType.SNAPSHOT));
    }

    @Test
    public void testOpenSnapshotAsQuery() {
        findService.search("open");
        getElementFactory().getResultsPage().waitForResultsToLoad();

        saveService.saveCurrentAs("sesame", SearchType.SNAPSHOT);
        findService.search("no longer open");
        searchTabBar.switchTo("sesame");

        elementFactory.getSearchOptionsBar().openSnapshotAsQuery();

        assertThat(searchTabBar.currentTab().getTitle(), is("New Search"));
        assertThat(searchTabBar.currentTab().getType(), is(SearchType.QUERY));
        assertThat(searchTabBar.tab("sesame").getType(), is(SearchType.SNAPSHOT));
        assertThat(getElementFactory().getTopNavBar().getSearchBoxTerm(), is("open"));
    }

    @Test
    public void testDuplicateNamesPrevented() {
        findService.search("useless");
        saveService.saveCurrentAs("duplicate", SearchType.QUERY);
        saveService.openNewTab();
        getElementFactory().getResultsPage().waitForResultsToLoad();

        checkSavingDuplicateThrowsError("duplicate", SearchType.QUERY);
        checkSavingDuplicateThrowsError("duplicate", SearchType.SNAPSHOT);
    }

    private void checkSavingDuplicateThrowsError(final String searchName, final SearchType type) {
        Waits.loadOrFadeWait();
        final SearchOptionsBar options = saveService.nameSavedSearch(searchName, type);
        options.saveConfirmButton().click();
        assertThat(options.getSaveErrorMessage(), isError(Errors.Find.DUPLICATE_SEARCH));
        options.cancelSave();
    }

    @Test
    public void testSavedSearchVisibleInNewSession() {
        findService.search(new Query("live forever"));
        ResultsView results = getElementFactory().getResultsPage();
        results.waitForResultsToLoad();

        final FilterPanel filterPanel = getElementFactory().getFilterPanel();
        filterPanel.waitForParametricFields();

        final int index = filterPanel.nonZeroParamFieldContainer(0);
        filterPanel.parametricField(index).expand();
        filterPanel.checkboxForParametricValue(index, 0).check();

        results.waitForResultsToLoad();
        saveService.saveCurrentAs("oasis", SearchType.QUERY);

        final BIIdolFind other = new BIIdolFind();
        launchInNewSession(other);
        other.loginService().login(getConfig().getDefaultUser());
        other.findService().search("blur");

        final BIIdolFindElementFactory factory = other.elementFactory();
        factory.getSearchTabBar().switchTo("oasis");
        factory.getFilterPanel().waitForParametricFields();
        assertThat(factory.getFilterPanel().checkboxForParametricValue(index, 0), checked());
    }

    @Test
    @ResolvedBug("FIND-278")
    public void testCannotChangeParametricValuesInSnapshot() {
        findService.search("terrible");
        final String searchName = "horrible";

        saveService.saveCurrentAs(searchName, SearchType.SNAPSHOT);
        searchTabBar.switchTo(searchName);

        final IdolFindPage findPage = getElementFactory().getFindPage();
        findPage.goToSunburst();
        Waits.loadOrFadeWait();

        final SavedSearchPanel panel = new SavedSearchPanel(getDriver());
        final int originalCount = panel.resultCount();

        final SunburstView results = elementFactory.getSunburst();

        results.waitForSunburst();
        results.getIthSunburstSegment(1).click();
        results.waitForSunburst();

        verifyThat("Has not added filter", findPage.filterLabels(), hasSize(0));
        verifyThat("Same number of results", panel.resultCount(), is(originalCount));
    }

    @Test
    @ResolvedBug("FIND-284")
    public void testRenamingSnapshot() {
        findService.search("broken");

        final String originalName = "originalName";
        saveService.saveCurrentAs(originalName, SearchType.SNAPSHOT);
        searchTabBar.switchTo(originalName);

        final String newName = "newName";
        saveService.renameCurrentAs(newName);

        saveService.openNewTab();
        searchTabBar.switchTo(newName);
        verifyThat("Saved search has content", elementFactory.getTopicMap().topicMapVisible());
    }

    @Test
    @ResolvedBug("FIND-269")
    public void testSearchesWithNumericFilters() {
        final NumericWidgetService widgetService = getApplication().numericWidgetService();

        final MainNumericWidget mainGraph = widgetService.searchAndSelectNthGraph(1, "saint");
        mainGraph.clickAndDrag(100, mainGraph.graph());

        getElementFactory().getResultsPage().waitForResultsToLoad();
        saveService.saveCurrentAs("saaaaved", SearchType.QUERY);

        assertThat(searchTabBar.currentTab(), not(modified()));
    }

    // Checks that the saved-ness of the search respects the selected concepts
    @Test
    public void testSearchesWithConcepts() {
        elementFactory.getFindPage().goToTopicMap();

        final TopicMapView topicMap = elementFactory.getTopicMap();
        topicMap.waitForMapLoaded();

        // Select a concept and save the search
        final String selectedConcept = topicMap.clickClusterHeading();
        new WebDriverWait(getDriver(), 30L).withMessage("Buttons should become active").until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".service-view-container:not(.hide) .save-button:not(.disabled)")));
        saveService.saveCurrentAs("Conceptual Search", SearchType.QUERY);

        // Remove the selected concept
        final ConceptsPanel conceptsPanel = elementFactory.getConceptsPanel();
        conceptsPanel.removableConceptForHeader(selectedConcept).removeAndWait();

        assertThat(searchTabBar.currentTab(), is(modified()));
        assertThat(conceptsPanel.selectedConceptHeaders(), empty());

        // Reset the search
        saveService.resetCurrentQuery();

        assertThat(searchTabBar.currentTab(), not(modified()));
        final List<String> finalConceptHeaders = conceptsPanel.selectedConceptHeaders();
        assertThat(finalConceptHeaders, hasSize(1));
        assertThat(finalConceptHeaders, hasItem('"' + selectedConcept + '"'));
    }

    @Test
    @ResolvedBug("FIND-167")
    public void testCannotSaveSearchWithWhitespaceAsName() {
        findService.search("yolo");
        final SearchOptionsBar searchOptions = saveService.nameSavedSearch("   ", SearchType.QUERY);

        assertThat("Save button is disabled", !searchOptions.saveConfirmButton().isEnabled());
    }

    private static class ModifiedMatcher extends TypeSafeMatcher<SearchTab> {
        private static final Matcher<SearchTab> INSTANCE = new ModifiedMatcher();

        @Override
        protected boolean matchesSafely(final SearchTab searchTab) {
            return searchTab.isNew();
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("a modified tab");
        }
    }
}
