package docs;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.alerts.AlertsExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;

/**
 * Registers the commonmark extensions that {@code quarkus-qute-web-markdown}
 * normally provides on its own.
 * <p>
 * The library produces them as {@code @DefaultBean}s, and a default bean is
 * dropped as soon as the application declares any other bean of the same type.
 * {@link HighlightLinesExtension} is such a bean, so without these producers
 * the site loses tables, callout boxes ({@code > [!NOTE]}), autolinks and the
 * heading ids the page table of contents links to.
 */
@Dependent
public class MarkdownExtensions {

    @Produces
    public Extension tables() {
        return TablesExtension.create();
    }

    @Produces
    public Extension alerts() {
        return AlertsExtension.create();
    }

    @Produces
    public Extension autolink() {
        return AutolinkExtension.create();
    }

    @Produces
    public Extension headingAnchor() {
        return HeadingAnchorExtension.create();
    }
}
