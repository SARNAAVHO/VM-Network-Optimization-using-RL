# VM-Network-Optimization-using-RL

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

- **VM Features**:  
  - CPU cores, RAM, bandwidth requirement  
  - Task type (CPU-bound, IO-bound, Mixed)

- **Network States (for 20 networks)**:  
  - Load (0 to 1), latency (ms), available bandwidth

- **Target Outputs**:
  - `label_network_id`: network selected by CloudSim
  - `task_completion_time`
  - `throughput_achieved`

---

## 📊 Problem Setup

- **Input**: VM specs + state of all 20 networks + selected network label
- **Action Space**:  
  - `no_action`  
  - `increase_bandwidth`  
  - `decrease_bandwidth`  
  - *(Extendable to migration, CPU tuning, etc.)*

- **Reward Function**:

```math
Reward = w1 * normalized_throughput 
       - w2 * normalized_task_completion_time 
       - w3 * average_network_load 
       + w4 * impact_on_network_bandwidth_avail
