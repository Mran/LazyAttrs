import java.util.List;

public class ElementWrapper {
    private String styleName;
    private List<MyElement> elements;

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public List<MyElement> getElements() {
        return elements;
    }

    public void setElements(List<MyElement> elements) {
        this.elements = elements;
    }
}
