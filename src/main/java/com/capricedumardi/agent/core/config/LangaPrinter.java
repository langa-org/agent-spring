package com.capricedumardi.agent.core.config;

public class LangaPrinter {

    private static AgentConfig agentConfig;

    public static boolean isDebugEnabled() {
        if (agentConfig == null) agentConfig = ConfigLoader.getConfigInstance();
        return agentConfig.isDebugMode();
    }

    public static void printError(String message) {
        System.err.println("===== LANGA SPRING AGENT ERROR =======");
        System.err.println(message);
        System.err.println("===============================");
    }

    public static void printConditionalError(String s) {
        if (isDebugEnabled()) {
            printError(s);
        }
    }

    public static void printWarning(String message) {
        System.out.println("===== LANGA SPRING AGENT WANRING =====");
        System.out.println(message);
        System.out.println("===============================");
    }

    public static void printTrace(String message) {
        if (isDebugEnabled()) {
            System.out.println("===== LANGA SPRING AGENT DEBUG =======");
            System.out.println(message);
            System.out.println("===============================");
        }
    }

    public static void agentStarting() {
        System.out.println("========================================");
        System.out.println("  Langa Spring Agent Starting");
        System.out.println("========================================");
    }


    public static void agentInitializationComplete() {
        System.out.println("========================================");
        System.out.println("  Langa Spring Agent Initialization Complete");
        System.out.println("========================================");
    }


}
