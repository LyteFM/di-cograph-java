package dicograph.utils;

import com.google.common.base.Stopwatch;

import java.util.logging.Logger;

/**
 * Created by Fynn Leitow on 23.12.17.
 */
public class TimerLog {

    private Stopwatch totalTime;
    private Stopwatch interimTime;
    private Logger log;
    private boolean info;

    public TimerLog(Logger logger, boolean isInfo){
        totalTime = Stopwatch.createStarted();
        interimTime = Stopwatch.createStarted();
        log = logger;
        info = isInfo;
    }

    public void logTime(String name){
        totalTime.stop();
        interimTime.stop();
        String msg1 = String.format("*** Elapsed time for %s: %s", name, interimTime.toString());
        String msg2 = String.format("*** Total time after %s: %s", name, totalTime.toString());
        interimTime.reset();
        if(info){
            log.info(msg1);
            log.info(msg2);
        } else {
            log.fine(msg1);
            log.fine(msg2);
        }
        interimTime.start();
        totalTime.start();
    }
}
