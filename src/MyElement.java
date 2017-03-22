import org.eclipse.jdt.internal.compiler.impl.Constant;

/**
 * Created by M on 2017/3/20.
 */
public class MyElement {
    private String name;
    private String format;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFormat() {

        if (format.contains("string"))
            return Costant.STRING;
        else if (format.contains("dimension"))
            return Costant.DIMENSION;
        else if (format.contains("color"))
            return Costant.COLOR;
        else if (format.contains("boolean"))
            return Costant.BOOL;
        else if (format.contains("integer"))
            return Costant.INT;
        else if (format.contains("float"))
            return Costant.FLOAT;
        else return 0;


    }

    public void setFormat(String format) {
        this.format = format;
    }
}
