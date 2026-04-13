# jchat
jchat is a peer to peer chat application written in Java.
You can chat and also host files.
## How to run:
If you only want to run 1 client (e.g. when hosting a real peer) you can run the jar file like: `java -jar jchat.jar`

However, if you want to fully test this program out on one machine, you must use the docker image, as how the networking works. The docker image can be found at: https://hub.docker.com/r/angrypig555/jchat

To run it, use the docker compose file found in src/docker
