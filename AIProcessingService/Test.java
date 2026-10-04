import org.springframework.ai.document.Document;
import java.lang.reflect.Method;
public class Test {
    public static void main(String[] args) throws Exception {
        for(Method m : Document.class.getMethods()) {
            System.out.println(m.getName());
        }
    }
}
