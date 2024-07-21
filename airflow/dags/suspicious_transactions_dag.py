from airflow import DAG
from airflow.utils.dates import days_ago
from airflow.providers.http.operators.http import SimpleHttpOperator
from airflow.providers.apache.spark.operators.spark_submit import SparkSubmitOperator
from airflow.operators.python_operator import PythonOperator
from airflow.models import Variable
from datetime import timedelta, datetime
import json

default_args = {
    'owner': 'airflow',
    'start_date': days_ago(1),
    'retries': 1,
}

dag = DAG(
    'suspicious_transactions_dag',
    default_args=default_args,
    description='DAG to create suspicious transactions and check them using Spark',
    schedule_interval=timedelta(seconds=20),  # Set the DAG to run every 20 seconds
    catchup=False,
)

# Initialize the transaction_id if it does not exist
if Variable.get("transaction_id", default_var=None) is None:
    Variable.set("transaction_id", 1001)

def prepare_transaction(**kwargs):
    transaction_id = Variable.get("transaction_id", default_var=1001)
    current_timestamp = datetime.now().isoformat()  # Get current system timestamp in ISO format
    transaction = {
        "transactionId": f"txn_{transaction_id}",
        "fromAccountId": "account_1",
        "toAccountId": "account_2",
        "amount": 1500.00,  # Amount greater than 1000
        "transactionDate": current_timestamp,
        "transactionType": "Transfer"
    }
    Variable.set("transaction_id", int(transaction_id) + 1)
    return transaction

prepare_transaction_task = PythonOperator(
    task_id='prepare_transaction',
    python_callable=prepare_transaction,
    provide_context=True,
    dag=dag,
)

generate_task = SimpleHttpOperator(
    task_id='generate_suspicious_transactions',
    method='POST',
    http_conn_id='my_http',
    endpoint='api/transactions',
    data='{{ task_instance.xcom_pull(task_ids="prepare_transaction") }}',
    headers={"Content-Type": "application/json"},
    dag=dag,
)

spark_submit_task = SparkSubmitOperator(
    task_id='check_suspicious_transactions',
    application='/Users/macbook/spark/spark_application.py',
    conn_id='my_spark',
    verbose=True,
    dag=dag,
)

prepare_transaction_task >> generate_task >> spark_submit_task
