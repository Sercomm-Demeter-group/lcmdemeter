package com.sercomm.commons.util;

import java.io.File;
import java.util.Map;
import java.util.zip.Deflater;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class Log
{
    static {
        // initialize Logger root configuration 
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = loggerContext.getConfiguration();
        LoggerConfig rootLoggerConfig = configuration.getLoggerConfig("");
        rootLoggerConfig.setLevel(Level.ALL);
    }

    // then, create the logger instance
    private static final Logger logger = LogManager.getLogger(Log.class);

    public static void initialize(
            String fileIndex,
            String fileSize,
            Integer maxRollingCount)
    {
        // initialize appenders
        
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = loggerContext.getConfiguration();
        LoggerConfig rootLoggerConfig = configuration.getLoggerConfig("");

        // remove exist appenders
        for(Map.Entry<String, Appender> entry : rootLoggerConfig.getAppenders().entrySet())
        {
            rootLoggerConfig.removeAppender(entry.getKey());
        }

        final Layout<String> layout = createPatternLayout();

        // size-based
        SizeBasedTriggeringPolicy sizeBasedTriggeringPolicy = 
                SizeBasedTriggeringPolicy.createPolicy(fileSize);

        @SuppressWarnings("deprecation")
        RolloverStrategy strategy =
                DefaultRolloverStrategy.createStrategy(
                    maxRollingCount.toString(), 
                    "1", 
                    fileIndex, 
                    String.valueOf(Deflater.NO_COMPRESSION), 
                    null, 
                    false, 
                    configuration);
        
        // rolling file appender
        final RollingFileAppender rollingFileAppender = RollingFileAppender.newBuilder()
                .setName("SERCOMM.ROLLING.FILE.APPENDER")
                .setLayout(layout)
                .withFileName(File.separator + "tmp" + File.separator + fileIndex + ".log")
                .withFilePattern(File.separator + "tmp" + File.separator + fileIndex + ".%i.log")
                .withStrategy(strategy)
                .withPolicy(sizeBasedTriggeringPolicy)
                .withAppend(true)
                .withCreateOnDemand(false)
                .withBufferedIo(true)
                .withBufferSize(8192)
                .build();

        rollingFileAppender.start();

        rootLoggerConfig.addAppender(
            rollingFileAppender, 
            Level.ALL, 
            null);

        // console appender
        final ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
                .setName("SERCOMM.CONSOLE.APPENDER")
                .withBufferedIo(false)
                .setLayout(layout)
                .build();

        consoleAppender.start();

        rootLoggerConfig.addAppender(
            consoleAppender, 
            Level.ALL, 
            null);
    }
    
    private static PatternLayout createPatternLayout() 
    {
        return PatternLayout.newBuilder().withPattern(
            "[%-5p][%-d{MM-dd HH:mm:ss.SSS}] %C{1}::%M %m%n")
                .build();
    }

    public static org.apache.logging.log4j.Logger write()
    {
        return Log.logger;
    }
}
