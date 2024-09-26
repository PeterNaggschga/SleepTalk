from __future__ import annotations

import argparse
import os

import librosa
import numpy as np
import pandas as pd
import tensorflow as tf
import tensorflow_hub as tf_hub

parser = argparse.ArgumentParser()
parser.add_argument("--data-dir", "-d", default=os.path.join(os.pardir, "future_training_data"),
                    help="Directory containing the new data.")
parser.add_argument("--label-csv", "-l", default="labels.csv", help="File containing the correct labels.")
parser.add_argument("--output-dir", "-o", default=os.path.join(os.pardir, "training_data"),
                    help="Directory where the created training data is stored.")
parser.add_argument("--quantize", "-q", default=False, action=argparse.BooleanOptionalAction,
                    help="Is selected, the embedding values will be quantized and saved in 8 bit unsigned integers "
                         "characters instead of 32 bit floats.")

args = parser.parse_args()

if not os.path.isdir(args.data_dir):
    print(f"New training data must be placed in {args.data_dir} directory!")
    exit(1)

if not os.path.exists(args.label_csv):
    print(f"Label data must be placed in {args.label_csv}!")
    exit(1)

if not os.path.exists(args.output_dir):
    os.mkdir(args.output_dir)

vggish = tf_hub.load("https://www.kaggle.com/models/google/vggish/TensorFlow2/vggish/1")


def get_labels(row: pd.Series) -> list[int]:
    return [i for i, val in enumerate(row[1:]) if val == 1]


def get_embeddings(path: str) -> list[np.ndarray[np.float32]] | list[np.ndarray[np.byte]]:
    pcm, _ = librosa.load(path, sr=16000)
    result = list(vggish(pcm).numpy())
    if args.quantize:
        result = list(map(lambda x: ((x + 1) * 2 ** 7).astype(np.uint8).tobytes(), result))
    return result


labels = pd.read_csv(args.label_csv)

classes = labels.columns.tolist()[1:]

for _, row in labels.iterrows():
    labels = get_labels(row)
    embeddings = get_embeddings(row['filename'])

    context = tf.train.Features(feature={
        'labels': tf.train.Feature(int64_list=tf.train.Int64List(value=labels)),
        'quantization': tf.train.Feature(int64_list=tf.train.Int64List(value=[1 if args.quantize else 4])),
    })

    # TODO: create embedding list either as list of float32 or bytelist
    embedding_feature_list = None

    sequence_example = tf.train.SequenceExample(
        context=context,
        feature_lists=tf.train.FeatureLists(feature_list={
            'embeddings': embedding_feature_list,
        })
    )

    print(sequence_example)

    # TODO: save sequence_example(s) as TFRecords
    break
