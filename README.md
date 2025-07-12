This project implements a **Deep Reinforcement Learning (DRL)**-based agent that learns to recommend optimal actions **after a VM is assigned to a cloud network**. It is built on top of a realistic, dynamically generated dataset from **CloudSim Plus**, simulating 20 cloud networks with varying **latency**, **load**, and **bandwidth availability**.

> 🎯 **Goal**: To improve task performance (e.g., reduce completion time, increase throughput) by recommending adaptive actions based on current network and VM states.

---

## 🔧 Key Features

- Realistic VM-network simulation using [CloudSim Plus](https://github.com/msrks/cloudsimplus)
- 20 simulated cloud networks (edge, regional, core) with dynamic load conditions
- DQN-based RL agent to learn action policies
- Custom reward function based on:
  - Task completion time
  - Network throughput
  - Load balancing
  - Bandwidth impact
- Baseline comparison: random vs. rule-based vs. RL
- Modular training and evaluation pipeline using PyTorch

---

## 📦 Dataset Overview

The dataset `vm_network_dynamic_realistic.csv` includes:

### 🖥️ VM Features:
- `vm_id`: Unique ID for each VM
- `cpu_cores`: Number of CPU cores requested
- `ram`: RAM requested (in MB)
- `bandwidth_req`: Required bandwidth (in Mbps)
- `task_type`: Task type (`cpu_bound`, `io_bound`, `mixed`)

### 🌐 Network States:
For each of the 20 networks (`net_1` to `net_20`), the following are included:
- `net_i_load`: Normalized current load on the network (0 to 1)
- `net_i_latency`: Current latency (in milliseconds)
- `net_i_bandwidth_avail`: Remaining available bandwidth (in Mbps)

### 📄 Additional Context:
- `label_network_id`: The network selected by CloudSim for this VM
- `task_completion_time`: Actual time taken to complete the VM's workload (seconds)
- `throughput_achieved`: Effective throughput achieved by the VM (Mbps)

### 🎯 Target Output:
- `action_label`: The optimal action to take **after the VM has been assigned** to the selected network
  - Examples: `no_action`, `increase_bandwidth`, `decrease_bandwidth`

---

## 📊 Problem Setup

- **Input**:  
  VM specs + state of all 20 networks + selected network (label)

- **Action Space**:  
  - `no_action`  
  - `increase_bandwidth`  
  - `decrease_bandwidth`  
  - *(Can be extended to support: VM migration, CPU tuning, energy control, etc.)*

- **Reward Function**:

```text
Reward = w1 * normalized_throughput 
       - w2 * normalized_task_completion_time 
       - w3 * average_network_load 
       + w4 * impact_on_network_bandwidth_avail
