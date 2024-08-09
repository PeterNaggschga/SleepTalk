from concurrent.futures import ProcessPoolExecutor as ConcurrentExecutor

import numpy as np
import tensorflow as tf


def _to_ragged(tensors):
    return tf.ragged.constant(tensors, ragged_rank=1)


def transform_to_tensor(data: list[tuple[np.ndarray, np.ndarray]], nr_batches: int = 32) \
        -> tuple[tf.Tensor, tf.Tensor]:
    training_embeddings, training_labels = zip(*data)
    del data

    k, m = divmod(len(training_embeddings), nr_batches)
    training_embeddings = [training_embeddings[i * k + min(i, m): (i + 1) * k + min(i + 1, m)] for i in
                           range(nr_batches)]

    print("Creating batch tensors")
    with ConcurrentExecutor() as pool:
        training_embeddings = pool.map(_to_ragged, training_embeddings)

    training_embeddings = list(training_embeddings)

    print("Concatenating batch tensors")
    training_embeddings = tf.concat(list(training_embeddings), axis=0).to_tensor()
    training_labels = tf.constant(training_labels)

    return training_embeddings, training_labels
