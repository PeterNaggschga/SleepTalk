import numpy as np
import tensorflow as tf
import tf_keras
from tensorflow_hub import KerasLayer
from tf_keras import Sequential
from tf_keras.layers import Conv1D, MaxPooling1D, GlobalMaxPooling1D, Dropout, Dense, Input, Lambda


# TODO: Documentation
class SleepTalkModel:
    def __init__(self, nr_classes: int, optimizer: str = "adam", loss: str = "binary_crossentropy",
                 metrics: list[str] = None):
        self.nr_classes = nr_classes
        self.optimizer = optimizer
        self.loss = loss
        if metrics is None:
            self.metrics = ["accuracy"]
        else:
            self.metrics = metrics

        self.classifier = None

    def _get_classifier(self, weights: list[np.ndarray] = None) -> Sequential:
        result = Sequential()

        result.add(Input(shape=(None, 128), dtype=tf.float32))

        # adding layers for classification
        result.add(Conv1D(64, kernel_size=5, strides=1, padding="same", activation="relu", data_format="channels_last"))
        result.add(MaxPooling1D(padding="same", data_format="channels_last"))  # TODO: check whether helpful
        result.add(Dropout(0.3))  # TODO: check whether helpful

        result.add(
            Conv1D(128, kernel_size=5, strides=1, padding="same", activation="relu", data_format="channels_last"))
        result.add(GlobalMaxPooling1D(data_format="channels_last"))
        result.add(Dropout(0.3))

        result.add(Dense(256, activation="relu"))
        result.add(Dropout(0.3))

        result.add(Dense(512, activation="relu"))
        result.add(Dropout(0.3))

        result.add(Dense(self.nr_classes, activation="sigmoid"))

        if weights is not None:
            result.set_weights(weights)

        result.compile(optimizer="adam", loss="binary_crossentropy", metrics=["accuracy"])
        return result

    def fit_classifier(self, embeddings: tf.Tensor, labels: tf.Tensor, batch_size: int = 32,
                       validation_split: float = .15, shuffle: bool = True, epochs: int = 30, patience: int = 5,
                       restore_best_weights: bool = True) -> None:
        if self.classifier is None:
            self.classifier = self._get_classifier()

        early_stop = tf_keras.callbacks.EarlyStopping(patience=patience, restore_best_weights=restore_best_weights)

        self.classifier.fit(embeddings, labels, batch_size=batch_size, validation_split=validation_split,
                            shuffle=shuffle, epochs=epochs, callbacks=[early_stop])

    def export(self, filename: str = "model.tflite") -> None:
        # downloading Googles VGGish model for sound embeddings
        # TODO: look into licensing
        vggish = KerasLayer("https://www.kaggle.com/models/google/vggish/TensorFlow2/vggish/1")

        # adding classification model
        model = Sequential([
            Input(shape=(None,), batch_size=1, dtype=tf.float32),
            Lambda(lambda x: tf.map_fn(vggish, x)),
            self.classifier
        ])

        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS,  # Enable TensorFlow Lite ops.
            tf.lite.OpsSet.SELECT_TF_OPS  # Enable TensorFlow ops.
        ]
        converter._experimental_lower_tensor_list_ops = False

        tflite_model = converter.convert()

        with tf.io.gfile.GFile(filename, "wb") as file:
            file.write(tflite_model)
