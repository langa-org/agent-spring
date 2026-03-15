package com.capricedumardi.agent.core.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class LangaPrinterTest {

    @Test
    void printError_printsToStdErr() {
        PrintStream original = System.err;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setErr(new PrintStream(baos));

            LangaPrinter.printError("test error message");

            String output = baos.toString();
            assertTrue(output.contains("LANGA SPRING AGENT ERROR"));
            assertTrue(output.contains("test error message"));
        } finally {
            System.setErr(original);
        }
    }

    @Test
    void printWarning_printsToStdOut() {
        PrintStream original = System.out;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos));

            LangaPrinter.printWarning("test warning");

            String output = baos.toString();
            assertTrue(output.contains("LANGA SPRING AGENT WANRING"));
            assertTrue(output.contains("test warning"));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void agentStarting_printsHeader() {
        PrintStream original = System.out;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos));

            LangaPrinter.agentStarting();

            String output = baos.toString();
            assertTrue(output.contains("Langa Spring Agent Starting"));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void agentInitializationComplete_printsFooter() {
        PrintStream original = System.out;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos));

            LangaPrinter.agentInitializationComplete();

            String output = baos.toString();
            assertTrue(output.contains("Initialization Complete"));
        } finally {
            System.setOut(original);
        }
    }
}
