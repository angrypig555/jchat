#!/bin/sh

CONTAINER_IP=$(hostname -i | awk '{print $1}')
echo "jchat docker container"
echo "jchat is a p2p application, use only on internal networks, no encryption"
echo "ip: $CONTAINER_IP"

exec java -jar /app/app.jar