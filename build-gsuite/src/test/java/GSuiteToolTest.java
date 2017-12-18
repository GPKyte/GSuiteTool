import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;

/**
 * The test class for the GSuiteTool class
 *
 * @author Gavin Kyte
 * @version (1.2 - 10.18.2017)
 */
public class GSuiteToolTest {
    /**
     * Default constructor for class GSuiteToolTest
     */
    public GSuiteToolTest() {}

    private static final String testDataPath =
        GSuiteToolTest.class
            .getResource("/testData.csv")
            .toString()
            .substring(5);

    @Test
    public void testNoFile() throws IOException {
        String name = new Object(){}.getClass().getEnclosingMethod().getName();
        System.out.println("<< Results of "+name+" >>");
        try {
            GSuiteTool.main(new String[] {"--path", "/notAFile"});
        } catch (IOException io) {
            System.out.println(io);
        }
    }
    @Test
    public void testEmptyFile() throws IOException {
        String name = new Object(){}.getClass().getEnclosingMethod().getName();
        System.out.println("<< Results of "+name+" >>");
        String emptyFile = GSuiteToolTest.class
            .getResource("/empty.csv")
            .toString()
            .substring(5);
        GSuiteTool.main(new String[] {"--path", emptyFile});
    }
    @Test
    public void testTestMethod() throws IOException {
        String name = new Object(){}.getClass().getEnclosingMethod().getName();
        System.out.println("<< Results of "+name+" >>");
        GSuiteTool.main(new String[] {"--test", "--verbose", "--debug", "--path", testDataPath});
    }
    @Test
    public void testCreateUsers() throws IOException {
        String name = new Object(){}.getClass().getEnclosingMethod().getName();
        System.out.println("<< Results of "+name+" >>");
        GSuiteTool.main(new String[] {"--create", "--verbose", "--debug", "--path", testDataPath});
    }
    @Test
    public void testAddMembers() throws IOException {
        String name = new Object(){}.getClass().getEnclosingMethod().getName();
        System.out.println("<< Results of "+name+" >>");
        GSuiteTool.main(new String[] {"--add", "--verbose", "--debug", "--path", testDataPath});
    }
    @Test
    public void testUpdateTitle() throws IOException {
        String name = new Object(){}.getClass().getEnclosingMethod().getName();
        System.out.println("<< Results of "+name+" >>");
        GSuiteTool.main(new String[] {"--update", "--verbose", "--debug", "--path", testDataPath});
    }
    @Test
    public void testListUsers() throws IOException {
        String name = new Object(){}.getClass().getEnclosingMethod().getName();
        System.out.println("<< Results of "+name+" >>");
        GSuiteTool.main(new String[] {"--list-users", "--verbose", "--debug"});
    }
    @Test
    public void testListGroups() throws IOException {
        String name = new Object(){}.getClass().getEnclosingMethod().getName();
        System.out.println("<< Results of "+name+" >>");
        GSuiteTool.main(new String[] {"--list-groups", "--verbose", "--debug"});
    }
    @Test
    public void testDry() throws IOException {
        String name = new Object(){}.getClass().getEnclosingMethod().getName();
        System.out.println("<< Results of "+name+" >>");
        GSuiteTool.main(new String[] {"-a", "-c", "-u", "-lu", "-lg", "-v", "-d", "-p", testDataPath, "--dry"});
    }
}
