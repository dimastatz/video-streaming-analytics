docker rm -vf $(docker ps -aq)
docker build --tag encoder .
docker run -it -p 8181:5000 encoder