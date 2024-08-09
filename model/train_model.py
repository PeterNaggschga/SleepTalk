import argparse

from model.data_extraction import DataExtraction
from model.model import SleepTalkModel
from model.tensor_conversion import transform_to_tensor

parser = argparse.ArgumentParser()
parser.add_argument("--classes", "-c", default="classes.csv",
                    help="Path to a csv file containing information on the different classes and their synonyms in "
                         "google labels.")
parser.add_argument("--model-file", "-m", default="model.tflite", help="Path where the model should be saved.")
parser.add_argument("--patience", "-p", default=5, type=int,
                    help="Number of epochs before model training is aborted, when the validation loss does not "
                         "improve anymore.")
parser.add_argument("--epochs", "-e", default=30, type=int, help="Maximum number of epochs to train the model.")
parser.add_argument("--batch-size", "-b", default=32, type=int, help="Batch size in training.")
parser.add_argument("--val-split", "-v", default=.15, type=float, help="Fraction of training data to use vor "
                                                                       "validation.")
parser.add_argument("--delete-data", "-d", default=False, action=argparse.BooleanOptionalAction,
                    help="If set, google embeddings are removed after the training is done.")
parser.add_argument("--force-download", "-f", default=False, action=argparse.BooleanOptionalAction,
                    help="If set, google embeddings are newly downloaded, even if the corresponding directory already "
                         "exists.")

args = parser.parse_args()

data_extractor = DataExtraction(args.classes)

print("Downloading google data...")
data_extractor.download_google_data(force=args.force_download)
print("done")

print("Reading google data...")
google_class, google_no_class = data_extractor.retrieve_google_data()
print("done")

# TODO: get own training data


# TODO: balance whole training data

google_data = [x for y in zip(google_class, google_no_class) for x in y]
del google_class, google_no_class

print("Converting data to tensors...")
training_embeddings, training_labels = transform_to_tensor(google_data)
del google_data
print("done")

model = SleepTalkModel(len(data_extractor.classes))

model.fit_classifier(training_embeddings, training_labels, patience=args.patience, validation_split=args.val_split,
                     batch_size=args.batch_size, epochs=args.epochs)

model.export(args.model_file)

if args.delete_data:
    data_extractor.delete_google_data()
