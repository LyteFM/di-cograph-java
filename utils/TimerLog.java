package dicograph.utils;

import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Fynn Leitow on 23.12.17.
 */
public class TimerLog {

    private Stopwatch totalTime;
    private Stopwatch interimTime;
    private Logger log;
    private Level level;

    public TimerLog(Logger logger, Level lv){
        totalTime = Stopwatch.createStarted();
        interimTime = Stopwatch.createStarted();
        log = logger;
        level = lv;
    }

    public void logTime(String name){
        totalTime.stop();
        interimTime.stop();
        String msg1 = String.format("*** Elapsed time for %s: %s", name, interimTime.toString());
        String msg2 = String.format("*** Total time after %s: %s", name, totalTime.toString());
        interimTime.reset();
        log.log(level,msg1);
        log.log(level,msg2);

        interimTime.start();
        totalTime.start();
    }

    public long elapsedSeconds(){
        return totalTime.elapsed(TimeUnit.SECONDS);
    }

}
