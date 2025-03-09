import cv2
import pandas as pd

# Path to the CSV file and image directory
CSV_PATH = "data.csv"
IMAGE_DIR = "./images/"  # Change this to your actual image directory

# Read the CSV file
df = pd.read_csv(CSV_PATH)

old_image = None
start_image = 0
current_image = 0

# Loop through each row and draw the bounding boxes
for _, row in df.iterrows():
    filename, x_min, y_min, width, height = row["Image"], int(row["xMin"]), int(row["yMin"]), int(row["width"]), int(row["height"])
    
    # Calculate y_min from y_max and height
    y_max = y_min + height
    x_max = x_min + width

    if filename != old_image:
        current_image += 1
        if old_image != None:
            if current_image > start_image:
                # Show the image
                cv2.imshow("Labeled Image", image)
                # Wait for key press
                key = cv2.waitKey(0)
                cv2.destroyAllWindows()
        old_image = filename
        image_path = IMAGE_DIR + filename
        image = cv2.imread(image_path)
        if image is None:
            print(f"Error: Unable to load image {image_path}")
            exit()
    
    # Draw rectangle (BGR color: green)
    cv2.rectangle(image, (x_min, y_min), (x_max, y_max), (0, 255, 0), 2)
    
    # Put label
    label = row["MobType"]
    cv2.putText(image, label, (x_min, y_min - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

    
cv2.imshow("Labeled Image", image)
cv2.waitKey(0)
cv2.destroyAllWindows()