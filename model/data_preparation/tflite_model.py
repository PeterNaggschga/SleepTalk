import numpy as np
import tensorflow as tf


# TODO: Documentation
class TFLiteModel:
    def __init__(self, path: str = "model.tflite") -> None:
        self.interpreter = tf.lite.Interpreter(path)
        self.input_index = self.interpreter.get_input_details()[0]["index"]
        self.output_index = self.interpreter.get_output_details()[0]["index"]

    def __call__(self, data: np.ndarray, *args, **kwargs):
        self.interpreter.resize_tensor_input(self.input_index, data.shape)
        self.interpreter.allocate_tensors()

        self.interpreter.set_tensor(self.input_index, data)
        self.interpreter.invoke()
        return self.interpreter.get_tensor(self.output_index)
