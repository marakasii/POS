package studying;

import com.google.inject.Guice;
import java.io.IOException;
import studying.pos.config.PosModule;
import studying.pos.infrastructure.console.ConsoleApplication;

/** Starts the console application through the real dependency container. */
public final class Main {
    private Main() {
    }

    /**
     * Starts a console session; EOF and exit terminate it normally.
     * @param args command-line arguments, currently unused
     * @throws IOException when the console input cannot be read
     */
    public static void main(final String[] args) throws IOException {
        Guice.createInjector(new PosModule())
                .getInstance(ConsoleApplication.class).run();
    }
}
