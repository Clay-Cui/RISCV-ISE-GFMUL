package vexriscv.demo

import spinal.core._
import vexriscv.plugin.Plugin
import vexriscv.{Stageable, DecoderService, VexRiscv}

class GFMulPlugin_vector extends Plugin[VexRiscv]{
    //Define the concept of gfmul signals, which specify if the current instruction is destined for ths plugin
    object IS_GFMUL_VEC extends Stageable(Bool)
    object IS_GFMULImme extends Stageable(Bool)
  override def setup(pipeline: VexRiscv): Unit = {
    import pipeline.config._

    //Retrieve the DecoderService instance
    val decoderService = pipeline.service(classOf[DecoderService])

    //Specify the gfmul default value when instruction are decoded
    decoderService.addDefault(IS_GFMUL_VEC, False)
    decoderService.addDefault(IS_GFMULImme, False)
    //Specify the instruction decoding which should be applied when the instruction match the 'key' parttern
    decoderService.add(
      //Bit pattern of the new gfmul instruction
      key = M"0000011----------000-----0110011",

      //Decoding specification when the 'key' pattern is recognized in the instruction
      List(
        IS_GFMUL_VEC             -> True,
        REGFILE_WRITE_VALID      -> True, //Enable the register file write
        BYPASSABLE_EXECUTE_STAGE -> True, //Notify the hazard management unit that the instruction result is already accessible in the EXECUTE stage (Bypass ready)
        BYPASSABLE_MEMORY_STAGE  -> True, //Same as above but for the memory stage
        RS1_USE                  -> True, //Notify the hazard management unit that this instruction use the RS1 value
        RS2_USE                  -> True  //Same than above but for RS2.
      )
    )
    decoderService.add(
      //Bit pattern of the new gfmul instruction
      key = M"-----------------111-----0000011",

      //Decoding specification when the 'key' pattern is recognized in the instruction
      List(
        SRC2_CTRL                -> Src2CtrlEnum.IMI,
        IS_GFMULImme             -> True,
        REGFILE_WRITE_VALID      -> True, //Enable the register file write
        BYPASSABLE_EXECUTE_STAGE -> True, //Notify the hazard management unit that the instruction result is already accessible in the EXECUTE stage (Bypass ready)
        BYPASSABLE_MEMORY_STAGE  -> True, //Same as above but for the memory stage
        RS1_USE                  -> True, //Notify the hazard management unit that this instruction use the RS1 value
        RS2_USE                  -> True  //Same than above but for RS2.
      )
    )    
  }
  override def build(pipeline: VexRiscv): Unit = {
    import pipeline._
    import pipeline.config._

    //Add a new scope on the execute stage (used to give a name to signals)
    execute plug new Area {
      //Define some signals used internally to the plugin
      val rs1 = execute.input(RS1).asUInt //32 bits UInt value of the regfile(RS1)
      val imm = execute.input(INSTRUCTION)(27 downto 20).asUInt
      
      val rs2 = execute.input(RS2).asUInt
      
      when(execute.input(IS_GFMULImme)) {
        rs2(31 downto 24) := imm(7 downto 0)
        rs2(23 downto 16) := imm(7 downto 0)
        rs2(15 downto 8) := imm(7 downto 0)
        rs2(7 downto 0) := imm(7 downto 0)
      }
      
      val rd =  U"0000_0000_0000_0000_0000_0000_0000_0000"
      val temp = UInt(60 bits)

        //Do some computation
        var i = 0;
        for (i <- 0 to 3){
        temp(15*i+14) := rs1(i*8+7) & rs2(8*i+7)
        temp(15*i+13) := rs1(i*8+7) & rs2(8*i+6) ^ rs1(i*8+6) & rs2(8*i+7)
        temp(15*i+12) := rs1(i*8+7) & rs2(8*i+5) ^ rs1(i*8+6) & rs2(8*i+6) ^ rs1(i*8+5) & rs2(8*i+7)
        temp(15*i+11) := rs1(i*8+7) & rs2(8*i+4) ^ rs1(i*8+6) & rs2(8*i+5) ^ rs1(i*8+5) & rs2(8*i+6) ^ rs1(i*8+4) & rs2(8*i+7)
        temp(15*i+10) := rs1(i*8+7) & rs2(8*i+3) ^ rs1(i*8+6) & rs2(8*i+4) ^ rs1(i*8+5) & rs2(8*i+5) ^ rs1(i*8+4) & rs2(8*i+6) ^ rs1(i*8+3) & rs2(8*i+7)
        temp(15*i+9)  := rs1(i*8+7) & rs2(8*i+2) ^ rs1(i*8+6) & rs2(8*i+3) ^ rs1(i*8+5) & rs2(8*i+4) ^ rs1(i*8+4) & rs2(8*i+5) ^ rs1(i*8+3) & rs2(8*i+6) ^ rs1(i*8+2) & rs2(8*i+7)
        temp(15*i+8)  := rs1(i*8+7) & rs2(8*i+1) ^ rs1(i*8+6) & rs2(8*i+2) ^ rs1(i*8+5) & rs2(8*i+3) ^ rs1(i*8+4) & rs2(8*i+4) ^ rs1(i*8+3) & rs2(8*i+5) ^ rs1(i*8+2) & rs2(8*i+6) ^ rs1(i*8+1) & rs2(8*i+7)
        temp(15*i+7)  := rs1(i*8+7) & rs2(8*i+0) ^ rs1(i*8+6) & rs2(8*i+1) ^ rs1(i*8+5) & rs2(8*i+2) ^ rs1(i*8+4) & rs2(8*i+3) ^ rs1(i*8+3) & rs2(8*i+4) ^ rs1(i*8+2) & rs2(8*i+5) ^ rs1(i*8+1) & rs2(8*i+6) ^ rs1(i*8+0) & rs2(8*i+7)
        temp(15*i+6)  := rs1(i*8+6) & rs2(8*i+0) ^ rs1(i*8+5) & rs2(8*i+1) ^ rs1(i*8+4) & rs2(8*i+2) ^ rs1(i*8+3) & rs2(8*i+3) ^ rs1(i*8+2) & rs2(8*i+4) ^ rs1(i*8+1) & rs2(8*i+5) ^ rs1(i*8+0) & rs2(8*i+6)
        temp(15*i+5)  := rs1(i*8+5) & rs2(8*i+0) ^ rs1(i*8+4) & rs2(8*i+1) ^ rs1(i*8+3) & rs2(8*i+2) ^ rs1(i*8+2) & rs2(8*i+3) ^ rs1(i*8+1) & rs2(8*i+4) ^ rs1(i*8+0) & rs2(8*i+5)
        temp(15*i+4)  := rs1(i*8+4) & rs2(8*i+0) ^ rs1(i*8+3) & rs2(8*i+1) ^ rs1(i*8+2) & rs2(8*i+2) ^ rs1(i*8+1) & rs2(8*i+3) ^ rs1(i*8+0) & rs2(8*i+4)
        temp(15*i+3)  := rs1(i*8+3) & rs2(8*i+0) ^ rs1(i*8+2) & rs2(8*i+1) ^ rs1(i*8+1) & rs2(8*i+2) ^ rs1(i*8+0) & rs2(8*i+3)
        temp(15*i+2)  := rs1(i*8+2) & rs2(8*i+0) ^ rs1(i*8+1) & rs2(8*i+1) ^ rs1(i*8+0) & rs2(8*i+2)
        temp(15*i+1)  := rs1(i*8+1) & rs2(8*i+0) ^ rs1(i*8+0) & rs2(8*i+1)
        temp(15*i+0)  := rs1(i*8+0) & rs2(8*i+0) 

        rd(8*i+7) := temp(15*i+7) ^ temp(15*i+11) ^ temp(15*i+12) ^ temp(15*i+14)
        rd(8*i+6) := temp(15*i+6) ^ temp(15*i+10) ^ temp(15*i+11) ^ temp(15*i+13)
        rd(8*i+5) := temp(15*i+5) ^ temp(15*i+9)  ^ temp(15*i+10) ^ temp(15*i+12)
        rd(8*i+4) := temp(15*i+4) ^ temp(15*i+8)  ^ temp(15*i+9)  ^ temp(15*i+11) ^ temp(15*i+14)
        rd(8*i+3) := temp(15*i+3) ^ temp(15*i+8)  ^ temp(15*i+10) ^ temp(15*i+11) ^ temp(15*i+12) ^ temp(15*i+13) ^ temp(15*i+14)
        rd(8*i+2) := temp(15*i+2) ^ temp(15*i+9)  ^ temp(15*i+10) ^ temp(15*i+13)
        rd(8*i+1) := temp(15*i+1) ^ temp(15*i+8)  ^ temp(15*i+9)  ^ temp(15*i+12) ^ temp(15*i+14)
        rd(8*i+0) := temp(15*i+0) ^ temp(15*i+8)  ^ temp(15*i+12) ^ temp(15*i+13)
        }
                



      //When the instruction is a IS_GFMUL one, then write the result into the register file data path.
      when(execute.input(IS_GFMUL_VEC)||execute.input(IS_GFMULImme)) {
        execute.output(REGFILE_WRITE_DATA) := rd.asBits
      }
      // when(execute.input(IS_GFMULImme)) {
      //   execute.output(REGFILE_WRITE_DATA) := rd.asBits
      // }
    }
  }
}