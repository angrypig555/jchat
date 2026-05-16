#!/bin/bash
./build-docker.sh
cd src/docker

tmux new-session -d -s foo_session 'docker compose run jchat'

tmux split-window -h -t foo_session 'docker compose run jchat'

tmux attach-session -t foo_session

#sudo docker compose run jchat