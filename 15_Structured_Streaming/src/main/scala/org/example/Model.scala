package org.example

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.feature._
import org.apache.spark.sql.SparkSession

class Model {
  def createModel(dataPath: String, dataFileName: String, modelPath: String ): Unit = {

    implicit val spark: SparkSession = SparkSession
      .builder()
      .appName("IrisClassificationModel")
      .config("spark.master", "local")
      .getOrCreate()

    val data = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(s"$dataPath/$dataFileName")
      .toDF()

    val assembler = new VectorAssembler()
      .setHandleInvalid("skip")
      .setInputCols(Array("sepal_length", "sepal_width", "petal_length", "petal_width"))
      .setOutputCol("features")

    // Index labels, adding metadata to the label column.
    // Fit on whole dataset to include all labels in index.
    val labelIndexer = new StringIndexer()
      .setInputCol("species")
      .setOutputCol("species_indexed")
      .fit(data)

    // Train a RandomForest model
    val randomForest = new RandomForestClassifier()
      .setFeaturesCol("features")
      .setLabelCol("species_indexed")
      .setFeatureSubsetStrategy("sqrt")
      .setNumTrees(15)

    // Convert indexed labels back to original labels
    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(labelIndexer.labelsArray(0))

    //creating pipeline
    // Chain indexers and forest in a Pipeline
    val pipeline = new Pipeline()
      .setStages(Array(labelIndexer, assembler, randomForest, labelConverter))

    //fitting the model
    val model = pipeline.fit(data)

    model.write.overwrite().save(modelPath)
  }
}
