docker rm $(docker stop $(docker ps -a -f name=data-grid -q))
docker rmi kimim/react-data-grid

docker build -t kimim/react-data-grid .
docker run -d -p 3000:3000 --name=data-grid kimim/react-data-grid
docker logs $(docker ps -f name=data-grid -q)
