# Port-forwards to PLCCcomS and Mosquitto broker running in K8s
kubectl --context j5005 port-forward svc/plccoms 5010:5010 &
kubectl --context j5005 port-forward svc/mosquitto 1883:1883 &
