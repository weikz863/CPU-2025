# Common Data Bus (CDB)

## Overview

The Common Data Bus is a broadcast mechanism that carries results from completed operations. When a functional unit finishes an operation, it broadcasts the result along with the destination tag on the CDB, allowing multiple reservation stations and the register file to capture the value simultaneously. This enables efficient communication in the out-of-order execution pipeline.

## Ports

### Inputs

- `reset`: Reset signal to clear the bus.
- `producers`: Input bundles from functional units(LSB/ALU) with ready/validness protocol(backpressure).
  - `index`: ROB index of the result.
  - `value`: The result value.

### Outputs

- `consumers`: Vector of output bundles to RS, RF and ROB with validness indicator.
  - `index`: ROB index of the result.
  - `value`: The broadcast value.

## Inner Workings

### Structure

The CDB only consists of arbitration logic.

### Operation

The core of the CDB is arbitration logic if multiple units try to broadcast simultaneously. This uses a round-robin arbiter of Chisel.

## Implementation Details

There are two separate input bundles, one from LSB and one from ALU with ready/validness protocol. They are wrapped up and fed to the arbiter.
There are three separate output bundles sharing the same output. The output has validness indicator but not readiness. Consumers are always assumed to readily accept the broadcast.
