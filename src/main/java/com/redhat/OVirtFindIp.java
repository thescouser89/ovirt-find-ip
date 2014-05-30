package com.redhat;

import org.ovirt.engine.sdk.Api;
import org.ovirt.engine.sdk.decorators.Cluster;
import org.ovirt.engine.sdk.decorators.VM;
import org.ovirt.engine.sdk.entities.GuestInfo;

import java.util.List;
import java.util.ArrayList;


/*
 * The purpose of this class is to find out the ip address of a VM managed by
 * ovirt/RHEV. The VM must have rhev-agent (or ovirt-agent) installed
 * beforehand.
 *
 * You will need to provide the username, password, ovirt truststore,
 * rhev/ovirt server api link, and the cluster name of the RHEV/OVirt vm.
 *
 */
public class OVirtFindIp {
    private Api api;
    private Cluster cluster;
    private String ovirtURL;
    private String clusterName;
    private String username;
    private String password;
    private String truststoreLocation;

    public OVirtFindIp(final String ovirtURL,
                       final String username,
                       final String password,
                       final String clusterName,
                       final String truststoreLocation) {
        this.ovirtURL = ovirtURL;
        this.username = username;
        this.password = password;
        this.clusterName = clusterName;
        this.truststoreLocation = truststoreLocation;
    }

    /**
     * Get the api object. Will create a new Api object if it has not been
     * initialized yet.
     *
     * @return Api object for this hypervisor
     *         null if creation of Api object throws an exception
     */
    public Api getAPI() throws Exception {
        if(api == null) {
            api = new Api(ovirtURL,
                          username,
                          password,
                          truststoreLocation);
        }
        return api;
    }

    /**
     * Get the cluster object corresponding to the clusterName if clusterName
     * is specified. The cluster object will then be memoized.
     * @return null if clusterName is empty
     *         cluster object corresponding to clusterName
     * @throws Exception
     */
    public Cluster getCluster() throws Exception {
        if (cluster == null && isClusterSpecified()) {
            cluster = getAPI().getClusters().get(clusterName);
        }
        return cluster;
    }

    /**
     * Determines if the cluster was specified in this object. If clusterName
     * is just an empty string, then return False
     *
     * @return true if clusterName is specified
     */
    private boolean isClusterSpecified() {
        return !clusterName.trim().equals("");
    }

    /**
     * Get the VM object of a vm from the vm name string.
     *
     * @param vm: vm name in the ovirt server
     * @return the VM object
     */
    public VM getVM(String vm) {
        for(VM vmi: getVMs()) {
            if (vmi.getName().equals(vm)) {
                return vmi;
            }
        }
        return null;
    }

    /**
     * Get a list of VM objects; those VM objects represents all the vms in
     * the ovirt server belonging to a cluster, if the cluster value is
     * specified.
     *
     * @return list of VM objects
     */
    public List<VM> getVMs() {
        try {
            List<VM> vms = getAPI().getVMs().list();
            List<VM> vmsInCluster = new ArrayList<VM>();
            // if clusterName specified, search for vms in that cluster
            if (isClusterSpecified()) {
                for (VM vm: vms) {
                    if (vm.getCluster()
                            .getHref()
                            .equals(getCluster().getHref())) {
                        vmsInCluster.add(vm);
                    }
                }
                return vmsInCluster;
            } else {
                return vms;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String getIpOfVM(String vmName) {
        try {
            VM vm = this.getVM(vmName);
            GuestInfo guestInfo = vm.getGuestInfo();
            return guestInfo.getIps().getIPs().get(0).getAddress();
        } catch(Exception e) {
            System.err.println("VM Name is not valid!");
            e.printStackTrace();
        }
        return null;
    }

    public boolean areCredentialsValid() {
        try {
            /* call this to see if username, password,
            and truststore are fine. Will throw an exception if one of these
            items are wrong */
            getAPI();

            return true;
        } catch (Exception e) {
            System.err.println("Something wrong with username, password, " +
                    "or truststore");
            return false;
        }
    }
    public void shutdown() {
        try {
            getAPI().shutdown();
        } catch (Exception e) {}
    }

    private static void printUsage() {
        System.out.println();
        System.out.println("Usage: java -jar <jar> <username> " +
                "<password> <trustore> <server> <cluster> <vm>");
        System.out.println("e.g java -jar <jar> jim mypassword ovirt" +
                ".truststore https://localhost:443/api Default myVM");
        System.out.println();
    }

    private static boolean doesFileExist(final String fileToCheck) {
        return new java.io.File(fileToCheck).exists();
    }

    public static void main (String[] args) {
        if (args.length != 6) {
            printUsage();
            System.exit(1);
        }

        String username = args[0];
        String password = args[1];
        String trustore = args[2];
        String server = args[3];
        String cluster = args[4];
        String vm = args[5];

        if (!doesFileExist(trustore)) {
            System.err.println("Truststore location is wrong! Aborting.");
            System.exit(1);
        }

        OVirtFindIp  ipFinder = new OVirtFindIp(server,
                                                username, password,
                                                cluster,
                                                trustore);

        if(ipFinder.areCredentialsValid()) {
            System.out.println(ipFinder.getIpOfVM(vm));
        }
        ipFinder.shutdown();
    }
}
