import os
import logging
from kafka import KafkaProducer
from flask import Flask, request


app = Flask(__name__)


@app.route('/')
def health():
    try:
        test_producer = create_producer()
        test_producer.close()
        return '<h1>Hello from Video Ingest</h2>'
    except Exception as e:
        servers = os.environ.get('KAFKA_SERVERS', '')
        logging.error(f'Error creating Video Ingest {e} {servers}')
        return 'Error creating Video Ingest'


@app.route('/frame/<beam_id>/<slice_id>/<frame_id>', methods=['POST']) 
def send_frame(beam_id, slice_id, frame_id):
    try:
        logging.info('Sending frame {beam_id} {slice_id} {frame_id}')
        producer = create_producer()
        producer.send(topic=beam_id, value=request.data)
    except Exception as e:
        logging.error(f'Error sending frame {e}')


def create_producer(servers=os.environ.get('KAFKA_SERVERS', '')):
    return KafkaProducer(bootstrap_servers=servers)


if __name__ == "__main__":
    app.run(debug=True)