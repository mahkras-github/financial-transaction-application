from pyspark.sql import SparkSession

spark = SparkSession.builder.appName("TransactionAnalysis").getOrCreate()

spark.stop()

