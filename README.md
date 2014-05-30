The purpose of this project is to find out the ip address of a VM managed by
ovirt/RHEV. The VM must have rhev-agent (or ovirt-agent) installed
beforehand.

You will need to provide the username, password, ovirt truststore,
rhev/ovirt server api link, and the cluster name of the RHEV/OVirt vm.

The OVirt-Java SDK is being used in this case.


## How to build?
```bash
mvn clean install
```

## How to run?
```bash
# print usage
java -jar target/ovirt-find-ip-1.0.0.jar
```
