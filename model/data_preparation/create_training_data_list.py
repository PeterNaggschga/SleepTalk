import argparse
import csv
import os

import librosa
import numpy as np

from tflite_model import TFLiteModel

parser = argparse.ArgumentParser()
parser.add_argument("--model-file", "-m", default=os.path.join(os.pardir, "model.tflite"),
                    help="Path to the TFLite model used to pre-classify new data.")
parser.add_argument("--data-dir", "-d", default=os.path.join(os.pardir, "future_training_data"),
                    help="Directory containing the new data.")
parser.add_argument("--label-csv", "-l", default="labels.csv", help="File that is written.")
parser.add_argument("--classes", "-c", default=os.path.join(os.pardir, "classes.csv"),
                    help="Path to csv-file containing the available classes.")
parser.add_argument("--all-zero", "-z", default=False, action=argparse.BooleanOptionalAction,
                    help="If selected, no model is used for pretraining and every sample is initialized with only "
                         "zeros as labels.")

args = parser.parse_args()

if not os.path.isdir(args.data_dir):
    print(f"New training data must be placed in {args.data_dir} directory!")
    exit(1)

classes = []
with open(args.classes, newline='') as class_file:
    reader = csv.reader(class_file)
    next(reader)
    for row in reader:
        if row[1] == "True":
            classes.append(row[0])

if not args.all_zero:
    model = TFLiteModel(args.model_file)


def get_labels(path: str) -> list[int]:
    pcm, _ = librosa.load(path, sr=16000)
    return [int(round(x)) for x in model(np.array([pcm]))[0]]


new_files = (f"{args.data_dir}/{filename}" for filename in os.listdir(args.data_dir))

if args.all_zero:
    labels = ([0] * len(classes) for _ in new_files)
else:
    labels = map(get_labels, new_files)

lines = [[filename] + label for filename, label in zip(new_files, labels)]

lines = [["filename"] + classes] + lines

with open(args.label_csv, "w", newline="") as label_file:
    writer = csv.writer(label_file)
    writer.writerows(lines)
