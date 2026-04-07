package dev.brny;

public class Protocol {
    final static String name_header = "JCHAT";
    final static String version = "V0.1";
    final static String end = "\n\n";

    final static String header = name_header + version;

    final static String version_mismatch = "VER_MISMATCH\n\n";
}
