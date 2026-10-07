from PIL import Image
import numpy as np
from scipy import ndimage as ndi
import os, sys
from pathlib import Path
ROOT=str(Path(__file__).resolve().parents[1])
NAMES=[
 'jar_empty','coin_pile','coin','gacha_machine','desk_clock','mushroom_lamp','cat_calico',
 'rabbit','film_camera','snow_globe','world_globe','books_standing','books_stack','framed_landscape',
 'monstera','anthurium','pothos_trailing','pothos_potted','ivy_hanging','flower_vase','reed_diffuser',
 'wooden_house','framed_flowers','framed_night','storage_boxes_cream','storage_box_green','basket','ceramic_vases',
 'bonsai','stacked_stones','wooden_tray','mini_mountains','moon_lamp','bird_figure','pen_cup']
XE=[0,225,440,652,875,1083,1300,1536]
YE=[0,247,455,685,850,1024]
im=Image.open(f'{ROOT}/Items/Cozy Home and Plant Asset Sticker Sheet.png').convert('RGBA')
a=np.array(im); al=a[...,3]
core=al>=100
lab,n=ndi.label(core)
sizes=ndi.sum(core,lab,range(1,n+1)); cms=ndi.center_of_mass(core,lab,range(1,n+1))
group={}   # cell index -> list of labels
for i,(sz,(cy,cx)) in enumerate(zip(sizes,cms),1):
    if sz<60: continue
    col=max(k for k in range(7) if cx>=XE[k]); row=max(k for k in range(5) if cy>=YE[k])
    group.setdefault(row*7+col,[]).append(i)
owner=np.zeros(lab.shape,dtype=np.int32)   # cell+1 owning each core pixel
for cell,labels in group.items():
    owner[np.isin(lab,labels)]=cell+1
report=[]
for cell,labels in sorted(group.items()):
    gm=(owner==cell+1)
    grown=ndi.binary_dilation(gm,iterations=4)
    keep=grown&((owner==0)|(owner==cell+1))
    out=a.copy(); alpha=al.copy().astype(np.int32)
    alpha[~keep]=0
    alpha[alpha<10]=0
    out[...,3]=alpha.astype(np.uint8)
    ys,xs=np.where(alpha>0); y0,y1,x0,x1=ys.min(),ys.max()+1,xs.min(),xs.max()+1
    pad=3; y0=max(0,y0-pad); x0=max(0,x0-pad); y1=min(out.shape[0],y1+pad); x1=min(out.shape[1],x1+pad)
    crop=Image.fromarray(out[y0:y1,x0:x1],'RGBA')
    crop.save(f'{ROOT}/Cropped/Items/{NAMES[cell]}.png',optimize=True)
    report.append((NAMES[cell],crop.size))
print(len(report),'items'); missing=[NAMES[i] for i in range(35) if i not in group]; print('missing',missing)
for r in report: print(r)
