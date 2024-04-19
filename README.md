
# Efficient Software Masking of AES through Instruction Set Extension
This repository contains the source code of the implementation of the GFMUL instruction extension for RISC-V.

This ReadMe walks through the steps of recreating the [LiteX](https://github.com/enjoy-digital/litex) framework environment, generating GFMUL extended [VexRiscV](https://github.com/SpinalHDL/VexRiscv) with SpinalHDL, and creating the SoC file as well as the bitstream used for FPGA evaluation. 

If you use our code, please cite our work properly. 
```
@INPROCEEDINGS{10137150,
  author={Cui, Songqiao and Balasch, Josep},
  booktitle={2023 Design, Automation & Test in Europe Conference & Exhibition (DATE)}, 
  title={Efficient Software Masking of AES through Instruction Set Extensions}, 
  year={2023},
  doi={10.23919/DATE56975.2023.10137150}}
```
Setup script setup.sh provides a simple solution to prepare all required environment, but you can also install them individually. 
```sh
chmod +x setup.sh
setup.sh
```
## LiteX Environment

This project relies on LiteX environment to generate SoC wrapper project which is later used to generate the bitstream.

To install Litex:
```sh
wget https://raw.githubusercontent.com/enjoy-digital/litex/master/litex_setup.py
chmod +x litex_setup.py
./litex_setup.py --init --install --user --config=standard
```

## VexRISCV
To clone VexRiscV:
```sh
git clone https://github.com/SpinalHDL/VexRiscv.git
``` 

To install Scala SBT for compiling from SpinalHDL to normal verilog file:

```sh
# JAVA JDK 8
sudo add-apt-repository -y ppa:openjdk-r/ppa
sudo apt-get update
sudo apt-get install openjdk-8-jdk -y
sudo update-alternatives --config java
sudo update-alternatives --config javac

# Install SBT - https://www.scala-sbt.org/
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt-get update
sudo apt-get install sbt
```
After installing VexRiscV and SBT, copy the plugin scala file as well as the configuration file to VexRiscv folder:
```sh
cp src/scala/* VexRiscv/src/main/scala/vexriscv/demo/
```
To generate core file:
```sh
pushd VexRiscv/
sbt "runMain vexriscv.demo.GenCoreGFMUL"
popd
```
Notes:

- You can choose from GFMulPlugin_singel and GFMulPlugin_vector by changing corresponding plugin in the GenCoreGFMUL.scala file.
- We disable both data and instruction cache in GenCoreGFMUL.scala.

Two files will be created after commands above, one `.v` file and one `.yaml` file. These two are both required in LiteX framework to generate SoC wrapper. 

## SoC Generation
Copy the generated files to VexRiscv subfolder which comes with LiteX:
```sh
cp VexRiscv_GFMULS.* litex/pythondata-cpu-vexriscv/pythondata_cpu_vexriscv/verilog/
```
For LiteX to recognize the newly added files, the corresponding liteX python source code needs to be adatped.

Simply replace the `core.py`:
```sh
cp src/py/core.py litex/litex/litex/soc/cores/cpu/vexriscv/core.py
```

Two lines are appended in the `CPU_VARIANTS`:
```sh
"gfmuls":		  "VexRiscv_GFMULS",
#"gfmulv":		  "VexRiscv_GFMULV",
```
Similarly for `GCC_FLAGS`:
```sh
"gfmuls":           "-march=rv32im    -mabi=ilp32",
#"gfmulv":           "-march=rv32im    -mabi=ilp32",
```

Finally, we can use LiteX to generate the SoC wrapper:
```sh
pushd src/SoC
python3 base.py
popd
```
This command will invoke Vivado and create `/build` directory which contains the generated SoC project and compiled software library.
## GCC modification
### Build from scrach
In order to compile the firmware with custom instruction, customized toolchain is needed and needs to be compiled from source code. Note that this may take some time depending on computer configuration. More detailed tutorial can be found [here](https://pcotret.gitlab.io/riscv-custom/).

First, clone the official GCC compiler. 
```sh
git clone https://github.com/riscv/riscv-gnu-toolchain
```
Then copy the two files in `riscv-gnu-toolchain` to the correct location.
```sh
cp toolchain/riscv-opc.h riscv-gnu-toolchain/riscv-binutils/include/opcode/riscv-opc.h
cp toolchain/riscv-opc.c riscv-gnu-toolchain/riscv-binutils/opcodes/riscv-opc.c
```
To target the installation on Newlib:
```sh
./configure --prefix=/opt/riscv
make
```
### Precompiled version
We provide precompiled binary file in `/toolchain/bin` for easy usage.

## Firmware update:

To generate new firmware:
```sh
pushd src/SoC/firmware
make all CC='path/to/your/riscv/compiler'
popd
```

Litex provides simple but handy tool, the litex_term to update firmware through UART:
```sh
pushd src/SoC
litex_term --speed 115200 --kernel firmware/demo.bin /dev/ttyUSB0
```
We provide python script for easier updating the firmware as well as communication with the board.
```sh
python3 aes_validate.py
```