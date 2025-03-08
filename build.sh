repo=lr-acr-registry.cn-shanghai.cr.aliyuncs.com/devops-apac-mgmt
image=spark-3
tag=3.3.0

livyRepo=lr-acr-registry.cn-shanghai.cr.aliyuncs.com/devops-apac-mgmt
livyImageName=livy-custom
livyImageVersion=0.1.40

if ! [ -d "./ext/dist/" ]; then
    echo "./ext/dist/ not exists , mkdir -p ./ext/dist/"
    mkdir -p ./ext/dist/
fi

rm -rf ./ext/dist/*
if ! [ -e "./ext/spark/spark-3.3.0-bin-hadoop3.tgz" ]; then
    echo "spark-3.3.0-bin-hadoop3.tgz not exists in ./ext/spark/  Downloading....."
    cd ./ext/spark/
    wget https://archive.apache.org/dist/spark/spark-3.3.0/spark-3.3.0-bin-hadoop3.tgz
    cd ../../
fi

tar -xzvf ./ext/spark/spark-3.3.0-bin-hadoop3.tgz -C ./ext/dist/


cd ./ext/spark/s3/
cat aws-java-sdk-bundle-1.11.1026.jar* > aws-java-sdk-bundle-1.11.1026.jar
cd ../../../
cp ./ext/spark/s3/*.jar ./ext/dist/spark-3.3.0-bin-hadoop3/jars/
sh ./ext/dist/spark-3.3.0-bin-hadoop3/bin/docker-image-tool.sh -r ${repo} -t ${tag}   build

docker tag ${repo}/spark:${tag} ${repo}/${image}:${tag}
docker push ${repo}/${image}:${tag}

mvn package -DskipTests

docker build -t ${livyRepo}/${livyImageName}:${livyImageVersion} .
docker push ${livyRepo}/${livyImageName}:${livyImageVersion}

echo "build success:"
echo "spark image >> " ${repo}/${image}:${tag}
echo "livy image >> " ${livyRepo}/${livyImageName}:${livyImageVersion}
