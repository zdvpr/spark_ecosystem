# Домашнее задание

#### Настроите применение предобученной модели в Spark Streaming

**Цель:** 

Научимся обучать и сохранять модели Spark ML. Научимся использовать предобученные модели Spark ML в Spark Streaming

- Построить модель классификации Ирисов Фишера и сохранить её Описание набора данных: https://ru.wikipedia.org/wiki/%D0%98%D1%80%D0%B8%D1%81%D1%8B_%D0%A4%D0%B8%D1%88%D0%B5%D1%80%D0%B0 Набор данных в формате CSV: https://www.kaggle.com/arshid/iris-flower-dataset Набор данных в формате LIBSVM: https://github.com/apache/spark/blob/master/data/mllib/iris_libsvm.txt Должен быть предоставлен код построения модели (ноутбук или программа)
- Разработать приложение, которое читает из одной темы Kafka (например, "input") CSV-записи с четырми признаками ирисов, и возвращает в другую тему (например, "predictition") CSV-записи с теми же признаками и классом ириса
- 

### run docker image with kafka 
docker-compose up --build -d

### create kafka topics
docker exec 12_spark_ML_1 kafka-topics \
  --create \
  --zookeeper zookeeper:2181 \
  --replication-factor 1 \
  --partitions 3 \
  --topic input

docker exec 12_spark_ML_1 kafka-topics \
--create \
  --zookeeper zookeeper:2181 \
  --replication-factor 1 \
  --partitions 3 \
  --topic prediction

### create input data flow
docker exec -it 12_spark_ML_1 sh
kafka-console-producer --topic input --broker-list localhost:9092

### create output terminal windows
docker exec -it 12_spark_ML_1 sh
kafka-console-consumer --bootstrap-server localhost:9092 --topic prediction

### Learn model
ru.otus.sparkmlstreaming.IrisFIsherModel

### Use model
ru.otus.sparkmlstreaming.MLStreaming

### run the application
spark-submit MLStreaming-assembly-1.0.jar <path-to-model> <input-bootstrap-servers> <prediction-bootstrap-servers> <groupId> <input-topic> <prediction-topic> <data-path> <data-fileName>
