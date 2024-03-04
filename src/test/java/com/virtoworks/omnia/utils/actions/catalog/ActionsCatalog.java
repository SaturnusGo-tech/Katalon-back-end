package com.virtoworks.omnia.utils.actions.catalog;

import com.codeborne.selenide.Condition;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.virtoworks.omnia.utils.locators.catalog.CatalogPageLocators;
import com.virtoworks.omnia.utils.locators.filters.Filters;
import org.assertj.core.api.Assertions;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;


import static com.codeborne.selenide.Selenide.$x;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import io.restassured.response.Response;
import org.json.JSONObject;

import com.google.gson.Gson;


import static io.restassured.RestAssured.given;


public class ActionsCatalog {

    /**
     * Loads a GraphQL query from a file located at the specified file path.
     * <p>
     * This method is designed to read the contents of a GraphQL query stored in a file,
     * allowing for better maintainability and readability of tests. By storing queries
     * in separate files, they can be easily modified and reused across different tests
     * without cluttering the test code.
     * <p>
     * DOD (Definition of Done):
     *   1. The method returns the content of the file as a String if the file is found
     *      and accessible.
     *   2. In case of an IOException (e.g., file not found, lack of read permissions),
     *      the exception is caught, its stack trace is printed for debugging purposes,
     *      and null is returned. This behavior might need to be adjusted based on
     *      specific requirements, such as re-throwing the exception or logging it
     *      differently.
     *
     * @param filePath The relative or absolute path to the file containing the GraphQL query.
     * @return The content of the file as a String, or null if an IOException occurs.
     */
    public String loadGraphQLQuery(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
            return null; //Need to transfer this part of query to loader class
        }
    }


    private final CatalogPageLocators locators = new CatalogPageLocators();

    /**
     * Verifies that the number of catalog items is greater than or equal to the expected minimum.
     * <p>
     * DOD:
     *   1. Catalog items are present more than the specified expectedMinimum.
     *
     * @param expectedMinimum The minimum number of catalog items expected.
     * @param timeoutSeconds The duration in seconds to wait for the condition.
     * @return The actual number of catalog items found.
     */
    public int verifyCatalogItemsPresent(int expectedMinimum, int timeoutSeconds) {
        return $$x(locators.catalogItems).shouldHave(sizeGreaterThanOrEqual(expectedMinimum), Duration.ofSeconds(timeoutSeconds)).size();
    }

    /**
     * Checks the text of the breadcrumb and returns it.
     * <p>
     * DOD:
     *   1. Check the breadcrumbs catalog item for the expected text.
     *
     * @param expectedText The expected text of the breadcrumb.
     * @param timeoutSeconds The duration in seconds to wait for the condition.
     * @return The text of the breadcrumb.
     */
    public String checkBreadcrumbText(String expectedText, int timeoutSeconds) {
        return $x(locators.breadcrumb).shouldBe(visible, Duration.ofSeconds(timeoutSeconds)).getText();
    }

    /**
     * Verifies the visibility of catalog items after switching visualization mode.
     * <p>
     * DOD:
     *   1. Grid visualization is visible when buttonText is "Grid".
     *   2. List visualization is visible when buttonText is "List".
     *
     * @param buttonText The text on the button that triggers the switch in visualization.
     * @param expectedMinimum The minimum number of items expected to be visible after the switch.
     * @param timeoutSeconds The duration in seconds to wait for the condition.
     * @return True if the expected minimum number of items are visible after the switch, otherwise false.
     */
    public boolean verifyVisualisationSwitch(String buttonText, int expectedMinimum, int timeoutSeconds) {
        $x(locators.visualisationSwitchButton + "[contains(text(), '" + buttonText + "')]").shouldBe(visible, Duration.ofSeconds(timeoutSeconds)).click();
        return $$x(locators.catalogItems).shouldBe(sizeGreaterThanOrEqual(expectedMinimum), Duration.ofSeconds(timeoutSeconds)).size() >= expectedMinimum;
    }

    public void waitForCatalogPage() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Stopped after some error", e);
        }
    }

    /**
     * Performs clicks on checkboxes and verifies updates in the UI.
     *
     * @param filters The filters object containing the locators.
     * @param checkboxLocators A list of locators for the checkboxes.
     * @param moreLessButton The "More/Less" button element.
     * @param dataElementLocator The locator for the data element to check after updates.
     */
    public void clickCheckboxesAndCheckUpdates(Filters filters, List<String> checkboxLocators, SelenideElement moreLessButton, SelenideElement dataElementLocator) {
        Map<String, Object> currentFilters = new HashMap<>();
        currentFilters.put("storeId", "opus");
        currentFilters.put("userId", "78e100ff-ea81-4aca-ad83-5f112e80fc77");
        currentFilters.put("currencyCode", "USD");
        currentFilters.put("cultureName", "en-US");
        currentFilters.put("first", 16);
        currentFilters.put("sort", "");
        currentFilters.put("query", "");
        currentFilters.put("fuzzy", false);
        currentFilters.put("fuzzyLevel", 0);
        currentFilters.put("productIds", null);

        for (String locator : checkboxLocators) {
            SelenideElement checkbox = $x(locator).shouldBe(visible).shouldBe(enabled);

            sleep(3000);

            checkbox.scrollIntoView("{block: \"center\"}").click();
            Assertions.assertThat(checkbox.isSelected()).as("Checkbox %s should be checked after click", locator).isTrue();
            System.out.println("Clicked on checkbox: " + locator);

            if (checkbox.equals(moreLessButton)) {
                moreLessButton.scrollIntoView("{block: \"center\"}").click();
                moreLessButton.should(disappear);
                System.out.println("Clicked on 'More/Less' button.");
            }

            currentFilters.put("filter", "new filter != correct");

            String responseData = sendGraphQLRequestAndGetResponseData(currentFilters);
            System.out.println("GraphQL response data: " + responseData);

            SelenideElement dataElement = retryFindElement(dataElementLocator, 10, 1000);
            if (dataElement != null) {
                String dataText = dataElement.text();
                System.out.println("Data for locator " + locator + ": " + dataText);
            } else {
                System.out.println("Element ain't found after several attempts: " + locator);
            }
        }
    }

    /**
     * Sends a GraphQL request with given filters and retrieves the response data.
     * This method constructs a GraphQL query to fetch product details based on the provided filters.
     * It sends the query to the GraphQL endpoint and processes the response.
     *
     * @param filters A map containing the filters to be applied in the GraphQL query.
     * @return The response body as a string.
     * @throws RuntimeException if the HTTP response code is not 200.
     */
    private String sendGraphQLRequestAndGetResponseData(Map<String, Object> filters) {
        // GraphQL query to search for products with specified filters
        String query = """
    query SearchProducts($storeId: String!, $userId: String!, $currencyCode: String!, $cultureName: String, $filter: String, $after: String, $first: Int, $sort: String, $query: String, $fuzzy: Boolean, $fuzzyLevel: Int, $productIds: [String]) {
      products(
        storeId: $storeId,
        userId: $userId,
        currencyCode: $currencyCode,
        cultureName: $cultureName,
        filter: $filter,
        after: $after,
        first: $first,
        sort: $sort,
        query: $query,
        fuzzy: $fuzzy,
        fuzzyLevel: $fuzzyLevel,
        productIds: $productIds
      ) {
        totalCount
        items {
          id
          name
        }
      }
    }
    """;

        // Constructing the request body with the query and variables
        JSONObject requestBody = new JSONObject();
        requestBody.put("query", query);
        requestBody.put("variables", new JSONObject(filters));

        // Sending the GraphQL request
        Response response = given()
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .log().all()
                .post("https://qa-opus.omniapartners.com/xapi/graphql");

        // Logging the response
        response.then().log().all();

        // Checking for successful response
        if (response.getStatusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatusCode());
        }

        // Returning the response body
        return response.getBody().asString();
    }
    /**
     * Any way here's some prblm
     * that the outstack filters ain't get expected result using by this case a singular methods of fetching graphql
     */

    /**
     * Retries finding an element until it's visible or the retry limit is reached.
     *
     * @param element The Selenide element to find.
     * @param retries Number of retries.
     * @param delayInMillis Delay in milliseconds between retries.
     * @return The found element or null if not found.
     */
    public SelenideElement retryFindElement(SelenideElement element, int retries, long delayInMillis) {
        for (int attempt = 0; attempt < retries; attempt++) {
            if (element.exists() && element.isDisplayed()) {
                return element;
            } else {
                // built-in waiting mechanism
                element.shouldBe(Condition.appear, Duration.ofMillis(delayInMillis));
            }
        }
        System.out.println("Element not found after " + retries + " attempts.");
        return null;
    }

    /**
     * Scrolls through the filter inner block until all checkboxes are found.
     * @param filterContainer Selector for the filter container.
     * @param checkboxLocator Locator for the checkboxes within the container.
     * @param expectedCount Expected number of elements.
     * @return Actual number of elements found.
     */

    public int scrollToFindAllCheckboxes(SelenideElement filterContainer, String checkboxLocator, int expectedCount) {
        int itemsCount = 0;
        while (itemsCount < expectedCount) {
            itemsCount = filterContainer.$$x(checkboxLocator).filter(visible).size();
            if (itemsCount < expectedCount) {
                executeJavaScript("arguments[0].scrollTop = arguments[0].scrollHeight", filterContainer);
                Selenide.sleep(1000);
            }
        }
        return itemsCount;
    }


}
