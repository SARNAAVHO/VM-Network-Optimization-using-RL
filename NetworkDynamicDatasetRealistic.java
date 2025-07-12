// Save this as NetworkDynamicDatasetRealistic.java

package org.cloudsimplus.examples;

import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class NetworkDynamicDatasetRealistic {
    private static final int NUM_NETWORKS = 20;
    private static final int NUM_RECORDS = 20000;
    private static final long[] CLOUDLET_LENGTHS = {10000, 50000, 150000}; // short, medium, long
    private static final String[] TASK_TYPES = {"cpu_bound", "io_bound", "mixed"};
    private static final int VM_MIPS = 1000;
    private static FileWriter writer;

    static class NetState {
        int activeVms = 0;
        double load = 0.0;
        double latency = 5.0;
        int bwCapacity = 1000;
        int bwUsed = 0;
        double baseLatency; // edge: 5ms, regional: 10ms, core: 20ms
    }

    static NetState[] nets = new NetState[NUM_NETWORKS];

    public static void main(String[] args) throws IOException {
        Random rnd = new Random();

        // Assign base latencies per datacenter (simulate topology)
        for (int i = 0; i < NUM_NETWORKS; i++) {
            nets[i] = new NetState();
            if (i < 5)
                nets[i].baseLatency = 5;       // Edge
            else if (i < 15)
                nets[i].baseLatency = 10;      // Regional
            else
                nets[i].baseLatency = 20;      // Core
        }

        writer = new FileWriter("vm_network_dynamic_realistic_20k.csv");
        writeHeader();

        for (int vmId = 1; vmId <= NUM_RECORDS; vmId++) {
            CloudSimPlus simulation = new CloudSimPlus();
            DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

            List<Datacenter> datacenters = new ArrayList<>();
            for (int i = 0; i < NUM_NETWORKS; i++)
                datacenters.add(createDatacenter(simulation));

            // Vary temporal load pattern (simulate peak/off-peak every 200 VMs)
            boolean peakHour = (vmId % 400) < 200;

            // Pick cloudlet type
            int cloudletType = rnd.nextInt(3);
            long length = CLOUDLET_LENGTHS[cloudletType];
            String taskType = TASK_TYPES[cloudletType];

            int cpu = pick(new int[]{1, 2, 4}, rnd);
            int ram = pick(new int[]{512, 1024, 2048, 4096}, rnd);
            int bwReq = rnd.nextInt(300 - 50) + 50;

            // Update net state
            for (int i = 0; i < NUM_NETWORKS; i++) {
                NetState ns = nets[i];
                double bgNoise = peakHour ? 0.6 : 0.3; // background noise
                ns.load = Math.min(1.0, (ns.activeVms / 10.0) + rnd.nextDouble() * bgNoise);
                ns.latency = ns.baseLatency + ns.load * 15;
                ns.bwCapacity = 800 + rnd.nextInt(400); // vary bw capacity
            }

            // Select best network
            int sel = -1;
            double bestScore = Double.MAX_VALUE;
            for (int i = 0; i < NUM_NETWORKS; i++) {
                NetState ns = nets[i];
                int bwAvail = ns.bwCapacity - ns.bwUsed;
                if (bwAvail >= bwReq) {
                    double score = ns.load * 2.5 + ns.latency / 100.0;
                    if (score < bestScore) {
                        bestScore = score;
                        sel = i;
                    }
                }
            }

            if (sel < 0) sel = rnd.nextInt(NUM_NETWORKS); // fallback

            NetState selectedNet = nets[sel];
            selectedNet.activeVms++;
            selectedNet.bwUsed += bwReq;

            Vm vm = new VmSimple(vmId, VM_MIPS * cpu, cpu)
                .setRam(ram).setBw(bwReq).setSize(10000)
                .setCloudletScheduler(new CloudletSchedulerTimeShared());

            Cloudlet cloudlet = new CloudletSimple(length, cpu)
                .setFileSize(300).setOutputSize(300)
                .setUtilizationModelCpu(new UtilizationModelFull())
                .setUtilizationModelRam(new UtilizationModelFull())
                .setUtilizationModelBw(new UtilizationModelFull());

            cloudlet.setVm(vm);
            broker.submitVm(vm);
            broker.submitCloudlet(cloudlet);

            simulation.start();

            List<Cloudlet> finishedList = broker.getCloudletFinishedList();
            if (finishedList.isEmpty()) {
                System.out.printf("VM %d failed.\n", vmId);
                selectedNet.bwUsed -= bwReq;
                selectedNet.activeVms--;
                continue;
            }

            Cloudlet finished = finishedList.get(0);
            double execTime = finished.getFinishTime() - finished.getStartTime();
            double throughput = bwReq * (1.0 - selectedNet.load) * (1.0 - rnd.nextDouble() * 0.1);

            writeRecord(vmId, cpu, ram, bwReq, sel + 1, execTime, throughput, taskType);

            selectedNet.bwUsed -= bwReq;
            selectedNet.activeVms--;

            if (vmId % 100 == 0)
                System.out.println("Generated: " + vmId + " records");
        }

        writer.close();
        System.out.println("Realistic dataset generation complete!");
    }

    private static Datacenter createDatacenter(CloudSimPlus sim) {
        List<Host> hostList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                peList.add(new PeSimple(VM_MIPS));
            }

            Host host = new HostSimple(8192, 100000, 1000000, peList)
                .setVmScheduler(new VmSchedulerTimeShared());
            hostList.add(host);
        }
        return new DatacenterSimple(sim, hostList);
    }

    private static void writeHeader() throws IOException {
        StringBuilder sb = new StringBuilder("vm_id,cpu_cores,ram,bandwidth_req,");
        for (int i = 1; i <= NUM_NETWORKS; i++) {
            sb.append("net_").append(i).append("_load,")
                .append("net_").append(i).append("_latency,")
                .append("net_").append(i).append("_bandwidth_avail,");
        }
        sb.append("label_network_id,task_completion_time,throughput_achieved,task_type\n");
        writer.append(sb.toString());
    }

    private static void writeRecord(int vmId, int cpu, int ram, int bwReq,
                                    int labelNet, double ttime, double thr, String taskType) throws IOException {
        StringBuilder sb = new StringBuilder(String.format("%d,%d,%d,%d,", vmId, cpu, ram, bwReq));
        for (int i = 0; i < NUM_NETWORKS; i++) {
            NetState ns = nets[i];
            int bwAvail = Math.max(0, ns.bwCapacity - ns.bwUsed);
            sb.append(String.format("%.2f,%.2f,%d,", ns.load, ns.latency, bwAvail));
        }
        sb.append(labelNet).append(String.format(",%.2f,%.2f,%s\n", ttime, thr, taskType));
        writer.append(sb.toString());
    }

    private static int pick(int[] arr, Random rnd) {
        return arr[rnd.nextInt(arr.length)];
    }
}
