import ast
import csv
import os
import shutil
import tarfile
import urllib.request
from concurrent.futures import ProcessPoolExecutor as ConcurrentExecutor
from functools import reduce

import numpy as np
import tensorflow as tf


# TODO: Documentation
class DataExtraction:
    def __init__(self, class_file: str = "classes.csv", google_data_dir: str = "audioset_v1_embeddings",
                 google_label_file: str = "labels.csv"):
        self.google_data_dir = google_data_dir
        self.google_label_file = os.path.join(google_data_dir, google_label_file)
        self.classes = []
        self.no_class = ""

        self.class_file = class_file
        self.label_to_class = {}

    def _get_label_vector(self, labels: list[int]) -> np.ndarray:
        class_names = {self.label_to_class[label] for label in labels if label in self.label_to_class.keys()}
        label_data = [0 for _ in range(len(self.classes))]
        for class_name in class_names:
            if class_name != self.no_class:
                label_data[self.classes.index(class_name)] = 1
        return np.array(label_data)

    @staticmethod
    def _get_embeddings(sequence: tf.train.SequenceExample) -> np.ndarray:
        result = []
        for feature in sequence.feature_lists.feature_list["audio_embedding"].feature:
            byte_list = feature.bytes_list.value[0]
            byte_list = tf.io.decode_raw(byte_list, tf.uint8).numpy()
            byte_list = tf.cast(byte_list, tf.float32).numpy()
            result.append(byte_list / 2 ** 7 - 1)
        return np.array(result)

    def _get_file_content(self, filename) \
            -> tuple[list[tuple[np.ndarray, np.ndarray]], list[tuple[np.ndarray, np.ndarray]]]:
        with_class = []
        without_class = []
        for example in (tf.train.SequenceExample.FromString(record) for record in
                        tf.data.TFRecordDataset(filename).as_numpy_iterator()):
            labels = example.context.feature["labels"].int64_list.value
            if self.label_to_class.keys().isdisjoint(labels):
                continue
            label_vector = self._get_label_vector(labels)
            embeddings = self._get_embeddings(example)
            if max(label_vector) != 0:
                with_class.append((embeddings, label_vector))
            else:
                without_class.append((embeddings, label_vector))
        return with_class, without_class

    def download_google_data(self, force: bool = False, region: str = "eu") -> None:
        if force:
            self.delete_google_data()

        # download google training embeddings
        if not os.path.exists(self.google_data_dir):
            with urllib.request.urlopen(
                    f"https://storage.googleapis.com/{region}_audioset/youtube_corpus/v1/features/features.tar.gz") as response:
                with tarfile.open(fileobj=response, mode='r|gz') as targz:
                    targz.extractall(filter='tar')

        # download label file
        if not os.path.exists(self.google_label_file):
            print(self.google_label_file)
            urllib.request.urlretrieve(
                "https://storage.googleapis.com/us_audioset/youtube_corpus/v1/csv/class_labels_indices.csv",
                self.google_label_file)

        self.classes = []
        self.no_class = ""
        self.label_to_class = {}

        synonyms = {}
        with open(self.class_file, newline='') as class_file:
            reader = csv.reader(class_file)
            next(reader)
            for row in reader:
                if row[1] == "True":
                    self.classes.append(row[0])
                else:
                    self.no_class = row[0]

                for synonym in [x.strip() for x in ast.literal_eval(row[2])]:
                    synonyms[synonym] = row[0]

        with open(self.google_label_file, newline='') as label_file:
            reader = csv.reader(label_file)
            for row in filter(lambda r: r[2] in synonyms.keys(), reader):
                self.label_to_class[int(row[0])] = synonyms[row[2]]

    def retrieve_google_data(self, train_dir: str = "bal_train") \
            -> tuple[list[tuple[np.ndarray, np.ndarray]], list[tuple[np.ndarray, np.ndarray]]]:
        self.download_google_data()
        train_dir = os.path.join(self.google_data_dir, train_dir)

        filenames = (os.path.join(train_dir, file) for file in os.listdir(train_dir))
        with ConcurrentExecutor() as pool:
            intermediate_results = pool.map(self._get_file_content, filenames)

        google_class, google_no_class = reduce(lambda a, b: (a[0] + b[0], a[1] + b[1]), list(intermediate_results))

        np.random.shuffle(google_class)
        np.random.shuffle(google_no_class)

        return google_class, google_no_class

    def delete_google_data(self):
        if os.path.exists(self.google_data_dir):
            shutil.rmtree(self.google_data_dir)
