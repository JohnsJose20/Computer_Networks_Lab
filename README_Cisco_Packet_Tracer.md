# Cisco Packet Tracer: Two Routers and Two PCs

A step-by-step reference for building a small routed network with two Cisco 1941 routers, two PCs, a serial connection, and static routes.

## 1. Network topology

```text
PC0                                      PC1
 |                                        |
 | Copper Straight-Through                | Copper Straight-Through
 |                                        |
Router0 ======== Serial DCE/DTE ========= Router1
 G0/0            S0/1/0       S0/1/0      G0/0
```

### Devices required
- 2 × Cisco 1941 routers
- 2 × PCs
- 2 × HWIC-2T serial modules (one per router)
- 2 × Copper Straight-Through cables
- 1 × Serial DCE cable

> **Important:** This guide uses `Serial0/1/0` on both routers, as in the working configuration shown during the lab. Interface numbering can vary depending on the slot used. Check `show ip interface brief` and use the actual interface name on your router.

## 2. Add devices in Packet Tracer

1. Open Cisco Packet Tracer and create a blank project.
2. From **Network Devices → Routers**, place two **Cisco 1941** routers.
3. From **End Devices**, place two PCs.
4. Arrange them as `PC0 — Router0 — Router1 — PC1`.

## 3. Install serial modules

Do this on **each** router:

1. Click the router and open **Physical**.
2. Turn the router off using its power switch.
3. Drag an `HWIC-2T` module into an available HWIC slot.
4. Turn the router on.

## 4. Connect the devices

### PC0 to Router0
1. Select **Connections** (lightning-bolt icon).
2. Choose **Copper Straight-Through**.
3. Connect `PC0 FastEthernet0` to `Router0 GigabitEthernet0/0`.

### Router0 to Router1
1. Choose the **Serial DCE** cable.
2. Connect `Router0 Serial0/1/0` to `Router1 Serial0/1/0`.
3. The router on the DCE end needs a clock rate.

### Router1 to PC1
1. Choose **Copper Straight-Through**.
2. Connect `Router1 GigabitEthernet0/0` to `PC1 FastEthernet0`.

The unused `GigabitEthernet0/1` interface can remain administratively down. That is normal when it is not connected or used.

## 5. IP addressing plan

| Device | Interface | IPv4 address | Subnet mask | Default gateway |
|---|---|---|---|---|
| Router0 | GigabitEthernet0/0 | `192.168.1.1` | `255.255.255.0` | — |
| Router0 | Serial0/1/0 | `10.0.0.1` | `255.255.255.252` | — |
| Router1 | Serial0/1/0 | `10.0.0.2` | `255.255.255.252` | — |
| Router1 | GigabitEthernet0/0 | `192.168.2.1` | `255.255.255.0` | — |
| PC0 | FastEthernet0 | `192.168.1.10` | `255.255.255.0` | `192.168.1.1` |
| PC1 | FastEthernet0 | `192.168.2.10` | `255.255.255.0` | `192.168.2.1` |

## 6. Configure Router0

Click **Router0 → CLI**. If asked whether to enter the initial configuration dialog, type `no` and press Enter.

Enter these commands one at a time:

```text
enable
configure terminal
interface gigabitEthernet 0/0
ip address 192.168.1.1 255.255.255.0
no shutdown
exit
interface serial 0/1/0
ip address 10.0.0.1 255.255.255.252
```

Check whether this serial interface is the DCE end:

```text
do show controllers serial 0/1/0
```

If the output identifies it as **DCE**, configure the clock rate:

```text
clock rate 64000
```

Then activate the serial interface and exit configuration mode:

```text
no shutdown
end
```

### Verify Router0

```text
show ip interface brief
```

Expected key lines:

```text
GigabitEthernet0/0     192.168.1.1     YES manual up     up
Serial0/1/0            10.0.0.1        YES manual up     up
```

Spacing and other interfaces may differ. The important point is that the interfaces used by the topology should be `up/up`.

## 7. Configure Router1

Click **Router1 → CLI**. If asked about the initial configuration dialog, type `no` and press Enter.

```text
enable
configure terminal
interface serial 0/1/0
ip address 10.0.0.2 255.255.255.252
no shutdown
exit
interface gigabitEthernet 0/0
ip address 192.168.2.1 255.255.255.0
no shutdown
end
```

Do **not** set `clock rate` on Router1 if Router0 is the DCE end. If the DCE end is actually Router1, set the clock rate on Router1 instead.

### Verify Router1

```text
show ip interface brief
```

Expected key lines:

```text
GigabitEthernet0/0     192.168.2.1     YES manual up     up
Serial0/1/0            10.0.0.2        YES manual up     up
```

## 8. Configure PC0

1. Click **PC0 → Desktop → IP Configuration**.
2. Enter:

```text
IP Address:      192.168.1.10
Subnet Mask:     255.255.255.0
Default Gateway: 192.168.1.1
```

