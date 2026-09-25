## CCNetworks

This mod is an addon for [CC: Tweaked](https://modrinth.com/mod/cc-tweaked) that adds long-distance wired networking that works even through unloaded chunks, unlike standard [CC: Tweaked](https://modrinth.com/mod/cc-tweaked) modems. It does this relatively efficiently via storing network data in a separate per-world network graph.

---

### Nodes and links

CCNetworks has two node blocks:

- **Network Node** extends a network but does not expose a ComputerCraft peripheral
- **Network Interface Node** exposes a modem peripheral to an adjacent ComputerCraft device
  - these have the type `modem`, and behave exactly like a normal CC modem, but with an additional type of `network_node`

Use **Fiber Optic Cable** to link nodes

---

### Config
The server config, `config/ccnetworks.toml`, controls the maximum cable length and the maximum number of links per node. Defaults are 64 blocks and 4 links per node. Lowering the link limit does not affect existing links.

The client config, `config/ccnetworks-client.toml`, contains rendering settings, which you probably don't need to change.