package dev.brny;

public class Protocol {
    final static String name_header = "JCHAT";
    final static String version = "V0.4";
    final static String end = "\n\n";

    final static String header = name_header + version;

    final static String version_mismatch = "VER_MISMATCH";

    final static String router_header = "JCHROUTER" + version;
    final static String b64_header = "JCHATB64";
}

// PROTOCOL LAYOUT:
// Normal messages:
// JCHATVXinsertmessageJCHATVX
// Name | Version | Message | Name | Version
// Router messages:
// JCHROUTERVXYarrayofpeeripsNICKarrayofpeernicksJCHROUTERVX
// Name | Version | Size of arrays | IP's | Nicknames | Name | Version
// Encoded messages:
// JCHATB64base_64_msg
// Header | Base64


// V0.1 - Beta protocol, no usernames
// V0.2 - Second iteration of the beta protocol, now with usernames
// V0.3 - Third iteration of the beta protocol, now with peer discovery
// V0.4 - Fourth iteration of the beta protocol, uses base 64 encoding for message integrity
// Ports used:
// 5400 - Port for connecting and communicating with peers
// 5401 - Port used for the router