"""Reference-check of the Samsung AC encoder logic used in SamsungAc.kt.
Mirrors the Kotlin code 1:1 and compares against the known-good frames from
IRremoteESP8266 (IRSamsungAc::sendOn / sendOff)."""
RESET = [0x02,0x92,0x0F,0x00,0x00,0x00,0xF0, 0x01,0x02,0xAE,0x71,0x00,0x15,0xF0]
MIDDLE = [0x01,0xD2,0x0F,0x00,0x00,0x00,0x00]

class Ac:
    def __init__(s): s.raw = RESET[:]
    def setbits(s,i,shift,w,v):
        m=((1<<w)-1)<<shift; s.raw[i]=(s.raw[i]&~m&0xFF)|((v<<shift)&m)
    def power(s,on): v=3 if on else 0; s.setbits(6,4,2,v); s.setbits(13,4,2,v)
    def temp(s,t): s.setbits(11,4,4,min(30,max(16,t))-16)
    def mode(s,m):
        s.setbits(12,4,3,m)
        fan=(s.raw[12]>>1)&7
        if m==0: s.setbits(12,1,3,6)
        elif fan==6: s.setbits(12,1,3,0)
    def fan(s,f): s.setbits(12,1,3,f)
    def swing(s,on): s.setbits(9,4,3,0b010 if on else 0b111)

def section_sum(b,o):
    n=bin(b[o]).count("1")+bin(b[o+1]&0xF).count("1")+bin(b[o+2]>>4).count("1")
    n+=sum(bin(x).count("1") for x in b[o+3:o+7])
    return (~n)&0xFF

def frame(ac, extended):
    r=ac.raw[:]
    for o in (0,7):
        c=section_sum(r,o)
        r[o+1]=(r[o+1]&0x0F)|((c&0xF)<<4)
        r[o+2]=(r[o+2]&0xF0)|(c>>4)
    return r[:7]+MIDDLE+r[7:] if extended else r

def pattern(b):
    p=[690,17844]
    for o in range(0,len(b),7):
        p+=[3086,8864]
        for byte in b[o:o+7]:
            for bit in range(8):
                p+=[586,1432 if (byte>>bit)&1 else 436]
        p+=[586,2886]
    return p

ON =[0x02,0x92,0x0F,0,0,0,0xF0, 0x01,0xD2,0x0F,0,0,0,0, 0x01,0xE2,0xFE,0x71,0x80,0x11,0xF0]
OFF=[0x02,0xB2,0x0F,0,0,0,0xC0, 0x01,0xD2,0x0F,0,0,0,0, 0x01,0x02,0xFF,0x71,0x80,0x11,0xC0]

def check(name,got,exp):
    ok=got==exp
    print(("PASS " if ok else "FAIL ")+name)
    if not ok:
        print("  got", " ".join(f"{x:02X}" for x in got)); print("  exp", " ".join(f"{x:02X}" for x in exp))
    return ok

a=Ac(); a.mode(1); a.temp(24); a.fan(0); a.swing(False); a.power(True)
ok=check("ON  frame (cool,24C,fan auto,swing off) == library sendOn", frame(a,True), ON)
a.power(False)
ok&=check("OFF frame == library sendOff", frame(a,True), OFF)
a.power(True); f=frame(a,False)
ok&=(len(f)==14); print("PASS" if len(f)==14 else "FAIL","14-byte standard frame length")
p=pattern(frame(a,True)); print("pulse count",len(p),"(expect 2+3*(2+112+2)=350)"); ok&=len(p)==350
# every section checksum validates
fr=frame(a,True)
for o in (0,7,14):
    got=((fr[o+2]&0xF)<<4)|(fr[o+1]>>4); ok&=(got==section_sum(fr,o))
print("PASS" if ok else "FAIL","all section checksums self-consistent")
raise SystemExit(0 if ok else 1)
