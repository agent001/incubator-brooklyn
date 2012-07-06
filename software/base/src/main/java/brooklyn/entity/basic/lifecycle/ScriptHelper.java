package brooklyn.entity.basic.lifecycle;

import brooklyn.util.GroovyJavaMethods;
import brooklyn.util.RuntimeInterruptedException;
import brooklyn.util.mutex.WithMutexes;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

public class ScriptHelper {

    public static final Logger log = LoggerFactory.getLogger(ScriptHelper.class);

    protected final ScriptRunner runner;
    public final String summary;

    public final ScriptPart header = new ScriptPart(this);
    public final ScriptPart body = new ScriptPart(this);
    public final ScriptPart footer = new ScriptPart(this);

    protected Predicate<Integer> resultCodeCheck = Predicates.alwaysTrue();
    protected Predicate<ScriptHelper> executionCheck = Predicates.alwaysTrue();

    public ScriptHelper(ScriptRunner runner, String summary) {
        this.runner = runner;
        this.summary = summary;
    }

    /**
     * Takes a closure which accepts this ScriptHelper and returns true or false
     * as to whether the script needs to run (or can throw error if desired)
     */
    public ScriptHelper executeIf(Closure c) {
        Predicate<ScriptHelper> predicate = GroovyJavaMethods.predicateFromClosure(c);
        return executeIf(predicate);
    }

    public ScriptHelper executeIf(Predicate<ScriptHelper> c) {
        executionCheck = c;
        return this;
    }

    public ScriptHelper skipIfBodyEmpty() {
        Predicate<ScriptHelper> p = new Predicate<ScriptHelper>() {
            @Override
            public boolean apply(ScriptHelper input) {
                return !input.body.isEmpty();
            }
        };

        return executeIf(p);
    }

    public ScriptHelper failIfBodyEmpty() {
        Predicate<ScriptHelper> p = new Predicate<ScriptHelper>() {
            @Override
            public boolean apply(ScriptHelper input) {
                if (input.body.isEmpty()) {
                    throw new IllegalStateException("body empty for " + summary);
                }
                return true;
            }
        };

        return executeIf(p);
    }

    public ScriptHelper failOnNonZeroResultCode(boolean val) {
        Predicate<Integer> onTrue = Predicates.equalTo(0);
        Predicate<Integer> onFalse = Predicates.alwaysTrue();
        requireResultCode(val ? onTrue : onFalse);
        return this;
    }

    public ScriptHelper failOnNonZeroResultCode() {
        return failOnNonZeroResultCode(true);
    }

    /**
     * Convenience for error-checking the result.
     * <p/>
     * Takes closure which accepts bash exit code (integer),
     * and returns false if it is invalid. Default is that this resultCodeCheck
     * closure always returns true (and the exit code is made available to the
     * caller if they care)
     */
    public ScriptHelper requireResultCode(Closure integerFilter) {
        Predicate<Integer> objectPredicate = GroovyJavaMethods.predicateFromClosure(integerFilter);
        return requireResultCode(objectPredicate);
    }

    public ScriptHelper requireResultCode(Predicate<Integer> integerFilter) {
        resultCodeCheck = integerFilter;
        return this;
    }

    protected Runnable mutexAcquire = new Runnable() {
        public void run() {
        }
    };

    protected Runnable mutexRelease = new Runnable() {
        public void run() {
        }
    };

    /**
     * indicates that the script should acquire the given mutexId on the given mutexSupport
     * and maintain it for the duration of script execution;
     * typically used to prevent parallel scripts from conflicting in access to a resource
     * (e.g. a folder, or a config file used by a process)
     */
    public ScriptHelper useMutex(final WithMutexes mutexSupport, final String mutexId, final String description) {
        mutexAcquire = new Runnable() {
            public void run() {
                try {
                    mutexSupport.acquireMutex(mutexId, description);
                } catch (InterruptedException e) {
                    throw new RuntimeInterruptedException(e);
                }
            }
        };

        mutexRelease = new Runnable() {
            public void run() {
                mutexSupport.releaseMutex(mutexId);
            }
        };

        return this;
    }

    public int execute() {
        if (!executionCheck.apply(this)) {
            return 0;
        }

        List<String> lines = getLines();
        if (log.isDebugEnabled()) {
            log.debug("executing: {} - {}", summary, lines);
        }
        int result;
        try {
            mutexAcquire.run();
            result = runner.execute(lines, summary);
        } catch (RuntimeInterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(format("execution failed, invocation error for %s", summary), e);
        } finally {
            mutexRelease.run();
        }
        if (log.isDebugEnabled()) {
            log.debug("finished executing: {} - result code {}", summary, result);
        }
        if (!resultCodeCheck.apply(result)) {
            throw new IllegalStateException(format("execution failed, invalid result %s for %s", result, summary));
        }
        return result;
    }

    public List<String> getLines() {
        List<String> result = new LinkedList<String>();
        result.addAll(header.lines);
        result.addAll(body.lines);
        result.addAll(footer.lines);
        return result;
    }
}


