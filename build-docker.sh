echo "Building jar"
./gradlew jar
cp build/libs/Jchat-*.*-SNAPSHOT.jar src/docker/jchat.jar
echo "Building container"
sudo docker build src/docker -t angrypig555/jchat
