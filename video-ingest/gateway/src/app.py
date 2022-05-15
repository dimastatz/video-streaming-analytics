import os
import pickle
import logging
from pydoc_data.topics import topics
from flask import Flask, request
from kafka import KafkaProducer, KafkaConsumer
from kafka.admin import KafkaAdminClient, NewTopic


app = Flask(__name__)


@app.route('/')
def health():
    try:
        consumer = create_consumer()
        topics = consumer.topics()

        logging.warn(f'Running health {topics}')
        if not 'healthcheck' in topics:
            create_topic('healthcheck')

        consumer.close()
        return f'Healthcheck {topics}'
    except Exception as e:
        servers = os.environ['KAFKA_SERVERS']
        return f'Error creating Video Ingest {e} {servers}'


@app.route('/frame/<beam_id>/<slice_id>/<frame_id>', methods=['POST']) 
def send_frame(beam_id, slice_id, frame_id):
    try:
        logging.info('Sending frame {beam_id} {slice_id} {frame_id}')
        producer = create_producer()
        msg = pickle.dumps((slice_id, frame_id, request.data))
        producer.send(topic=beam_id, value=msg)
    except Exception as e:
        logging.error(f'Error sending frame {e}')


def create_producer(servers=os.environ.get('KAFKA_SERVERS', '')):
    return KafkaProducer(bootstrap_servers=servers)


def create_consumer(servers=os.environ.get('KAFKA_SERVERS', '')):
    return KafkaConsumer(group_id='gateway', bootstrap_servers=servers)


def create_topic(topic_name, servers=os.environ.get('KAFKA_SERVERS', '')):
    admin = KafkaAdminClient(bootstrap_servers=servers, client_id='gateway')
    topic = NewTopic(name=topic_name, num_partitions=1, replication_factor=1)
    admin.create_topics(new_topics=[topic], validate_only=False)
    admin.close()


if __name__ == "__main__":
    app.run(debug=True)