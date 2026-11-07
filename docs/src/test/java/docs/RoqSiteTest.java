package docs;

import io.quarkiverse.roq.testing.RoqAndRoll;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

/**
 * Validates that the Roq site generates without errors and that the generated
 * pages contain what they should: source snippets included via
 * {@code {snippet:insert(...)}}, page links rewritten to URLs, images with
 * root-path-safe relative paths, and the sitemap/search index in place.
 */
@QuarkusTest
@RoqAndRoll
public class RoqSiteTest {

    @Test
    public void siteGeneratesWithoutErrors() {
        // Site generation and template validation happen during test setup:
        // if this test runs, every page rendered without Qute errors.
    }

    @Test
    public void homepageRenders() {
        when().get("/").then().statusCode(200)
                .body(containsString("Quarkus LangChain4j Workshop"));
    }

    @Test
    public void wholeFileSnippetsAreIncluded() {
        when().get("/section-1/step-01/").then().statusCode(200)
                .body(containsString("@RegisterAiService"));
    }

    @Test
    public void taggedSnippetsAreIncluded() {
        // extracted between the -8<- [start:easy-rag] / [end:easy-rag] markers
        when().get("/section-1/step-05/").then().statusCode(200)
                .body(containsString("quarkus-langchain4j-easy-rag"));
    }

    @Test
    public void lineRangeSnippetsAreIncluded() {
        // extracted from TripPlanStore.java lines 3-39
        when().get("/section-3/step-05/").then().statusCode(200)
                .body(containsString("import java.time.Duration;"));
    }

    @Test
    public void pageLinksAreRewrittenToUrls() {
        when().get("/section-1/step-01/").then().statusCode(200)
                .body(containsString("href=\"../step-02/\""));
    }

    @Test
    public void imagesUseRelativePaths() {
        // absolute /images/... paths would break under the GitHub Pages root path
        when().get("/section-1/step-01/").then().statusCode(200)
                .body(containsString("src=\"../../images/"));
    }

    @Test
    public void sitemapAndSearchIndexAreGenerated() {
        when().get("/sitemap.xml").then().statusCode(200)
                .body(containsString("<urlset"));
        when().get("/search-index.json").then().statusCode(200);
    }
}
