package de.fabisosc.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/** Laesst alle laufenden Strikes jeden Tick weiterlaufen. */
public final class StrikeRunner {

    private final List<Strike> strikes = new ArrayList<>();
    private final Consumer<RuntimeException> errorLog;

    public StrikeRunner(Consumer<RuntimeException> errorLog) {
        this.errorLog = errorLog;
    }

    public void launch(Strike strike) {
        strikes.add(strike);
    }

    public void tick() {
        Iterator<Strike> it = strikes.iterator();
        while (it.hasNext()) {
            Strike strike = it.next();
            boolean done;
            try {
                done = strike.tick();
            } catch (RuntimeException e) {
                errorLog.accept(e);
                done = true;
            }
            if (done) {
                strike.cleanup();
                it.remove();
            }
        }
    }

    public void stopAll() {
        for (Strike strike : strikes) strike.cleanup();
        strikes.clear();
    }
}
