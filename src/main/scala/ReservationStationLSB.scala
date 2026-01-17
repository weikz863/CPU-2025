import chisel3._
import chisel3.util._
import utils._

class ReservationStationLSB(entries: Int = 4) extends Module {
  val io = IO(new Bundle {
    val reset = Input(Bool())
    val issue_valid = Input(Bool())
    val issue_bits = Input(new IssueBitsLSB())
    val cdb_valid = Input(Bool())
    val cdb_tag = Input(UInt(5.W))
    val cdb_value = Input(UInt(32.W))
    val rf_entries = Input(Vec(32, new RegisterEntry()))
    val rob_entries = Input(Vec(32, new ROBEntry()))
    val issue_ready = Output(Bool())
    val exec_valid = Output(Bool())
    val exec_bits = Output(new ExecBitsLSB())
  })

  val mem = Reg(Vec(entries, new ReservationStationEntryLSB()))
  val enqPtr = RegInit(0.U(log2Ceil(entries).W))
  val deqPtr = RegInit(0.U(log2Ceil(entries).W))
  val count = RegInit(0.U(log2Ceil(entries + 1).W))

  val full = count === entries.U
  val empty = count === 0.U

  io.issue_ready := !full

  val rf_op1 = io.rf_entries(io.issue_bits.op1_index)
  val op1_tag = rf_op1.tag
  val op1_ready = !rf_op1.tag_valid || io.rob_entries(op1_tag).value.valid
  val rf_op2 = io.rf_entries(io.issue_bits.op2_index)
  val op2_tag = rf_op2.tag
  val op2_ready = !rf_op2.tag_valid || io.rob_entries(op2_tag).value.valid
  val isStore = io.issue_bits.op.isOneOf(MemOpEnum.sb, MemOpEnum.sh, MemOpEnum.sw)
  val op3_ready = true.B // immediate

  // Prepare entry
  val entry = Wire(new ReservationStationEntryLSB())
  entry.op := io.issue_bits.op
  entry.dest_tag := io.issue_bits.dest_tag
  entry.op1_tag := op1_tag
  entry.op1_ready := op1_ready
  entry.op1_value := Mux(!rf_op1.tag_valid, rf_op1.value,
                         Mux(io.rob_entries(op1_tag).value.valid, io.rob_entries(op1_tag).value.bits, 0.U))
  entry.op2_tag := op2_tag
  entry.op2_ready := op2_ready
  entry.op2_value := Mux(!rf_op2.tag_valid, rf_op2.value,
                         Mux(io.rob_entries(op2_tag).value.valid, io.rob_entries(op2_tag).value.bits, 0.U))
  entry.op3_tag := 0.U
  entry.op3_ready := op3_ready
  entry.op3_value := io.issue_bits.op3_value

  // Handle CDB broadcast in same cycle as issue
  when(io.cdb_valid && io.cdb_tag === op1_tag) {
    entry.op1_ready := true.B
    entry.op1_value := io.cdb_value
  }
  when(io.cdb_valid && io.cdb_tag === entry.op3_tag) {
    entry.op3_ready := true.B
    entry.op3_value := io.cdb_value
  }

  // Issue
  when(io.issue_valid && !full) {
    mem(enqPtr) := entry
    enqPtr := (enqPtr + 1.U) % entries.U
    count := count + 1.U
  }

  // Deq
  val deq_entry = mem(deqPtr)
  val effective_empty = empty && ! (io.issue_valid && !full)
  val effective_deq_entry = Mux(io.issue_valid && !full && empty, entry, deq_entry)
  val exec_from_deq = !effective_empty && effective_deq_entry.op1_ready && (effective_deq_entry.op3_ready || !isStoreFromOp(effective_deq_entry.op))
  def isStoreFromOp(op: MemOpEnum.Type) = op.isOneOf(MemOpEnum.sb, MemOpEnum.sh, MemOpEnum.sw)

  io.exec_valid := exec_from_deq
  io.exec_bits.op := effective_deq_entry.op
  io.exec_bits.value := Mux(isStoreFromOp(effective_deq_entry.op), effective_deq_entry.op1_value, 0.U)
  io.exec_bits.address := effective_deq_entry.op2_value + effective_deq_entry.op3_value
  io.exec_bits.index := effective_deq_entry.dest_tag

  when(exec_from_deq) {
    when(!(io.issue_valid && !full && empty)) {
      deqPtr := (deqPtr + 1.U) % entries.U
      count := count - 1.U
    }
  }

  // CDB updates
  when(io.cdb_valid) {
    for (i <- 0 until entries) {
      when(mem(i).op1_tag === io.cdb_tag && !mem(i).op1_ready) {
        mem(i).op1_value := io.cdb_value
        mem(i).op1_ready := true.B
      }
      when(mem(i).op2_tag === io.cdb_tag && !mem(i).op2_ready) {
        mem(i).op2_value := io.cdb_value
        mem(i).op2_ready := true.B
      }
      when(mem(i).op3_tag === io.cdb_tag && !mem(i).op3_ready) {
        mem(i).op3_value := io.cdb_value
        mem(i).op3_ready := true.B
      }
    }
  }

  // Reset
  when(io.reset) {
    enqPtr := 0.U
    deqPtr := 0.U
    count := 0.U
    for (i <- 0 until entries) {
      mem(i) := 0.U.asTypeOf(new ReservationStationEntryLSB())
    }
  }
}