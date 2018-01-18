package dicograph.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Created by Fynn Leitow on 30.10.17. Code taken from:
 * https://stackoverflow.com/questions/194765/how-do-i-get-java-logging-output-to-appear-on-a-single-line
 */
public class VerySimpleFormatter extends Formatter {

    private final static String format = "{0,date} {0,time}";
    private final Object[] args = new Object[1];
    private final Date dat = new Date();
    private MessageFormat formatter;
    private final boolean useClassname;
    private final boolean useDate;

    public VerySimpleFormatter() {
        this(false, false);
    }

    private VerySimpleFormatter(boolean useDate, boolean useClassname) {
        super();
        this.useDate = useDate;
        this.useClassname = useClassname;
    }

    /**
     * Format the given LogRecord.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record) {

        StringBuilder sb = new StringBuilder();

        // Minimize memory allocations here.
        dat.setTime(record.getMillis());
        args[0] = dat;


        // Date and time
        if (useDate) {
            StringBuffer text = new StringBuffer();
            if (formatter == null) {
                formatter = new MessageFormat(format);
            }
            formatter.format(args, text, null);
            sb.append(text);
            sb.append(" ");
        }


        // Class name
        if (useClassname) {
            if (record.getSourceClassName() != null) {
                sb.append(record.getSourceClassName());
            } else {
                sb.append(record.getLoggerName());
            }
        }

        // Method name
        if (record.getSourceMethodName() != null) {
            sb.append(" ");
            sb.append(record.getSourceMethodName());
        }
        sb.append(" - "); // lineSeparator


        String message = formatMessage(record);

        // Level
        sb.append(record.getLevel().getName()); // want info, not information...
        sb.append(": ");

        // Indent - the more serious, the more indented.
        //sb.append( String.format("% ""s") );
        int iOffset = (1000 - record.getLevel().intValue()) / 100;
        for (int i = 0; i < iOffset; i++) {
            sb.append(" ");
        }


        sb.append(message);
        String lineSeparator = "\n";
        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }
        return sb.toString();
    }
}
