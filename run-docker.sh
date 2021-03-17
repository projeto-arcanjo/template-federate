#! /bin/sh

docker run --name template --hostname=template --network arcanjo \
    -e FEDERATION_NAME=ArcanjoFederation \
    -e FEDERATE_NAME=TemplateFederate \
	-e FIRST_COLLECT=2 \
	-e COLLECT_INTERVAL=3 \
	-v /etc/localtime:/etc/localtime:ro \
	-p 36008:8080 \
	-d projetoarcanjo/template:1.0	



