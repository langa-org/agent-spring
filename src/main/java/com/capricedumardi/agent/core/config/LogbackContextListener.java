package com.capricedumardi.agent.core.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;

public class LogbackContextListener implements LoggerContextListener {
    private final Consumer<LoggerContext> loggerContextConsumer;

    public LogbackContextListener(Consumer<LoggerContext> loggerContextConsumer) {
        this.loggerContextConsumer = loggerContextConsumer;
    }

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onStart(LoggerContext loggerContext) {
        LangaPrinter.printTrace("LogbackContextListener started.");
        loggerContextConsumer.accept(loggerContext);
    }

    @Override
    public void onReset(LoggerContext loggerContext) {
        LangaPrinter.printTrace("LogbackContextListener reset.");
        loggerContextConsumer.accept(loggerContext);
    }

    @Override
    public void onStop(LoggerContext loggerContext) {
        LangaPrinter.printTrace("LogbackContextListener stopped.");
        // Rebind immediat si Logback a bascule vers un nouveau contexte
        LoggerContext currentContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        if (currentContext != loggerContext) {
            LangaPrinter.printTrace("LogbackContextListener detected context switch, rebinding now.");
            loggerContextConsumer.accept(currentContext);
        }
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
        LangaPrinter.printTrace("LogbackContextListener level changed.");
        loggerContextConsumer.accept(logger.getLoggerContext());
    }


}
