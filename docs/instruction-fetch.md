# Instruction Fetch

## Ports

The Instruction Fetch module would have the following IO ports:

### Inputs

- Boolean signals from dowmstream indicating readiness:
  - `ROB_ready`
  - `ALU_ready`
  - `LSB_ready`
- Instruction memory (Harvard architecture)
- `reset`: clear signal from ROB

### Outputs

- `enq_ready`: Boolean signal indicating if the queue can accept a new instruction.
- `deq_valid`: Boolean signal indicating if there is a valid instruction available for dequeue.
- `deq_bits`: Bundle containing the instruction data to be issued, same structure as `enq_bits`.
- `full`: Boolean signal indicating if the queue is full.
- `empty`: Boolean signal indicating if the queue is empty.

## Inner Workings

### Structure

The Instruction Fetch has an asynchronous memory as instruction memory, a program counter, a decoder and a FIFO buffer with a fixed depth (i.e. chisel Queue) of decoded instructions.

### Operation

On the clear signal, clear all contents of 
When the queue is not full, the decoder reads the next instruction and decodes it, putting it in the buffer.

## Implementation Details

```
class InstructionQueue:
  buffer <= []  # Sequential state
  enq_ready <= true
  deq_valid <= false
  deq_bits <= None
  full <= false
  empty <= true

  def on_clock():
    # Enqueue logic
    if enq_valid and len(buffer) < max_size:
      buffer <= buffer + [enq_bits]  # append
      enq_ready <= true
    else:
      enq_ready <= false

    # Dequeue logic
    if deq_ready and buffer:
      deq_bits <= buffer[0]
      buffer <= buffer[1:]  # pop front
      deq_valid <= true
    else:
      deq_valid <= false

    # Status
    full <= (len(buffer) == max_size)
    empty <= (len(buffer) == 0)
```
