FROM apache/airflow:2.1.4

COPY ./airflow/src/dags /opt/airflow/dags
COPY ./airflow/src/scripts /opt/airflow/scripts

USER root
RUN chmod 777 /opt/airflow/scripts/entrypoint.sh
USER airflow