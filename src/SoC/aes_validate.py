
import base64
import serial
import random
from Crypto.Cipher import AES
from litex.tools import litex_term, litex_sim
import argparse
import sys
import threading
import time
pt = bytearray([0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10])
pt1 = bytearray([0x00, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10])
pt2 = bytearray([0x00, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10])
pt3 = bytearray([0x00, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10])
pt4 = bytearray([0x00, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10])

key = bytes([0x2b, 0x7e, 0x15, 0x16, 0x28, 0xae, 0xd2, 0xa6, 0xab, 0xf7, 0x15, 0x88, 0x09, 0xcf, 0x4f, 0x3c])
ct = bytearray(16)


cipher = AES.new(key,AES.MODE_ECB)
# ser = serial.Serial('/dev/ttyACM0', 115200)         ##For VEGA Board

# # print(ser.name)
# for j in range(100):
#     for i in range(16):
#         pt[i] = random.getrandbits(8)
#     ser.write(pt)
#     ct=ser.read(16)
#     print(ct.hex())
#     encrypt_data =  cipher.encrypt(bytes(pt))
#     print(encrypt_data.hex())
#     print(encrypt_data == ct)


port = ('/dev/ttyUSB0')
kernel = "firmware/firmware_BM.bin"
term = litex_term.LiteXTerm(False, kernel, "0x40000000", None,"store_true")
term.open(port, 115200)

# term.start()
term.reader_alive = True

try:
    while term.reader_alive:
        c = term.port.read()
        sys.stdout.buffer.write(c)
        sys.stdout.flush()
        # print("get:")
        # print(c)
        if len(term.mem_regions):
            if term.serial_boot and term.detect_prompt(c):
                term.answer_prompt()
            if term.detect_magic(c):
                term.answer_magic()
            if term.detect_end(c):
                break
except serial.SerialException:
    term.reader_alive = False
    term.console.unconfigure()
    raise

# Time
# print(term.port.read(4).hex())
# print(term.port.read(4).hex())


elapsed = 0
loop = 1

num = 0


# for i in range(10):
#     term.port.write(pt)
#     print(term.port.read(16).hex())
# term.port.timeout=0.1
# for j in range(2):
#     start=int.from_bytes(term.port.read(),"big")
#     end=int.from_bytes(term.port.read(),"big")
#     t = end-start
#     print(t)
#     print(term.port.read(4).hex())
#     print(term.port.read(4).hex())
#########################################
#############AES random test#############
#########################################
# for i in range(16):
#     pt[i] = random.getrandbits(8)
# term.port.write(pt)
# print(pt.hex())
# for i in range(177):
#     print(i)
#     ct = term.port.read(16)
#     encrypt_data =  cipher.encrypt(bytes(pt))
#     print("returned ct1   :  "+ct.hex())
#     print("python AES1    :  "+encrypt_data.hex())
#     print(encrypt_data == ct)
#     pt = encrypt_data
    
f16=20922789888000
#########################################
############Normal IPM test##############
#########################################
for j in range(loop):
    print(j)
    for i in range(16):
        pt[i] = random.getrandbits(8)
    term.port.write(pt)
    
    for i in range(16):
        pt1[i] = random.getrandbits(8)
    # pt1 = bytearray([0x00, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10])
    term.port.write(pt1)

    start = time.time()

    ct1=term.port.read(16)
    # s = term.port.read(16)
    end = time.time()
    # term.port.write(pt1)
    # ctc=term.port.read(16)    
    print("plain text1    :  "+pt1.hex())

    
    # encrypt_data =  cipher.encrypt(bytes(pt))

    encrypt_data1 =  cipher.encrypt(bytes(pt1))
    
    print("returned ct1   :  "+ct1.hex())
    # print("returned state :  "+s.hex())
    print("python AES1    :  "+encrypt_data1.hex())
    # term.port.write(key)
    # key_get=term.port.read(16)
    # print("key           :  "+key_get.hex())
    t = encrypt_data1 == ct1 or  pt1 == ct1
    # t = pt1 == ct1
    # t = pt1 == ct1  and pt2 == ct2 and pt3 == ct3 and pt4 == ct4
    # t = pt1 == ct1
    print(t)
    
    if(not t):
        num+=1
        while(term.port.read()==b''):
            time.sleep(0.1)
            break
    else:
        elapsed = (elapsed*(j-num)+ end - start)/(j+1-num)
    if(j%50==0):
        print(elapsed)



        
    # time.sleep(0.1)
