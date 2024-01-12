package util;

import java.util.Date;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.transaction.Transactional;
import model.Todo;

@ApplicationScoped
@Blocking //idk, fromage made it (Transactionnal on method should be enough)
public class Startup {
    /**
     * This method is executed at the start of your application
     *
     * @Transactional cause it publish to DB
     */
    @Transactional
    public void start(@Observes StartupEvent evt) {
        // in DEV mode we seed some data
        if(LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            Todo a = new Todo();
            a.task = "First item";
            a.persist();

            Todo b = new Todo();
            b.task = "Second item";
            b.completed = new Date();
            b.persist();
        }
    }
}
