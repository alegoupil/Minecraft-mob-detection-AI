import cv2
import pandas as pd
import os
import numpy as np
import PIL
from PIL import Image

# Path to the CSV file and image directory
CSV_PATH = "data.csv"
IMAGE_DIR = "./images/"

IMAGE_SIZE = 64 # Dimension des images en entrée du réseau
CELL_PER_DIM = 8 # Nombre de cellules en largeur et en hauteur
BOX_PER_CELL = 1 # Nombre d'objets par cellule
NB_CLASSES = 4 # Nombre de classes du problème
PIX_PER_CELL = round(IMAGE_SIZE/CELL_PER_DIM)

# Read the CSV file
df = pd.read_csv(CSV_PATH)

print("CSV imported")

total_classifications = 0
dataset_size = len(os.listdir(IMAGE_DIR))

x = np.zeros((dataset_size, IMAGE_SIZE, IMAGE_SIZE, 3))

# Group bounding boxes by image
y = []
test = True
boxes = []
last_image_name = ""
for _, row in df.iterrows():

    filename = row["Image"]

    if last_image_name != filename:

        # Lecture de l'image : on va remplir la variable x
        # Lecture de l'image
        img = Image.open(IMAGE_DIR+filename)
        # Mise à l'échelle de l'image
        img = img.resize((IMAGE_SIZE, IMAGE_SIZE), Image.LANCZOS)
        # Remplissage de la variable x
        x[total_classifications] = np.asarray(img, dtype=np.int32)
        if not test :
            y.append(boxes)
        else :
            test = False
        boxes=[]
        last_image_name = filename

    box = []
    x_min, y_min, width, height = int(row["xMin"]), int(row["yMin"]), int(row["width"]), int(row["height"])
    box.append(x_min+width/2)
    box.append(y_min+height/2)
    box.append(width)
    box.append(height)
    label = row["MobType"]
    box.append(label)
    boxes.append(box)
    


    total_classifications += 1

y.append(boxes)

print(y)
print("Data linked to images")

# Get sorted list of images
image_files = sorted(os.listdir(IMAGE_DIR))

print("\n------ Results ------")
print(f"Total images in folder : {len(image_files)}")
print(f"Images with presence : {len(x)}")
print(f"Total classifications : {total_classifications}")
print("")