if(num):    
    print("There are total "+str(num)+" time(s) (out of "+str(loop)+") check failure")
else:
    print("All AES ("+str(loop)+") calculations are correct")
print("Average time for one AES run is "+str(elapsed))
            


##########################################
###########Normal IPM Vec test############
##########################################
# for j in range(loop):
#     print(j)
#     for i in range(16):
#         pt[i] = random.getrandbits(8)
#     term.port.write(pt)
    
#     for i in range(16):
#         pt1[i] = random.getrandbits(8)
#         pt2[i] = random.getrandbits(8)
#         pt3[i] = random.getrandbits(8)
#         pt4[i] = random.getrandbits(8)
#     term.port.write(pt1)
#     term.port.write(pt2)
#     term.port.write(pt3)
#     term.port.write(pt4)
        
#     # time.sleep(0.1)



#     start = time.time()
#     # for k in range(16):
#     #     x=term.port.read()
#     #     print(x.hex())
#     ct1=term.port.read(16)
#     ct2=term.port.read(16)
#     ct3=term.port.read(16)
#     ct4=term.port.read(16)
#     end = time.time()
    
#     print("plain text1    :  "+pt1.hex())
#     print("plain text2    :  "+pt2.hex())
#     print("plain text3    :  "+pt3.hex())
#     print("plain text4    :  "+pt4.hex())

    
#     # encrypt_data =  cipher.encrypt(bytes(pt))

#     encrypt_data1 =  cipher.encrypt(bytes(pt1))
#     encrypt_data2 =  cipher.encrypt(bytes(pt2))
#     encrypt_data3 =  cipher.encrypt(bytes(pt3))
#     encrypt_data4 =  cipher.encrypt(bytes(pt4))
    
#     print("returned ct1   :  "+ct1.hex())
#     print("python AES1    :  "+encrypt_data1.hex())
#     print("returned ct2   :  "+ct2.hex())
#     print("python AES2    :  "+encrypt_data2.hex())
#     print("returned ct3   :  "+ct3.hex())
#     print("python AES3    :  "+encrypt_data3.hex())
#     print("returned ct4   :  "+ct4.hex())
#     print("python AES4    :  "+encrypt_data4.hex())
#     # term.port.write(key)
#     # key_get=term.port.read(16)
#     # print("key           :  "+key_get.hex())
#     t = encrypt_data1 == ct1  and encrypt_data2 == ct2 and encrypt_data3 == ct3 and encrypt_data4 == ct4
#     # t = pt1 == ct1  and pt2 == ct2 and pt3 == ct3 and pt4 == ct4
#     # t = pt == ct
#     print(t)
    
#     if(not t):
#         num+=1
#         while(term.port.read()==b''):
#             time.sleep(0.1)
#             break
#     else:
#         elapsed = (elapsed*(j-num)+ end - start)/(j+1-num)




        
#     # time.sleep(0.1)
# if(num):    
#     print("There are total "+str(num)+" time(s) (out of "+str(loop)+") check failure")
# else:
#     print("All AES ("+str(loop)+") calculations are correct")
# print("Average time for one AES run is "+str(elapsed/4))
            
##########################################
##############AES_IPM_VEC Test############
##########################################
# for i in range(5):
#     print(term.port.read(16).hex())
# end = time.time()
# print(str(end-start))
