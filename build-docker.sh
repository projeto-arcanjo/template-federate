#! /bin/sh

mvn clean package

docker ps -a | awk '{ print $1,$2 }' | grep projetoarcanjo/template:1.0 | awk '{print $1 }' | xargs -I {} docker rm -f {}
docker rmi projetoarcanjo/template:1.0

docker build --tag=projetoarcanjo/template:1.0 --rm=true .

