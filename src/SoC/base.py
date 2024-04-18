#!/usr/bin/env python3

from curses import baudrate
from migen import *

from migen.genlib.io import CRG

from litex.build.generic_platform import *
from litex.build.xilinx import XilinxPlatform

from litex.soc.integration.soc_core import *
from litex.soc.integration.builder import *
from litex.soc.cores import dna, xadc
from litex.soc.cores.spi import SPIMaster

from ios import Led,  Trigger

# IOs ----------------------------------------------------------------------------------------------

_io = [
    # ("user_led", 0, Pins("R14"), IOStandard("LVCMOS33")),
    # ("user_led", 1, Pins("P14"), IOStandard("LVCMOS33")),
    # ("user_led", 2, Pins("N16"), IOStandard("LVCMOS33")),
    # ("user_led", 3, Pins("M14"), IOStandard("LVCMOS33")),


    # ("user_sw", 0, Pins("M20"), IOStandard("LVCMOS33")),
    # ("user_sw", 1, Pins("M19"), IOStandard("LVCMOS33")),


    # ("user_btn", 0, Pins("D19"), IOStandard("LVCMOS33")),
    # ("user_btn", 1, Pins("D20"), IOStandard("LVCMOS33")),
    # ("user_btn", 2, Pins("L20"), IOStandard("LVCMOS33")),
    # ("user_btn", 3, Pins("L19"), IOStandard("LVCMOS33")),

    # ("user_rgb_led", 0,
    #     Subsignal("r", Pins("N15")),
    #     Subsignal("g", Pins("G17")),
    #     Subsignal("b", Pins("L15")),
    #     IOStandard("LVCMOS33"),
    # ),


    ("pll_clk1", 0, Pins("N13"), IOStandard("LVCMOS33")),
    ("clk_output",0,Pins("T13"),IOStandard("LVCMOS33")),

    ("cpu_reset", 0, Pins("R1"), IOStandard("LVCMOS33")),
    ("led", 0, Pins("T2"),IOStandard("LVCMOS33")),
    ("trigger", 0, Pins("T14"), IOStandard("LVCMOS33")),
    ("serial", 0,
        Subsignal("tx", Pins("B16")),
        Subsignal("rx", Pins("C16")),
        IOStandard("LVCMOS33"),
    ),


##    ("adxl362_spi", 0,
##        Subsignal("cs_n", Pins("D15")),
##        Subsignal("clk", Pins("F15")),
##        Subsignal("mosi", Pins("F14")),
##        Subsignal("miso", Pins("E15")),
##        IOStandard("LVCMOS33")
##    ),

]

# Platform -----------------------------------------------------------------------------------------

class Platform(XilinxPlatform):
    default_clk_name   = "pll_clk1"
    default_clk_period = 1e9/100e6

    def __init__(self):
        XilinxPlatform.__init__(self, "xc7a100tftg256-2", _io, toolchain="vivado")

# Design -------------------------------------------------------------------------------------------

# Create our platform (fpga interface)
platform = Platform()

# Create our soc (fpga description)
class BaseSoC(SoCCore):
    def __init__(self, platform):
        sys_clk_freq = int(100e6)

        # SoC with CPU
        SoCCore.__init__(self, platform,
            cpu_type                 = "vexriscv",
            cpu_variant              = "gfmulv", 
            # cpu_cfu                  = "",         
            clk_freq                 = 100e6,
            ident                    = "LiteX VexRiscV Test SoC", ident_version=True,
            integrated_rom_size      = 0x8000,
            integrated_sram_size     = 0x4000,
            integrated_main_ram_size = 0x6000,
            uart_baudrate            = 115200
            )

        # Clock Reset Generation
        self.submodules.crg = CRG(platform.request("pll_clk1"), ~platform.request("cpu_reset"))
        # self.submodules.crg = CRG(platform.request("pll_clk1"))
        # FPGA identification
        self.submodules.dna = dna.DNA()
        self.add_csr("dna")

        # FPGA Temperature/Voltage


        # Led

        usr_led = Cat(*[platform.request("led")])
        self.submodules.leds = Led(usr_led)
        self.add_csr("leds")
        # Switches


        # Buttons




        # Trigger
        trigger_pin = Cat(*[platform.request("trigger")])
        self.submodules.trigger_pin = Trigger(trigger_pin)
        self.add_csr("trigger_pin")

        # Accelerometer
##        self.submodules.adxl362 = SPIMaster(platform.request("adxl362_spi"),
##            data_width   = 32,
##            sys_clk_freq = sys_clk_freq,
##            spi_clk_freq = 1e6)
##        self.add_csr("adxl362")

        # SevenSegmentDisplay
##        self.submodules.display = SevenSegmentDisplay(sys_clk_freq)
##        self.add_csr("display")
##        self.comb += [
##            platform.request("display_cs_n").eq(~self.display.cs),
##            platform.request("display_abcdefg").eq(~self.display.abcdefg)
##        ]

soc = BaseSoC(platform)

# Build --------------------------------------------------------------------------------------------

builder = Builder(soc, output_dir="build", csr_csv="test/csr.csv")
builder.build(build_name="top")
