# Renders the app icon: python3 scripts/make-icon.py ios/Lunaria/Assets.xcassets/AppIcon.appiconset/AppIcon.png  (needs Pillow)
import sys, math
from PIL import Image, ImageDraw, ImageFilter, ImageChops
S=4096; N=1024
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
# diagonal gradient background
top=(0xFF,0x9A,0xB0); bot=(0xD8,0x3F,0x68)
g=Image.new("RGB",(256,256))
px=g.load()
for y in range(256):
    for x in range(256):
        t=min(1,max(0,(x*0.35+y)/(256*1.35)))
        px[x,y]=lerp(top,bot,t)
bg=g.resize((S,S),Image.BICUBIC).convert("RGBA")
# soft glow behind moon
glow=Image.new("L",(S,S),0); ImageDraw.Draw(glow).ellipse([S*.2,S*.18,S*.8,S*.78],fill=110)
glow=glow.filter(ImageFilter.GaussianBlur(S*.08))
bg=Image.composite(Image.new("RGBA",(S,S),(255,235,240,255)),bg,glow)
# crescent
cx,cy,r=S*.5,S*.5,S*.30
m=Image.new("L",(S,S),0); d=ImageDraw.Draw(m)
d.ellipse([cx-r,cy-r,cx+r,cy+r],fill=255)
cut=Image.new("L",(S,S),0); ox,oy,rr=cx+r*.42,cy-r*.30,r*.86
ImageDraw.Draw(cut).ellipse([ox-rr,oy-rr,ox+rr,oy+rr],fill=255)
moon=ImageChops.subtract(m,cut)
# shadow
sh=moon.filter(ImageFilter.GaussianBlur(S*.02)).point(lambda v:int(v*.35))
shadow=Image.new("RGBA",(S,S),(120,20,50,0)); shadow.putalpha(sh)
bg=Image.alpha_composite(bg,shadow.transform((S,S),Image.AFFINE,(1,0,0,0,1,-S*.015)))
white=Image.new("RGBA",(S,S),(255,255,255,255)); white.putalpha(moon)
bg=Image.alpha_composite(bg,white)
# four-point sparkle stars
def star(x,y,R,a=255):
    k=R*.22; pts=[]
    for i in range(8):
        ang=math.pi/2*i/2-math.pi/2
        rad=R if i%2==0 else k
        pts.append((x+rad*math.cos(ang),y+rad*math.sin(ang)))
    l=Image.new("L",(S,S),0); ImageDraw.Draw(l).polygon(pts,fill=a)
    w=Image.new("RGBA",(S,S),(255,255,255,255)); w.putalpha(l); return w
bg=Image.alpha_composite(bg,star(S*.66,S*.40,S*.075))
bg=Image.alpha_composite(bg,star(S*.75,S*.57,S*.04,220))
bg.convert("RGB").resize((N,N),Image.LANCZOS).save(sys.argv[1])