## 9. Configure PC1

1. Click **PC1 → Desktop → IP Configuration**.
2. Enter:

```text
IP Address:      192.168.2.10
Subnet Mask:     255.255.255.0
Default Gateway: 192.168.2.1
```

## 10. Configure static routes

Each router needs a route to the LAN behind the other router.

### On Router0

```text
enable
configure terminal
ip route 192.168.2.0 255.255.255.0 10.0.0.2
end
```

Meaning: send traffic for network `192.168.2.0/24` to Router1 at `10.0.0.2`.

### On Router1

```text
enable
configure terminal
ip route 192.168.1.0 255.255.255.0 10.0.0.1
end
```

Meaning: send traffic for network `192.168.1.0/24` to Router0 at `10.0.0.1`.

## 11. Test in this order

Run one test at a time. If one fails, troubleshoot that link before moving to the next test.

### Test A — PC0 to its gateway

On **PC0 → Desktop → Command Prompt**:

```text
ping 192.168.1.1
```

Expected: replies from `192.168.1.1`.

### Test B — PC1 to its gateway

On **PC1 → Desktop → Command Prompt**:

```text
ping 192.168.2.1
```

Expected: replies from `192.168.2.1`.

### Test C — Router0 to Router1 over serial

On **Router0 → CLI**:

```text
ping 10.0.0.2
```

Expected: replies from `10.0.0.2`.

### Test D — Router1 to PC1

On **Router1 → CLI**:

```text
ping 192.168.2.10
```

Expected: replies from `192.168.2.10`.

### Test E — PC0 to PC1 (end-to-end test)

On **PC0 → Desktop → Command Prompt**:

```text
ping 192.168.2.10
```

Expected: replies from `192.168.2.10`.

The first ping can occasionally time out while ARP resolves addresses. If that happens, repeat the ping once before troubleshooting.

## 12. Useful troubleshooting commands

### Check interface status (on either router)

```text
show ip interface brief
```

For the interfaces used in this lab, aim for `up/up`.

### Check the routing table

```text
show ip route
```

Look for the connected networks and the static route to the other LAN. Static routes commonly appear with an `S`.

### Check serial DCE/DTE information

```text
show controllers serial 0/1/0
```

If needed, use the actual serial interface name.

### Check the running configuration

```text
show running-config
```

### If an interface is administratively down

For an interface that is actually used, enter configuration mode and enable it. Example:

```text
configure terminal
interface gigabitEthernet 0/0
no shutdown
end
```

Do not enable unused interfaces just because they show administratively down. For example, `GigabitEthernet0/1` can stay down if nothing is connected to it.

### If PC1 cannot ping `192.168.2.1`

1. Verify PC1's IP address, mask, and gateway.
2. Verify Router1's `GigabitEthernet0/0` is `192.168.2.1` and `up/up`.
3. Check the PC1-to-Router1 cable: `PC1 FastEthernet0` to `Router1 GigabitEthernet0/0`.
4. From Router1, run `ping 192.168.2.10`.
5. If Router1 cannot ping PC1, recheck PC1 addressing and the Ethernet connection.

### If the routers cannot ping each other

1. Check both serial interface IP addresses and ensure both serial interfaces are `up/up`.
2. Confirm the serial cable is attached to the intended interfaces.
3. Check which end is DCE.
4. Configure `clock rate 64000` on the DCE end only.
5. Retry `ping 10.0.0.2` from Router0.

## 13. Save the router configurations

Run on **each router**:

```text
enable
copy running-config startup-config
```

When asked:

```text
Destination filename [startup-config]?
```

Press **Enter**.

## 14. Router modes to remember

| Prompt | Mode | Common action |
|---|---|---|
| `Router>` | User EXEC | Type `enable` |
| `Router#` | Privileged EXEC | Run `show` commands or enter configuration mode |
| `Router(config)#` | Global configuration | Configure routes or select an interface |
| `Router(config-if)#` | Interface configuration | Set an IP address, `clock rate`, or `no shutdown` |

Typical mode movement:

```text
Router> enable
Router# configure terminal
Router(config)# interface gigabitEthernet 0/0
Router(config-if)# exit
Router(config)# end
Router#
```

## Final checklist

- [ ] Both routers and both PCs are placed.
- [ ] HWIC-2T modules installed in both routers.
- [ ] Correct cables and interfaces connected.
- [ ] Router0 LAN and serial interfaces configured.
- [ ] Router1 LAN and serial interfaces configured.
- [ ] Both PCs have correct IP addresses and default gateways.
- [ ] Static routes configured on both routers.
- [ ] PC0 can ping `192.168.1.1`.
- [ ] PC1 can ping `192.168.2.1`.
- [ ] Router0 can ping `10.0.0.2`.
- [ ] Router1 can ping `192.168.2.10`.
- [ ] PC0 can ping `192.168.2.10`.
- [ ] Both router configurations saved.
