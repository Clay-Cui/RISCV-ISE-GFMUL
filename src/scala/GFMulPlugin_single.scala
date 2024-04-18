package vexriscv.demo

import spinal.core._
import vexriscv.plugin.Plugin
import vexriscv.{Stageable, DecoderService, VexRiscv}

class GFMulPlugin_single extends Plugin[VexRiscv]{
    //Define the concept of gfmul signals, which specify if the current instruction is destined for ths plugin
    object IS_GFMUL_SIN extends Stageable(Bool)
    object IS_GFMULImme extends Stageable(Bool)
  override def setup(pipeline: VexRiscv): Unit = {
    import pipeline.config._

    //Retrieve the DecoderService instance
    val decoderService = pipeline.service(classOf[DecoderService])

    //Specify the gfmul default value when instruction are decoded
    decoderService.addDefault(IS_GFMUL_SIN, False)
    decoderService.addDefault(IS_GFMULImme, False)
    //Specify the instruction decoding which should be applied when the instruction match the 'key' parttern
    decoderService.add(
      //Bit pattern of the new gfmul instruction
      key = M"0000011----------000-----0001011",

      //Decoding specification when the 'key' pattern is recognized in the instruction
      List(
        IS_GFMUL_SIN             -> True,
        REGFILE_WRITE_VALID      -> True, //Enable the register file write
        BYPASSABLE_EXECUTE_STAGE -> True, //Notify the hazard management unit that the instruction result is already accessible in the EXECUTE stage (Bypass ready)
        BYPASSABLE_MEMORY_STAGE  -> True, //Same as above but for the memory stage
        RS1_USE                  -> True, //Notify the hazard management unit that this instruction use the RS1 value
        RS2_USE                  -> True  //Same than above but for RS2.
      )
    )
    decoderService.add(
      //Bit pattern of the new gfmul instruction
      key = M"-----------------111-----0001011",

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
        rs2(7 downto 0) := imm(7 downto 0)
      }
      
      val rd =  U"0000_0000_0000_0000_0000_0000_0000_0000"
      val temp = UInt(15 bits)

        //Do some computation
        
        temp(14) := rs1(7) & rs2(7)
        temp(13) := rs1(7) & rs2(6) ^ rs1(6) & rs2(7)
        temp(12) := rs1(7) & rs2(5) ^ rs1(6) & rs2(6) ^ rs1(5) & rs2(7)
        temp(11) := rs1(7) & rs2(4) ^ rs1(6) & rs2(5) ^ rs1(5) & rs2(6) ^ rs1(4) & rs2(7)
        temp(10) := rs1(7) & rs2(3) ^ rs1(6) & rs2(4) ^ rs1(5) & rs2(5) ^ rs1(4) & rs2(6) ^ rs1(3) & rs2(7)
        temp(9)  := rs1(7) & rs2(2) ^ rs1(6) & rs2(3) ^ rs1(5) & rs2(4) ^ rs1(4) & rs2(5) ^ rs1(3) & rs2(6) ^ rs1(2) & rs2(7)
        temp(8)  := rs1(7) & rs2(1) ^ rs1(6) & rs2(2) ^ rs1(5) & rs2(3) ^ rs1(4) & rs2(4) ^ rs1(3) & rs2(5) ^ rs1(2) & rs2(6) ^ rs1(1) & rs2(7)
        temp(7)  := rs1(7) & rs2(0) ^ rs1(6) & rs2(1) ^ rs1(5) & rs2(2) ^ rs1(4) & rs2(3) ^ rs1(3) & rs2(4) ^ rs1(2) & rs2(5) ^ rs1(1) & rs2(6) ^ rs1(0) & rs2(7)
        temp(6)  := rs1(6) & rs2(0) ^ rs1(5) & rs2(1) ^ rs1(4) & rs2(2) ^ rs1(3) & rs2(3) ^ rs1(2) & rs2(4) ^ rs1(1) & rs2(5) ^ rs1(0) & rs2(6)
        temp(5)  := rs1(5) & rs2(0) ^ rs1(4) & rs2(1) ^ rs1(3) & rs2(2) ^ rs1(2) & rs2(3) ^ rs1(1) & rs2(4) ^ rs1(0) & rs2(5)
        temp(4)  := rs1(4) & rs2(0) ^ rs1(3) & rs2(1) ^ rs1(2) & rs2(2) ^ rs1(1) & rs2(3) ^ rs1(0) & rs2(4)
        temp(3)  := rs1(3) & rs2(0) ^ rs1(2) & rs2(1) ^ rs1(1) & rs2(2) ^ rs1(0) & rs2(3)
        temp(2)  := rs1(2) & rs2(0) ^ rs1(1) & rs2(1) ^ rs1(0) & rs2(2)
        temp(1)  := rs1(1) & rs2(0) ^ rs1(0) & rs2(1)
        temp(0)  := rs1(0) & rs2(0) 

        rd(7) := temp(7) ^ temp(11) ^ temp(12) ^ temp(14)
        rd(6) := temp(6) ^ temp(10) ^ temp(11) ^ temp(13)
        rd(5) := temp(5) ^ temp(9)  ^ temp(10) ^ temp(12)
        rd(4) := temp(4) ^ temp(8)  ^ temp(9)  ^ temp(11) ^ temp(14)
        rd(3) := temp(3) ^ temp(8)  ^ temp(10) ^ temp(11) ^ temp(12) ^ temp(13) ^ temp(14)
        rd(2) := temp(2) ^ temp(9)  ^ temp(10) ^ temp(13)
        rd(1) := temp(1) ^ temp(8)  ^ temp(9)  ^ temp(12) ^ temp(14)
        rd(0) := temp(0) ^ temp(8)  ^ temp(12) ^ temp(13)
                



      //When the instruction is a IS_GFMUL one, then write the result into the register file data path.
      when(execute.input(IS_GFMUL_SIN)||execute.input(IS_GFMULImme)) {
        execute.output(REGFILE_WRITE_DATA) := rd.asBits
      }
      // when(execute.input(IS_GFMULImme)) {
      //   execute.output(REGFILE_WRITE_DATA) := rd.asBits
      // }
    }
  }
}
