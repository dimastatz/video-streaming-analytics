docker rm -vf $(docker ps -aq)
docker build --tag gateway .
docker run -it -p 8181:5000 gateway