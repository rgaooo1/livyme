sh ./build.sh

cd ./minikube
kubectl delete -f .
sleep 5

minikube image remove liveramp-cn-north-1.jcr.service.jdcloud.com/spark-3:3.3.0
minikube image load liveramp-cn-north-1.jcr.service.jdcloud.com/spark-3:3.3.0
minikube image remove liveramp-cn-north-1.jcr.service.jdcloud.com/livy-custom:0.0.11
minikube image load liveramp-cn-north-1.jcr.service.jdcloud.com/livy-custom:0.0.11

kubectl apply -f .
cd ../

echo "build minikube local success"

sleep 5
kubectl -n livy logs --tail 200 -f "livy-0"