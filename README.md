# jchat
jchat is a peer to peer chat application written in Java.
You can chat and also host files.

## Demo Video

https://github.com/user-attachments/assets/3e39ec14-1552-4a4b-963b-c081769b9528



## How to run:
If you only want to run 1 client (e.g. when hosting a real peer) you can run the jar file like: `java -jar jchat.jar`

However, if you want to fully test this program out on one machine, you must use the docker image, as how the networking works. The docker image can be found at: https://hub.docker.com/r/angrypig555/jchat

To run it, use the docker compose file found in src/docker OR if you want the bleeding edge version, run the `devtest.sh` in the root of the git repo, but it is not guaranteed to work.
You do not need to download any libraries or dependencies if you are running the docker version. If you are using the native jar, you need java 21.

### What does it run on?
Due to it being written in java, it technically can run on any platform. It has only been tested on linux. If it doesn't work on your platform, try out the docker version.

### AI Notice
Some small amounts AI was used with help in the making of this project.

## Why over bittorrent?
Why should you use this over bittorrent? Jchat can run on any device that can run java which, is nearly every device.
Jchat is also very stable which means it can barely crash.
Jchat uses hashing of every chunk to ensure the integrity of the file.

## How does it work?
### The chat part
We send a header when initiating a connection to the peer, which confirms the version and everything else.
Then we exchange public keys to ensure encryption.
After that, you can chat normally.
Every message is wrapped in base 64, so the message is ensured to be the same that was sent.

### The router
The router runs on a completely seperate port and thread from the normal chat part.
The router has a table of ip addresses and nicknames of every known peer.
This table is exchanged between every peer you connect to.
First, there is an initial handshake, then the table is sent over.
The router asks for the table of peers every 20 seconds when there aren't any active connections to the router.

### The file sharing
JChat file sharing works similarly to bittorrent but uses TCP instead of UDP.
First we hash the file that we want to share and then wait for connection. On the other side, we get the hash of the file, and get the list of known peers off of the router.
We then contact every peer in that arraylist until we find one that responds and has the hash. If it does not have the hash, we skip that peer but if it isn't responding it gets taken off the peer list.

### The protocol
Jchat uses a custom protocol built on top of TCP.
The handshake looks like this: `JCHATVX.X` (where X stands for the version).
This is the handshake header and also the message header.
This is wrapped into base 64 and sent over the network.
On the other side, the peer decodes the base 64 and checks if the header is the same.
If it is not the same, the peer closes the connection as there may be a version mismatch, or simply another program connected.
After all of this is done, public keys are exchanged so we can ensure encryption.
Jchat uses Google Tink for encryption, and we send the aead hybrid public key.
After the keys are sent, the nicknames are exchanged and the two peers are ready to chat.
In case the message cannot be decoded after it is sent, the client gets alerted.
The router protocol uses the same handshake as the normal chatting one, but it does not wrap every message in that header.
First we check handshakes, then send the data, but after the data is sent the connection is closed so oter people can connect as well.
There is no hashing or encryption in the router.
For filesharing, we also use the standard messaging handshake. For filesharing, every chunk is hashed and then sent to the peer who checks the hash and reconstructs the file.
Everything that is encoded in base 64 is wrapped in a header that looks like this: `JCHATB64`
In case the decoding of the base 64 message fails, we always return null.
