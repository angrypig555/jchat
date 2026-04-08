package dev.brny;

public class Protocol {
    final static String name_header = "JCHAT";
    final static String version = "V0.3";
    final static String end = "\n\n";

    final static String header = name_header + version;

    final static String version_mismatch = "VER_MISMATCH";
}

// V0.1 - Beta protocol, no usernames
// V0.2 - Second iteration of the beta protocol, now with usernames