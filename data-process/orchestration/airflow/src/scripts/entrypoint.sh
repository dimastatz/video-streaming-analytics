#!/usr/bin/env bash
airflow db upgrade
airflow users  create --role Admin --username admin --email admin --firstname admin --lastname admin --password admin
airflow webserver