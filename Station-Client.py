from socket import *

def send_tcp():
    s = socket(AF_INET,SOCK_STREAM)
    s.connect(('localhost',4002))
    data="TCP "*4
    s.send(data)
    s.close()

def send_udp():
    s = socket(AF_INET,SOCK_DGRAM)
    data="UDP "*4
    s.sendto(data, ('localhost',4003))
    s.close()

if __name__ == '__main__':
    send_tcp()
    send_udp()