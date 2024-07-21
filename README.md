# financial-transaction-application

## Projenin Amacı
Finansal Analiz projesi kapsamında 3 mikroservis geliştirilmiştir. Spark işlemlerini görüntülemek ve yönetmek için Apache Airflow kütüphanesi kullanılmıştır. 
Mikroservisler şu şekildedir:
1. financial-transaction -> Finansal hareketleri kafka kuyruk yapısına kaydeden data producer mikroservis.
2. financial-transaction-analysis -> Kafka consumer mikroservis. Gelen kayıtları veri deposuna kaydeder. Spark kütüphanesi aracılığıyla işlemler şüpheli veya normal şekilide raporlanır.
3. financial-transaction-analysis-web -> Admin kullanımı için mikroservisi. Gelen analiz isteklerini veri deposumdan çekip görüntüler.

### financial-transaction
Proje Detayları:

1. Derleyici: Gradle - 8.7, Groovy
2. Dil: java - 17
3. Çerçeve: Spring Boot - 3.3.1
4. Kafka - docker image (apache/kafka)
5. Kütüphaneler: projectlombok - 1.18.34

### financial-transaction-analysis
Proje Detayları:

1. Derleyici: Gradle - 7.5, Groovy
2. Dil: java - 1.8
3. Çerçeve: Spring Boot - 2.7.2
4. Kafka - docker image (apache/kafka)
5. h2database - docker image (thomseno/h2)
6. Kütüphaneler: Apache Spark - 3.5.1

### financial-transaction-analysis-web
Proje Detayları:

1. Derleyici: Gradle - 8.7, Groovy
2. Dil: java - 17
3. Çerçeve: Spring Boot - 3.5.1
5. h2database - docker image (thomseno/h2)
6. Kütüphaneler: projectlombok - 1.18.34

## Projelerin Çalıştırılması
Gereksinimler:
- docker hub 
- Kafka Kuyruk Uygulaması
- h2database
- Intellij IDEA

### Kafka
Aşağıdaki komut ile docker hub 'dan kafka uygulaması çalıştırılır.

`docker run -p 9092:9092 apache/kafka
`

### h2database
Aşağıdaki komut ile docker hub 'dan h2database uygulaması çalıştırılır.

`docker run -d -p 9093:9092 -p 8083:8082 -v /path/to/local/h2/h2-data:/h2-data --name=my-h2 thomseno/h2
`

### Mikroservislerin Çalıştırılması
Mikroservisler Intellij IDEA kod geliştirici arayüzünde açılıp derlendikten sonra, ilgili "*Application" classı Run/Debug edilebilir.
Projelerin ortam ayar değerleri "application.properties" dosyasından değiştirilebilir.

"financial-transaction-analysis" projesi çalıştırılırken aşağıdaki VM options eklenmelidir.

`--add-exports java.base/sun.nio.ch=ALL-UNNAMED
`

## Apache Airflow
Apache airflow projenin yüklü işlem testlerinde ve bu işlemlerin kontrolünde kullanılmıştır. Apache Airflow akışları UI arayüzünden yönetilmiştir.

### Kurulum
Gereksinimler:
- python3 
- pip

Aşağıdaki komutlar ile apache airflow kurulumu gerçekleştirilebilir. (Not: kurulum MacOS ortamı için geçerlidir.)

`pip install apache-airflow
`

### Servislerin Ayağa kaldırılması
Aşağıdaki komutlar ile python env yaratılır.

* `python3 -m venv airflow_venv`
* `source airflow_venv/bin/activate`


Aşağıdaki komutlar ile airflow servisleri başlatılır.

* `airflow db init`
* `airflow webserver --port 8080`
* `airflow scheduler`


### Spark İşlerini Yönetmek ve İzlemek için DAG Dosyası
Aşağıdaki dag dosyası yardımıyla transaction işlemi 30 saniye aralıklarla simüle edilmiştir.
transaction.amount değeri 1000' den büyük ve 1 dakika içerisinde 2 kez gönderilerek şüpheli işlem gibi raporlanması sağlanmıştır.
Dosya "AIRFLOW_HOME/dags" dizinine kopyalanarak, çalışması sağlanır. (Not: airflow scheduler servisinin tekrar başlatılması da gereklidir)

Airflow bağlantılarının hazırlanması için Airflow UI kullanılmıştır. 

Arayüzden Admin -> Connections -> (+) butonu ile "http" ve "spark" bağlantı tipinde yeni executorlar yaratılmıştır.

"suspicious_transactions_dag.py" -> bu dag akışı "financial-analysis" mikroservisine, SimpleHttpOperator ile http isteği atar ve SparkSession 'dan kontrol eder.



