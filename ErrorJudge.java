import java.io.FileWriter;
import java.io.IOException;

public class ErrorJudge {
    private MidItem compunit;

    public ErrorJudge(MidItem compunit) {
        this.compunit = compunit;
    }

    public void print_comp() throws IOException {
        FileWriter writer = new FileWriter("output.txt");
        writer.write(compunit.toString());
        writer.flush();
        writer.close();
    }
}
