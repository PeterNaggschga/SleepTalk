from __future__ import annotations

import argparse
import os
from concurrent.futures import ThreadPoolExecutor
from time import gmtime, strftime
from typing import Generator, Iterable

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
                    help="If selected, the embedding values will be quantized and saved in 8 bit unsigned integers "
                         "characters instead of 32 bit floats.")
parser.add_argument("--samples-per-file", "-s", default=100, help="Defines the number of training samples saved per "
                                                                  "TFRecord file.")

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


def get_labels(row: tuple[str, int, int, int, int, int, int]) -> list[int]:
    return [i for i, val in enumerate(row[1:]) if val == 1]


def get_embeddings(path: str) -> list[list[np.float32] | bytes]:
    pcm, _ = librosa.load(path, sr=16000)
    result = list(vggish(pcm).numpy())
    if args.quantize:
        return list(map(lambda x: ((x + 1) * 2 ** 7).astype(np.uint8).tobytes(), result))

    return [x.tolist() for x in result]


def get_sequence_example(row: tuple[str, int, int, int, int, int, int]) -> tf.train.SequenceExample:
    labels_list = get_labels(row)
    embeddings = get_embeddings(row[0])

    context = tf.train.Features(feature={
        'labels': tf.train.Feature(int64_list=tf.train.Int64List(value=labels_list)),
        'quantization': tf.train.Feature(int64_list=tf.train.Int64List(value=[1 if args.quantize else 4])),
    })

    if args.quantize:
        embedding_feature_list = tf.train.FeatureList(feature=[
            tf.train.Feature(bytes_list=tf.train.BytesList(value=[embedding])) for embedding in embeddings
        ])
    else:
        embedding_feature_list = tf.train.FeatureList(feature=[
            tf.train.Feature(float_list=tf.train.FloatList(value=embedding)) for embedding in embeddings
        ])

    sequence_example = tf.train.SequenceExample(
        context=context,
        feature_lists=tf.train.FeatureLists(feature_list={
            'embeddings': embedding_feature_list,
        })
    )

    return sequence_example


def split(split_list: list, part_size: int) -> Generator[list]:
    nr_parts = int(np.ceil(len(split_list) / part_size))
    k, m = divmod(len(split_list), nr_parts)
    return (split_list[i * k + min(i, m): (i + 1) * k + min(i + 1, m)] for i in range(nr_parts))


def write_tfrecord(file_examples: tuple[str, Iterable[tf.train.SequenceExample]]) -> None:
    file, examples = file_examples

    options = tf.io.TFRecordOptions(compression_type="GZIP")
    with tf.io.TFRecordWriter(os.path.join(args.output_dir, file), options=options) as writer:
        for example in examples:
            writer.write(example.SerializeToString())


labels = pd.read_csv(args.label_csv)
filename = strftime("%Y-%m-%d_%H-%M-%S", gmtime())
with ThreadPoolExecutor() as pool:
    sequence_examples = pool.map(get_sequence_example, labels.itertuples(index=False))

    sequence_examples_split = split(list(sequence_examples), args.samples_per_file)
    sequence_examples_split = (
        (f"{filename}_{i}.tfrecord", examples) for i, examples in enumerate(sequence_examples_split)
    )

    pool.map(write_tfrecord, sequence_examples_split)
