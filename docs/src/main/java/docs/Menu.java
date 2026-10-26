package docs;

import java.util.List;

import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

@DataMapping(value = "menu")
public record Menu(List<MenuItem> items) {

    public record MenuItem(
            String title,
            String path,
            String icon,
            String section,
            List<ChildItem> children) {
    }

    public record ChildItem(
            String title,
            String path) {
    }
}
