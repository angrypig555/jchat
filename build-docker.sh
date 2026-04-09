echo "Building jar"
./gradlew shadowJar
cp build/libs/Jchat-*.*-SNAPSHOT-all.jar src/docker/jchat.jar
echo "Building container"
sudo docker build src/docker -t angrypig555/jchat
